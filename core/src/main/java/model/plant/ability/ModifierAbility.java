package model.plant.ability;

import model.enums.*;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.ZombieFactory;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Strategy for the {@link PlantCategory#MODIFIER} family.
 */
public class ModifierAbility implements PlantAbility {

    /** Velocity of plant-food fire peas. */
    private static final float PF_FIRE_PEA_VELOCITY = 1.25f;

    private float torchwoodDamageMultiplier = 2f;

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
                // Torchwood plant-food: increases the damage multiplier
                // which affects the projectiles that pass through it
                applyTorchwoodPlantFoodBuff();
                break;
            case RANDOM_HYPNOTIZE:
                // Hypno-shroom plant-food: hypnotize the eater zombie and
                // transform it to a gargantuar.
                hypnotiseTheEater(plant, context);
                break;
            case SPAWN_CLONES:
                // Lily Pad plant-food: spawn clones on adjacent water tiles.
                spawnLilyPadClones(plant, context, (int) def.getPlantFoodValue());
                break;
            default:
                break;
        }
    }

    // --- Torchwood plant-food ---

    public void applyTorchwoodPlantFoodBuff() {
        this.torchwoodDamageMultiplier = 3f;
    }

    public float getTorchwoodDamageMultiplier() {
        return torchwoodDamageMultiplier;
    }

    // --- Hypno-shroom plant-food ---

    /**
     * transform the zombie that eats him into a Gargantuar
     * with increased health from a regular one.
     */
    private void hypnotiseTheEater(PlantInstance plant, PlantAbilityContext context) {
        ZombieInstance eater = getEaterOf(plant, context);
        if (eater == null) return;

        if(eater.getDefinition().getSize() == ZombieSize.LARGE) {
            hypnotise(eater);
            return;
        }

        ZombieInstance newGargantuar = context.spawnZombieAt(
                "ZombieGargantuar", eater.getGridY(), eater.getGridX()
                );
        if(newGargantuar == null) return;
        hypnotise(newGargantuar);
        context.removeZombie(eater);
    }

    /** Flips a zombie to the player's side. */
    private static void hypnotise(ZombieInstance zombie) {
        zombie.setState(ZombieState.HYPNOTIZED);
        zombie.setMovingBackward(true);
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

    // --- Lily Pad plant-food ---

    /**
     * Spawns up to {@code count} Lily Pad clones on adjacent water tiles
     * (8-neighborhood) that are currently empty. Each clone is a fresh
     * level-1 Lily Pad instance placed via the context.
     */
    private void spawnLilyPadClones(PlantInstance plant, PlantAbilityContext context, int count) {
        if (count <= 0) return;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        int spawned = 0;

        for (int rowDist = -1; rowDist <= 1 && spawned < count; rowDist++) {
            for (int colDist = -1; colDist <= 1 && spawned < count; colDist++) {
                if (rowDist == 0 && colDist == 0) continue;
                int targetRow = row + rowDist;
                int targetCol = col + colDist;
                if (targetRow < 0 || targetCol < 0 ||
                    targetRow >= context.getRowCount() ||
                    targetCol >= context.getColumnCount()) {
                        continue;
                }
                if (!context.isWaterTile(targetRow, targetCol)) continue;
                if (context.getPlantAt(targetRow, targetCol) != null) continue;

                PlantInstance clone = PlantFactory.createInstance("Lily Pad");
                if (clone == null) continue;
                if (context.placePlant(clone, targetRow, targetCol)) {
                    spawned++;
                }
            }
        }
    }
}