package model.app;

import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.network.client.NetworkClient;
import model.network.packet.auth.LoginResponsePacket;
import model.network.packet.auth.SessionResumeRequestPacket;
import model.user.User;
import model.user.persistance.LocalSessionStore;
import model.user.persistance.NullUserRepository;
import model.user.persistance.UserRepository;
import view.tui.TuiShell;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class App {
    private static App instance;

    private User currentUser;
    private MenuType currentMenu;
    private UserRepository userRepository;
    private NetworkClient networkClient;
    private String serverHost = NetworkClient.DEFAULT_HOST;
    private int serverPort = NetworkClient.DEFAULT_PORT;
    private Runnable onNetworkConnected;
    private final LocalSessionStore localSessionStore = new LocalSessionStore();
    /** In-memory stay-logged-in token for this process (also on disk when stay was checked). */
    private String sessionToken;

    // Game session state — set when starting a level, cleared on exit
    private GameModel currentGameModel;
    private PvZGameLoop currentGameLoop;

    private App() {
        this.currentUser = null;
        this.currentMenu = MenuType.REGISTER;
        // Never write accounts on the client disk; swap to RemoteUserRepository after login.
        this.userRepository = new NullUserRepository();
    }

    public static App getInstance() {
        if (instance == null) instance = new App();
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public MenuType getCurrentMenu() {
        return currentMenu;
    }

    public void setCurrentMenu(MenuType currentMenu) {
        this.currentMenu = currentMenu;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Keeps {@link RemoteUserRepository} on the live socket after reconnect. */
    private void syncUserRepositoryClient(NetworkClient client) {
        if (client == null || currentUser == null) {
            return;
        }
        if (userRepository instanceof model.user.persistance.RemoteUserRepository) {
            userRepository = new model.user.persistance.RemoteUserRepository(client);
        }
    }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public boolean isConnected() {
        return networkClient != null && networkClient.isConnected();
    }

    public void disconnectNetwork() {
        if (networkClient != null) {
            try {
                networkClient.disconnect();
            } catch (Exception ignored) {}
            networkClient = null;
        }
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerEndpoint(String host, int port) {
        this.serverHost = (host != null && !host.isBlank()) ? host : NetworkClient.DEFAULT_HOST;
        this.serverPort = port > 0 ? port : NetworkClient.DEFAULT_PORT;
    }

    public LocalSessionStore getLocalSessionStore() {
        return localSessionStore;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    /** Persist stay-logged-in token locally, or clear when token is null. */
    public void applyStayLoggedInToken(String username, String token) {
        this.sessionToken = token;
        if (token != null && username != null) {
            localSessionStore.save(username, token);
        } else {
            localSessionStore.clear();
        }
    }

    public void clearStayLoggedInToken() {
        this.sessionToken = null;
        localSessionStore.clear();
    }

    public void setOnNetworkConnected(Runnable onNetworkConnected) {
        this.onNetworkConnected = onNetworkConnected;
    }

    public NetworkClient ensureConnected() throws IOException {
        return ensureConnected(serverHost, serverPort, 3000);
    }

    public NetworkClient ensureConnected(String host, int port) throws IOException {
        return ensureConnected(host, port, 3000);
    }

    public NetworkClient ensureConnected(String host, int port, int timeoutMillis) throws IOException {
        if (networkClient != null && networkClient.isConnected()) {
            return networkClient;
        }
        if (networkClient != null) {
            disconnectNetwork();
        }
        NetworkClient client = new NetworkClient(host, port);
        client.connect(timeoutMillis);
        this.networkClient = client;
        syncUserRepositoryClient(client);
        resumeSessionIfLoggedIn(client);
        if (onNetworkConnected != null) {
            onNetworkConnected.run();
        }
        return client;
    }

    /**
     * Re-binds the TCP connection to the logged-in account after reconnect
     * (e.g. opening multiplayer matchmaking on a fresh socket).
     */
    private void resumeSessionIfLoggedIn(NetworkClient client) {
        if (client == null || !client.isConnected() || currentUser == null) {
            return;
        }
        String token = sessionToken;
        if (token == null || token.isBlank()) {
            var file = localSessionStore.load();
            if (file.isEmpty()) {
                return;
            }
            token = file.get().token;
        }
        if (token == null || token.isBlank()) {
            return;
        }

        AtomicReference<LoginResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<LoginResponsePacket> handler = responseRef::set;
        boolean prevAutoPost = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(LoginResponsePacket.class, handler);
        try {
            if (!client.sendPacket(new SessionResumeRequestPacket(token))) {
                return;
            }
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline && responseRef.get() == null && client.isConnected()) {
                client.pollEvents();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception ignored) {
            return;
        } finally {
            client.unregisterHandler(LoginResponsePacket.class, handler);
            client.setAutoPostToGdx(prevAutoPost);
        }

        LoginResponsePacket resp = responseRef.get();
        if (resp == null || !resp.isSuccess() || resp.getUserProfile() == null) {
            return;
        }
        User serverUser = resp.getUserProfile();
        serverUser.setStayLoggedIn(true);
        setCurrentUser(serverUser);
        String resumed = resp.getSessionToken() != null ? resp.getSessionToken() : token;
        applyStayLoggedInToken(serverUser.getUsername(), resumed);
        setUserRepository(new model.user.persistance.RemoteUserRepository(client));
    }

    /**
     * Resumes a stay-logged-in session from the local token file (or in-memory token).
     * Clears the local file on failure so the user is not stuck in a bad loop.
     */
    public boolean reconnectStayLoggedInSession() {
        String token = sessionToken;
        if (token == null || token.isBlank()) {
            var file = localSessionStore.load();
            if (file.isEmpty()) {
                return false;
            }
            token = file.get().token;
        }
        if (isConnected() && currentUser != null) {
            return true;
        }

        NetworkClient client;
        try {
            client = ensureConnected();
        } catch (Exception e) {
            return false;
        }
        if (client == null || !client.isConnected()) {
            return false;
        }

        AtomicReference<LoginResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<LoginResponsePacket> handler = responseRef::set;
        boolean prevAutoPost = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(LoginResponsePacket.class, handler);
        try {
            boolean sent = client.sendPacket(new SessionResumeRequestPacket(token));
            if (!sent) {
                return false;
            }
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline && responseRef.get() == null && client.isConnected()) {
                client.pollEvents();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            client.unregisterHandler(LoginResponsePacket.class, handler);
            client.setAutoPostToGdx(prevAutoPost);
        }

        LoginResponsePacket resp = responseRef.get();
        if (resp == null || !resp.isSuccess() || resp.getUserProfile() == null) {
            clearStayLoggedInToken();
            disconnectNetwork();
            return false;
        }

        User serverUser = resp.getUserProfile();
        serverUser.setStayLoggedIn(true);
        setCurrentUser(serverUser);
        String resumed = resp.getSessionToken() != null ? resp.getSessionToken() : token;
        applyStayLoggedInToken(serverUser.getUsername(), resumed);
        setUserRepository(new model.user.persistance.RemoteUserRepository(client));
        return true;
    }

    public GameModel getCurrentGameModel() {
        return currentGameModel;
    }

    public void setCurrentGameModel(GameModel currentGameModel) {
        this.currentGameModel = currentGameModel;
    }

    public PvZGameLoop getCurrentGameLoop() {
        return currentGameLoop;
    }

    public void setCurrentGameLoop(PvZGameLoop currentGameLoop) {
        this.currentGameLoop = currentGameLoop;
    }

    public static void logToShell(String message) {
        TuiShell.tryLog(message);
    }
}
