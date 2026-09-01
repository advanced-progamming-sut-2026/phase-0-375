package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

/**
 * Fisherman: intro, then idle in place and periodically {@code cast} → hook a
 * plant one tile closer (or toss it) → {@code reel}.
 */
public class FishBehavior implements ZombieBehavior {

    public enum FishPhase { INTRO, IDLE, CASTING, REELING }

    /** {@code intro} on {@code ZOMBIE_BEACH_FISHERMAN}. */
    public static final float INTRO_DURATION = 1.6333f;
    /** {@code cast} clip length. */
    public static final float CAST_DURATION = 1.2667f;
    /** {@code reel} clip length. */
    public static final float REEL_DURATION = 1.4667f;
    /** Seconds between two consecutive hook casts. */
    public static final float DELAY_BETWEEN_CASTING = 2.5f;
    /** Pause after the hook lands before {@code reel}. */
    public static final float DELAY_BEFORE_REELING = 0.3f;

    private FishPhase phase = FishPhase.INTRO;
    private float phaseTimer = 0f;
    private float castTimer = 0f;
    private boolean plantHooked;

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }
        if (tickIntro(zombie, deltaTime)) {
            return;
        }
        holdStation(zombie);
        if (tickCasting(zombie, context, deltaTime) || tickReeling(deltaTime)) {
            return;
        }
        beginCastIfReady(zombie, context, deltaTime);
    }

    private boolean tickIntro(ZombieInstance zombie, float deltaTime) {
        if (phase != FishPhase.INTRO) {
            return false;
        }
        zombie.setState(ZombieState.SPAWNING);
        phaseTimer += deltaTime;
        if (phaseTimer >= INTRO_DURATION) {
            phase = FishPhase.IDLE;
            phaseTimer = 0f;
            holdStation(zombie);
        }
        return true;
    }

    private boolean tickCasting(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (phase != FishPhase.CASTING) {
            return false;
        }
        phaseTimer += deltaTime;
        if (!plantHooked && phaseTimer >= CAST_DURATION) {
            plantHooked = true;
            castHook(zombie, context);
        }
        if (phaseTimer >= CAST_DURATION + DELAY_BEFORE_REELING) {
            phase = FishPhase.REELING;
            phaseTimer = 0f;
        }
        return true;
    }

    private boolean tickReeling(float deltaTime) {
        if (phase != FishPhase.REELING) {
            return false;
        }
        phaseTimer += deltaTime;
        if (phaseTimer >= REEL_DURATION) {
            phase = FishPhase.IDLE;
            phaseTimer = 0f;
            castTimer = 0f;
        }
        return true;
    }

    private void beginCastIfReady(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        castTimer += deltaTime;
        if (castTimer < delayBetween(zombie) || findNearestPlantInLane(zombie, context) == null) {
            return;
        }
        castTimer = 0f;
        phase = FishPhase.CASTING;
        phaseTimer = 0f;
        plantHooked = false;
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.FISH;
    }

    private static void holdStation(ZombieInstance zombie) {
        if (zombie.getState() != ZombieState.SPECIAL_ACTION
                && zombie.getState() != ZombieState.DYING
                && zombie.getState() != ZombieState.DEAD) {
            zombie.setState(ZombieState.SPECIAL_ACTION);
        }
    }

    private static float delayBetween(ZombieInstance zombie) {
        if (zombie.getDefinition() == null) {
            return DELAY_BETWEEN_CASTING;
        }
        float delay = zombie.getDefinition().getBehaviorPropFloat(
                "DelayBetweenCasting", DELAY_BETWEEN_CASTING);
        return delay > 0f ? delay : DELAY_BETWEEN_CASTING;
    }

    /**
     * Hooks the plant in the Fisherman's row, if any, and reels it in.
     */
    private void castHook(ZombieInstance zombie, BehaviorContext context) {
        PlantInstance hookedPlant = findNearestPlantInLane(zombie, context);
        if (hookedPlant == null || hookedPlant.getCurrentHP() <= 0) {
            return;
        }
        reelIn(hookedPlant, context);
    }

    /**
     * Pulls the hooked plant one tile forward. If that tile is
     * occupied, the plant is thrown away and destroyed instead.
     */
    private void reelIn(PlantInstance hookedPlant, BehaviorContext context) {
        int currentCol = hookedPlant.getPosition().getX();
        int targetCol = currentCol + 1;
        int currentRow = hookedPlant.getPosition().getY();

        if (targetCol >= context.getColumnCount() || context.getPlantAt(currentRow, targetCol) != null) {
            context.destroyPlant(hookedPlant);
            return;
        }

        context.movePlant(hookedPlant, currentRow, targetCol);
    }

    /**
     * Finds the plant closest to the zombie's current position within its own lane.
     */
    private PlantInstance findNearestPlantInLane(ZombieInstance zombie, BehaviorContext context) {
        int row = zombie.getGridY();
        int zombieCol = zombie.getGridX();

        PlantInstance nearest = null;
        int nearestCol = -1;
        for (PlantInstance plant : context.getPlantsInLane(row)) {
            if (plant == null || plant.getCurrentHP() <= 0) {
                continue;
            }
            int col = plant.getPosition() != null ? plant.getPosition().getX() : -1;
            if (col < 0 || col >= zombieCol) {
                continue;
            }
            if (col > nearestCol) {
                nearestCol = col;
                nearest = plant;
            }
        }
        return nearest;
    }

    public FishPhase getPhase() {
        return phase;
    }

    public float getPhaseTimer() {
        return phaseTimer;
    }

    public float getCastTimer() {
        return castTimer;
    }

    public boolean isPlantHooked() {
        return plantHooked;
    }

    public void setCastTimer(float castTimer) {
        this.castTimer = castTimer;
    }

    public void setPhase(FishPhase phase) {
        this.phase = phase != null ? phase : FishPhase.IDLE;
    }

    public void setPhaseTimer(float phaseTimer) {
        this.phaseTimer = phaseTimer;
    }
}
