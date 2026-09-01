package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.network.client.NetworkClient;
import model.network.packet.Packet;
import model.network.packet.auth.RegisterRequestPacket;
import model.network.packet.auth.RegisterResponsePacket;
import model.network.packet.auth.RegisterValidateRequestPacket;

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
     * Step 1: validate profile fields on the server. Fails if the server is unreachable.
     */
    public CommandResult<Void> register(String username, String password, String passwordConfirm,
                                         String nickname, String email, String gender) {
        if (password == null || passwordConfirm == null || !password.equals(passwordConfirm)) {
            return CommandResult.error("Password and confirmation do not match.");
        }

        NetworkClient client;
        try {
            client = App.getInstance().ensureConnected();
        } catch (Exception e) {
            return CommandResult.error("Cannot register: server is unreachable.");
        }
        if (client == null || !client.isConnected()) {
            return CommandResult.error("Cannot register: server is unreachable.");
        }

        RegisterValidateRequestPacket validatePacket = new RegisterValidateRequestPacket(
                username != null ? username.trim() : "",
                password,
                nickname != null ? nickname.trim() : "",
                email != null ? email.trim() : "",
                gender != null ? gender.trim() : ""
        );
        RegisterResponsePacket resp = exchangeRegisterResponse(client, validatePacket);
        if (resp == null) {
            return CommandResult.error("Cannot register: no response from server.");
        }
        if (!resp.isSuccess()) {
            return CommandResult.error(resp.getMessage() != null ? resp.getMessage() : "Registration validation failed.");
        }

        this.pendingUsername = validatePacket.getUsername();
        this.pendingPassword = password;
        this.pendingNickname = validatePacket.getNickname();
        this.pendingEmail = validatePacket.getEmail();
        this.pendingGender = validatePacket.getGender() != null ? validatePacket.getGender().toLowerCase() : "";

        return CommandResult.success(resp.getMessage() != null
                ? resp.getMessage()
                : "All fields validated. Now choose a security question.");
    }

    /**
     * Step 2: send registration to the server. Fails if the server is unreachable.
     * Does not persist any user data on the client.
     */
    public CommandResult<Void> pickQuestion(int questionNumber, String answer, String answerConfirm) {
        if (pendingUsername == null)
            return CommandResult.error("No registration in progress. Start with register command first.");

        if (answer == null || answerConfirm == null || !answer.equals(answerConfirm)) {
            return CommandResult.error("Answers do not match.");
        }

        NetworkClient client;
        try {
            client = App.getInstance().ensureConnected();
        } catch (Exception e) {
            return CommandResult.error("Cannot register: server is unreachable.");
        }
        if (client == null || !client.isConnected()) {
            return CommandResult.error("Cannot register: server is unreachable.");
        }

        RegisterRequestPacket regPacket = new RegisterRequestPacket(
                pendingUsername,
                pendingPassword,
                pendingNickname,
                pendingEmail,
                pendingGender,
                questionNumber,
                answer.trim()
        );
        RegisterResponsePacket resp = exchangeRegisterResponse(client, regPacket);
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

    private RegisterResponsePacket exchangeRegisterResponse(NetworkClient client, Packet request) {
        AtomicReference<RegisterResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<RegisterResponsePacket> handler = responseRef::set;

        boolean prevAutoPost = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(RegisterResponsePacket.class, handler);

        try {
            if (!client.sendPacket(request)) {
                return null;
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
        return responseRef.get();
    }

    private void clearPending() {
        pendingUsername = null;
        pendingPassword = null;
        pendingNickname = null;
        pendingEmail = null;
        pendingGender = null;
    }
}
