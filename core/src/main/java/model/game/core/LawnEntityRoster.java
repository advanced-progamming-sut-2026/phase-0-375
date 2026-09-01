package model.game.core;

import model.event.GameEvent;
import model.game.map.GameMap;
import model.game.map.Point;
import model.item.LootDrop;
import model.item.LootPickup;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.item.pushable.Pushable;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Live entity lists for one lawn (zombies, shots, pickups, drops). */
final class LawnEntityRoster {

    final List<ZombieInstance> zombies = new ArrayList<>();
    final List<Projectile> projectiles = new ArrayList<>();
    final List<Sun> suns = new ArrayList<>();
    final List<PlantFoodPickup> plantFood = new ArrayList<>();
    final List<LootPickup> lootPickups = new ArrayList<>();
    final List<LootDrop> pendingLootDrops = new ArrayList<>();
    final List<Pushable> orphanedPushables = new ArrayList<>();

    void clearBoard(GameMap gameMap) {
        zombies.clear();
        projectiles.clear();
        suns.clear();
        plantFood.clear();
        lootPickups.clear();
        pendingLootDrops.clear();
        orphanedPushables.clear();
        if (gameMap == null) {
            return;
        }
        for (int row = 0; row < gameMap.getRows(); row++) {
            for (int col = 0; col < gameMap.getCols(); col++) {
                gameMap.getCell(col, row).clearDynamics();
            }
        }
    }

    boolean restorePlant(GameMap gameMap, PlantInstance plant, int row, int col) {
        if (plant == null || gameMap == null || row < 0 || col < 0
                || row >= gameMap.getRows() || col >= gameMap.getCols()) {
            return false;
        }
        plant.setPosition(new Point(col, row));
        return gameMap.getCell(col, row).addPlaceable(plant);
    }

    void restoreZombie(GameMap gameMap, ZombieInstance instance) {
        if (instance == null) {
            return;
        }
        zombies.add(instance);
        Point grid = instance.getGridPosition();
        if (grid == null || gameMap == null) {
            return;
        }
        int col = Math.max(0, Math.min(grid.getX(), gameMap.getCols() - 1));
        int row = Math.max(0, Math.min(grid.getY(), gameMap.getRows() - 1));
        instance.setGridPosition(new Point(col, row));
        gameMap.addZombie(instance, col, row);
    }

    void replaceSuns(List<Sun> next) {
        suns.clear();
        if (next != null) {
            suns.addAll(next);
        }
    }

    void orphanPushable(Pushable pushable) {
        if (pushable == null || pushable.isDestroyed()) {
            return;
        }
        if (!orphanedPushables.contains(pushable)) {
            orphanedPushables.add(pushable);
        }
    }

    void queueLootDrop(LootDrop loot, GameModel model) {
        if (loot == null) {
            return;
        }
        pendingLootDrops.add(loot);
        if (model.getEventBus() != null) {
            model.getEventBus().dispatch(new GameEvent(GameEvent.Type.LOOT_DROPPED));
        }
    }

    void processLootDrops(GameModel model) {
        if (pendingLootDrops.isEmpty()) {
            return;
        }
        Iterator<LootDrop> iterator = pendingLootDrops.iterator();
        while (iterator.hasNext()) {
            LootDrop drop = iterator.next();
            if (drop != null) {
                drop.apply(model);
            }
            iterator.remove();
        }
    }
}
