package view.gui.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import model.app.App;
import model.enums.Chapter;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.LevelConfig;
import view.gui.PvzGdxGame;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.WaterUnderlayerRenderer;
import view.gui.ui.LevelObjectivesOverlay;

/**
 * Full-screen "Level Objectives" splash shown before plant selection.
 * Renders the chapter lawn behind a dimmed overlay; CONTINUE goes to
 * {@link PlantSelectionScreen}.
 */
public final class LevelObjectivesScreen extends AbstractGameplayScreen {

    private final Chapter chapter;
    private final LawnBackgroundRenderer lawnBackground;
    private final WaterUnderlayerRenderer waterUnderlayer;

    public LevelObjectivesScreen(PvzGdxGame game, Chapter chapter) {
        super(game);
        this.chapter = chapter;
        App.getInstance().setCurrentMenu(MenuType.PLANT_SELECTION);

        lawnBackground = new LawnBackgroundRenderer(
            assets.textures, LawnBackgroundRenderer.Style.forChapter(chapter));
        lawnBackground.ensureLoaded();
        waterUnderlayer = chapter == Chapter.BIG_WAVE_BEACH
            ? new WaterUnderlayerRenderer(assets, lawnLayout())
            : null;

        LevelConfig config = levelConfig();
        Table overlay = LevelObjectivesOverlay.create(skin, config, this::proceed);
        uiStage.addActor(overlay);
        toast.toFront();
    }

    private void proceed() {
        game.setScreen(new PlantSelectionScreen(game, chapter));
    }

    @Override
    protected void renderWorld(float delta) {
        lawnBackground.draw(game.batch);
        if (waterUnderlayer != null) {
            waterUnderlayer.draw(game.batch, App.getInstance().getCurrentGameModel(), delta);
        }
    }

    @Override
    protected void updateLogic(float delta) {}

    private static LawnLayout lawnLayout() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int rows = model != null ? model.getMap().getRows() : LawnLayout.DEFAULT_ROWS;
        int cols = model != null ? model.getMap().getCols() : LawnLayout.DEFAULT_COLS;
        return new LawnLayout(rows, cols);
    }

    private static LevelConfig levelConfig() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) return null;
        Level level = model.getCurrentLevel();
        return level == null ? null : level.getConfig();
    }
}
