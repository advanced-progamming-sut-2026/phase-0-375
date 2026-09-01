package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.network.client.NetworkClient;
import model.network.packet.auth.RegisterRequestPacket;
import model.network.packet.auth.RegisterResponsePacket;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class RegisterMenuController extends AppMenuController {
    private static RegisterMenuController instance = null;

    // Pending registration data (step 1 → step 2). Authoritative validation is on the server.
    private String pendingUsername;
    private String pendingPassword;
    private String pendingNickname;
    private String pendingEmail;
    private String pendingGender;

    private RegisterMenuController() {}

    public static RegisterMenuController getInstance() {
        if (instance == null) instance = new RegisterMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        if (menuName.equalsIgnoreCase("login")) {
            if (pendingUsername != null) {
                return CommandResult.error(
                        "Complete your security question first, or start over with a new register command."
                );
            }
            App.getInstance().setCurrentMenu(MenuType.LOGIN);
            return CommandResult.success("Entered login menu.");
        }
        return CommandResult.error("Cannot go to '" + menuName + "' from register menu.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        return CommandResult.success("Exiting application.");
    }

    /**
     * Step 1: stash form fields. Uniqueness, password strength, and email format
     * are enforced by the server on step 2.
     */
    public CommandResult<Void> register(String username, String password, String passwordConfirm,
                                         String nickname, String email, String gender) {
        if (username == null || username.trim().isEmpty())
            return CommandResult.error("Username cannot be empty.");
        if (password == null || password.isEmpty())
            return CommandResult.error("Password cannot be empty.");
        if (!password.equals(passwordConfirm))
            return CommandResult.error("Password and confirmation do not match.");
        if (nickname == null || nickname.trim().isEmpty())
            return CommandResult.error("Nickname cannot be empty.");
        if (email == null || email.trim().isEmpty())
            return CommandResult.error("Email cannot be empty.");
        String g = gender != null ? gender.toLowerCase() : "";
        if (!g.equals("male") && !g.equals("female"))
            return CommandResult.error("Gender must be 'male' or 'female'.");

        this.pendingUsername = username.trim();
        this.pendingPassword = password;
        this.pendingNickname = nickname.trim();
        this.pendingEmail = email.trim();
        this.pendingGender = g;

        return CommandResult.success("All fields validated. Now choose a security question.");
    }

    /**
     * Step 2: send registration to the server. Fails if the server is unreachable.
     * Does not persist any user data on the client.
     */
    public CommandResult<Void> pickQuestion(int questionNumber, String answer, String answerConfirm) {
        if (pendingUsername == null)
            return CommandResult.error("No registration in progress. Start with register command first.");

        if (questionNumber < 1 || questionNumber > 5)
            return CommandResult.error("Question number must be 1-5.");
        if (answer == null || answer.trim().isEmpty())
            return CommandResult.error("Answer cannot be empty.");
        if (!answer.equals(answerConfirm))
            return CommandResult.error("Answers do not match.");

        NetworkClient client;
        try {
            client = App.getInstance().ensureConnected();
        } catch (Exception e) {
            return CommandResult.error("Cannot register: server is unreachable.");
        }
        if (client == null || !client.isConnected()) {
            return CommandResult.error("Cannot register: server is unreachable.");
        }

        AtomicReference<RegisterResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<RegisterResponsePacket> handler = responseRef::set;

        boolean prevAutoPost = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(RegisterResponsePacket.class, handler);

        try {
            // Send raw password so the server can enforce complexity and hash authoritatively.
            RegisterRequestPacket regPacket = new RegisterRequestPacket(
                    pendingUsername,
                    pendingPassword,
                    pendingNickname,
                    pendingEmail,
                    pendingGender,
                    questionNumber,
                    answer.trim()
            );
            boolean sent = client.sendPacket(regPacket);
            if (!sent) {
                return CommandResult.error("Cannot register: failed to reach the server.");
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
            client.unregisterHandler(RegisterResponsePacket.class, handler);
            client.setAutoPostToGdx(prevAutoPost);
        }

        RegisterResponsePacket resp = responseRef.get();
        if (resp == null) {
            return CommandResult.error("Cannot register: no response from server.");
        }
        if (!resp.isSuccess()) {
            return CommandResult.error(resp.getMessage() != null ? resp.getMessage() : "Registration failed.");
        }

        clearPending();
        App.getInstance().setCurrentMenu(MenuType.LOGIN);
        return CommandResult.success("Registration successful! Redirecting to login.");
    }

    private void clearPending() {
        pendingUsername = null;
        pendingPassword = null;
        pendingNickname = null;
        pendingEmail = null;
        pendingGender = null;
    }
}
