package view;

import controller.RegisterMenuController;
import controller.result.CommandResult;
import model.enums.SecurityQuestion;

public class RegisterMenuView extends AppMenuView {
    private static RegisterMenuView instance = null;

    public static RegisterMenuView getInstance() {
        if (instance == null) instance = new RegisterMenuView();
        return instance;
    }

    private final RegisterMenuController controller = RegisterMenuController.getInstance();
    private boolean awaitingSecurityAnswer = false;

    @Override
    public void processInput(String input) {
        if (awaitingSecurityAnswer) {
            if (model.command.RegisterMenuCommand.PICK_QUESTION.matches(input)) {
                int questionNumber = Integer.parseInt(
                    model.command.RegisterMenuCommand.PICK_QUESTION.getParameter("question_number"));
                String answer = model.command.RegisterMenuCommand.PICK_QUESTION.getParameter("answer");
                String answerConfirm = model.command.RegisterMenuCommand.PICK_QUESTION.getParameter("answer_confirm");
                pickQuestion(questionNumber, answer, answerConfirm);
            } else {
                displayError("Please choose a security question:");
                displayError("  pick question -q <number> -a <answer> -c <confirm>");
            }
        } else {
            if (model.command.RegisterMenuCommand.REGISTER.matches(input)) {
                String username = model.command.RegisterMenuCommand.REGISTER.getParameter("username");
                String password = model.command.RegisterMenuCommand.REGISTER.getParameter("password");
                String passwordConfirm = model.command.RegisterMenuCommand.REGISTER.getParameter("password_confirm");
                String nickname = model.command.RegisterMenuCommand.REGISTER.getParameter("nickname");
                String email = model.command.RegisterMenuCommand.REGISTER.getParameter("email");
                String gender = model.command.RegisterMenuCommand.REGISTER.getParameter("gender");
                register(username, password, passwordConfirm, nickname, email, gender);
            } else {
                displayError("Usage:");
                displayError("  register -u <username> -p <password> <password_confirm> -n <nickname> -e <email> -g <gender>");
                displayError("  menu enter login");
            }
        }
    }

    public void register(String username, String password, String passwordConfirm,
                         String nickname, String email, String gender) {
        CommandResult<Void> result = controller.register(username, password, passwordConfirm,
                nickname, email, gender);
        if (result.isSuccess()) {
            displayMessage("All fields look good!");
            displayMessage("Now choose a security question:");
            displaySecurityQuestions();
            awaitingSecurityAnswer = true;
        } else {
            displayError(result.getMessage());
        }
    }

    public void pickQuestion(int questionNumber, String answer, String answerConfirm) {
        CommandResult<Void> result = controller.pickQuestion(questionNumber, answer, answerConfirm);
        if (result.isSuccess()) {
            displayMessage(result.getMessage());
            awaitingSecurityAnswer = false;
        } else {
            displayError(result.getMessage());
        }
    }

    private void displaySecurityQuestions() {
        displayMessage("Available security questions:");
        SecurityQuestion[] questions = SecurityQuestion.values();
        for (int i = 0; i < questions.length; i++) {
            displayMessage("  " + (i + 1) + ". " + questions[i].getText());
        }
        displayMessage("");
        displayMessage("  pick question -q <number> -a <answer> -c <confirm>");
    }
}
