package model.game.systems;

import model.enums.PlacableLayer;
import model.enums.PlantCategory;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.event.EventBus;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.plant.ability.PlantAbilityContext;
import model.plant.ability.WallAbility;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;

public class PlantSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;
    private final PlantAbilityContext context;

    public PlantSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
        this.context = new GameModelPlantAbilityContext(gameModel);
    }

    @Override
    public void tick(float deltaTime) {
        List<PlantInstance> snapshot = new ArrayList<>(gameModel.getAllPlants());
        for (PlantInstance plant : snapshot) {
            if (plant.getState() == PlantState.DYING) continue;
            plant.tick(deltaTime, context);
            if (plant.getCurrentHP() <= 0 && plant.getState() != PlantState.DYING) {
                // Explode-o-nut: trigger the death explosion before
                // removing the plant from the field.
                triggerDeathExplosionIfNeeded(plant);
                plant.setState(PlantState.DYING);
                gameModel.destroyPlant(plant);
            }
        }
    }

    /**
     * If the dying plant is a WALL_NUT with the EXPLOSIVE tag
     * (Explode-o-nut), invokes the {@link WallAbility#onPlantDeath}
     * hook to detonate the plant in a 3x3 area.
     */
    private void triggerDeathExplosionIfNeeded(PlantInstance plant) {
        if (plant == null) return;
        Plant def = plant.getDefinition();
        if (def == null) return;
        if (def.getCategory() != PlantCategory.WALL_NUT) return;
        if (!def.hasTag(PlantTags.EXPLOSIVE)) return;

        // Reuse the singleton WallAbility strategy to fire the explosion.
        WallAbility wallAbility = new WallAbility();
        wallAbility.onPlantDeath(plant, context);
    }

    private static class GameModelPlantAbilityContext implements PlantAbilityContext {
        private final GameModel gameModel;

        GameModelPlantAbilityContext(GameModel gameModel) {
            this.gameModel = gameModel;
        }

        @Override public int getSunAmount() { return gameModel.getSunAmount(); }
        @Override public int getRowCount() { return gameModel.getRowCount(); }
        @Override public int getColumnCount() { return gameModel.getColumnCount(); }

        @Override
        public PlantInstance getPlantAt(int row, int col) {
            return gameModel.getPlantAt(row, col);
        }

        @Override
        public List<PlantInstance> getPlantsInLane(int lane) {
            return gameModel.getPlantsInLane(lane);
        }

        @Override
        public List<PlantInstance> getAllPlants() {
            return gameModel.getAllPlants();
        }

        @Override
        public List<ZombieInstance> getZombiesInLane(int lane) {
            return gameModel.getZombiesInLane(lane);
        }

        @Override
        public List<ZombieInstance> getZombiesInArea(int row, int col, int rowRadius, int colRadius) {
            return gameModel.getZombiesInArea(row, col, rowRadius, colRadius);
        }

        @Override
        public boolean hasZombieInLane(int lane) {
            return !gameModel.getZombiesInLane(lane).isEmpty();
        }

        @Override
        public boolean hasAdjacentZombie(int row, int col) {
            for (int rowDist = -1; rowDist <= 1; rowDist++) {
                for (int colDist = -1; colDist <= 1; colDist++) {
                    if (rowDist == 0 && colDist == 0) continue;
                    if (!gameModel.getZombiesInArea(row + rowDist, col + colDist, 0, 0).isEmpty()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean isNightLevel() {
            return gameModel.isNightLevel();
        }

        @Override
        public boolean isWaterTile(int row, int col) {
            return gameModel.isWaterTile(row, col);
        }

        @Override
        public model.projectile.Projectile spawnProjectile(model.projectile.Projectile p, float x, float y) {
            gameModel.spawnProjectile(p, (int) x, (int) y);
            return p;
        }

        @Override
        public void spawnSun(model.item.Sun sun) {
            gameModel.spawnSun(sun);
        }

        @Override
        public void addSun(int amount) {
            gameModel.addSun(amount);
        }

        @Override
        public void damageZombie(ZombieInstance zombie, int damage) {
            gameModel.damageZombie(zombie, damage);
        }

        @Override
        public void damagePlant(PlantInstance plant, int damage) {
            gameModel.damagePlant(plant, damage);
        }

        @Override
        public void destroyPlant(PlantInstance plant) {
            gameModel.destroyPlant(plant);
        }

        @Override
        public boolean placePlant(PlantInstance plant, int row, int col) {
            return gameModel.placePlant(plant, row, col);
        }

        @Override
        public boolean moveZombieToLane(ZombieInstance zombie, int newRow) {
            return gameModel.moveZombieToLane(zombie, newRow);
        }

        @Override
        public void pushZombieBack(ZombieInstance zombie, float tiles) {
            gameModel.pushZombieBack(zombie, tiles);
        }

        @Override
        public void triggerFamilyPlantFood(model.enums.PlantCategory family) {
            boolean resetCooldowns = false;
            // Detect if any plant of this family has the RESET_FAMILY_COOLDOWNS upgrade.
            // If so, after triggering plant-food on every family member, also clear their
            // per-ability cooldowns so they can immediately act.
            for (PlantInstance plant : new ArrayList<>(gameModel.getAllPlants())) {
                if (plant.getDefinition().getCategory() == family) {
                    if (hasResetFamilyCooldownsUpgrade(plant)) {
                        resetCooldowns = true;
                    }
                }
            }

            for (PlantInstance plant : new ArrayList<>(gameModel.getAllPlants())) {
                if (plant.getDefinition().getCategory() == family) {
                    plant.activatePlantFood(this);
                    if (resetCooldowns) {
                        // Clear every ability cooldown on this plant.
                        for (AbilityState state : plant.getAbilityStates().values()) {
                            state.setCooldownRemaining(0f);
                        }
                        plant.setCurrentRecharge(0f);
                    }
                }
            }
        }

        /** @return true if the plant has the RESET_FAMILY_COOLDOWNS upgrade. */
        private boolean hasResetFamilyCooldownsUpgrade(PlantInstance plant) {
            Plant def = plant.getDefinition();
            if (def == null || def.getLevels() == null) return false;
            for (int lvl = 2; lvl <= 4; lvl++) {
                if (lvl > plant.getLevel()) break;
                LevelUpgrade upgrade = def.getLevels().getUpgrade(lvl);
                if (upgrade == null) continue;
                if (upgrade.isSpecialMechanic()
                        && upgrade.getSpecialTag() == model.enums.PlantSpecialTag.RESET_FAMILY_COOLDOWNS) {
                    return true;
                }
            }
            return false;
        }
    }
}