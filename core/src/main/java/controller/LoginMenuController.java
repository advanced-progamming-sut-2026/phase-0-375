package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.network.client.NetworkClient;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
import model.user.PasswordHasher;
import model.user.User;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class LoginMenuController extends AppMenuController {
    private static LoginMenuController instance = null;

    private LoginMenuController() {}

    public static LoginMenuController getInstance() {
        if (instance == null) instance = new LoginMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("From login you can only log in or go back.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.REGISTER);
        return CommandResult.success("Returned to register menu.");
    }

    /**
     * Authenticates against the server only. No client-side user lookup or persistence.
     */
    public CommandResult<Void> login(String username, String password, boolean stayLoggedIn) {
        if (username == null || username.trim().isEmpty())
            return CommandResult.error("Username cannot be empty.");
        if (password == null || password.isEmpty())
            return CommandResult.error("Password cannot be empty.");

        String rawUsername = username.trim();
        String passwordHash = PasswordHasher.hash(password);

        NetworkClient client;
        try {
            client = App.getInstance().ensureConnected();
        } catch (Exception e) {
            return CommandResult.error("Cannot log in: server is unreachable.");
        }
        if (client == null || !client.isConnected()) {
            return CommandResult.error("Cannot log in: server is unreachable.");
        }

        AtomicReference<LoginResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<LoginResponsePacket> handler = responseRef::set;

        boolean prevAutoPost = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(LoginResponsePacket.class, handler);

        try {
            boolean sent = client.sendPacket(new LoginRequestPacket(rawUsername, passwordHash, stayLoggedIn));
            if (!sent) {
                return CommandResult.error("Cannot log in: failed to reach the server.");
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
        if (resp == null) {
            return CommandResult.error("Cannot log in: no response from server.");
        }
        if (!resp.isSuccess()) {
            return CommandResult.error(resp.getMessage() != null ? resp.getMessage() : "Invalid username or password.");
        }

        User serverUser = resp.getUserProfile();
        if (serverUser == null) {
            serverUser = new User();
            serverUser.setUsername(rawUsername);
        }
        if (stayLoggedIn && resp.getSessionToken() != null && !resp.getSessionToken().isBlank()) {
            serverUser.setStayLoggedIn(true);
            App.getInstance().applyStayLoggedInToken(serverUser.getUsername(), resp.getSessionToken());
        } else {
            App.getInstance().clearStayLoggedInToken();
        }
        App.getInstance().setCurrentUser(serverUser);
        if (client != null && client.isConnected()) {
            App.getInstance().setUserRepository(new model.user.persistance.RemoteUserRepository(client));
        }
        App.getInstance().setCurrentMenu(MenuType.MAIN);
        String nick = serverUser.getNickname() != null ? serverUser.getNickname() : serverUser.getUsername();
        return CommandResult.success("Welcome back, " + nick + "!");
    }

    public CommandResult<Void> forgetPassword(String username, String email) {
        return CommandResult.error("Password recovery must be handled by the server and is not available yet.");
    }

    public CommandResult<Void> answer(String answer) {
        return CommandResult.error("Password recovery must be handled by the server and is not available yet.");
    }

    public CommandResult<Void> resetPassword(String newPassword) {
        return CommandResult.error("Password recovery must be handled by the server and is not available yet.");
    }
}
