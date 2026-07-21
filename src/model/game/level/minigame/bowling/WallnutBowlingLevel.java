package model.game.level.minigame.bowling;

import model.app.App;
import model.enums.BowlingWalnutType;
import model.enums.MiniGameType;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;
import model.game.map.FloatPoint;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.zombie.instance.ZombieInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Wall-nut Bowling mini-game.
 *
 * <p>The walnuts are simulated by this level (not by {@code ProjectileSystem})
 * because ricochets, area explosions and crush-through are bowling-specific;
 */
public class WallnutBowlingLevel extends MiniGameLevel {

    /** Rolling speed, in tiles per second. */
    public static final float WALNUT_SPEED = 2.0f;
    /** Per-hit damage fallback while Wall-nut's definition damage is 0. */
    public static final int DEFAULT_HIT_DAMAGE = 400;
    /** Blast damage fallback if the Explode-o-nut definition is unavailable. */
    public static final int DEFAULT_EXPLODE_DAMAGE = 1800;
    /** Giant walnut crush damage (crushing is an instant kill by spec). */
    public static final int CRUSH_DAMAGE = 100_000;

    /** Collision tolerance, mirroring {@code ProjectileSystem}. */
    private static final float HIT_TOLERANCE = 0.5f;
    private static final float DIAGONAL = (float) (1.0 / Math.sqrt(2.0));

    private final List<BowlingWalnut> rolling = new ArrayList<>();
    private float timeSinceLastDelivery;
    private int nextPoolIndex;

    public WallnutBowlingLevel(LevelConfig config, MiniGameType miniGameType, int difficultyTier) {
        super(config, miniGameType, difficultyTier);
    }

    /** Maps a belt entry / command name to a walnut type (null if unknown). */
    public static BowlingWalnutType parseWalnutType(String raw) {
        if (raw == null) return null;
        String name = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (name) {
            case "WALL_NUT", "WALLNUT", "BOWLING_WALNUT", "WALNUT", "NORMAL" -> BowlingWalnutType.NORMAL;
            case "EXPLODE_O_NUT", "EXPLODEONUT" -> BowlingWalnutType.EXPLODE_O_NUT;
            case "GIANT_WALL_NUT", "GIANT_WALNUT", "GIANT" -> BowlingWalnutType.GIANT;
            default -> null;
        };
    }

    /** The plants.json definition name backing a walnut type. */
    private static String plantNameFor(BowlingWalnutType type) {
        return switch (type) {
            case NORMAL -> "Wall-nut";
            case EXPLODE_O_NUT -> "Explode-o-nut";
            case GIANT -> "Giant Wall-nut"; // no definition; crush constant applies
        };
    }

    /** Damage a walnut of this type deals, resolved from plants.json. */
    private static int damageFor(BowlingWalnutType type) {
        return switch (type) {
            case NORMAL -> definitionDamage(plantNameFor(type), DEFAULT_HIT_DAMAGE);
            case EXPLODE_O_NUT -> definitionDamage(plantNameFor(type), DEFAULT_EXPLODE_DAMAGE);
            case GIANT -> CRUSH_DAMAGE;
        };
    }

    /** Definition damage when positive, otherwise the given fallback. */
    private static int definitionDamage(String plantName, int fallback) {
        if (!ensurePlantFactory() || !PlantFactory.hasDefinition(plantName)) {
            return fallback;
        }
        Plant definition = PlantFactory.getDefinition(plantName);
        return definition != null && definition.getDamage() > 0
                ? definition.getDamage()
                : fallback;
    }

