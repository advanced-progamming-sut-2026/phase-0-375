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
import view.gui.ui.ResourceBar;

/**
 * Stub main hub: currency chrome, news badge, logout. Other destinations are placeholders.
 */
public final class MainHubScreen extends AbstractMenuScreen {
    private final MainMenuController controller = MainMenuController.getInstance();

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
        card.add(new Label("Main Menu", skin, "big")).padBottom(8f).row();
        card.add(new Label("Welcome, " + nick, skin, "medium")).padBottom(20f).row();

        int unread = NewsMenuController.getInstance().countUnread();
        String newsLabel = unread > 0 ? "News (!" + unread + ")" : "News";
        TextButton news = new TextButton(newsLabel, skin);
        news.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuEnter("news");
                showToast(r.isSuccess()
                        ? "News menu stub — coming soon. (" + unread + " unread)"
                        : r.getMessage(), !r.isSuccess());
                // Stay on hub until a NewsScreen exists; revert menu type.
                if (r.isSuccess()) {
                    App.getInstance().setCurrentMenu(MenuType.MAIN);
                }
            }
        });
        card.add(news).width(300f).height(64f).padBottom(10f).row();

        TextButton adventure = stubButton("Adventure (stub)");
        TextButton profile = stubButton("Profile (stub)");
        TextButton settings = stubButton("Settings (stub)");
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
        card.add(logout).width(300f).height(64f);

        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(520f);
        stage.addActor(root);
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
