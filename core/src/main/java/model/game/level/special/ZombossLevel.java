package model.game.level.special;

import model.app.App;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.rule.ZombossEndGameCondition;
import model.plant.PlantFactory;
import model.zombie.ZombieFactory;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Final chapter boss fight: conveyor seed delivery (from
 * {@link ConveyorBeltLevel}) plus a parked Zomboss with no wave meter.
 */
public class ZombossLevel extends ConveyorBeltLevel {

    public static final String DEFAULT_ZOMBOSS = "ZombieDarkZomboss";
    /**
     * Primary lane (0-based). Boss occupies this lane and the next — indexes
     * {@code 2} and {@code 3} at start. Clamped to {@code rows - 2}.
     */
    public static final int DEFAULT_PRIMARY_ROW = 2;

    private boolean bossSpawned;

    public ZombossLevel(LevelConfig config) {
        super(config);
        config.setEndGameCondition(new ZombossEndGameCondition());
    }

    @Override
    public boolean canStart() {
        LevelConfig config = getConfig();
        if (config.getRows() <= 0 || config.getColumns() <= 0 || config.getRules() == null) {
            return false;
        }
        List<String> pool = config.getConveyorPlants();
        if (pool == null || pool.isEmpty()
                || config.getConveyorIntervalSeconds() <= 0
                || config.getConveyorCapacity() <= 0
                || !ensurePlantFactory()) {
            return false;
        }
        for (String plantName : pool) {
            if (!PlantFactory.hasDefinition(plantName)) {
                return false;
            }
        }
        return ZombieFactory.hasDefinition(zombossName());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void ensureBossSpawned() {
        spawnBoss();
    }

    private void spawnBoss() {
        if (bossSpawned) {
            return;
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return;
        }
        String name = zombossName();
        int rows = model.getRowCount();
        int primary = Math.min(DEFAULT_PRIMARY_ROW, Math.max(0, rows - 2));
        int col = Math.max(0, model.getColumnCount() - 1 - ZombossBehavior.PARK_COLUMNS_FROM_RIGHT);
        ZombieInstance boss = model.spawnZombieAt(name, primary, col);
        if (boss != null) {
            bossSpawned = true;
        }
    }

    private String zombossName() {
        String name = getConfig().getZombossDefinition();
        if (name == null || name.isBlank()) {
            return DEFAULT_ZOMBOSS;
        }
        return name;
    }
}
