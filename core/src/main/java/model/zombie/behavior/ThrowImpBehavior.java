package model.zombie.behavior;

import model.enums.ImpType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.zombie.instance.ZombieInstance;

/**
 * Throw-imp behavior.
 */
public class ThrowImpBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Default definition name used to look up and spawn the thrown Imp. */
    public static final String DEFAULT_IMP_NAME = "ZombieImp";

    /** Default fraction of max HP at (or below) which the Gargantuar throws its Imp. */
    public static final float DEFAULT_HEALTH_PERCENT_THROW_IMP = 0.5f;

    /** Default column the Imp is thrown to. */
    public static final int DEFAULT_IMP_TARGET_COLUMN = 2;

    /** Seconds the Gargantuar spends winding up ({@code fire} clip). */
    public static final float FIRE_DURATION = 0.9667f;

    /** Seconds the toss clip ({@code cannon_fire}) lasts. */
    public static final float CANNON_DURATION = 0.5667f;

    /** Seconds into {@code cannon_fire} when the Imp leaves the Gargantuar. */
    public static final float RELEASE_AT = 0.13f;

    /** Seconds the thrown Imp spends in the air. */
    public static final float FLIGHT_DURATION = 0.85f;

    /** Peak height of the throw arc, in tiles. */
    public static final float FLIGHT_APEX_TILES = 1.25f;

    /** Seconds the Imp spends playing {@code land} after hitting the ground. */
    public static final float LAND_DURATION = 1f;

    // --- State ---

    /** True once this Gargantuar has thrown its Imp. */
    private boolean hasThrownImp = false;

    /** Current phase of the throw animation. IDLE until the throw starts. */
    private ThrowPhase throwPhase = ThrowPhase.IDLE;

    /** Seconds elapsed in the current FIRE / CANNON phase. */
    private float throwTimer = 0f;

    /** True once the Imp has left the Gargantuar sprite and been spawned. */
    private boolean released = false;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) return;

        if (throwPhase == ThrowPhase.FIRE || throwPhase == ThrowPhase.CANNON) {
            tickThrowing(zombie, context, deltaTime);
            return;
        }

        if (hasThrownImp) return;

        if (!hasReachedThrowThreshold(zombie)) {
            return;
        }

        startThrow(zombie);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.THROW_IMP;
    }

    // --- Core logic ---

    /**
     * @return true if the Gargantuar's current HP has dropped to or below
     * the configured threshold of its max HP.
     */
    private boolean hasReachedThrowThreshold(ZombieInstance zombie) {
        int maxHP = zombie.getDefinition().getBaseHP();
        if (maxHP <= 0) return false;

        float threshold = zombie.getDefinition().getBehaviorPropFloat(
                "HealthPercentThrowImp", DEFAULT_HEALTH_PERCENT_THROW_IMP);
        if (threshold <= 0f || threshold > 1f) {
            threshold = DEFAULT_HEALTH_PERCENT_THROW_IMP;
        }
        float healthFraction = (float) zombie.getCurrentHP() / maxHP;
        return healthFraction <= threshold;
    }

    /**
     * Resolves which imp definition to spawn based on the zombie's
     * configured {@link ImpType}.
     */
    private String resolveImpName(ZombieInstance zombie) {
        ImpType impType = zombie.getDefinition().getImpType();
        if (impType == null) return DEFAULT_IMP_NAME;
        switch (impType) {
            case EGYPT_IMP: return "ZombieImp";
            case ICEAGE_IMP: return "ZombieImp";
            case DRAGON_IMP: return "ZombieDarkImpDragon";
            default: return DEFAULT_IMP_NAME;
        }
    }

    /** Begins the fire → cannon_fire cycle. The Imp is spawned at {@link #RELEASE_AT}. */
    private void startThrow(ZombieInstance zombie) {
        SmashBehavior smash = (SmashBehavior) zombie.getBehavior(ZombieBehaviorType.SMASH);
        if (smash != null) {
            smash.cancelForThrow();
        }
        hasThrownImp = true;
        released = false;
        throwTimer = 0f;
        throwPhase = ThrowPhase.FIRE;
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    private void tickThrowing(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        throwTimer += deltaTime;
        if (throwPhase == ThrowPhase.FIRE) {
            if (throwTimer >= FIRE_DURATION) {
                throwTimer = 0f;
                throwPhase = ThrowPhase.CANNON;
            }
            return;
        }
        if (!released && throwTimer >= RELEASE_AT) {
            throwImp(zombie, context);
        }
        if (throwTimer >= CANNON_DURATION) {
            throwPhase = ThrowPhase.IDLE;
            zombie.setState(ZombieState.WALKING);
        }
    }

    /**
     * Spawns an Imp on the Gargantuar's tile; {@link Flight} carries it to
     * the configured target column of the Gargantuar's row.
     */
    private void throwImp(ZombieInstance zombie, BehaviorContext context) {
        released = true;
        int row = zombie.getGridY();
        int targetCol = zombie.getDefinition().getBehaviorPropInt(
                "ImpTargetColumn", DEFAULT_IMP_TARGET_COLUMN);
        if (targetCol < 0) targetCol = DEFAULT_IMP_TARGET_COLUMN;
        String impName = resolveImpName(zombie);
        int spawnCol = Math.max(0, Math.min(zombie.getGridX(), context.getColumnCount() - 1));
        ZombieInstance imp = context.spawnZombieAt(impName, row, spawnCol);
        if (imp != null) {
            float startX = zombie.getContinuousX();
            imp.setGridPosition(new Point(spawnCol, row));
            imp.setContinuousPosition(new FloatPoint(startX, row));
            imp.setState(ZombieState.SPECIAL_ACTION);
            imp.getBehaviors().add(new Flight(zombie, startX, row, targetCol));
        }
    }

    public static Flight flightOf(ZombieInstance zombie) {
        if (zombie == null || zombie.getBehaviors() == null) {
            return null;
        }
        for (ZombieBehavior behavior : zombie.getBehaviors()) {
            if (behavior instanceof Flight flight) {
                return flight;
            }
        }
        return null;
    }

    // --- Getters ---

    public boolean hasThrownImp() {
        return hasThrownImp;
    }

    public boolean isThrowing() {
        return throwPhase == ThrowPhase.FIRE || throwPhase == ThrowPhase.CANNON;
    }

    public boolean hasReleasedImp() {
        return released;
    }

    public ThrowPhase getThrowPhase() {
        return throwPhase;
    }

    public float getThrowTimer() {
        return throwTimer;
    }

    // --- Setters ---

    public void setHasThrownImp(boolean hasThrownImp) {
        this.hasThrownImp = hasThrownImp;
    }

    // --- Inner types ---

    public enum ThrowPhase {
        IDLE, FIRE, CANNON
    }

    /** Attached to the spawned Imp: flies to {@code targetCol}, then lands. */
    public static final class Flight implements ZombieBehavior {
        private final ZombieInstance thrower;
        private final float startX;
        private final int row;
        private final int targetCol;
        private FlightPhase phase = FlightPhase.FLYING;
        private float timer;

        Flight(ZombieInstance thrower, float startX, int row, int targetCol) {
            this.thrower = thrower;
            this.startX = startX;
            this.row = row;
            this.targetCol = targetCol;
        }

        @Override
        public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
            if (zombie == null || zombie.isDead()) return;

            if (phase == FlightPhase.FLYING) {
                timer += deltaTime;
                float t = progress();
                float x = startX + (targetCol - startX) * t;
                zombie.setContinuousX(x);
                int gridX = (int) Math.floor(x);
                if (gridX != zombie.getGridX()) {
                    zombie.setGridX(gridX);
                }
                if (timer >= FLIGHT_DURATION) {
                    zombie.setContinuousPosition(new FloatPoint(targetCol, row));
                    zombie.setGridPosition(new Point(targetCol, row));
                    timer = 0f;
                    phase = FlightPhase.LANDING;
                }
                return;
            }
            if (phase == FlightPhase.LANDING) {
                timer += deltaTime;
                if (timer >= LAND_DURATION) {
                    phase = FlightPhase.DONE;
                    zombie.setState(ZombieState.WALKING);
                }
            }
        }

        @Override
        public ZombieBehaviorType getType() {
            return ZombieBehaviorType.THROW_IMP;
        }

        public ZombieInstance thrower() {
            return thrower;
        }

        public boolean isFlying() {
            return phase == FlightPhase.FLYING;
        }

        public boolean isLanding() {
            return phase == FlightPhase.LANDING;
        }

        /** 0 at release, 1 on landing. */
        public float progress() {
            if (phase != FlightPhase.FLYING) {
                return 1f;
            }
            return Math.min(1f, timer / FLIGHT_DURATION);
        }

        /** Parabola in tiles; 0 on the ground. */
        public float heightTiles() {
            if (phase != FlightPhase.FLYING) {
                return 0f;
            }
            float t = progress();
            return 4f * FLIGHT_APEX_TILES * t * (1f - t);
        }

        private enum FlightPhase {
            FLYING, LANDING, DONE
        }
    }
}
