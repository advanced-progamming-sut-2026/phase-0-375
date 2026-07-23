package model.game.level;

import model.app.App;
import model.data.level.LevelRegistry;
import model.enums.Chapter;
import model.enums.GroundType;
import model.enums.SlideDirection;
import model.game.core.GameModel;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.map.terrain.SlideTerrainStrategy;
import model.game.map.terrain.TerrainStrategyFactory;
import model.zombie.ZombieFactory;
import model.zombie.instance.ZombieInstance;
import model.game.rule.EndGameCondition;
import model.game.rule.RegularEndGameCondition;
import model.plant.PlantFactory;
import model.user.User;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A normal level: survive every wave to win, lose if a zombie
 * reaches the house.
 */
public class RegularLevel extends Level {

    /** Zombie definition frozen inside each initial Frostbite ice block. */
    private static final String FROZEN_ZOMBIE = "ZombieDefault";

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
        applyChapterTerrain(model);
    }

    /**
     * Applies the chapter terrain declared in the level config to the map:
     * Big Wave Beach water / low-tide tiles, Frostbite Caves slide tiles and
     * initial ice blocks (each holding a frozen zombie), and Dark Ages
     * necromancy tiles.
     */
    private void applyChapterTerrain(GameModel model) {
        List<Point> water = getConfig().getWaterTiles();
        if (water != null) {
            for (Point p : water) {
                setGround(model, p, GroundType.WATER);
            }
        }
        List<Point> lowTides = getConfig().getLowTideTiles();
        if (lowTides != null) {
            for (Point p : lowTides) {
                // Low-tide cells inside the permanent sea start submerged;
                // ones on dry land stay normal until the tide covers them.
                Cell cell = model.getCellAt(p.getY(), p.getX());
                if (cell != null && cell.getGroundType() == GroundType.WATER) {
                    setGround(model, p, GroundType.LOW_TIDE);
                }
            }
        }
        Map<Point, SlideDirection> slides = getConfig().getSlideTiles();
        if (slides != null) {
            for (Map.Entry<Point, SlideDirection> e : slides.entrySet()) {
                Cell cell = model.getCellAt(e.getKey().getY(), e.getKey().getX());
                if (cell == null) continue;
                cell.setGroundType(e.getValue() == SlideDirection.UP
                        ? GroundType.SLIDE_UP : GroundType.SLIDE_DOWN);
                cell.setTerrainStrategy(new SlideTerrainStrategy(e.getValue()));
            }
        }
        List<Point> necromancy = getConfig().getNecromancyTiles();
        if (necromancy != null) {
            for (Point p : necromancy) {
                setGround(model, p, GroundType.NECROMANCY);
            }
        }
        List<Point> iceBlocks = getConfig().getInitialIceBlocks();
        if (iceBlocks != null) {
            for (Point p : iceBlocks) {
                Cell cell = model.getCellAt(p.getY(), p.getX());
                if (cell == null) continue;
                // Each initial ice block holds a frozen zombie that resumes
                // walking once the ice melts or is shot down.
                ZombieInstance frozen = ZombieFactory.createInstance(FROZEN_ZOMBIE);
                if (frozen != null) {
                    frozen.setGridPosition(new Point(p.getX(), p.getY()));
                    frozen.setContinuousPosition(new FloatPoint(p.getX(), p.getY()));
                }
                cell.setGroundType(GroundType.ICE);
                cell.setTerrainStrategy(new IceTerrainStrategy(frozen));
            }
        }
    }

    private void setGround(GameModel model, Point p, GroundType ground) {
        Cell cell = model.getCellAt(p.getY(), p.getX());
        if (cell == null) return;
        cell.setGroundType(ground);
        cell.setTerrainStrategy(TerrainStrategyFactory.create(ground));
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
        if (user == null) return;

        Chapter chapter = getConfig().getChapter();
        int completedId = getConfig().getLevelId();

        Map<Chapter, Integer> progress = user.getChapterProgress();
        if (progress != null && chapter != null) {
            progress.merge(chapter, getConfig().getLevelId(), Math::max);
        }

        if (user.getUnlockedLevels() == null) {
            user.setUnlockedLevels(new HashSet<>());
        }
        if (chapter != null) {
            try {
                int nextId = completedId + 1;
                LevelRegistry registry = LevelRegistry.getInstance();
                if (registry.hasLevel(chapter, nextId)) {
                    user.getUnlockedLevels().add(chapter.name() + "#" + nextId);
                }
            } catch (IllegalStateException notInitialised) {
                // LevelRegistry not loaded yet, just skip the next-level peek.
            }
        }

        App.getInstance().getUserRepository().flush();
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
