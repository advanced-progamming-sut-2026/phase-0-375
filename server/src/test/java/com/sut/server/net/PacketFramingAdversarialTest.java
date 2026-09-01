package com.sut.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.network.packet.Packet;
import model.network.packet.auth.LogoutRequestPacket;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.game.PlacePlantRequestPacket;
import model.network.packet.system.ErrorMessagePacket;
import model.network.packet.system.HeartbeatPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adversarial test suite challenging packet framing, malformed payloads,
 * streaming fragmentation, oversized inputs, unicode preservation, and error resilience.
 */
class PacketFramingAdversarialTest {

    private TcpServer server;
    private PacketRouter router;
    private ObjectMapper mapper;
    private int boundPort;

    @BeforeEach
    void setUp() throws Exception {
        router = new PacketRouter();
        mapper = TcpServer.createDefaultObjectMapper();
        server = new TcpServer("127.0.0.1", 0, router, mapper);
        server.start();
        boundPort = server.getBoundPort();
        assertTrue(boundPort > 0, "Server must bind to valid port");
    }

    @AfterEach
    void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 1: Malformed JSON syntax, non-objects, and missing types return MALFORMED_JSON without killing connection")
    void testMalformedJsonVariants() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String[] malformedPayloads = {
                "{\"type\": \"LOGIN_REQUEST\", \"username\":", // Incomplete JSON
                "{\"type\": \"HEARTBEAT\",,, }",               // Invalid syntax
                "This is completely raw text not JSON at all",  // Plain text
                "[1, 2, 3, 4, 5]",                             // JSON Array root
                "12345678",                                    // Number root
                "\"just a raw json string\"",                  // String root
                "true",                                        // Boolean root
                "{}",                                          // Empty object (missing type property)
                "{\"randomKey\": \"noTypeKey\"}"               // Object missing type
            };

            for (String payload : malformedPayloads) {
                writer.write(payload);
                writer.newLine();
                writer.flush();

                String line = reader.readLine();
                assertNotNull(line, "Server must return error message line for payload: " + payload);

                Packet errorPacket = mapper.readValue(line, Packet.class);
                assertTrue(errorPacket instanceof ErrorMessagePacket, "Expected ErrorMessagePacket for: " + payload);
                ErrorMessagePacket err = (ErrorMessagePacket) errorPacket;
                assertEquals("MALFORMED_JSON", err.getCode(), "Error code should be MALFORMED_JSON");
            }

            // Verify connection is still intact and handles valid packet
            HeartbeatPacket ping = new HeartbeatPacket(9999L);
            writer.write(mapper.writeValueAsString(ping));
            writer.newLine();
            writer.flush();

            String responseLine = reader.readLine();
            assertNotNull(responseLine);
            Packet response = mapper.readValue(responseLine, Packet.class);
            assertTrue(response instanceof HeartbeatPacket);
            assertEquals(9999L, ((HeartbeatPacket) response).getClientTimestamp());
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 2: Null JSON literal handling")
    void testNullJsonHandling() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // "null" parses to null in Jackson readValue("null", Packet.class)
            writer.write("null");
            writer.newLine();
            writer.flush();

            // Send immediate valid ping to verify server didn't block or crash
            HeartbeatPacket ping = new HeartbeatPacket(42L);
            writer.write(mapper.writeValueAsString(ping));
            writer.newLine();
            writer.flush();

            String responseLine = reader.readLine();
            assertNotNull(responseLine);
            Packet response = mapper.readValue(responseLine, Packet.class);
            assertTrue(response instanceof HeartbeatPacket);
            assertEquals(42L, ((HeartbeatPacket) response).getClientTimestamp());
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 3: Empty and whitespace-only lines are ignored without emitting errors")
    void testEmptyAndWhitespaceLines() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // Send multiple empty / whitespace lines
            writer.write("\n");
            writer.write("   \n");
            writer.write("\t\t  \t\n");
            writer.write("\r\n");
            writer.flush();

            // Followed by valid heartbeat
            HeartbeatPacket ping = new HeartbeatPacket(777L);
            writer.write(mapper.writeValueAsString(ping));
            writer.newLine();
            writer.flush();

            // The FIRST line received must be the heartbeat response, NOT an error
            String responseLine = reader.readLine();
            assertNotNull(responseLine);
            Packet response = mapper.readValue(responseLine, Packet.class);
            assertTrue(response instanceof HeartbeatPacket, "Empty lines should not produce error responses");
            assertEquals(777L, ((HeartbeatPacket) response).getClientTimestamp());
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 4: Fragmented byte-by-byte TCP streaming")
    void testFragmentedStreaming() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             OutputStream os = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            HeartbeatPacket ping = new HeartbeatPacket(123456L);
            byte[] bytes = (mapper.writeValueAsString(ping) + "\n").getBytes(StandardCharsets.UTF_8);

