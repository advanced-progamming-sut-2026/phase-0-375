package model.zombie.behavior.zomboss;

import model.enums.ZombieBehaviorType;
import model.game.core.GameModel;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Big Wave Beach shark Zomboss: baby sharks that eat water plants, a turbine
 * that sucks entities on its two rows toward its mouth, plus shared summon /
 * lane-change moves.
 */
public class BeachZombossBehavior extends ZombossBehavior {

    public static final String DEFINITION_NAME = "ZombieBeachZomboss";

    public static final float INTRO_SECONDS = 6.2f;
    public static final float SPAWN_SECONDS = 3f;
    public static final float SUBMERGE_SECONDS = 1.7f;
    public static final float EMERGE_SECONDS = 1.6f;
    public static final float SUCTION_ON_SECONDS = 2.1333f;
    public static final float SUCTION_LOOP_SECONDS = 2f;
    public static final float SUCTION_OFF_SECONDS = 2.5f;
    public static final float SHARK_WALK_SECONDS = 2f;
    public static final float SHARK_SUBMERGE_SECONDS = 2.7f;
    public static final float SHARK_ATTACK_SECONDS = 2.1333f;

    public static final int SUMMON_COUNT_MIN = 2;
    public static final int SUMMON_COUNT_MAX = 4;
    public static final float TURBINE_PULL_TILES_PER_SEC = 2.2f;
    public static final float PLANT_PULL_INTERVAL = 0.28f;

    private boolean summonDone;
    private boolean laneChanged;
    private boolean sharksScheduled;
    private int laneFromRow = -1;
    private int laneToRow = -1;
    private float plantPullTimer;
    private final List<BeachZombossPendingShark> pendingSharks = new ArrayList<>();

    @Override
    protected List<ZombossAction> buildActionPool() {
        return List.of(
                ZombossAction.BABY_SHARK,
                ZombossAction.TURBINE,
                ZombossAction.SUMMON,
                ZombossAction.CHANGE_LANE
        );
    }

    @Override
    protected List<String> buildSummonPool() {
        return List.of(
                "ZombieDefault",
                "ZombieBeachSnorkel",
                "ZombieBeachOctopus"
        );
    }

