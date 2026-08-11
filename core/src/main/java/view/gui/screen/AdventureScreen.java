package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.GameMenuController;
import controller.GameMenuController.ChapterSummary;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.theme.AdventureTheme;
import view.gui.ui.ResourceBar;
import view.gui.ui.SelectableMenuCard;

import java.util.List;

/**
 * Adventure hub: chapter list → {@link ChapterLevelsScreen}.
 */
public final class AdventureScreen extends AbstractMenuScreen {
    private final GameMenuController controller = GameMenuController.getInstance();

    public AdventureScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.GAME);

        Table top = new Table();
        top.setFillParent(true);
        top.top();
        top.add(new ResourceBar(skin)).expandX().right().pad(12f);
        stage.addActor(top);

        BorderedTable card = new BorderedTable();
        card.pad(24f);
        card.add(new Label("Adventure", skin, "big")).padBottom(8f).row();
        card.add(new Label("Choose a world", skin, "secondary")).padBottom(16f).row();

        Table list = new Table();
        CommandResult<List<ChapterSummary>> result = controller.listChapters();
        if (!result.isSuccess() || result.getData() == null) {
            list.add(new Label(result.getMessage(), skin, "medium")).row();
            showToast(result.getMessage(), true);
        } else {
            for (ChapterSummary summary : result.getData()) {
                String subtitle = summary.unlocked()
                        ? "Progress " + summary.completedLevels() + " / " + summary.totalLevels()
                        : "Locked — finish the previous world";
                String action = summary.unlocked() ? "Open" : "Locked";
                SelectableMenuCard row = new SelectableMenuCard(
                        skin, summary.displayName(), subtitle, action);
                row.setArt(AdventureTheme.get().chapterArt(summary.chapter()));
                row.setActionEnabled(summary.unlocked());
                if (summary.unlocked()) {
                    row.onAction(() -> game.setScreen(new ChapterLevelsScreen(game, summary.chapter())));
                }
                list.add(row).growX().padBottom(10f).row();
            }
        }
        card.add(list).growX().padBottom(16f).row();

        TextButton debug = new TextButton("Debug playground", skin, "brown");
        debug.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.enterDebugLevel();
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new DebugPlaygroundScreen(game));
                }
            }
        });
        card.add(debug).width(280f).height(56f).padBottom(10f).row();

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
        card.add(back).width(220f).height(56f);

        ScrollPane scroll = new ScrollPane(card, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        Table root = new Table();
        root.setFillParent(true);
        root.add(scroll).width(720f).maxHeight(UI_HEIGHT - 64f);
        stage.addActor(root);
    }
}
