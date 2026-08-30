package model.zombie.behavior.zomboss;

import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.behavior.ZombieBehavior;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared Zomboss fight loop for every chapter boss.
 */
public abstract class ZombossBehavior implements ZombieBehavior {

    public static final int PHASE_COUNT = 3;
    public static final float DEFAULT_INTRO_SECONDS = 3.5f;
    public static final float DEFAULT_STUN_SECONDS = 8f;
    public static final float DEFAULT_ACTION_INTERVAL_MIN = 5f;
    public static final float DEFAULT_ACTION_INTERVAL_MAX = 9f;
    public static final float DEFAULT_LANE_CHANGE_SECONDS = 1.2f;
    public static final float DEFAULT_SUMMON_SECONDS = 2.2f;
    public static final int PARK_COLUMNS_FROM_RIGHT = 1;

    private ZombossPhase phase = ZombossPhase.INTRO;
    private ZombossAction currentAction;
    private float phaseTimer;
    private float actionCooldown;
    private int segmentsRemaining = PHASE_COUNT;
    private int hpAtSegmentStart;
    private int maxHp = 1;
    private boolean segmentThresholdArmed = true;
    private final List<ZombossPendingImpact> pendingImpacts = new ArrayList<>();
    private final List<String> summonPool = new ArrayList<>();
    private boolean actionPoolBuilt;
    private List<ZombossAction> actionPool = List.of();