    @Override
    protected void beginAction(ZombieInstance zombie, BehaviorContext context, ZombossAction action) {
        summonDone = false;
        laneChanged = false;
        sharksScheduled = false;
        laneFromRow = -1;
        laneToRow = -1;
        plantPullTimer = 0f;
        pendingSharks.clear();
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
            case BABY_SHARK -> tickBabyShark(zombie, context);
            case TURBINE -> tickTurbine(zombie, context, deltaTime);
            case SUMMON -> tickSummon(context);
            case CHANGE_LANE -> tickLaneChange(zombie, context);
            default -> true;
        };
    }

    @Override
    protected void tickChapterProjectiles(ZombieInstance zombie, BehaviorContext context,
                                          float deltaTime) {
        tickPendingSharks(zombie, context, deltaTime);
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
            case BABY_SHARK -> babySharkDurationSeconds();
            case TURBINE -> turbineDurationSeconds();
            case CHANGE_LANE -> SUBMERGE_SECONDS + EMERGE_SECONDS;
            case SUMMON -> SPAWN_SECONDS;
            default -> super.actionDurationSeconds(action);
        };
    }

    public static float babySharkDurationSeconds() {
        return SPAWN_SECONDS + SHARK_WALK_SECONDS + SHARK_SUBMERGE_SECONDS + SHARK_ATTACK_SECONDS;
    }

    public static float turbineDurationSeconds() {
        return SUCTION_ON_SECONDS + SUCTION_LOOP_SECONDS + SUCTION_OFF_SECONDS;
    }

    public int laneToRow() {
        return laneToRow;
    }

    public List<BeachZombossPendingShark> getPendingSharks() {
        return Collections.unmodifiableList(pendingSharks);
    }

    public float turbineElapsedSeconds() {
        if (getCurrentAction() != ZombossAction.TURBINE) {
            return 0f;
        }
        return phaseProgress01() * turbineDurationSeconds();
    }

    private boolean tickBabyShark(ZombieInstance zombie, BehaviorContext context) {
        float elapsed = phaseProgress01() * babySharkDurationSeconds();
        if (!sharksScheduled && elapsed >= SPAWN_SECONDS) {
            sharksScheduled = true;
            scheduleBabySharks(zombie, context);
        }
        if (!sharksScheduled) {
            return false;
        }
        return pendingSharks.isEmpty();
    }

    private void scheduleBabySharks(ZombieInstance zombie, BehaviorContext context) {
        if (context == null) {
            return;
        }
        List<int[]> targets = waterPlantTiles(context);
        if (targets.isEmpty()) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int[] pick = targets.get(rng.nextInt(targets.size()));
        int row = pick[0];
        int col = pick[1];
        int bossCol = zombie != null ? zombie.getGridX() : Math.max(0, context.getColumnCount() - 2);
        float distance = Math.max(1f, bossCol - col);
        float walk = SHARK_WALK_SECONDS * (distance / Math.max(1, context.getColumnCount()));
        pendingSharks.add(new BeachZombossPendingShark(
                row, col, walk, SHARK_SUBMERGE_SECONDS, SHARK_ATTACK_SECONDS));
    }

    private List<int[]> waterPlantTiles(BehaviorContext context) {
        List<int[]> tiles = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        GameModel model = context instanceof GameModel g ? g : null;
        for (PlantInstance plant : context.getAllPlants()) {
            if (plant == null || plant.getCurrentHP() <= 0 || plant.getPosition() == null) {
                continue;
            }
            int row = plant.getPosition().getY();
            int col = plant.getPosition().getX();
            if (model != null && !model.isWaterTile(row, col)) {
                continue;
            }
            long key = (((long) row) << 32) | (col & 0xffffffffL);
            if (!seen.add(key)) {
                continue;
            }
            tiles.add(new int[]{row, col});
        }
        return tiles;
    }

    private void tickPendingSharks(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (pendingSharks.isEmpty()) {
            return;
        }
        List<BeachZombossPendingShark> snapshot = new ArrayList<>(pendingSharks);
        for (BeachZombossPendingShark shark : snapshot) {
            if (shark.tick(deltaTime)) {
                onSharkAttack(context, shark);
            }
        }
        pendingSharks.removeIf(BeachZombossPendingShark::isResolved);
    }

    private void onSharkAttack(BehaviorContext context, BeachZombossPendingShark shark) {
        if (context == null || shark == null) {
            return;
        }
        PlantInstance plant = resolveSharkTarget(context, shark.getRow(), shark.getCol());
        if (plant != null && plant.getCurrentHP() > 0) {
            context.destroyPlant(plant);
        }
    }

    private PlantInstance resolveSharkTarget(BehaviorContext context, int row, int col) {
        PlantInstance top = context.getPlantAt(row, col);
        if (top != null && top.getCurrentHP() > 0) {
            return top;
        }
        if (!(context instanceof GameModel model)) {
            return null;
        }
        for (PlantInstance plant : model.getAllPlantsAt(row, col)) {
            if (plant != null && plant.getCurrentHP() > 0) {
                return plant;
            }
        }
        return null;
    }

    private boolean tickTurbine(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        float elapsed = turbineElapsedSeconds();
        if (elapsed >= SUCTION_ON_SECONDS
                && elapsed < SUCTION_ON_SECONDS + SUCTION_LOOP_SECONDS) {
            applyTurbinePull(zombie, context, deltaTime);
        }
        return false;
    }

    private void applyTurbinePull(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null) {
            return;
        }
        int primary = zombie.getGridY();
        int secondary = zombie.getSecondaryRow();
        float mouthX = zombie.getContinuousPosition() != null
                ? zombie.getContinuousX()
                : zombie.getGridX() + 0.35f;
        float pull = TURBINE_PULL_TILES_PER_SEC * deltaTime;

        pullZombiesOnRow(zombie, context, primary, mouthX, pull);
        if (secondary != primary) {
            pullZombiesOnRow(zombie, context, secondary, mouthX, pull);
        }

        plantPullTimer += deltaTime;
        while (plantPullTimer >= PLANT_PULL_INTERVAL) {
            plantPullTimer -= PLANT_PULL_INTERVAL;
            pullPlantsOnRow(context, primary, mouthX);
            if (secondary != primary) {
                pullPlantsOnRow(context, secondary, mouthX);
            }
        }
    }

    private void pullZombiesOnRow(ZombieInstance boss, BehaviorContext context, int row,
                                  float mouthX, float pull) {
        GameModel model = context instanceof GameModel g ? g : null;
        for (ZombieInstance zombie : new ArrayList<>(context.getZombiesInLane(row))) {
            if (zombie == null || zombie.isDead() || zombie == boss
                    || zombie.hasBehavior(ZombieBehaviorType.ZOMBOSS)) {
                continue;
            }
            if (zombie.getContinuousPosition() == null) {
                continue;
            }
            float x = zombie.getContinuousX();
            if (x >= mouthX - 0.15f) {
                consumeZombie(model, zombie);
                continue;
            }
            float nextX = Math.min(mouthX - 0.15f, x + pull);
            zombie.setContinuousX(nextX);
            int gridX = Math.max(0, (int) Math.floor(nextX));
            if (gridX != zombie.getGridX()) {
                zombie.setGridX(gridX);
            }
        }
    }

    private void pullPlantsOnRow(BehaviorContext context, int row, float mouthX) {
        int mouthCol = Math.max(0, (int) Math.floor(mouthX));
        for (PlantInstance plant : new ArrayList<>(context.getPlantsInLane(row))) {
            if (plant == null || plant.getCurrentHP() <= 0 || plant.getPosition() == null) {
                continue;
            }
            int col = plant.getPosition().getX();
            if (col >= mouthCol) {
                context.destroyPlant(plant);
                continue;
            }
            if (col + 1 >= mouthCol) {
                context.destroyPlant(plant);
                continue;
            }
            if (context.getPlantAt(row, col + 1) == null) {
                context.movePlant(plant, row, col + 1);
            }
        }
    }

    private void consumeZombie(GameModel model, ZombieInstance zombie) {
        if (model != null) {
            model.removeZombie(zombie);
            return;
        }
        zombie.setCurrentHP(0);
    }

    private boolean tickSummon(BehaviorContext context) {
        if (summonDone) {
            return false;
        }
        if (phaseProgress01() * SPAWN_SECONDS < SPAWN_SECONDS * 0.45f) {
            return false;
        }
        summonDone = true;
        int count = ThreadLocalRandom.current().nextInt(SUMMON_COUNT_MIN, SUMMON_COUNT_MAX + 1);
        summonMinions(context, count);
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
        float elapsed = phaseProgress01() * actionDurationSeconds(ZombossAction.CHANGE_LANE);
        if (!laneChanged && elapsed >= SUBMERGE_SECONDS) {
            laneChanged = true;
            context.moveZombieToLane(zombie, laneToRow);
            if (zombie.getContinuousPosition() != null) {
                zombie.getContinuousPosition().setY(laneToRow);
            }
        }
        return false;
    }
}
