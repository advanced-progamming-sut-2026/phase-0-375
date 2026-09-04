package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.network.client.NetworkClient;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
import model.network.packet.user.PasswordResetRequestPacket;
import model.network.packet.user.PasswordResetResponsePacket;
import model.network.packet.user.SecurityQuestionRequestPacket;
import model.network.packet.user.SecurityQuestionResponsePacket;
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
        NetworkClient client = connectForLogin();
        if (client == null) {
            return CommandResult.error("Cannot log in: server is unreachable.");
        }
        LoginResponsePacket resp = requestLogin(client, username.trim(),
                PasswordHasher.hash(password), stayLoggedIn);
        return applyLoginResponse(client, resp, username.trim(), stayLoggedIn);
    }

    private NetworkClient connectForLogin() {
        try {
            NetworkClient client = App.getInstance().ensureConnected();
            if (client == null || !client.isConnected()) {
                return null;
            }
            return client;
        } catch (Exception e) {
            return null;
        }
    }

    private LoginResponsePacket requestLogin(NetworkClient client, String rawUsername,
                                             String passwordHash, boolean stayLoggedIn) {
        AtomicReference<LoginResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<LoginResponsePacket> handler = responseRef::set;
        boolean prevAutoPost = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(LoginResponsePacket.class, handler);
        try {
            boolean sent = client.sendPacket(
                    new LoginRequestPacket(rawUsername, passwordHash, stayLoggedIn));
            if (!sent) {
                return null;
            }
            pollUntil(client, responseRef, 3000);
        } finally {
            client.unregisterHandler(LoginResponsePacket.class, handler);
            client.setAutoPostToGdx(prevAutoPost);
        }
        return responseRef.get();
    }

    private static <T> void pollUntil(NetworkClient client,
                                      AtomicReference<T> responseRef, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline && responseRef.get() == null && client.isConnected()) {
            client.pollEvents();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private CommandResult<Void> applyLoginResponse(NetworkClient client, LoginResponsePacket resp,
                                                   String rawUsername, boolean stayLoggedIn) {
        if (resp == null) {
            return CommandResult.error("Cannot log in: no response from server.");
        }
        if (!resp.isSuccess()) {
            return CommandResult.error(resp.getMessage() != null
                    ? resp.getMessage() : "Invalid username or password.");
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
            App.getInstance().setUserRepository(
                    new model.user.persistance.RemoteUserRepository(client));
        }
        App.getInstance().setCurrentMenu(MenuType.MAIN);
        String nick = serverUser.getNickname() != null
                ? serverUser.getNickname() : serverUser.getUsername();
        return CommandResult.success("Welcome back, " + nick + "!");
    }

    private String pendingForgotUsername;
    private String pendingForgotEmail;
    private String pendingForgotAnswer;

    public void clearPendingForgotState() {
        pendingForgotUsername = null;
        pendingForgotEmail = null;
        pendingForgotAnswer = null;
    }

    public CommandResult<Void> forgetPassword(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            return CommandResult.error("Username cannot be empty.");
        }
        if (email == null || email.trim().isEmpty()) {
            return CommandResult.error("Email cannot be empty.");
        }
        NetworkClient client = connectForLogin();
        if (client == null) {
            return CommandResult.error("Cannot recover password: server is unreachable.");
        }
        AtomicReference<SecurityQuestionResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<SecurityQuestionResponsePacket> handler = responseRef::set;
        boolean prevAutoPost = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(SecurityQuestionResponsePacket.class, handler);
        try {
            boolean sent = client.sendPacket(
                    new SecurityQuestionRequestPacket(username.trim(), email.trim()));
            if (!sent) {
                return CommandResult.error("Cannot recover password: failed to send request.");
            }
            pollUntil(client, responseRef, 3000);
        } finally {
            client.unregisterHandler(SecurityQuestionResponsePacket.class, handler);
            client.setAutoPostToGdx(prevAutoPost);
        }
        SecurityQuestionResponsePacket resp = responseRef.get();
        if (resp == null) {
            return CommandResult.error("Cannot recover password: no response from server.");
        }
        if (!resp.isSuccess()) {
            return CommandResult.error(resp.getMessage() != null
                    ? resp.getMessage() : "User not found or email mismatch.");
        }
        pendingForgotUsername = username.trim();
        pendingForgotEmail = email.trim();
        pendingForgotAnswer = null;
        String question = resp.getQuestion() != null ? resp.getQuestion() : resp.getMessage();
        return CommandResult.successWithData(question, null);
    }

    public CommandResult<Void> answer(String answer) {
        if (pendingForgotUsername == null || pendingForgotEmail == null) {
            return CommandResult.error("Please enter username and email first.");
        }
        if (answer == null || answer.trim().isEmpty()) {
            return CommandResult.error("Security answer cannot be empty.");
        }
        pendingForgotAnswer = answer.trim();
        return CommandResult.success("Answer recorded. Please enter your new password.");
    }

    public CommandResult<Void> resetPassword(String newPassword) {
        if (pendingForgotUsername == null || pendingForgotEmail == null || pendingForgotAnswer == null) {
            return CommandResult.error("Please provide username, email, and security answer first.");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return CommandResult.error("Password cannot be empty.");
        }
        NetworkClient client = connectForLogin();
        if (client == null) {
            return CommandResult.error("Cannot reset password: server is unreachable.");
        }
        AtomicReference<PasswordResetResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<PasswordResetResponsePacket> handler = responseRef::set;
        boolean prevAutoPost = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(PasswordResetResponsePacket.class, handler);
        try {
            boolean sent = client.sendPacket(new PasswordResetRequestPacket(
                    pendingForgotUsername, pendingForgotEmail, pendingForgotAnswer, newPassword));
            if (!sent) {
                return CommandResult.error("Cannot reset password: failed to send request.");
            }
            pollUntil(client, responseRef, 3000);
        } finally {
            client.unregisterHandler(PasswordResetResponsePacket.class, handler);
            client.setAutoPostToGdx(prevAutoPost);
        }
        PasswordResetResponsePacket resp = responseRef.get();
        if (resp == null) {
            return CommandResult.error("Cannot reset password: no response from server.");
        }
        if (!resp.isSuccess()) {
            return CommandResult.error(resp.getMessage() != null ? resp.getMessage() : "Password reset failed.");
        }
        clearPendingForgotState();
        return CommandResult.success(resp.getMessage());
    }
}
