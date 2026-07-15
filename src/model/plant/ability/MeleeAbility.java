package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantSpecialTag;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Strategy for the {@link PlantCategory#MELEE} family.
 */
public class MeleeAbility implements PlantAbility {

    @Override
    public PlantCategory getCategory() { return PlantCategory.MELEE; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        if (def == null) return;

        // Enforce-mint: trigger plant-food on every MELEE plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.MELEE);
            return;
        }

        // Chomper: swallow-then-digest cycle.
        if (def.getAbilityType() == PlantAbilityType.DELAYED_EXPLOSIVE) {
            handleChomper(plant, context);
            return;
        }

        if (def.getAbilityType() != PlantAbilityType.MELEE_ATTACK) return;

        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();

        // AoE melee: hit everything in a small area
        int radius = (int) def.getAbilityValue();
        if (radius >= 9) {
            // Big swipe - 3x3 around the plant
            List<ZombieInstance> targets = context.getZombiesInArea(row, col, 1, 1);
            for (ZombieInstance zombie : targets) {
                context.damageZombie(zombie, def.getDamage());
            }
            return;
        }

        // Single-target melee - hit the first zombie in any of the 8 neighbors
        for (int rowDist = -1; rowDist <= 1; rowDist++) {
            for (int colDist = -1; colDist <= 1; colDist++) {
                if (rowDist == 0 && colDist == 0) continue;
                List<ZombieInstance> targets = context.getZombiesInArea(row + rowDist, col + colDist, 0, 0);
                if (!targets.isEmpty()) {
                    context.damageZombie(targets.getFirst(), def.getDamage());
                    return;
                }
            }
        }
    }

    // --- Chomper swallow + digest cycle ---

    /** Default digestion time (seconds) after the Chomper swallows a zombie. */
    private static final float CHOMPER_DIGEST_DURATION = 30.0f;
    /** Damage dealt to the swallowed zombie (intentionally huge to one-shot). */
    private static final int CHOMPER_SWALLOW_DAMAGE = 6767;

    /** Implements the Chomper's signature swallow-then-digest cycle. */
    private void handleChomper(PlantInstance plant, PlantAbilityContext context) {
        AbilityState state = plant.getAbilityState(PlantAbilityType.DELAYED_EXPLOSIVE);
        if (state == null) return;

        // Phase 1: still digesting, wait.
        if (state.isDigesting()) return;

        // Phase 2: look for a swallowable zombie.
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        List<ZombieInstance> candidates = context.getZombiesInArea(row, col, 0, 1);
        ZombieInstance target = null;
        for (ZombieInstance zombie : candidates) {
            if (zombie == null || zombie.isDead()) continue;
            target = zombie;
            break;
        }
        if (target == null) return;

        // Swallow the zombie.
        context.damageZombie(target, CHOMPER_SWALLOW_DAMAGE);

        // Enter digestion phase.
        float digestDuration = CHOMPER_DIGEST_DURATION;
        float reduction = cumulativeDigestReduction(plant);
        digestDuration = Math.max(5f, digestDuration - reduction);

        state.setDigesting(true);
        state.setDigestRemaining(digestDuration);
        state.setCooldownRemaining(digestDuration);
    }

    /**
     * Sums up every {@link PlantSpecialTag#DIGEST_REDUCTION} upgrade
     * value the plant has accumulated via its level upgrades.
     */
    private float cumulativeDigestReduction(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null) return 0f;
        PlantLevels levels = def.getLevels();
        float total = 0f;
        for (int lvl = 2; lvl <= 4; lvl++) {
            if (lvl > plant.getLevel()) break;
            LevelUpgrade upgrade = levels.getUpgrade(lvl);
            if (upgrade == null) continue;
            if (upgrade.isSpecialMechanic()
                    && upgrade.getSpecialTag() == PlantSpecialTag.DIGEST_REDUCTION) {
                total += upgrade.getValue();
            }
        }
        return total;
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getPlantFoodType() != PlantFoodType.LOCAL_AOE_ATTACK) return;
        if (plant.getPosition() == null) return;

        int radius = (int) def.getPlantFoodValue();
        if (radius <= 0) radius = 1;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        for (ZombieInstance zombie : context.getZombiesInArea(row, col, radius, radius)) {
            context.damageZombie(zombie, def.getDamage() * 3);
        }
    }
}