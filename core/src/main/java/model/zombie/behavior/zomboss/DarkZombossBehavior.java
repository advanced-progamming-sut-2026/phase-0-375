package model.zombie.behavior.zomboss;

import model.enums.GroundType;
import model.game.map.Cell;
import model.game.map.terrain.FireTerrainStrategy;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dark Ages dragon Zomboss: fireballs and row-wide burns, plus shared
 * summon / lane-change moves.
 */
public class DarkZombossBehavior extends ZombossBehavior {

    public static final String DEFINITION_NAME = "ZombieDarkZomboss";
    public static final String DRAGON_IMP = "ZombieDarkImpDragon";
    public static final int FIREBALL_COUNT_MIN = 3;
    public static final int FIREBALL_COUNT_MAX = 5;
    public static final float FIREBALL_TRAVEL_MIN = 0.7f;
    public static final float FIREBALL_TRAVEL_MAX = 1.4f;
    public static final int SUMMON_COUNT_MIN = 2;
    public static final int SUMMON_COUNT_MAX = 4;

    public static final float INTRO_SECONDS = 10.3333f;
    public static final float FIRE_ATTACK_START_SECONDS = 1.8f;
    public static final float FIRE_ATTACK_END_SECONDS = 0.8333f;

    private boolean burnApplied;
    private boolean summonDone;
    private boolean laneChanged;

    @Override
    protected List<ZombossAction> buildActionPool() {
        return List.of(
                ZombossAction.FIREBALLS,
                ZombossAction.BURN_ROWS,
                ZombossAction.SUMMON,
                ZombossAction.CHANGE_LANE
        );
    }

    @Override
    protected List<String> buildSummonPool() {
        return List.of(
                "ZombieDefault",
                "ZombieDarkJuggler",
                "ZombieWizard",
                "ZombieDarkArmor3",
                "ZombieDarkImpDragon"
        );
    }

    @Override
    protected void beginAction(ZombieInstance zombie, BehaviorContext context, ZombossAction action) {
        burnApplied = false;
        summonDone = false;
        laneChanged = false;
        if (action == ZombossAction.FIREBALLS) {
            scheduleFireballs(zombie, context);
        }
    }

    @Override
    protected boolean tickAction(ZombieInstance zombie, BehaviorContext context,
                                 float deltaTime, ZombossAction action) {
        if (action == null) {
            return true;
        }
        return switch (action) {
            case BURN_ROWS -> tickBurnRows(zombie, context);
            case SUMMON -> tickSummon(context);
            case CHANGE_LANE -> tickLaneChange(zombie, context);
            case FIREBALLS -> getPendingImpacts().isEmpty();
            default -> true;
        };
    }

    private boolean tickBurnRows(ZombieInstance zombie, BehaviorContext context) {
        float total = burnRowsDurationSeconds();
        float elapsed = phaseProgress01() * total;
        if (!burnApplied && elapsed >= FIRE_ATTACK_START_SECONDS) {
            burnApplied = true;
            int primary = zombie.getGridY();
            int secondary = zombie.getSecondaryRow();
            destroyPlantsOnRows(zombie, context, primary, secondary);
            igniteRow(context, primary);
            igniteRow(context, secondary);
        }
        return false;
    }

    @Override
    protected float introDurationSeconds() {
        return INTRO_SECONDS;
    }

    @Override
    protected float actionDurationSeconds(ZombossAction action) {
        if (action == ZombossAction.BURN_ROWS) {
            return burnRowsDurationSeconds();
        }
        return super.actionDurationSeconds(action);
    }

    public static float burnRowsDurationSeconds() {
        return FIRE_ATTACK_START_SECONDS
                + FireTerrainStrategy.DEFAULT_DURATION_SECONDS
                + FIRE_ATTACK_END_SECONDS;
    }

    private boolean tickSummon(BehaviorContext context) {
        if (summonDone) {
            return true;
        }
        if (phaseProgress01() < 0.45f) {
            return false;
        }
        summonDone = true;
        int count = ThreadLocalRandom.current().nextInt(SUMMON_COUNT_MIN, SUMMON_COUNT_MAX + 1);
        summonMinions(context, count);
        return false;
    }

    private boolean tickLaneChange(ZombieInstance zombie, BehaviorContext context) {
        if (laneChanged) {
            return true;
        }
        if (phaseProgress01() < 0.4f) {
            return false;
        }
        laneChanged = true;
        changeLane(zombie, context);
        return false;
    }

    private void scheduleFireballs(ZombieInstance zombie, BehaviorContext context) {
        int rows = context.getRowCount();
        int cols = context.getColumnCount();
        if (rows <= 0 || cols <= 0) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int count = rng.nextInt(FIREBALL_COUNT_MIN, FIREBALL_COUNT_MAX + 1);
        Set<Long> used = new HashSet<>();
        for (int i = 0; i < count; i++) {
            int row = rng.nextInt(rows);
            int col = rng.nextInt(Math.min(2, cols), Math.max(0, cols - 2));
            long key = (((long) row) << 32) | (col & 0xffffffffL);
            if (!used.add(key)) {
                continue;
            }
            float travel = FIREBALL_TRAVEL_MIN
                    + rng.nextFloat() * (FIREBALL_TRAVEL_MAX - FIREBALL_TRAVEL_MIN);
            addPendingImpact(new ZombossPendingImpact(row, col, travel));
        }
    }

    @Override
    protected void onImpact(ZombieInstance zombie, BehaviorContext context,
                            ZombossPendingImpact impact) {
        if (context == null || impact == null) {
            return;
        }
        int row = impact.getRow();
        int col = impact.getCol();
        PlantInstance plant = context.getPlantAt(row, col);
        if (plant != null && plant.getCurrentHP() > 0) {
            context.destroyPlant(plant);
        }
        igniteTile(context, row, col, FireTerrainStrategy.DEFAULT_DURATION_SECONDS);
        context.spawnZombieAt(DRAGON_IMP, row, col);
    }

    private void igniteRow(BehaviorContext context, int row) {
        if (context == null || row < 0 || row >= context.getRowCount()) {
            return;
        }
        for (int col = 0; col < context.getColumnCount(); col++) {
            igniteTile(context, row, col, FireTerrainStrategy.DEFAULT_DURATION_SECONDS);
        }
    }

    private void igniteTile(BehaviorContext context, int row, int col, float duration) {
        if (context instanceof model.game.core.GameModel model) {
            model.igniteTile(row, col, duration);
            return;
        }
        Cell cell = context.getCellAt(row, col);
        if (cell == null) {
            return;
        }
        cell.setGroundType(GroundType.FIRE);
        cell.setTerrainStrategy(new FireTerrainStrategy(duration));
    }
}
