package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.item.Sun;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sun-stealing behavior.
 */
public class StealSunBehavior implements ZombieBehavior {

    // --- Turquoise constants (defaults; overridden by JSON) ---

    /** Default Turquoise steal rate (sun per second). */
    public static final float DEFAULT_TURQUOISE_DRAIN_RATE = 25.0f;

    /** Default tile radius in which the Turquoise zombie detects plants. */
    public static final int DEFAULT_TURQUOISE_DETECTION_RANGE = 4;

    /** Default base time (seconds) the Turquoise needs to charge before firing its laser. */
    public static final float DEFAULT_TURQUOISE_CHARGE_TIME = 5.0f;

    /** Default how many cells ahead the Turquoise laser reaches. */
    public static final int DEFAULT_TURQUOISE_LASER_RANGE = 4;

    /** Default damage dealt by the Turquoise laser to each plant in range. */
    public static final int DEFAULT_TURQUOISE_LASER_DAMAGE = 4001;

    /** Fraction of stolen sun the Turquoise drops on death. */
    public static final float TURQUOISE_DEATH_RETURN_FRACTION = 0.5f;

    /** Max sun the Ra zombie can steal before it stops. JSON: {@code MaxClaimedSunCurrency}. */
    public static final int DEFAULT_RA_MAX_STOLEN_SUN = 5000;

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
        int maxStolen = zombie.getDefinition().getBehaviorPropInt(
                "MaxClaimedSunCurrency", DEFAULT_RA_MAX_STOLEN_SUN);
        if (maxStolen <= 0) maxStolen = DEFAULT_RA_MAX_STOLEN_SUN;

        Iterator<Sun> iterator = context.getActiveSuns().iterator();
        while (iterator.hasNext()) {
            if (stolenSunAmount >= maxStolen) {
                // Ra has hit its cap; stop capturing.
                break;
            }
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
     * then after the configured charge time triggers the laser. The
     * charge time shortens as more sun is stolen (per the JSON
     * {@code ChargingTimeDecrementPerFiveSun} field).
     */
    private void tickDrainPhase(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        boolean plantInRange = isPlantInRange(zombie, context);

        if (plantInRange) {
            isDraining = true;
            drainTimer += deltaTime;

            // Drain from player reserve proportionally to elapsed time.
            float drainRate = zombie.getDefinition().getBehaviorPropFloat(
                    "TurquoiseDrainRate", DEFAULT_TURQUOISE_DRAIN_RATE);
            if (drainRate <= 0f) drainRate = DEFAULT_TURQUOISE_DRAIN_RATE;
            float drainThisTick = drainRate * deltaTime;
            int drainAmount = (int) drainThisTick;
            if (drainAmount > 0) {
                boolean spent = context.spendSun(drainAmount);
                if (spent) {
                    stolenSunAmount += drainAmount;
                }
            }

            // Effective charge time shortens as more sun is stolen.
            float baseCharge = zombie.getDefinition().getBehaviorPropFloat(
                    "ChargingTime", DEFAULT_TURQUOISE_CHARGE_TIME);
            if (baseCharge <= 0f) baseCharge = DEFAULT_TURQUOISE_CHARGE_TIME;
            float decrementPerFiveSun = zombie.getDefinition().getBehaviorPropFloat(
                    "ChargingTimeDecrementPerFiveSun", 0f);
            float reduction = (stolenSunAmount / 5f) * decrementPerFiveSun;
            float effectiveCharge = Math.max(0.5f, baseCharge - reduction);

            // After the full drain window, switch to laser phase.
            if (drainTimer >= effectiveCharge) {
                turquoisePhase = TurquoisePhase.LASER;
                drainTimer = 0f;
            }
        } else {
            // No plant visible: pause drain timer but keep the phase.
            isDraining = false;
        }
    }

    /**
     * Fires a laser that instantly destroys every plant in the configured
     * laser range cells directly to the left of the zombie.
     */
    private void fireLaser(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        int laserRange = zombie.getDefinition().getBehaviorPropInt(
                "LaserBeamLength", DEFAULT_TURQUOISE_LASER_RANGE);
        // LaserBeamLength in JSON is in world-units (pixels); convert to
        // cells. The default 220 maps to 4 cells.
        if (laserRange > 50) laserRange = Math.max(1, laserRange / 55);
        if (laserRange <= 0) laserRange = DEFAULT_TURQUOISE_LASER_RANGE;

        int laserDamage = zombie.getDefinition().getBehaviorPropInt(
                "LaserBeamDamage", DEFAULT_TURQUOISE_LASER_DAMAGE);
        if (laserDamage <= 0) laserDamage = DEFAULT_TURQUOISE_LASER_DAMAGE;

        int startCol = zombie.getGridX() - 1; // first cell in front
        int row = zombie.getGridY();
        int cols = context.getColumnCount();

        for (int col = startCol; col >= 0 && col > startCol - laserRange; col--) {
            if (col < 0 || col >= cols) continue;

            // Destroy the plant in this cell if there is one.
            PlantInstance plant = context.getPlantAt(row, col);
            if (plant != null) {
                context.damagePlant(plant, laserDamage);
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
     * Called by the ZombieSystem whenever any sun-stealing zombie dies.
     *
     * @param zombie the zombie that just died.
     */
    @Override
    public void onZombieDeath(ZombieInstance zombie, BehaviorContext context) {
        if (zombie == null || context == null) return;
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
     * Returns true if there is at least one plant within the configured
     * detection range to the left of the zombie on the same row.
     */
    private boolean isPlantInRange(ZombieInstance zombie, BehaviorContext context) {
        int detectionRange = zombie.getDefinition().getBehaviorPropInt(
                "TurquoiseDetectionRange", DEFAULT_TURQUOISE_DETECTION_RANGE);
        if (detectionRange <= 0) detectionRange = DEFAULT_TURQUOISE_DETECTION_RANGE;

        int zombieCol = zombie.getGridX();
        int row = zombie.getGridY();

        for (int col = zombieCol - 1;  col >= 0 && col > zombieCol - 1 - detectionRange;  col--) {
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