package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.ProfileMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.User;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.ui.ModalCard;
import view.gui.ui.ResourceBar;

/**
 * Profile info + edit flows via {@link ProfileMenuController}. Uses PvzSkin chrome.
 */
public final class ProfileScreen extends AbstractMenuScreen {
    private static final float FIELD_WIDTH = 400f;
    private static final float BUTTON_WIDTH = 280f;

    private final ProfileMenuController controller = ProfileMenuController.getInstance();

    private Label usernameValue;
    private Label nicknameValue;
    private Label emailValue;
    private Label gamesValue;
    private Label coinsValue;
    private Label gemsValue;
    private Label levelsValue;
    private Label myopointValue;
    private ResourceBar resourceBar;

    public ProfileScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.PROFILE);

        Table top = new Table();
        top.setFillParent(true);
        top.top();
        resourceBar = new ResourceBar(skin, game.assets != null ? game.assets.textures : null);
        top.add(resourceBar).expandX().right().pad(12f);
        stage.addActor(top);

        BorderedTable card = new BorderedTable();
        card.pad(28f);
        card.add(new Label("Profile", skin, "big")).padBottom(16f).row();

        Table info = new Table();
        usernameValue = addInfoRow(info, "Username");
        nicknameValue = addInfoRow(info, "Nickname");
        emailValue = addInfoRow(info, "Email");
        gamesValue = addInfoRow(info, "Games played");
        coinsValue = addInfoRow(info, "Coins");
        gemsValue = addInfoRow(info, "Gems");
        levelsValue = addInfoRow(info, "Levels completed");
        myopointValue = addInfoRow(info, "Highest myopoint");
        card.add(info).growX().padBottom(20f).row();

        card.add(editButton("Change username", this::openChangeUsername)).width(BUTTON_WIDTH).height(48f)
                .padBottom(8f).row();
        card.add(editButton("Change nickname", this::openChangeNickname)).width(BUTTON_WIDTH).height(48f)
                .padBottom(8f).row();
        card.add(editButton("Change email", this::openChangeEmail)).width(BUTTON_WIDTH).height(48f)
                .padBottom(8f).row();
        card.add(editButton("Change password", this::openChangePassword)).width(BUTTON_WIDTH).height(48f)
                .padBottom(16f).row();

        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuExit();
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new MainHubScreen(game));
                }
            }
        });
        card.add(back).width(BUTTON_WIDTH).height(56f);

        ScrollPane scroll = new ScrollPane(card, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        Table root = new Table();
        root.setFillParent(true);
        root.add(scroll).width(560f).maxHeight(UI_HEIGHT - 64f);
        stage.addActor(root);

        refreshInfo();
    }

    private Label addInfoRow(Table info, String label) {
        Label key = new Label(label + ":", skin, "secondary");
        Label value = new Label("—", skin, "medium");
        info.add(key).left().padRight(16f).padBottom(6f);
        info.add(value).left().expandX().padBottom(6f).row();
        return value;
    }

    private TextButton editButton(String text, Runnable action) {
        TextButton button = new TextButton(text, skin, "purple");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private void refreshInfo() {
        CommandResult<User> result = controller.showInfo();
        if (!result.isSuccess() || result.getData() == null) {
            showToast(result.getMessage(), true);
            return;
        }
        User user = result.getData();
        usernameValue.setText(nullSafe(user.getUsername()));
        nicknameValue.setText(nullSafe(user.getNickname()));
        emailValue.setText(nullSafe(user.getEmail()));
        gamesValue.setText(String.valueOf(user.getGamesPlayed()));
        coinsValue.setText(String.valueOf(user.getCoins()));
        gemsValue.setText(String.valueOf(user.getGems()));
        int levelsCompleted = 0;
        if (user.getChapterProgress() != null) {
            levelsCompleted = user.getChapterProgress().values().stream().mapToInt(Integer::intValue).sum();
        }
        levelsValue.setText(String.valueOf(levelsCompleted));
        myopointValue.setText(String.valueOf(user.getHighestMyopoint()));
        if (resourceBar != null) {
            resourceBar.refresh();
        }
    }

    private void openChangeUsername() {
        Table body = new Table();
        TextField field = new TextField("", skin);
        field.setMessageText("New username");
        body.add(field).width(FIELD_WIDTH).height(48f).padBottom(12f).row();
        TextButton save = new TextButton("Save", skin);
        body.add(save).width(200f).height(52f);

        Table overlay = ModalCard.create(skin, "Change username", body, null);
        save.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.changeUsername(field.getText());
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    overlay.remove();
                    refreshInfo();
                }
            }
        });
        stage.addActor(overlay);
        toast.toFront();
    }

    private void openChangeNickname() {
        Table body = new Table();
        TextField field = new TextField("", skin);
        field.setMessageText("New nickname");
        body.add(field).width(FIELD_WIDTH).height(48f).padBottom(12f).row();
        TextButton save = new TextButton("Save", skin);
        body.add(save).width(200f).height(52f);

        Table overlay = ModalCard.create(skin, "Change nickname", body, null);
        save.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.changeNickname(field.getText());
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    overlay.remove();
                    refreshInfo();
                }
            }
        });
        stage.addActor(overlay);
        toast.toFront();
    }

    private void openChangeEmail() {
        Table body = new Table();
        TextField field = new TextField("", skin);
        field.setMessageText("New email");
        body.add(field).width(FIELD_WIDTH).height(48f).padBottom(12f).row();
        TextButton save = new TextButton("Save", skin);
        body.add(save).width(200f).height(52f);

        Table overlay = ModalCard.create(skin, "Change email", body, null);
        save.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.changeEmail(field.getText());
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    overlay.remove();
                    refreshInfo();
                }
            }
        });
        stage.addActor(overlay);
        toast.toFront();
    }

    private void openChangePassword() {
        Table body = new Table();
        TextField oldPassword = new TextField("", skin);
        oldPassword.setMessageText("Current password");
        oldPassword.setPasswordMode(true);
        oldPassword.setPasswordCharacter('*');
        TextField newPassword = new TextField("", skin);
        newPassword.setMessageText("New password");
        newPassword.setPasswordMode(true);
        newPassword.setPasswordCharacter('*');
        body.add(oldPassword).width(FIELD_WIDTH).height(48f).padBottom(8f).row();
        body.add(newPassword).width(FIELD_WIDTH).height(48f).padBottom(12f).row();
        TextButton save = new TextButton("Save", skin);
        body.add(save).width(200f).height(52f);

        Table overlay = ModalCard.create(skin, "Change password", body, null);
        save.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.changePassword(
                        newPassword.getText(), oldPassword.getText());
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    overlay.remove();
                }
            }
        });
        stage.addActor(overlay);
        toast.toFront();
    }

    private static String nullSafe(String value) {
        return value == null || value.isEmpty() ? "—" : value;
    }
}
