package model.zombie.behavior;

import model.enums.GroundType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.game.map.Cell;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

/**
 * Swim behavior.
 */
public class SwimBehavior implements ZombieBehavior {

    // --- State ---

    /** Current phase of the swim cycle. */
    private SwimPhase phase = SwimPhase.WALKING;

    /** Plant currently being eaten while at the surface; null if not eating. */
    private PlantInstance eatingTarget;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        switch (phase) {
            case WALKING:
                tickWalking(zombie, context, deltaTime);
                break;
            case SUBMERGED:
                tickSubmerged(zombie, context, deltaTime);
                break;
            case SURFACED:
                tickSurfaced(zombie, context, deltaTime);
                break;
            default:
                break;
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.SWIM;
    }

    // --- Walking phase ---

    /**
     * The zombie is on land and behaves like a regular walker. As soon as
     * it steps onto a water tile, it dives and switches to the SUBMERGED
     * phase. Movement itself is handled by {@code ZombieSystem}.
     */
    private void tickWalking(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (isOnWater(zombie, context)) {
            dive(zombie);
        }
    }

    // --- Submerged phase ---

    /**
     * The zombie glides underwater, ignoring plants except to surface for
     * meals. If it has stepped onto a plant tile's facing border, it surfaces
     * to eat (becoming vulnerable) and immediately deals its first tick of
     * damage. If the zombie has drifted back onto land, it resurfaces and
     * walks normally.
     */
    private void tickSubmerged(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (!isOnWater(zombie, context)) {
            // Tide receded or zombie crossed back onto land - no reason to stay down.
            surfaceAndWalk(zombie);
            return;
        }

        PlantInstance plantHere = plantAtFacingBorder(zombie, context);
        if (plantHere != null && plantHere.getCurrentHP() > 0) {
            // Surface and start eating immediately; apply this tick's damage too.
            phase = SwimPhase.SURFACED;
            eatingTarget = plantHere;
            zombie.startEating(plantHere);
            applyEatDamage(zombie, context, deltaTime, plantHere);
        }
    }

    // --- Surfaced phase ---

    /**
     * The zombie is at the surface, eating a plant. It is fully vulnerable
     * to all plant attacks while in this phase. Once the plant is gone the
     * zombie dives again (if still on water) or resumes walking on land.
     */
    private void tickSurfaced(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        PlantInstance plantHere = plantAtFacingBorder(zombie, context);

        if (plantHere == null || plantHere.getCurrentHP() <= 0) {
            // Plant gone - stop eating and pick the next phase based on terrain.
            if (eatingTarget != null) {
                eatingTarget = null;
                zombie.stopEating();
            }

            if (isOnWater(zombie, context)) {
                dive(zombie);
            } else {
                surfaceAndWalk(zombie);
            }
            return;
        }

        if (eatingTarget != plantHere) {
            eatingTarget = plantHere;
            zombie.startEating(plantHere);
        }

        applyEatDamage(zombie, context, deltaTime, plantHere);
    }

    /**
     * Applies one tick's worth of eating damage to the plant. If the plant
     * drops to zero HP, the zombie stops eating.
     */
    private void applyEatDamage(ZombieInstance zombie, BehaviorContext context,
                                float deltaTime, PlantInstance plant) {
        float damage = zombie.getDefinition().getEatDPS() * deltaTime;
        if (damage > 0) {
            context.damagePlant(plant, (int) damage);
        }
        if (plant.getCurrentHP() <= 0) {
            eatingTarget = null;
            zombie.stopEating();
        }
    }

    // --- State transitions ---

    /**
     * Submerges the zombie underwater.
     */
    private void dive(ZombieInstance zombie) {
        phase = SwimPhase.SUBMERGED;
        eatingTarget = null;
        if (zombie.getState() == ZombieState.EATING) {
            zombie.setState(ZombieState.WALKING);
        }
    }

    /** Brings the zombie back to the surface and resumes normal walking. */
    private void surfaceAndWalk(ZombieInstance zombie) {
        phase = SwimPhase.WALKING;
        eatingTarget = null;
        if (zombie.getState() == ZombieState.EATING) {
            zombie.setState(ZombieState.WALKING);
        }
    }

    // --- Terrain helpers ---

    private static PlantInstance plantAtFacingBorder(ZombieInstance zombie, BehaviorContext context) {
        int eatCol = zombie.plantColumnAtFacingBorder();
        return eatCol < 0 ? null : context.getPlantAt(zombie.getGridY(), eatCol);
    }

    /**
     * @return true if the zombie's current cell is a water tile (either
     *         deep water or low-tide shallow water). Both are passable
     *         only to swimming zombies and force the zombie to dive.
     */
    private boolean isOnWater(ZombieInstance zombie, BehaviorContext context) {
        int row = zombie.getGridY();
        int col = zombie.getGridX();
        if (row < 0 || col < 0
                || row >= context.getRowCount()
                || col >= context.getColumnCount()) {
            return false;
        }
        Cell cell = context.getCellAt(row, col);
        if (cell == null) {
            return false;
        }
        GroundType ground = cell.getGroundType();
        return ground == GroundType.WATER || ground == GroundType.LOW_TIDE;
    }

    // --- Public state queries ---

    /**
     * @return true while the zombie is fully submerged underwater. Combat
     *         and projectile systems should consult this (or
     *         {@link ZombieInstance#isSubmerged()}) before applying damage.
     *         only lobber plants can hit a submerged zombie.
     */
    public boolean isSubmerged() {
        return phase == SwimPhase.SUBMERGED;
    }

    /** @return true while the zombie is at the surface eating a plant (fully vulnerable). */
    public boolean isSurfaced() {
        return phase == SwimPhase.SURFACED;
    }

    // --- Getters / setters ---

    public SwimPhase getPhase() {
        return phase;
    }

    public void setPhase(SwimPhase phase) {
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
     * The three phases of a swimming zombie's lifecycle.
     */
    public enum SwimPhase {
        WALKING, // On land; behaves like a regular walker.
        SUBMERGED, // Underwater; only lobbers can damage it.
        SURFACED // At the surface, eating a plant; fully vulnerable.
    }
}