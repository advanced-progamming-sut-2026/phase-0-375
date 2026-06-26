package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.SecurityQuestion;
import model.user.PasswordHasher;

public class LoginMenuController extends AppMenuController {
    private static LoginMenuController instance = null;

    // Pending forgot-password state
    private String pendingResetUsername;

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

    public CommandResult<Void> login(String username, String password, boolean stayLoggedIn) {
        if (username == null || username.trim().isEmpty())
            return CommandResult.error("Username cannot be empty.");
        if (password == null || password.isEmpty())
            return CommandResult.error("Password cannot be empty.");

        var opt = App.getInstance().getUserRepository().authenticate(username, PasswordHasher.hash(password));
        if (opt.isEmpty())
            return CommandResult.error("Invalid username or password.");

        var user = opt.get();
        if (stayLoggedIn) {
            user.setStayLoggedIn(true);
            App.getInstance().getUserRepository().flush();
        }
        App.getInstance().setCurrentUser(user);
        App.getInstance().setCurrentMenu(MenuType.MAIN);
        return CommandResult.success("Welcome back, " + user.getNickname() + "!");
    }

    public CommandResult<Void> forgetPassword(String username, String email) {
        if (username == null || username.trim().isEmpty())
            return CommandResult.error("Username cannot be empty.");
        if (email == null || email.trim().isEmpty())
            return CommandResult.error("Email cannot be empty.");

        var opt = App.getInstance().getUserRepository().findByUsername(username);
        if (opt.isEmpty())
            return CommandResult.error("Username not found.");

        var user = opt.get();
        if (!user.getEmail().equalsIgnoreCase(email.trim()))
            return CommandResult.error("Email does not match this username.");

        // Store for the answer step
        this.pendingResetUsername = username;

        SecurityQuestion q = SecurityQuestion.fromNumber(user.getSecurityQuestionNumber());
        String questionText = (q != null) ? q.getText() : "Security question";
        return CommandResult.success(questionText);
    }

    public CommandResult<Void> answer(String answer) {
        if (pendingResetUsername == null)
            return CommandResult.error("No password reset in progress.");

        boolean ok = App.getInstance().getUserRepository()
                .verifySecurityAnswer(pendingResetUsername, answer);
        if (!ok) {
            pendingResetUsername = null;
            return CommandResult.error("Incorrect answer. Returning to login.");
        }

        // Answer correct — signal view to capture new password
        return CommandResult.success("Correct! Enter your new password:");
    }

    /**
     * Called after the user enters a new password during forgot-password flow.
     */
    public CommandResult<Void> resetPassword(String newPassword) {
        if (pendingResetUsername == null)
            return CommandResult.error("No password reset in progress.");

        if (newPassword == null || newPassword.length() < 8)
            return CommandResult.error("Password must be at least 8 characters.");

        String hash = PasswordHasher.hash(newPassword);
        App.getInstance().getUserRepository().updatePassword(pendingResetUsername, hash);
        App.getInstance().getUserRepository().flush();
        pendingResetUsername = null;
        return CommandResult.success("Password updated! Please log in.");
    }
}
