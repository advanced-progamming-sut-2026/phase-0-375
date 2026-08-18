package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dark Ages Wizard: every {@link #TRANSFORM_INTERVAL} seconds (or on contact)
 * plays {@code sheep} and turns a plant into a sheep until this Wizard dies.
 */
public class TransformBehavior implements ZombieBehavior {

    /** Seconds between two consecutive casts. */
    public static final float TRANSFORM_INTERVAL = 5.0f;

    /** {@code sheep} clip length on {@code ZOMBIE_DARK_WIZARD}. */
    public static final float SHEEP_DURATION = 2.3f;

    /** Seconds elapsed since the last cast started. */
    private float castTimer = 0f;

    /** True while {@code sheep} plays. */
    private boolean casting = false;

    /** Seconds elapsed in {@link #SHEEP_DURATION}. */
    private float sheepTimer = 0f;

    /** Plant this cast is converting; {@code null} when idle. */
    private PlantInstance currentTarget;

    /** Plants this Wizard has transformed, so they can be reverted on its death. */
    private final List<PlantInstance> transformedPlants = new ArrayList<>();

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        if (casting) {
            sheepTimer += deltaTime;
            if (sheepTimer >= SHEEP_DURATION) {
                casting = false;
                sheepTimer = 0f;
                currentTarget = null;
                if (zombie.getState() == ZombieState.SPECIAL_ACTION) {
                    zombie.setState(ZombieState.WALKING);
                }
            }
            return;
        }

        PlantInstance plantHere = context.getPlantAt(zombie.getGridY(), zombie.getGridX());
        if (canTransform(plantHere)) {
            startCast(zombie, plantHere);
            return;
        }

        castTimer += deltaTime;
        if (castTimer < TRANSFORM_INTERVAL) {
            return;
        }
        castTimer -= TRANSFORM_INTERVAL;

        PlantInstance target = pickRandomTransformablePlant(context);
        if (target != null) {
            startCast(zombie, target);
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.TRANSFORM;
    }

    private static boolean canTransform(PlantInstance plant) {
        return plant != null && plant.getCurrentHP() > 0 && !plant.isTransformed();
    }

    private PlantInstance pickRandomTransformablePlant(BehaviorContext context) {
        List<PlantInstance> candidates = new ArrayList<>();
        for (PlantInstance plant : context.getAllPlants()) {
            if (canTransform(plant)) {
                candidates.add(plant);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(index);
    }

    private void startCast(ZombieInstance zombie, PlantInstance plant) {
        plant.transform();
        transformedPlants.add(plant);
        currentTarget = plant;
        casting = true;
        sheepTimer = 0f;
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    /**
     * Called by the ZombieSystem when this Wizard is killed. Reverts every
     * plant this Wizard had transformed back to its normal state.
     */
    @Override
    public void onZombieDeath(ZombieInstance zombie, BehaviorContext context) {
        for (PlantInstance plant : transformedPlants) {
            if (plant != null) {
                plant.revertTransform();
            }
        }
        transformedPlants.clear();
        casting = false;
        sheepTimer = 0f;
        currentTarget = null;
    }

    public float getCastTimer() {
        return castTimer;
    }

    public boolean isCasting() {
        return casting;
    }

    public float getSheepTimer() {
        return sheepTimer;
    }

    public PlantInstance getCurrentTarget() {
        return currentTarget;
    }

    public List<PlantInstance> getTransformedPlants() {
        return transformedPlants;
    }

    public void setCastTimer(float castTimer) {
        this.castTimer = castTimer;
    }
}
