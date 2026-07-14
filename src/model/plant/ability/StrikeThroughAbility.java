package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.game.map.FloatPoint;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

/**
 * Strategy for the {@link PlantCategory#STRIKE_THROUGH} family.
 */
public class StrikeThroughAbility implements PlantAbility {

    private static final float PELLET_VELOCITY = 1f;

    @Override
    public PlantCategory getCategory() { return PlantCategory.STRIKE_THROUGH; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        if (!context.hasZombieInLane(plant.getPosition().getY())) return;

        Plant def = plant.getDefinition();
        int pelletsCount = (int) def.getAbilityValue();
        if (pelletsCount <= 0) pelletsCount = 1;

        int row = plant.getPosition().getY();
        FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f, row);

        for (int i = 0; i < pelletsCount; i++) {
            Pellet pellet = new Pellet(
                    def.getDamage(),
                    new FloatPoint(origin.getX() + i * 0.05f, origin.getY()),
                    row,
                    PELLET_VELOCITY,
                    Projectile.Element.NONE,
                    +1
            );
            pellet.setPierce(true);
            context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
        }
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getPlantFoodType() == PlantFoodType.KNOCKBACK_BLAST) {
            if (plant.getPosition() == null) return;
            int row = plant.getPosition().getY();
            for (ZombieInstance zombie : context.getZombiesInLane(row)) {
                context.damageZombie(zombie, (int) def.getPlantFoodValue());
            }
        }
    }
}