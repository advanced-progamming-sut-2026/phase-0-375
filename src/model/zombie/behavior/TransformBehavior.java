package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Transform behavior.
 */
public class TransformBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Seconds between two consecutive transformations. */
    public static final float TRANSFORM_INTERVAL = 5.0f;

    // --- State ---

    /** Seconds elapsed since the last transformation. */
    private float castTimer = 0f;

    /** Plants this Wizard has transformed, so they can be reverted on its death. */
    private final List<PlantInstance> transformedPlants = new ArrayList<>();

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        // If the Wizard has walked directly onto a plant, transform it on
        // contact instead of eating it.
        PlantInstance plantHere = context.getPlantAt(zombie.getGridY(), zombie.getGridX());
        if (plantHere != null && plantHere.getCurrentHP() > 0 && !plantHere.isTransformed()) {
            transformPlant(plantHere);
        }

        castTimer += deltaTime;
        if (castTimer < TRANSFORM_INTERVAL) {
            return;
        }
        castTimer -= TRANSFORM_INTERVAL;

        PlantInstance target = pickRandomTransformablePlant(context);
        if (target != null) {
            transformPlant(target);
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.TRANSFORM;
    }

    // --- Core logic ---

    /**
     * Picks a random plant on the field that isn't already transformed.
     */
    private PlantInstance pickRandomTransformablePlant(BehaviorContext context) {
        List<PlantInstance> candidates = new ArrayList<>();
        for (PlantInstance plant : context.getAllPlants()) {
            if (plant != null && plant.getCurrentHP() > 0 && !plant.isTransformed()) {
                candidates.add(plant);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(index);
    }

    /**
     * Transforms the given plant into a cat and tracks it so it can be
     * reverted once this Wizard dies.
     */
    private void transformPlant(PlantInstance plant) {
        plant.transform();
        transformedPlants.add(plant);
    }

    // --- Death handling ---

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
    }

    // --- Getters ---

    public float getCastTimer() {
        return castTimer;
    }

    public List<PlantInstance> getTransformedPlants() {
        return transformedPlants;
    }

    // --- Setters ---

    public void setCastTimer(float castTimer) {
        this.castTimer = castTimer;
    }
}