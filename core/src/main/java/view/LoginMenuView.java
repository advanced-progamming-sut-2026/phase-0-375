package view;

import controller.LoginMenuController;
import controller.result.CommandResult;

public class LoginMenuView extends AppMenuView {
    private static LoginMenuView instance;

    public static LoginMenuView getInstance() {
        if (instance == null) instance = new LoginMenuView();
        return instance;
    }

    private final LoginMenuController controller = LoginMenuController.getInstance();

    // Forgot-password state machine
    private boolean awaitingSecurityAnswer = false;
    private boolean awaitingNewPassword = false;

    @Override
    public void processInput(String input) {
        if (awaitingNewPassword) {
            // Raw new password — no command format
            String newPassword = input.trim();
            if (newPassword.isEmpty()) {
                displayError("Password cannot be empty.");
                return;
            }
            CommandResult<Void> result = controller.resetPassword(newPassword);
            if (result.isSuccess()) {
                displayMessage(result.getMessage());
            } else {
                displayError(result.getMessage());
            }
            awaitingNewPassword = false;
            return;
        }

        if (awaitingSecurityAnswer) {
            if (model.command.LoginMenuCommand.ANSWER.matches(input)) {
                String answer = model.command.LoginMenuCommand.ANSWER.getParameter("answer");
                answerSecurity(answer);
            } else {
                displayError("Please answer the security question:");
                displayError("  answer -a <answer>");
            }
            return;
        }

        // Normal login menu state
        if (model.command.LoginMenuCommand.LOGIN.matches(input)) {
            String username = model.command.LoginMenuCommand.LOGIN.getParameter("username");
            String password = model.command.LoginMenuCommand.LOGIN.getParameter("password");
            boolean stayLoggedIn = input.contains("-stay-logged-in");
            login(username, password, stayLoggedIn);
        } else if (model.command.LoginMenuCommand.FORGET_PASSWORD.matches(input)) {
            String username = model.command.LoginMenuCommand.FORGET_PASSWORD.getParameter("username");
            String email = model.command.LoginMenuCommand.FORGET_PASSWORD.getParameter("email");
            forgetPassword(username, email);
        } else {
            displayError("Usage:");
            displayError("  login -u <username> -p <password> [-stay-logged-in]");
            displayError("  forget password -u <username> -e <email>");
        }
    }

    public void login(String username, String password, boolean stayLoggedIn) {
        CommandResult<Void> result = controller.login(username, password, stayLoggedIn);
        if (result.isSuccess()) {
            displayMessage(result.getMessage());
        } else {
            displayError(result.getMessage());
        }
    }

    public void forgetPassword(String username, String email) {
        CommandResult<Void> result = controller.forgetPassword(username, email);
        if (result.isSuccess()) {
            displayMessage("Security question:");
            displayMessage("  " + result.getMessage());
            displayMessage("Answer with:");
            displayMessage("  answer -a <answer>");
            awaitingSecurityAnswer = true;
        } else {
            displayError(result.getMessage());
        }
    }

    public void answerSecurity(String answer) {
        CommandResult<Void> result = controller.answer(answer);
        if (result.isSuccess()) {
            awaitingSecurityAnswer = false;
            awaitingNewPassword = true;
            displayMessage(result.getMessage());
        } else {
            awaitingSecurityAnswer = false;
            displayError(result.getMessage());
        }
    }
}
