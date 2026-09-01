package model.game.core;

import model.app.App;
import model.enums.GroundType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.enums.PlacableLayer;
import model.event.GameEvent;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.GameMap;
import model.game.map.Point;
import model.game.map.terrain.CraterTerrainStrategy;
import model.game.map.terrain.FireTerrainStrategy;
import model.game.map.terrain.IceTerrainStrategy;
import model.item.Grave;
import model.item.Grave.GraveType;
import model.item.placeable.Placeable;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.zombie.ZombieFactory;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Plant / grave / terrain / query operations extracted from {@link GameModel}. */
final class GameBoardCombat {

    private final GameModel model;
    private final LawnEntityRoster roster;

    GameBoardCombat(GameModel model, LawnEntityRoster roster) {
        this.model = model;
        this.roster = roster;
    }

    boolean placePlant(PlantInstance plant, int row, int col) {
        GameMap gameMap = model.getGameMap();
        if (plant == null || gameMap == null) {
            return false;
        }
        if (row < 0 || col < 0 || row >= gameMap.getRows() || col >= gameMap.getCols()) {
            return false;
        }
        Cell cell = gameMap.getCell(col, row);
        PlacableLayer targetLayer = plant.getLayer();
        if (cell.getPlaceable(targetLayer) != null) {
            return false;
        }
        if (targetLayer == PlacableLayer.MAIN) {
            var terrain = cell.getTerrainStrategy();
            if (terrain != null && !terrain.canPlant(plant.getDefinition(), cell)) {
                return false;
            }
        }
        plant.setPosition(new Point(col, row));
        boolean added = cell.addPlaceable(plant);
        if (added) {
            model.questStats().onPlantPlaced(model, plant.getDefinition(), row, col);
        }
        if (added && model.getEventBus() != null) {
            model.getEventBus().dispatch(new GameEvent(GameEvent.Type.PLANT_PLACED));
        }
        return added;
    }

    PlantInstance getPlantAt(int row, int col) {
        Cell cell = model.getCellAt(row, col);
        return cell == null ? null : cell.getTopmostPlant();
    }

    List<PlantInstance> getAllPlantsAt(int row, int col) {
        Cell cell = model.getCellAt(row, col);
        return cell == null ? Collections.emptyList() : cell.getAllPlants();
    }

    List<PlantInstance> getPlantsInLane(int lane) {
        GameMap gameMap = model.getGameMap();
        if (gameMap == null || lane < 0 || lane >= gameMap.getRows()) {
            return Collections.emptyList();
        }
        List<PlantInstance> plants = new ArrayList<>();
        for (int col = 0; col < gameMap.getCols(); col++) {
            plants.addAll(gameMap.getCell(col, lane).getAllPlants());
        }
        return plants;
    }

    List<PlantInstance> getAllPlants() {
        GameMap gameMap = model.getGameMap();
        if (gameMap == null) {
            return Collections.emptyList();
        }
        List<PlantInstance> plants = new ArrayList<>();
        for (int row = 0; row < gameMap.getRows(); row++) {
            for (int col = 0; col < gameMap.getCols(); col++) {
                plants.addAll(gameMap.getCell(col, row).getAllPlants());
            }
        }
        return plants;
    }

    void damagePlant(PlantInstance plant, int damage) {
        if (plant == null || damage <= 0) {
            return;
        }
        boolean wasAlive = plant.getCurrentHP() > 0;
        plant.takeDamage(damage);
        if (wasAlive && plant.getCurrentHP() == 0) {
            model.breach().incrementPlantsLost();
            hypnotiseHypnoShroomEaters(plant);
        }
    }

    boolean movePlant(PlantInstance plant, int row, int col) {
        GameMap gameMap = model.getGameMap();
        if (plant == null || gameMap == null
                || row < 0 || col < 0 || row >= gameMap.getRows() || col >= gameMap.getCols()) {
            return false;
        }
        Point currentPos = plant.getPosition();
        if (currentPos == null) {
            return false;
        }
        Cell destinationCell = gameMap.getCell(col, row);
        if (destinationCell.getPlaceable(plant.getLayer()) != null) {
            return false;
        }
        Cell sourceCell = gameMap.getCell(currentPos.getX(), currentPos.getY());
        sourceCell.removePlaceable(plant);
        destinationCell.addPlaceable(plant);
        plant.setPosition(new Point(col, row));
        model.questStats().markRowColumnPlanted(row, col);
        return true;
    }

