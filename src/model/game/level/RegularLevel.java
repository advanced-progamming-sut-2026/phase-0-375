package model.game.level;

import model.app.App;
import model.enums.Chapter;
import model.game.core.GameModel;
import model.game.map.Point;
import model.game.rule.EndGameCondition;
import model.game.rule.RegularEndGameCondition;
import model.plant.PlantFactory;
import model.user.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A normal level: survive every wave to win, lose if a zombie
 * reaches the house.
 */
public class RegularLevel extends Level {

    public RegularLevel(LevelConfig config) {
        super(config);
        config.setEndGameCondition(new RegularEndGameCondition(this));
    }

    @Override
    public boolean canStart() {
        LevelConfig config = getConfig();
        return config.getRows() > 0
                && config.getColumns() > 0
                && config.getRules() != null
                && config.getWaves() != null
                && !config.getWaves().isEmpty();
    }

    @Override
    public void onStart() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) return;

        List<Point> graves = getConfig().getInitialGraves();
        if (graves != null) {
            for (Point grave : graves) {
                model.spawnGraveAt(grave.getY(), grave.getX());
            }
        }
        // Initial ice blocks (Frostbite Caves) are not pre-placed yet:
        // pushables currently only enter the map via a pushing zombie.
    }

    @Override
    public void tick(float deltaTime) {
        // No per-tick logic: the game systems do all the work.
    }

    @Override
    public void onWaveCleared(int waveNumber) {
        // Nothing special happens on wave clear in a regular level.
    }

    @Override
    public void onComplete() {
        getConfig().setCompleted(true);

        User user = App.getInstance().getCurrentUser();
        Chapter chapter = getConfig().getChapter();
        Map<Chapter, Integer> progress = user == null ? null : user.getChapterProgress();
        if (progress != null && chapter != null) {
            progress.merge(chapter, getConfig().getLevelId(), Math::max);
        }
    }

    @Override
    public void onFail() {
        // Nothing to roll back on failure.
    }

    @Override
    public boolean checkWinCondition(GameModel model) {
        EndGameCondition condition = getConfig().getEndGameCondition();
        return condition != null && condition.isWin(model);
    }

    @Override
    public boolean checkLossCondition(GameModel model) {
        EndGameCondition condition = getConfig().getEndGameCondition();
        return condition != null && condition.isGameOver(model);
    }

    /**
     * True when the plant factory is usable, initialising it from the default
     * data file if nothing has done so yet. Shared by levels that pre-place
     * or pre-select plants.
     */
    protected static boolean ensurePlantFactory() {
        try {
            PlantFactory.getAllDefinitions();
            return true;
        } catch (IllegalStateException notInitialised) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
                return true;
            } catch (IOException | RuntimeException loadError) {
                return false;
            }
        }
    }
}
