package model.zombie.behavior.zomboss;

import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ancient Egypt sphinx Zomboss: missile (destroy plant + plant graves) and
 * charge (dash both occupied rows), plus shared summon / lane-change.
 */
public class EgyptZombossBehavior extends ZombossBehavior {

    public static final String DEFINITION_NAME = "ZombieEgyptZomboss";

    public static final float INTRO_SECONDS = 2.6f;
    public static final float MISSILE_START_SECONDS = 3.3333f;
    public static final float ROCKET_LAUNCH_SECONDS = 1.8333f;
    public static final float MISSILE_FALL_SECONDS = 2.4f;
    public static final float WALK_SECONDS = 1.2333f;
    public static final float PORTAL_START_SECONDS = 2.2667f;
    public static final float PORTAL_END_SECONDS = 1.5667f;
    public static final float CHARGE_REACH_X = 1.5f;
    public static final int SUMMON_COUNT = 3;
    public static final int GRAVES_PER_MISSILE = 2;

    private boolean summonDone;
    private boolean laneChanged;
    private boolean missileScheduled;
    private boolean chargeDestroyed;
    private boolean chargingForward = true;
    private float chargeParkX;
    private int laneFromRow = -1;
    private int laneToRow = -1;
    private final List<int[]> explosionCues = new ArrayList<>();

    @Override
    protected List<ZombossAction> buildActionPool() {
        return List.of(
                ZombossAction.MISSILE,
                ZombossAction.CHARGE,
                ZombossAction.SUMMON,
                ZombossAction.CHANGE_LANE
        );
    }

    @Override
    protected List<String> buildSummonPool() {
        return List.of(
                "ZombieDefault",
                "ZombieRa",
                "ZombieExplorer",
                "ZombieTombRaiser",
                "ZombieImp"
        );
    }

    @Override
    protected void beginAction(ZombieInstance zombie, BehaviorContext context, ZombossAction action) {
        summonDone = false;
        laneChanged = false;
        missileScheduled = false;
        chargeDestroyed = false;
        chargingForward = true;
        laneFromRow = -1;
        laneToRow = -1;
        if (action == ZombossAction.CHARGE && zombie != null) {
            chargeParkX = zombie.getContinuousPosition() != null
                    ? zombie.getContinuousX()
                    : zombie.getGridX() + 0.35f;
        }
        if (action == ZombossAction.CHANGE_LANE) {
            prepareLaneChange(zombie, context);
        }
    }

    @Override
    protected boolean tickAction(ZombieInstance zombie, BehaviorContext context,
                                 float deltaTime, ZombossAction action) {
        if (action == null) {
            return true;
        }
        return switch (action) {
            case MISSILE -> tickMissile(zombie, context);
            case CHARGE -> tickCharge(zombie, context, deltaTime);
            case SUMMON -> tickSummon(context);
            case CHANGE_LANE -> tickLaneChange(zombie, context);
            default -> true;
        };
    }

    @Override
    protected boolean shouldParkOnRight() {
        return getPhase() != ZombossPhase.ACTION || getCurrentAction() != ZombossAction.CHARGE;
    }

    @Override
    protected float introDurationSeconds() {
        return INTRO_SECONDS;
    }

    @Override
    protected float actionDurationSeconds(ZombossAction action) {
        if (action == null) {
            return super.actionDurationSeconds(null);
        }
        return switch (action) {
            case MISSILE -> MISSILE_START_SECONDS + ROCKET_LAUNCH_SECONDS + MISSILE_FALL_SECONDS + 0.2f;
            case CHARGE -> WALK_SECONDS * 2f;
            case CHANGE_LANE -> WALK_SECONDS;
            case SUMMON -> summonDurationSeconds();
            default -> super.actionDurationSeconds(action);
        };
    }

    public static float summonDurationSeconds() {
        return PORTAL_START_SECONDS + PORTAL_END_SECONDS;
    }

    @Override
    protected void onImpact(ZombieInstance zombie, BehaviorContext context,
                            ZombossPendingImpact impact) {
        if (context == null || impact == null) {
            return;
        }
        int row = impact.getRow();
        int col = impact.getCol();
        var plant = context.getPlantAt(row, col);
        if (plant != null && plant.getCurrentHP() > 0) {
            context.destroyPlant(plant);
        }
        plantRandomGraves(context, row, col);
        explosionCues.add(new int[]{row, col});
    }

    /** Drain tiles that should play a missile explosion FX this frame. */
    public List<int[]> drainExplosionCues() {
        if (explosionCues.isEmpty()) {
            return List.of();
        }
        List<int[]> drained = new ArrayList<>(explosionCues);
        explosionCues.clear();
        return drained;
    }

    /** {@code >0} walk_down, {@code <0} walk_up, {@code 0} unknown / idle. */
    public int laneChangeDelta() {
        if (laneFromRow < 0 || laneToRow < 0) {
            return 0;
        }
        return Integer.compare(laneToRow, laneFromRow);
    }

