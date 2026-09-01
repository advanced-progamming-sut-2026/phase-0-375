package com.sut.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sut.server.repository.ServerUserRepository;
import com.sut.server.service.AuthService;
import model.network.packet.Packet;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
import model.network.packet.auth.LogoutRequestPacket;
import model.network.packet.auth.RegisterRequestPacket;
import model.network.packet.auth.RegisterResponsePacket;
import model.network.packet.system.ErrorMessagePacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial Challenger Test Suite for Network Auth Protocol & Session Lifecycle.
 * Stress-tests TCP concurrency, duplicate eviction thrashing, malformed JSON injection,
 * brute-force attacks, and abrupt socket disconnects.
 */
class AuthSessionChallengerTest {

    @TempDir
    Path tempDir;

    private TcpServer tcpServer;
    private ServerUserRepository userRepository;
    private AuthService authService;
    private PacketRouter router;
    private ObjectMapper objectMapper;
    private int serverPort;

    @BeforeEach
    void setUp() throws IOException {
        Path storagePath = tempDir.resolve("challenger-users.json");
        userRepository = new ServerUserRepository(storagePath);
        authService = new AuthService(userRepository);

        router = new PacketRouter();
        authService.registerRoutes(router);

        objectMapper = TcpServer.createDefaultObjectMapper();
        tcpServer = new TcpServer("127.0.0.1", 0, router, objectMapper);
        tcpServer.start();
        serverPort = tcpServer.getBoundPort();
    }

    @AfterEach
    void tearDown() {
        if (tcpServer != null) {
            tcpServer.stop();
        }
    }

    // Helper Raw Socket Client for Adversarial Packet Transmission
    private static class RawTestClient implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final ObjectMapper mapper;

        public RawTestClient(String host, int port, ObjectMapper mapper) throws IOException {
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(5000);
            this.socket.setTcpNoDelay(true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.mapper = mapper;
        }

        public void sendPacket(Packet packet) throws IOException {
            String json = mapper.writeValueAsString(packet);
            sendRawLine(json);
        }

        public void sendRawLine(String line) throws IOException {
            writer.write(line);
            writer.newLine();
            writer.flush();
        }

        public String readRawLine() throws IOException {
            return reader.readLine();
        }

        public Packet readPacket() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            return mapper.readValue(line, Packet.class);
        }

        public void abortConnection() throws IOException {
            socket.setSoLinger(true, 0); // Send TCP RST
            socket.close();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    @Test
    @DisplayName("Adversarial: Rapid concurrent registrations for identical username over TCP")
    void testConcurrentRegistrationsForIdenticalUsernameOverTcp() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> failureReasons = Collections.synchronizedList(new ArrayList<>());

            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int clientId = i;
                futures.add(executor.submit(() -> {
                    try (RawTestClient client = new RawTestClient("127.0.0.1", serverPort, objectMapper)) {
                        readyLatch.countDown();
                        startLatch.await(); // Simultaneous unleash

                        RegisterRequestPacket req = new RegisterRequestPacket();
                        req.setUsername("raceTargetUser");
                        req.setPasswordHash("Password@123");
                        req.setNickname("Target" + clientId);
                        req.setEmail("racetarget" + clientId + "@pvz.com");
                        req.setGender("male");
                        req.setSecurityQuestionNumber(1);
                        req.setSecurityAnswer("SecretAnswer");

                        client.sendPacket(req);
                        Packet resp = client.readPacket();

                        assertNotNull(resp, "Client " + clientId + " received null packet");
                        assertInstanceOf(RegisterResponsePacket.class, resp);
                        RegisterResponsePacket regResp = (RegisterResponsePacket) resp;

                        if (regResp.isSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                            failureReasons.add(regResp.getMessage());
                        }
                    } catch (Exception e) {
                        failureReasons.add("Exception: " + e.getMessage());
                    }
                }));
            }

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
            startLatch.countDown(); // Fire!

            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
            executor.shutdown();

