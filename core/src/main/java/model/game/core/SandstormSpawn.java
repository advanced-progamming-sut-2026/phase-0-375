package model.game.core;

import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

/**
 * Pending Ancient Egypt sandstorm entry: the zombie rides the storm in from
 * the right edge of the lawn and only spawns when the storm reaches its
 * touchdown column. The storm then fades out (outro) over the touchdown point
 * while the freshly spawned zombie stays hidden behind it.
 */
public class SandstormSpawn {

    /** Seconds the storm travels from off-screen right to the touchdown column. */
    public static final float TRAVEL_SECONDS = 1.9f;

    /**
     * Seconds after touchdown the record is kept so the view can hide the
     * zombie behind the outro fade; generous upper bound on the outro clip.
     */
    public static final float OUTRO_SECONDS = 2f;

    private final GameModel gameModel;
    private final Zombie zombie;
    private final int lane;
    private final int columnsAhead;
    private float travelTime;
    private float outroTime;
    private boolean landed;
    private ZombieInstance spawned;

    SandstormSpawn(GameModel gameModel, Zombie zombie, int lane, int columnsAhead) {
        this.gameModel = gameModel;
        this.zombie = zombie;
        this.lane = lane;
        this.columnsAhead = columnsAhead;
    }

    /**
     * Advances the storm, spawning its zombie at touchdown.
     *
     * @return true once the outro window is over and the record can be dropped
     */
    boolean tick(float deltaTime) {
        if (!landed) {
            travelTime += deltaTime;
            if (travelTime >= TRAVEL_SECONDS) {
                landed = true;
                spawned = gameModel.spawnZombieWithTornado(zombie, lane, columnsAhead);
            }
            return false;
        }
        outroTime += deltaTime;
        return outroTime >= OUTRO_SECONDS;
    }

    /** Lane row the storm travels in. */
    public int getLane() {
        return lane;
    }

    /** Touchdown column (resolved against the current map width). */
    public int getColumn() {
        return GameModel.tornadoColumn(gameModel.getColumnCount(), columnsAhead);
    }

    /** {@code 0..1} travel progress; 1 once the touchdown column is reached. */
    public float travelProgress() {
        if (landed) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, travelTime / TRAVEL_SECONDS));
    }

    /** True once the storm has reached its touchdown column. */
    public boolean hasLanded() {
        return landed;
    }

    /** The zombie carried in by this storm, or {@code null} before touchdown. */
    public ZombieInstance getSpawned() {
        return spawned;
    }
}
