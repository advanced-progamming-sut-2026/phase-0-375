package model.zombie.behavior;

import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import model.enums.ZombieBehaviorType;

/**
 * Shoot behavior.
 */
public class ShootBehavior implements ZombieBehavior {

    // --- Explorer constants ---

    /** Torch kills plants in the same lane strictly closer than this many tiles ahead. */
    public static final float EXPLORER_TORCH_REACH = 1f;

    // --- Ice Age Hunter constants ---

    /** Seconds between snowball barrages. */
    public static final float HUNTER_BARRAGE_INTERVAL = 4.0f;

    /** Number of snowballs thrown per barrage. */
    public static final int HUNTER_SNOWBALLS_PER_BARRAGE = 3;

    /** Seconds between individual snowballs within a single barrage. */
    public static final float HUNTER_SNOWBALL_INTERVAL = 0.3f;

    /** Number of snowball hits required to freeze a plant solid. */
    public static final int HUNTER_HITS_TO_FREEZE = 3;

    // --- Beach Octopus constants ---

    /** Seconds between octopus throws. */
    public static final float OCTOPUS_THROW_INTERVAL = 4.0f;

    // --- State ---

    /** Whether the Explorer's torch is currently lit. Lit by default. */
    private boolean torchLit = true;

    /** Seconds elapsed since the last snowball barrage / octopus throw started. */
    private float castTimer = 0f;

    /** Snowballs remaining to be thrown in the current Ice Age Hunter barrage. */
    private int snowballsRemainingInBarrage = 0;

    /** Seconds elapsed since the last snowball was thrown within a barrage. */
    private float snowballTimer = 0f;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        if (isExplorer(zombie)) {
            tickExplorer(zombie, context);
        } else if (isIceAgeHunter(zombie)) {
            tickIceAgeHunter(zombie, context, deltaTime);
        } else if (isBeachOctopus(zombie)) {
            tickBeachOctopus(zombie, context, deltaTime);
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.SHOOT;
    }

    // --- Explorer ---

    /**
     * While lit, instantly destroys plants in this lane whose centre is closer
     * than {@value #EXPLORER_TORCH_REACH} tile ahead. Ice in that span
     * extinguishes the torch; fire relights it.
     */
    private void tickExplorer(ZombieInstance zombie, BehaviorContext context) {
        for (PlantInstance plant : context.getPlantsInLane(zombie.getGridY())) {
            float d = torchDistanceAhead(zombie, plant);
            if (d < 0f || d >= EXPLORER_TORCH_REACH || plant.getDefinition() == null) {
                continue;
            }
            if (plant.getDefinition().hasTag(model.enums.PlantTags.ICE)) {
                extinguishTorch();
            } else if (plant.getDefinition().hasTag(model.enums.PlantTags.FIRE)) {
                igniteTorch();
            }
        }

        if (!torchLit) {
            return;
        }

        for (PlantInstance plant : context.getPlantsInLane(zombie.getGridY())) {
            float d = torchDistanceAhead(zombie, plant);
            if (d > 0f && d < EXPLORER_TORCH_REACH) {
                context.destroyPlant(plant);
            }
        }
    }

    /**
     * Tiles the plant sits ahead of the Explorer (walks left). {@code 0} is the
     * same centre; negative is behind or dead.
     */
    private static float torchDistanceAhead(ZombieInstance zombie, PlantInstance plant) {
        if (plant == null || plant.getCurrentHP() <= 0 || plant.getPosition() == null) {
            return -1f;
        }
        return zombie.getContinuousX() - plant.getPosition().getX();
    }

    /**
     * Called by the game systems when an ice projectile or ice plant hits
     * this zombie, extinguishing its torch.
     */
    public void extinguishTorch() {
        torchLit = false;
    }

    /**
     * Called by the game systems when a fire projectile or fire plant hits
     * this zombie, relighting its torch.
     */
    public void igniteTorch() {
        torchLit = true;
    }

    public boolean isTorchLit() {
        return torchLit;
    }

    // --- Ice Age Hunter (snowballs) ---

    /**
     * Throws a barrage of {@value #HUNTER_SNOWBALLS_PER_BARRAGE}
     * snowballs at the nearest plant in its lane, spaced
     * {@value #HUNTER_SNOWBALL_INTERVAL} seconds apart.
     */
    private void tickIceAgeHunter(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (snowballsRemainingInBarrage > 0) {
            snowballTimer += deltaTime;
            if (snowballTimer >= HUNTER_SNOWBALL_INTERVAL) {
                snowballTimer -= HUNTER_SNOWBALL_INTERVAL;
                throwSnowball(zombie, context);
                snowballsRemainingInBarrage--;
            }
            return;
        }

        castTimer += deltaTime;
        if (castTimer >= HUNTER_BARRAGE_INTERVAL) {
            castTimer -= HUNTER_BARRAGE_INTERVAL;
            snowballsRemainingInBarrage = HUNTER_SNOWBALLS_PER_BARRAGE;
            snowballTimer = 0f;
        }
    }

    /**
     * Throws a single snowball at the nearest plant in the zombie's lane,
     * registering a freeze hit on it.
     */
    private void throwSnowball(ZombieInstance zombie, BehaviorContext context) {
        PlantInstance target = findNearestPlantInLane(zombie, context);
        if (target == null) {
            return;
        }
        target.registerFreezeHit(HUNTER_HITS_TO_FREEZE);
    }

    // --- Beach Octopus ---

    /**
     * Throws an octopus at the nearest plant in its lane,
     * instantly freezing it on contact.
     */
    private void tickBeachOctopus(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        castTimer += deltaTime;
        if (castTimer < OCTOPUS_THROW_INTERVAL) {
            return;
        }
        castTimer -= OCTOPUS_THROW_INTERVAL;

        PlantInstance target = findNearestPlantInLane(zombie, context);
        if (target == null) {
            return;
        }
        target.freeze();
    }

    // --- Shared targeting helpers ---

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
                continue; // only plants ahead of (to the left of) the zombie are valid targets
            }
            if (col > nearestCol) {
                nearestCol = col;
                nearest = plant;
            }
        }
        return nearest;
    }

    // --- Zombie identification helpers ---

    /** @return true if this zombie is an Explorer zombie. */
    public boolean isExplorer(ZombieInstance zombie) {
        String name = zombie.getDefinition().getName();
        if (name == null) return false;
        return name.toLowerCase().contains("explorer");
    }

    /** @return true if this zombie is an Ice Age Hunter zombie. */
    public boolean isIceAgeHunter(ZombieInstance zombie) {
        String name = zombie.getDefinition().getName();
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("iceagehunter") || lower.contains("hunter");
    }

    /** @return true if this zombie is a Beach Octopus zombie. */
    public boolean isBeachOctopus(ZombieInstance zombie) {
        String name = zombie.getDefinition().getName();
        if (name == null) return false;
        return name.toLowerCase().contains("octopus");
    }

    // --- Getters ---

    public float getCastTimer() {
        return castTimer;
    }

    public int getSnowballsRemainingInBarrage() {
        return snowballsRemainingInBarrage;
    }

    public float getSnowballTimer() {
        return snowballTimer;
    }

    // --- Setters ---

    public void setTorchLit(boolean torchLit) {
        this.torchLit = torchLit;
    }

    public void setCastTimer(float castTimer) {
        this.castTimer = castTimer;
    }

    public void setSnowballsRemainingInBarrage(int snowballsRemainingInBarrage) {
        this.snowballsRemainingInBarrage = snowballsRemainingInBarrage;
    }

    public void setSnowballTimer(float snowballTimer) {
        this.snowballTimer = snowballTimer;
    }
}
