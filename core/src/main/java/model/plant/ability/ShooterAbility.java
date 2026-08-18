package model.plant.ability;

import model.enums.BowlingBulbType;
import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantTags;
import model.game.map.FloatPoint;
import model.plant.definition.Plant;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.projectile.BowlingBulb;
import model.projectile.Pellet;
import model.projectile.Projectile;

import java.util.Random;

/**
 * Strategy for the {@link PlantCategory#SHOOTER} family.
 */
public class ShooterAbility implements PlantAbility {

    private static final Random RNG = new Random();

    private static final float PELLET_VELOCITY = 1f;

    private static final int GIANT_PEA_DAMAGE_MULTIPLIER = 20;

    // --- Bowling Bulb constants ---

    private static final float BULB_VELOCITY = 2.0f;
    private static final float BULB_BASE_ACTION_INTERVAL = 2.0f;
    private static final int BULB_PLANT_FOOD_BOUNCES = 3;

    @Override
    public PlantCategory getCategory() { return PlantCategory.SHOOTER; }

    @Override
    public PlantAction beginAction(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return null;

        // Appease-mint: trigger plant-food on every SHOOTER plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.SHOOTER);
            return null;
        }

        if (def.getAbilityType() != PlantAbilityType.SHOOT_PROJECTILE) return null;