    public boolean isChargingForward() {
        return chargingForward;
    }

    public static float missileWindupSeconds() {
        return MISSILE_START_SECONDS + ROCKET_LAUNCH_SECONDS;
    }

    private boolean tickMissile(ZombieInstance zombie, BehaviorContext context) {
        float elapsed = phaseProgress01() * actionDurationSeconds(ZombossAction.MISSILE);
        if (!missileScheduled && elapsed >= missileWindupSeconds()) {
            missileScheduled = true;
            scheduleMissile(zombie, context);
        }
        if (!missileScheduled) {
            return false;
        }
        return getPendingImpacts().isEmpty();
    }

    private void scheduleMissile(ZombieInstance zombie, BehaviorContext context) {
        if (context == null) {
            return;
        }
        int rows = context.getRowCount();
        int cols = context.getColumnCount();
        if (rows <= 0 || cols <= 0) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int row = rng.nextInt(rows);
        int colMax = Math.max(1, cols - 2);
        int col = rng.nextInt(colMax);
        var plant = context.getPlantAt(row, col);
        if (plant == null || plant.getCurrentHP() <= 0) {
            List<int[]> occupied = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                for (var p : context.getPlantsInLane(r)) {
                    if (p != null && p.getCurrentHP() > 0 && p.getPosition() != null) {
                        occupied.add(new int[]{p.getPosition().getY(), p.getPosition().getX()});
                    }
                }
            }
            if (!occupied.isEmpty()) {
                int[] pick = occupied.get(rng.nextInt(occupied.size()));
                row = pick[0];
                col = pick[1];
            }
        }
        addPendingImpact(new ZombossPendingImpact(row, col, MISSILE_FALL_SECONDS));
    }

    private boolean tickCharge(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || zombie.getContinuousPosition() == null) {
            return true;
        }
        float total = WALK_SECONDS * 2f;
        float elapsed = phaseProgress01() * total;
        float parkX = chargeParkX;
        float reachX = CHARGE_REACH_X;

        if (elapsed < WALK_SECONDS) {
            chargingForward = true;
            float t = elapsed / WALK_SECONDS;
            zombie.setContinuousX(parkX + (reachX - parkX) * t);
        } else {
            chargingForward = false;
            if (!chargeDestroyed) {
                chargeDestroyed = true;
                destroyPlantsOnRows(zombie, context, zombie.getGridY(), zombie.getSecondaryRow());
            }
            float t = Math.min(1f, (elapsed - WALK_SECONDS) / WALK_SECONDS);
            zombie.setContinuousX(reachX + (parkX - reachX) * t);
        }
        int gridX = Math.max(0, (int) Math.floor(zombie.getContinuousX()));
        if (zombie.getGridX() != gridX) {
            zombie.setGridX(gridX);
        }
        return false;
    }

    private boolean tickSummon(BehaviorContext context) {
        if (summonDone) {
            return false;
        }
        if (phaseProgress01() * summonDurationSeconds() < PORTAL_START_SECONDS) {
            return false;
        }
        summonDone = true;
        summonMinions(context, SUMMON_COUNT);
        return false;
    }

    private void prepareLaneChange(ZombieInstance zombie, BehaviorContext context) {
        if (zombie == null || context == null) {
            return;
        }
        int rows = context.getRowCount();
        if (rows < 2) {
            return;
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
            return;
        }
        laneFromRow = current;
        laneToRow = options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }

    private boolean tickLaneChange(ZombieInstance zombie, BehaviorContext context) {
        if (laneFromRow < 0 || laneToRow < 0 || zombie == null) {
            return true;
        }
        float t = Math.max(0f, Math.min(1f, phaseProgress01()));
        float y = laneFromRow + (laneToRow - laneFromRow) * t;
        if (zombie.getContinuousPosition() != null) {
            zombie.getContinuousPosition().setY(y);
        }
        if (!laneChanged && (t >= 0.95f || getPhaseTimer() <= 1f / 30f)) {
            laneChanged = true;
            context.moveZombieToLane(zombie, laneToRow);
        }
        return false;
    }

    private void plantRandomGraves(BehaviorContext context, int avoidRow, int avoidCol) {
        if (context == null) {
            return;
        }
        int rows = context.getRowCount();
        int cols = context.getColumnCount();
        if (rows <= 0 || cols <= 0) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int planted = 0;
        int attempts = 0;
        while (planted < GRAVES_PER_MISSILE && attempts < 40) {
            attempts++;
            int row = rng.nextInt(rows);
            int col = rng.nextInt(cols);
            if (row == avoidRow && col == avoidCol) {
                continue;
            }
            if (context.spawnGraveAt(row, col)) {
                planted++;
            }
        }
    }
}
