package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import model.app.App;
import model.enums.MenuType;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.ui.ResourceBar;

/**
 * Thin placeholder for adventure side-menus until full GUI is built.
 */
public final class PlaceholderMenuScreen extends AbstractMenuScreen {
    private final String title;
    private final MenuType menuType;
    private final Table content;

    public PlaceholderMenuScreen(PvzGdxGame game, String title, MenuType menuType, Table content) {
        super(game);
        this.title = title;
        this.menuType = menuType;
        this.content = content;
    }

    public static PlaceholderMenuScreen message(PvzGdxGame game, String title, MenuType menuType,
                                                String body) {
        Table t = new Table();
        t.add(new Label(body, game.skin, "medium")).pad(8f);
        return new PlaceholderMenuScreen(game, title, menuType, t);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(menuType);

        Table top = new Table();
        top.setFillParent(true);
        top.top();
        top.add(new ResourceBar(skin, game.assets != null ? game.assets.textures : null))
                .expandX().right().pad(12f);
        stage.addActor(top);

        BorderedTable card = new BorderedTable();
        card.pad(24f);
        Label titleLabel = new Label(title, skin, "big");
        titleLabel.setColor(Color.BLACK);
        card.add(titleLabel).padBottom(16f).row();
        if (content != null) {
            paintLabelsBlack(content);
            card.add(content).grow().padBottom(16f).row();
        }

        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new AdventureScreen(game));
            }
        });
        card.add(back).width(220f).height(56f);

        ScrollPane scroll = new ScrollPane(card, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        Table root = new Table();
        root.setFillParent(true);
        root.add(scroll).width(900f).maxHeight(UI_HEIGHT - 64f);
        stage.addActor(root);
    }

    /** Skin fonts default to light colors; bordered cards need dark text. */
    private static void paintLabelsBlack(Actor root) {
        if (root instanceof Label label) {
            label.setColor(Color.BLACK);
            return;
        }
        if (root instanceof Group group) {
            for (Actor child : group.getChildren()) {
                paintLabelsBlack(child);
            }
        }
    }
}
