package model.game.save;

import model.app.App;
import model.data.level.LevelRegistry;
import model.data.minigame.MiniGameRegistry;
import model.enums.ArmorType;
import model.enums.GameState;
import model.enums.GroundType;
import model.enums.MenuType;
import model.enums.PlacableLayer;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.Level;
import model.game.level.minigame.MiniGameLevel;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.GameMap;
import model.game.map.Lane;
import model.game.map.LawnMower;
import model.game.map.Point;
import model.game.map.terrain.CraterTerrainStrategy;
import model.game.map.terrain.FireTerrainStrategy;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.score.ScoreLevelGenerator;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.game.wave.WaveManager;
import model.item.Grave;
import model.item.LootPickup;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.item.placeable.Placeable;
import model.plant.PlantFactory;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.zombie.ZombieFactory;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rebuilds a live session from {@link GameSaveData}.
 */
final class GameSaveRestore {

    private GameSaveRestore() {
    }

    static void restoreIntoApp(GameSaveData data) throws IOException {
        Level level = createLevel(data);
        GameModel model = new GameModel(level);
        App.getInstance().setCurrentGameModel(model);
        App.getInstance().setCurrentGameLoop(null);

        level.onStart();
        apply(data, model);

        PvZGameLoop loop = new PvZGameLoop(model);
        restoreSunFall(data, loop);
        loop.resume();
        model.setGameState(GameState.RUNNING);
        App.getInstance().setCurrentGameLoop(loop);
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);
    }

    private static void restoreSunFall(GameSaveData data, PvZGameLoop loop) {
        if (loop.getSunFallSystem() != null) {
            loop.getSunFallSystem().restoreTimers(
                    data.getSunFallElapsed(),
                    data.getSunFallDropTimer(),
                    data.isSkyDropEnabled());
        }
    }

    private static Level createLevel(GameSaveData data) throws IOException {
        return switch (data.getMode()) {
            case MINI_GAME -> createMiniGameLevel(data);
            case SCORE -> ScoreLevelGenerator.createDailyLevel();
            default -> createAdventureLevel(data);
        };
    }

    private static Level createMiniGameLevel(GameSaveData data) throws IOException {
        MiniGameRegistry registry;
        try {
            registry = MiniGameRegistry.getInstance();
        } catch (IllegalStateException e) {
            MiniGameRegistry.init("/assets/data/minigames/minigames.json");
            registry = MiniGameRegistry.getInstance();
        }
        MiniGameLevel level = registry.createMiniGame(data.getMiniGameType(), data.getMiniGameStage());
        if (level == null) {
            throw new IOException("Could not rebuild mini-game from save.");
        }
        return level;
    }

    private static Level createAdventureLevel(GameSaveData data) throws IOException {
        LevelRegistry registry;
        try {
            registry = LevelRegistry.getInstance();
        } catch (IllegalStateException e) {
            LevelRegistry.init("/assets/data/levels/levels.json");
            registry = LevelRegistry.getInstance();
        }
        Level level = registry.createLevel(data.getChapter(), data.getLevelId());
        if (level == null) {
            throw new IOException("Could not rebuild level from save.");
        }
        return level;
    }

    private static void apply(GameSaveData data, GameModel model) {
        model.clearBoardForRestore();
        model.restoreResources(data.getSunAmount(), data.getPlantFoodCount(), data.getPersistentPlantFood());
        model.restoreSeedCooldowns(data.getSeedCooldowns(), data.isSeedCooldownsDisabled());
        applyProgress(data, model);
        model.setSelectedPlants(data.getSelectedPlants() == null
                ? new ArrayList<>()
                : new ArrayList<>(data.getSelectedPlants()));
        if (data.getImitaterCopyTarget() != null) {
            model.setImitaterCopyTarget(data.getImitaterCopyTarget());
        }
        model.setGameState(GameState.PAUSED);
        applyTide(data, model);
        applyEntities(data, model);
    }

    private static void applyProgress(GameSaveData data, GameModel model) {
        model.restoreProgress(
                data.getCurrentTick(),
                data.getElapsedSeconds(),
                data.getDifficultyLevel(),
                data.isHouseBreached(),
                data.getBreachedRows(),
                data.getZombiesKilled(),
                data.getPlantsLost(),
                data.getDiamondCount(),
                data.getCoinCount(),
                data.getFlowerPotCount());
    }

    private static void applyTide(GameSaveData data, GameModel model) {
        if (model.getTideState() != null && model.getTideState().isActive()) {
            model.getTideState().setDynamicColumns(data.getTideDynamicColumns());
            model.getTideState().applyToMap(model.getMap());
        }
    }

    private static void applyEntities(GameSaveData data, GameModel model) {
        applyTerrains(data, model);
        applyGraves(data, model);
        applyPlants(data, model);
        List<ZombieInstance> waveLiving = applyZombies(data, model);
        applySuns(data, model);
        applyPlantFood(data, model);
        applyLoot(data, model);
        applyMowers(data, model);
        applyWaveManager(data, model, waveLiving);
    }

    private static void applyWaveManager(GameSaveData data, GameModel model,
                                         List<ZombieInstance> waveLiving) {
        WaveManager wm = model.getWaveManager();
        WaveManagerSave save = data.getWaveManager();
        if (wm == null || save == null) {
            return;
        }
        applyWaves(wm.getWaves(), save.getWaves());
        wm.restoreFromSave(
                save.getCurrentWaveIndex(),
                save.getPhase(),
                save.getInterWaveTimer(),
                save.getCurrentWaveTotal(),
                save.getCurrentWaveKilled(),
                save.getMaxReportedProgress(),
                waveLiving);
    }

    private static void applyWaves(List<Wave> waves, List<WaveSave> savedWaves) {
        int n = Math.min(waves.size(), savedWaves == null ? 0 : savedWaves.size());
        for (int i = 0; i < n; i++) {
            Wave wave = waves.get(i);
            WaveSave ws = savedWaves.get(i);
            wave.setState(ws.getState());
            wave.setWaveClock(ws.getWaveClock());
            applyEntries(wave.getRuntimeEntries(), ws.getEntries());
        }
    }

    private static void applyEntries(List<EntryRuntime> runtimes, List<EntryRuntimeSave> entries) {
        int m = Math.min(runtimes.size(), entries == null ? 0 : entries.size());
        for (int j = 0; j < m; j++) {
            EntryRuntime rt = runtimes.get(j);
            EntryRuntimeSave es = entries.get(j);
            rt.setActivated(es.isActivated());
            rt.setFirstSpawnAt(es.getFirstSpawnAt());
            rt.setRemainingSpawns(es.getRemainingSpawns());
            rt.setNextSpawnAt(es.getNextSpawnAt());
            rt.setExhausted(es.isExhausted());
            rt.setGroupVolleyFired(es.isGroupVolleyFired());
        }
    }

    private static void applyPlants(GameSaveData data, GameModel model) {
        ensurePlantFactory();
        for (PlantSave ps : data.getPlants()) {
            if (ps == null || ps.getDefinitionName() == null) {
                continue;
            }
            PlantInstance plant = restorePlant(ps);
            if (plant != null) {
                model.restorePlant(plant, ps.getRow(), ps.getCol());
            }
        }
    }

    private static PlantInstance restorePlant(PlantSave ps) {
        PlantInstance plant = PlantFactory.createInstance(ps.getDefinitionName(), Math.max(1, ps.getLevel()));
        if (plant == null) {
            return null;
        }
        plant.setState(ps.getState());
        plant.setCurrentHP(ps.getCurrentHp());
        plant.setArmorHp(ps.getArmorHp(), ps.getArmorMaxHp());
        plant.setCurrentRecharge(ps.getCurrentRecharge());
        plant.setPlantFoodActive(ps.isPlantFoodActive(), ps.getPlantFoodDurationRemaining());
        plant.setLifespanRemaining(ps.getLifespanRemaining());
        plant.setLifespanTotal(ps.getLifespanTotal());
        plant.setStackCount(ps.getStackCount());
        if (ps.getImitateTarget() != null) {
            plant.setImitateTarget(ps.getImitateTarget());
        }
        plant.setTransformCountdown(ps.getTransformCountdown());
        plant.setIceHp(ps.getIceHp());
        plant.setOctopusCoating(ps.isOctopusCoating());
        plant.setFreezeHitCount(ps.getFreezeHitCount());
        applyAbilities(plant, ps);
        return plant;
    }

    private static void applyAbilities(PlantInstance plant, PlantSave ps) {
        for (AbilitySave as : ps.getAbilities()) {
            if (as == null || as.getAbilityType() == null) {
                continue;
            }
            AbilityState state = plant.getAbilityState(as.getAbilityType());
            if (state == null) {
                state = new AbilityState(as.getAbilityType());
                plant.getAbilityStates().put(as.getAbilityType(), state);
            }
            state.setCooldownRemaining(as.getCooldownRemaining());
            state.setChargeProgress(as.getChargeProgress());
            state.setActive(as.isActive());
            state.restoreArmed(as.isArmed(), as.getArmedElapsed());
            state.setGrowthStage(as.getGrowthStage());
            state.setDigesting(as.isDigesting());
            state.setDigestRemaining(as.getDigestRemaining());
            state.setShotOrdinal(as.getShotOrdinal());
        }
    }

    private static List<ZombieInstance> applyZombies(GameSaveData data, GameModel model) {
        ensureZombieFactory();
        List<ZombieInstance> waveLiving = new ArrayList<>();
        for (ZombieSave zs : data.getZombies()) {
            if (zs == null || zs.getDefinitionName() == null) {
                continue;
            }
            ZombieInstance zombie = restoreZombie(zs);
            if (zombie == null) {
                continue;
            }
            model.restoreZombie(zombie);
            if (zs.isCountsTowardCurrentWave()) {
                waveLiving.add(zombie);
            }
        }
        return waveLiving;
    }

    private static ZombieInstance restoreZombie(ZombieSave zs) {
        ZombieInstance zombie = ZombieFactory.createInstance(zs.getDefinitionName());
        if (zombie == null) {
            return null;
        }
        zombie.setGridPosition(new Point(zs.getGridCol(), zs.getGridRow()));
        zombie.setContinuousPosition(new FloatPoint(zs.getContinuousX(), zs.getContinuousY()));
        zombie.setState(zs.getState());
        zombie.setCurrentHP(zs.getCurrentHp());
        zombie.restoreStatus(
                zs.isGlowing(),
                zs.getChillLevel(),
                zs.getChillStackTimer(),
                zs.isButtered(),
                zs.isHypnotized(),
                zs.isMovingBackward(),
                zs.getCurrentSpeed(),
                zs.getSpeedModifier());
        applyArmors(zombie, zs);
        return zombie;
    }

    private static void applyArmors(ZombieInstance zombie, ZombieSave zs) {
        if (zombie.getArmors() == null || zs.getArmors() == null) {
            return;
        }
        for (ArmorSave as : zs.getArmors()) {
            if (as == null || as.getArmorType() == null) {
                continue;
            }
            ArmorType type;
            try {
                type = ArmorType.valueOf(as.getArmorType());
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            applyArmorHealth(zombie, type, as.getCurrentHealth());
        }
    }

    private static void applyArmorHealth(ZombieInstance zombie, ArmorType type, int health) {
        for (Armor armor : zombie.getArmors()) {
            if (armor != null && armor.getType() == type) {
                armor.setCurrentHealth(health);
                break;
            }
        }
    }

    private static void applySuns(GameSaveData data, GameModel model) {
        for (SunSave ss : data.getSuns()) {
            if (ss == null) {
                continue;
            }
            Sun sun = new Sun(ss.getType(), ss.getValue(), ss.getX(), ss.getY());
            sun.setOffset(ss.getOffsetX(), ss.getOffsetY());
            sun.setFall(ss.getFallRemaining(), ss.getFallDuration());
            model.spawnSun(sun);
        }
    }

    private static void applyPlantFood(GameSaveData data, GameModel model) {
        for (PlantFoodSave ps : data.getPlantFoodPickups()) {
            if (ps == null) {
                continue;
            }
            model.spawnPlantFood(new PlantFoodPickup(ps.getX(), ps.getY(), ps.getOffsetX(), ps.getOffsetY()));
        }
    }

    private static void applyLoot(GameSaveData data, GameModel model) {
        for (LootSave ls : data.getLootPickups()) {
            if (ls == null || ls.getKind() == null) {
                continue;
            }
            model.spawnLootPickup(new LootPickup(
                    ls.getKind(), ls.getAmount(), ls.getX(), ls.getY(),
                    ls.getOffsetX(), ls.getOffsetY()));
        }
    }

    private static void applyGraves(GameSaveData data, GameModel model) {
        for (GraveSave gs : data.getGraves()) {
            if (gs == null) {
                continue;
            }
            if (model.spawnGraveAt(gs.getRow(), gs.getCol(), gs.getType())) {
                Cell cell = model.getCellAt(gs.getRow(), gs.getCol());
                Placeable placeable = cell == null ? null : cell.getPlaceable(PlacableLayer.GROUND);
                if (placeable instanceof Grave grave) {
                    grave.setHp(gs.getHp());
                }
            }
        }
    }

    private static void applyMowers(GameSaveData data, GameModel model) {
        GameMap map = model.getMap();
        for (MowerSave ms : data.getMowers()) {
            if (ms == null) {
                continue;
            }
            Lane lane = map.getLane(ms.getRow());
            if (lane == null) {
                continue;
            }
            applyMower(lane, ms);
        }
    }

    private static void applyMower(Lane lane, MowerSave ms) {
        if (!ms.isPresent()) {
            lane.clearLawnMower();
            return;
        }
        LawnMower mower = lane.getLawnMower();
        if (mower == null) {
            mower = new LawnMower();
            lane.setLawnMower(mower);
        }
        mower.restore(ms.isActive(), ms.isTriggered(), ms.isSweeping(),
                ms.getXPosition(), ms.getTransitionElapsed());
    }

    private static void applyTerrains(GameSaveData data, GameModel model) {
        ensureZombieFactory();
        for (TerrainSave ts : data.getTerrains()) {
            if (ts == null) {
                continue;
            }
            Cell cell = model.getCellAt(ts.getRow(), ts.getCol());
            if (cell == null) {
                continue;
            }
            if (ts.getGroundType() != null) {
                cell.setGroundType(ts.getGroundType());
            }
            applyTerrainKind(cell, ts);
        }
    }

    private static void applyTerrainKind(Cell cell, TerrainSave ts) {
        String kind = ts.getKind() == null ? "" : ts.getKind().toUpperCase(Locale.ROOT);
        switch (kind) {
            case "ICE" -> applyIceTerrain(cell, ts);
            case "FIRE" -> {
                cell.setTerrainStrategy(new FireTerrainStrategy(ts.getFireRemaining()));
                if (ts.getGroundType() == null) {
                    cell.setGroundType(GroundType.FIRE);
                }
            }
            case "CRATER" -> {
                cell.setTerrainStrategy(new CraterTerrainStrategy());
                if (ts.getGroundType() == null) {
                    cell.setGroundType(GroundType.CRATER);
                }
            }
            default -> {
                // Keep whatever onStart installed for slides / necromancy / water.
            }
        }
    }

    private static void applyIceTerrain(Cell cell, TerrainSave ts) {
        ZombieInstance frozen = null;
        if (ts.getFrozenZombieName() != null) {
            frozen = ZombieFactory.createInstance(ts.getFrozenZombieName());
            if (frozen != null) {
                frozen.setGridPosition(new Point(ts.getCol(), ts.getRow()));
                frozen.setContinuousPosition(new FloatPoint(ts.getCol(), ts.getRow()));
            }
        }
        IceTerrainStrategy ice = new IceTerrainStrategy(frozen);
        ice.restore(ts.getIceHp(), ts.isIceMelted());
        cell.setTerrainStrategy(ice);
        if (ts.getGroundType() == null) {
            cell.setGroundType(GroundType.ICE);
        }
    }

    private static void ensurePlantFactory() {
        try {
            PlantFactory.getAllDefinitions();
        } catch (RuntimeException e) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
            } catch (IOException ignored) {
                // Restore will skip unknown plants.
            }
        }
    }

    private static void ensureZombieFactory() {
        try {
            ZombieFactory.getAllDefinitions();
        } catch (RuntimeException e) {
            try {
                ZombieFactory.init(
                        "/assets/data/zombies/zombies.json",
                        "/assets/data/armor/ArmorTypeData.json");
            } catch (IOException ignored) {
                // Restore will skip unknown zombies.
            }
        }
    }
}
