package model.zombie.behavior.zomboss;

import model.enums.PlantTags;
import model.game.core.GameModel;
import model.game.systems.ChapterEffectsSystem;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Frostbite Caves mammoth Zomboss: ice missile, chill wind, freeze column.
 * No lane change and no summon.
 */
public class IceZombossBehavior extends ZombossBehavior {

    public static final String DEFINITION_NAME = "ZombieIceZomboss";
    /** Zombie sealed inside each cell of a freeze-column strike. */
    public static final String FROZEN_COLUMN_ZOMBIE = "ZombieDefault";

    public static final float INTRO_SECONDS = 4.7f;
    public static final float SLINGSHOT_SECONDS = 3.5f;
    public static final float MISSILE_FALL_SECONDS = 2.4f;
    public static final float WIND_SECONDS = 2.8333f;
    public static final float GLACIER_SECONDS = 6.3f;
    /** When during {@link ZombossAction#FREEZE_COLUMN} the ice blocks appear. */
    public static final float FREEZE_APPLY_SECONDS = 2.5f;
    /** How many lanes the mammoth wind chills. */
    public static final int ICE_WIND_ROW_COUNT = 2;
    public static final int WIND_PAM_INDEX_MIN = 1;
    public static final int WIND_PAM_INDEX_MAX = 4;
    public static final int GLACIER_PAM_INDEX_MIN = 1;
    public static final int GLACIER_PAM_INDEX_MAX = 6;
    /**
     * Columns the parked mammoth body spans left of {@code gridX}.
     * With park at column 7 this yields leftmost {@code c = 6}.
     */
    public static final int BODY_COLUMNS_LEFT_OF_PARK = 1;

    private boolean missileScheduled;
    private boolean windApplied;
    private boolean freezeApplied;
    private int windPamIndex = WIND_PAM_INDEX_MIN;
    private int glacierPamIndex = GLACIER_PAM_INDEX_MIN;
    private int freezeTargetCol = -1;
    private final List<Integer> windRows = new ArrayList<>();
    private final List<int[]> explosionCues = new ArrayList<>();

    @Override
    protected List<ZombossAction> buildActionPool() {
        return List.of(
                ZombossAction.ICE_MISSILE,
                ZombossAction.ICE_WIND,
                ZombossAction.FREEZE_COLUMN
        );
    }

    @Override
    protected List<String> buildSummonPool() {
        return List.of();
    }

    @Override
    protected boolean canChangeLane() {
        return false;
    }

    @Override
    protected boolean canSummon() {
        return false;
    }

    @Override
    protected void beginAction(ZombieInstance zombie, BehaviorContext context, ZombossAction action) {
        missileScheduled = false;
        windApplied = false;
        freezeApplied = false;
        windPamIndex = WIND_PAM_INDEX_MIN;
        glacierPamIndex = GLACIER_PAM_INDEX_MIN;
        freezeTargetCol = -1;
        windRows.clear();
        if (action == ZombossAction.ICE_WIND) {
            prepareIceWind(context);
        } else if (action == ZombossAction.FREEZE_COLUMN) {
            prepareFreezeColumn(zombie, context);
        }
    }

