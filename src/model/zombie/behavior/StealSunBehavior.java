package model.zombie.behavior;

import model.enums.SunType;
import model.enums.ZombieBehaviorType;
import model.item.Sun;
import model.plant.instance.PlantInstance;
import model.zombie.instance.BehaviorState;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sun-stealing behavior.
 */
public class StealSunBehavior implements ZombieBehavior {

    // --- Turquoise constants ---

    /** Turquoise steals this many sun per second from the player's reserve. */
    public static final float TURQUOISE_DRAIN_RATE = 25.0f;

    /** Tile radius in which the Turquoise zombie detects plants. */
    public static final int TURQUOISE_DETECTION_RANGE = 4;

    /** Base time (seconds) the Turquoise needs to charge before firing its laser. */
    public static final float TURQUOISE_CHARGE_TIME = 5.0f;

    /** How many cells ahead the Turquoise laser reaches. */
    public static final int TURQUOISE_LASER_RANGE = 4;

    /** Damage dealt by the Turquoise laser to each plant in range. 6767 is
     *  intentionally above any plant's HP to guarantee a one-shot kill. */
    public static final int TURQUOISE_LASER_DAMAGE = 6767;

    /** Fraction of stolen sun the Turquoise drops on death. */
    public static final float TURQUOISE_DEATH_RETURN_FRACTION = 0.5f;

    // --- States ---

    /** Accumulated sun stolen so far. */
    private int stolenSunAmount = 0;

    /** Suns physically pulled from the ground (Ra zombie). */
    private final List<Sun> capturedGroundSuns = new ArrayList<>();

    /** Which phase the Turquoise zombie is currently in. */
    private TurquoisePhase turquoisePhase = TurquoisePhase.DRAIN;

    /** Seconds elapsed in the current DRAIN phase. */
    private float drainTimer = 0f;

    /**
     * Whether the Turquoise zombie has detected a plant and started draining
     * in the current DRAIN cycle.
     */
    private boolean isDraining = false;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie.isDead()) { return; }

        if (isTurquoise(zombie)) {
            tickTurquoise(zombie, context, deltaTime);
        } else {
            tickRa(zombie, context, deltaTime);
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.STEAL_SUN;
    }

    // --- Ra Zombie ---

    private void tickRa(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        Iterator<Sun> iterator = context.getActiveSuns().iterator();
        while (iterator.hasNext()) {
            Sun sun = iterator.next();
            capturedGroundSuns.add(sun);
            stolenSunAmount += sun.getValue();
            iterator.remove(); // remove from the active-sun list
        }
    }

    // --- Turquoise Zombie ---

    private void tickTurquoise(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (context == null || zombie == null || zombie.isDead()) return;

        switch (turquoisePhase) {
            case DRAIN:
                tickDrainPhase(zombie, context, deltaTime);
                break;
            case LASER:
                fireLaser(zombie, context, deltaTime);
                break;
            default:
                break;
        }
    }

    /**
     * Drains sun from the player if a plant is detectable within range,
     * then after {@value #TURQUOISE_CHARGE_TIME} seconds triggers the laser.
     */
    private void tickDrainPhase(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        boolean plantInRange = isPlantInRange(zombie, context);

        if (plantInRange) {
            isDraining = true;
            drainTimer += deltaTime;

            // Drain from player reserve proportionally to elapsed time.
            float drainThisTick = TURQUOISE_DRAIN_RATE * deltaTime;
            int drainAmount = (int) drainThisTick;
            if (drainAmount > 0) {
                boolean spent = context.spendSun(drainAmount);
                if (spent) {
                    stolenSunAmount += drainAmount;
                }
            }

            // After the full drain window, switch to laser phase.
            if (drainTimer >= TURQUOISE_CHARGE_TIME) {
                turquoisePhase = TurquoisePhase.LASER;
                drainTimer = 0f;
            }
        } else {
            // No plant visible: pause drain timer but keep the phase.
            isDraining = false;
        }
    }

    /**
     * Fires a laser that instantly destroys every plant in the
     * {@value #TURQUOISE_LASER_RANGE} cells directly to the left of the zombie
     */
    private void fireLaser(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        int startCol = zombie.getGridX() - 1; // first cell in front
        int row = zombie.getGridY();
        int cols = context.getColumnCount();

        for (int col = startCol; col >= 0 && col > startCol - TURQUOISE_LASER_RANGE; col--) {
            if (col < 0 || col >= cols) continue;

            // Destroy the plant in this cell if there is one.
            PlantInstance plant = context.getPlantAt(row, col);
            if (plant != null) {
                context.damagePlant(plant, TURQUOISE_LASER_DAMAGE);
            }
        }

        // Reset back to drain phase.
        turquoisePhase = TurquoisePhase.DRAIN;
        isDraining = false;
    }

    // --- Death handling ---

    /**
     * Called by the game system when the Ra zombie is killed.
     * Returns all captured sun to the player's reserve.
     */
    public void onRaZombieDeath(BehaviorContext context) {
        if (context == null) return;
        context.addSun(stolenSunAmount);
        capturedGroundSuns.clear();
        stolenSunAmount = 0;
    }

    /**
     * Called by the game system when the Turquoise zombie is killed.
     * Half of the stolen sun is returned to the player.
     */
    public void onTurquoiseZombieDeath(BehaviorContext context) {
        if (context == null) return;
        int returned = (int) (stolenSunAmount * TURQUOISE_DEATH_RETURN_FRACTION);
        context.addSun(returned);
        stolenSunAmount = 0;
    }

    /**
     * Called by the game system whenever any sun-stealing zombie dies.
     *
     * @param zombie the zombie that just died.
     */
    public void onZombieDeath(ZombieInstance zombie, BehaviorContext context) {
        if(zombie == null) return;
        if (isTurquoise(zombie)) {
            onTurquoiseZombieDeath(context);
        } else {
            onRaZombieDeath(context);
        }
    }

    // --- Helpers ---

    /** @return true if this zombie is a Turquoise Zombie. */
    public boolean isTurquoise(ZombieInstance zombie) {
        String name = zombie.getDefinition().getName();
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("zombiecrystal") ||
                lower.contains("crystal");
    }

    /** @return true if this zombie is a Ra Zombie. */
    public boolean isRa(ZombieInstance zombie) {
        String name = zombie.getDefinition().getName();
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("zombiera");
    }

    /**
     * Returns true if there is at least one plant within {@value #TURQUOISE_DETECTION_RANGE}
     * tiles to the left of the zombie on the same row.
     */
    private boolean isPlantInRange(ZombieInstance zombie, BehaviorContext context) {
        int zombieCol = zombie.getGridX();
        int row = zombie.getGridY();

        for (int col = zombieCol - 1;  col >= 0 && col > zombieCol - 1 - TURQUOISE_DETECTION_RANGE;  col--) {
            if (context.getPlantAt(row, col) != null) {
                return true;
            }
        }
        return false;
    }

    // --- Getters ---

    public int getStolenSunAmount() {
        return stolenSunAmount;
    }

    public TurquoisePhase getTurquoisePhase() {
        return turquoisePhase;
    }

    public float getDrainTimer() {
        return drainTimer;
    }

    public boolean isDraining() {
        return isDraining;
    }

    // --- Inner types ---

    /**
     * The two alternating phases of the Turquoise zombie's attack cycle.
     */
    public enum TurquoisePhase {
        DRAIN, // Draining sun from the player's reserve
        LASER // Firing the destructive laser across 4 tiles
    }
}