    void destroyPlant(PlantInstance plant) {
        if (plant == null) {
            return;
        }
        if (plant.getCurrentHP() > 0) {
            model.breach().incrementPlantsLost();
        }
        hypnotiseHypnoShroomEaters(plant);
        plant.setCurrentHP(0);
        model.getEventBus().dispatch(new GameEvent(GameEvent.Type.PLANT_DESTROYED));
    }

    private void hypnotiseHypnoShroomEaters(PlantInstance plant) {
        if (plant == null || !plant.isHypnoShroom()) {
            return;
        }
        Point pos = plant.getPosition();
        for (ZombieInstance zombie : roster.zombies) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }
            if (zombie.getEatingTarget() == plant) {
                zombie.hypnotise();
                continue;
            }
            if (pos != null
                    && zombie.getGridY() == pos.getY()
                    && zombie.getGridX() == pos.getX()
                    && zombie.isEating()) {
                zombie.hypnotise();
            }
        }
    }

    List<ZombieInstance> getZombiesInLane(int lane) {
        GameMap gameMap = model.getGameMap();
        if (gameMap == null || lane < 0 || lane >= gameMap.getRows()) {
            return Collections.emptyList();
        }
        List<ZombieInstance> zombies = new ArrayList<>();
        for (ZombieInstance zombie : roster.zombies) {
            if (zombie != null && !zombie.isDead() && zombie.occupiesLane(lane)) {
                zombies.add(zombie);
            }
        }
        return zombies;
    }

    List<ZombieInstance> getZombiesInArea(int centerRow, int centerCol, int rowRadius, int colRadius) {
        List<ZombieInstance> zombies = new ArrayList<>();
        for (ZombieInstance zombie : roster.zombies) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }
            Point pos = zombie.getGridPosition();
            if (pos == null) {
                continue;
            }
            if (Math.abs(pos.getX() - centerCol) > colRadius) {
                continue;
            }
            for (int occupied : zombie.getOccupiedRows()) {
                if (Math.abs(occupied - centerRow) <= rowRadius) {
                    zombies.add(zombie);
                    break;
                }
            }
        }
        return zombies;
    }

    void damageZombie(ZombieInstance zombie, int damage, Plant source) {
        if (zombie == null || damage <= 0) {
            return;
        }
        if (source != null) {
            zombie.recordPlantDamage(source);
        } else {
            zombie.recordNonPlantDamage();
        }
        zombie.takeDamage(damage);
    }

    boolean moveZombieToLane(ZombieInstance zombie, int newRow) {
        GameMap gameMap = model.getGameMap();
        if (zombie == null || gameMap == null) {
            return false;
        }
        if (newRow < 0 || newRow >= gameMap.getRows()) {
            return false;
        }
        Point pos = zombie.getGridPosition();
        if (pos == null) {
            return false;
        }
        int oldRow = pos.getY();
        int col = pos.getX();
        if (oldRow == newRow) {
            return true;
        }
        Cell oldCell = gameMap.getCell(col, oldRow);
        if (oldCell != null) {
            oldCell.removeZombie(zombie);
        }
        Cell newCell = gameMap.getCell(col, newRow);
        if (newCell != null) {
            newCell.addZombie(zombie);
        }
        zombie.setGridPosition(new Point(col, newRow));
        zombie.setContinuousPosition(new FloatPoint(zombie.getContinuousX(), newRow));
        return true;
    }

    void pushZombieBack(ZombieInstance zombie, float tiles) {
        GameMap gameMap = model.getGameMap();
        if (zombie == null || zombie.isDead() || tiles <= 0 || gameMap == null) {
            return;
        }
        FloatPoint pos = zombie.getContinuousPosition();
        if (pos == null) {
            return;
        }
        float newX = pos.getX() + tiles;
        if (newX >= gameMap.getCols()) {
            zombie.setCurrentHP(0);
            zombie.setState(ZombieState.DEAD);
            return;
        }
        zombie.setContinuousX(newX);
        int newGridX = (int) Math.floor(newX);
        Point gridPos = zombie.getGridPosition();
        if (gridPos == null || newGridX == gridPos.getX()) {
            return;
        }
        int row = gridPos.getY();
        Cell oldCell = gameMap.getCell(gridPos.getX(), row);
        if (oldCell != null) {
            oldCell.removeZombie(zombie);
        }
        if (newGridX >= 0 && newGridX < gameMap.getCols()) {
            Cell newCell = gameMap.getCell(newGridX, row);
            if (newCell != null) {
                newCell.addZombie(zombie);
            }
        }
        zombie.setGridX(newGridX);
    }

    List<Projectile> getProjectilesInLane(int lane) {
        GameMap gameMap = model.getGameMap();
        if (gameMap == null || lane < 0 || lane >= gameMap.getRows()) {
            return Collections.emptyList();
        }
        List<Projectile> inLane = new ArrayList<>();
        for (Projectile projectile : roster.projectiles) {
            if (projectile != null && projectile.getRow() == lane) {
                inLane.add(projectile);
            }
        }
        return inLane;
    }

    boolean spawnGraveAt(int row, int col, GraveType type, ZombieInstance raiser) {
        Cell cell = model.getCellAt(row, col);
        if (cell == null || cell.getPlaceable(PlacableLayer.GROUND) != null
                || !cell.getAllPlants().isEmpty()) {
            return false;
        }
        Grave grave = new Grave(Grave.DEFAULT_HP, type);
        grave.setRaiser(raiser);
        boolean placed = cell.addPlaceable(grave);
        if (placed) {
            model.getEventBus().dispatch(new GameEvent(GameEvent.Type.GRAVE_SPAWNED));
            String kind = type == GraveType.PLAIN ? "plain"
                    : type == GraveType.SUN ? "sun" : "plant-food";
            App.logToShell("[Grave] A " + kind + " grave surfaced at ("
                    + col + ", " + row + ").");
        }
        return placed;
    }

    int countGravesRaisedBy(ZombieInstance raiser) {
        GameMap gameMap = model.getGameMap();
        if (raiser == null || gameMap == null) {
            return 0;
        }
        int n = 0;
        for (int row = 0; row < gameMap.getRows(); row++) {
            for (int col = 0; col < gameMap.getCols(); col++) {
                Grave grave = getGraveAt(row, col);
                if (grave != null && grave.getRaiser() == raiser && !grave.isDestroyed()) {
                    n++;
                }
            }
        }
        return n;
    }

    Grave getGraveAt(int row, int col) {
        Cell cell = model.getCellAt(row, col);
        if (cell == null) {
            return null;
        }
        Placeable p = cell.getPlaceable(PlacableLayer.GROUND);
        return (p instanceof Grave) ? (Grave) p : null;
    }

    boolean removeGraveAt(int row, int col) {
        Grave grave = getGraveAt(row, col);
        if (grave == null) {
            return false;
        }
        Cell cell = model.getCellAt(row, col);
        cell.removePlaceable(grave);
        return true;
    }

    void createCraterAt(int row, int col) {
        Cell cell = model.getCellAt(row, col);
        if (cell == null) {
            return;
        }
        cell.setGroundType(GroundType.CRATER);
        cell.setTerrainStrategy(new CraterTerrainStrategy());
    }

    void igniteTile(int row, int col, float durationSeconds) {
        Cell cell = model.getCellAt(row, col);
        if (cell == null) {
            return;
        }
        cell.setGroundType(GroundType.FIRE);
        cell.setTerrainStrategy(new FireTerrainStrategy(durationSeconds));
    }

    void plantFrozenZombieAt(int row, int col, String zombieDefinitionName) {
        Cell cell = model.getCellAt(row, col);
        if (cell == null || cell.getTerrainStrategy() instanceof IceTerrainStrategy) {
            return;
        }
        PlantInstance plant = getPlantAt(row, col);
        if (plant != null) {
            destroyPlant(plant);
        }
        String name = zombieDefinitionName == null || zombieDefinitionName.isBlank()
                ? "ZombieDefault" : zombieDefinitionName;
        ZombieInstance frozen = ZombieFactory.createInstance(name);
        if (frozen != null) {
            frozen.setGridPosition(new Point(col, row));
            frozen.setContinuousPosition(new FloatPoint(col, row));
        }
        cell.setGroundType(GroundType.ICE);
        cell.setTerrainStrategy(new IceTerrainStrategy(frozen));
    }

    ZombieInstance findZomboss() {
        for (ZombieInstance zombie : roster.zombies) {
            if (zombie != null && !zombie.isDead()
                    && zombie.hasBehavior(ZombieBehaviorType.ZOMBOSS)) {
                return zombie;
            }
        }
        return null;
    }

    void damageIceAt(int row, int col, int damage) {
        if (damage <= 0) {
            return;
        }
        Cell cell = model.getCellAt(row, col);
        if (cell != null && cell.getTerrainStrategy() instanceof IceTerrainStrategy ice) {
            ice.takeDamage(damage);
        }
    }

    void damageIceInArea(int row, int col, int rowRadius, int colRadius, int damage) {
        if (damage <= 0) {
            return;
        }
        for (int rowDist = -rowRadius; rowDist <= rowRadius; rowDist++) {
            for (int colDist = -colRadius; colDist <= colRadius; colDist++) {
                damageIceAt(row + rowDist, col + colDist, damage);
            }
        }
    }

    void addExistingZombie(ZombieInstance zombie, int row, int col) {
        GameMap gameMap = model.getGameMap();
        if (zombie == null || gameMap == null) {
            return;
        }
        int clampedRow = Math.max(0, Math.min(row, gameMap.getRows() - 1));
        int clampedCol = Math.max(0, Math.min(col, gameMap.getCols() - 1));
        zombie.setGridPosition(new Point(clampedCol, clampedRow));
        if (zombie.getContinuousPosition() == null) {
            zombie.setContinuousPosition(new FloatPoint(clampedCol, clampedRow));
        }
        if (!roster.zombies.contains(zombie)) {
            roster.zombies.add(zombie);
        }
        Cell cell = gameMap.getCell(clampedCol, clampedRow);
        if (cell != null && !cell.getZombies().contains(zombie)) {
            cell.addZombie(zombie);
        }
    }

    void removePlantFromBoard(PlantInstance plant) {
        GameMap gameMap = model.getGameMap();
        if (plant == null || plant.getPosition() == null || gameMap == null) {
            return;
        }
        Cell cell = gameMap.getCell(plant.getPosition().getX(), plant.getPosition().getY());
        if (cell != null) {
            cell.removePlaceable(plant);
        }
    }

    void syncZombieWorldPose(ZombieInstance zombie, int row, float continuousX, float continuousY) {
        GameMap gameMap = model.getGameMap();
        if (zombie == null || gameMap == null) {
            return;
        }
        int clampedRow = Math.max(0, Math.min(row, gameMap.getRows() - 1));
        float y = Float.isNaN(continuousY) ? clampedRow : continuousY;
        zombie.setContinuousPosition(new FloatPoint(continuousX, y));
        int newCol = (int) Math.floor(continuousX);
        Point grid = zombie.getGridPosition();
        int oldCol = grid != null ? grid.getX() : newCol;
        int oldRow = grid != null ? grid.getY() : clampedRow;
        if (oldCol != newCol || oldRow != clampedRow) {
            remapZombieCell(gameMap, zombie, oldCol, oldRow, newCol, clampedRow);
            zombie.setGridPosition(new Point(newCol, clampedRow));
        } else if (grid == null) {
            zombie.setGridPosition(new Point(newCol, clampedRow));
        }
    }

    private static void remapZombieCell(GameMap gameMap, ZombieInstance zombie,
                                        int oldCol, int oldRow, int newCol, int newRow) {
        if (inMapBounds(gameMap, oldCol, oldRow)) {
            Cell oldCell = gameMap.getCell(oldCol, oldRow);
            if (oldCell != null) {
                oldCell.removeZombie(zombie);
            }
        }
        if (inMapBounds(gameMap, newCol, newRow)) {
            Cell newCell = gameMap.getCell(newCol, newRow);
            if (newCell != null) {
                newCell.addZombie(zombie);
            }
        }
    }

    private static boolean inMapBounds(GameMap gameMap, int col, int row) {
        return col >= 0 && row >= 0 && col < gameMap.getCols() && row < gameMap.getRows();
    }
}