    @Override
    protected boolean tickAction(ZombieInstance zombie, BehaviorContext context,
                                 float deltaTime, ZombossAction action) {
        if (action == null) {
            return true;
        }
        return switch (action) {
            case ICE_MISSILE -> tickMissile(zombie, context);
            case ICE_WIND -> tickIceWind(context);
            case FREEZE_COLUMN -> tickFreezeColumn(zombie, context);
            default -> true;
        };
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
            case ICE_MISSILE -> SLINGSHOT_SECONDS + MISSILE_FALL_SECONDS + 0.2f;
            case ICE_WIND -> WIND_SECONDS;
            case FREEZE_COLUMN -> GLACIER_SECONDS;
            default -> super.actionDurationSeconds(action);
        };
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
        explosionCues.add(new int[]{row, col});
    }

    /** Drain tiles that should play an ice-missile explosion FX this frame. */
    public List<int[]> drainExplosionCues() {
        if (explosionCues.isEmpty()) {
            return List.of();
        }
        List<int[]> drained = new ArrayList<>(explosionCues);
        explosionCues.clear();
        return drained;
    }

    /** PAM index {@code i} for {@code wind_i}. */
    public int getWindPamIndex() {
        return windPamIndex;
    }

    /** PAM index {@code i} for {@code glacier_column_i}. */
    public int getGlacierPamIndex() {
        return glacierPamIndex;
    }

    /** 0-based lawn column receiving the freeze-column ice. */
    public int getFreezeTargetCol() {
        return freezeTargetCol;
    }

    /** Rows chilled by the current ice-wind attack. */
    public List<Integer> getWindRows() {
        return List.copyOf(windRows);
    }

    /** Leftmost column occupied by the mammoth body. */
    public static int leftmostColumn(ZombieInstance zombie) {
        if (zombie == null) {
            return 6;
        }
        return Math.max(0, zombie.getGridX() - BODY_COLUMNS_LEFT_OF_PARK);
    }

    public static int resolveGlacierPamIndex(int leftmostCol, int requestedI) {
        int c = Math.max(0, leftmostCol);
        int maxI = Math.min(GLACIER_PAM_INDEX_MAX, c);
        if (maxI < GLACIER_PAM_INDEX_MIN) {
            return GLACIER_PAM_INDEX_MIN;
        }
        int i = requestedI;
        if (i < GLACIER_PAM_INDEX_MIN || i > maxI || c - i < 0) {
            i = maxI;
        }
        return i;
    }

    private boolean tickMissile(ZombieInstance zombie, BehaviorContext context) {
        float elapsed = phaseProgress01() * actionDurationSeconds(ZombossAction.ICE_MISSILE);
        if (!missileScheduled && elapsed >= SLINGSHOT_SECONDS) {
            missileScheduled = true;
            scheduleMissile(context);
        }
        if (!missileScheduled) {
            return false;
        }
        return getPendingImpacts().isEmpty();
    }

    private void scheduleMissile(BehaviorContext context) {
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
        PlantInstance plant = context.getPlantAt(row, col);
        if (plant == null || plant.getCurrentHP() <= 0) {
            List<int[]> occupied = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                for (PlantInstance p : context.getPlantsInLane(r)) {
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

    private void prepareIceWind(BehaviorContext context) {
        windRows.clear();
        if (context == null) {
            windPamIndex = WIND_PAM_INDEX_MIN;
            return;
        }
        int rowCount = context.getRowCount();
        if (rowCount <= 0) {
            windPamIndex = WIND_PAM_INDEX_MIN;
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        windPamIndex = WIND_PAM_INDEX_MIN
                + rng.nextInt(WIND_PAM_INDEX_MAX - WIND_PAM_INDEX_MIN + 1);
        int primary = Math.min(rowCount - 1, windPamIndex);
        windRows.add(primary);
        if (rowCount > 1 && ICE_WIND_ROW_COUNT > 1) {
            int secondary = rng.nextInt(rowCount);
            int guard = 0;
            while (secondary == primary && guard++ < 8) {
                secondary = rng.nextInt(rowCount);
            }
            if (secondary != primary) {
                windRows.add(secondary);
            }
        }
    }

    private boolean tickIceWind(BehaviorContext context) {
        if (windApplied) {
            return false;
        }
        windApplied = true;
        applyIceWind(context);
        return false;
    }

    private void applyIceWind(BehaviorContext context) {
        if (context == null || windRows.isEmpty()) {
            return;
        }
        GameModel model = context instanceof GameModel g ? g : null;
        for (int row : windRows) {
            if (model != null) {
                model.queueIceWindGust(row);
            }
            for (PlantInstance plant : context.getPlantsInLane(row)) {
                if (plant == null || plant.getCurrentHP() <= 0) {
                    continue;
                }
                if (plant.getDefinition() != null
                        && plant.getDefinition().hasTag(PlantTags.FIRE)) {
                    continue;
                }
                plant.registerFreezeHit(ChapterEffectsSystem.FROST_LEVELS_TO_FREEZE);
            }
        }
    }

    private void prepareFreezeColumn(ZombieInstance zombie, BehaviorContext context) {
        int c = leftmostColumn(zombie);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int requested = GLACIER_PAM_INDEX_MIN
                + rng.nextInt(GLACIER_PAM_INDEX_MAX - GLACIER_PAM_INDEX_MIN + 1);
        glacierPamIndex = resolveGlacierPamIndex(c, requested);
        freezeTargetCol = c - glacierPamIndex + 1;
        if (context != null) {
            int cols = context.getColumnCount();
            if (cols > 0) {
                freezeTargetCol = Math.max(0, Math.min(cols - 1, freezeTargetCol));
            }
        }
    }

    private boolean tickFreezeColumn(ZombieInstance zombie, BehaviorContext context) {
        float elapsed = phaseProgress01() * GLACIER_SECONDS;
        if (!freezeApplied && elapsed >= FREEZE_APPLY_SECONDS) {
            freezeApplied = true;
            applyFreezeColumn(context);
        }
        return false;
    }

    private void applyFreezeColumn(BehaviorContext context) {
        if (context == null || freezeTargetCol < 0) {
            return;
        }
        int rows = context.getRowCount();
        for (int row = 0; row < rows; row++) {
            if (context instanceof GameModel model) {
                model.plantFrozenZombieAt(row, freezeTargetCol, FROZEN_COLUMN_ZOMBIE);
            }
        }
    }

}
