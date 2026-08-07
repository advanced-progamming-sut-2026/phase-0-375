package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.zombie.instance.ZombieInstance;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Summon behavior.
 */
public class SummonBehavior implements ZombieBehavior {

    // --- Tomb Raiser constants ---

    /** Number of tombs summoned per cast. */
    public static final int TOMBS_PER_CAST = 2;

    /** Seconds between two consecutive summon casts. */
    public static final float TIME_BETWEEN_RAISINGS = 6.0f;

    /** Maximum number of random-cell attempts per tomb before giving up on that tomb. */
    private static final int MAX_PLACEMENT_ATTEMPTS = 10;

    // --- State ---

    /** Seconds elapsed since the last summon cast. */
    private float castTimer = 0f;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        castTimer += deltaTime;
        if (castTimer >= TIME_BETWEEN_RAISINGS) {
            castTimer -= TIME_BETWEEN_RAISINGS;
            summonTombs(context);
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.SUMMON;
    }

    // --- Core logic ---

    /**
     * Throws {@value #TOMBS_PER_CAST} bones at random cells on the map,
     * raising a grave wherever a bone lands on a free GROUND tile.
     */
    private void summonTombs(BehaviorContext context) {
        int rows = context.getRowCount();
        int cols = context.getColumnCount();
        if (rows <= 0 || cols <= 0) {
            return;
        }

        for (int i = 0; i < TOMBS_PER_CAST; i++) {
            summonOneTomb(context, rows, cols);
        }
    }

    /**
     * Picks a random cell and drops a grave there. Retries a few times
     * if the chosen cell already has something on the GROUND layer.
     */
    private void summonOneTomb(BehaviorContext context, int rows, int cols) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            int row = random.nextInt(rows);
            int col = random.nextInt(cols);

            if (context.spawnGraveAt(row, col)) {
                return;
            }
        }
    }

    // --- Getters ---

    public float getCastTimer() {
        return castTimer;
    }

    // --- Setters ---

    public void setCastTimer(float castTimer) {
        this.castTimer = castTimer;
    }
}