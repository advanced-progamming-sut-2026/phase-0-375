package model.game.systems;

import model.enums.SunType;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.item.Sun;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Drops suns from the sky and handles their landing effects.
 *
 * <p>Rules implemented (from the project spec):
 * <ul>
 *   <li>A sky sun starts falling every {@code x} seconds where
 *       {@code x = max(6 + 0.05 * t, 12)} and {@code t} is the number of
 *       seconds since the level started. The level's sun drop rate modifier
 *       scales this rate.</li>
     *   <li>A falling sun lands after 5 seconds. It is collectible as soon as it
     *       appears in the air.</li>
 *   <li>Sun types: 80% NORMAL (25 sun), 15% SPECIAL (100 sun),
 *       5% RADIOACTIVE (150 sun).</li>
 *   <li>A radioactive sun deals 80 damage when it lands: to zombies in the
 *       surrounding 5x5 area and to plants in the surrounding 3x3 area.</li>
 * </ul>
 */
public class SunFallSystem implements Tickable {

    public static final float FALL_TIME_SECONDS = 5f;
    private static final int NORMAL_SUN_VALUE = 25;
    private static final int SPECIAL_SUN_VALUE = 100;
    private static final int RADIOACTIVE_SUN_VALUE = 150;
    private static final int RADIOACTIVE_DAMAGE = 80;

    private final GameModel gameModel;
    private final float sunDropRateModifier;
    private boolean skyDropEnabled;

    private float elapsedSeconds;
    private float dropTimer;
    private final Random random = new Random();

    public SunFallSystem(GameModel gameModel, float sunDropRateModifier, boolean skyDropEnabled) {
        this.gameModel = gameModel;
        this.sunDropRateModifier = sunDropRateModifier > 0f ? sunDropRateModifier : 1f;
        this.skyDropEnabled = skyDropEnabled;
        this.elapsedSeconds = 0f;
        this.dropTimer = nextDropInterval();
    }

    public boolean isSkyDropEnabled() {
        return skyDropEnabled;
    }

    @Override
    public void tick(float deltaTime) {
        elapsedSeconds += deltaTime;

        tickFallingSuns(deltaTime);

        if (!skyDropEnabled) return;

        dropTimer -= deltaTime;
        if (dropTimer <= 0f) {
            dropRandomSkySun();
            dropTimer = nextDropInterval();
        }
    }

    /**
     * Starts a sun of the given type falling toward a random point on cell (x, y).
     * Collectible immediately; lands after {@link #FALL_TIME_SECONDS}.
     */
    public void spawnSkySun(int x, int y, SunType type) {
        Sun sun = new Sun(type, valueOf(type), x, y);
        sun.setOffset((random.nextFloat() - 0.5f) * 0.8f, (random.nextFloat() - 0.5f) * 0.8f);
        sun.setFall(FALL_TIME_SECONDS, FALL_TIME_SECONDS);
        gameModel.spawnSun(sun);
    }

    public void toggleSkyDrop(boolean enabled) {
        this.skyDropEnabled = enabled;
    }

    // internals

    /** Seconds until the next sky sun: max(6 + 0.05t, 12), scaled by the drop rate modifier. */
    private float nextDropInterval() {
        float base = Math.max(6f + 0.05f * elapsedSeconds, 12f);
        return base / sunDropRateModifier;
    }

    private void dropRandomSkySun() {
        int col = random.nextInt(Math.max(1, gameModel.getColumnCount()));
        int row = random.nextInt(Math.max(1, gameModel.getRowCount()));
        spawnSkySun(col, row, rollSunType());
    }

    /** 80% normal, 15% special, 5% radioactive. */
    private SunType rollSunType() {
        float roll = random.nextFloat();
        if (roll < 0.80f) return SunType.NORMAL;
        if (roll < 0.95f) return SunType.SPECIAL;
        return SunType.RADIOACTIVE;
    }

    private static int valueOf(SunType type) {
        switch (type) {
            case SPECIAL:
                return SPECIAL_SUN_VALUE;
            case RADIOACTIVE:
                return RADIOACTIVE_SUN_VALUE;
            default:
                return NORMAL_SUN_VALUE;
        }
    }

    private void tickFallingSuns(float deltaTime) {
        List<Sun> active = gameModel.getActiveSuns();
        if (active == null || active.isEmpty()) {
            return;
        }
        for (Sun sun : new ArrayList<>(active)) {
            if (!sun.isFalling()) {
                continue;
            }
            sun.tickFall(deltaTime);
            if (!sun.isFalling()) {
                land(sun);
            }
        }
    }

    /** The sun touches the ground. Collected mid-air suns skip landing effects. */
    private void land(Sun sun) {
        if (!gameModel.getActiveSuns().contains(sun)) {
            return;
        }
        dispatch(GameEvent.Type.SUN_DROPPED);

        if (sun.getType() == SunType.RADIOACTIVE) {
            applyRadioactiveDamage(sun.getY(), sun.getX());
        }
    }

    private void applyRadioactiveDamage(int row, int col) {
        // Zombies in the surrounding 5x5 area.
        for (ZombieInstance zombie : gameModel.getZombiesInArea(row, col, 2, 2)) {
            gameModel.damageZombie(zombie, RADIOACTIVE_DAMAGE);
        }
        // Plants in the surrounding 3x3 area.
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                PlantInstance plant = gameModel.getPlantAt(r, c);
                if (plant != null) {
                    gameModel.damagePlant(plant, RADIOACTIVE_DAMAGE);
                }
            }
        }
    }

    private void dispatch(GameEvent.Type type) {
        EventBus bus = gameModel.getEventBus();
        if (bus != null) {
            bus.dispatch(new GameEvent(type));
        }
    }
}
