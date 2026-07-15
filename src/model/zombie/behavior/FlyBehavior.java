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
     * "high-HP" obstacles that the flying zombie cannot bypass.
     */
    public static final int HIGH_HP_THRESHOLD = 300;

    /** Tall nut definition name. */
    public static final String TALL_NUT_NAME = "Tall-nut";

    // --- State ---

    /** Current phase of the fly lifecycle. */
    private FlyPhase phase = FlyPhase.FLYING;

    /** The plant currently being eaten after landing; null if not eating. */
    private PlantInstance eatingTarget;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        switch (phase) {
            case FLYING:
                tickFlying(zombie, context, deltaTime);
                break;
            case LANDED:
                tickLanded(zombie, context, deltaTime);
                break;
            default:
                break;
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.FLY;
    }

    // --- Flying phase ---

    /**
     * While airborne the zombie continuously checks its current cell.
     * If a plant that blocks flight is present, the zombie lands and
     * begins eating that plant.
     */
    private void tickFlying(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        int row = zombie.getGridY();
        int col = zombie.getGridX();

        PlantInstance plant = context.getPlantAt(row, col);
        if (plant != null && plant.getCurrentHP() > 0 && shouldLandForPlant(plant)) {
            land(zombie, plant);
        }
    }

    // --- Landed phase ---

    /**
     * After landing the zombie behaves like a regular walker.
     */
    private void tickLanded(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        int row = zombie.getGridY();
        int col = zombie.getGridX();

        PlantInstance plantHere = context.getPlantAt(row, col);

        if (plantHere == null || plantHere.getCurrentHP() <= 0) {
            if (eatingTarget != null) {
                eatingTarget = null;
                zombie.stopEating();
            }
            return;
        }

        if (eatingTarget != plantHere) {
            eatingTarget = plantHere;
            zombie.startEating(plantHere);
        }

        float damage = zombie.getDefinition().getEatDPS() * deltaTime;
        if (damage > 0) {
            context.damagePlant(plantHere, (int) damage);
        }

        if (plantHere.getCurrentHP() <= 0) {
            eatingTarget = null;
            zombie.stopEating();
        }
    }

    // --- Plant-blocking logic ---

    /** Determines whether the given plant forces the flying zombie to land. */
    private boolean shouldLandForPlant(PlantInstance plant) {
        Plant definition = plant.getDefinition();

        // Plants that redirect zombies to another lane block flight
        if (definition.getTags().contains(PlantTags.MOVE_ZOMBIE)) {
            return true;
        }

        String name = definition.getName();

        // Tall-nut blocks flying zombies
        if (TALL_NUT_NAME.equalsIgnoreCase(name)) {
            return true;
        }

        // High-HP obstacle plants block flight
        return definition.getBaseHP() >= HIGH_HP_THRESHOLD;
    }

    // --- State transitions ---

    /**
     * Transitions the zombie from flying to landed state and begins
     * eating the given plant.
     */
    private void land(ZombieInstance zombie, PlantInstance plant) {
        phase = FlyPhase.LANDED;
        eatingTarget = plant;
        zombie.startEating(plant);
    }

    /**
     * Forcefully grounds the zombie without a specific plant target.
     * Can be called by external systems.
     */
    public void forceLand() {
        phase = FlyPhase.LANDED;
        eatingTarget = null;
    }

    // --- Getters ---

    /** @return true if the zombie is currently airborne */
    public boolean isFlying() {
        return phase == FlyPhase.FLYING;
    }

    public FlyPhase getPhase() {
        return phase;
    }

    public PlantInstance getEatingTarget() {
        return eatingTarget;
    }

    // --- Setters ---

    public void setPhase(FlyPhase phase) {
        this.phase = phase;
    }

    public void setEatingTarget(PlantInstance eatingTarget) {
        this.eatingTarget = eatingTarget;
    }

    // --- Inner types ---

    /**
     * The two phases of a flying zombie's lifecycle.
     */
    public enum FlyPhase {
        FLYING, // Zombie is airborne, bypassing non-blocking plants.
        LANDED // Zombie has landed permanently and walks/eats normally.
    }
}