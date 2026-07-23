package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Strategy for the {@link PlantCategory#MODIFIER} family.
 */
public class ModifierAbility implements PlantAbility {

    /** Velocity of plant-food fire peas. */
    private static final float PF_FIRE_PEA_VELOCITY = 1.25f;

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
                // Torchwood plant-food: launch a burst of fire peas that
                // travel down the lane, roasting every zombie in the way.
                firePeaBurst(plant, context);
                break;
            case RANDOM_HYPNOTIZE:
                // Hypno-shroom plant-food: hypnotise a random zombie.
                hypnotiseRandomZombie(plant, context);
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

    /**
     * Spawns a volley of fire peas traveling forward (toward the zombies)
     * in the Torchwood's lane. The volley size comes from the plant-food
     * value; each pea deals the Torchwood's own damage (which is boosted
     * by the fire element on hit).
     */
    private void firePeaBurst(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        int volley = (int) def.getPlantFoodValue();
        if (volley <= 0) volley = 3;

        int row = plant.getPosition().getY();
        float originX = plant.getPosition().getX() + 0.5f;
        int damage = Math.max(1, def.getDamage() > 0 ? def.getDamage() : 20);

        for (int i = 0; i < volley; i++) {
            Pellet pea = new Pellet(
                    damage,
                    new FloatPoint(originX + i * 0.15f, row),
                    row,
                    PF_FIRE_PEA_VELOCITY,
                    Projectile.Element.FIRE,
                    +1
            );
            context.spawnProjectile(pea, pea.getX(), pea.getY());
        }
    }

    // --- Hypno-shroom plant-food ---

    /**
     * Hypnotizes a single random alive zombie on the field. The hypnotized
     * zombie switches sides and walks back toward the zombie spawn point,
     * attacking other zombies in its path.
     */
    private void hypnotiseRandomZombie(PlantInstance plant, PlantAbilityContext context) {
        ZombieInstance target = pickFirstAliveZombie(plant, context);
        if (target == null) return;
        hypnotise(target);
    }

    /** Flips a zombie to the player's side. */
    private static void hypnotise(ZombieInstance zombie) {
        zombie.setState(ZombieState.HYPNOTIZED);
        zombie.setMovingBackward(true);
    }

    /** Picks the closest alive zombie to the plant. */
    private ZombieInstance pickFirstAliveZombie(PlantInstance plant, PlantAbilityContext context) {
        int plantRow = plant.getPosition().getY();
        int plantCol = plant.getPosition().getX();
        ZombieInstance best = null;
        double bestDist = Double.MAX_VALUE;

        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                if (zombie == null || zombie.isDead()) continue;
                if (zombie.getState() == ZombieState.HYPNOTIZED) continue;

                int zCol = zombie.getGridPosition() != null ? zombie.getGridPosition().getX() : 0;
                double dist = Math.abs(zCol - plantCol) + Math.abs(lane - plantRow);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = zombie;
                }
            }
        }
        return best;
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