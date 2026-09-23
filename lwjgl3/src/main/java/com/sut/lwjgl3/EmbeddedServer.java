package com.sut.lwjgl3;

import com.sut.server.ServerLauncher;
import model.app.App;
import model.network.client.NetworkClient;
import model.network.client.ServerEndpoint;
import model.network.packet.system.HeartbeatPacket;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts the dedicated {@code :server} module in-process so login/register work from a single client.jar.
 * If the preferred port is occupied by a non-responsive process, tries the next free ports.
 */
final class EmbeddedServer {
    private static final int MAX_PORT_ATTEMPTS = 10;

    private EmbeddedServer() {}

    static void startIfAvailable() {
        String host = "127.0.0.1";
        int preferred = ServerEndpoint.port();
        if (preferred <= 0) {
            preferred = ServerLauncher.DEFAULT_PORT;
        }

        try {
            int port = bindOrReuse(host, preferred);
            App.getInstance().setServerEndpoint(host, port);
            System.setProperty("pvz.client.host", host);
            System.setProperty("pvz.client.port", Integer.toString(port));
            System.setProperty("pvz.server.host", host);
            System.setProperty("pvz.server.port", Integer.toString(port));
            System.out.println("[Client] Game client will use server at " + host + ":" + port);
        } catch (Throwable t) {
            System.err.println("[Client] Could not start embedded server: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static int bindOrReuse(String host, int preferred) {
        for (int i = 0; i < MAX_PORT_ATTEMPTS; i++) {
            int port = preferred + i;
            boolean started = ServerLauncher.startEmbedded(host, port);
            if (started && ServerLauncher.isRunning()) {
                System.out.println("[Client] Embedded server listening on " + host + ":" + port);
                return port;
            }
            // Bind failed — another process owns this port. Only reuse it if it speaks our protocol.
            if (probeResponsive(host, port)) {
                System.out.println(
                        "[Client] Reusing healthy existing server on " + host + ":" + port);
                return port;
            }
            System.err.println(
                    "[Client] Port " + port + " is occupied by a non-responsive process; trying next...");
        }
        throw new IllegalStateException(
                "Could not bind or find a healthy PvZ server near port " + preferred);
    }

    /** Returns true if a PvZ server on host:port answers a heartbeat within ~1.5s. */
    private static boolean probeResponsive(String host, int port) {
        NetworkClient client = new NetworkClient(host, port);
        client.setAutoPostToGdx(false);
        AtomicBoolean gotPong = new AtomicBoolean(false);
        try {
            client.connect(800);
            client.registerHandler(HeartbeatPacket.class, hb -> {
                if (hb != null && hb.isPong()) {
                    gotPong.set(true);
                }
            });
            if (!client.sendPacket(new HeartbeatPacket(System.currentTimeMillis()))) {
                return false;
            }
            long deadline = System.currentTimeMillis() + 1500;
            while (System.currentTimeMillis() < deadline && !gotPong.get() && client.isConnected()) {
                client.pollEvents();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return gotPong.get();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }
}
