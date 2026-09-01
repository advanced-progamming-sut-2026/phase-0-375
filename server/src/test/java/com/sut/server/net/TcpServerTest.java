package com.sut.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.network.packet.Packet;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TcpServerTest {

    private TcpServer server;
    private PacketRouter router;
    private ObjectMapper mapper;
    private int boundPort;

    @BeforeEach
    void setUp() throws Exception {
        router = new PacketRouter();
        mapper = TcpServer.createDefaultObjectMapper();
        // Port 0 instructs OS to allocate an available ephemeral port
        server = new TcpServer("127.0.0.1", 0, router, mapper);
        server.start();
        boundPort = server.getBoundPort();
        assertTrue(boundPort > 0, "Server should be bound to a valid dynamic port");
        assertTrue(server.isRunning(), "Server should be running");
    }

    @AfterEach
    void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    @Test
    @DisplayName("Test 1: Server startup, port binding, and clean shutdown")
    void testServerStartupAndShutdown() {
        assertTrue(server.isRunning());
        assertEquals(0, server.getActiveConnectionCount());

        server.stop();
        assertFalse(server.isRunning());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Test 2: Single client connection tracking and disconnect cleanup")
    void testClientConnectionAndDisconnect() throws Exception {
        assertEquals(0, server.getActiveConnectionCount());

        try (Socket clientSocket = new Socket("127.0.0.1", boundPort)) {
            // Wait for server to register connection
            assertEventually(() -> server.getActiveConnectionCount() == 1, 1000);
            assertEquals(1, server.getActiveConnectionCount());
        }

        // After socket close, server should unregister connection
        assertEventually(() -> server.getActiveConnectionCount() == 0, 2000);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Test 3: Bidirectional packet roundtrip (Heartbeat echo)")
    void testBidirectionalPacketRoundtrip() throws Exception {
        try (Socket clientSocket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {

            long sendTime = System.currentTimeMillis();
            HeartbeatPacket ping = new HeartbeatPacket(sendTime);
            String json = mapper.writeValueAsString(ping);

            writer.write(json);
            writer.newLine();
            writer.flush();

            String responseLine = reader.readLine();
            assertNotNull(responseLine, "Server should respond with a line");

            Packet responsePacket = mapper.readValue(responseLine, Packet.class);
            assertTrue(responsePacket instanceof HeartbeatPacket, "Response should be HeartbeatPacket");
            HeartbeatPacket pong = (HeartbeatPacket) responsePacket;
            assertEquals(sendTime, pong.getClientTimestamp(), "Client timestamp should match original ping");
            assertTrue(pong.isPong(), "Response should have pong=true");
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Test 4: Polymorphic packet routing (LoginRequest -> LoginResponse)")
    void testPolymorphicPacketRouting() throws Exception {
        CountDownLatch handlerLatch = new CountDownLatch(1);
        AtomicReference<LoginRequestPacket> receivedPacket = new AtomicReference<>();

        router.registerHandler(LoginRequestPacket.class, (conn, packet) -> {
            receivedPacket.set(packet);
            conn.setUsername(packet.getUsername());
            conn.sendPacket(new LoginResponsePacket(true, "Login successful", null));
            handlerLatch.countDown();
        });

        try (Socket clientSocket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {

            LoginRequestPacket req = new LoginRequestPacket("pvz_master", "hashed_pw", false);
            writer.write(mapper.writeValueAsString(req));
            writer.newLine();
            writer.flush();

            assertTrue(handlerLatch.await(2, TimeUnit.SECONDS), "Handler should be executed");
            assertNotNull(receivedPacket.get());
            assertEquals("pvz_master", receivedPacket.get().getUsername());

            String resLine = reader.readLine();
            assertNotNull(resLine);
            Packet resPacket = mapper.readValue(resLine, Packet.class);
            assertTrue(resPacket instanceof LoginResponsePacket);
            LoginResponsePacket loginRes = (LoginResponsePacket) resPacket;
            assertTrue(loginRes.isSuccess());
            assertEquals("Login successful", loginRes.getMessage());
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Test 5: Multi-client broadcasting")
    void testMultiClientBroadcast() throws Exception {
        int clientCount = 3;
        List<Socket> sockets = new ArrayList<>();
        List<BufferedReader> readers = new ArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            Socket s = new Socket("127.0.0.1", boundPort);
            sockets.add(s);
            readers.add(new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)));
        }

        assertEventually(() -> server.getActiveConnectionCount() == clientCount, 1500);

        ReactionPacket broadcastPacket = new ReactionPacket("ServerAdmin", "EMOJI", "THUMBS_UP");
        server.broadcast(broadcastPacket);

        for (BufferedReader r : readers) {
            String line = r.readLine();
            assertNotNull(line, "Each client should receive broadcast line");
            Packet p = mapper.readValue(line, Packet.class);
            assertTrue(p instanceof ReactionPacket);
            ReactionPacket reaction = (ReactionPacket) p;
            assertEquals("ServerAdmin", reaction.getSenderUsername());
            assertEquals("THUMBS_UP", reaction.getContent());
        }

        for (Socket s : sockets) {
            s.close();
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Test 6: Malformed JSON handling does not crash server")
    void testMalformedJsonHandling() throws Exception {
        try (Socket clientSocket = new Socket("127.0.0.1", boundPort);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {

            // Send invalid non-JSON string
            writer.write("INVALID_NON_JSON_LINE");
            writer.newLine();
            writer.flush();

            String errorLine = reader.readLine();
            assertNotNull(errorLine);
            Packet errorPacket = mapper.readValue(errorLine, Packet.class);
            assertTrue(errorPacket instanceof ErrorMessagePacket);
            ErrorMessagePacket err = (ErrorMessagePacket) errorPacket;
            assertEquals("MALFORMED_JSON", err.getCode());

            // Verify connection remains functional for subsequent valid packet
            HeartbeatPacket ping = new HeartbeatPacket(System.currentTimeMillis());
            writer.write(mapper.writeValueAsString(ping));
            writer.newLine();
            writer.flush();

            String validLine = reader.readLine();
            assertNotNull(validLine);
            assertTrue(mapper.readValue(validLine, Packet.class) instanceof HeartbeatPacket);
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Test 7: Server shutdown cleanly disconnects all active clients")
    void testServerShutdownDisconnectsClients() throws Exception {
        Socket client1 = new Socket("127.0.0.1", boundPort);
        Socket client2 = new Socket("127.0.0.1", boundPort);
        BufferedReader r1 = new BufferedReader(new InputStreamReader(client1.getInputStream(), StandardCharsets.UTF_8));

        assertEventually(() -> server.getActiveConnectionCount() == 2, 1000);

        server.stop();

        // Reading after server stop should return EOF (null)
        String line = r1.readLine();
        assertNull(line, "Client should receive EOF when server stops");

        client1.close();
        client2.close();
    }

    private void assertEventually(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(20);
        }
        assertTrue(condition.getAsBoolean(), "Condition not satisfied within " + timeoutMillis + "ms");
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
