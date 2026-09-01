package com.sut.server.net;

import model.network.packet.system.ErrorMessagePacket;
import model.network.packet.system.HeartbeatPacket;
import model.network.packet.Packet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Type-safe packet dispatcher and connection lifecycle observer.
 */
public class PacketRouter {

    @FunctionalInterface
    public interface PacketHandler<T extends Packet> {
        void handle(ClientConnectionHandler connection, T packet) throws Exception;
    }

    @FunctionalInterface
    public interface ConnectionClosedListener {
        void onClosed(ClientConnectionHandler connection);
    }

    private final Map<Class<? extends Packet>, PacketHandler<?>> handlerMap = new ConcurrentHashMap<>();
    private final List<ConnectionClosedListener> closedListeners = new CopyOnWriteArrayList<>();

    public PacketRouter() {
        // Register default built-in heartbeat handler
        registerHandler(HeartbeatPacket.class, (conn, packet) -> {
            long clientTs = packet != null ? packet.getClientTimestamp() : 0L;
            conn.sendPacket(new HeartbeatPacket(clientTs, System.currentTimeMillis(), true));
        });
    }

    /**
     * Registers a typed handler for a specific Packet class.
     */
    @SuppressWarnings("unchecked")
    public <T extends Packet> void registerHandler(Class<T> packetClass, PacketHandler<T> handler) {
        handlerMap.put(packetClass, handler);
    }

    /**
     * Routes an inbound packet to its registered handler.
     */
    @SuppressWarnings("unchecked")
    public void route(ClientConnectionHandler connection, Packet packet) throws Exception {
        if (packet == null) {
            return;
        }

        PacketHandler<Packet> handler = (PacketHandler<Packet>) handlerMap.get(packet.getClass());
        if (handler != null) {
            handler.handle(connection, packet);
        } else {
            System.err.println("[WARN] [PacketRouter] No handler registered for packet type: "
                    + packet.getClass().getSimpleName() + " (type tag: " + packet.getType() + ")");
            connection.sendPacket(new ErrorMessagePacket("UNHANDLED_PACKET_TYPE",
                    "No handler registered on server for " + packet.getClass().getSimpleName()));
        }
    }

    /**
     * Registers a listener to be notified when any client disconnects.
     */
    public void addConnectionClosedListener(ConnectionClosedListener listener) {
        closedListeners.add(listener);
    }

    /**
     * Removes a registered connection closed listener.
     */
    public void removeConnectionClosedListener(ConnectionClosedListener listener) {
        closedListeners.remove(listener);
    }

    /**
     * Invoked by ClientConnectionHandler when a connection terminates.
     */
    public void onConnectionClosed(ClientConnectionHandler connection) {
        for (ConnectionClosedListener listener : closedListeners) {
            try {
                listener.onClosed(connection);
            } catch (Exception e) {
                System.err.println("[WARN] [PacketRouter] Error in ConnectionClosedListener: " + e.getMessage());
            }
        }
    }
}