    @Override
    public final void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }
        ensureSetup(zombie);
        parkOnRight(zombie, context);
        tickPendingImpacts(zombie, context, deltaTime);

        if (zombie.getCurrentHP() <= 0) {
            enterDying(zombie);
            return;
        }

        checkSegmentBreak(zombie);

        switch (phase) {
            case INTRO -> tickIntro(zombie, deltaTime);
            case IDLE -> tickIdle(zombie, context, deltaTime);
            case ACTION -> tickActionPhase(zombie, context, deltaTime);
            case STUNNED -> tickStun(zombie, deltaTime);
            case DYING -> {
                // Death clip / removal handled by ZombieSystem once HP hits 0.
            }
        }
    }

    private void ensureSetup(ZombieInstance zombie) {
        if (!actionPoolBuilt) {
            actionPool = List.copyOf(buildActionPool());
            actionPoolBuilt = true;
            List<String> pool = buildSummonPool();
            summonPool.clear();
            if (pool != null) {
                summonPool.addAll(pool);
            }
            maxHp = Math.max(1, zombie.getCurrentHP());
            hpAtSegmentStart = maxHp;
            segmentsRemaining = PHASE_COUNT;
            segmentThresholdArmed = true;
            phase = ZombossPhase.INTRO;
            phaseTimer = introDurationSeconds();
            actionCooldown = 1.5f;
            zombie.setState(ZombieState.SPECIAL_ACTION);
        }
    }

    /** Keeps the boss on the rightmost park column. */
    private void parkOnRight(ZombieInstance zombie, BehaviorContext context) {
        int cols = context.getColumnCount();
        if (cols <= 0) {
            return;
        }
        int parkCol = Math.max(0, cols - 1 - PARK_COLUMNS_FROM_RIGHT);
        float parkX = parkCol + 0.35f;
        if (zombie.getContinuousPosition() == null) {
            return;
        }
        if (Math.abs(zombie.getContinuousX() - parkX) > 0.05f
                || zombie.getGridX() != parkCol) {
            zombie.setContinuousX(parkX);
            zombie.setGridX(parkCol);
        }
    }

    private void checkSegmentBreak(ZombieInstance zombie) {
        if (phase == ZombossPhase.STUNNED || phase == ZombossPhase.DYING
                || phase == ZombossPhase.INTRO || !segmentThresholdArmed) {
            return;
        }
        if (segmentsRemaining <= 1) {
            return;
        }
        int segmentHp = Math.max(1, hpAtSegmentStart / PHASE_COUNT);
        int hpLostInSegment = hpAtSegmentStart - zombie.getCurrentHP();
        int segmentsCleared = hpLostInSegment / segmentHp;
        int expectedRemaining = PHASE_COUNT - segmentsCleared;
        if (expectedRemaining < segmentsRemaining && expectedRemaining >= 1) {
            segmentsRemaining = expectedRemaining;
            enterStun(zombie);
        }
    }

    private void tickIntro(ZombieInstance zombie, float deltaTime) {
        zombie.setState(ZombieState.SPECIAL_ACTION);
        phaseTimer -= deltaTime;
        if (phaseTimer <= 0f) {
            enterIdle(zombie);
        }
    }

    private void tickIdle(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        zombie.setState(ZombieState.SPECIAL_ACTION);
        actionCooldown -= deltaTime;
        if (actionCooldown > 0f || actionPool.isEmpty()) {
            return;
        }
        ZombossAction next = pickAction(context);
        if (next == null) {
            actionCooldown = actionIntervalSeconds();
            return;
        }
        beginActionInternal(zombie, context, next);
    }

    private void tickActionPhase(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        zombie.setState(ZombieState.SPECIAL_ACTION);
        boolean done = tickAction(zombie, context, deltaTime, currentAction);
        phaseTimer -= deltaTime;
        if (done || phaseTimer <= 0f) {
            finishAction(zombie);
        }
    }

    private void tickStun(ZombieInstance zombie, float deltaTime) {
        zombie.setState(ZombieState.STUNNED);
        phaseTimer -= deltaTime;
        if (phaseTimer <= 0f) {
            segmentThresholdArmed = true;
            enterIdle(zombie);
        }
    }

    private void beginActionInternal(ZombieInstance zombie, BehaviorContext context,
                                     ZombossAction action) {
        currentAction = action;
        phase = ZombossPhase.ACTION;
        phaseTimer = actionDurationSeconds(action);
        beginAction(zombie, context, action);
    }

    private void finishAction(ZombieInstance zombie) {
        currentAction = null;
        enterIdle(zombie);
    }

    private void enterIdle(ZombieInstance zombie) {
        phase = ZombossPhase.IDLE;
        actionCooldown = actionIntervalSeconds();
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    private void enterStun(ZombieInstance zombie) {
        currentAction = null;
        pendingImpacts.clear();
        phase = ZombossPhase.STUNNED;
        phaseTimer = stunDurationSeconds();
        segmentThresholdArmed = false;
        zombie.setState(ZombieState.STUNNED);
        onEnterStun(zombie);
    }

    private void enterDying(ZombieInstance zombie) {
        phase = ZombossPhase.DYING;
        currentAction = null;
        pendingImpacts.clear();
        zombie.setState(ZombieState.DYING);
    }

    private ZombossAction pickAction(BehaviorContext context) {
        List<ZombossAction> candidates = new ArrayList<>(actionPool.size());
        for (ZombossAction action : actionPool) {
            if (canPerform(action, context)) {
                candidates.add(action);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private boolean canPerform(ZombossAction action, BehaviorContext context) {
        return switch (action) {
            case CHANGE_LANE -> canChangeLane() && context.getRowCount() > 2;
            case SUMMON -> canSummon() && !summonPool.isEmpty();
            default -> true;
        };
    }

    // --- Shared action helpers subclasses / base use ---

    /** Moves the boss's primary row to a random valid two-row slot. */
    protected final boolean changeLane(ZombieInstance zombie, BehaviorContext context) {
        if (!canChangeLane()) {
            return false;
        }
        int rows = context.getRowCount();
        if (rows < 2) {
            return false;
        }
        int maxPrimary = rows - 2;
        int current = zombie.getGridY();
        List<Integer> options = new ArrayList<>();
        for (int r = 0; r <= maxPrimary; r++) {
            if (r != current) {
                options.add(r);
            }
        }
        if (options.isEmpty()) {
            return false;
        }
        int target = options.get(ThreadLocalRandom.current().nextInt(options.size()));
        return context.moveZombieToLane(zombie, target);
    }

    /** Spawns {@code count} zombies from the summon pool along the right edge. */
    protected final void summonMinions(BehaviorContext context, int count) {
        if (!canSummon() || summonPool.isEmpty() || context == null || count <= 0) {
            return;
        }
        int rows = context.getRowCount();
        int cols = context.getColumnCount();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            String name = summonPool.get(rng.nextInt(summonPool.size()));
            int row = rng.nextInt(Math.max(1, rows));
            int col = rng.nextInt(Math.min(2, cols), Math.max(0, cols));
            context.spawnZombieAt(name, row, col);
        }
    }

    protected final void destroyPlantsOnRows(ZombieInstance zombie, BehaviorContext context,
                                            int... rows) {
        if (context == null || rows == null) {
            return;
        }
        for (int row : rows) {
            List<PlantInstance> plants = new ArrayList<>(context.getPlantsInLane(row));
            for (PlantInstance plant : plants) {
                if (plant != null && plant.getCurrentHP() > 0) {
                    context.destroyPlant(plant);
                }
            }
        }
    }

    protected final void addPendingImpact(ZombossPendingImpact impact) {
        if (impact != null) {
            pendingImpacts.add(impact);
        }
    }

    private void tickPendingImpacts(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (pendingImpacts.isEmpty()) {
            return;
        }
        List<ZombossPendingImpact> snapshot = new ArrayList<>(pendingImpacts);
        for (ZombossPendingImpact impact : snapshot) {
            if (impact.tick(deltaTime)) {
                onImpact(zombie, context, impact);
            }
        }
        pendingImpacts.removeIf(ZombossPendingImpact::isResolved);
    }

    /** Called when a pending lobbed attack lands. */
    protected void onImpact(ZombieInstance zombie, BehaviorContext context,
                            ZombossPendingImpact impact) {
        // subclass
    }

    protected void onEnterStun(ZombieInstance zombie) {
        // subclass hook
    }

    // --- Subclass contract ---

    /** Actions this boss may randomly pick while idle. */
    protected abstract List<ZombossAction> buildActionPool();

    /** Zombie definition names summoned by {@link ZombossAction#SUMMON}. */
    protected abstract List<String> buildSummonPool();

    /** @return false for bosses that never leave their starting rows. */
    protected boolean canChangeLane() {
        return true;
    }

    /** @return false for bosses that never summon. */
    protected boolean canSummon() {
        return true;
    }

    /** Start a newly selected action (play windup, spawn projectiles, etc.). */
    protected abstract void beginAction(ZombieInstance zombie, BehaviorContext context,
                                        ZombossAction action);

    /**
     * Advance the current action.
     *
     * @return true when the action has fully finished early
     */
    protected abstract boolean tickAction(ZombieInstance zombie, BehaviorContext context,
                                          float deltaTime, ZombossAction action);

    protected float introDurationSeconds() {
        return DEFAULT_INTRO_SECONDS;
    }

    protected float stunDurationSeconds() {
        return DEFAULT_STUN_SECONDS;
    }

    protected float actionIntervalSeconds() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        return DEFAULT_ACTION_INTERVAL_MIN
                + rng.nextFloat() * (DEFAULT_ACTION_INTERVAL_MAX - DEFAULT_ACTION_INTERVAL_MIN);
    }

    protected float actionDurationSeconds(ZombossAction action) {
        if (action == null) {
            return 2f;
        }
        return switch (action) {
            case CHANGE_LANE -> DEFAULT_LANE_CHANGE_SECONDS;
            case SUMMON -> DEFAULT_SUMMON_SECONDS;
            case FIREBALLS -> 2.8f;
            case BURN_ROWS -> 3.2f;
            case MISSILE, ICE_MISSILE -> 2.5f;
            case CHARGE -> 2.0f;
            case ICE_WIND -> 2.5f;
            case FREEZE_COLUMN -> 3.0f;
            case BABY_SHARK -> 2.5f;
            case TURBINE -> 3.5f;
        };
    }

    // --- Getters ---

    public ZombossPhase getPhase() {
        return phase;
    }

    public ZombossAction getCurrentAction() {
        return currentAction;
    }

    public int getSegmentsRemaining() {
        return segmentsRemaining;
    }

    public int getPhaseCount() {
        return PHASE_COUNT;
    }

    /**
     * Boss HP bar fill in {@code [0, 1]} across all three segments
     * (1 = full health).
     */
    public float healthProgress01(ZombieInstance zombie) {
        if (zombie == null) {
            return 0f;
        }
        int max = Math.max(1, maxHp);
        if (!actionPoolBuilt && zombie.getDefinition() != null) {
            // Before first tick: fall back to current/definition so the HUD is
            // full if spawn HP already matches the boosted value.
            max = Math.max(1, zombie.getCurrentHP());
        }
        return Math.max(0f, Math.min(1f, zombie.getCurrentHP() / (float) max));
    }

    /** How far the current stun/intro/action has progressed (0→1). */
    public float phaseProgress01() {
        float total = currentPhaseDurationSeconds();
        if (total <= 0f) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, 1f - phaseTimer / total));
    }

    /** Full length of the active intro / stun / action window in seconds. */
    public float currentPhaseDurationSeconds() {
        return switch (phase) {
            case INTRO -> introDurationSeconds();
            case STUNNED -> stunDurationSeconds();
            case ACTION -> currentAction == null ? 1f : actionDurationSeconds(currentAction);
            default -> 1f;
        };
    }

    /** Remaining countdown for the current phase (intro/stun/action). */
    public float getPhaseTimer() {
        return phaseTimer;
    }

    public List<ZombossPendingImpact> getPendingImpacts() {
        return Collections.unmodifiableList(pendingImpacts);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.ZOMBOSS;
    }
}
