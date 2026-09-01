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

import static org.junit.jupiter.api.Assertions.*;

class AuthIntegrationTest {

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
        Path storagePath = tempDir.resolve("e2e-users.json");
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

    private static class TestClient implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final ObjectMapper mapper;

        public TestClient(String host, int port, ObjectMapper mapper) throws IOException {
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(5000);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.mapper = mapper;
        }

        public void sendPacket(Packet packet) throws IOException {
            String json = mapper.writeValueAsString(packet);
            writer.write(json);
            writer.newLine();
            writer.flush();
        }

        public Packet readPacket() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            return mapper.readValue(line, Packet.class);
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    @Test
    @DisplayName("End-to-End TCP: Register -> Login -> Session Verified -> Logout")
    void testEndToEndAuthFlow() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            try (TestClient client = new TestClient("127.0.0.1", serverPort, objectMapper)) {
                // 1. Send Register Request
                RegisterRequestPacket regReq = new RegisterRequestPacket();
                regReq.setUsername("tcpPlayer");
                regReq.setPasswordHash("SecureP@ssword1!");
                regReq.setNickname("TcpPlayerOne");
                regReq.setEmail("tcp@pvz.com");
                regReq.setGender("male");
                regReq.setSecurityQuestionNumber(2);
                regReq.setSecurityAnswer("Fluffy");

                client.sendPacket(regReq);

                Packet resp1 = client.readPacket();
                assertNotNull(resp1);
                assertInstanceOf(RegisterResponsePacket.class, resp1);
                RegisterResponsePacket regResp = (RegisterResponsePacket) resp1;
                assertTrue(regResp.isSuccess());
                assertEquals("Registration successful.", regResp.getMessage());

                // 2. Send Login Request
                LoginRequestPacket loginReq = new LoginRequestPacket("tcpPlayer", "SecureP@ssword1!", false);
                client.sendPacket(loginReq);

                Packet resp2 = client.readPacket();
                assertNotNull(resp2);
                assertInstanceOf(LoginResponsePacket.class, resp2);
                LoginResponsePacket loginResp = (LoginResponsePacket) resp2;
                assertTrue(loginResp.isSuccess());
                assertNotNull(loginResp.getUserProfile());
                assertEquals("tcpPlayer", loginResp.getUserProfile().getUsername());
                assertEquals("TcpPlayerOne", loginResp.getUserProfile().getNickname());

                // 3. Verify server-side session state
                assertTrue(authService.isUserLoggedIn("tcpPlayer"));
                assertEquals(1, authService.getOnlineCount());

                // 4. Send Logout Request
                LogoutRequestPacket logoutReq = new LogoutRequestPacket("tcpPlayer", null);
                client.sendPacket(logoutReq);

                // Short sleep to allow server to process unroute
                Thread.sleep(100);
                assertFalse(authService.isUserLoggedIn("tcpPlayer"));
                assertEquals(0, authService.getOnlineCount());
            }
        });
    }

    @Test
    @DisplayName("End-to-End TCP: Duplicate login from new client evicts previous connection")
    void testDuplicateLoginEviction() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            // First register account
            RegisterRequestPacket regReq = new RegisterRequestPacket();
            regReq.setUsername("multiSessionUser");
            regReq.setPasswordHash("Password@123");
            regReq.setNickname("MultiSession");
            regReq.setEmail("multi@pvz.com");
            regReq.setGender("female");
            regReq.setSecurityQuestionNumber(3);
            regReq.setSecurityAnswer("Tehran");
            authService.register(regReq);

            // Client 1 connects and logs in
            TestClient client1 = new TestClient("127.0.0.1", serverPort, objectMapper);
            client1.sendPacket(new LoginRequestPacket("multiSessionUser", "Password@123", false));
            Packet p1 = client1.readPacket();
            assertInstanceOf(LoginResponsePacket.class, p1);
            assertTrue(((LoginResponsePacket) p1).isSuccess());
            assertTrue(authService.isUserLoggedIn("multiSessionUser"));

            // Client 2 connects and logs in with same credentials
            try (TestClient client2 = new TestClient("127.0.0.1", serverPort, objectMapper)) {
                client2.sendPacket(new LoginRequestPacket("multiSessionUser", "Password@123", false));
                Packet p2 = client2.readPacket();
                assertInstanceOf(LoginResponsePacket.class, p2);
                assertTrue(((LoginResponsePacket) p2).isSuccess());

                // Client 1 should receive SESSION_REPLACED error and be disconnected
                Packet kickedPacket = client1.readPacket();
                assertNotNull(kickedPacket);
                assertInstanceOf(ErrorMessagePacket.class, kickedPacket);
                assertEquals("SESSION_REPLACED", ((ErrorMessagePacket) kickedPacket).getCode());

                client1.close();
            }
        });
    }

    @Test
    @DisplayName("End-to-End TCP: Invalid login credentials returns failure response")
    void testInvalidLoginOverTcp() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            try (TestClient client = new TestClient("127.0.0.1", serverPort, objectMapper)) {
                LoginRequestPacket loginReq = new LoginRequestPacket("nonExistent", "BadPass123!", false);
                client.sendPacket(loginReq);

                Packet resp = client.readPacket();
                assertNotNull(resp);
                assertInstanceOf(LoginResponsePacket.class, resp);
                LoginResponsePacket loginResp = (LoginResponsePacket) resp;
                assertFalse(loginResp.isSuccess());
                assertNull(loginResp.getUserProfile());
            }
        });
    }
}
