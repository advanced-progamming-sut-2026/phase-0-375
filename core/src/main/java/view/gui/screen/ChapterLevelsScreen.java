package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import controller.GameMenuController;
import controller.GameMenuController.LevelSummary;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.MenuType;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.assets.ChapterIslandArt;
import view.gui.ui.ResourceBar;
import view.gui.ui.SelectableMenuCard;

import java.util.List;
import java.util.Locale;

/**
 * Levels for one chapter → plant selection (or locked toast).
 * Doc: after picking a chapter, list its levels; only unlocked ones are playable.
 */
public final class ChapterLevelsScreen extends AbstractMenuScreen {
    private final GameMenuController controller = GameMenuController.getInstance();
    private final ChapterIslandArt chapterArt = new ChapterIslandArt();
    private final Chapter chapter;

    public ChapterLevelsScreen(PvzGdxGame game, Chapter chapter) {
        super(game);
        this.chapter = chapter;
    }

    @Override
    public void show() {
        game.ensureAssets();
        chapterArt.ensureLoaded(game.assets.textures);
        super.show();
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.GAME);

        Table top = new Table();
        top.setFillParent(true);
        top.top();
        top.add(new ResourceBar(skin, game.assets != null ? game.assets.textures : null))
                .expandX().right().pad(12f);
        stage.addActor(top);

        String chapterTitle = chapter.name().replace('_', ' ');
        BorderedTable card = new BorderedTable();
        card.pad(24f);

        TextureRegion thumb = game.assets.textures.region(chapterArt.imageId(chapter));
        if (thumb != null) {
            Image art = new Image(new TextureRegionDrawable(thumb));
            card.add(art).size(120f, 220f).padBottom(12f).row();
        }

        Label title = new Label(chapterTitle, skin, "big");
        title.setColor(Color.BLACK);
        card.add(title).padBottom(8f).row();

        Label subtitle = new Label("Select a level", skin, "secondary");
        subtitle.setColor(Color.BLACK);
        card.add(subtitle).padBottom(16f).row();

        Table list = new Table();
        CommandResult<List<LevelSummary>> result = controller.listLevels(chapter);
        if (!result.isSuccess() || result.getData() == null) {
            Label err = new Label(result.getMessage(), skin, "medium");
            err.setColor(Color.BLACK);
            list.add(err).row();
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
                String rowSubtitle = typeLabel + " · " + status;
                String action = summary.unlocked() ? "Play" : "Locked";
                SelectableMenuCard row = new SelectableMenuCard(
                        skin,
                        "Level " + summary.levelId(),
                        rowSubtitle,
                        action);
                row.setActionEnabled(summary.unlocked());
                paintCardLabelsBlack(row);
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

    private static void paintCardLabelsBlack(Actor root) {
        if (root instanceof Label label) {
            label.setColor(Color.BLACK);
            return;
        }
        if (root instanceof Group group) {
            for (Actor child : group.getChildren()) {
                paintCardLabelsBlack(child);
            }
        }
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
