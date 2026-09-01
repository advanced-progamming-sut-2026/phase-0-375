package com.sut.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.network.packet.Packet;
import model.network.packet.system.ErrorMessagePacket;
import model.user.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles communication with a single connected client over TCP with Jackson NDJSON framing.
 */
public class ClientConnectionHandler implements Runnable {

    private final String connectionId;
    private final Socket socket;
    private final TcpServer server;
    private final PacketRouter router;
    private final ObjectMapper objectMapper;

    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Object writeLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    // Session Context State
    private volatile User userProfile;
    private volatile String username;
    private volatile String currentRoomId;
    private volatile long lastPacketReceivedTimestamp;
    private final long connectionEstablishedTimestamp;

    public ClientConnectionHandler(
            String connectionId,
            Socket socket,
            TcpServer server,
            PacketRouter router,
            ObjectMapper objectMapper
    ) throws IOException {
        this.connectionId = connectionId;
        this.socket = socket;
        this.server = server;
        this.router = router;
        this.objectMapper = objectMapper;

        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        this.connectionEstablishedTimestamp = System.currentTimeMillis();
        this.lastPacketReceivedTimestamp = connectionEstablishedTimestamp;
    }

    /**
     * Dedicated inbound reader loop running on worker thread.
     */
    @Override
    public void run() {
        try {
            String line;
            while (!closed.get() && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // Ignore blank keep-alive lines
                }

                this.lastPacketReceivedTimestamp = System.currentTimeMillis();

                try {
                    Packet packet = objectMapper.readValue(line, Packet.class);
                    if (packet != null) {
                        handlePacket(packet);
                    }
                } catch (Exception parseException) {
                    System.err.println("[WARN] [" + connectionId + "] Malformed packet JSON: " + line
                            + " (Error: " + parseException.getMessage() + ")");
                    sendPacket(new ErrorMessagePacket("MALFORMED_JSON",
                            "Could not parse JSON packet: " + parseException.getMessage()));
                }
            }
        } catch (SocketException e) {
            // Normal client socket drop or connection reset
        } catch (IOException e) {
            if (!closed.get()) {
                System.err.println("[WARN] [" + connectionId + "] IO error in client read loop: " + e.getMessage());
            }
        } finally {
            disconnect("Connection closed by remote peer or EOF reached");
        }
    }

    /**
     * Dispatches the parsed packet to the server's PacketRouter.
     */
    public void handlePacket(Packet packet) {
        try {
            router.route(this, packet);
        } catch (Exception e) {
            System.err.println("[ERROR] [" + connectionId + "] Error processing packet "
                    + packet.getClass().getSimpleName() + ": " + e.getMessage());
            sendPacket(new ErrorMessagePacket("INTERNAL_SERVER_ERROR",
                    "Error executing packet handler: " + e.getMessage()));
        }
    }

    /**
     * Thread-safe packet transmission over TCP using Newline-Delimited JSON framing.
     *
     * @param packet The packet DTO to serialize and send.
     * @return true if successfully written and flushed, false if connection closed or serialization failed.
     */
    public boolean sendPacket(Packet packet) {
        if (closed.get() || packet == null) {
            return false;
        }

        try {
            String json = objectMapper.writeValueAsString(packet);

            synchronized (writeLock) {
                if (closed.get()) {
                    return false;
                }
                writer.write(json);
                writer.newLine();
                writer.flush();
                return true;
            }
        } catch (IOException e) {
            System.err.println("[ERROR] [" + connectionId + "] Failed to send packet: " + e.getMessage());
            disconnect("Write failure: " + e.getMessage());
            return false;
        }
    }

    /**
     * Closes the connection cleanly and triggers session unregistration.
     */
    public void disconnect(String reason) {
        if (!closed.compareAndSet(false, true)) {
            return; // Already closed
        }

        System.out.println("[INFO] [ClientConnectionHandler] Disconnecting " + connectionId
                + (username != null ? " (" + username + ")" : "") + ". Reason: " + reason);

        // Notify router of connection drop (for game room / matchmaking cleanup)
        try {
            router.onConnectionClosed(this);
        } catch (Exception e) {
            System.err.println("[WARN] [" + connectionId + "] Error in onConnectionClosed hook: " + e.getMessage());
        }

        // Unregister from server active connections
        server.removeConnection(this);

        // Close I/O streams and socket
        synchronized (writeLock) {
            try {
                writer.close();
            } catch (IOException ignored) {}
        }

        try {
            reader.close();
        } catch (IOException ignored) {}

        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    // --- Session Getters & Setters ---

    public String getConnectionId() {
        return connectionId;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean isAuthenticated() {
        return userProfile != null;
    }

    public User getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(User userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) {
            this.username = userProfile.getUsername();
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public long getLastPacketReceivedTimestamp() {
        return lastPacketReceivedTimestamp;
    }

    public long getConnectionEstablishedTimestamp() {
        return connectionEstablishedTimestamp;
    }
}
