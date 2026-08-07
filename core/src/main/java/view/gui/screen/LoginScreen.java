package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.LoginMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.ui.ModalCard;

/**
 * Login form → {@link LoginMenuController}. Errors via toast; forgot-password as modal.
 */
public final class LoginScreen extends AbstractMenuScreen {
    private final LoginMenuController controller = LoginMenuController.getInstance();

    private TextField username;
    private TextField password;
    private CheckBox stayLoggedIn;

    // Forgot-password modal state
    private int resetStep; // 0 idle, 1 answer, 2 new password
    private Label resetPrompt;
    private TextField resetField;
    private TextButton resetAction;

    public LoginScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.LOGIN);

        BorderedTable card = new BorderedTable();
        card.pad(28f);
        Label title = new Label("Login", skin, "big");
        card.add(title).padBottom(16f).row();

        username = new TextField("", skin);
        username.setMessageText("Username");
        password = new TextField("", skin);
        password.setMessageText("Password");
        password.setPasswordMode(true);
        password.setPasswordCharacter('*');
        stayLoggedIn = new CheckBox(" Stay logged in", skin);

        card.add(username).width(360f).height(40f).padBottom(8f).row();
        card.add(password).width(360f).height(40f).padBottom(8f).row();
        card.add(stayLoggedIn).left().padBottom(12f).row();

        TextButton login = new TextButton("Login", skin);
        login.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitLogin();
            }
        });
        card.add(login).width(260f).height(52f).padBottom(8f).row();

        TextButton forgot = new TextButton("Forgot password", skin, "brown");
        forgot.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openForgotModal();
            }
        });
        card.add(forgot).width(260f).height(48f).padBottom(8f).row();

        TextButton toRegister = new TextButton("Create account", skin, "purple");
        toRegister.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuExit();
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new RegisterScreen(game));
                }
            }
        });
        card.add(toRegister).width(260f).height(48f);

        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(520f);
        stage.addActor(root);
    }

    private void submitLogin() {
        CommandResult<Void> result = controller.login(
                username.getText(),
                password.getText(),
                stayLoggedIn.isChecked());
        showToast(result.getMessage(), !result.isSuccess());
        if (result.isSuccess()) {
            game.setScreen(new MainHubScreen(game));
        }
    }

    private void openForgotModal() {
        resetStep = 0;
        Table body = new Table();
        TextField userField = new TextField(username.getText(), skin);
        userField.setMessageText("Username");
        TextField emailField = new TextField("", skin);
        emailField.setMessageText("Email");
        resetPrompt = new Label("Enter username and email to begin reset.", skin);
        resetPrompt.setWrap(true);
        resetField = new TextField("", skin);
        resetField.setVisible(false);
        resetAction = new TextButton("Continue", skin);

        body.add(resetPrompt).width(400f).padBottom(12f).row();
        body.add(userField).width(400f).height(48f).padBottom(8f).row();
        body.add(emailField).width(400f).height(48f).padBottom(8f).row();
        body.add(resetField).width(400f).height(48f).padBottom(12f).row();
        body.add(resetAction).width(220f).height(56f);

        Table overlay = ModalCard.create(skin, "Forgot password", body, () -> resetStep = 0);

        resetAction.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (resetStep == 0) {
                    CommandResult<Void> r = controller.forgetPassword(
                            userField.getText(), emailField.getText());
                    showToast(r.getMessage(), !r.isSuccess());
                    if (!r.isSuccess()) {
                        return;
                    }
                    resetStep = 1;
                    resetPrompt.setText("Security question:\n" + r.getMessage());
                    userField.setVisible(false);
                    emailField.setVisible(false);
                    resetField.setVisible(true);
                    resetField.setPasswordMode(false);
                    resetField.setText("");
                    resetField.setMessageText("Your answer");
                    resetAction.setText("Submit answer");
                } else if (resetStep == 1) {
                    CommandResult<Void> r = controller.answer(resetField.getText());
                    showToast(r.getMessage(), !r.isSuccess());
                    if (!r.isSuccess()) {
                        overlay.remove();
                        resetStep = 0;
                        return;
                    }
                    resetStep = 2;
                    resetPrompt.setText(r.getMessage());
                    resetField.setText("");
                    resetField.setPasswordMode(true);
                    resetField.setPasswordCharacter('*');
                    resetField.setMessageText("New password");
                    resetAction.setText("Update password");
                } else {
                    CommandResult<Void> r = controller.resetPassword(resetField.getText());
                    showToast(r.getMessage(), !r.isSuccess());
                    if (r.isSuccess()) {
                        overlay.remove();
                        resetStep = 0;
                    }
                }
            }
        });

        stage.addActor(overlay);
        toast.toFront();
    }
}
