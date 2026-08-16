package model.zombie.behavior;

import model.enums.PlantTags;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.game.map.Point;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    /** Tiles ahead a snowball can travel ({@code FarAttackRange} in zombies.json). */
    public static final float HUNTER_RANGE = 4f;

    /**
     * Winter-mint / named immunes that are not {@link PlantTags#FIRE}.
     * Fire plants use the tag; this set is the rest of the wiki list.
     */
    private static final Set<String> HUNTER_FREEZE_IMMUNE_NAMES = Set.of(
            "snapdragon", "cold snapdragon", "winter melon", "missile toe", "iceweed",
            "lava guava", "jack o' lantern", "jack o.lantern");

    // --- Beach Octopus constants ---

    /** Seconds between octopus toss starts. */
    public static final float OCTOPUS_THROW_INTERVAL = 4.0f;

    /** {@code toss} clip length on {@code ZOMBIE_BEACH_OCTOPUS}. */
    public static final float OCTOPUS_TOSS_DURATION = 3.0667f;

    /** Seconds into {@code toss} when the held octopus leaves the hand. */
    public static final float OCTOPUS_RELEASE_AT = 1.37f;

    /** Seconds the thrown octopus spends in the air. Same parabola as Imp. */
    public static final float OCTOPUS_FLIGHT_DURATION = 0.85f;

    /** Peak height of the throw arc, in tiles. */
    public static final float OCTOPUS_FLIGHT_APEX_TILES = 1.25f;

    // --- State ---

    /** Whether the Explorer's torch is currently lit. Lit by default. */
    private boolean torchLit = true;

    /** Seconds elapsed since the last snowball barrage / octopus throw started. */
    private float castTimer = 0f;

    /** Snowballs remaining to be thrown in the current Ice Age Hunter barrage. */
    private int snowballsRemainingInBarrage = 0;

    /** Seconds elapsed since the last snowball was thrown within a barrage. */
    private float snowballTimer = 0f;

    /** Keep {@code throw} playing after the last ball so the clip is not cut at impact. */
    private float throwHold = 0f;

    /** Grid cell of the plant last hit by a snowball; renderer plays the splat here. */
    private Point lastSnowballSplatAt;

    /** Increments on each snowball that hits a plant. */
    private int snowballSplatSeq = 0;

    /** True while the Beach Octopus {@code toss} clip should play. */
    private boolean octopusThrowing;

    /** Elapsed seconds of the current {@code toss}. */
    private float octopusTossTimer;

    /** True after {@link #OCTOPUS_RELEASE_AT} until the toss clip ends. */
    private boolean octopusReleased;

    private final List<OctopusShot> octopusShots = new ArrayList<>();

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null) {
            return;
        }
        if (zombie.isDead()) {
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
        if (throwHold > 0f) {
            throwHold -= deltaTime;
            if (throwHold <= 0f && snowballsRemainingInBarrage == 0) {
                clearHunterThrow(zombie);
            }
        }
        if (snowballsRemainingInBarrage > 0) {
            snowballTimer += deltaTime;
            if (snowballTimer >= HUNTER_SNOWBALL_INTERVAL) {
                snowballTimer -= HUNTER_SNOWBALL_INTERVAL;
                throwSnowball(zombie, context);
                snowballsRemainingInBarrage--;
                if (snowballsRemainingInBarrage == 0) {
                    throwHold = HUNTER_SNOWBALL_INTERVAL;
                }
            }
            return;
        }

        castTimer += deltaTime;
        if (castTimer < HUNTER_BARRAGE_INTERVAL) {
            return;
        }
        if (findHunterTarget(zombie, context) == null) {
            castTimer = HUNTER_BARRAGE_INTERVAL;
            return;
        }
        castTimer -= HUNTER_BARRAGE_INTERVAL;
        snowballsRemainingInBarrage = HUNTER_SNOWBALLS_PER_BARRAGE;
        snowballTimer = 0f;
        throwHold = 0f;
        beginHunterThrow(zombie);
    }

    private static void beginHunterThrow(ZombieInstance zombie) {
        if (zombie.getState() != ZombieState.DYING && zombie.getState() != ZombieState.DEAD) {
            zombie.setState(ZombieState.SPECIAL_ACTION);
        }
    }

    private static void clearHunterThrow(ZombieInstance zombie) {
        if (zombie.getState() == ZombieState.SPECIAL_ACTION) {
            zombie.setState(ZombieState.WALKING);
        }
    }

    /**
     * Throws a single snowball at the nearest plant in the zombie's lane,
     * registering a freeze hit on it.
     */
    private void throwSnowball(ZombieInstance zombie, BehaviorContext context) {
        PlantInstance target = findHunterTarget(zombie, context);
        if (target == null) {
            return;
        }
        if (!isHunterFreezeImmune(target)) {
            target.registerFreezeHit(HUNTER_HITS_TO_FREEZE);
        }
        lastSnowballSplatAt = target.getPosition();
        snowballSplatSeq++;
    }

    // --- Beach Octopus ---

    /**
     * Plays {@code toss}, releases a flying octopus at {@link #OCTOPUS_RELEASE_AT},
     * and freezes the target when it lands.
     */
    private void tickBeachOctopus(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        tickOctopusShots(deltaTime);
        if (octopusThrowing) {
            octopusTossTimer += deltaTime;
            if (!octopusReleased && octopusTossTimer >= OCTOPUS_RELEASE_AT) {
                releaseOctopus(zombie, context);
            }
            if (octopusTossTimer >= OCTOPUS_TOSS_DURATION) {
                octopusThrowing = false;
                octopusReleased = false;
                octopusTossTimer = 0f;
                clearHunterThrow(zombie);
            }
            return;
        }
        castTimer += deltaTime;
        if (castTimer < OCTOPUS_THROW_INTERVAL) {
            return;
        }
        PlantInstance target = findNearestPlantInLane(zombie, context);
        if (target == null) {
            castTimer = OCTOPUS_THROW_INTERVAL;
            return;
        }
        castTimer = 0f;
        octopusThrowing = true;
        octopusTossTimer = 0f;
        octopusReleased = false;
        beginHunterThrow(zombie);
    }

    private void releaseOctopus(ZombieInstance zombie, BehaviorContext context) {
        octopusReleased = true;
        PlantInstance target = findNearestPlantInLane(zombie, context);
        if (target == null || target.getPosition() == null) {
            return;
        }
        octopusShots.add(new OctopusShot(zombie, target, zombie.getContinuousX(), zombie.getGridY()));
    }

    private void tickOctopusShots(float deltaTime) {
        for (int i = octopusShots.size() - 1; i >= 0; i--) {
            OctopusShot shot = octopusShots.get(i);
            shot.timer += deltaTime;
            if (shot.timer >= OCTOPUS_FLIGHT_DURATION) {
                shot.land();
                octopusShots.remove(i);
            }
        }
    }

    @Override
    public void onZombieDeath(ZombieInstance zombie, BehaviorContext context) {
        if (!isBeachOctopus(zombie)) {
            return;
        }
        for (OctopusShot shot : octopusShots) {
            shot.land();
        }
        octopusShots.clear();
        octopusThrowing = false;
        octopusReleased = false;
    }

    // --- Shared targeting helpers ---

    /**
     * Nearest plant ahead within {@link #HUNTER_RANGE}. Skips low {@link PlantTags#TRAP}
     * plants (Spikeweed). Frozen plants stay eligible so they block the lane.
     */
    private PlantInstance findHunterTarget(ZombieInstance zombie, BehaviorContext context) {
        PlantInstance nearest = null;
        float nearestDist = Float.MAX_VALUE;
        for (PlantInstance plant : context.getPlantsInLane(zombie.getGridY())) {
            if (isHunterLowPlant(plant)) {
                continue;
            }
            float d = hunterDistanceAhead(zombie, plant);
            if (d <= 0f || d > HUNTER_RANGE) {
                continue;
            }
            if (d < nearestDist) {
                nearestDist = d;
                nearest = plant;
            }
        }
        return nearest;
    }

    private static float hunterDistanceAhead(ZombieInstance zombie, PlantInstance plant) {
        if (plant == null || plant.getCurrentHP() <= 0 || plant.getPosition() == null) {
            return -1f;
        }
        return zombie.getContinuousX() - plant.getPosition().getX();
    }

    private static boolean isHunterLowPlant(PlantInstance plant) {
        Plant def = plant == null ? null : plant.getDefinition();
        return def != null && def.hasTag(PlantTags.TRAP);
    }

    private static boolean isHunterFreezeImmune(PlantInstance plant) {
        Plant def = plant == null ? null : plant.getDefinition();
        if (def == null) {
            return false;
        }
        if (def.hasTag(PlantTags.FIRE)) {
            return true;
        }
        String name = def.getName();
        return name != null && HUNTER_FREEZE_IMMUNE_NAMES.contains(name.toLowerCase(Locale.ROOT));
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
            if (plant == null || plant.getCurrentHP() <= 0 || plant.isFrozen()) {
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

    /** True while the Hunter's {@code throw} clip should play. */
    public boolean isThrowing() {
        return snowballsRemainingInBarrage > 0 || throwHold > 0f;
    }

    public boolean isOctopusThrowing() {
        return octopusThrowing;
    }

    public float getOctopusTossTimer() {
        return octopusTossTimer;
    }

    public boolean hasReleasedOctopus() {
        return octopusReleased;
    }

    public List<OctopusShot> getOctopusShots() {
        return octopusShots;
    }

    public Point getLastSnowballSplatAt() {
        return lastSnowballSplatAt;
    }

    public int getSnowballSplatSeq() {
        return snowballSplatSeq;
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

    /** In-flight octopus: parabola from the thrower to {@code target}. */
    public static final class OctopusShot {
        private final ZombieInstance thrower;
        private final PlantInstance target;
        private final float startX;
        private final int row;
        private float timer;

        OctopusShot(ZombieInstance thrower, PlantInstance target, float startX, int row) {
            this.thrower = thrower;
            this.target = target;
            this.startX = startX;
            this.row = row;
        }

        void land() {
            if (target == null || target.getCurrentHP() <= 0) {
                return;
            }
            target.freezeFromOctopus();
        }

        public ZombieInstance thrower() {
            return thrower;
        }

        public PlantInstance target() {
            return target;
        }

        public float startX() {
            return startX;
        }

        public int row() {
            return row;
        }

        public float timer() {
            return timer;
        }

        public boolean isFlying() {
            return timer < OCTOPUS_FLIGHT_DURATION;
        }

        /** 0 at release, 1 on landing. */
        public float progress() {
            return Math.min(1f, timer / OCTOPUS_FLIGHT_DURATION);
        }

        /** Parabola in tiles; 0 on the ground. */
        public float heightTiles() {
            float t = progress();
            return 4f * OCTOPUS_FLIGHT_APEX_TILES * t * (1f - t);
        }

        public Point targetCell() {
            return target == null ? null : target.getPosition();
        }
    }
}
