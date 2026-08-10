package model.zombie.behavior;

import model.enums.PlantTags;
import model.enums.ZombieBehaviorType;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

/**
 * Fly behavior
 */
public class FlyBehavior implements ZombieBehavior {

    // --- Constants ---

    /**
     * Plants whose base HP meets or exceeds this threshold are considered
     * "high-HP" obstacles (nuts) that the flying zombie flies over.
     */
    public static final int HIGH_HP_THRESHOLD = 1000;

    /** Tall nut definition name — blocks flight. */
    public static final String TALL_NUT_NAME = "Tall-nut";

    // --- State ---

    /** Current phase of the fly lifecycle. Starts grounded; takes off only over flyable plants. */
    private FlyPhase phase = FlyPhase.LANDED;

    /** The plant currently being eaten; null if not eating. */
    private PlantInstance eatingTarget;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        int row = zombie.getGridY();
        int col = zombie.getGridX();
        if (row < 0 || col < 0
                || row >= context.getRowCount()
                || col >= context.getColumnCount()) {
            return;
        }

        syncToPlant(zombie, context.getPlantAt(row, col));
    }

    /**
     * Updates flight phase for the plant currently under the zombie.
     * Called from the behavior tick and again after movement so entering a
     * flyable cell takes off before {@code ZombieSystem} can start eating.
     */
    public void syncToPlant(ZombieInstance zombie, PlantInstance plant) {
        boolean shouldFly = shouldFlyOver(plant);

        if (shouldFly && phase != FlyPhase.FLYING) {
            takeOff(zombie);
        } else if (!shouldFly && phase != FlyPhase.LANDED) {
            land(zombie);
        }

        if (phase == FlyPhase.LANDED) {
            eatingTarget = plant;
        } else {
            eatingTarget = null;
            if (zombie != null && zombie.isEating()) {
                zombie.stopEating();
            }
        }
    }

    /**
     * @return true if this flying zombie should pass over {@code plant}
     *         without stopping to eat it
     */
    public boolean shouldFlyOver(PlantInstance plant) {
        return plant != null
                && plant.getCurrentHP() > 0
                && isFlyableObstacle(plant);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.FLY;
    }

    // --- Obstacle classification ---

    /**
     * Returns {@code true} if the given plant is an obstacle or dangerous
     * plant that this flying zombie should fly over rather than stop and eat.
     * Spec: high-HP nuts, lane-redirectors, and traps/mines; never Tall-nut.
     */
    private boolean isFlyableObstacle(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null) return false;

        String name = def.getName();
        if (name != null && TALL_NUT_NAME.equalsIgnoreCase(name)) {
            return false;
        }

        // Lane-redirecting plants (Garlic, Sweet Potato, …).
        if (def.hasTag(PlantTags.MOVE_ZOMBIE)) {
            return true;
        }

        // Dangerous trap plants (Potato Mine, …).
        if (def.hasTag(PlantTags.TRAP)) {
            return true;
        }

        // High-HP obstacle plants (Wall-nut, …).
        return def.getBaseHP() >= HIGH_HP_THRESHOLD;
    }

    // --- State transitions ---

    /** Transitions the zombie from LANDED to FLYING. */
    private void takeOff(ZombieInstance zombie) {
        phase = FlyPhase.FLYING;
        eatingTarget = null;
        if (zombie.isEating()) {
            zombie.stopEating();
        }
    }

    /** Transitions the zombie from FLYING to LANDED. */
    private void land(ZombieInstance zombie) {
        phase = FlyPhase.LANDED;
    }

    /**
     * Forcefully grounds the zombie without a specific plant target.
     * Can be called by external systems.
     */
    public void forceLand() {
        phase = FlyPhase.LANDED;
        eatingTarget = null;
    }

    // --- Getters / setters ---

    /** @return true if the zombie is currently airborne */
    public boolean isFlying() {
        return phase == FlyPhase.FLYING;
    }

    public FlyPhase getPhase() {
        return phase;
    }

    public void setPhase(FlyPhase phase) {
        this.phase = phase;
    }

    public PlantInstance getEatingTarget() {
        return eatingTarget;
    }

    public void setEatingTarget(PlantInstance eatingTarget) {
        this.eatingTarget = eatingTarget;
    }

    // --- Inner types ---

    /**
     * The two phases of a flying zombie's lifecycle.
     */
    public enum FlyPhase {
        LANDED,
        FLYING
    }
}