            // Verification: Exactly one registration must succeed, and all others must fail
            assertEquals(1, successCount.get(), "Expected exactly 1 successful registration for identical username, but got " + successCount.get());
            assertEquals(threadCount - 1, failureCount.get(), "Expected " + (threadCount - 1) + " failed registrations");
            assertTrue(userRepository.findByUsername("raceTargetUser").isPresent());
        });
    }

    @Test
    @DisplayName("Adversarial: Duplicate login eviction thrashing over TCP")
    void testDuplicateLoginEvictionThrashingOverTcp() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            // Register target user
            RegisterRequestPacket reg = new RegisterRequestPacket();
            reg.setUsername("thrashTarget");
            reg.setPasswordHash("P@ssword999");
            reg.setNickname("ThrashTarget");
            reg.setEmail("thrash@pvz.com");
            reg.setGender("female");
            reg.setSecurityQuestionNumber(2);
            reg.setSecurityAnswer("ThrashAns");
            authService.register(reg);

            int iterations = 30;
            RawTestClient activeClient = null;

            try {
                for (int i = 0; i < iterations; i++) {
                    RawTestClient nextClient = new RawTestClient("127.0.0.1", serverPort, objectMapper);
                    nextClient.sendPacket(new LoginRequestPacket("thrashTarget", "P@ssword999", false));

                    Packet resp = nextClient.readPacket();
                    assertNotNull(resp);
                    assertInstanceOf(LoginResponsePacket.class, resp);
                    assertTrue(((LoginResponsePacket) resp).isSuccess());

                    if (activeClient != null) {
                        // Previous client should receive SESSION_REPLACED notification or EOF
                        try {
                            Packet evictedPkt = activeClient.readPacket();
                            if (evictedPkt != null) {
                                assertInstanceOf(ErrorMessagePacket.class, evictedPkt);
                                assertEquals("SESSION_REPLACED", ((ErrorMessagePacket) evictedPkt).getCode());
                            }
                        } catch (IOException ignored) {
                            // Socket closed by eviction is also acceptable
                        }
                        activeClient.close();
                    }

                    activeClient = nextClient;
                    assertTrue(authService.isUserLoggedIn("thrashTarget"));
                    assertEquals(1, authService.getOnlineCount());
                }
            } finally {
                if (activeClient != null) {
                    activeClient.close();
                }
            }
        });
    }

    @Test
    @DisplayName("Adversarial: Malformed JSON packet injections over TCP")
    void testMalformedJsonInjectionsOverTcp() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            try (RawTestClient client = new RawTestClient("127.0.0.1", serverPort, objectMapper)) {
                // 1. Send completely invalid JSON syntax
                client.sendRawLine("{ this is NOT json at all :::");
                Packet resp1 = client.readPacket();
                assertNotNull(resp1, "Server should reply to malformed JSON with ErrorMessagePacket");
                assertInstanceOf(ErrorMessagePacket.class, resp1);
                assertEquals("MALFORMED_JSON", ((ErrorMessagePacket) resp1).getCode());

                // 2. Send truncated JSON
                client.sendRawLine("{\"type\":\"REGISTER_REQUEST\", \"username\": \"half_pack");
                Packet resp2 = client.readPacket();
                assertNotNull(resp2);
                assertInstanceOf(ErrorMessagePacket.class, resp2);
                assertEquals("MALFORMED_JSON", ((ErrorMessagePacket) resp2).getCode());

                // 3. Send unknown packet subtype
                client.sendRawLine("{\"type\":\"UNRECOGNIZED_INJECTED_PACKET\",\"payload\":\"attack\"}");
                Packet resp3 = client.readPacket();
                assertNotNull(resp3);
                assertInstanceOf(ErrorMessagePacket.class, resp3);
                assertEquals("MALFORMED_JSON", ((ErrorMessagePacket) resp3).getCode());

                // 4. Send bad field type (e.g. string for integer field)
                client.sendRawLine("{\"type\":\"REGISTER_REQUEST\",\"securityQuestionNumber\":\"NOT_A_NUMBER\"}");
                Packet resp4 = client.readPacket();
                assertNotNull(resp4);
                assertInstanceOf(ErrorMessagePacket.class, resp4);
                assertEquals("MALFORMED_JSON", ((ErrorMessagePacket) resp4).getCode());

                // 5. Send empty blank lines (keep-alives) - server should ignore without crashing
                client.sendRawLine("   ");
                client.sendRawLine("");

                // 6. Verify channel is still healthy by issuing valid registration after all injections
                RegisterRequestPacket validReg = new RegisterRequestPacket();
                validReg.setUsername("resilientUser");
                validReg.setPasswordHash("StrongP@ss1");
                validReg.setNickname("Resilient");
                validReg.setEmail("resilient@pvz.com");
                validReg.setGender("male");
                validReg.setSecurityQuestionNumber(1);
                validReg.setSecurityAnswer("Answer");
                client.sendPacket(validReg);

                Packet resp5 = client.readPacket();
                assertNotNull(resp5);
                assertInstanceOf(RegisterResponsePacket.class, resp5);
                assertTrue(((RegisterResponsePacket) resp5).isSuccess());
            }
        });
    }

    @Test
    @DisplayName("Adversarial: Brute-force password login hammering over TCP")
    void testBruteForceLoginHammeringOverTcp() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            // Register target user
            RegisterRequestPacket reg = new RegisterRequestPacket();
            reg.setUsername("victimAccount");
            reg.setPasswordHash("CorrectPassword!1");
            reg.setNickname("Victim");
            reg.setEmail("victim@pvz.com");
            reg.setGender("female");
            reg.setSecurityQuestionNumber(4);
            reg.setSecurityAnswer("SecretCity");
            authService.register(reg);

            int workerThreads = 10;
            int attemptsPerWorker = 20;
            ExecutorService executor = Executors.newFixedThreadPool(workerThreads);
            CountDownLatch latch = new CountDownLatch(workerThreads);
            AtomicInteger failedResponses = new AtomicInteger(0);

            for (int w = 0; w < workerThreads; w++) {
                final int workerId = w;
                executor.submit(() -> {
                    try (RawTestClient client = new RawTestClient("127.0.0.1", serverPort, objectMapper)) {
                        for (int i = 0; i < attemptsPerWorker; i++) {
                            LoginRequestPacket badLogin = new LoginRequestPacket("victimAccount", "WrongPass_" + workerId + "_" + i, false);
                            client.sendPacket(badLogin);
                            Packet resp = client.readPacket();
                            if (resp instanceof LoginResponsePacket lr && !lr.isSuccess()) {
                                failedResponses.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Worker exception: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(workerThreads * attemptsPerWorker, failedResponses.get());
            assertFalse(authService.isUserLoggedIn("victimAccount"));

            // Legitimate login succeeds immediately
            try (RawTestClient legitClient = new RawTestClient("127.0.0.1", serverPort, objectMapper)) {
                legitClient.sendPacket(new LoginRequestPacket("victimAccount", "CorrectPassword!1", false));
                Packet legitResp = legitClient.readPacket();
                assertNotNull(legitResp);
                assertInstanceOf(LoginResponsePacket.class, legitResp);
                assertTrue(((LoginResponsePacket) legitResp).isSuccess());
                assertTrue(authService.isUserLoggedIn("victimAccount"));
            }
        });
    }

    @Test
    @DisplayName("Adversarial: Abrupt socket disconnect during in-flight operations")
    void testAbruptSocketDisconnectDuringInFlightOps() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            int abruptIterations = 25;

            for (int i = 0; i < abruptIterations; i++) {
                RawTestClient client = new RawTestClient("127.0.0.1", serverPort, objectMapper);
                RegisterRequestPacket req = new RegisterRequestPacket();
                req.setUsername("dropUser" + i);
                req.setPasswordHash("P@ssword123!");
                req.setNickname("DropUser" + i);
                req.setEmail("drop" + i + "@pvz.com");
                req.setGender("male");
                req.setSecurityQuestionNumber(1);
                req.setSecurityAnswer("Answer" + i);

                client.sendPacket(req);
                // Immediately abort TCP connection with RST without reading response
                client.abortConnection();
            }

            // Give background executor brief moment to process disconnect cleanup
            Thread.sleep(300);

            // Server must remain completely healthy and functional
            assertTrue(tcpServer.isRunning());
            try (RawTestClient healthyClient = new RawTestClient("127.0.0.1", serverPort, objectMapper)) {
                RegisterRequestPacket goodReq = new RegisterRequestPacket();
                goodReq.setUsername("afterShockUser");
                goodReq.setPasswordHash("ValidPass@123");
                goodReq.setNickname("AfterShock");
                goodReq.setEmail("aftershock@pvz.com");
                goodReq.setGender("male");
                goodReq.setSecurityQuestionNumber(2);
                goodReq.setSecurityAnswer("Safe");

                healthyClient.sendPacket(goodReq);
                Packet resp = healthyClient.readPacket();
                assertNotNull(resp);
                assertInstanceOf(RegisterResponsePacket.class, resp);
                assertTrue(((RegisterResponsePacket) resp).isSuccess());
            }
        });
    }

    @Test
    @DisplayName("Adversarial: Concurrent logins and logouts for distinct users")
    void testConcurrentDistinctUserLoginsAndLogouts() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            int userCount = 15;
            for (int i = 0; i < userCount; i++) {
                RegisterRequestPacket r = new RegisterRequestPacket();
                r.setUsername("distinctUser" + i);
                r.setPasswordHash("Password@123");
                r.setNickname("Distinct" + i);
                r.setEmail("distinct" + i + "@pvz.com");
                r.setGender("female");
                r.setSecurityQuestionNumber(1);
                r.setSecurityAnswer("DistinctAns");
                authService.register(r);
            }

            ExecutorService executor = Executors.newFixedThreadPool(userCount);
            CountDownLatch readyLatch = new CountDownLatch(userCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(userCount);
            AtomicInteger successfulLogins = new AtomicInteger(0);

            for (int i = 0; i < userCount; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try (RawTestClient client = new RawTestClient("127.0.0.1", serverPort, objectMapper)) {
                        readyLatch.countDown();
                        startLatch.await();

                        // 1. Login
                        client.sendPacket(new LoginRequestPacket("distinctUser" + idx, "Password@123", false));
                        Packet resp = client.readPacket();
                        if (resp instanceof LoginResponsePacket lr && lr.isSuccess()) {
                            successfulLogins.incrementAndGet();
                        }

                        // 2. Logout
                        client.sendPacket(new LogoutRequestPacket("distinctUser" + idx, null));
                        Thread.sleep(50);
                    } catch (Exception e) {
                        System.err.println("Distinct user error: " + e.getMessage());
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(userCount, successfulLogins.get());
            // Give unroute handlers a moment to finish
            Thread.sleep(200);
            assertEquals(0, authService.getOnlineCount());
        });
    }

    @Test
    @DisplayName("Adversarial: Null and malformed packet fields do not cause unhandled internal errors")
    void testNullAndBoundaryPacketFields() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            try (RawTestClient client = new RawTestClient("127.0.0.1", serverPort, objectMapper)) {
                // 1. Register with all null fields
                client.sendPacket(new RegisterRequestPacket());
                Packet p1 = client.readPacket();
                assertNotNull(p1);
                assertInstanceOf(RegisterResponsePacket.class, p1);
                assertFalse(((RegisterResponsePacket) p1).isSuccess());

                // 2. Login with null credentials
                client.sendPacket(new LoginRequestPacket(null, null, false));
                Packet p2 = client.readPacket();
                assertNotNull(p2);
                assertInstanceOf(LoginResponsePacket.class, p2);
                assertFalse(((LoginResponsePacket) p2).isSuccess());

                // 3. Register with invalid security question number (out of bounds)
                RegisterRequestPacket badQ = new RegisterRequestPacket();
                badQ.setUsername("badQuestionUser");
                badQ.setPasswordHash("Password@123");
                badQ.setNickname("BadQ");
                badQ.setEmail("badq@pvz.com");
                badQ.setGender("male");
                badQ.setSecurityQuestionNumber(99);
                badQ.setSecurityAnswer("Answer");
                client.sendPacket(badQ);

                Packet p3 = client.readPacket();
                assertNotNull(p3);
                assertInstanceOf(RegisterResponsePacket.class, p3);
                assertFalse(((RegisterResponsePacket) p3).isSuccess());
                assertTrue(((RegisterResponsePacket) p3).getMessage().contains("Question number"));
            }
        });
    }
}
