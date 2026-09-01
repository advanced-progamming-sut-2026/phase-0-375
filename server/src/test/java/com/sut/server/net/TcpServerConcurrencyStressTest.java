package com.sut.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.network.dto.PlantSnapshotDto;
import model.network.dto.ProjectileSnapshotDto;
import model.network.dto.ZombieSnapshotDto;
import model.network.packet.Packet;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.game.GameStateSnapshotPacket;
import model.network.packet.game.PlacePlantRequestPacket;
import model.network.packet.game.PlaceZombieRequestPacket;
import model.network.packet.game.PlayerActionResponsePacket;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TcpServerConcurrencyStressTest {

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
        assertTrue(boundPort > 0, "Server must bind to dynamic port");
        assertTrue(server.isRunning(), "Server must be running");
    }

    @AfterEach
    void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 1: 20 concurrent clients sending 2,000 rapid packets with simultaneous server broadcasts")
    void testHighConcurrency20ClientsWithContinuousTrafficAndBroadcasting() throws Exception {
        int clientCount = 20;
        int packetsPerClient = 100;
        int totalClientPackets = clientCount * packetsPerClient;

        AtomicInteger processedPackets = new AtomicInteger(0);

        // Register handlers for various packet types
        router.registerHandler(PlacePlantRequestPacket.class, (conn, pkt) -> {
            processedPackets.incrementAndGet();
            conn.sendPacket(new PlayerActionResponsePacket(true, "PLANT_PLACED", "PLANT"));
        });

        router.registerHandler(PlaceZombieRequestPacket.class, (conn, pkt) -> {
            processedPackets.incrementAndGet();
            conn.sendPacket(new PlayerActionResponsePacket(true, "ZOMBIE_PLACED", "ZOMBIE"));
        });

        router.registerHandler(LoginRequestPacket.class, (conn, pkt) -> {
            processedPackets.incrementAndGet();
            conn.sendPacket(new LoginResponsePacket(true, "Welcome " + pkt.getUsername(), null));
        });

        router.registerHandler(ReactionPacket.class, (conn, pkt) -> {
            processedPackets.incrementAndGet();
            conn.sendPacket(new ReactionPacket("SERVER_ECHO", pkt.getReactionType(), pkt.getContent()));
        });

        ExecutorService clientPool = Executors.newFixedThreadPool(clientCount * 2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(clientCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        AtomicBoolean keepBroadcasting = new AtomicBoolean(true);

        // Start 2 concurrent server broadcast threads
        Thread broadcaster1 = new Thread(() -> {
            int seq = 0;
            while (keepBroadcasting.get()) {
                server.broadcast(new ReactionPacket("SYSTEM_BROADCAST", "TEXT", "ANNOUNCEMENT_" + seq++));
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "Server-Broadcaster-1");

        Thread broadcaster2 = new Thread(() -> {
            int tick = 0;
            while (keepBroadcasting.get()) {
                GameStateSnapshotPacket snapshot = createSampleSnapshot(tick++);
                server.broadcast(snapshot);
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "Server-Broadcaster-2");

        broadcaster1.start();
        broadcaster2.start();

        List<Socket> activeSockets = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            clientPool.submit(() -> {
                try {
                    Socket socket = new Socket("127.0.0.1", boundPort);
                    socket.setTcpNoDelay(true);
                    activeSockets.add(socket);

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                    CountDownLatch clientSendDone = new CountDownLatch(1);
                    AtomicInteger clientReceivedResponses = new AtomicInteger(0);

                    // Client receiver thread
                    Future<?> receiverFuture = clientPool.submit(() -> {
                        try {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (line.isEmpty()) continue;

                                // Parse every line with Jackson polymorphic parser to verify ZERO interleaving/corruption
                                Packet packet = mapper.readValue(line, Packet.class);
                                assertNotNull(packet, "Parsed packet must not be null");

                                if (packet instanceof PlayerActionResponsePacket ||
                                    packet instanceof LoginResponsePacket ||
                                    (packet instanceof ReactionPacket && "SERVER_ECHO".equals(((ReactionPacket) packet).getSenderUsername())) ||
                                    (packet instanceof HeartbeatPacket && ((HeartbeatPacket) packet).isPong())) {
                                    clientReceivedResponses.incrementAndGet();
                                }

                                if (clientReceivedResponses.get() >= packetsPerClient) {
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            errors.add(new AssertionError("Client " + clientId + " parse error: " + e.getMessage(), e));
                        }
                    });

                    startLatch.await();

                    // Send rapid packets
                    for (int p = 0; p < packetsPerClient; p++) {
                        Packet pkt;
                        int mod = p % 4;
                        if (mod == 0) {
                            pkt = new PlacePlantRequestPacket("PEASHOOTER", p % 5, p % 9);
                        } else if (mod == 1) {
                            pkt = new PlaceZombieRequestPacket("CONEHEAD_ZOMBIE", p % 5, 8);
                        } else if (mod == 2) {
                            pkt = new LoginRequestPacket("user_" + clientId + "_" + p, "hash", false);
                        } else {
                            pkt = new ReactionPacket("user_" + clientId, "EMOJI", "THUMBS_UP");
                        }

                        String json = mapper.writeValueAsString(pkt);
                        writer.write(json);
                        writer.newLine();
                        writer.flush();
                    }

                    clientSendDone.countDown();
                    receiverFuture.get(6, TimeUnit.SECONDS);

                    socket.close();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Trigger all clients to send simultaneously
        startLatch.countDown();
        boolean completedInTime = finishLatch.await(10, TimeUnit.SECONDS);

        // Stop broadcasters
        keepBroadcasting.set(false);
        broadcaster1.join(1000);
        broadcaster2.join(1000);

        clientPool.shutdownNow();

        // Close any remaining open sockets
        for (Socket s : activeSockets) {
            try { s.close(); } catch (Exception ignored) {}
        }

        // Verify zero errors and zero JSON corruption
        assertTrue(completedInTime, "All 20 clients should finish packet exchange within timeout");
        assertTrue(errors.isEmpty(), "Zero errors expected during high concurrency, but found: " + errors);
        assertEquals(totalClientPackets, processedPackets.get(), "Server must process all 2,000 client packets");

        // Verify server connection registry returns to 0
        assertEventually(() -> server.getActiveConnectionCount() == 0, 3000);
        assertEquals(0, server.getActiveConnectionCount(), "Server connection tracking must return to 0 after all clients disconnect");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 2: Connection Lifecycle Storm (50 rapid connect/disconnect cycles)")
    void testRapidConnectionLifecycleStorm50Clients() throws Exception {
        int clientCount = 50;
        AtomicInteger closedCallbackCount = new AtomicInteger(0);

        router.addConnectionClosedListener(conn -> closedCallbackCount.incrementAndGet());

        ExecutorService pool = Executors.newFixedThreadPool(15);
        CountDownLatch doneLatch = new CountDownLatch(clientCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < clientCount; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    try (Socket s = new Socket("127.0.0.1", boundPort);
                         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
                         BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {

                        HeartbeatPacket ping = new HeartbeatPacket(System.currentTimeMillis());
                        writer.write(mapper.writeValueAsString(ping));
                        writer.newLine();
                        writer.flush();

                        String pongLine = reader.readLine();
                        assertNotNull(pongLine, "Client " + id + " should receive pong line");
                        Packet pkt = mapper.readValue(pongLine, Packet.class);
                        assertTrue(pkt instanceof HeartbeatPacket, "Must receive HeartbeatPacket pong");
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(doneLatch.await(8, TimeUnit.SECONDS), "All 50 client lifecycle connections must finish in time");
        pool.shutdownNow();

        assertTrue(errors.isEmpty(), "Zero errors expected in connection storm: " + errors);

        // Verify active connections count and closed callbacks
        assertEventually(() -> server.getActiveConnectionCount() == 0, 3000);
        assertEquals(0, server.getActiveConnectionCount(), "Active connections must be 0 after all 50 sockets closed");
        assertEventually(() -> closedCallbackCount.get() == clientCount, 3000);
        assertEquals(clientCount, closedCallbackCount.get(), "Closed callback should be triggered for every connection");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 3: Multi-Threaded Broadcast Storm (Zero Line Interleaving across 10 broadcaster threads)")
    void testMultiThreadedBroadcastStormZeroLineInterleaving() throws Exception {
        int clientCount = 10;
        int broadcasterThreadCount = 10;
        int broadcastsPerThread = 50;
        int totalExpectedBroadcasts = broadcasterThreadCount * broadcastsPerThread;

        List<Socket> clientSockets = new ArrayList<>();
        List<BufferedReader> readers = new ArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            Socket s = new Socket("127.0.0.1", boundPort);
            s.setTcpNoDelay(true);
            clientSockets.add(s);
            readers.add(new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)));
        }

        assertEventually(() -> server.getActiveConnectionCount() == clientCount, 2000);

        ExecutorService broadcastExecutor = Executors.newFixedThreadPool(broadcasterThreadCount);
        CountDownLatch startBroadcastLatch = new CountDownLatch(1);
        CountDownLatch finishBroadcastLatch = new CountDownLatch(broadcasterThreadCount);

        for (int b = 0; b < broadcasterThreadCount; b++) {
            final int bId = b;
            broadcastExecutor.submit(() -> {
                try {
                    startBroadcastLatch.await();
                    for (int i = 0; i < broadcastsPerThread; i++) {
                        ReactionPacket reaction = new ReactionPacket("BROADCASTER_" + bId, "TAUNT", "MSG_" + i + "_" + UUID.randomUUID());
                        server.broadcast(reaction);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishBroadcastLatch.countDown();
                }
            });
        }

        // Start broadcasts concurrently
        startBroadcastLatch.countDown();
        assertTrue(finishBroadcastLatch.await(5, TimeUnit.SECONDS), "All broadcast threads should finish sending");
        broadcastExecutor.shutdown();

        // Read and verify all packets on all 10 clients
        for (int c = 0; c < clientCount; c++) {
            BufferedReader r = readers.get(c);
            int received = 0;
            while (received < totalExpectedBroadcasts) {
                String line = r.readLine();
                assertNotNull(line, "Client " + c + " stream ended prematurely after " + received + " packets");
                line = line.trim();
                if (line.isEmpty()) continue;

                // Test Jackson NDJSON parsing on every line
                Packet p = mapper.readValue(line, Packet.class);
                assertTrue(p instanceof ReactionPacket, "Received packet must be ReactionPacket");
                ReactionPacket rp = (ReactionPacket) p;
                assertTrue(rp.getSenderUsername().startsWith("BROADCASTER_"), "Packet sender must match broadcaster");
                received++;
            }
            assertEquals(totalExpectedBroadcasts, received, "Client " + c + " should receive exact broadcast count");
        }

        for (Socket s : clientSockets) {
            s.close();
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 4: Graceful Shutdown Under Heavy I/O Load")
    void testGracefulShutdownUnderHeavyIOLoad() throws Exception {
        int clientCount = 15;
        List<Socket> sockets = new ArrayList<>();
        List<BufferedWriter> writers = new ArrayList<>();
        List<BufferedReader> readers = new ArrayList<>();
        AtomicBoolean stopClientTraffic = new AtomicBoolean(false);

        router.registerHandler(HeartbeatPacket.class, (conn, pkt) -> {
            conn.sendPacket(new HeartbeatPacket(pkt.getClientTimestamp(), System.currentTimeMillis(), true));
        });

        for (int i = 0; i < clientCount; i++) {
            Socket s = new Socket("127.0.0.1", boundPort);
            s.setTcpNoDelay(true);
            sockets.add(s);
            writers.add(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8)));
            readers.add(new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)));
        }

        assertEventually(() -> server.getActiveConnectionCount() == clientCount, 2000);

        ExecutorService clientTrafficPool = Executors.newFixedThreadPool(clientCount);
        for (int i = 0; i < clientCount; i++) {
            final int id = i;
            clientTrafficPool.submit(() -> {
                BufferedWriter w = writers.get(id);
                try {
                    while (!stopClientTraffic.get()) {
                        HeartbeatPacket ping = new HeartbeatPacket(System.currentTimeMillis());
                        w.write(mapper.writeValueAsString(ping));
                        w.newLine();
                        w.flush();
                        Thread.sleep(5);
                    }
                } catch (Exception ignored) {
                    // Socket closed on shutdown
                }
            });
        }

        // Allow traffic to run intensely for 100ms
        Thread.sleep(100);

        // Initiate server stop while full traffic is ongoing
        long shutdownStart = System.currentTimeMillis();
        server.stop();
        long shutdownDuration = System.currentTimeMillis() - shutdownStart;

        assertTrue(shutdownDuration < 3000, "Server shutdown must complete within 3 seconds under load (took " + shutdownDuration + "ms)");
        assertFalse(server.isRunning(), "Server must report isRunning == false");
        assertEquals(0, server.getActiveConnectionCount(), "Active connections must be cleared on server stop");

        stopClientTraffic.set(true);
        clientTrafficPool.shutdownNow();

        // Clients reading from disconnected sockets should receive EOF (null)
        for (BufferedReader r : readers) {
            try {
                String line = r.readLine();
                // When disconnected, readLine should return null or socket is closed
                // If a line was in flight before socket closed, verify it's valid
                while (line != null) {
                    Packet p = mapper.readValue(line, Packet.class);
                    assertNotNull(p);
                    line = r.readLine();
                }
            } catch (Exception ignored) {
                // SocketException or IOException is expected upon abrupt connection close
            }
        }

        for (Socket s : sockets) {
            try { s.close(); } catch (Exception ignored) {}
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 5: Large Payload Burst Transmission (>50KB per packet)")
    void testHugePayloadHandlingWithoutBufferCorruption() throws Exception {
        int clientCount = 5;
        int packetCount = 20;

        List<Socket> sockets = new ArrayList<>();
        List<BufferedReader> readers = new ArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            Socket s = new Socket("127.0.0.1", boundPort);
            s.setTcpNoDelay(true);
            sockets.add(s);
            readers.add(new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)));
        }

        assertEventually(() -> server.getActiveConnectionCount() == clientCount, 2000);

        ExecutorService readerPool = Executors.newFixedThreadPool(clientCount);
        CountDownLatch readCompleteLatch = new CountDownLatch(clientCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        // Spawn concurrent readers to prevent TCP socket receive buffer stalling
        for (int c = 0; c < clientCount; c++) {
            final int clientId = c;
            final BufferedReader r = readers.get(c);
            readerPool.submit(() -> {
                try {
                    for (int i = 0; i < packetCount; i++) {
                        String line = r.readLine();
                        assertNotNull(line, "Client " + clientId + " large packet line should not be null at idx " + i);
                        Packet packet = mapper.readValue(line, Packet.class);
                        assertTrue(packet instanceof GameStateSnapshotPacket, "Packet must be GameStateSnapshotPacket");
                        GameStateSnapshotPacket snap = (GameStateSnapshotPacket) packet;
                        assertEquals(i, snap.getTick());
                        assertEquals(50, snap.getPlants().size(), "Must contain 50 plant DTOs");
                        assertEquals(50, snap.getZombies().size(), "Must contain 50 zombie DTOs");
                        assertEquals(100, snap.getProjectiles().size(), "Must contain 100 projectile DTOs");
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    readCompleteLatch.countDown();
                }
            });
        }

        // Server broadcasts large snapshots
        for (int i = 0; i < packetCount; i++) {
            GameStateSnapshotPacket hugeSnapshot = createLargeSnapshot(i);
            server.broadcast(hugeSnapshot);
        }

        assertTrue(readCompleteLatch.await(5, TimeUnit.SECONDS), "All clients must receive and deserialize large snapshots in time");
        readerPool.shutdownNow();

        assertTrue(errors.isEmpty(), "Zero errors expected during large payload burst: " + errors);

        for (Socket s : sockets) {
            s.close();
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 6: Adversarial Malformed Packet Burst and Graceful Session Recovery")
    void testAdversarialMalformedPacketStormAndRecovery() throws Exception {
        int clientCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(clientCount);
        CountDownLatch doneLatch = new CountDownLatch(clientCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            pool.submit(() -> {
                try {
                    try (Socket s = new Socket("127.0.0.1", boundPort);
                         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
                         BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {

                        // 1. Send invalid non-JSON garbage line
                        writer.write("GARBAGE_LINE_###_" + clientId);
                        writer.newLine();
                        writer.flush();

                        String errLine1 = reader.readLine();
                        assertNotNull(errLine1);
                        Packet p1 = mapper.readValue(errLine1, Packet.class);
                        assertTrue(p1 instanceof ErrorMessagePacket);
                        assertEquals("MALFORMED_JSON", ((ErrorMessagePacket) p1).getCode());

                        // 2. Send truncated / broken JSON line
                        writer.write("{\"type\":\"PLACE_PLANT_REQUEST\",\"plantName\":\"PEA\""); // Missing closing brace
                        writer.newLine();
                        writer.flush();

                        String errLine2 = reader.readLine();
                        assertNotNull(errLine2);
                        Packet p2 = mapper.readValue(errLine2, Packet.class);
                        assertTrue(p2 instanceof ErrorMessagePacket);
                        assertEquals("MALFORMED_JSON", ((ErrorMessagePacket) p2).getCode());

                        // 3. Send valid packet on the same socket to prove recovery
                        long sendTs = System.currentTimeMillis();
                        writer.write(mapper.writeValueAsString(new HeartbeatPacket(sendTs)));
                        writer.newLine();
                        writer.flush();

                        String validLine = reader.readLine();
                        assertNotNull(validLine);
                        Packet p3 = mapper.readValue(validLine, Packet.class);
                        assertTrue(p3 instanceof HeartbeatPacket);
                        HeartbeatPacket pong = (HeartbeatPacket) p3;
                        assertEquals(sendTs, pong.getClientTimestamp());
                        assertTrue(pong.isPong());
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(doneLatch.await(8, TimeUnit.SECONDS), "Malformed packet storm test must complete within timeout");
        pool.shutdownNow();

        assertTrue(errors.isEmpty(), "Zero unexpected failures during malformed storm: " + errors);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 7: Adversarial TCP RST (SoLinger=0) Abrupt Disconnects During Active Broadcast")
    void testAdversarialTcpRstAbortDuringBroadcast() throws Exception {
        int totalClients = 20;
        int abortCount = 10;
        List<Socket> survivingSockets = new ArrayList<>();
        List<BufferedReader> survivingReaders = new ArrayList<>();
        List<Socket> doomedSockets = new ArrayList<>();

        for (int i = 0; i < totalClients; i++) {
            Socket s = new Socket("127.0.0.1", boundPort);
            s.setTcpNoDelay(true);
            if (i < abortCount) {
                doomedSockets.add(s);
            } else {
                survivingSockets.add(s);
                survivingReaders.add(new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)));
            }
        }

        assertEventually(() -> server.getActiveConnectionCount() == totalClients, 2000);

        // Abruptly RST abort the doomed sockets
        for (Socket s : doomedSockets) {
            s.setSoLinger(true, 0); // Forces TCP RST rather than FIN
            s.close();
        }

        // Server continuously broadcasts while RST drops occur
        for (int b = 0; b < 10; b++) {
            ReactionPacket reaction = new ReactionPacket("SURVIVOR_CHECK", "TEXT", "BROADCAST_" + b);
            server.broadcast(reaction);
        }

        // Surviving clients should cleanly receive all 10 broadcasts
        for (BufferedReader r : survivingReaders) {
            for (int b = 0; b < 10; b++) {
                String line = r.readLine();
                assertNotNull(line, "Surviving client must receive broadcast line " + b);
                Packet p = mapper.readValue(line, Packet.class);
                assertTrue(p instanceof ReactionPacket);
                ReactionPacket rp = (ReactionPacket) p;
                assertEquals("SURVIVOR_CHECK", rp.getSenderUsername());
                assertEquals("BROADCAST_" + b, rp.getContent());
            }
        }

        for (Socket s : survivingSockets) {
            s.close();
        }

        assertEventually(() -> server.getActiveConnectionCount() == 0, 3000);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 8: Concurrent Broadcast and Stop Race Condition")
    void testConcurrentBroadcastAndStopRaceCondition() throws Exception {
        int clientCount = 10;
        List<Socket> sockets = new ArrayList<>();
        for (int i = 0; i < clientCount; i++) {
            Socket s = new Socket("127.0.0.1", boundPort);
            sockets.add(s);
        }

        assertEventually(() -> server.getActiveConnectionCount() == clientCount, 2000);

        ExecutorService pool = Executors.newFixedThreadPool(5);
        AtomicBoolean stopSending = new AtomicBoolean(false);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        // Start 4 background threads calling broadcast constantly
        for (int t = 0; t < 4; t++) {
            pool.submit(() -> {
                try {
                    while (!stopSending.get()) {
                        server.broadcast(new HeartbeatPacket(System.currentTimeMillis()));
                        Thread.sleep(1);
                    }
                } catch (InterruptedException ignored) {
                } catch (Throwable t1) {
                    errors.add(t1);
                }
            });
        }

        Thread.sleep(50);
        // Call server.stop() while broadcasts are flying
        server.stop();
        stopSending.set(true);
        pool.shutdownNow();

        assertTrue(errors.isEmpty(), "Zero exceptions expected during concurrent broadcast and stop: " + errors);
        assertFalse(server.isRunning(), "Server should be stopped");
        assertEquals(0, server.getActiveConnectionCount());

        for (Socket s : sockets) {
            try { s.close(); } catch (Exception ignored) {}
        }
    }

    private GameStateSnapshotPacket createSampleSnapshot(long tick) {
        List<PlantSnapshotDto> plants = List.of(
                new PlantSnapshotDto("p1", "PEASHOOTER", 0, 1, 300, 300, "IDLE", false, false, 0),
                new PlantSnapshotDto("p2", "SUNFLOWER", 1, 0, 300, 300, "IDLE", false, false, 0)
        );
        List<ZombieSnapshotDto> zombies = List.of(
                new ZombieSnapshotDto("z1", "BASIC_ZOMBIE", 0, 500f, 0.5f, 200, 200, 0, "WALKING", 0.5f, false, false, false, false)
        );
        List<ProjectileSnapshotDto> projectiles = List.of(
                new ProjectileSnapshotDto("pr1", "PEA", 0, 200f, 300f, 20f, "NONE")
        );
        return new GameStateSnapshotPacket(tick, tick * 0.05f, 180f, 150, 200, plants, zombies, projectiles, List.of(), false, null, null);
    }

    private GameStateSnapshotPacket createLargeSnapshot(long tick) {
        List<PlantSnapshotDto> plants = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            plants.add(new PlantSnapshotDto("p_" + i, "REPEATER", i % 5, i % 9, 300, 300, "ATTACKING", false, false, 0));
        }
        List<ZombieSnapshotDto> zombies = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            zombies.add(new ZombieSnapshotDto("z_" + i, "BUCKETHEAD_ZOMBIE", i % 5, 200f + i * 5, 0.4f, 1300, 1300, 500, "WALKING", 0.4f, false, false, false, false));
        }
        List<ProjectileSnapshotDto> projectiles = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            projectiles.add(new ProjectileSnapshotDto("pr_" + i, "FIRE_PEA", i % 5, 100f + i * 8, 400f, 40f, "FIRE"));
        }
        return new GameStateSnapshotPacket(tick, tick * 0.05f, 150f, 500, 750, plants, zombies, projectiles, List.of(0, 2), false, null, null);
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
