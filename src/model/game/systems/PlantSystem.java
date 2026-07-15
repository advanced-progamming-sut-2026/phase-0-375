package model.game.systems;

import model.enums.PlacableLayer;
import model.enums.PlantState;
import model.event.EventBus;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.plant.ability.PlantAbilityContext;
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
                plant.setState(PlantState.DYING);
                gameModel.destroyPlant(plant);
            }
        }
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
            for (PlantInstance plant : new ArrayList<>(gameModel.getAllPlants())) {
                if (plant.getDefinition().getCategory() == family) {
                    plant.activatePlantFood(this);
                }
            }
        }
    }
}