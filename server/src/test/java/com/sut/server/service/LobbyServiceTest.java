package com.sut.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sut.server.net.ClientConnectionHandler;
import com.sut.server.net.PacketRouter;
import com.sut.server.net.TcpServer;
import com.sut.server.room.RoomManager;
import model.network.enums.MatchmakingMode;
import model.network.enums.MatchmakingStatus;
import model.network.enums.PlayerRole;
import model.network.packet.Packet;
import model.network.packet.matchmaking.CancelMatchmakingPacket;
import model.network.packet.matchmaking.MatchFoundPacket;
import model.network.packet.matchmaking.MatchmakingRequestPacket;
import model.network.packet.matchmaking.MatchmakingResponsePacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class LobbyServiceTest {

    private RoomManager roomManager;
    private LobbyService lobbyService;
    private PacketRouter packetRouter;
    private ObjectMapper objectMapper;
    private ScheduledExecutorService executor;

    private ServerSocket testServerSocket;
    private final List<TestClientHandle> activeHandles = new ArrayList<>();

    private static class TestClientHandle {
        final Socket clientSocket;
        final Socket serverSocket;
        final ClientConnectionHandler connectionHandler;
        final BufferedReader clientReader;
        final BufferedWriter clientWriter;
        final ObjectMapper mapper;

        TestClientHandle(Socket clientSocket, Socket serverSocket, ClientConnectionHandler connectionHandler, ObjectMapper mapper) throws IOException {
            this.clientSocket = clientSocket;
            this.serverSocket = serverSocket;
            this.connectionHandler = connectionHandler;
            this.mapper = mapper;
            this.clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            this.clientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void send(Packet packet) throws IOException {
            String json = mapper.writeValueAsString(packet);
            clientWriter.write(json);
            clientWriter.newLine();
            clientWriter.flush();
        }

        Packet receive(long timeoutMs) throws Exception {
            clientSocket.setSoTimeout((int) timeoutMs);
            String line = clientReader.readLine();
            if (line != null && !line.isBlank()) {
                return mapper.readValue(line, Packet.class);
            }
            throw new AssertionError("Received empty or EOF from server");
        }

        void close() {
            connectionHandler.disconnect("Test teardown");
            try { clientSocket.close(); } catch (IOException ignored) {}
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
    }

    private TcpServer testTcpServer;

    @BeforeEach
    void setUp() throws IOException {
        executor = Executors.newScheduledThreadPool(2);
        roomManager = new RoomManager(executor);
        packetRouter = new PacketRouter();
        objectMapper = TcpServer.createDefaultObjectMapper();
        testTcpServer = new TcpServer("127.0.0.1", 0, packetRouter, objectMapper);
        lobbyService = new LobbyService(roomManager, testTcpServer);

        lobbyService.registerRoutes(packetRouter);
        roomManager.registerRoutes(packetRouter);

        testServerSocket = new ServerSocket(0);
    }

    @AfterEach
    void tearDown() throws IOException {
        for (TestClientHandle handle : activeHandles) {
            handle.close();
        }
        activeHandles.clear();

        if (testServerSocket != null && !testServerSocket.isClosed()) {
            testServerSocket.close();
        }

        if (roomManager != null) {
            roomManager.shutdown();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private TestClientHandle createClient(String username) throws Exception {
        Socket client = new Socket("127.0.0.1", testServerSocket.getLocalPort());
        Socket serverSide = testServerSocket.accept();

        ClientConnectionHandler handler = new ClientConnectionHandler(
                "conn-" + username,
                serverSide,
                testTcpServer,
                packetRouter,
                objectMapper
        );
        handler.setUsername(username);
        testTcpServer.registerConnection(handler);

        TestClientHandle handle = new TestClientHandle(client, serverSide, handler, objectMapper);
        activeHandles.add(handle);
        return handle;
    }

    @Test
    @DisplayName("Random Queue: Pairs complementary PLANT and ZOMBIE roles into an active room")
    void testRandomQueueMatchingComplementaryRoles() throws Exception {
        TestClientHandle playerA = createClient("AlicePlant");
        TestClientHandle playerB = createClient("BobZombie");

        // Alice requests PLANT
        playerA.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.PLANT, "AlicePlant"));
        Packet respA = playerA.receive(1000);
        assertTrue(respA instanceof MatchmakingResponsePacket);
        assertEquals(MatchmakingStatus.QUEUED, ((MatchmakingResponsePacket) respA).getStatus());
        assertEquals(1, lobbyService.getQueueSize());

        // Bob requests ZOMBIE
        playerB.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.ZOMBIE, "BobZombie"));

        // Both should receive MatchFoundPacket
        Packet foundA = playerA.receive(1000);
        Packet foundB = playerB.receive(1000);

        assertTrue(foundA instanceof MatchFoundPacket);
        assertTrue(foundB instanceof MatchFoundPacket);

        MatchFoundPacket mfpA = (MatchFoundPacket) foundA;
        MatchFoundPacket mfpB = (MatchFoundPacket) foundB;

        assertEquals(PlayerRole.PLANT, mfpA.getAssignedRole());
        assertEquals("BobZombie", mfpA.getOpponentUsername());

        assertEquals(PlayerRole.ZOMBIE, mfpB.getAssignedRole());
        assertEquals("AlicePlant", mfpB.getOpponentUsername());

        assertEquals(mfpA.getRoomId(), mfpB.getRoomId());
        assertEquals(0, lobbyService.getQueueSize());
        assertEquals(1, roomManager.getActiveRoomCount());
    }

    @Test
    @DisplayName("Random Queue: Pairs ANY and ANY roles and assigns distinct roles")
    void testRandomQueueMatchingAnyRoles() throws Exception {
        TestClientHandle playerA = createClient("PlayerAny1");
        TestClientHandle playerB = createClient("PlayerAny2");

        playerA.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.ANY, "PlayerAny1"));
        playerA.receive(1000);

        playerB.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.ANY, "PlayerAny2"));

        MatchFoundPacket foundA = (MatchFoundPacket) playerA.receive(1000);
        MatchFoundPacket foundB = (MatchFoundPacket) playerB.receive(1000);

        assertNotEquals(foundA.getAssignedRole(), foundB.getAssignedRole());
        assertTrue(foundA.getAssignedRole() == PlayerRole.PLANT || foundA.getAssignedRole() == PlayerRole.ZOMBIE);
        assertTrue(foundB.getAssignedRole() == PlayerRole.PLANT || foundB.getAssignedRole() == PlayerRole.ZOMBIE);
        assertEquals(0, lobbyService.getQueueSize());
    }

    @Test
    @DisplayName("Direct Invite: Host creates room with 6-char code, guest joins by code")
    void testDirectInviteCreateAndJoin() throws Exception {
        TestClientHandle host = createClient("HostUser");
        TestClientHandle guest = createClient("GuestUser");

        // Host creates private room
        host.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.CREATE_ROOM, null, PlayerRole.PLANT, "HostUser"));
        MatchmakingResponsePacket createResp = (MatchmakingResponsePacket) host.receive(1000);

        assertEquals(MatchmakingStatus.ROOM_CREATED, createResp.getStatus());
        String code = createResp.getRoomCode();
        assertNotNull(code);
        assertEquals(6, code.length());
        assertEquals(1, lobbyService.getHostedRoomCount());

        // Guest joins using code
        guest.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.DIRECT_INVITE, code, PlayerRole.ZOMBIE, "GuestUser"));

        MatchFoundPacket foundHost = (MatchFoundPacket) host.receive(1000);
        MatchFoundPacket foundGuest = (MatchFoundPacket) guest.receive(1000);

        assertEquals(PlayerRole.PLANT, foundHost.getAssignedRole());
        assertEquals(PlayerRole.ZOMBIE, foundGuest.getAssignedRole());
        assertEquals("GuestUser", foundHost.getOpponentUsername());
        assertEquals("HostUser", foundGuest.getOpponentUsername());

        assertEquals(0, lobbyService.getHostedRoomCount());
        assertEquals(1, roomManager.getActiveRoomCount());
    }

    @Test
    @DisplayName("Direct Invite: Join with invalid/non-existent code returns error")
    void testDirectInviteInvalidCode() throws Exception {
        TestClientHandle guest = createClient("GuestUser");
        guest.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.DIRECT_INVITE, "NONO99", PlayerRole.ANY, "GuestUser"));

        MatchmakingResponsePacket resp = (MatchmakingResponsePacket) guest.receive(1000);
        assertEquals(MatchmakingStatus.ERROR, resp.getStatus());
        assertTrue(resp.getMessage().contains("Room not found"));
    }

    @Test
    @DisplayName("Direct Invite: Host cannot join their own private room")
    void testDirectInviteCannotJoinOwnRoom() throws Exception {
        TestClientHandle host = createClient("HostUser");
        host.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.CREATE_ROOM, null, PlayerRole.ANY, "HostUser"));
        MatchmakingResponsePacket createResp = (MatchmakingResponsePacket) host.receive(1000);
        String code = createResp.getRoomCode();

        host.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.DIRECT_INVITE, code, PlayerRole.ANY, "HostUser"));
        MatchmakingResponsePacket errResp = (MatchmakingResponsePacket) host.receive(1000);

        assertEquals(MatchmakingStatus.ERROR, errResp.getStatus());
        assertTrue(errResp.getMessage().contains("Cannot join your own room"));
        assertEquals(1, lobbyService.getHostedRoomCount());
    }

    @Test
    @DisplayName("Direct Invite: Guest joining a disconnected host room receives ERROR")
    void testDirectInviteHostDisconnected() throws Exception {
        TestClientHandle host = createClient("HostUser");
        TestClientHandle guest = createClient("GuestUser");

        host.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.CREATE_ROOM, null, PlayerRole.ANY, "HostUser"));
        MatchmakingResponsePacket createResp = (MatchmakingResponsePacket) host.receive(1000);
        String code = createResp.getRoomCode();

        // Host drops connection
        host.connectionHandler.disconnect("Leaving");
        Thread.sleep(50);

        guest.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.DIRECT_INVITE, code, PlayerRole.ANY, "GuestUser"));
        MatchmakingResponsePacket resp = (MatchmakingResponsePacket) guest.receive(1000);

        assertEquals(MatchmakingStatus.ERROR, resp.getStatus());
    }

    @Test
    @DisplayName("Cancel Matchmaking: Removes player from random matchmaking queue")
    void testCancelMatchmakingQueue() throws Exception {
        TestClientHandle player = createClient("PlayerCancel");

        player.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.ANY, "PlayerCancel"));
        player.receive(1000);
        assertEquals(1, lobbyService.getQueueSize());

        player.connectionHandler.handlePacket(new CancelMatchmakingPacket("PlayerCancel"));
        MatchmakingResponsePacket cancelResp = (MatchmakingResponsePacket) player.receive(1000);

        assertEquals(MatchmakingStatus.CANCELLED, cancelResp.getStatus());
        assertEquals(0, lobbyService.getQueueSize());
    }

    @Test
    @DisplayName("Cancel Matchmaking: Removes hosted private room")
    void testCancelMatchmakingHostedRoom() throws Exception {
        TestClientHandle host = createClient("HostUser");

        host.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.CREATE_ROOM, null, PlayerRole.ANY, "HostUser"));
        MatchmakingResponsePacket createResp = (MatchmakingResponsePacket) host.receive(1000);
        String code = createResp.getRoomCode();
        assertEquals(1, lobbyService.getHostedRoomCount());

        host.connectionHandler.handlePacket(new CancelMatchmakingPacket("HostUser", code));
        MatchmakingResponsePacket cancelResp = (MatchmakingResponsePacket) host.receive(1000);

        assertEquals(MatchmakingStatus.CANCELLED, cancelResp.getStatus());
        assertEquals(0, lobbyService.getHostedRoomCount());
    }

    @Test
    @DisplayName("Lobby: Duplicate queue request from same connection is rejected")
    void testDuplicateQueueRejected() throws Exception {
        TestClientHandle player = createClient("PlayerDup");

        player.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.ANY, "PlayerDup"));
        player.receive(1000);

        player.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.ANY, "PlayerDup"));
        MatchmakingResponsePacket dupResp = (MatchmakingResponsePacket) player.receive(1000);

        assertEquals(MatchmakingStatus.ERROR, dupResp.getStatus());
        assertTrue(dupResp.getMessage().contains("Already in"));
        assertEquals(1, lobbyService.getQueueSize());
    }

    @Test
    @DisplayName("Lobby: Disconnect automatically cleans up queued entries")
    void testDisconnectCleansUpQueue() throws Exception {
        TestClientHandle player = createClient("PlayerDrop");

        player.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.ANY, "PlayerDrop"));
        player.receive(1000);
        assertEquals(1, lobbyService.getQueueSize());

        // Disconnect triggers packetRouter.onConnectionClosed -> lobbyService.handleDisconnect
        player.connectionHandler.disconnect("Client quit");
        assertEquals(0, lobbyService.getQueueSize());
    }

    @Test
    @DisplayName("findConnection: requires TcpServer wiring to resolve online players")
    void testFindConnectionNeedsTcpServer() throws Exception {
        TestClientHandle bob = createClient("BobLookup");
        LobbyService unwired = new LobbyService(roomManager);
        assertNull(unwired.findConnection("BobLookup"));
        assertNotNull(lobbyService.findConnection("BobLookup"));
    }

    @Test
    @DisplayName("Direct Invite: Alice invites Bob -> Bob accepts -> Room created and MatchFoundPacket sent to both")
    void testDirectInviteFlowAccept() throws Exception {
        TestClientHandle alice = createClient("Alice");
        TestClientHandle bob = createClient("Bob");

        // Alice invites Bob
        alice.connectionHandler.handlePacket(new model.network.packet.InviteRequestPacket("Bob", PlayerRole.PLANT, "Alice"));

        // Alice receives PENDING status
        Packet statusA = alice.receive(1000);
        assertInstanceOf(model.network.packet.InviteStatusPacket.class, statusA);
        model.network.packet.InviteStatusPacket ispA = (model.network.packet.InviteStatusPacket) statusA;
        assertEquals(model.network.enums.InviteStatus.PENDING, ispA.getStatus());
        String inviteId = ispA.getInviteId();
        assertNotNull(inviteId);

        // Bob receives InviteReceivedPacket
        Packet receivedB = bob.receive(1000);
        assertInstanceOf(model.network.packet.InviteReceivedPacket.class, receivedB);
        model.network.packet.InviteReceivedPacket irpB = (model.network.packet.InviteReceivedPacket) receivedB;
        assertEquals(inviteId, irpB.getInviteId());
        assertEquals("Alice", irpB.getInviterUsername());
        assertEquals(PlayerRole.PLANT, irpB.getInviterRole());
        assertEquals(10, irpB.getTimeoutSeconds());

        // Bob accepts invite
        bob.connectionHandler.handlePacket(new model.network.packet.InviteResponsePacket(
                inviteId, "Alice", model.network.enums.InviteDecision.ACCEPT
        ));

        // Alice receives ACCEPTED status
        Packet statusAccept = alice.receive(1000);
        assertInstanceOf(model.network.packet.InviteStatusPacket.class, statusAccept);
        assertEquals(model.network.enums.InviteStatus.ACCEPTED, ((model.network.packet.InviteStatusPacket) statusAccept).getStatus());

        // Both receive MatchFoundPacket
        Packet matchA = alice.receive(1000);
        Packet matchB = bob.receive(1000);

        assertInstanceOf(MatchFoundPacket.class, matchA);
        assertInstanceOf(MatchFoundPacket.class, matchB);

        MatchFoundPacket mfpA = (MatchFoundPacket) matchA;
        MatchFoundPacket mfpB = (MatchFoundPacket) matchB;

        assertEquals(PlayerRole.PLANT, mfpA.getAssignedRole());
        assertEquals("Bob", mfpA.getOpponentUsername());

        assertEquals(PlayerRole.ZOMBIE, mfpB.getAssignedRole());
        assertEquals("Alice", mfpB.getOpponentUsername());

        assertEquals(1, roomManager.getActiveRoomCount());
    }

    @Test
    @DisplayName("Direct Invite: Alice invites Bob -> Bob declines -> Alice receives DECLINED status and no room created")
    void testDirectInviteFlowDecline() throws Exception {
        TestClientHandle alice = createClient("AliceDec");
        TestClientHandle bob = createClient("BobDec");

        alice.connectionHandler.handlePacket(new model.network.packet.InviteRequestPacket("BobDec", PlayerRole.ZOMBIE, "AliceDec"));

        model.network.packet.InviteStatusPacket pendingA = (model.network.packet.InviteStatusPacket) alice.receive(1000);
        model.network.packet.InviteReceivedPacket recB = (model.network.packet.InviteReceivedPacket) bob.receive(1000);

        // Bob declines
        bob.connectionHandler.handlePacket(new model.network.packet.InviteResponsePacket(
                recB.getInviteId(), "AliceDec", model.network.enums.InviteDecision.DECLINE
        ));

        // Alice receives DECLINED
        Packet statusA = alice.receive(1000);
        assertInstanceOf(model.network.packet.InviteStatusPacket.class, statusA);
        assertEquals(model.network.enums.InviteStatus.DECLINED, ((model.network.packet.InviteStatusPacket) statusA).getStatus());

        assertEquals(0, roomManager.getActiveRoomCount());
        assertEquals(0, lobbyService.getPendingInvites().size());
    }

    @Test
    @DisplayName("Direct Invite: Alice invites Bob -> Server 10s timeout triggers -> Alice receives TIMED_OUT status")
    void testDirectInviteServerTimeout() throws Exception {
        TestClientHandle alice = createClient("AliceTimeout");
        TestClientHandle bob = createClient("BobTimeout");

        alice.connectionHandler.handlePacket(new model.network.packet.InviteRequestPacket("BobTimeout", PlayerRole.ANY, "AliceTimeout"));

        model.network.packet.InviteStatusPacket pendingA = (model.network.packet.InviteStatusPacket) alice.receive(1000);
        bob.receive(1000); // Bob gets invite

        String inviteId = pendingA.getInviteId();
        assertNotNull(inviteId);
        assertEquals(1, lobbyService.getPendingInvites().size());

        // Trigger timeout manually
        lobbyService.handleInviteTimeout(inviteId);

        Packet statusTimeout = alice.receive(1000);
        assertInstanceOf(model.network.packet.InviteStatusPacket.class, statusTimeout);
        assertEquals(model.network.enums.InviteStatus.TIMED_OUT, ((model.network.packet.InviteStatusPacket) statusTimeout).getStatus());

        assertEquals(0, lobbyService.getPendingInvites().size());
    }

    @Test
    @DisplayName("Direct Invite: Alice invites offline user -> Alice immediately receives OFFLINE status")
    void testDirectInviteTargetOffline() throws Exception {
        TestClientHandle alice = createClient("AliceSolo");

        alice.connectionHandler.handlePacket(new model.network.packet.InviteRequestPacket("NonExistentUser", PlayerRole.ANY, "AliceSolo"));

        Packet statusA = alice.receive(1000);
        assertInstanceOf(model.network.packet.InviteStatusPacket.class, statusA);
        assertEquals(model.network.enums.InviteStatus.OFFLINE, ((model.network.packet.InviteStatusPacket) statusA).getStatus());
    }

    @Test
    @DisplayName("Direct Invite: Multiplayer busy avoidance -> Auto-rejects without disrupting active match")
    void testDirectInviteBusyAvoidance() throws Exception {
        TestClientHandle player1 = createClient("BusyPlayer1");
        TestClientHandle player2 = createClient("BusyPlayer2");
        TestClientHandle inviter = createClient("InviterAlice");

        // Put player1 and player2 into an active multiplayer room
        player1.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.PLANT, "BusyPlayer1"));
        player1.receive(1000);
        player2.connectionHandler.handlePacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.ZOMBIE, "BusyPlayer2"));
        player1.receive(1000);
        player2.receive(1000);

        assertEquals(1, roomManager.getActiveRoomCount());
        assertNotNull(roomManager.getRoomForPlayer(player1.connectionHandler));

        // Alice tries to invite BusyPlayer1 who is inside active room
        inviter.connectionHandler.handlePacket(new model.network.packet.InviteRequestPacket("BusyPlayer1", PlayerRole.ANY, "InviterAlice"));

        Packet status = inviter.receive(1000);
        assertInstanceOf(model.network.packet.InviteStatusPacket.class, status);
        model.network.packet.InviteStatusPacket isp = (model.network.packet.InviteStatusPacket) status;
        assertEquals(model.network.enums.InviteStatus.BUSY, isp.getStatus());

        // Player1 is unaffected and active room continues
        assertEquals(1, roomManager.getActiveRoomCount());
        assertEquals(0, lobbyService.getPendingInvites().size());
    }

    @Test
    @DisplayName("Direct Invite: Alice cancels in-flight invite -> Bob receives CancelInvitePacket")
    void testDirectInviteCancel() throws Exception {
        TestClientHandle alice = createClient("AliceCan");
        TestClientHandle bob = createClient("BobCan");

        alice.connectionHandler.handlePacket(new model.network.packet.InviteRequestPacket("BobCan", PlayerRole.ANY, "AliceCan"));
        model.network.packet.InviteStatusPacket pendingA = (model.network.packet.InviteStatusPacket) alice.receive(1000);
        bob.receive(1000);

        // Alice cancels
        alice.connectionHandler.handlePacket(new model.network.packet.CancelInvitePacket(pendingA.getInviteId(), "BobCan"));

        model.network.packet.InviteStatusPacket cancelA = (model.network.packet.InviteStatusPacket) alice.receive(1000);
        assertEquals(model.network.enums.InviteStatus.CANCELLED, cancelA.getStatus());

        Packet cancelB = bob.receive(1000);
        assertInstanceOf(model.network.packet.CancelInvitePacket.class, cancelB);

        assertEquals(0, lobbyService.getPendingInvites().size());
    }

    @Test
    @DisplayName("Direct Invite: Disconnect cleans up pending invites and notifies peer")
    void testDirectInviteDisconnectCleanup() throws Exception {
        TestClientHandle alice = createClient("AliceDropInv");
        TestClientHandle bob = createClient("BobDropInv");

        alice.connectionHandler.handlePacket(new model.network.packet.InviteRequestPacket("BobDropInv", PlayerRole.ANY, "AliceDropInv"));
        alice.receive(1000);
        bob.receive(1000);

        // Alice drops
        alice.connectionHandler.disconnect("Alice left");

        Packet statusB = bob.receive(1000);
        assertInstanceOf(model.network.packet.InviteStatusPacket.class, statusB);
        assertEquals(model.network.enums.InviteStatus.OFFLINE, ((model.network.packet.InviteStatusPacket) statusB).getStatus());

        assertEquals(0, lobbyService.getPendingInvites().size());
    }
}
