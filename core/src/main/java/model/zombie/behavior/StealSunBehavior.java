package model.zombie.behavior;

import model.enums.SunType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.item.Sun;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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

    /** {@code power_up} clip length on Ra and Crystal Skull. */
    public static final float POWER_UP_DURATION = 0.6667f;

    /** Seconds Crystal Skull loops {@code power} before {@code power_down}. JSON {@code ChargingTime}. */
    public static final float POWER_DURATION = DEFAULT_TURQUOISE_CHARGE_TIME;

    /** {@code power} clip length on {@code ZOMBIE_EGYPT_RA}. Loops until pulls arrive. */
    public static final float RA_POWER_DURATION = 1f;

    /** Seconds between Ra steal scans while walking. */
    public static final float RA_SCAN_INTERVAL = 2f;

    /** Seconds for a claimed sun to fly to Ra. */
    public static final float RA_PULL_DURATION = 1f;

    /** Short fall when stolen sun scatters on death. */
    public static final float RA_DEATH_FALL = 1.5f;

    /** {@code power_down} clip length on Ra and Crystal Skull. */
    public static final float POWER_DOWN_DURATION = 1.2667f;

    /** {@code attack} clip length on {@code ZOMBIE_LOSTCITY_CRYSTALSKULL}. */
    public static final float ATTACK_DURATION = 1.9667f;

    /** Seconds into {@code attack} when {@code zombie_egypt_ra_staff_whiteglow} fires the beam. */
    public static final float ATTACK_BEAM_AT = 0.63f;

    /** Max sun the Ra zombie can steal before it stops. JSON: {@code MaxClaimedSunCurrency}. */
    public static final int DEFAULT_RA_MAX_STOLEN_SUN = 5000;

    // --- States ---

    /** Accumulated sun stolen so far. */
    private int stolenSunAmount = 0;

    /** Fractional sun waiting to be spent (drain rate is 25/s; ticks are smaller). */
    private float drainRemainder = 0f;

    /** Suns flying to Ra this absorb cycle. */
    private final List<SunPull> pulling = new ArrayList<>();

    /** Stolen tokens kept for death scatter (type + value). */
    private final List<Sun> captured = new ArrayList<>();

    private float raScanTimer = 0f;

    private final Random rng = new Random();

    /** Which phase the Turquoise zombie is currently in. */
    private TurquoisePhase turquoisePhase = TurquoisePhase.WALKING;

    /** Seconds stolen this charge cycle ({@code power_up} + {@code power} + {@code power_down}). */
    private float drainTimer = 0f;

    /** Seconds elapsed in the current clip phase. */
    private float phaseTimer = 0f;

    /**
     * Whether the Turquoise zombie has detected a plant and started draining
     * in the current charge cycle.
     */
    private boolean isDraining = false;

    /** True after the laser has dealt damage this {@code attack} clip. */
    private boolean laserFired = false;

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
        if (context == null || zombie == null) {
            return;
        }
        switch (turquoisePhase) {
            case WALKING -> tickRaWalking(zombie, context, deltaTime);
            case POWER_UP -> tickRaPowerUp(zombie, context, deltaTime);
            case POWER -> tickRaPower(zombie, context, deltaTime);
            case POWER_DOWN -> tickRaPowerDown(zombie, context, deltaTime);
            case ATTACK -> { /* Ra has no attack clip. */ }
        }
    }

    private void tickRaWalking(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        raScanTimer += deltaTime;
        if (raScanTimer < RA_SCAN_INTERVAL) {
            return;
        }
        raScanTimer = 0f;
        if (!canStealMore(zombie) || !hasGroundSun(context)) {
            return;
        }
        turquoisePhase = TurquoisePhase.POWER_UP;
        phaseTimer = 0f;
        zombie.stopEating();
        zombie.setState(ZombieState.SPECIAL_ACTION);
        beginPulls(zombie, context);
        tickRaPowerUp(zombie, context, deltaTime);
    }

    private void tickRaPowerUp(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        tickPulls(zombie, context, deltaTime);
        phaseTimer += deltaTime;
        if (phaseTimer >= POWER_UP_DURATION) {
            phaseTimer = 0f;
            turquoisePhase = TurquoisePhase.POWER;
        }
    }

    private void tickRaPower(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        tickPulls(zombie, context, deltaTime);
        phaseTimer += deltaTime;
        if (pulling.isEmpty() && phaseTimer >= RA_POWER_DURATION) {
            phaseTimer = 0f;
            turquoisePhase = TurquoisePhase.POWER_DOWN;
        }
    }

    private void tickRaPowerDown(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        tickPulls(zombie, context, deltaTime);
        phaseTimer += deltaTime;
        if (phaseTimer >= POWER_DOWN_DURATION) {
            phaseTimer = 0f;
            pulling.clear();
            turquoisePhase = TurquoisePhase.WALKING;
            zombie.setState(ZombieState.WALKING);
        }
    }

    private void beginPulls(ZombieInstance zombie, BehaviorContext context) {
        pulling.clear();
        List<Sun> ground = context.getActiveSuns();
        if (ground == null) {
            return;
        }
        int room = raMaxStolen(zombie) - stolenSunAmount;
        for (Sun sun : ground) {
            if (room <= 0) {
                break;
            }
            pulling.add(new SunPull(sun));
            room -= sun.getValue();
        }
    }

    private void tickPulls(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (pulling.isEmpty()) {
            return;
        }
        List<Sun> ground = context.getActiveSuns();
        Iterator<SunPull> it = pulling.iterator();
        while (it.hasNext()) {
            SunPull pull = it.next();
            if (ground == null || !ground.contains(pull.sun)) {
                it.remove();
                continue;
            }
            pull.t += deltaTime / RA_PULL_DURATION;
            if (pull.t >= 1f) {
                context.removeSun(pull.sun);
                stolenSunAmount += pull.sun.getValue();
                captured.add(new Sun(pull.sun.getType(), pull.sun.getValue(), 0, 0));
                it.remove();
            }
        }
    }

    private int raMaxStolen(ZombieInstance zombie) {
        int maxStolen = zombie.getDefinition().getBehaviorPropInt(
                "MaxClaimedSunCurrency", DEFAULT_RA_MAX_STOLEN_SUN);
        return maxStolen <= 0 ? DEFAULT_RA_MAX_STOLEN_SUN : maxStolen;
    }

    private boolean canStealMore(ZombieInstance zombie) {
        return stolenSunAmount < raMaxStolen(zombie);
    }

    private static boolean hasGroundSun(BehaviorContext context) {
        List<Sun> ground = context.getActiveSuns();
        return ground != null && !ground.isEmpty();
    }

    // --- Turquoise Zombie ---

    private void tickTurquoise(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (context == null || zombie == null || zombie.isDead()) return;

        switch (turquoisePhase) {
            case WALKING -> tickWalking(zombie, context, deltaTime);
            case POWER_UP -> tickPowerUp(zombie, context, deltaTime);
            case POWER -> tickPower(zombie, context, deltaTime);
            case POWER_DOWN -> tickPowerDown(zombie, context, deltaTime);
            case ATTACK -> tickAttack(zombie, context, deltaTime);
        }
    }

    private void tickWalking(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (!isPlantInRange(zombie, context)) {
            isDraining = false;
            return;
        }
        beginPowerUp(zombie);
        tickPowerUp(zombie, context, deltaTime);
    }

    private void beginPowerUp(ZombieInstance zombie) {
        turquoisePhase = TurquoisePhase.POWER_UP;
        phaseTimer = 0f;
        drainTimer = 0f;
        drainRemainder = 0f;
        laserFired = false;
        isDraining = true;
        zombie.stopEating();
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    private void tickPowerUp(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        drainIfPlantInRange(zombie, context, deltaTime);
        phaseTimer += deltaTime;
        if (phaseTimer >= POWER_UP_DURATION) {
            phaseTimer = 0f;
            turquoisePhase = TurquoisePhase.POWER;
        }
    }

    /** Loops {@code power} for {@link #POWER_DURATION} (JSON {@code ChargingTime}). */
    private void tickPower(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        drainIfPlantInRange(zombie, context, deltaTime);
        phaseTimer += deltaTime;
        if (phaseTimer >= powerDuration(zombie)) {
            phaseTimer = 0f;
            turquoisePhase = TurquoisePhase.POWER_DOWN;
        }
    }

    private void tickPowerDown(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        drainIfPlantInRange(zombie, context, deltaTime);
        phaseTimer += deltaTime;
        if (phaseTimer >= POWER_DOWN_DURATION) {
            phaseTimer = 0f;
            laserFired = false;
            isDraining = false;
            turquoisePhase = TurquoisePhase.ATTACK;
        }
    }

    private void tickAttack(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        phaseTimer += deltaTime;
        if (!laserFired && phaseTimer >= ATTACK_BEAM_AT) {
            fireLaser(zombie, context);
            laserFired = true;
        }
        if (phaseTimer >= ATTACK_DURATION) {
            turquoisePhase = TurquoisePhase.WALKING;
            phaseTimer = 0f;
            drainTimer = 0f;
            drainRemainder = 0f;
            isDraining = false;
            laserFired = false;
            zombie.setState(ZombieState.WALKING);
        }
    }

    private void drainIfPlantInRange(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (!isPlantInRange(zombie, context)) {
            isDraining = false;
            return;
        }
        isDraining = true;
        drainTimer += deltaTime;
        drainRemainder += DEFAULT_TURQUOISE_DRAIN_RATE * deltaTime;
        int drainAmount = (int) drainRemainder;
        if (drainAmount <= 0) {
            return;
        }
        drainRemainder -= drainAmount;
        if (context.spendSun(drainAmount)) {
            stolenSunAmount += drainAmount;
        }
    }

    private float powerDuration(ZombieInstance zombie) {
        float seconds = zombie.getDefinition().getBehaviorPropFloat(
                "ChargingTime", POWER_DURATION);
        return seconds > 0f ? seconds : POWER_DURATION;
    }

    /**
     * Fires a laser that instantly destroys every plant in the configured
     * laser range cells directly to the left of the zombie.
     */
    private void fireLaser(ZombieInstance zombie, BehaviorContext context) {
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

            PlantInstance plant = context.getPlantAt(row, col);
            if (plant != null) {
                context.damagePlant(plant, laserDamage);
            }
        }
    }

    // --- Death handling ---

    /**
     * Called by the game system when the Ra zombie is killed.
     * Returns all captured sun to the player's reserve.
     */
    public void onRaZombieDeath(ZombieInstance zombie, BehaviorContext context) {
        if (context == null) {
            return;
        }
        pulling.clear();
        dropScatteredSuns(zombie, context);
        captured.clear();
        stolenSunAmount = 0;
    }

    /**
     * Called by the game system when the Turquoise zombie is killed.
     * Half of the stolen sun is returned to the player.
     */
    public void onTurquoiseZombieDeath(ZombieInstance zombie, BehaviorContext context) {
        if (context == null) return;
        int returned = (int) (stolenSunAmount * TURQUOISE_DEATH_RETURN_FRACTION);
        dropMultipleSuns(zombie, context, returned);
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
            onTurquoiseZombieDeath(zombie, context);
        } else {
            onRaZombieDeath(zombie, context);
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
        int detectionRange = DEFAULT_TURQUOISE_DETECTION_RANGE;

        int zombieCol = zombie.getGridX();
        int row = zombie.getGridY();

        for (int col = zombieCol - 1;  col >= 0 && col > zombieCol - 1 - detectionRange;  col--) {
            if (context.getPlantAt(row, col) != null) {
                return true;
            }
        }
        return false;
    }

    private void dropSun(ZombieInstance zombie, BehaviorContext context, int amount) {
        int row = zombie.getGridY();
        int col = zombie.getGridX();
        Sun sun = new Sun(SunType.NORMAL, amount, col, row);
        context.spawnSun(sun);
    }

    private void dropScatteredSuns(ZombieInstance zombie, BehaviorContext context) {
        int cols = Math.max(1, context.getColumnCount());
        int rows = Math.max(1, context.getRowCount());
        int originCol = zombie.getGridX();
        int originRow = zombie.getGridY();
        float fromX = zombie.getContinuousPosition() != null
                ? zombie.getContinuousX() : originCol;
        float fromY = zombie.getContinuousPosition() != null
                ? zombie.getContinuousY() : originRow;
        for (Sun kept : captured) {
            int col = clamp(originCol + rng.nextInt(3) - 1, 0, cols - 1);
            int row = clamp(originRow + rng.nextInt(3) - 1, 0, rows - 1);
            SunType type = kept.getType() != null ? kept.getType() : SunType.NORMAL;
            Sun sun = new Sun(type, kept.getValue(), col, row);
            sun.setOffset((rng.nextFloat() - 0.5f) * 0.8f, (rng.nextFloat() - 0.5f) * 0.8f);
            sun.setFall(RA_DEATH_FALL, RA_DEATH_FALL);
            sun.setOrigin(fromX, fromY);
            context.spawnSun(sun);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void dropMultipleSuns(ZombieInstance zombie, BehaviorContext context, int amount) {
        int numOfSuns = (int) Math.ceil(amount / 50f);
        for (int i = 0; i < numOfSuns; i++) {
            int currentSunAmount = Math.min(50, amount);
            dropSun(zombie, context, currentSunAmount);
            amount -= 50;
        }
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

    public float getPhaseTimer() {
        return phaseTimer;
    }

    public boolean isDraining() {
        return isDraining;
    }

    public boolean hasFiredLaser() {
        return laserFired;
    }

    public List<SunPull> getPulls() {
        return pulling;
    }

    // --- Inner types ---

    /** One lawn sun flying to Ra. {@code t} is 0..1 along {@link #RA_PULL_DURATION}. */
    public static final class SunPull {
        private final Sun sun;
        private final int startCol;
        private final int startRow;
        private final float startOffsetX;
        private final float startOffsetY;
        private final float startFallRemaining;
        private final float startFallDuration;
        private float t;

        SunPull(Sun sun) {
            this.sun = sun;
            this.startCol = sun.getX();
            this.startRow = sun.getY();
            this.startOffsetX = sun.getOffsetX();
            this.startOffsetY = sun.getOffsetY();
            this.startFallRemaining = sun.getFallRemaining();
            this.startFallDuration = sun.getFallDuration();
        }

        public Sun sun() {
            return sun;
        }

        public int startCol() {
            return startCol;
        }

        public int startRow() {
            return startRow;
        }

        public float startOffsetX() {
            return startOffsetX;
        }

        public float startOffsetY() {
            return startOffsetY;
        }

        public float startFallRemaining() {
            return startFallRemaining;
        }

        public float startFallDuration() {
            return startFallDuration;
        }

        public float t() {
            return t;
        }
    }

    /**
     * Charge clips shared by Ra and Crystal Skull. Skull continues to {@code attack};
     * Ra returns to {@code WALKING} after {@code power_down}.
     */
    public enum TurquoisePhase {
        WALKING,
        POWER_UP,
        POWER,
        POWER_DOWN,
        ATTACK
    }
}
