package com.sut.server.service;

import com.sut.server.net.ClientConnectionHandler;
import com.sut.server.net.PacketRouter;
import com.sut.server.net.TcpServer;
import com.sut.server.room.IZombieGameRoom;
import com.sut.server.room.RoomManager;
import model.network.enums.InviteDecision;
import model.network.enums.InviteStatus;
import model.network.enums.MatchmakingMode;
import model.network.enums.MatchmakingStatus;
import model.network.enums.PlayerRole;
import model.network.packet.CancelInvitePacket;
import model.network.packet.InviteReceivedPacket;
import model.network.packet.InviteRequestPacket;
import model.network.packet.InviteResponsePacket;
import model.network.packet.InviteStatusPacket;
import model.network.packet.matchmaking.CancelMatchmakingPacket;
import model.network.packet.matchmaking.MatchmakingRequestPacket;
import model.network.packet.matchmaking.MatchmakingResponsePacket;
import model.network.packet.system.ErrorMessagePacket;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Authoritative Lobby & Matchmaking Service.
 * Manages FIFO Random Matchmaking Queue with complementary role pairing,
 * 6-character alphanumeric Direct Invite private room codes, cross-client direct player invites
 * with 10-second timeout tracking, and automatic disconnect cleanup.
 */
public class LobbyService {

    private static final String ROOM_CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // 32 unambiguous chars
    private static final int ROOM_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static class QueueEntry {
        private final ClientConnectionHandler connection;
        private final PlayerRole preferredRole;
        private final long enqueueTime;

        public QueueEntry(ClientConnectionHandler connection, PlayerRole preferredRole) {
            this.connection = connection;
            this.preferredRole = preferredRole != null ? preferredRole : PlayerRole.ANY;
            this.enqueueTime = System.currentTimeMillis();
        }

        public ClientConnectionHandler getConnection() { return connection; }
        public PlayerRole getPreferredRole() { return preferredRole; }
        public long getEnqueueTime() { return enqueueTime; }
    }

    public static class PrivateRoom {
        private final String roomCode;
        private final ClientConnectionHandler hostConnection;
        private final PlayerRole hostRole;
        private final long creationTime;

        public PrivateRoom(String roomCode, ClientConnectionHandler hostConnection, PlayerRole hostRole) {
            this.roomCode = roomCode;
            this.hostConnection = hostConnection;
            this.hostRole = hostRole != null ? hostRole : PlayerRole.ANY;
            this.creationTime = System.currentTimeMillis();
        }

        public String getRoomCode() { return roomCode; }
        public ClientConnectionHandler getHostConnection() { return hostConnection; }
        public PlayerRole getHostRole() { return hostRole; }
        public long getCreationTime() { return creationTime; }
    }

    public static class PendingInvite {
        private final String inviteId;
        private final ClientConnectionHandler inviterConnection;
        private final ClientConnectionHandler targetConnection;
        private final String inviterUsername;
        private final String targetUsername;
        private final PlayerRole inviterRole;
        private final long creationTime;
        private volatile ScheduledFuture<?> timeoutFuture;

        public PendingInvite(String inviteId, ClientConnectionHandler inviterConnection,
                             ClientConnectionHandler targetConnection, String inviterUsername,
                             String targetUsername, PlayerRole inviterRole) {
            this.inviteId = inviteId;
            this.inviterConnection = inviterConnection;
            this.targetConnection = targetConnection;
            this.inviterUsername = inviterUsername;
            this.targetUsername = targetUsername;
            this.inviterRole = inviterRole != null ? inviterRole : PlayerRole.ANY;
            this.creationTime = System.currentTimeMillis();
        }

