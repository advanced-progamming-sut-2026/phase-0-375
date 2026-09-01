package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.PasswordHasher;
import model.user.User;
import model.user.persistance.UserRepository;
import view.gui.assets.AvatarArt;

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

    private UserRepository repo() {
        return App.getInstance().getUserRepository();
    }

    public CommandResult<Void> changeUsername(String username) {
        if (username == null || username.trim().isEmpty())
            return CommandResult.error("Username cannot be empty.");

        String trimmed = username.trim();

        if (trimmed.equals(user().getUsername()))
            return CommandResult.error("New username is the same as the current one.");

        if (!trimmed.matches("^[a-zA-Z0-9-]+$"))
            return CommandResult.error("Invalid username. Only letters, numbers, and hyphens allowed.");

        if (repo().existsByUsername(trimmed))
            return CommandResult.error("Username '" + trimmed + "' is already taken.");

        user().setUsername(trimmed);
        repo().flush();
        return CommandResult.success("Username changed to '" + trimmed + "'.");
    }

    public CommandResult<Void> changeNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty())
            return CommandResult.error("Nickname cannot be empty.");

        String trimmed = nickname.trim();

        if (trimmed.equals(user().getNickname()))
            return CommandResult.error("New nickname is the same as the current one.");

        if (trimmed.length() < 3 || trimmed.length() > 30)
            return CommandResult.error("Nickname must be between 3 and 30 characters.");

        user().setNickname(trimmed);
        repo().flush();
        return CommandResult.success("Nickname changed to '" + trimmed + "'.");
    }

    public CommandResult<Void> changeEmail(String email) {
        if (email == null || email.trim().isEmpty())
            return CommandResult.error("Email cannot be empty.");

        String trimmed = email.trim();

        if (trimmed.equalsIgnoreCase(user().getEmail()))
            return CommandResult.error("New email is the same as the current one.");

        if (repo().existsByEmail(trimmed))
            return CommandResult.error("Email '" + trimmed + "' is already in use.");

        String emailErr = validateEmail(trimmed);
        if (emailErr != null)
            return CommandResult.error("Invalid email: " + emailErr);

        user().setEmail(trimmed);
        repo().flush();
        return CommandResult.success("Email changed to '" + trimmed + "'.");
    }

    public CommandResult<Void> changePassword(String newPassword, String oldPassword) {
        if (newPassword == null || oldPassword == null)
            return CommandResult.error("Both old and new passwords are required.");

        if (!PasswordHasher.verify(oldPassword, user().getPasswordHash()))
            return CommandResult.error("Old password is incorrect.");

        if (oldPassword.equals(newPassword))
            return CommandResult.error("New password must differ from the old one.");

        String pwErr = validatePassword(newPassword);
        if (pwErr != null)
            return CommandResult.error("Weak password: " + pwErr);

        user().setPasswordHash(PasswordHasher.hash(newPassword));
        repo().flush();
        return CommandResult.success("Password changed successfully.");
    }

    public CommandResult<User> showInfo() {
        return CommandResult.successWithData("Profile info retrieved.", user());
    }

    public CommandResult<Void> changeAvatar(int avatarId) {
        if (!AvatarArt.isValid(avatarId)) {
            return CommandResult.error("Invalid avatar.");
        }
        if (user().getAvatarId() == avatarId) {
            return CommandResult.error("That is already your avatar.");
        }
        user().setAvatarId(avatarId);
        repo().flush();
        return CommandResult.success("Avatar updated.");
    }

    // ── Validation helpers ──

    private String validatePassword(String pw) {
        if (pw.length() < 8) return "minimum 8 characters.";
        if (!pw.matches(".*[a-z].*")) return "must include a lowercase letter.";
        if (!pw.matches(".*[A-Z].*")) return "must include an uppercase letter.";
        if (!pw.matches(".*\\d.*")) return "must include a digit.";
        if (!pw.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*"))
            return "must include a special character.";
        return null;
    }

    private String validateEmail(String email) {
        int at = email.indexOf('@');
        int lastAt = email.lastIndexOf('@');
        if (at == -1 || at != lastAt) return "must have exactly one '@'.";

        String local = email.substring(0, at);
        String domain = email.substring(at + 1);

        if (local.isEmpty()) return "local part cannot be empty.";
        if (!local.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]*[a-zA-Z0-9]$") && local.length() > 1)
            return "invalid local part.";
        if (local.length() == 1 && !local.matches("[a-zA-Z0-9]"))
            return "invalid local part.";
        if (local.contains("..")) return "local part cannot have consecutive dots.";

        if (domain.isEmpty()) return "domain cannot be empty.";
        if (!domain.matches("^[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$"))
            return "invalid domain.";
        if (domain.contains("..")) return "domain cannot have consecutive dots.";

        if (email.matches(".*[?><, \"';:\\\\/|\\[\\]{}()+*&^%$#!].*"))
            return "contains forbidden characters.";
        return null;
    }
}