        return TimedPlantAction.attackAt(plant, context, this::execute);
    }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        // Appease-mint: trigger plant-food on every SHOOTER plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.SHOOTER);
            return;
        }

        if (def.getAbilityType() != PlantAbilityType.SHOOT_PROJECTILE) return;

        Projectile.Element element = inferElement(def);
        FloatPoint origin = context.plantProjectileOriginOrCell(plant);

        if (isBowlingBulb(def)) {
            shootBowlingBulb(context, plant, origin);
            return;
        }

        int pelletCount;
        if (def.hasTag(PlantTags.STACK)) {
            pelletCount = plant.getStackCount();
        } else {
            pelletCount = (int) def.getAbilityValue();
        }
        if (pelletCount <= 0) pelletCount = 1;

        if (isThreepeater(def)) {
            shootThreepeater(context, plant, pelletCount, origin, element);
        } else if (isSplitPea(def)) {
            shootSplitPea(context, plant, pelletCount, origin, element);
        } else if (isRotobaga(def)) {
            shootRotobaga(context, plant, pelletCount, origin, element);
        } else if (isStarfruit(def)) {
            shootStarfruit(context, plant, pelletCount, origin, element);
        } else {
            shootDefault(context, plant, pelletCount, origin, element);
        }
    }

    @Override
    public float getNextActionCooldown(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null || !isBowlingBulb(def)) return -1f;

        AbilityState state = plant.getAbilityState(def.getAbilityType());
        int cycleIndex = (state != null) ? state.getGrowthStage() : 0;
        BowlingBulbType firedType = bulbTypeForCycleIndex((cycleIndex - 1 + 3) % 3);
        return bulbCooldown(plant, firedType);
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getPlantFoodType() != PlantFoodType.PROJECTILE_BURST) {
            return;
        }

        if (isBowlingBulb(def)) {
            bowlingBulbPlantFood(plant, context);
            return;
        }

        int volley = (int) def.getPlantFoodValue();
        if (def.hasTag(PlantTags.STACK)) {
            volley = volley * plant.getStackCount();
        }
        if (volley <= 0) return;
        Projectile.Element element = inferElement(def);
        int lane = plant.getPosition().getY();
        FloatPoint origin = context.plantProjectileOriginOrCell(plant);
        for (int i = 0; i < volley; i++) {
            if (i % 5 == 0) {
                origin.setX(origin.getX() + i * 0.1f);
            }

            float dx = (RNG.nextFloat() - 0.5f) * 0.4f;
            float dy = (RNG.nextFloat() - 0.5f) * 0.4f;

            FloatPoint pelletOrigin = new FloatPoint(origin.getX() + dx, origin.getY() + dy);

            shootOne(context, def.getDamage(), pelletOrigin, lane,
                    PELLET_VELOCITY * 1.25f,  element, +1, 0f);
        }

        if (shootsGiantPea(def)) {
            for (int i = 0; i < plant.getStackCount(); i++) {
                float offset = ((i & 1) == 0 ? 1 : -1) * 0.1f * ((i + 1) / 2);
                FloatPoint pelletOrigin = new FloatPoint(origin.getX(), origin.getY() + offset);
                shootOne(context, plant.getDefinition().getDamage() * GIANT_PEA_DAMAGE_MULTIPLIER,
                        pelletOrigin, plant.getPosition().getY(), PELLET_VELOCITY, element, +1, 0);
            }
        }
    }

    // --- Helpers ---

    private boolean shouldFire(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return false;
        return context.hasZombieInLane(plant.getPosition().getY());
    }

    private Projectile.Element inferElement(Plant def) {
        if (def.hasTag(PlantTags.ICE)) return Projectile.Element.ICE;
        if (def.hasTag(PlantTags.FIRE)) return Projectile.Element.FIRE;
        if (def.hasTag(PlantTags.POISON)) return Projectile.Element.POISON;
        return Projectile.Element.NONE;
    }

    private boolean shootsGiantPea(Plant def) {
        return def.getName().toLowerCase().contains("repeater") ||
                def.getName().toLowerCase().contains("pea pod");
    }

    private boolean isThreepeater(Plant def) {
        return def.getName().toLowerCase().contains("threepeater");
    }

    private boolean isSplitPea(Plant def) {
        return def.getName().toLowerCase().contains("split pea");
    }

    private boolean isRotobaga(Plant def) {
        return def.getName().toLowerCase().contains("rotobaga");
    }

    private boolean isStarfruit(Plant def) {
        return def.getName().toLowerCase().contains("starfruit");
    }

    private boolean isBowlingBulb(Plant def) {
        return def.getName() != null
                && def.getName().toLowerCase().contains("bowling bulb");
    }

    // --- Bowling Bulb ---

    private void shootBowlingBulb(PlantAbilityContext context, PlantInstance plant,
                                  FloatPoint origin) {
        Plant def = plant.getDefinition();
        AbilityState state = plant.getAbilityState(def.getAbilityType());
        if (state == null) {
            state = new AbilityState(def.getAbilityType());
            plant.getAbilityStates().put(def.getAbilityType(), state);
        }

        int cycleIndex = state.getGrowthStage() % 3;
        if (cycleIndex < 0) cycleIndex = 0;
        BowlingBulbType type = bulbTypeForCycleIndex(cycleIndex);

        int damage = bulbDamage(plant, type);
        int row = plant.getPosition().getY();

        BowlingBulb bulb = new BowlingBulb(
                damage,
                new FloatPoint(origin.getX(), origin.getY()),
                row,
                BULB_VELOCITY,
                type,
                type.getMaxBounces());
        context.spawnProjectile(bulb, bulb.getX(), bulb.getY());
        state.setGrowthStage((cycleIndex + 1) % 3);
    }

    private void bowlingBulbPlantFood(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        int row = plant.getPosition().getY();
        float originX = context.plantProjectileOriginOrCell(plant).getX();
        int damage = (int) def.getPlantFoodValue();
        if (damage <= 0) damage = def.getDamage() * 3;

        for (int i = 0; i < 3; i++) {
            if (row < 0 || row >= context.getRowCount()) continue;

            BowlingBulb bulb = new BowlingBulb(
                    damage,
                    new FloatPoint(originX + i * 0.2f, row),
                    row,
                    BULB_VELOCITY,
                    BowlingBulbType.ORANGE,
                    BULB_PLANT_FOOD_BOUNCES);
            bulb.setExplosive(true);
            context.spawnProjectile(bulb, bulb.getX(), bulb.getY());
        }
    }

    /** Maps a cycle index (0..2) to its bulb type. */
    public BowlingBulbType bulbTypeForCycleIndex(int cycleIndex) {
        switch (cycleIndex) {
            case 1: return BowlingBulbType.BLUE;
            case 2: return BowlingBulbType.ORANGE;
            default: return BowlingBulbType.CYAN;
        }
    }

    /** Resolves a bulb's damage. */
    private int bulbDamage(PlantInstance plant, BowlingBulbType type) {
        Plant def = plant.getDefinition();
        int buffDelta = def.getDamage() - BowlingBulbType.CYAN.getBaseDamage();
        return type.getBaseDamage() + Math.max(0, buffDelta);
    }

    /** Resolves a bulb's regenerate cooldown. */
    private float bulbCooldown(PlantInstance plant, BowlingBulbType type) {
        Plant def = plant.getDefinition();
        float reduction = BULB_BASE_ACTION_INTERVAL - def.getActionInterval();
        return Math.max(0f, type.getBaseCooldownSeconds() - Math.max(0f, reduction));
    }

    private void shootOne(PlantAbilityContext context, int damage, FloatPoint origin, int row,
                          float velocity, Projectile.Element element, int direction, float yVelocity) {
        Pellet pellet = new Pellet(
                damage,
                new FloatPoint(origin.getX(), origin.getY()),
                row,
                velocity,
                element,
                direction
        );
        pellet.setYVelocity(yVelocity);
        context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
    }

    private void shootDefault(PlantAbilityContext context, PlantInstance plant, int pelletCount,
                              FloatPoint origin, Projectile.Element element) {
        for (int i = 0; i < pelletCount; i++) {
            FloatPoint pelletOrigin = new FloatPoint(origin.getX() + i * 0.5f, origin.getY());
            shootOne(context, plant.getDefinition().getDamage(), pelletOrigin,
                    plant.getPosition().getY(), PELLET_VELOCITY, element, +1, 0);
        }
    }

    private void shootThreepeater(PlantAbilityContext context, PlantInstance plant, int pelletCount,
                                  FloatPoint origin, Projectile.Element element) {
        shootOne(context, plant.getDefinition().getDamage(), origin,
            plant.getPosition().getY(), PELLET_VELOCITY, element, +1, 0);

        FloatPoint secondOneOrigin = new FloatPoint(
            origin.getX() - 0.5f, origin.getY() + 0.1f
        );
        shootOne(context, plant.getDefinition().getDamage(), secondOneOrigin,
            plant.getPosition().getY(), PELLET_VELOCITY, element, +1, 0);

        FloatPoint thirdOneOrigin = new FloatPoint(
            origin.getX() - 0.4f, origin.getY() - 0.2f
        );
        shootOne(context, plant.getDefinition().getDamage(), thirdOneOrigin,
            plant.getPosition().getY(), PELLET_VELOCITY, element, +1, 0);
    }

    private void shootSplitPea(PlantAbilityContext context, PlantInstance plant, int pelletCount,
                               FloatPoint origin, Projectile.Element element) {
        shootOne(context, plant.getDefinition().getDamage(), origin,
            plant.getPosition().getY(), PELLET_VELOCITY, element,
            1, 0);

        FloatPoint firstBackwardOrigin = new FloatPoint(
            origin.getX() - 1f, origin.getY() - 0.05f
        );
        shootOne(context, plant.getDefinition().getDamage(), firstBackwardOrigin,
            plant.getPosition().getY(), PELLET_VELOCITY, element,
            -1, 0);

        FloatPoint secondBackwardOrigin = new FloatPoint(
            origin.getX() - 1.5f, origin.getY() - 0.05f
        );
        shootOne(context, plant.getDefinition().getDamage(), secondBackwardOrigin,
            plant.getPosition().getY(), PELLET_VELOCITY, element,
            -1, 0);
    }

    private void shootRotobaga(PlantAbilityContext context, PlantInstance plant, int pelletCount,
                               FloatPoint origin, Projectile.Element element) {
        FloatPoint firstOrigin = new FloatPoint(
            origin.getX(), origin.getY() + 0.3f
        );
        shootOne(context, plant.getDefinition().getDamage(), firstOrigin,
            plant.getPosition().getY(), PELLET_VELOCITY, element,
            +1, PELLET_VELOCITY);

        FloatPoint secondOrigin = new FloatPoint(
            origin.getX(), origin.getY() - 0.1f
        );
        shootOne(context, plant.getDefinition().getDamage(), secondOrigin,
            plant.getPosition().getY(), PELLET_VELOCITY, element,
            +1, -PELLET_VELOCITY);

        FloatPoint thirdOrigin = new FloatPoint(
            origin.getX() + 0.1f, origin.getY() + 0.3f
        );
        shootOne(context, plant.getDefinition().getDamage(), thirdOrigin,
            plant.getPosition().getY(), PELLET_VELOCITY, element,
            -1, PELLET_VELOCITY);

        FloatPoint fourthOrigin = new FloatPoint(
            origin.getX(), origin.getY() - 0.1f
        );
        shootOne(context, plant.getDefinition().getDamage(), fourthOrigin,
            plant.getPosition().getY(), PELLET_VELOCITY, element,
            -1, -PELLET_VELOCITY);
    }

    private void shootStarfruit(PlantAbilityContext context, PlantInstance plant, int pelletCount,
                                FloatPoint origin, Projectile.Element element) {
        shootOne(context, plant.getDefinition().getDamage(), origin,
                plant.getPosition().getY(), PELLET_VELOCITY, element,
                -1, 0);

        shootOne(context, plant.getDefinition().getDamage(), origin,
                plant.getPosition().getY(), 0, element,
                1, PELLET_VELOCITY);

        shootOne(context, plant.getDefinition().getDamage(), origin,
                plant.getPosition().getY(), 0, element,
                1, -PELLET_VELOCITY);

        shootOne(context, plant.getDefinition().getDamage(), origin,
                plant.getPosition().getY(), PELLET_VELOCITY, element,
                1, PELLET_VELOCITY);

        shootOne(context, plant.getDefinition().getDamage(), origin,
                plant.getPosition().getY(), PELLET_VELOCITY, element,
                1, -PELLET_VELOCITY);
    }
}
