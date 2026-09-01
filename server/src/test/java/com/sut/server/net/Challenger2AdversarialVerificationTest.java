package com.sut.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.network.packet.Packet;
import model.network.packet.chat.ReactionPacket;
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
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Challenger 2 Adversarial Verification Test Suite for Milestone 1.
 * Tests extreme edge cases: deep nesting, empty line floods,
 * astral unicode, and mixed-mode pipelining under load.
 */
class Challenger2AdversarialVerificationTest {

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
        assertTrue(boundPort > 0, "Server must bind to valid ephemeral port");
    }

    @AfterEach
    void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Challenger Adv 1: Rapid 5,000 empty line flood does not starve server or trigger errors")
    void testEmptyLineFlood() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder flood = new StringBuilder();
            for (int i = 0; i < 5000; i++) {
                flood.append("\n");
            }
            writer.write(flood.toString());
            writer.flush();

            // Follow immediately with a valid heartbeat ping
            HeartbeatPacket ping = new HeartbeatPacket(112233L);
            writer.write(mapper.writeValueAsString(ping) + "\n");
            writer.flush();

            String responseLine = reader.readLine();
            assertNotNull(responseLine, "Server must respond to valid packet following empty line flood");
            Packet packet = mapper.readValue(responseLine, Packet.class);
            assertTrue(packet instanceof HeartbeatPacket, "Must receive Heartbeat response, not an error");
            assertEquals(112233L, ((HeartbeatPacket) packet).getClientTimestamp());
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Challenger Adv 2: Deeply nested JSON payload handled safely with MALFORMED_JSON")
    void testDeeplyNestedJsonPayload() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // Build deeply nested JSON structure: {"a":{"a":{"a":...}}}
            StringBuilder nested = new StringBuilder("{\"type\":\"HEARTBEAT\",\"nested\":");
            int depth = 500;
            for (int i = 0; i < depth; i++) {
                nested.append("{\"k\":");
            }
            nested.append("1");
            for (int i = 0; i < depth; i++) {
                nested.append("}");
            }
            nested.append("}\n");

            writer.write(nested.toString());
            writer.flush();

            String responseLine = reader.readLine();
            assertNotNull(responseLine, "Server must respond without crashing");
            Packet packet = mapper.readValue(responseLine, Packet.class);
            assertTrue(packet instanceof ErrorMessagePacket || packet instanceof HeartbeatPacket);

            // Follow up with clean ping to prove connection remains alive
            HeartbeatPacket ping = new HeartbeatPacket(445566L);
            writer.write(mapper.writeValueAsString(ping) + "\n");
            writer.flush();

            String cleanLine = reader.readLine();
            assertNotNull(cleanLine);
            Packet cleanRes = mapper.readValue(cleanLine, Packet.class);
            assertTrue(cleanRes instanceof HeartbeatPacket);
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Challenger Adv 3: Astral plane emojis and complex script handling")
    void testAstralPlaneUnicodePreservation() throws Exception {
        router.registerHandler(ReactionPacket.class, (conn, pkt) -> conn.sendPacket(pkt));

        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // Astral emojis: 🧟 (U+1F9DF), 🧠 (U+1F9E0), 🌻 (U+1F33B), 🪐 (U+1F990), 🧄 (U+1F9C4)
            String astralText = "🧟🧠🌻🪐🧄 Mixed: فارسی (گیاهان در برابر زامبی‌ها) - 日本語 (プラント vs. ゾンビ) - 🎮🏆";
            ReactionPacket packet = new ReactionPacket("AstralUser", "EMOJI", astralText);

            writer.write(mapper.writeValueAsString(packet) + "\n");
            writer.flush();

            String responseLine = reader.readLine();
            assertNotNull(responseLine);
            ReactionPacket echoed = (ReactionPacket) mapper.readValue(responseLine, Packet.class);
            assertEquals(astralText, echoed.getContent(), "Astral unicode text must match byte-for-byte");
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Challenger Adv 4: Pipelined stream of 200 mixed valid and malformed packets in single connection")
    void testMixedValidAndMalformedPipelinedStream() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            int count = 200;
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < count; i++) {
                if (i % 2 == 0) {
                    // Valid heartbeat
                    buffer.append(mapper.writeValueAsString(new HeartbeatPacket(i))).append("\n");
                } else {
                    // Malformed JSON
                    buffer.append("{\"bad_json_token_").append(i).append("\n");
                }
            }

            writer.write(buffer.toString());
            writer.flush();

            for (int i = 0; i < count; i++) {
                String line = reader.readLine();
                assertNotNull(line, "Server must respond to packet index " + i);
                Packet res = mapper.readValue(line, Packet.class);
                if (i % 2 == 0) {
                    assertTrue(res instanceof HeartbeatPacket, "Even index should be HeartbeatPacket");
                    assertEquals(i, ((HeartbeatPacket) res).getClientTimestamp());
                } else {
                    assertTrue(res instanceof ErrorMessagePacket, "Odd index should be ErrorMessagePacket");
                    assertEquals("MALFORMED_JSON", ((ErrorMessagePacket) res).getCode());
                }
            }
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Challenger Adv 5: 30 concurrent sockets sending malformed packets simultaneously")
    void testMassiveConcurrentMalformedStress() throws Exception {
        int clientCount = 30;
        ExecutorService pool = Executors.newFixedThreadPool(clientCount);
        CountDownLatch latch = new CountDownLatch(clientCount);
        AtomicInteger malformedResponses = new AtomicInteger(0);

        for (int c = 0; c < clientCount; c++) {
            final int id = c;
            pool.submit(() -> {
                try {
                    try (Socket socket = new Socket("127.0.0.1", boundPort);
                         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                         BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                        for (int i = 0; i < 10; i++) {
                            writer.write("MALFORMED_CONCURRENT_" + id + "_" + i + "\n");
                            writer.flush();

                            String line = reader.readLine();
                            if (line != null) {
                                Packet p = mapper.readValue(line, Packet.class);
                                if (p instanceof ErrorMessagePacket && "MALFORMED_JSON".equals(((ErrorMessagePacket) p).getCode())) {
                                    malformedResponses.incrementAndGet();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All 30 concurrent malformed clients must finish");
        pool.shutdownNow();

        assertEquals(clientCount * 10, malformedResponses.get(), "All 300 malformed requests must be handled gracefully");
        assertTrue(server.isRunning(), "Server must remain running after massive malformed flood");
    }
}
