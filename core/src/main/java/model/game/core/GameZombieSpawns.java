package model.game.core;

import model.app.App;
import model.enums.ZombieState;
import model.event.GameEvent;
import model.game.map.FloatPoint;
import model.game.map.GameMap;
import model.game.map.Point;
import model.news.NewsFactory;
import model.user.User;
import model.zombie.ZombieFactory;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

import java.util.HashSet;
import java.util.Set;

/** Zombie spawn / sighting helpers extracted from {@link GameModel}. */
final class GameZombieSpawns {

    private final GameModel model;
    private final LawnEntityRoster roster;

    GameZombieSpawns(GameModel model, LawnEntityRoster roster) {
        this.model = model;
        this.roster = roster;
    }

    void recordZombieSeen(String zombieName) {
        model.questStats().onZombieSpawned(model.getElapsedSeconds());
        User user = App.getInstance().getCurrentUser();
        if (user == null || zombieName == null) {
            return;
        }
        Set<String> seen = user.getUnlockedZombies();
        if (seen == null) {
            seen = new HashSet<>();
            user.setUnlockedZombies(seen);
        }
        if (seen.add(zombieName)) {
            user.rememberNewsPublishDate(NewsFactory.zombieNewsId(zombieName));
            if (App.getInstance().getUserRepository() != null) {
                App.getInstance().getUserRepository().unlockZombie(user.getUsername(), zombieName);
            }
        }
    }

    void spawnZombie(Zombie zombie, int lane) {
        ZombieInstance instance = prepareSpawn(zombie.getName(), zombie);
        GameMap gameMap = model.getGameMap();
        instance.setContinuousPosition(new FloatPoint(gameMap.getCols(), lane));
        instance.setGridPosition(new Point(gameMap.getCols(), lane));
        roster.zombies.add(instance);
        gameMap.addZombie(instance, gameMap.getCols(), lane);
        afterSpawn(instance);
    }

    ZombieInstance spawnZombieWithTornado(Zombie zombie, int lane, int columnsAhead) {
        ZombieInstance instance = prepareSpawn(zombie.getName(), zombie);
        GameMap gameMap = model.getGameMap();
        int col = GameModel.tornadoColumn(gameMap.getCols(), columnsAhead);
        instance.setContinuousPosition(new FloatPoint(col, lane));
        instance.setGridPosition(new Point(col, lane));
        roster.zombies.add(instance);
        gameMap.addZombie(instance, col, lane);
        afterSpawn(instance);
        App.logToShell("[Sandstorm] A " + zombie.getName()
                + " is carried in by a sandstorm and lands " + columnsAhead
                + " column(s) ahead in lane " + (lane + 1) + "!");
        return instance;
    }

    ZombieInstance spawnZombieAt(String zombieDefinitionName, int row, int col) {
        ZombieInstance instance = ZombieFactory.createInstance(zombieDefinitionName);
        if (instance == null) {
            return null;
        }
        scaleHp(instance);
        String seenName = instance.getDefinition() != null
                ? instance.getDefinition().getName() : zombieDefinitionName;
        recordZombieSeen(seenName);
        GameMap gameMap = model.getGameMap();
        int clampedRow = Math.max(0, Math.min(row, gameMap.getRows() - 1));
        int clampedCol = Math.max(0, Math.min(col, gameMap.getCols() - 1));
        instance.setGridPosition(new Point(clampedCol, clampedRow));
        instance.setContinuousPosition(new FloatPoint(clampedCol, clampedRow));
        instance.setState(ZombieState.SPAWNING);
        roster.zombies.add(instance);
        gameMap.addZombie(instance, clampedCol, clampedRow);
        if (model.getMyopointTracker() != null) {
            model.getMyopointTracker().onZombieSpawned(instance, model.getElapsedSeconds());
        }
        model.getEventBus().dispatch(new GameEvent(GameEvent.Type.ZOMBIE_SPAWNED));
        return instance;
    }

    void removeZombie(ZombieInstance zombie) {
        roster.zombies.remove(zombie);
        model.getGameMap().removeZombie(zombie);
        if (model.getWaveManager() != null) {
            model.getWaveManager().onZombieRemoved(zombie);
        }
    }

    private ZombieInstance prepareSpawn(String seenName, Zombie zombie) {
        ZombieInstance instance = ZombieFactory.createInstance(zombie);
        scaleHp(instance);
        recordZombieSeen(seenName);
        return instance;
    }

    private void scaleHp(ZombieInstance instance) {
        instance.setCurrentHP(Math.max(1, (int) (instance.getCurrentHP() * model.difficultyBoost())));
    }

    private void afterSpawn(ZombieInstance instance) {
        if (model.getMyopointTracker() != null) {
            model.getMyopointTracker().onZombieSpawned(instance, model.getElapsedSeconds());
        }
        if (model.getWaveManager() != null) {
            model.getWaveManager().onWaveZombieSpawned(instance);
        }
        model.getEventBus().dispatch(new GameEvent(GameEvent.Type.ZOMBIE_SPAWNED));
    }
}
