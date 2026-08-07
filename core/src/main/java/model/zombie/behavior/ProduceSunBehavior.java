package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.zombie.instance.ZombieInstance;

/**
 * Produces sun for the player over time. Used by the I, Zombie sun zombies.
 */
public class ProduceSunBehavior implements ZombieBehavior {
    public static final float DEFAULT_INTERVAL_SECONDS = 8f;
    public static final int DEFAULT_BASE_AMOUNT = 25;
    public static final int DEFAULT_GROWTH_AMOUNT = 5;
    private float productionTimer;
    private int productionCount;

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        float interval = zombie.getDefinition().getBehaviorPropFloat(
                "SunProduceIntervalSeconds", DEFAULT_INTERVAL_SECONDS);
        if (interval <= 0f) {
            interval = DEFAULT_INTERVAL_SECONDS;
        }

        productionTimer += deltaTime;
        if (productionTimer < interval) {
            return;
        }
        productionTimer -= interval;

        int base = zombie.getDefinition().getBehaviorPropInt(
                "SunProduceBaseAmount", DEFAULT_BASE_AMOUNT);
        if (base <= 0) {
            base = DEFAULT_BASE_AMOUNT;
        }
        int growth = zombie.getDefinition().getBehaviorPropInt(
                "SunProduceGrowthAmount", DEFAULT_GROWTH_AMOUNT);
        if (growth < 0) {
            growth = DEFAULT_GROWTH_AMOUNT;
        }

        // The production rate grows over time: each drop is worth more.
        context.addSun(base + growth * productionCount);
        productionCount++;
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.PRODUCE_SUN;
    }

    // --- Getters ---

    public int getProductionCount() {
        return productionCount;
    }
}
