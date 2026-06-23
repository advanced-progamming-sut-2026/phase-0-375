package controller;

import controller.result.CommandResult;

public class RegisterMenuController extends AppMenuController {
    private static RegisterMenuController instance = null;

    private RegisterMenuController() {}

    public static RegisterMenuController getInstance() {
        if (instance == null) instance = new RegisterMenuController();
        return instance;
    }


    public CommandResult<Void> register(String username, String password, String passwordConfirm,
                                  String nickname, String email, String gender) { return null; }
    public CommandResult<Void> pickQuestion(int questionNumber, String answer, String answerConfirm) { return null; }
}
