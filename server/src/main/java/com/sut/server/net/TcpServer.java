package com.sut.server.net;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.network.packet.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * High-performance TCP Socket Server managing client connections, JSON packet routing,
 * session tracking, and broadcast capabilities.
 */
public class TcpServer {

    private final String host;
    private final int port;
    private final PacketRouter packetRouter;
    private final ObjectMapper objectMapper;

    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread acceptorThread;
    private final ExecutorService clientExecutor;
    private final Map<String, ClientConnectionHandler> activeConnections = new ConcurrentHashMap<>();
    private Thread shutdownHook;

    public TcpServer(int port) {
        this("0.0.0.0", port, new PacketRouter(), createDefaultObjectMapper());
    }

    public TcpServer(String host, int port) {
        this(host, port, new PacketRouter(), createDefaultObjectMapper());
    }

    public TcpServer(String host, int port, PacketRouter packetRouter, ObjectMapper objectMapper) {
        this.host = host != null ? host : "0.0.0.0";
        this.port = port;
        this.packetRouter = packetRouter != null ? packetRouter : new PacketRouter();
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
        this.clientExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("TcpClient-IO"));
    }

    public static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        return mapper;
    }

    /**
     * Starts the TCP server socket and begins listening for client connections.
     */
    public synchronized void start() throws IOException {
        if (running.get()) {
            throw new IllegalStateException("TcpServer is already running on port " + getBoundPort());
        }

        this.serverSocket = new ServerSocket();
        this.serverSocket.setReuseAddress(true);
        this.serverSocket.bind(new InetSocketAddress(host, port), 50);
        this.running.set(true);

        this.acceptorThread = new Thread(this::acceptLoop, "TcpServer-Acceptor");
        this.acceptorThread.setDaemon(true);
        this.acceptorThread.start();

        // Register JVM graceful shutdown hook
        this.shutdownHook = new Thread(this::stop, "TcpServer-ShutdownHook");
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM might already be shutting down in some test scenarios
        }

        System.out.println(
                "[INFO] [TcpServer] Server successfully started and listening on "
                        + host + ":" + getBoundPort());
    }

    /**
     * Background accept loop running on dedicated acceptor thread.
     */
    private void acceptLoop() {
        while (running.get() && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true);
                clientSocket.setKeepAlive(true);

                String connectionId = "conn-" + UUID.randomUUID().toString().substring(0, 8);
                ClientConnectionHandler handler = new ClientConnectionHandler(
                        connectionId, clientSocket, this, packetRouter, objectMapper
                );

                registerConnection(handler);
                clientExecutor.submit(handler);

                System.out.println(
                        "[INFO] [TcpServer] Accepted connection " + connectionId
                                + " from " + clientSocket.getRemoteSocketAddress());
            } catch (SocketException e) {
                if (!running.get() || serverSocket == null || serverSocket.isClosed()) {
                    break; // Expected exception during graceful shutdown
                }
                System.err.println("[WARN] [TcpServer] SocketException in accept loop: " + e.getMessage());
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[ERROR] [TcpServer] IOException in accept loop: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gracefully stops the server, disconnects all active clients, and terminates worker threads.
     */
    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        System.out.println("[INFO] [TcpServer] Initiating graceful shutdown...");

        // 1. Unhook JVM shutdown hook if invoked programmatically
        if (shutdownHook != null && Thread.currentThread() != shutdownHook) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM already shutting down
            }
        }

        // 2. Close ServerSocket to unblock acceptor thread
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("[WARN] [TcpServer] Error closing server socket: " + e.getMessage());
            }
        }

        // 3. Disconnect all active client connections
        for (ClientConnectionHandler connection : activeConnections.values()) {
            try {
                connection.disconnect("Server shutting down");
            } catch (Exception e) {
                System.err.println("[WARN] [TcpServer] Error disconnecting client: " + e.getMessage());
            }
        }
        activeConnections.clear();

        // 4. Shutdown client executor
        clientExecutor.shutdown();
        try {
            if (!clientExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                clientExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("[INFO] [TcpServer] Server stopped successfully.");
    }

    public void registerConnection(ClientConnectionHandler connection) {
        if (connection != null) {
            activeConnections.put(connection.getConnectionId(), connection);
        }
    }

    public void removeConnection(ClientConnectionHandler connection) {
        if (connection != null) {
            activeConnections.remove(connection.getConnectionId());
        }
    }

    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    public int getClientCount() {
        return getActiveConnectionCount();
    }

    public Collection<ClientConnectionHandler> getActiveConnections() {
        return Collections.unmodifiableCollection(activeConnections.values());
    }

    public Optional<ClientConnectionHandler> getConnection(String connectionId) {
        if (connectionId == null) return Optional.empty();
        return Optional.ofNullable(activeConnections.get(connectionId));
    }

    public Optional<ClientConnectionHandler> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return activeConnections.values().stream()
                .filter(c -> username.equalsIgnoreCase(c.getUsername()))
                .findFirst();
    }

    /**
     * Broadcasts a packet to all connected clients.
     */
    public void broadcast(Packet packet) {
        if (packet == null) return;
        for (ClientConnectionHandler connection : activeConnections.values()) {
            connection.sendPacket(packet);
        }
    }

    /**
     * Broadcasts a packet to a filtered subset of clients.
     */
    public void broadcastTo(Predicate<ClientConnectionHandler> filter, Packet packet) {
        if (packet == null || filter == null) return;
        for (ClientConnectionHandler connection : activeConnections.values()) {
            if (filter.test(connection)) {
                connection.sendPacket(packet);
            }
        }
    }

    /**
     * Broadcasts a packet to all clients except the specified connection ID.
     */
    public void broadcastExcept(String excludeConnectionId, Packet packet) {
        if (excludeConnectionId == null) {
            broadcast(packet);
            return;
        }
        broadcastTo(conn -> !excludeConnectionId.equals(conn.getConnectionId()), packet);
    }

    public boolean isRunning() {
        return running.get() && serverSocket != null && !serverSocket.isClosed();
    }

    public int getBoundPort() {
        return (serverSocket != null && serverSocket.isBound()) ? serverSocket.getLocalPort() : port;
    }

    public int getPort() {
        return getBoundPort();
    }

    public String getHost() {
        return host;
    }

    public PacketRouter getPacketRouter() {
        return packetRouter;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
