package model.plant.ability;

import model.enums.BowlingBulbType;
import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantSpecialTag;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.game.map.FloatPoint;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.projectile.BowlingBulb;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

import java.util.Random;

/**
 * Strategy for the {@link PlantCategory#SHOOTER} family.
 */
public class ShooterAbility implements PlantAbility {

    private static final Random RNG = new Random();

    private static final float PELLET_VELOCITY = 1f;

    private static final int GIANT_PEA_DAMAGE_MULTIPLIER = 20;

    private static final float SHROOM_RANGE_TILES = 3f;

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
        if (plant.getPosition() == null) return null;

        if (!shouldFireGate(plant, context, def)) return null;

        if (isBowlingBulb(def)) {
            return beginBowlingBulbAction(plant, context);
        }
        return TimedPlantAction.attackAt(plant, context, this::execute);
    }

    private PlantAction beginBowlingBulbAction(PlantInstance plant, PlantAbilityContext context) {
        float duration = TimedPlantAction.presentationDurationFor(
                plant, context, PlantState.ATTACKING, TimedPlantAction.DEFAULT_ATTACK_DURATION);
        return new TimedPlantAction(
                PlantState.ATTACKING,
                duration,
                null,
                (p, ctx) -> shootBowlingBulb(ctx, p, ctx.plantProjectileOriginOrCell(p)),
                TimedPlantAction.DEFAULT_ATTACK_FIRE_FRACTION,
                this::advanceBowlingBulbCycle);
    }

    private boolean shouldFireGate(PlantInstance plant, PlantAbilityContext context, Plant def) {
        int row = plant.getPosition().getY();
        float plantX = plant.getPosition().getX();
        if (isRotobaga(def)) {
            return context.hasZombieAlongDiagonal(row, plantX, +1, +1f, context.getRowCount(), context.getColumnCount())
                || context.hasZombieAlongDiagonal(row, plantX, +1, -1f, context.getRowCount(), context.getColumnCount())
                || context.hasZombieAlongDiagonal(row, plantX, -1, +1f, context.getRowCount(), context.getColumnCount())
                || context.hasZombieAlongDiagonal(row, plantX, -1, -1f, context.getRowCount(), context.getColumnCount());
        }
        if (isStarfruit(def)) {
            return context.hasZombieOrGraveAhead(row, plantX, -1)
                || context.hasZombieOrGraveAhead(row, plantX, +1)
                || hasZombieInAdjacentLane(context, row, plantX, +1)
                || hasZombieInAdjacentLane(context, row, plantX, -1)
                || context.hasZombieAlongDiagonal(row, plantX, +1, +1f, context.getRowCount(), context.getColumnCount())
                || context.hasZombieAlongDiagonal(row, plantX, +1, -1f, context.getRowCount(), context.getColumnCount());
        }
        if (isSplitPea(def)) {
            return context.hasZombieOrGraveAhead(row, plantX, +1)
                || context.hasZombieOrGraveAhead(row, plantX, -1);
        }
        if (isBowlingBulb(def)) {
            return context.hasZombieInLane(row);
        }
        if (isShroomShooter(def)) {
            return context.hasZombieOrGraveAheadInRange(row, plantX, +1, shroomRangeTiles(plant));
        }
        return context.hasZombieOrGraveAhead(row, plantX, +1);
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
            shootThreepeater(context, plant, origin, element);
        } else if (isSplitPea(def)) {
            shootSplitPea(context, plant, origin, element);
        } else if (isRotobaga(def)) {
            shootRotobaga(context, plant, origin, element);
        } else if (isStarfruit(def)) {
            shootStarfruit(context, plant, origin, element);
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
        BowlingBulbType firingType = bulbTypeForCycleIndex(cycleIndex);
        return bulbCooldown(plant, firingType);
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getPlantFoodType() != PlantFoodType.PROJECTILE_BURST) {
            return;
        }
        if (plant.getPosition() == null) return;

        if (isBowlingBulb(def)) {
            bowlingBulbPlantFood(plant, context);
            return;
        }
        if (isCitron(def)) {
            citronPlantFood(plant, context);
            return;
        }
        if (isSnowPea(def)) {
            snowPeaPlantFood(plant, context);
            return;
        }
        if (isShroomShooter(def)) {
            resetShroomFamilyLifespan(plant, context);
            return;
        }
        if (isThreepeater(def)) {
            threepeaterPlantFood(plant, context);
            return;
        }
        if (isSplitPea(def)) {
            splitPeaPlantFood(plant, context);
            return;
        }
        if (isRotobaga(def)) {
            rotobagaPlantFood(plant, context);
            return;
        }
        if (isStarfruit(def)) {
            starfruitPlantFood(plant, context);
            return;
        }

        projectileBurstPlantFood(plant, context, +1, 0f, false);

        if (shootsGiantPea(def)) {
            spawnGiantPeas(plant, context, inferElement(def), +1, 0f);
        }
    }

    // --- Plant-food variants ---

    private void projectileBurstPlantFood(PlantInstance plant, PlantAbilityContext context,
                                          int direction, float yVelocity, boolean allLanes) {
        Plant def = plant.getDefinition();
        int volley = (int) def.getPlantFoodValue();
        if (def.hasTag(PlantTags.STACK)) {
            volley = volley * plant.getStackCount();
        }
        if (volley <= 0) return;

        Projectile.Element element = inferElement(def);
        FloatPoint origin = context.plantProjectileOriginOrCell(plant);
        int plantRow = plant.getPosition().getY();

        if (allLanes) {
            int perLane = Math.max(1, volley / Math.max(1, context.getRowCount()));
            for (int lane = 0; lane < context.getRowCount(); lane++) {
                burstVolley(context, def, plant, origin, lane, perLane, element, direction, yVelocity);
            }
            return;
        }

        burstVolley(context, def, plant, origin, plantRow, volley, element, direction, yVelocity);
    }

    private void burstVolley(PlantAbilityContext context, Plant def, PlantInstance plant,
                             FloatPoint origin, int lane, int volley,
                             Projectile.Element element, int direction, float yVelocity) {
        for (int i = 0; i < volley; i++) {
            float dx = (RNG.nextFloat() - 0.5f) * 0.4f;
            float dy = (RNG.nextFloat() - 0.5f) * 0.4f;
            FloatPoint pelletOrigin = new FloatPoint(origin.getX() + dx + i * 0.05f, origin.getY() + dy);
            shootOne(context, def.getDamage(), pelletOrigin, lane,
                    PELLET_VELOCITY * 1.25f, element, direction, yVelocity);
        }
    }

    private void citronPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        int row = plant.getPosition().getY();
        FloatPoint origin = context.plantProjectileOriginOrCell(plant);
        int damage = (int) def.getPlantFoodValue();
        if (damage <= 0) damage = def.getDamage() * 2;

        Pellet plasma = new Pellet(
                damage,
                new FloatPoint(origin.getX(), origin.getY()),
                row,
                PELLET_VELOCITY * 2f,
                Projectile.Element.NONE,
                +1
        );
        plasma.setPierce(true);
        context.spawnProjectile(plasma, plasma.getX(), plasma.getY());
    }

    private void snowPeaPlantFood(PlantInstance plant, PlantAbilityContext context) {
        int row = plant.getPosition().getY();
        for (ZombieInstance zombie : context.getZombiesInLane(row)) {
            if (zombie == null || zombie.isDead() || zombie.isHypnotized()) continue;
            zombie.applyChill();
            zombie.applyChill();
            zombie.applyChill();
        }
        projectileBurstPlantFood(plant, context, +1, 0f, false);
    }

    private void threepeaterPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        int volley = (int) def.getPlantFoodValue();
        if (volley <= 0) return;
        int perLane = Math.max(1, volley / 3);
        FloatPoint origin = context.plantProjectileOriginOrCell(plant);
        Projectile.Element element = inferElement(def);
        int centerRow = plant.getPosition().getY();
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            int lane = centerRow + rowOffset;
            if (lane < 0 || lane >= context.getRowCount()) continue;
            burstVolley(context, def, plant, origin, lane, perLane, element, +1, 0f);
        }
    }

    private void splitPeaPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        int volley = (int) def.getPlantFoodValue();
        if (volley <= 0) return;
        int half = Math.max(1, volley / 2);
        Projectile.Element element = inferElement(def);
        FloatPoint toRightOrigin = context.plantProjectileOriginOrCell(plant);
        FloatPoint toLeftOrigin = new FloatPoint(toRightOrigin.getX() - 3.5f, toRightOrigin.getY());
        int row = plant.getPosition().getY();
        burstVolley(context, def, plant, toRightOrigin, row, half, element, +1, 0f);
        burstVolley(context, def, plant, toLeftOrigin, row, half, element, -1, 0f);
    }

    private void rotobagaPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        int volley = (int) def.getPlantFoodValue();
        if (volley <= 0) return;
        int perDirection = Math.max(1, volley / 4);
        Projectile.Element element = inferElement(def);
        FloatPoint origin = context.plantProjectileOriginOrCell(plant);
        int row = plant.getPosition().getY();
        for (int i = 0; i < perDirection; i++) {
            float jitter = i * 0.05f;
            shootOne(context, def.getDamage(), new FloatPoint(origin.getX(), origin.getY() + 0.3f + jitter),
                    row, PELLET_VELOCITY * 1.25f, element, +1, PELLET_VELOCITY);
            shootOne(context, def.getDamage(), new FloatPoint(origin.getX(), origin.getY() - 0.1f + jitter),
                    row, PELLET_VELOCITY * 1.25f, element, +1, -PELLET_VELOCITY);
            shootOne(context, def.getDamage(), new FloatPoint(origin.getX() + 0.1f + jitter, origin.getY() + 0.3f),
                    row, PELLET_VELOCITY * 1.25f, element, -1, PELLET_VELOCITY);
            shootOne(context, def.getDamage(), new FloatPoint(origin.getX() + jitter, origin.getY() - 0.1f),
                    row, PELLET_VELOCITY * 1.25f, element, -1, -PELLET_VELOCITY);
        }
    }

    private void starfruitPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        int volley = (int) def.getPlantFoodValue();
        if (volley <= 0) return;
        int perDirection = Math.max(1, volley / 5);
        Projectile.Element element = inferElement(def);
        FloatPoint origin = context.plantProjectileOriginOrCell(plant);
        int row = plant.getPosition().getY();
        for (int i = 0; i < perDirection; i++) {
            float jitter = i * 0.05f;
            shootOne(context, def.getDamage(), new FloatPoint(origin.getX() + jitter, origin.getY()),
                    row, PELLET_VELOCITY * 1.25f, element, -1, 0f);
            shootOne(context, def.getDamage(), new FloatPoint(origin.getX() + jitter, origin.getY()),
                    row, 0f, element, +1, PELLET_VELOCITY);
            shootOne(context, def.getDamage(), new FloatPoint(origin.getX() + jitter, origin.getY()),
                    row, 0f, element, +1, -PELLET_VELOCITY);
            shootOne(context, def.getDamage(), new FloatPoint(origin.getX() + jitter, origin.getY()),
                    row, PELLET_VELOCITY * 1.25f, element, +1, PELLET_VELOCITY);
            shootOne(context, def.getDamage(), new FloatPoint(origin.getX() + jitter, origin.getY()),
                    row, PELLET_VELOCITY * 1.25f, element, +1, -PELLET_VELOCITY);
        }
    }

    private void resetShroomFamilyLifespan(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        String name = def.getName();
        for (PlantInstance other : context.getAllPlants()) {
            if (other.getDefinition().getName().equals(name)) {
                other.setLifespanRemaining(PlantInstance.SHROOM_BASE_LIFESPAN);
            }
        }
        projectileBurstPlantFood(plant, context, +1, 0f, false);
    }

    private void spawnGiantPeas(PlantInstance plant, PlantAbilityContext context,
                                Projectile.Element element, int direction, float yVelocity) {
        FloatPoint origin = context.plantProjectileOriginOrCell(plant);
        int count = plant.getStackCount();
        if (isMegaGatling(plant.getDefinition())) {
            count = 4;
        } else if (!plant.getDefinition().hasTag(PlantTags.STACK)) {
            count = 1;
        }
        for (int i = 0; i < count; i++) {
            float offset = ((i & 1) == 0 ? 1 : -1) * 0.1f * ((i + 1) / 2);
            FloatPoint pelletOrigin = new FloatPoint(origin.getX(), origin.getY() + offset);
            shootOne(context, plant.getDefinition().getDamage() * GIANT_PEA_DAMAGE_MULTIPLIER,
                    pelletOrigin, plant.getPosition().getY(), PELLET_VELOCITY, element, direction, yVelocity);
        }
    }

    // --- Helpers ---

    private boolean hasZombieInAdjacentLane(PlantAbilityContext context, int row, float plantX, int rowOffset) {
        int targetRow = row + rowOffset;
        if (targetRow < 0 || targetRow >= context.getRowCount()) return false;
        for (ZombieInstance zombie : context.getZombiesInLane(targetRow)) {
            if (zombie == null || zombie.isDead() || zombie.isHypnotized()) continue;
            if (zombie.getContinuousPosition() == null) continue;
            float dx = zombie.getContinuousX() - plantX;
            if (Math.abs(dx) <= 1.5f) return true;
        }
        for (PlantInstance ally : context.getPlantsInLane(targetRow)) {
            if (ally == null || !ally.isFrozen() || ally.getPosition() == null) continue;
            float dx = ally.getPosition().getX() - plantX;
            if (Math.abs(dx) <= 1.5f) return true;
        }
        return false;
    }

    private float shroomRangeTiles(PlantInstance plant) {
        return SHROOM_RANGE_TILES + cumulativeSpecialValue(plant, PlantSpecialTag.TILE_RANGE_EXT);
    }

    private float cumulativeSpecialValue(PlantInstance plant, PlantSpecialTag tag) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null) return 0f;
        PlantLevels levels = def.getLevels();
        float total = 0f;
        for (int lvl = 2; lvl <= 4; lvl++) {
            if (lvl > plant.getLevel()) break;
            LevelUpgrade upgrade = levels.getUpgrade(lvl);
            if (upgrade == null) continue;
            if (upgrade.isSpecialMechanic() && upgrade.getSpecialTag() == tag) {
                total += upgrade.getValue();
            }
        }
        return total;
    }

    private Projectile.Element inferElement(Plant def) {
        if (def.hasTag(PlantTags.ICE)) return Projectile.Element.ICE;
        if (def.hasTag(PlantTags.FIRE)) return Projectile.Element.FIRE;
        if (def.hasTag(PlantTags.POISON)) return Projectile.Element.POISON;
        return Projectile.Element.NONE;
    }

    private boolean shootsGiantPea(Plant def) {
        String name = def.getName().toLowerCase();
        return name.contains("repeater") || name.contains("pea pod") || name.contains("mega gatling");
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

    private boolean isCitron(Plant def) {
        return def.getName() != null && def.getName().equalsIgnoreCase("Citron");
    }

    private boolean isSnowPea(Plant def) {
        return def.getName() != null && def.getName().equalsIgnoreCase("Snow Pea");
    }

    private boolean isMegaGatling(Plant def) {
        return def.getName() != null && def.getName().toLowerCase().contains("mega gatling");
    }

    private boolean isShroomShooter(Plant def) {
        return def.isShroom()
                && def.getCategory() == PlantCategory.SHOOTER
                && def.getAbilityType() == PlantAbilityType.SHOOT_PROJECTILE;
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
    }

    private void advanceBowlingBulbCycle(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;
        AbilityState state = plant.getAbilityState(def.getAbilityType());
        if (state == null) return;
        state.setGrowthStage((state.getGrowthStage() + 1) % 3);
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
                    new FloatPoint(originX + i * 0.7f, row),
                    row,
                    BULB_VELOCITY,
                    bulbTypeForCycleIndex(2 - i),
                    BULB_PLANT_FOOD_BOUNCES);
            bulb.setExplosive(true);
            context.spawnProjectile(bulb, bulb.getX(), bulb.getY());
        }
    }

    /** Maps a cycle index (0..2) to its bulb type. */
    public static BowlingBulbType bulbTypeForCycleIndex(int cycleIndex) {
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

    private void shootThreepeater(PlantAbilityContext context, PlantInstance plant,
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

    private void shootSplitPea(PlantAbilityContext context, PlantInstance plant,
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

    private void shootRotobaga(PlantAbilityContext context, PlantInstance plant,
                               FloatPoint origin, Projectile.Element element) {
        int damage = plant.getDefinition().getDamage();
        int row = plant.getPosition().getY();

        FloatPoint firstOrigin = new FloatPoint(origin.getX(), origin.getY() + 0.3f);
        shootOne(context, damage, firstOrigin, row, PELLET_VELOCITY, element, +1, PELLET_VELOCITY);

        FloatPoint secondOrigin = new FloatPoint(origin.getX(), origin.getY() - 0.1f);
        shootOne(context, damage, secondOrigin, row, PELLET_VELOCITY, element, +1, -PELLET_VELOCITY);

        FloatPoint thirdOrigin = new FloatPoint(origin.getX() + 0.1f, origin.getY() + 0.3f);
        shootOne(context, damage, thirdOrigin, row, PELLET_VELOCITY, element, -1, PELLET_VELOCITY);

        FloatPoint fourthOrigin = new FloatPoint(origin.getX(), origin.getY() - 0.1f);
        shootOne(context, damage, fourthOrigin, row, PELLET_VELOCITY, element, -1, -PELLET_VELOCITY);
    }

    private void shootStarfruit(PlantAbilityContext context, PlantInstance plant,
                                FloatPoint origin, Projectile.Element element) {
        int damage = plant.getDefinition().getDamage();
        int row = plant.getPosition().getY();

        shootOne(context, damage, origin, row, PELLET_VELOCITY, element, -1, 0);

        shootOne(context, damage, origin, row, 0, element, +1, PELLET_VELOCITY);

        shootOne(context, damage, origin, row, 0, element, +1, -PELLET_VELOCITY);

        shootOne(context, damage, origin, row, PELLET_VELOCITY, element, +1, PELLET_VELOCITY);

        shootOne(context, damage, origin, row, PELLET_VELOCITY, element, +1, -PELLET_VELOCITY);
    }
}
