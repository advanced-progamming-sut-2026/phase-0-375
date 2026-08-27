package view.gui.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import controller.PlantSelectionMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.data.level.NpcDialogueData;
import model.data.level.NpcDialogueRegistry;
import model.enums.Chapter;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;
import model.game.level.minigame.beghouled.BeghouledLevel;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.level.special.ScoreLevel;
import view.gui.PvzGdxGame;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.WaterUnderlayerRenderer;
import view.gui.ui.LevelObjectivesOverlay;
import view.gui.ui.NpcDialogueOverlay;

/**
 * Full-screen "Level Objectives" splash shown before plant selection.
 * Renders the chapter lawn behind a dimmed overlay; CONTINUE goes to
 * {@link PlantSelectionScreen}, or straight into gameplay when the level
 * does not allow choosing plants (e.g. Wall-nut Bowling).
 */
public final class LevelObjectivesScreen extends AbstractGameplayScreen {

    private final Chapter chapter;
    private final LawnBackgroundRenderer lawnBackground;
    private final WaterUnderlayerRenderer waterUnderlayer;

    public LevelObjectivesScreen(PvzGdxGame game, Chapter chapter) {
        super(game);
        this.chapter = chapter;
        App.getInstance().setCurrentMenu(MenuType.PLANT_SELECTION);

        LawnBackgroundRenderer.Style style = lawnStyle(chapter);
        lawnBackground = new LawnBackgroundRenderer(assets.textures, style);
        lawnBackground.ensureLoaded();
        waterUnderlayer = style != LawnBackgroundRenderer.Style.FRONT_LAWN
                && chapter == Chapter.BIG_WAVE_BEACH
            ? new WaterUnderlayerRenderer(assets, lawnLayout())
            : null;

        // Check for NPC dialogue
        LevelConfig config = levelConfig();
        if (config != null && chapter != null) {
            NpcDialogueRegistry registry = NpcDialogueRegistry.getInstance();
            NpcDialogueData dialogueData = registry.getDialogue(chapter.name(), config.getLevelId());
            
            if (dialogueData != null && dialogueData.getNpcs() != null && !dialogueData.getNpcs().isEmpty()) {
                // Show NPC dialogue first, then objectives
                NpcDialogueOverlay npcOverlay = new NpcDialogueOverlay(
                    skin, assets.textures, dialogueData.getNpcs(), this::showObjectives);
                uiStage.addActor(npcOverlay);
            } else {
                // No NPC dialogue, show objectives directly
                showObjectives();
            }
        } else {
            showObjectives();
        }
        toast.toFront();
    }

    private void showObjectives() {
        LevelConfig config = levelConfig();
        Table overlay = LevelObjectivesOverlay.create(skin, config, this::proceed);
        uiStage.addActor(overlay);
        toast.toFront();
    }

    private void proceed() {
        if (!plantChoiceAllowed()) {
            CommandResult<Void> result = PlantSelectionMenuController.getInstance().startGame();
            showToast(result.getMessage(), !result.isSuccess());
            if (result.isSuccess()) {
                game.setScreen(openGameplay(game));
            }
            return;
        }
        game.setScreen(new PlantSelectionScreen(game, chapter));
    }

    @Override
    protected void onConfirm() {
        proceed();
    }

    @Override
    protected void onBack() {
        if (chapter != null) {
            game.setScreen(new ChapterLevelsScreen(game, chapter));
        } else {
            game.setScreen(new AdventureScreen(game));
        }
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

    private static LawnBackgroundRenderer.Style lawnStyle(Chapter chapter) {
        return LawnBackgroundRenderer.Style.forChapter(chapter != null ? chapter : currentChapter());
    }

    private static boolean plantChoiceAllowed() {
        GameModel model = App.getInstance().getCurrentGameModel();
        return model == null
                || model.getCurrentLevel() == null
                || model.getCurrentLevel().getConfig() == null
                || model.getCurrentLevel().getConfig().getRules() == null
                || model.getCurrentLevel().getConfig().getRules().isAllowsChoosingPlants();
    }

    static Screen openGameplay(PvzGdxGame game) {
        Level level = currentLevel();
        if (level instanceof WallnutBowlingLevel
            || level instanceof BeghouledLevel
            || level instanceof VaseBreakerLevel
            || level instanceof MiniGameLevel
            || level instanceof ScoreLevel) {
            return new GameplayScreen(game);
        }
        Chapter ch = currentChapter();
        if (LawnBackgroundRenderer.Style.forChapter(ch) != LawnBackgroundRenderer.Style.FRONT_LAWN) {
            return new GameplayScreen(game);
        }
        return new GameplayStubScreen(game);
    }

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

    private static Chapter currentChapter() {
        Level level = currentLevel();
        return level == null || level.getConfig() == null ? null : level.getConfig().getChapter();
    }

    private static Level currentLevel() {
        GameModel model = App.getInstance().getCurrentGameModel();
        return model == null ? null : model.getCurrentLevel();
    }
}
