package com.sut.server;

import com.sut.server.net.PacketRouter;
import com.sut.server.net.TcpServer;
import com.sut.server.repository.ServerUserRepository;
import com.sut.server.room.RoomManager;
import com.sut.server.service.AuthService;
import com.sut.server.service.DailyOfferService;
import com.sut.server.service.LobbyService;
import com.sut.server.service.UserService;
import model.data.minigame.MiniGameRegistry;
import model.plant.PlantFactory;
import model.zombie.ZombieFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main entry point for the Plants vs. Zombies 2 dedicated authoritative game server.
 */
public class ServerLauncher {

    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_HOST = "0.0.0.0";

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final CountDownLatch SHUTDOWN_LATCH = new CountDownLatch(1);
    private static ServerUserRepository userRepository;
    private static AuthService authService;
    private static UserService userService;
    private static RoomManager roomManager;
    private static LobbyService lobbyService;
    private static PacketRouter packetRouter;
    private static TcpServer tcpServer;

    public static void main(String[] args) {
        int port = resolvePort(args);
        String host = resolveHost(args);
        printBanner(port, host);
        RUNNING.set(true);
        try {
            runServer(host, port);
        } catch (IOException e) {
            System.err.println("[Server] Fatal error binding server to port " + port + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[Server] Unexpected fatal error during server runtime: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runServer(String host, int port) throws IOException {
        initCatalogs();
        userRepository = new ServerUserRepository();
        authService = new AuthService(userRepository);
        DailyOfferService dailyOfferService = new DailyOfferService();
        userService = new UserService(userRepository, authService, dailyOfferService);
        roomManager = new RoomManager();
        lobbyService = new LobbyService(roomManager);
        packetRouter = new PacketRouter();
        authService.registerRoutes(packetRouter);
        userService.registerRoutes(packetRouter);
        lobbyService.registerRoutes(packetRouter);
        roomManager.registerRoutes(packetRouter);
        tcpServer = new TcpServer(host, port, packetRouter, TcpServer.createDefaultObjectMapper());
        lobbyService.setTcpServer(tcpServer);
        lobbyService.setAuthService(authService);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Server] Shutdown hook invoked. Stopping server...");
            stop();
        }, "pvz-server-shutdown-hook"));
        tcpServer.start();
        System.out.println("[Server] Dedicated TCP Server successfully started on " + host + ":" + port);
        System.out.println(
                "[Server] Type 'stop' or 'exit' to terminate, 'status' for statistics,"
                        + " or 'help' for commands.\n");
        handleConsoleInput();
    }


    /**
     * Resolves the server listening port from CLI arguments, System property, Env var, or default.
     * Supports positional arguments, --port <port>, and --port=<port>.
     */
    public static int resolvePort(String[] args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i].trim();
                if (arg.startsWith("--port=")) {
                    try {
                        return Integer.parseInt(arg.substring("--port=".length()).trim());
                    } catch (NumberFormatException ignored) {}
                } else if (arg.equals("--port") || arg.equals("-p")) {
                    if (i + 1 < args.length) {
                        try {
                            return Integer.parseInt(args[i + 1].trim());
                        } catch (NumberFormatException ignored) {}
                    }
                } else if (!arg.startsWith("-")) {
                    try {
                        return Integer.parseInt(arg);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        String sysProp = System.getProperty("pvz.server.port");
        if (sysProp == null || sysProp.isBlank()) {
            sysProp = System.getProperty("server.port");
        }
        if (sysProp != null && !sysProp.isBlank()) {
            try {
                return Integer.parseInt(sysProp.trim());
            } catch (NumberFormatException ignored) {}
        }
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_PORT;
    }

    /**
     * Resolves the host binding address.
     * Supports positional arguments, --host <host>, and --host=<host>.
     */
    public static String resolveHost(String[] args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i].trim();
                if (arg.startsWith("--host=")) {
                    return arg.substring("--host=".length()).trim();
                } else if (arg.equals("--host") || arg.equals("-h")) {
                    if (i + 1 < args.length) {
                        return args[i + 1].trim();
                    }
                } else if (i == 1 && !arg.startsWith("-")) {
                    return arg;
                }
            }
        }
        String sysHost = System.getProperty("pvz.server.host");
        if (sysHost != null && !sysHost.isBlank()) {
            return sysHost.trim();
        }
        return DEFAULT_HOST;
    }

    /**
     * Prints startup banner and system metadata.
     */
    private static void printBanner(int port, String host) {
        System.out.println("==================================================================");
        System.out.println("   Plants vs. Zombies 2 — Dedicated Authoritative Server          ");
        System.out.println("==================================================================");
        System.out.println(" [Server] Java Version : " + System.getProperty("java.version"));
        System.out.println(" [Server] Java Vendor  : " + System.getProperty("java.vendor"));
        System.out.println(" [Server] Working Dir  : " + System.getProperty("user.dir"));
        System.out.println(" [Server] Bind Host    : " + host);
        System.out.println(" [Server] Port         : " + port);
        System.out.println("==================================================================");
    }

    /**
     * Handles console input if a terminal is available; otherwise awaits shutdown signal.
     */
    private static void handleConsoleInput() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (RUNNING.get()) {
                if (!reader.ready()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }
                String line = reader.readLine();
                if (line == null) {
                    // Stdin reached EOF (e.g. background daemon mode, test harness, or piped input)
                    break;
                }
                line = line.trim();
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("stop") || line.equalsIgnoreCase("quit")) {
                    System.out.println("[Server] Stop command received from console.");
                    stop();
                    break;
                } else if (line.equalsIgnoreCase("status")) {
                    printStatus();
                } else if (line.equalsIgnoreCase("help")) {
                    printHelp();
                } else if (!line.isEmpty()) {
                    System.out.println("[Server] Unknown command: '" + line + "'. Type 'help' for available commands.");
                }
            }
        } catch (IOException e) {
            // Error reading console input
        }

        // If stdin closed (background daemon / non-interactive), wait on SHUTDOWN_LATCH
        try {
            SHUTDOWN_LATCH.await();
        } catch (InterruptedException ignored) {}
    }

    private static void printStatus() {
        System.out.println("[Server Status]");
        System.out.println("  - Running: " + RUNNING.get());
        if (tcpServer != null) {
            System.out.println("  - Server Port: " + tcpServer.getBoundPort());
            System.out.println("  - Connected Clients: " + tcpServer.getActiveConnectionCount());
        }
        long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        System.out.println("  - Memory: " + (totalMem - freeMem) + "MB used / " + maxMem + "MB max");
    }

    private static void printHelp() {
        System.out.println("[Server Available Commands]");
        System.out.println("  status - Display server health, connected clients, and memory statistics");
        System.out.println("  help   - Show this command list");
        System.out.println("  stop   - Gracefully shut down server and disconnect clients");
        System.out.println("  exit   - Alias for stop");
    }

    private static void initCatalogs() {
        try {
            PlantFactory.init("/assets/data/plants/plants.json");
        } catch (Exception e) {
            System.err.println("[Server] Warning: Could not initialize PlantFactory: " + e.getMessage());
        }
        try {
            ZombieFactory.init("/assets/data/zombies/zombies.json", "/assets/data/armor/ArmorTypeData.json");
        } catch (Exception e) {
            System.err.println("[Server] Warning: Could not initialize ZombieFactory: " + e.getMessage());
        }
        try {
            MiniGameRegistry.init("/assets/data/minigames/minigames.json");
        } catch (Exception e) {
            System.err.println("[Server] Warning: Could not initialize MiniGameRegistry: " + e.getMessage());
        }
    }

    /**
     * Gracefully stops the server and releases all socket resources.
     */
    public static synchronized void stop() {
        if (!RUNNING.compareAndSet(true, false)) {
            return;
        }
        System.out.println("[Server] Shutting down RoomManager and rooms...");
        if (roomManager != null) {
            roomManager.shutdown();
        }
        System.out.println("[Server] Shutting down TCP Server...");
        if (tcpServer != null) {
            tcpServer.stop();
        }
        SHUTDOWN_LATCH.countDown();
        System.out.println("[Server] Server stopped successfully.");
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static TcpServer getTcpServer() {
        return tcpServer;
    }

    public static ServerUserRepository getUserRepository() {
        return userRepository;
    }

    public static AuthService getAuthService() {
        return authService;
    }

    public static RoomManager getRoomManager() {
        return roomManager;
    }

    public static LobbyService getLobbyService() {
        return lobbyService;
    }

    public static PacketRouter getPacketRouter() {
        return packetRouter;
    }
}

