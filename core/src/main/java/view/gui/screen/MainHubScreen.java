package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.MainMenuController;
import controller.NewsMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.User;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.ui.NewsOverlay;
import view.gui.ui.ResourceBar;

/**
 * Main hub: currency chrome, news, adventure, profile, logout.
 */
public final class MainHubScreen extends AbstractMenuScreen {
    private final MainMenuController controller = MainMenuController.getInstance();
    private TextButton newsButton;

    public MainHubScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.MAIN);

        User user = App.getInstance().getCurrentUser();
        String nick = user != null ? user.getNickname() : "Player";

        Table top = new Table();
        top.setFillParent(true);
        top.top();
        top.add(new ResourceBar(skin)).expandX().right().pad(12f);
        stage.addActor(top);

        BorderedTable card = new BorderedTable();
        card.pad(28f);
        card.add(new Label("Main Menu", skin, "big")).padBottom(8f).row();
        card.add(new Label("Welcome, " + nick, skin, "medium")).padBottom(20f).row();

        newsButton = new TextButton(newsButtonLabel(), skin);
        newsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openNewsOverlay();
            }
        });
        card.add(newsButton).width(300f).height(64f).padBottom(10f).row();

        TextButton adventure = new TextButton("Adventure", skin, "purple");
        adventure.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuEnter("game");
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new AdventureScreen(game));
                }
            }
        });
        TextButton profile = new TextButton("Profile", skin, "purple");
        profile.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuEnter("profile");
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new ProfileScreen(game));
                }
            }
        });
        TextButton settings = new TextButton("Settings", skin, "purple");
        settings.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuEnter("settings");
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new SettingsScreen(game));
                }
            }
        });
        card.add(adventure).width(300f).height(56f).padBottom(8f).row();
        card.add(profile).width(300f).height(56f).padBottom(8f).row();
        card.add(settings).width(300f).height(56f).padBottom(16f).row();

        TextButton logout = new TextButton("Logout", skin, "brown");
        logout.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.logout();
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new RegisterScreen(game));
                }
            }
        });
        card.add(logout).width(300f).height(56f).padBottom(8f).row();

        TextButton exit = new TextButton("Exit", skin, "brown");
        exit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                controller.menuExit();
            }
        });
        card.add(exit).width(300f).height(64f);

        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(520f);
        stage.addActor(root);
    }

    private void openNewsOverlay() {
        CommandResult<Void> r = controller.menuEnter("news");
        if (!r.isSuccess()) {
            showToast(r.getMessage(), true);
            return;
        }

        Table overlay = NewsOverlay.create(skin, () -> {
            CommandResult<Void> exit = NewsMenuController.getInstance().menuExit();
            if (!exit.isSuccess()) {
                showToast(exit.getMessage(), true);
            }
            refreshNewsButton();
        });
        stage.addActor(overlay);
        toast.toFront();
    }

    private void refreshNewsButton() {
        if (newsButton != null) {
            newsButton.setText(newsButtonLabel());
        }
    }

    private static String newsButtonLabel() {
        int unread = NewsMenuController.getInstance().countUnread();
        return unread > 0 ? "News (!" + unread + ")" : "News";
    }

    private TextButton stubButton(String text) {
        TextButton button = new TextButton(text, skin, "purple");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showToast(text + " — not implemented yet.", false);
            }
        });
        return button;
    }
}
