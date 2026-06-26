package view;

import controller.ProfileMenuController;
import controller.result.CommandResult;
import model.command.ProfileMenuCommand;
import model.user.User;

public class ProfileMenuView extends AppMenuView {
    private static ProfileMenuView instance;

    public static ProfileMenuView getInstance() {
        if (instance == null) instance = new ProfileMenuView();
        return instance;
    }

    private final ProfileMenuController controller = ProfileMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (ProfileMenuCommand.CHANGE_USERNAME.matches(input)) {
            String username = ProfileMenuCommand.CHANGE_USERNAME.getParameter("username");
            changeUsername(username);
        } else if (ProfileMenuCommand.CHANGE_NICKNAME.matches(input)) {
            String nickname = ProfileMenuCommand.CHANGE_NICKNAME.getParameter("nickname");
            changeNickname(nickname);
        } else if (ProfileMenuCommand.CHANGE_EMAIL.matches(input)) {
            String email = ProfileMenuCommand.CHANGE_EMAIL.getParameter("email");
            changeEmail(email);
        } else if (ProfileMenuCommand.CHANGE_PASSWORD.matches(input)) {
            String newPass = ProfileMenuCommand.CHANGE_PASSWORD.getParameter("new_password");
            String oldPass = ProfileMenuCommand.CHANGE_PASSWORD.getParameter("old_password");
            changePassword(newPass, oldPass);
        } else if (ProfileMenuCommand.SHOW_INFO.matches(input)) {
            showInfo();
        } else {
            displayError("Usage:");
            displayError("  menu profile change-username -u <username>");
            displayError("  menu profile change-nickname -u <nickname>");
            displayError("  menu profile change-email -e <email>");
            displayError("  menu profile change-password -p <new> -o <old>");
            displayError("  menu profile show-info");
        }
    }

    public void changeUsername(String username) {
        CommandResult<Void> result = controller.changeUsername(username);
        displayCommandResult(result);
    }

    public void changeNickname(String nickname) {
        CommandResult<Void> result = controller.changeNickname(nickname);
        displayCommandResult(result);
    }

    public void changeEmail(String email) {
        CommandResult<Void> result = controller.changeEmail(email);
        displayCommandResult(result);
    }

    public void changePassword(String newPassword, String oldPassword) {
        CommandResult<Void> result = controller.changePassword(newPassword, oldPassword);
        displayCommandResult(result);
    }

    public void showInfo() {
        CommandResult<User> result = controller.showInfo();
        if (result.isSuccess()) {
            User u = result.getData();
            displayMessage("── Profile Info ──");
            displayMessage("  Username:      " + u.getUsername());
            displayMessage("  Nickname:      " + u.getNickname());
            displayMessage("  Email:         " + u.getEmail());
            displayMessage("  Gender:        " + u.getGender());
            displayMessage("  Games Played:  " + u.getGamesPlayed());
            displayMessage("  Coins:         " + u.getCoins());
            displayMessage("  Gems:          " + u.getGems());
            displayMessage("  Difficulty:    " + u.getDifficultyLevel());
            displayMessage("  Highest Score: " + u.getHighestMyopoint());
            int levelsCompleted = u.getChapterProgress().values().stream()
                    .mapToInt(Integer::intValue).sum();
            displayMessage("  Levels Done:   " + levelsCompleted);
            displayMessage("──────────────────");
        } else {
            displayError(result.getMessage());
        }
    }
}