        public String getInviteId() { return inviteId; }
        public ClientConnectionHandler getInviterConnection() { return inviterConnection; }
        public ClientConnectionHandler getTargetConnection() { return targetConnection; }
        public String getInviterUsername() { return inviterUsername; }
        public String getTargetUsername() { return targetUsername; }
        public PlayerRole getInviterRole() { return inviterRole; }
        public long getCreationTime() { return creationTime; }
        public ScheduledFuture<?> getTimeoutFuture() { return timeoutFuture; }
        public void setTimeoutFuture(ScheduledFuture<?> timeoutFuture) { this.timeoutFuture = timeoutFuture; }
    }

    private final RoomManager roomManager;
    private volatile TcpServer tcpServer;
    private volatile AuthService authService;
    private final List<QueueEntry> randomQueue = new ArrayList<>();
    private final Map<String, PrivateRoom> hostedPrivateRooms = new ConcurrentHashMap<>();
    private final Map<String, PendingInvite> pendingInvites = new ConcurrentHashMap<>();
    private final ScheduledExecutorService fallbackScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "pvz-lobby-timeout");
        t.setDaemon(true);
        return t;
    });

    public LobbyService(RoomManager roomManager) {
        this(roomManager, null);
    }

    public LobbyService(RoomManager roomManager, TcpServer tcpServer) {
        this.roomManager = Objects.requireNonNull(roomManager, "RoomManager cannot be null");
        this.tcpServer = tcpServer;
    }

    public void setTcpServer(TcpServer tcpServer) {
        this.tcpServer = tcpServer;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public TcpServer getTcpServer() {
        return tcpServer;
    }

    /**
     * Registers matchmaking routes and disconnect listeners with PacketRouter.
     */
    public void registerRoutes(PacketRouter router) {
        if (router == null) return;

        router.registerHandler(MatchmakingRequestPacket.class, (conn, packet) -> {
            enqueuePlayer(conn, packet);
        });

        router.registerHandler(CancelMatchmakingPacket.class, (conn, packet) -> {
            cancelMatchmaking(conn, packet);
        });

        router.registerHandler(InviteRequestPacket.class, (conn, packet) -> {
            handleInviteRequest(conn, packet);
        });

        router.registerHandler(InviteResponsePacket.class, (conn, packet) -> {
            handleInviteResponse(conn, packet);
        });

        router.registerHandler(CancelInvitePacket.class, (conn, packet) -> {
            handleCancelInvite(conn, packet);
        });

        router.addConnectionClosedListener(this::handleDisconnect);
    }

    /**
     * Finds an online connected client by username.
     */
    public ClientConnectionHandler findConnection(String username) {
        if (username == null || username.isBlank()) return null;
        if (authService != null) {
            Optional<ClientConnectionHandler> session = authService.getSession(username.trim());
            if (session.isPresent() && !session.get().isClosed()) {
                return session.get();
            }
        }
        if (tcpServer != null) {
            Optional<ClientConnectionHandler> opt = tcpServer.findByUsername(username.trim());
            if (opt.isPresent() && !opt.get().isClosed()) {
                return opt.get();
            }
        }
        return null;
    }

    /**
     * Handles inbound direct invite requests from an inviter client.
     */
    public synchronized void handleInviteRequest(ClientConnectionHandler connection, InviteRequestPacket packet) {
        if (connection == null || connection.isClosed() || packet == null) return;

        String targetUsername = packet.getTargetUsername();
        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            connection.sendPacket(new InviteStatusPacket(null, InviteStatus.NOT_FOUND, "Target username cannot be empty."));
            return;
        }
        targetUsername = targetUsername.trim();

        String inviterUsername = connection.getUsername();
        if (inviterUsername == null || inviterUsername.isBlank()) {
            inviterUsername = packet.getInviterUsername() != null ? packet.getInviterUsername() : "Player";
        }

        // 1. Cannot invite self
        if (targetUsername.equalsIgnoreCase(inviterUsername)) {
            connection.sendPacket(new InviteStatusPacket(null, InviteStatus.NOT_FOUND, "Cannot invite yourself."));
            return;
        }

        // 2. Inviter in an active match guard
        if (roomManager.getRoomForPlayer(connection) != null) {
            connection.sendPacket(new InviteStatusPacket(null, InviteStatus.BUSY, "You are currently in an active multiplayer game."));
            return;
        }

        // 3. Look up target client
        ClientConnectionHandler targetConn = findConnection(targetUsername);
        if (targetConn == null || targetConn.isClosed()) {
            connection.sendPacket(new InviteStatusPacket(null, InviteStatus.OFFLINE, "User '" + targetUsername + "' is offline or not found."));
            return;
        }

        // 4. Target busy in active multiplayer match guard (Auto-reject without disrupting target's active game)
        if (roomManager.getRoomForPlayer(targetConn) != null) {
            connection.sendPacket(new InviteStatusPacket(null, InviteStatus.BUSY, "'" + targetUsername + "' is currently in an active multiplayer match."));
            return;
        }

        // 5. Cancel any prior pending invite sent by this inviter
        cancelPendingInvitesByInviter(connection);

        // 6. Formulate new pending invite
        String inviteId = "inv-" + UUID.randomUUID().toString().substring(0, 8);
        PlayerRole inviterRole = packet.getPreferredRole() != null ? packet.getPreferredRole() : PlayerRole.ANY;
        PendingInvite pending = new PendingInvite(inviteId, connection, targetConn, inviterUsername, targetUsername, inviterRole);

        // 7. Register authoritative 10-second server timeout task
        ScheduledExecutorService executor = roomManager.getScheduledExecutor() != null ? roomManager.getScheduledExecutor() : fallbackScheduler;
        ScheduledFuture<?> timeoutFuture = executor.schedule(() -> {
            handleInviteTimeout(inviteId);
        }, 10, TimeUnit.SECONDS);
        pending.setTimeoutFuture(timeoutFuture);

        pendingInvites.put(inviteId, pending);

        // 8. Send status to inviter
        connection.sendPacket(new InviteStatusPacket(inviteId, InviteStatus.PENDING,
                "Invite sent to " + targetUsername + ". Waiting for response (10s)..."));

        // 9. Forward InviteReceivedPacket to target client with 10s deadline
        targetConn.sendPacket(new InviteReceivedPacket(inviteId, inviterUsername, inviterRole, 10));
        System.out.println("[LobbyService] Direct invite " + inviteId + " sent from " + inviterUsername + " to " + targetUsername);
    }

    /**
     * Handles target client's decision on an incoming invite (ACCEPT, DECLINE, TIMEOUT).
     */
    public synchronized void handleInviteResponse(ClientConnectionHandler connection, InviteResponsePacket packet) {
        if (connection == null || packet == null) return;

        String inviteId = packet.getInviteId();
        if (inviteId == null) return;

        PendingInvite pending = pendingInvites.remove(inviteId);
        if (pending == null) {
            // Expired, already resolved, or cancelled
            return;
        }

        // Cancel the 10s server timeout task
        if (pending.getTimeoutFuture() != null) {
            pending.getTimeoutFuture().cancel(false);
        }

        ClientConnectionHandler inviterConn = pending.getInviterConnection();
        ClientConnectionHandler targetConn = pending.getTargetConnection();
        InviteDecision decision = packet.getDecision() != null ? packet.getDecision() : InviteDecision.DECLINE;

        if (decision == InviteDecision.ACCEPT) {
            if (inviterConn.isClosed()) {
                targetConn.sendPacket(new InviteStatusPacket(inviteId, InviteStatus.OFFLINE, "Inviter disconnected."));
                return;
            }
            if (targetConn.isClosed()) {
                inviterConn.sendPacket(new InviteStatusPacket(inviteId, InviteStatus.OFFLINE, "Target player disconnected."));
                return;
            }

            if (roomManager.getRoomForPlayer(inviterConn) != null || roomManager.getRoomForPlayer(targetConn) != null) {
                inviterConn.sendPacket(new InviteStatusPacket(inviteId, InviteStatus.BUSY, "A player is already in an active room."));
                targetConn.sendPacket(new InviteStatusPacket(inviteId, InviteStatus.BUSY, "A player is already in an active room."));
                return;
            }

            // Assign complementary roles
            PlayerRole inviterRole = pending.getInviterRole();
            ClientConnectionHandler plantConn;
            ClientConnectionHandler zombieConn;

            if (inviterRole == PlayerRole.PLANT) {
                plantConn = inviterConn;
                zombieConn = targetConn;
            } else if (inviterRole == PlayerRole.ZOMBIE) {
                plantConn = targetConn;
                zombieConn = inviterConn;
            } else {
                plantConn = inviterConn;
                zombieConn = targetConn;
            }

            System.out.println("[LobbyService] Direct invite " + inviteId + " accepted: "
                    + (plantConn.getUsername() != null ? plantConn.getUsername() : "Plant") + " (PLANT) vs "
                    + (zombieConn.getUsername() != null ? zombieConn.getUsername() : "Zombie") + " (ZOMBIE)");

            inviterConn.sendPacket(new InviteStatusPacket(inviteId, InviteStatus.ACCEPTED, "Invite accepted!"));
            roomManager.createRoom(plantConn, zombieConn);

        } else if (decision == InviteDecision.DECLINE) {
            if (!inviterConn.isClosed()) {
                inviterConn.sendPacket(new InviteStatusPacket(inviteId, InviteStatus.DECLINED,
                        pending.getTargetUsername() + " declined the invite."));
            }
            System.out.println("[LobbyService] Direct invite " + inviteId + " declined by " + pending.getTargetUsername());

        } else if (decision == InviteDecision.TIMEOUT) {
            if (!inviterConn.isClosed()) {
                inviterConn.sendPacket(new InviteStatusPacket(inviteId, InviteStatus.TIMED_OUT,
                        "Invite to " + pending.getTargetUsername() + " timed out."));
            }
            System.out.println("[LobbyService] Direct invite " + inviteId + " timed out by target client.");
        }
    }

    /**
     * Authoritative 10-second server timeout trigger.
     */
    public synchronized void handleInviteTimeout(String inviteId) {
        if (inviteId == null) return;
        PendingInvite pending = pendingInvites.remove(inviteId);
        if (pending != null) {
            if (!pending.getInviterConnection().isClosed()) {
                pending.getInviterConnection().sendPacket(new InviteStatusPacket(inviteId, InviteStatus.TIMED_OUT,
                        "Invite to " + pending.getTargetUsername() + " timed out after 10 seconds."));
            }
            if (!pending.getTargetConnection().isClosed()) {
                pending.getTargetConnection().sendPacket(new CancelInvitePacket(inviteId, pending.getTargetUsername()));
            }
            System.out.println("[LobbyService] Direct invite " + inviteId + " server timeout executed.");
        }
    }

    /**
     * Handles inviter cancellation of an in-flight invite.
     */
    public synchronized void handleCancelInvite(ClientConnectionHandler connection, CancelInvitePacket packet) {
        if (connection == null) return;

        String inviteId = packet != null ? packet.getInviteId() : null;
        if (inviteId != null) {
            PendingInvite pending = pendingInvites.remove(inviteId);
            if (pending != null) {
                if (pending.getTimeoutFuture() != null) {
                    pending.getTimeoutFuture().cancel(false);
                }
                connection.sendPacket(new InviteStatusPacket(inviteId, InviteStatus.CANCELLED, "Invite cancelled."));
                if (!pending.getTargetConnection().isClosed()) {
                    pending.getTargetConnection().sendPacket(new CancelInvitePacket(inviteId, pending.getTargetUsername()));
                }
                System.out.println("[LobbyService] Direct invite " + inviteId + " cancelled by inviter.");
            }
        } else {
            cancelPendingInvitesByInviter(connection);
        }
    }

    private boolean cancelPendingInvitesByInviter(ClientConnectionHandler inviter) {
        boolean removed = false;
        for (Iterator<Map.Entry<String, PendingInvite>> it = pendingInvites.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, PendingInvite> entry = it.next();
            PendingInvite pending = entry.getValue();
            if (pending.getInviterConnection() == inviter) {
                if (pending.getTimeoutFuture() != null) {
                    pending.getTimeoutFuture().cancel(false);
                }
                it.remove();
                removed = true;
                if (!pending.getTargetConnection().isClosed()) {
                    pending.getTargetConnection().sendPacket(new CancelInvitePacket(pending.getInviteId(), pending.getTargetUsername()));
                }
            }
        }
        return removed;
    }

    /**
     * Handles inbound matchmaking requests across RANDOM, CREATE_ROOM, and DIRECT_INVITE modes.
     */
    public synchronized void enqueuePlayer(ClientConnectionHandler connection, MatchmakingRequestPacket packet) {
        if (connection == null || connection.isClosed() || packet == null) {
            return;
        }

        MatchmakingMode mode = packet.getMode() != null ? packet.getMode() : MatchmakingMode.RANDOM;
        PlayerRole preferredRole = packet.getPreferredRole() != null ? packet.getPreferredRole() : PlayerRole.ANY;

        switch (mode) {
            case RANDOM -> handleRandomMatchmaking(connection, preferredRole);
            case CREATE_ROOM -> handleCreatePrivateRoom(connection, preferredRole);
            case DIRECT_INVITE -> handleDirectInviteJoin(connection, packet.getRoomCode(), preferredRole);
        }
    }

    /**
     * Processes Random Queue matchmaking with complementary role pairing.
     */
    private synchronized void handleRandomMatchmaking(ClientConnectionHandler connection, PlayerRole preferredRole) {
        // 1. Guard against duplicate queuing
        if (isPlayerInQueue(connection)) {
            connection.sendPacket(new MatchmakingResponsePacket(MatchmakingStatus.ERROR, null, "Already in matchmaking queue."));
            return;
        }

        // 2. Guard against players already inside an active room
        if (roomManager.getRoomForPlayer(connection) != null) {
            connection.sendPacket(new MatchmakingResponsePacket(MatchmakingStatus.ERROR, null, "Already in an active game room."));
            return;
        }

        // 3. Purge dead connections from queue
        purgeClosedQueueEntries();

        // 4. Find complementary opponent in queue
        QueueEntry matchedOpponent = null;
        PlayerRole assignedMyRole = null;
        PlayerRole assignedOpponentRole = null;

        for (Iterator<QueueEntry> it = randomQueue.iterator(); it.hasNext(); ) {
            QueueEntry candidate = it.next();
            if (candidate.getConnection().isClosed()) {
                it.remove();
                continue;
            }
            if (candidate.getConnection() == connection) {
                continue;
            }

            // Evaluate role compatibility
            PlayerRole cRole = candidate.getPreferredRole();
            if (preferredRole == PlayerRole.PLANT && cRole == PlayerRole.ZOMBIE) {
                matchedOpponent = candidate;
                assignedMyRole = PlayerRole.PLANT;
                assignedOpponentRole = PlayerRole.ZOMBIE;
                it.remove();
                break;
            } else if (preferredRole == PlayerRole.ZOMBIE && cRole == PlayerRole.PLANT) {
                matchedOpponent = candidate;
                assignedMyRole = PlayerRole.ZOMBIE;
                assignedOpponentRole = PlayerRole.PLANT;
                it.remove();
                break;
            } else if (preferredRole == PlayerRole.PLANT && cRole == PlayerRole.ANY) {
                matchedOpponent = candidate;
                assignedMyRole = PlayerRole.PLANT;
                assignedOpponentRole = PlayerRole.ZOMBIE;
                it.remove();
                break;
            } else if (preferredRole == PlayerRole.ZOMBIE && cRole == PlayerRole.ANY) {
                matchedOpponent = candidate;
                assignedMyRole = PlayerRole.ZOMBIE;
                assignedOpponentRole = PlayerRole.PLANT;
                it.remove();
                break;
            } else if (preferredRole == PlayerRole.ANY && cRole == PlayerRole.PLANT) {
                matchedOpponent = candidate;
                assignedMyRole = PlayerRole.ZOMBIE;
                assignedOpponentRole = PlayerRole.PLANT;
                it.remove();
                break;
            } else if (preferredRole == PlayerRole.ANY && cRole == PlayerRole.ZOMBIE) {
                matchedOpponent = candidate;
                assignedMyRole = PlayerRole.PLANT;
                assignedOpponentRole = PlayerRole.ZOMBIE;
                it.remove();
                break;
            } else if (preferredRole == PlayerRole.ANY && cRole == PlayerRole.ANY) {
                matchedOpponent = candidate;
                assignedMyRole = PlayerRole.PLANT;
                assignedOpponentRole = PlayerRole.ZOMBIE;
                it.remove();
                break;
            }
        }

        // If no complementary match found, match with the oldest queued player anyway
        if (matchedOpponent == null && !randomQueue.isEmpty()) {
            for (Iterator<QueueEntry> it = randomQueue.iterator(); it.hasNext(); ) {
                QueueEntry candidate = it.next();
                if (!candidate.getConnection().isClosed() && candidate.getConnection() != connection) {
                    matchedOpponent = candidate;
                    if (candidate.getPreferredRole() == PlayerRole.PLANT) {
                        assignedOpponentRole = PlayerRole.PLANT;
                        assignedMyRole = PlayerRole.ZOMBIE;
                    } else if (candidate.getPreferredRole() == PlayerRole.ZOMBIE) {
                        assignedOpponentRole = PlayerRole.ZOMBIE;
                        assignedMyRole = PlayerRole.PLANT;
                    } else {
                        assignedMyRole = preferredRole == PlayerRole.ZOMBIE ? PlayerRole.ZOMBIE : PlayerRole.PLANT;
                        assignedOpponentRole = assignedMyRole == PlayerRole.PLANT ? PlayerRole.ZOMBIE : PlayerRole.PLANT;
                    }
                    it.remove();
                    break;
                }
            }
        }

        // 5. Establish room if matched, or enqueue
        if (matchedOpponent != null) {
            ClientConnectionHandler plantConn = assignedMyRole == PlayerRole.PLANT ? connection : matchedOpponent.getConnection();
            ClientConnectionHandler zombieConn = assignedMyRole == PlayerRole.ZOMBIE ? connection : matchedOpponent.getConnection();

            System.out.println("[LobbyService] Match found in random queue: "
                    + (plantConn.getUsername() != null ? plantConn.getUsername() : "Plant") + " (PLANT) vs "
                    + (zombieConn.getUsername() != null ? zombieConn.getUsername() : "Zombie") + " (ZOMBIE)");

            roomManager.createRoom(plantConn, zombieConn);
        } else {
            randomQueue.add(new QueueEntry(connection, preferredRole));
            connection.sendPacket(new MatchmakingResponsePacket(MatchmakingStatus.QUEUED, null, "Waiting for opponent..."));
            System.out.println("[LobbyService] Queued player " + connection.getUsername() + " with preference " + preferredRole
                    + ". Queue size: " + randomQueue.size());
        }
    }

    /**
     * Generates a 6-character invite code and creates a private hosted room.
     */
    private synchronized void handleCreatePrivateRoom(ClientConnectionHandler connection, PlayerRole preferredRole) {
        removeHostedRoomsByConnection(connection);

        String code = generateUniqueRoomCode();
        PrivateRoom room = new PrivateRoom(code, connection, preferredRole);
        hostedPrivateRooms.put(code, room);

        connection.sendPacket(new MatchmakingResponsePacket(MatchmakingStatus.ROOM_CREATED, code,
                "Room created. Share this 6-character code with your opponent: " + code));
        System.out.println("[LobbyService] Private room " + code + " created by " + connection.getUsername());
    }

    /**
     * Joins a private room using a 6-character invite code.
     */
    private synchronized void handleDirectInviteJoin(
            ClientConnectionHandler guestConnection,
            String roomCode,
            PlayerRole guestPreferredRole
    ) {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            guestConnection.sendPacket(new MatchmakingResponsePacket(MatchmakingStatus.ERROR, null, "Room code cannot be empty."));
            return;
        }

        String normalizedCode = roomCode.trim().toUpperCase();
        PrivateRoom privateRoom = hostedPrivateRooms.remove(normalizedCode);

        if (privateRoom == null) {
            guestConnection.sendPacket(new MatchmakingResponsePacket(MatchmakingStatus.ERROR, normalizedCode,
                    "Room not found or expired: " + normalizedCode));
            return;
        }

        ClientConnectionHandler hostConnection = privateRoom.getHostConnection();
        if (hostConnection == null || hostConnection.isClosed()) {
            guestConnection.sendPacket(new MatchmakingResponsePacket(MatchmakingStatus.ERROR, normalizedCode, "Host has disconnected."));
            return;
        }

        if (hostConnection == guestConnection) {
            hostedPrivateRooms.put(normalizedCode, privateRoom);
            guestConnection.sendPacket(new MatchmakingResponsePacket(MatchmakingStatus.ERROR, normalizedCode, "Cannot join your own room."));
            return;
        }

        // Resolve roles
        PlayerRole hostRole = privateRoom.getHostRole();
        PlayerRole assignedHostRole;
        PlayerRole assignedGuestRole;

        if (hostRole == PlayerRole.PLANT) {
            assignedHostRole = PlayerRole.PLANT;
            assignedGuestRole = PlayerRole.ZOMBIE;
        } else if (hostRole == PlayerRole.ZOMBIE) {
            assignedHostRole = PlayerRole.ZOMBIE;
            assignedGuestRole = PlayerRole.PLANT;
        } else if (guestPreferredRole == PlayerRole.PLANT) {
            assignedGuestRole = PlayerRole.PLANT;
            assignedHostRole = PlayerRole.ZOMBIE;
        } else if (guestPreferredRole == PlayerRole.ZOMBIE) {
            assignedGuestRole = PlayerRole.ZOMBIE;
            assignedHostRole = PlayerRole.PLANT;
        } else {
            assignedHostRole = PlayerRole.PLANT;
            assignedGuestRole = PlayerRole.ZOMBIE;
        }

        ClientConnectionHandler plantConn = assignedHostRole == PlayerRole.PLANT ? hostConnection : guestConnection;
        ClientConnectionHandler zombieConn = assignedHostRole == PlayerRole.ZOMBIE ? hostConnection : guestConnection;

        System.out.println("[LobbyService] Direct invite room " + normalizedCode + " matched: "
                + plantConn.getUsername() + " (PLANT) vs " + zombieConn.getUsername() + " (ZOMBIE)");

        roomManager.createRoom(plantConn, zombieConn);
    }

    /**
     * Cancels an in-flight matchmaking queue ticket or private room hosting.
     */
    public synchronized void cancelMatchmaking(ClientConnectionHandler connection, CancelMatchmakingPacket packet) {
        if (connection == null) return;

        boolean removedFromQueue = false;
        for (Iterator<QueueEntry> it = randomQueue.iterator(); it.hasNext(); ) {
            QueueEntry entry = it.next();
            if (entry.getConnection() == connection) {
                it.remove();
                removedFromQueue = true;
            }
        }

        boolean removedFromRooms = removeHostedRoomsByConnection(connection);

        if (removedFromQueue || removedFromRooms) {
            if (!connection.isClosed()) {
                connection.sendPacket(new MatchmakingResponsePacket(MatchmakingStatus.CANCELLED, null, "Matchmaking cancelled."));
            }
            System.out.println("[LobbyService] Cancelled matchmaking for " + connection.getUsername());
        }
    }

    /**
     * Cleans up all lobby entries and in-flight invites associated with a disconnected client.
     */
    public void handleDisconnect(ClientConnectionHandler connection) {
        if (connection == null) return;
        cancelMatchmaking(connection, null);
        handleDisconnectInvites(connection);
    }

    private synchronized void handleDisconnectInvites(ClientConnectionHandler connection) {
        for (Iterator<Map.Entry<String, PendingInvite>> it = pendingInvites.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, PendingInvite> entry = it.next();
            PendingInvite pending = entry.getValue();
            if (pending.getInviterConnection() == connection) {
                if (pending.getTimeoutFuture() != null) {
                    pending.getTimeoutFuture().cancel(false);
                }
                it.remove();
                if (!pending.getTargetConnection().isClosed()) {
                    pending.getTargetConnection().sendPacket(new InviteStatusPacket(pending.getInviteId(), InviteStatus.OFFLINE, "Inviter disconnected."));
                    pending.getTargetConnection().sendPacket(new CancelInvitePacket(pending.getInviteId(), pending.getTargetUsername()));
                }
            } else if (pending.getTargetConnection() == connection) {
                if (pending.getTimeoutFuture() != null) {
                    pending.getTimeoutFuture().cancel(false);
                }
                it.remove();
                if (!pending.getInviterConnection().isClosed()) {
                    pending.getInviterConnection().sendPacket(new InviteStatusPacket(pending.getInviteId(), InviteStatus.OFFLINE, "Target user disconnected."));
                }
            }
        }
    }

    private boolean isPlayerInQueue(ClientConnectionHandler connection) {
        for (QueueEntry entry : randomQueue) {
            if (entry.getConnection() == connection) {
                return true;
            }
        }
        return false;
    }

    private void purgeClosedQueueEntries() {
        randomQueue.removeIf(entry -> entry.getConnection().isClosed());
    }

    private boolean removeHostedRoomsByConnection(ClientConnectionHandler connection) {
        boolean removed = false;
        for (Map.Entry<String, PrivateRoom> entry : hostedPrivateRooms.entrySet()) {
            if (entry.getValue().getHostConnection() == connection) {
                hostedPrivateRooms.remove(entry.getKey());
                removed = true;
            }
        }
        return removed;
    }

    private String generateUniqueRoomCode() {
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder sb = new StringBuilder(ROOM_CODE_LENGTH);
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                int idx = RANDOM.nextInt(ROOM_CODE_ALPHABET.length());
                sb.append(ROOM_CODE_ALPHABET.charAt(idx));
            }
            String code = sb.toString();
            if (!hostedPrivateRooms.containsKey(code)) {
                return code;
            }
        }
        return "RM" + (System.currentTimeMillis() % 10000);
    }

    // --- Inspection Getters ---

    public synchronized int getQueueSize() {
        purgeClosedQueueEntries();
        return randomQueue.size();
    }

    public synchronized List<QueueEntry> getQueueEntries() {
        purgeClosedQueueEntries();
        return Collections.unmodifiableList(new ArrayList<>(randomQueue));
    }

    public int getHostedRoomCount() {
        return hostedPrivateRooms.size();
    }

    public Map<String, PrivateRoom> getHostedPrivateRooms() {
        return Collections.unmodifiableMap(hostedPrivateRooms);
    }

    public Map<String, PendingInvite> getPendingInvites() {
        return Collections.unmodifiableMap(pendingInvites);
    }

    public PendingInvite getPendingInvite(String inviteId) {
        return inviteId != null ? pendingInvites.get(inviteId) : null;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }
}
