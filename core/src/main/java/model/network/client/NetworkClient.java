package model.network.client;

import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.network.packet.Packet;
import model.network.packet.game.GameStateSnapshotPacket;
import model.network.packet.system.HeartbeatPacket;
import model.network.util.NetworkJsonMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * High-performance, thread-safe TCP socket client for Plants vs. Zombies 2 multiplayer.
 * <p>
 * Features:
 * <ul>
 *   <li>Dedicated daemon background reader thread for Newline-Delimited JSON (NDJSON) streaming.</li>
 *   <li>Synchronized write-lock protection preventing packet tearing.</li>
 *   <li>Polymorphic typed packet handler registry with O(1) concurrent dispatch.</li>
 *   <li>Dual UI thread synchronization ({@code Gdx.app.postRunnable} + {@code pollEvents()}).</li>
 *   <li>Thread-safe state snapshot caching ({@link #getLatestSnapshot()}).</li>
 *   <li>Built-in heartbeat ping-pong responder.</li>
 * </ul>
 */
public class NetworkClient implements Closeable {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_TIMEOUT_MS = 5000;

    private String host;
    private int port;
    private final ObjectMapper objectMapper;

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final Object writeLock = new Object();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private Thread readerThread;

    // Polymorphic Packet Handlers
    private final Map<Class<? extends Packet>, List<Consumer<? extends Packet>>> handlers = new ConcurrentHashMap<>();
    private final List<Consumer<Packet>> globalListeners = new CopyOnWriteArrayList<>();
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();

    // Snapshot Caching
    private final AtomicReference<GameStateSnapshotPacket> latestSnapshot = new AtomicReference<>(null);
    private volatile long lastSnapshotTimestamp = 0L;

    // Thread Synchronization & Event Queue
    private final Queue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();
    private boolean autoPostToGdx = true;

    public NetworkClient() {
        this(DEFAULT_HOST, DEFAULT_PORT, NetworkJsonMapper.getMapper());
    }

    public NetworkClient(String host, int port) {
        this(host, port, NetworkJsonMapper.getMapper());
    }

    public NetworkClient(String host, int port, ObjectMapper objectMapper) {
        this.host = (host != null && !host.isBlank()) ? host : DEFAULT_HOST;
        this.port = port > 0 ? port : DEFAULT_PORT;
        this.objectMapper = (objectMapper != null) ? objectMapper : NetworkJsonMapper.getMapper();

        // Built-in automatic heartbeat responder
        registerHandler(HeartbeatPacket.class, hb -> {
            if (!hb.isPong()) {
                sendPacket(new HeartbeatPacket(hb.getClientTimestamp(), System.currentTimeMillis(), true));
            }
        });
    }

    /**
     * Connects to the configured host and port with default timeout.
     */
    public synchronized void connect() throws IOException {
        connect(DEFAULT_TIMEOUT_MS);
    }

    /**
     * Connects to the configured host and port with specified timeout in milliseconds.
     */
    public synchronized void connect(int timeoutMillis) throws IOException {
        if (state.get() == ConnectionState.CONNECTED || running.get()) {
            return;
        }

        state.set(ConnectionState.CONNECTING);
        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            running.set(true);
            state.set(ConnectionState.CONNECTED);

            readerThread = new Thread(this::readLoop, "NetworkClient-Reader-" + host + ":" + port);
            readerThread.setDaemon(true);
            readerThread.start();

            notifyConnected();
        } catch (IOException e) {
            state.set(ConnectionState.ERROR);
            cleanupSocket();
            notifyError(e);
            throw e;
        }
    }

    /**
     * Connects to an explicit target host and port.
     */
    public synchronized void connect(String host, int port) throws IOException {
        connect(host, port, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Connects to an explicit target host and port with timeout.
     */
    public synchronized void connect(String host, int port, int timeoutMillis) throws IOException {
        this.host = (host != null && !host.isBlank()) ? host : DEFAULT_HOST;
        this.port = port > 0 ? port : DEFAULT_PORT;
        connect(timeoutMillis);
    }

    /**
     * Dedicated background daemon loop parsing Newline-Delimited JSON (NDJSON) packets.
     */
    private void readLoop() {
        try {
            String line;
            while (running.get() && socket != null && !socket.isClosed() && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // Skip blank lines (keep-alives)
                }

                try {
                    Packet packet = objectMapper.readValue(line, Packet.class);
                    if (packet != null) {
                        handleInboundPacket(packet);
                    }
                } catch (Exception parseException) {
                    System.err.println("[WARN] [NetworkClient] Malformed packet received: " + line
                            + " (" + parseException.getMessage() + ")");
                }
            }
        } catch (SocketException e) {
            // Normal remote drop / socket close
        } catch (IOException e) {
            if (running.get()) {
                System.err.println("[WARN] [NetworkClient] IO exception in read loop: " + e.getMessage());
            }
        } finally {
            boolean wasExpected = (state.get() == ConnectionState.DISCONNECTING);
            disconnectInternal("Socket Closed or EOF", wasExpected);
        }
    }

    /**
     * Intercepts snapshots and dispatches inbound packets to registered handlers.
     */
    private void handleInboundPacket(Packet packet) {
        if (packet instanceof GameStateSnapshotPacket snapshot) {
            latestSnapshot.set(snapshot);
            lastSnapshotTimestamp = System.currentTimeMillis();
        }

        Runnable dispatchTask = () -> dispatchToHandlers(packet);

        if (autoPostToGdx && Gdx.app != null) {
            Gdx.app.postRunnable(dispatchTask);
        } else {
            eventQueue.offer(dispatchTask);
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatchToHandlers(Packet packet) {
        // Concrete class handlers
        List<Consumer<? extends Packet>> classHandlers = handlers.get(packet.getClass());
        if (classHandlers != null) {
            for (Consumer<? extends Packet> h : classHandlers) {
                try {
                    ((Consumer<Packet>) h).accept(packet);
                } catch (Exception e) {
                    System.err.println("[ERROR] [NetworkClient] Handler error for "
                            + packet.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        // Global listeners
        for (Consumer<Packet> global : globalListeners) {
            try {
                global.accept(packet);
            } catch (Exception e) {
                System.err.println("[ERROR] [NetworkClient] Global listener error: " + e.getMessage());
            }
        }
    }

    /**
     * Drains and executes all queued runnables.
     * Invoked from the LibGDX render loop or during headless testing.
     */
    public void pollEvents() {
        Runnable task;
        while ((task = eventQueue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println("[ERROR] [NetworkClient] Polled event execution error: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the count of currently queued events waiting to be polled.
     */
    public int getQueuedEventCount() {
        return eventQueue.size();
    }

    /**
     * Sends a packet to the server serialized as Newline-Delimited JSON (NDJSON).
     *
     * @param packet The packet DTO to serialize and transmit.
     * @return True if transmission succeeded, false otherwise.
     */
    public boolean sendPacket(Packet packet) {
        if (!isConnected() || packet == null) {
            return false;
        }

        try {
            String json = objectMapper.writeValueAsString(packet);
            synchronized (writeLock) {
                if (!isConnected()) return false;
                writer.write(json);
                writer.newLine();
                writer.flush();
                return true;
            }
        } catch (IOException e) {
            System.err.println("[ERROR] [NetworkClient] Failed to send packet "
                    + packet.getClass().getSimpleName() + ": " + e.getMessage());
            disconnectInternal("Write error: " + e.getMessage(), false);
            return false;
        }
    }

    /**
     * Registers a typed handler for a specific packet class.
     */
    public <T extends Packet> void registerHandler(Class<T> packetClass, Consumer<T> handler) {
        if (packetClass == null || handler == null) return;
        handlers.computeIfAbsent(packetClass, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /**
     * Unregisters a previously registered packet handler.
     */
    public <T extends Packet> void unregisterHandler(Class<T> packetClass, Consumer<T> handler) {
        if (packetClass == null || handler == null) return;
        List<Consumer<? extends Packet>> list = handlers.get(packetClass);
        if (list != null) {
            list.remove(handler);
        }
    }

    /**
     * Clears all registered packet handlers.
     */
    public void clearHandlers() {
        handlers.clear();
    }

    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null) connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        if (listener != null) connectionListeners.remove(listener);
    }

    public void addGlobalListener(Consumer<Packet> listener) {
        if (listener != null) globalListeners.add(listener);
    }

    public void removeGlobalListener(Consumer<Packet> listener) {
        if (listener != null) globalListeners.remove(listener);
    }

    public GameStateSnapshotPacket getLatestSnapshot() {
        return latestSnapshot.get();
    }

    public void setLatestSnapshot(GameStateSnapshotPacket snapshot) {
        this.latestSnapshot.set(snapshot);
        this.lastSnapshotTimestamp = System.currentTimeMillis();
    }

    public long getLastSnapshotTimestamp() {
        return lastSnapshotTimestamp;
    }

    public boolean isConnected() {
        return running.get() && state.get() == ConnectionState.CONNECTED && socket != null && !socket.isClosed();
    }

    public ConnectionState getState() {
        return state.get();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isAutoPostToGdx() {
        return autoPostToGdx;
    }

    public void setAutoPostToGdx(boolean autoPostToGdx) {
        this.autoPostToGdx = autoPostToGdx;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Initiates graceful client disconnection.
     */
    public synchronized void disconnect() {
        state.set(ConnectionState.DISCONNECTING);
        disconnectInternal("User requested disconnect", true);
    }

    @Override
    public void close() {
        disconnect();
    }

    private synchronized void disconnectInternal(String reason, boolean expected) {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        state.set(ConnectionState.DISCONNECTED);
        cleanupSocket();
        notifyDisconnected(reason, expected);
    }

    private void cleanupSocket() {
        synchronized (writeLock) {
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) {}
                writer = null;
            }
        }
        if (reader != null) {
            try { reader.close(); } catch (IOException ignored) {}
            reader = null;
        }
        if (socket != null && !socket.isClosed()) {
            try { socket.close(); } catch (IOException ignored) {}
            socket = null;
        }
    }

    private void notifyConnected() {
        for (ConnectionListener l : connectionListeners) {
            try { l.onConnected(); } catch (Exception ignored) {}
        }
    }

    private void notifyDisconnected(String reason, boolean expected) {
        for (ConnectionListener l : connectionListeners) {
            try { l.onDisconnected(reason, expected); } catch (Exception ignored) {}
        }
    }

    private void notifyError(Throwable cause) {
        for (ConnectionListener l : connectionListeners) {
            try { l.onError(cause); } catch (Exception ignored) {}
        }
    }
}