            // Write 1 byte at a time with tiny sleeps
            for (byte b : bytes) {
                os.write(b);
                os.flush();
                Thread.sleep(2);
            }

            String responseLine = reader.readLine();
            assertNotNull(responseLine);
            Packet response = mapper.readValue(responseLine, Packet.class);
            assertTrue(response instanceof HeartbeatPacket);
            assertEquals(123456L, ((HeartbeatPacket) response).getClientTimestamp());
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 5: Pipelined packets in a single TCP write buffer")
    void testPipelinedPacketsInSingleBuffer() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            int count = 5;
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < count; i++) {
                HeartbeatPacket ping = new HeartbeatPacket(1000L + i);
                buffer.append(mapper.writeValueAsString(ping)).append("\n");
            }

            // Write all 5 packets in one shot
            writer.write(buffer.toString());
            writer.flush();

            for (int i = 0; i < count; i++) {
                String line = reader.readLine();
                assertNotNull(line, "Should receive response for packet " + i);
                Packet res = mapper.readValue(line, Packet.class);
                assertTrue(res instanceof HeartbeatPacket);
                assertEquals(1000L + i, ((HeartbeatPacket) res).getClientTimestamp());
            }
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 6: Unknown packet type discriminator vs Unhandled packet type")
    void testUnknownAndUnhandledPacketTypes() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // Case A: Completely unknown discriminator tag
            String unknownTypeJson = "{\"type\": \"TOTALLY_UNKNOWN_PACKET\", \"dummy\": 123}";
            writer.write(unknownTypeJson);
            writer.newLine();
            writer.flush();

            String lineA = reader.readLine();
            assertNotNull(lineA);
            Packet errA = mapper.readValue(lineA, Packet.class);
            assertTrue(errA instanceof ErrorMessagePacket);
            assertEquals("MALFORMED_JSON", ((ErrorMessagePacket) errA).getCode());

            // Case B: Known packet type in schema (LogoutRequestPacket), but no handler registered in PacketRouter
            LogoutRequestPacket logout = new LogoutRequestPacket();
            writer.write(mapper.writeValueAsString(logout));
            writer.newLine();
            writer.flush();

            String lineB = reader.readLine();
            assertNotNull(lineB);
            Packet errB = mapper.readValue(lineB, Packet.class);
            assertTrue(errB instanceof ErrorMessagePacket);
            assertEquals("UNHANDLED_PACKET_TYPE", ((ErrorMessagePacket) errB).getCode());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 7: Oversized payload (1MB JSON packet) processing")
    void testOversizedPayloadHandling() throws Exception {
        router.registerHandler(ReactionPacket.class, (conn, packet) -> {
            // Echo back reaction with length confirmation
            conn.sendPacket(new ReactionPacket("SERVER", "TEXT", "Received bytes: " + packet.getContent().length()));
        });

        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // Construct 1MB payload string
            int payloadSize = 1024 * 1024; // 1 MB
            StringBuilder largeText = new StringBuilder(payloadSize);
            for (int i = 0; i < payloadSize; i++) {
                largeText.append((char) ('A' + (i % 26)));
            }

            ReactionPacket largePacket = new ReactionPacket("StressTester", "TEXT", largeText.toString());
            writer.write(mapper.writeValueAsString(largePacket));
            writer.newLine();
            writer.flush();

            String responseLine = reader.readLine();
            assertNotNull(responseLine, "Server should handle 1MB payload");
            Packet res = mapper.readValue(responseLine, Packet.class);
            assertTrue(res instanceof ReactionPacket);
            ReactionPacket confirmation = (ReactionPacket) res;
            assertEquals("Received bytes: " + payloadSize, confirmation.getContent());
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 8: Special Unicode, Emojis, RTL characters, and JSON escape sequences")
    void testUnicodeAndEscapeSequences() throws Exception {
        router.registerHandler(ReactionPacket.class, (conn, packet) -> {
            // Echo exact packet back
            conn.sendPacket(packet);
        });

        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            String complexUnicode = "🧟‍♂️ Sunflower 🌻 Zombie 🧟 Brains 🧠! Persian: کاربر ۱ سلام - Chinese: 植物大战僵尸 - Russian: Зомби - Math: ∀x∈ℝ, ∃y > x";
            ReactionPacket packet = new ReactionPacket("User_Unicode_Test", "EMOJI", complexUnicode);

            writer.write(mapper.writeValueAsString(packet));
            writer.newLine();
            writer.flush();

            String responseLine = reader.readLine();
            assertNotNull(responseLine);
            ReactionPacket echoed = (ReactionPacket) mapper.readValue(responseLine, Packet.class);
            assertEquals(complexUnicode, echoed.getContent());
            assertEquals("User_Unicode_Test", echoed.getSenderUsername());

            // Test escape sequences: newlines, quotes, backslashes, tabs inside string value
            String complexEscapes = "Line 1\nLine 2\tTabbed\r\n\"Quoted\" \\Backslash\\ /Slash/";
            ReactionPacket escapePacket = new ReactionPacket("User_Escape", "TEXT", complexEscapes);

            writer.write(mapper.writeValueAsString(escapePacket));
            writer.newLine();
            writer.flush();

            String escapeLine = reader.readLine();
            assertNotNull(escapeLine);
            ReactionPacket echoedEscape = (ReactionPacket) mapper.readValue(escapeLine, Packet.class);
            assertEquals(complexEscapes, echoedEscape.getContent());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 9: Fuzzing attack with random byte streams and corrupted payloads")
    void testFuzzingResistance() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             OutputStream os = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            Random random = new Random(42);
            int fuzzIterations = 50;

            for (int i = 0; i < fuzzIterations; i++) {
                // Generate random noisy string ending in \n
                int length = 10 + random.nextInt(200);
                byte[] randomBytes = new byte[length];
                for (int b = 0; b < length; b++) {
                    randomBytes[b] = (byte) (32 + random.nextInt(95)); // printable ASCII
                }

                os.write(randomBytes);
                os.write('\n');
                os.flush();

                // Each non-empty line must return MALFORMED_JSON ErrorMessagePacket
                String line = reader.readLine();
                assertNotNull(line, "Server must respond on fuzz line " + i);
                Packet p = mapper.readValue(line, Packet.class);
                assertTrue(p instanceof ErrorMessagePacket);
            }

            // Immediately send valid ping to confirm server is completely unharmed
            HeartbeatPacket ping = new HeartbeatPacket(8888L);
            byte[] validBytes = (mapper.writeValueAsString(ping) + "\n").getBytes(StandardCharsets.UTF_8);
            os.write(validBytes);
            os.flush();

            String finalLine = reader.readLine();
            assertNotNull(finalLine);
            Packet p = mapper.readValue(finalLine, Packet.class);
            assertTrue(p instanceof HeartbeatPacket);
            assertEquals(8888L, ((HeartbeatPacket) p).getClientTimestamp());
        }
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("AdvTest 10: Multi-threaded concurrent client hammer and abrupt socket drop")
    void testConcurrentClientHammerAndAbruptDrops() throws Exception {
        int threadCount = 10;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicBoolean serverFailed = new AtomicBoolean(false);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int op = 0; op < operationsPerThread; op++) {
                        try (Socket socket = new Socket("127.0.0.1", boundPort);
                             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                            if (op % 3 == 0) {
                                // Send malformed
                                writer.write("CORRUPTED_OP_" + op + "\n");
                                writer.flush();
                                String line = reader.readLine();
                                if (line != null && line.contains("MALFORMED_JSON")) {
                                    totalSuccess.incrementAndGet();
                                }
                            } else if (op % 3 == 1) {
                                // Send valid ping
                                HeartbeatPacket ping = new HeartbeatPacket(threadId * 1000L + op);
                                writer.write(mapper.writeValueAsString(ping) + "\n");
                                writer.flush();
                                String line = reader.readLine();
                                if (line != null && line.contains("HEARTBEAT")) {
                                    totalSuccess.incrementAndGet();
                                }
                            } else {
                                // Send half a packet and abruptly close
                                writer.write("{\"type\":\"HEARTBEAT");
                                writer.flush();
                                // socket closes immediately on try-with-resources exit
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS), "All client threads should finish");
        executor.shutdown();

        // Check server state
        assertTrue(server.isRunning(), "Server must remain running after hammer test");

        // Verify active connections drain down to 0
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (server.getActiveConnectionCount() == 0) break;
            Thread.sleep(50);
        }
        assertEquals(0, server.getActiveConnectionCount(), "All abruptly closed connections should be cleaned up");

        // Server can still accept new valid connection
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            HeartbeatPacket ping = new HeartbeatPacket(999999L);
            writer.write(mapper.writeValueAsString(ping) + "\n");
            writer.flush();

            String line = reader.readLine();
            assertNotNull(line);
            HeartbeatPacket pong = (HeartbeatPacket) mapper.readValue(line, Packet.class);
            assertEquals(999999L, pong.getClientTimestamp());
        }
    }
}
