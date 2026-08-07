package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import controller.RegisterMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.SecurityQuestion;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;

/**
 * Registration form → {@link RegisterMenuController} (fields + security question).
 */
public final class RegisterScreen extends AbstractMenuScreen {
    private static final float FIELD_WIDTH = 360f;
    private static final float FIELD_HEIGHT = 40f;
    private static final float CARD_MAX_HEIGHT = UI_HEIGHT - 64f;

    private final RegisterMenuController controller = RegisterMenuController.getInstance();

    private Table stepSecurity;
    private Cell<Table> contentCell;

    private TextField username;
    private TextField password;
    private TextField passwordConfirm;
    private TextField nickname;
    private TextField email;
    private SelectBox<String> gender;

    private SelectBox<String> question;
    private TextField answer;
    private TextField answerConfirm;

    public RegisterScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.REGISTER);

        stepSecurity = buildSecurityStep();

        BorderedTable card = new BorderedTable();
        card.pad(28f);
        card.add(new Label("Register", skin, "big")).padBottom(10f).row();
        contentCell = card.add(buildFieldsStep()).growX();
        contentCell.row();

        ScrollPane scroll = new ScrollPane(card, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        Table root = new Table();
        root.setFillParent(true);
        root.add(scroll).width(560f).maxHeight(CARD_MAX_HEIGHT);
        stage.addActor(root);
    }

    private Table buildFieldsStep() {
        Table t = new Table();
        username = field("Username");
        password = field("Password");
        password.setPasswordMode(true);
        password.setPasswordCharacter('*');
        passwordConfirm = field("Confirm password");
        passwordConfirm.setPasswordMode(true);
        passwordConfirm.setPasswordCharacter('*');
        nickname = field("Nickname");
        email = field("Email");
        gender = new SelectBox<>(skin);
        gender.setItems("male", "female");

        addField(t, username);
        addField(t, password);
        addField(t, passwordConfirm);
        addField(t, nickname);
        addField(t, email);
        t.add(gender).width(FIELD_WIDTH).height(FIELD_HEIGHT).padBottom(10f).row();

        TextButton next = new TextButton("Continue", skin);
        next.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.register(
                        username.getText(),
                        password.getText(),
                        passwordConfirm.getText(),
                        nickname.getText(),
                        email.getText(),
                        gender.getSelected());
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    contentCell.setActor(stepSecurity);
                }
            }
        });
        t.add(next).width(260f).height(52f).padBottom(8f).row();

        TextButton toLogin = new TextButton("I have an account", skin, "purple");
        toLogin.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuEnter("login");
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new LoginScreen(game));
                }
            }
        });
        t.add(toLogin).width(260f).height(48f);
        return t;
    }

    private Table buildSecurityStep() {
        Table t = new Table();
        t.add(new Label("Security question", skin, "medium")).padBottom(8f).row();

        question = new SelectBox<>(skin);
        Array<String> items = new Array<>();
        SecurityQuestion[] qs = SecurityQuestion.values();
        for (int i = 0; i < qs.length; i++) {
            items.add((i + 1) + ". " + qs[i].getText());
        }
        question.setItems(items);

        answer = field("Answer");
        answerConfirm = field("Confirm answer");

        t.add(question).width(440f).height(FIELD_HEIGHT).padBottom(8f).row();
        addField(t, answer);
        addField(t, answerConfirm);

        TextButton finish = new TextButton("Create account", skin);
        finish.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int qNumber = question.getSelectedIndex() + 1;
                CommandResult<Void> r = controller.pickQuestion(
                        qNumber, answer.getText(), answerConfirm.getText());
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new LoginScreen(game));
                }
            }
        });
        t.add(finish).width(260f).height(52f);
        return t;
    }

    private TextField field(String message) {
        TextField f = new TextField("", skin);
        f.setMessageText(message);
        return f;
    }

    private void addField(Table t, TextField field) {
        t.add(field).width(FIELD_WIDTH).height(FIELD_HEIGHT).padBottom(6f).row();
    }
}
