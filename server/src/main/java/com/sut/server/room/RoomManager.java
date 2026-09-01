package com.sut.server.room;

import com.sut.server.net.ClientConnectionHandler;
import com.sut.server.net.PacketRouter;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.game.PlacePlantRequestPacket;
import model.network.packet.game.PlaceZombieRequestPacket;
import model.network.packet.game.PlayerActionResponsePacket;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages active multiplayer I, Zombie game rooms, ticks scheduled physics loops (20 Hz),
 * and routes game action packets to the appropriate authoritative room.
 */
public class RoomManager {

    private final Map<String, IZombieGameRoom> activeRooms = new ConcurrentHashMap<>();
    private final Map<ClientConnectionHandler, IZombieGameRoom> playerToRoomMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutor;

    public RoomManager() {
        this(Executors.newScheduledThreadPool(4, new NamedThreadFactory("pvz-room-tick")));
    }

    public RoomManager(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = Objects.requireNonNull(scheduledExecutor, "scheduledExecutor cannot be null");
    }

    /**
     * Registers game packet routes and connection closed listeners with the server's PacketRouter.
     */
    public void registerRoutes(PacketRouter router) {
        if (router == null) return;

        router.registerHandler(PlacePlantRequestPacket.class, (conn, packet) -> {
            IZombieGameRoom room = getRoomForPlayer(conn);
            if (room != null) {
                room.handlePlantAction(conn, packet);
            } else {
                conn.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "NOT_IN_ROOM",
                        packet.getRow(), packet.getCol()));
            }
        });

        router.registerHandler(PlaceZombieRequestPacket.class, (conn, packet) -> {
            IZombieGameRoom room = getRoomForPlayer(conn);
            if (room != null) {
                room.handleZombieAction(conn, packet);
            } else {
                conn.sendPacket(new PlayerActionResponsePacket(false, "PLACE_ZOMBIE", "NOT_IN_ROOM",
                        packet.getRow(), packet.getCol()));
            }
        });

        router.registerHandler(ReactionPacket.class, (conn, packet) -> {
            IZombieGameRoom room = getRoomForPlayer(conn);
            if (room != null) {
                room.handleReaction(conn, packet);
            }
        });

        router.addConnectionClosedListener(this::handlePlayerDisconnect);
    }

    /**
     * Creates, registers, and starts a new authoritative IZombieGameRoom for two paired clients.
     */
    public IZombieGameRoom createRoom(ClientConnectionHandler plantPlayer, ClientConnectionHandler zombiePlayer) {
        String roomId = "room-" + UUID.randomUUID().toString().substring(0, 8);
        IZombieGameRoom room = new IZombieGameRoom(roomId, this, plantPlayer, zombiePlayer);

        activeRooms.put(roomId, room);
        if (plantPlayer != null) {
            playerToRoomMap.put(plantPlayer, room);
        }
        if (zombiePlayer != null) {
            playerToRoomMap.put(zombiePlayer, room);
        }

        room.start(scheduledExecutor);
        System.out.println("[RoomManager] Created and started room " + roomId
                + " (Plant: " + (plantPlayer != null ? plantPlayer.getUsername() : "null")
                + ", Zombie: " + (zombiePlayer != null ? zombiePlayer.getUsername() : "null") + ")");
        return room;
    }

    public IZombieGameRoom getRoom(String roomId) {
        if (roomId == null) return null;
        return activeRooms.get(roomId);
    }

    public IZombieGameRoom getRoomForPlayer(ClientConnectionHandler player) {
        if (player == null) return null;
        IZombieGameRoom room = playerToRoomMap.get(player);
        if (room != null) return room;

        // Fallback check by roomId if present on connection context
        String roomId = player.getCurrentRoomId();
        if (roomId != null) {
            room = activeRooms.get(roomId);
            if (room != null) {
                playerToRoomMap.put(player, room);
                return room;
            }
        }
        return null;
    }

    public synchronized void removeRoom(String roomId) {
        if (roomId == null) return;
        IZombieGameRoom room = activeRooms.remove(roomId);
        if (room != null) {
            if (room.getPlantPlayer() != null) {
                playerToRoomMap.remove(room.getPlantPlayer());
                if (roomId.equals(room.getPlantPlayer().getCurrentRoomId())) {
                    room.getPlantPlayer().setCurrentRoomId(null);
                }
            }
            if (room.getZombiePlayer() != null) {
                playerToRoomMap.remove(room.getZombiePlayer());
                if (roomId.equals(room.getZombiePlayer().getCurrentRoomId())) {
                    room.getZombiePlayer().setCurrentRoomId(null);
                }
            }
            System.out.println("[RoomManager] Removed room " + roomId);
        }
    }

    public void handlePlayerDisconnect(ClientConnectionHandler disconnectedPlayer) {
        if (disconnectedPlayer == null) return;
        IZombieGameRoom room = getRoomForPlayer(disconnectedPlayer);
        if (room != null) {
            room.handlePlayerDisconnect(disconnectedPlayer);
        }
    }

    public int getActiveRoomCount() {
        return activeRooms.size();
    }

    public Collection<IZombieGameRoom> getActiveRooms() {
        return Collections.unmodifiableCollection(activeRooms.values());
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    /**
     * Gracefully shuts down all active game rooms and the scheduler executor.
     */
    public synchronized void shutdown() {
        System.out.println("[RoomManager] Shutting down all " + activeRooms.size() + " active rooms...");
        for (IZombieGameRoom room : activeRooms.values()) {
            try {
                room.stop();
            } catch (Exception e) {
                System.err.println("[RoomManager] Error stopping room " + room.getRoomId() + ": " + e.getMessage());
            }
        }
        activeRooms.clear();
        playerToRoomMap.clear();

        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
        }
        System.out.println("[RoomManager] RoomManager shutdown complete.");
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
