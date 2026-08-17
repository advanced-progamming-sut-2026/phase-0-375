package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import model.app.App;
import model.enums.Chapter;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.level.Level;
import view.gui.PvzGdxGame;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.WaterUnderlayerRenderer;

/**
 * In-game lawn. Chapter backgrounds use the same left/center/right camera as debug FrontLawn.
 */
public final class GameplayScreen extends AbstractGameplayScreen {
    private final LawnBackgroundRenderer lawnBackground;
    private final WaterUnderlayerRenderer waterUnderlayer;

    public GameplayScreen(PvzGdxGame game) {
        super(game);
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);
        Chapter chapter = currentChapter();
        lawnBackground = new LawnBackgroundRenderer(
                assets.textures, LawnBackgroundRenderer.Style.forChapter(chapter));
        lawnBackground.ensureLoaded();
        waterUnderlayer = chapter == Chapter.BIG_WAVE_BEACH
                ? new WaterUnderlayerRenderer(assets, lawnLayout())
                : null;
        buildHud();
    }

    private static LawnLayout lawnLayout() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int rows = model != null ? model.getMap().getRows() : LawnLayout.DEFAULT_ROWS;
        int cols = model != null ? model.getMap().getCols() : LawnLayout.DEFAULT_COLS;
        return new LawnLayout(rows, cols);
    }

    private static Chapter currentChapter() {
        GameModel model = App.getInstance().getCurrentGameModel();
        Level level = model == null ? null : model.getCurrentLevel();
        return level == null || level.getConfig() == null ? null : level.getConfig().getChapter();
    }

    private void buildHud() {
        Table topRight = new Table();
        topRight.setFillParent(true);
        topRight.setTouchable(Touchable.childrenOnly);
        topRight.top().right().pad(12f);

        TextButton back = new TextButton("Back to levels", skin, "brown");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                exitToLevels();
            }
        });
        topRight.add(back).width(220f).height(48f);
        uiStage.addActor(topRight);
    }

    private void exitToLevels() {
        Chapter chapter = currentChapter();
        App.getInstance().setCurrentGameModel(null);
        App.getInstance().setCurrentGameLoop(null);
        App.getInstance().setCurrentMenu(MenuType.GAME);
        if (chapter != null) {
            game.setScreen(new ChapterLevelsScreen(game, chapter));
        } else {
            game.setScreen(new AdventureScreen(game));
        }
    }

    @Override
    protected void updateLogic(float delta) {
        // Background only for now; waves / planting come later.
    }

    @Override
    protected void renderWorld(float delta) {
        lawnBackground.draw(game.batch);
        if (waterUnderlayer != null) {
            waterUnderlayer.draw(game.batch, App.getInstance().getCurrentGameModel(), delta);
        }
    }
}
