package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.network.client.NetworkClient;
import model.network.packet.user.PasswordChangeRequestPacket;
import model.network.packet.user.PasswordChangeResponsePacket;
import model.network.packet.user.PasswordResetRequestPacket;
import model.network.packet.user.PasswordResetResponsePacket;
import model.network.packet.user.ProfileUpdateRequestPacket;
import model.network.packet.user.ProfileUpdateResponsePacket;
import model.user.PasswordHasher;
import model.user.User;
import model.user.persistance.UserSync;
import view.gui.assets.AvatarArt;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ProfileMenuController extends AppMenuController {
    private static ProfileMenuController instance = null;

    private ProfileMenuController() {}

    public static ProfileMenuController getInstance() {
        if (instance == null) instance = new ProfileMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("No menus reachable from profile.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.MAIN);
        return CommandResult.success("Returned to main menu.");
    }

    private User user() {
        return App.getInstance().getCurrentUser();
    }

    private NetworkClient client() {
        return App.getInstance().getNetworkClient();
    }

    public CommandResult<Void> changeUsername(String username) {
        if (username == null || username.trim().isEmpty())
            return CommandResult.error("Username cannot be empty.");
        String trimmed = username.trim();
        if (user() == null)
            return CommandResult.error("Not logged in.");
        if (trimmed.equals(user().getUsername()))
            return CommandResult.error("New username is the same as the current one.");
        return sendProfileUpdate(trimmed, null, null);
    }

    public CommandResult<Void> changeNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty())
            return CommandResult.error("Nickname cannot be empty.");
        String trimmed = nickname.trim();
        if (user() == null)
            return CommandResult.error("Not logged in.");
        if (trimmed.equals(user().getNickname()))
            return CommandResult.error("New nickname is the same as the current one.");
        return sendProfileUpdate(null, trimmed, null);
    }

    public CommandResult<Void> changeEmail(String email) {
        if (email == null || email.trim().isEmpty())
            return CommandResult.error("Email cannot be empty.");
        String trimmed = email.trim();
        if (user() == null)
            return CommandResult.error("Not logged in.");
        if (trimmed.equalsIgnoreCase(user().getEmail()))
            return CommandResult.error("New email is the same as the current one.");
        return sendProfileUpdate(null, null, trimmed);
    }

    public CommandResult<Void> changePassword(String newPassword, String oldPassword) {
        if (newPassword == null || oldPassword == null)
            return CommandResult.error("Both old and new passwords are required.");
        if (user() == null)
            return CommandResult.error("Not logged in.");
        if (oldPassword.equals(newPassword))
            return CommandResult.error("New password must differ from the old one.");

        NetworkClient client = client();
        if (client == null || !client.isConnected()) {
            return CommandResult.error("Cannot change password: server is unreachable.");
        }

        AtomicReference<PasswordChangeResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<PasswordChangeResponsePacket> handler = responseRef::set;
        boolean prev = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(PasswordChangeResponsePacket.class, handler);
        try {
            client.sendPacket(new PasswordChangeRequestPacket(PasswordHasher.hash(oldPassword), newPassword));
            waitFor(responseRef, client);
        } finally {
            client.unregisterHandler(PasswordChangeResponsePacket.class, handler);
            client.setAutoPostToGdx(prev);
        }
        PasswordChangeResponsePacket resp = responseRef.get();
        if (resp == null) {
            return CommandResult.error("Cannot change password: no response from server.");
        }
        if (!resp.isSuccess()) {
            return CommandResult.error(resp.getMessage() != null ? resp.getMessage() : "Password change failed.");
        }
        user().setPasswordHash(PasswordHasher.hash(newPassword));
        return CommandResult.success("Password changed successfully.");
    }

    public CommandResult<User> showInfo() {
        return CommandResult.successWithData("Profile info retrieved.", user());
    }

    public CommandResult<Void> changeAvatar(int avatarId) {
        if (!AvatarArt.isValid(avatarId)) {
            return CommandResult.error("Invalid avatar.");
        }
        if (user() == null) {
            return CommandResult.error("Not logged in.");
        }
        if (user().getAvatarId() == avatarId) {
            return CommandResult.error("That is already your avatar.");
        }
        user().setAvatarId(avatarId);
        UserSync.flushIfLocal();
        return CommandResult.success("Avatar updated.");
    }

    /** Server-side password reset (no active session required). */
    public CommandResult<Void> resetPassword(String username, String email, String answer, String newPassword) {
        NetworkClient client;
        try {
            client = App.getInstance().ensureConnected();
        } catch (Exception e) {
            return CommandResult.error("Cannot reset password: server is unreachable.");
        }
        if (client == null || !client.isConnected()) {
            return CommandResult.error("Cannot reset password: server is unreachable.");
        }
        AtomicReference<PasswordResetResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<PasswordResetResponsePacket> handler = responseRef::set;
        boolean prev = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(PasswordResetResponsePacket.class, handler);
        try {
            client.sendPacket(new PasswordResetRequestPacket(username, email, answer, newPassword));
            waitFor(responseRef, client);
        } finally {
            client.unregisterHandler(PasswordResetResponsePacket.class, handler);
            client.setAutoPostToGdx(prev);
        }
        PasswordResetResponsePacket resp = responseRef.get();
        if (resp == null) {
            return CommandResult.error("Cannot reset password: no response from server.");
        }
        if (!resp.isSuccess()) {
            return CommandResult.error(resp.getMessage() != null ? resp.getMessage() : "Password reset failed.");
        }
        return CommandResult.success(resp.getMessage());
    }

    private CommandResult<Void> sendProfileUpdate(String username, String nickname, String email) {
        NetworkClient client = client();
        if (client == null || !client.isConnected()) {
            return CommandResult.error("Cannot update profile: server is unreachable.");
        }
        AtomicReference<ProfileUpdateResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<ProfileUpdateResponsePacket> handler = responseRef::set;
        boolean prev = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(ProfileUpdateResponsePacket.class, handler);
        try {
            client.sendPacket(new ProfileUpdateRequestPacket(username, nickname, email));
            waitFor(responseRef, client);
        } finally {
            client.unregisterHandler(ProfileUpdateResponsePacket.class, handler);
            client.setAutoPostToGdx(prev);
        }
        ProfileUpdateResponsePacket resp = responseRef.get();
        if (resp == null) {
            return CommandResult.error("Cannot update profile: no response from server.");
        }
        if (!resp.isSuccess()) {
            return CommandResult.error(resp.getMessage() != null ? resp.getMessage() : "Profile update failed.");
        }
        User updated = resp.getUser();
        if (updated != null) {
            User local = user();
            if (local != null && local.getPasswordHash() != null) {
                updated.setPasswordHash(local.getPasswordHash());
            }
            App.getInstance().setCurrentUser(updated);
        }
        return CommandResult.success(resp.getMessage() != null ? resp.getMessage() : "Profile updated.");
    }

    private static <T> void waitFor(AtomicReference<T> ref, NetworkClient client) {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline && ref.get() == null && client.isConnected()) {
            client.pollEvents();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
