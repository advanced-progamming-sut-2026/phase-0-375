package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.zombie.instance.ZombieInstance;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Tomb Raiser: every {@link #TIME_BETWEEN_RAISINGS} seconds, raise up to
 * {@link #TOMBS_PER_CAST} graves, never more than {@link #TOMB_CAP} live tombs
 * owned by this raiser.
 */
public class SummonBehavior implements ZombieBehavior {

    // --- Tomb Raiser constants ---

    /** Number of tombs summoned per cast. */
    public static final int TOMBS_PER_CAST = 2;

    /** Live tombs this raiser may have on the lawn at once. */
    public static final int TOMB_CAP = 6;

    /** Seconds between two consecutive summon casts. */
    public static final float TIME_BETWEEN_RAISINGS = 6.0f;

    /** {@code power} clip length on {@code ZOMBIE_EGYPT_TOMBRAISER}. */
    public static final float POWER_DURATION = 3.0f;

    /** Maximum number of random-cell attempts per tomb before giving up on that tomb. */
    private static final int MAX_PLACEMENT_ATTEMPTS = 10;

    // --- State ---

    /** Seconds elapsed since the last summon cast. */
    private float castTimer = 0f;

    /** True while {@code power} plays; graves spawn when the clip ends. */
    private boolean raising = false;

    /** Seconds elapsed in {@link #POWER_DURATION}. */
    private float raiseTimer = 0f;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        if (raising) {
            raiseTimer += deltaTime;
            if (raiseTimer >= POWER_DURATION) {
                raising = false;
                raiseTimer = 0f;
                if (zombie.getState() == ZombieState.SPECIAL_ACTION) {
                    zombie.setState(ZombieState.WALKING);
                }
                int room = TOMB_CAP - context.countGravesRaisedBy(zombie);
                if (room > 0) {
                    summonTombs(zombie, context, Math.min(TOMBS_PER_CAST, room));
                }
            }
        }

        castTimer += deltaTime;
        if (raising || castTimer < TIME_BETWEEN_RAISINGS) {
            return;
        }
        castTimer -= TIME_BETWEEN_RAISINGS;

        if (context.countGravesRaisedBy(zombie) >= TOMB_CAP) {
            return;
        }
        startRaise(zombie);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.SUMMON;
    }

    // --- Core logic ---

    /**
     * Drops up to {@code limit} graves on random free GROUND tiles.
     *
     * @return how many graves were actually placed
     */
    private int summonTombs(ZombieInstance zombie, BehaviorContext context, int limit) {
        int rows = context.getRowCount();
        int cols = context.getColumnCount();
        if (rows <= 0 || cols <= 0 || limit <= 0) {
            return 0;
        }

        int spawned = 0;
        for (int i = 0; i < limit; i++) {
            if (summonOneTomb(zombie, context, rows, cols)) {
                spawned++;
            }
        }
        return spawned;
    }

    /**
     * Picks a random cell and drops a grave there. Retries a few times
     * if the chosen cell already has something on the GROUND layer.
     */
    private boolean summonOneTomb(ZombieInstance zombie, BehaviorContext context,
                                  int rows, int cols) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            int row = random.nextInt(rows);
            int col = random.nextInt(cols);

            if (context.spawnGraveAt(row, col, zombie)) {
                return true;
            }
        }
        return false;
    }

    private void startRaise(ZombieInstance zombie) {
        raising = true;
        raiseTimer = 0f;
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    // --- Getters ---

    public float getCastTimer() {
        return castTimer;
    }

    public boolean isRaising() {
        return raising;
    }

    public float getRaiseTimer() {
        return raiseTimer;
    }

    // --- Setters ---

    public void setCastTimer(float castTimer) {
        this.castTimer = castTimer;
    }
}
