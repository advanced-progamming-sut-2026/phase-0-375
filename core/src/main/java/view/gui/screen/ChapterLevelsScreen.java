package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.GameMenuController;
import controller.GameMenuController.LevelSummary;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.MenuType;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.theme.AdventureTheme;
import view.gui.ui.ResourceBar;
import view.gui.ui.SelectableMenuCard;

import java.util.List;
import java.util.Locale;

/**
 * Levels for one chapter → plant selection (or locked toast).
 */
public final class ChapterLevelsScreen extends AbstractMenuScreen {
    private final GameMenuController controller = GameMenuController.getInstance();
    private final Chapter chapter;

    public ChapterLevelsScreen(PvzGdxGame game, Chapter chapter) {
        super(game);
        this.chapter = chapter;
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.GAME);

        Table top = new Table();
        top.setFillParent(true);
        top.top();
        top.add(new ResourceBar(skin)).expandX().right().pad(12f);
        stage.addActor(top);

        String chapterTitle = chapter.name().replace('_', ' ');
        BorderedTable card = new BorderedTable();
        card.pad(24f);
        card.add(new Label(chapterTitle, skin, "big")).padBottom(8f).row();
        card.add(new Label("Select a level", skin, "secondary")).padBottom(16f).row();

        Table list = new Table();
        CommandResult<List<LevelSummary>> result = controller.listLevels(chapter);
        if (!result.isSuccess() || result.getData() == null) {
            list.add(new Label(result.getMessage(), skin, "medium")).row();
            showToast(result.getMessage(), true);
        } else {
            for (LevelSummary summary : result.getData()) {
                String typeLabel = summary.levelType() == null
                        ? "NORMAL"
                        : summary.levelType().name().replace('_', ' ');
                String status;
                if (summary.completed()) {
                    status = "Completed";
                } else if (summary.unlocked()) {
                    status = "Ready";
                } else {
                    status = "Locked";
                }
                String subtitle = typeLabel + " · " + status;
                String action = summary.unlocked() ? "Play" : "Locked";
                SelectableMenuCard row = new SelectableMenuCard(
                        skin,
                        "Level " + summary.levelId(),
                        subtitle,
                        action);
                row.setArt(AdventureTheme.get().levelArt(chapter, summary.levelId()));
                row.setActionEnabled(summary.unlocked());
                if (summary.unlocked()) {
                    final int levelId = summary.levelId();
                    row.onAction(() -> startLevel(levelId));
                }
                list.add(row).growX().padBottom(10f).row();
            }
        }
        card.add(list).growX().padBottom(16f).row();

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
        root.add(scroll).width(720f).maxHeight(UI_HEIGHT - 64f);
        stage.addActor(root);
    }

    private void startLevel(int levelId) {
        String chapterArg = chapter.name().toLowerCase(Locale.ROOT);
        CommandResult<Void> r = controller.enterChapter(chapterArg, levelId);
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new LevelObjectivesScreen(game, chapter));
        }
    }
}