    /** Loads plant definitions on demand, mirroring {@code RegularLevel}. */
    private static boolean ensurePlantFactory() {
        try {
            PlantFactory.getAllDefinitions();
            return true;
        } catch (IllegalStateException notInitialised) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
                return true;
            } catch (IOException | RuntimeException e) {
                return false;
            }
        }
    }

    @Override
    public boolean canStart() {
        LevelConfig config = getConfig();
        if (config == null
                || config.getRows() <= 0
                || config.getColumns() <= 0
                || config.getRules() == null
                || config.getWaves() == null
                || config.getWaves().isEmpty()) {
            return false;
        }
        List<String> pool = config.getConveyorPlants();
        if (pool == null || pool.isEmpty()
                || config.getConveyorIntervalSeconds() <= 0
                || config.getConveyorCapacity() <= 0) {
            return false;
        }
        for (String name : pool) {
            if (parseWalnutType(name) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) return;
        // The belt replaces the player's selection; deliver the first walnut
        // immediately so the player is not left idle.
        model.setSelectedPlants(new ArrayList<>());
        timeSinceLastDelivery = 0f;
        nextPoolIndex = 0;
        deliver(model);
    }

    @Override
    public void tick(float deltaTime) {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) return;
        deliverIfDue(model, deltaTime);
        advanceWalnuts(model, deltaTime);
    }

    @Override
    public void onWaveCleared(int waveNumber) {
        // Nothing special on wave clear.
    }

    @Override
    public void onFail() {
        // No extra behaviour on loss.
    }

    @Override
    public boolean checkWinCondition(GameModel model) {
        return model != null
                && model.getWaveManager() != null
                && model.getWaveManager().isLevelDone()
                && model.getZombieCount() == 0;
    }

    @Override
    public boolean checkLossCondition(GameModel model) {
        return model != null && model.isHouseBreached();
    }

    // --- Conveyor belt ---

    /** The belt pauses while full, mirroring the Conveyor Belt level. */
    private void deliverIfDue(GameModel model, float deltaTime) {
        List<String> belt = model.getSelectedPlants();
        if (belt == null || belt.size() >= getConfig().getConveyorCapacity()) {
            return;
        }
        timeSinceLastDelivery += deltaTime;
        float interval = getConfig().getConveyorIntervalSeconds();
        while (timeSinceLastDelivery >= interval
                && belt.size() < getConfig().getConveyorCapacity()) {
            timeSinceLastDelivery -= interval;
            deliver(model);
        }
    }

    private void deliver(GameModel model) {
        List<String> pool = getConfig().getConveyorPlants();
        List<String> belt = model.getSelectedPlants();
        if (pool == null || pool.isEmpty() || belt == null
                || belt.size() >= getConfig().getConveyorCapacity()) {
            return;
        }
        belt.add(pool.get(nextPoolIndex));
        nextPoolIndex = (nextPoolIndex + 1) % pool.size();
    }

    // --- Launching ---

    /** Rolls a walnut from the leftmost column down the given lane. */
    public void launchWalnut(BowlingWalnutType type, int lane) {
        BowlingWalnut walnut = new BowlingWalnut(
                damageFor(type), new FloatPoint(0f, lane), lane, WALNUT_SPEED);
        walnut.setType(type);
        walnut.setHorizontalVelocity(WALNUT_SPEED);
        walnut.setVerticalVelocity(0f);
        rolling.add(walnut);
    }

    /** Walnuts currently rolling (exposed for inspection). */
    public List<BowlingWalnut> getActiveWalnuts() {
        return new ArrayList<>(rolling);
    }

    // --- Rolling physics ---

    private void advanceWalnuts(GameModel model, float deltaTime) {
        if (rolling.isEmpty()) return;
        int rows = getConfig().getRows();
        int cols = model.getColumnCount();

        Iterator<BowlingWalnut> it = rolling.iterator();
        while (it.hasNext()) {
            BowlingWalnut walnut = it.next();

            float nx = walnut.getX() + walnut.getHorizontalVelocity() * deltaTime;
            float ny = walnut.getY() + walnut.getVerticalVelocity() * deltaTime;

            // Touching the top or bottom edge turns the walnut like a hit does.
            if (ny < 0f) {
                ny = -ny;
                walnut.setVerticalVelocity(-walnut.getVerticalVelocity());
            } else if (ny > rows - 1) {
                ny = 2 * (rows - 1) - ny;
                walnut.setVerticalVelocity(-walnut.getVerticalVelocity());
            }

            walnut.setX(nx);
            walnut.getCurrentPosition().setY(ny);
            walnut.setRow(Math.round(ny));

            if (nx < 0f || nx >= cols) {
                it.remove();
                continue;
            }

            ZombieInstance target = findCollision(model, walnut);
            if (target != null && onHit(model, walnut, target)) {
                it.remove();
            }
        }
    }

    /** Closest live zombie in the walnut's lane within tolerance, never re-hit. */
    private ZombieInstance findCollision(GameModel model, BowlingWalnut walnut) {
        List<ZombieInstance> zombies = model.getZombiesInLane(walnut.getRow());
        if (zombies == null) return null;
        float wx = walnut.getX();
        ZombieInstance best = null;
        float bestDist = Float.MAX_VALUE;
        for (ZombieInstance zombie : zombies) {
            if (zombie == null || zombie.isDead() || walnut.hasAlreadyHit(zombie)) continue;
            float dist = Math.abs(zombie.getContinuousX() - wx);
            if (dist > HIT_TOLERANCE) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = zombie;
            }
        }
        return best;
    }

    /** @return true when the walnut is consumed by this hit. */
    private boolean onHit(GameModel model, BowlingWalnut walnut, ZombieInstance target) {
        walnut.markHit(target);
        walnut.incrementHitCount();

        BowlingWalnutType type = walnut.getType();
        if (type == BowlingWalnutType.GIANT) {
            // Crushes the zombie and keeps rolling perfectly straight.
            model.damageZombie(target, walnut.getDamage());
            return false;
        }
        if (type == BowlingWalnutType.EXPLODE_O_NUT) {
            explode(model, walnut);
            return true;
        }
        model.damageZombie(target, walnut.getDamage());
        turn(walnut);
        return false;
    }

    /** 3x3 blast centred on the walnut, using the walnut's resolved damage. */
    private void explode(GameModel model, BowlingWalnut walnut) {
        int centerRow = walnut.getRow();
        int centerCol = Math.round(walnut.getX());
        List<ZombieInstance> zombies = model.getZombies();
        if (zombies == null) return;
        for (ZombieInstance zombie : new ArrayList<>(zombies)) {
            if (zombie == null || zombie.isDead()) continue;
            int zRow = Math.round(zombie.getContinuousY());
            int zCol = Math.round(zombie.getContinuousX());
            if (Math.abs(zRow - centerRow) <= 1 && Math.abs(zCol - centerCol) <= 1) {
                model.damageZombie(zombie, walnut.getDamage());
            }
        }
    }

    /**
     * First hit: 45-degree deflection (away from the nearer horizontal edge).
     * Later hits: 90-degree turn, i.e. diagonal up &lt;-&gt; diagonal down.
     */
    private void turn(BowlingWalnut walnut) {
        if (walnut.getHitCount() <= 1 || walnut.getVerticalVelocity() == 0f) {
            float y = walnut.getY();
            int rows = getConfig().getRows();
            float sign = y <= (rows - 1) / 2f ? 1f : -1f;
            walnut.setHorizontalVelocity(WALNUT_SPEED * DIAGONAL);
            walnut.setVerticalVelocity(sign * WALNUT_SPEED * DIAGONAL);
        } else {
            walnut.setVerticalVelocity(-walnut.getVerticalVelocity());
        }
    }
}
