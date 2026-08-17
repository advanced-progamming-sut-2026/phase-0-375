package model.plant.ability;

import model.enums.*;
import model.game.map.FloatPoint;
import model.plant.PlantFactory;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Strategy for the {@link PlantCategory#MODIFIER} family.
 */
public class ModifierAbility implements PlantAbility {

    private static final String ALLIED_GARGANTUAR = "ZombieGargantuar";
    private static final float DEFAULT_FIRE_MULTIPLIER = 2f;
    private static final float DEFAULT_BLUE_FIRE_MULTIPLIER = 3f;

    @Override
    public PlantCategory getCategory() { return PlantCategory.MODIFIER; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        PlantAbilityType type = def.getAbilityType();
        if (type == null) return;

        switch (type) {
            case MINT_FAMILY_BOOST:
                // Enchant-mint: one-shot boost of every MODIFIER plant.
                context.triggerFamilyPlantFood(PlantCategory.MODIFIER);
                break;

            case MODIFIER_UTILITY:
                // Imitater's countdown is ticked by PlantInstance.tick().
                // Torchwood / Hypno-shroom / Lily Pad are passive field
                // modifiers whose hooks live in ProjectileSystem /
                // ZombieSystem - they have no per-tick action here.
                break;

            default:
                break;
        }
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;
        if (plant.getPosition() == null) return;

        switch (def.getPlantFoodType()) {
            case PROJECTILE_BURST:
                // Torchwood: blue flame for the plant-food window. Damage
                // is read from plant-food state in getTorchwoodDamageMultiplier.
                break;
            case RANDOM_HYPNOTIZE:
                // Hypno-shroom: turn the eating (or nearest) zombie into
                // an allied Gargantuar.
                hypnotiseTheEater(plant, context);
                break;
            case SPAWN_CLONES:
                // Lily Pad: spawn copies on empty water tiles.
                spawnLilyPadClones(plant, context, (int) def.getPlantFoodValue());
                break;
            default:
                break;
        }
    }

    // --- Torchwood ---

    /**
     * Damage multiplier for peas passing through this Torchwood: 2× normally,
     * 3× (blue flame) while plant-food is active.
     */
    public float getTorchwoodDamageMultiplier(PlantInstance plant) {
        Plant def = plant == null ? null : plant.getDefinition();
        if (plant != null && plant.isPlantFoodActive()) {
            float blue = def != null ? def.getPlantFoodValue() : 0f;
            return blue > 0f ? blue : DEFAULT_BLUE_FIRE_MULTIPLIER;
        }
        float base = def != null ? def.getAbilityValue() : 0f;
        return base > 0f ? base : DEFAULT_FIRE_MULTIPLIER;
    }

    public boolean isBlueFlameActive(PlantInstance plant) {
        return plant != null && plant.isPlantFoodActive();
    }

    // --- Hypno-shroom plant-food ---

    /**
     * Turns the zombie eating this Hypno-shroom (or the nearest one in its
     * lane) into a hypnotized Gargantuar.
     */
    private void hypnotiseTheEater(PlantInstance plant, PlantAbilityContext context) {
        ZombieInstance eater = getEaterOf(plant, context);
        if (eater == null) {
            eater = nearestZombieInLane(plant, context);
        }
        if (eater == null) return;

        convertToAlliedGargantuar(plant, eater, context);
    }

    /**
     * Replaces {@code zombie} with a hypnotized Gargantuar unless it is
     * already {@link ZombieSize#LARGE}, in which case it is only hypnotized.
     */
    public void convertToAlliedGargantuar(PlantInstance plant, ZombieInstance zombie,
                                          PlantAbilityContext context) {
        if (zombie == null) return;
        if (zombie.getDefinition() != null && zombie.getDefinition().getSize() == ZombieSize.LARGE) {
            hypnotise(zombie);
            applyGargantuarBuffs(plant, zombie);
            return;
        }

        int row = zombie.getGridY();
        int col = zombie.getGridX();
        ZombieInstance gargantuar = context.spawnZombieAt(ALLIED_GARGANTUAR, row, col);
        if (gargantuar == null) {
            hypnotise(zombie);
            return;
        }
        gargantuar.setContinuousPosition(new FloatPoint(
                zombie.getContinuousX(), zombie.getContinuousY()));
        hypnotise(gargantuar);
        applyGargantuarBuffs(plant, gargantuar);
        context.removeZombie(zombie);
    }

    private void applyGargantuarBuffs(PlantInstance plant, ZombieInstance zombie) {
        if (plant == null || zombie == null) return;
        float hpMul = specialValue(plant, PlantSpecialTag.ZOMBIE_HEALTH_MULTIPLIER);
        if (hpMul > 0f && hpMul != 1f) {
            zombie.setCurrentHP(Math.max(1, Math.round(zombie.getCurrentHP() * hpMul)));
        }
    }

    /** Flips a zombie to the player's side. */
    public static void hypnotise(ZombieInstance zombie) {
        if (zombie == null) return;
        zombie.hypnotise();
    }

    /** @return the zombie that is eating the {@code plant}. {@code null} if none. */
    private ZombieInstance getEaterOf(PlantInstance plant, PlantAbilityContext context) {
        int plantRow = plant.getPosition().getY();
        int plantCol = plant.getPosition().getX();
        List<ZombieInstance> zombies = context.getZombiesInArea(plantRow, plantCol, 0, 0);
        for (ZombieInstance zombie : zombies) {
            if (zombie.getEatingTarget() == plant) {
                return zombie;
            }
        }
        return null;
    }

    private ZombieInstance nearestZombieInLane(PlantInstance plant, PlantAbilityContext context) {
        int row = plant.getPosition().getY();
        float col = plant.getPosition().getX();
        ZombieInstance best = null;
        float bestDist = Float.MAX_VALUE;
        for (ZombieInstance zombie : context.getZombiesInLane(row)) {
            if (zombie == null || zombie.isDead() || zombie.isHypnotized()) continue;
            float dist = Math.abs(zombie.getContinuousX() - col);
            if (dist < bestDist) {
                bestDist = dist;
                best = zombie;
            }
        }
        return best;
    }

    private static float specialValue(PlantInstance plant, PlantSpecialTag tag) {
        if (plant == null || plant.getDefinition() == null || tag == null) return 0f;
        Plant def = plant.getDefinition();
        if (def.getLevels() == null) return 0f;
        float total = 0f;
        for (int lvl = 2; lvl <= plant.getLevel(); lvl++) {
            LevelUpgrade upgrade = def.getLevels().getUpgrade(lvl);
            if (upgrade != null && upgrade.isSpecialMechanic() && upgrade.getSpecialTag() == tag) {
                total += upgrade.getValue();
            }
        }
        return total;
    }

    // --- Lily Pad plant-food ---

    /**
     * Spawns up to {@code count} Lily Pad clones on empty water tiles,
     * nearest first. Each clone is a fresh level-1 Lily Pad.
     */
    private void spawnLilyPadClones(PlantInstance plant, PlantAbilityContext context, int count) {
        if (count <= 0) return;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();

        List<int[]> tiles = new ArrayList<>();
        for (int r = 0; r < context.getRowCount(); r++) {
            for (int c = 0; c < context.getColumnCount(); c++) {
                if (r == row && c == col) continue;
                if (!context.isWaterTile(r, c)) continue;
                tiles.add(new int[]{r, c});
            }
        }
        tiles.sort(Comparator.comparingInt(t -> Math.abs(t[0] - row) + Math.abs(t[1] - col)));

        int spawned = 0;
        for (int[] tile : tiles) {
            if (spawned >= count) break;
            PlantInstance clone = PlantFactory.createInstance("Lily Pad");
            if (clone == null) continue;
            if (context.placePlant(clone, tile[0], tile[1])) {
                spawned++;
            }
        }
    }
}
