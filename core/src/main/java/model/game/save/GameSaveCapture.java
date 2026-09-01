package model.game.save;

import model.enums.GameState;
import model.enums.GroundType;
import model.enums.PlacableLayer;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.Level;
import model.game.level.minigame.MiniGameLevel;
import model.game.level.special.ScoreLevel;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.GameMap;
import model.game.map.Lane;
import model.game.map.LawnMower;
import model.game.map.Point;
import model.game.map.terrain.CraterTerrainStrategy;
import model.game.map.terrain.FireTerrainStrategy;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.map.terrain.TerrainStrategy;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.game.wave.WaveManager;
import model.item.Grave;
import model.item.LootPickup;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.item.placeable.Placeable;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Builds a {@link GameSaveData} snapshot from a live {@link GameModel}.
 */
final class GameSaveCapture {

    private GameSaveCapture() {
    }

    static GameSaveData capture(String username, GameModel model, PvZGameLoop loop) {
        GameSaveData data = new GameSaveData();
        data.setUsername(username);
        data.setSavedAtEpochMs(System.currentTimeMillis());
        fillLevel(data, model);
        fillResources(data, model);
        fillProgress(data, model, loop);
        fillEntities(data, model);
        return data;
    }

    private static void fillLevel(GameSaveData data, GameModel model) {
        Level level = model.getCurrentLevel();
        if (level instanceof MiniGameLevel mini) {
            data.setMode(GameSaveData.Mode.MINI_GAME);
            data.setMiniGameType(mini.getMiniGameType());
            data.setMiniGameStage(mini.getStage());
            data.setChapter(mini.getConfig() == null ? null : mini.getConfig().getChapter());
            data.setLevelId(mini.getConfig() == null ? 0 : mini.getConfig().getLevelId());
        } else if (level instanceof ScoreLevel) {
            data.setMode(GameSaveData.Mode.SCORE);
            data.setChapter(level.getConfig() == null ? null : level.getConfig().getChapter());
            data.setLevelId(level.getConfig() == null ? 0 : level.getConfig().getLevelId());
        } else {
            data.setMode(GameSaveData.Mode.ADVENTURE);
            data.setChapter(model.getChapter());
            data.setLevelId(level == null || level.getConfig() == null ? 0 : level.getConfig().getLevelId());
        }
    }

    private static void fillResources(GameSaveData data, GameModel model) {
        data.setSelectedPlants(model.getSelectedPlants() == null
                ? new ArrayList<>()
                : new ArrayList<>(model.getSelectedPlants()));
        data.setImitaterCopyTarget(model.getImitaterCopyTarget());
        data.setSunAmount(model.getSunAmount());
        data.setPlantFoodCount(model.getPlantFoodCount());
        data.setPersistentPlantFood(model.getPersistentPlantFood());
        data.setSeedCooldowns(model.getSeedCooldownsSnapshot());
        data.setSeedCooldownsDisabled(model.areSeedCooldownsDisabled());
    }

    private static void fillProgress(GameSaveData data, GameModel model, PvZGameLoop loop) {
        data.setElapsedSeconds(model.getElapsedSeconds());
        data.setCurrentTick(model.getTick());
        data.setGameState(GameState.PAUSED);
        data.setDifficultyLevel(model.getDifficulty());
        data.setDiamondCount(model.getDiamondCount());
        data.setCoinCount(model.getCoinCount());
        data.setFlowerPotCount(model.getFlowerPotCount());
        data.setHouseBreached(model.isHouseBreached());
        data.setBreachedRows(new HashSet<>(model.getBreachedRows()));
        data.setZombiesKilled(model.getZombiesKilled());
        data.setPlantsLost(model.getPlantsLost());
        fillSunFallAndTide(data, model, loop);
    }

    private static void fillSunFallAndTide(GameSaveData data, GameModel model, PvZGameLoop loop) {
        if (loop != null && loop.getSunFallSystem() != null) {
            data.setSunFallElapsed(loop.getSunFallSystem().getElapsedSeconds());
            data.setSunFallDropTimer(loop.getSunFallSystem().getDropTimer());
            data.setSkyDropEnabled(loop.getSunFallSystem().isSkyDropEnabled());
        }
        if (model.getTideState() != null) {
            data.setTideDynamicColumns(model.getTideState().getDynamicColumns());
        }
    }

    private static void fillEntities(GameSaveData data, GameModel model) {
        WaveManager wm = model.getWaveManager();
        if (wm != null) {
            data.setWaveManager(captureWaveManager(wm));
        }
        data.setPlants(capturePlants(model));
        data.setZombies(captureZombies(model));
        data.setSuns(captureSuns(model));
        data.setPlantFoodPickups(capturePlantFood(model));
        data.setLootPickups(captureLoot(model));
        data.setGraves(captureGraves(model));
        data.setMowers(captureMowers(model));
        data.setTerrains(captureTerrains(model));
    }

    private static WaveManagerSave captureWaveManager(WaveManager wm) {
        WaveManagerSave save = new WaveManagerSave();
        save.setCurrentWaveIndex(wm.getCurrentWaveIndex());
        save.setPhase(wm.getPhase());
        save.setInterWaveTimer(wm.getInterWaveTimer());
        save.setCurrentWaveTotal(wm.getCurrentWaveTotal());
        save.setCurrentWaveKilled(wm.getCurrentWaveKilled());
        save.setMaxReportedProgress(wm.getMaxReportedProgress());
        List<WaveSave> waves = new ArrayList<>();
        for (Wave wave : wm.getWaves()) {
            waves.add(captureWave(wave));
        }
        save.setWaves(waves);
        return save;
    }

    private static WaveSave captureWave(Wave wave) {
        WaveSave ws = new WaveSave();
        ws.setState(wave.getState());
        ws.setWaveClock(wave.getWaveClock());
        List<EntryRuntimeSave> entries = new ArrayList<>();
        for (EntryRuntime rt : wave.getRuntimeEntries()) {
            entries.add(captureEntry(rt));
        }
        ws.setEntries(entries);
        return ws;
    }

    private static EntryRuntimeSave captureEntry(EntryRuntime rt) {
        EntryRuntimeSave es = new EntryRuntimeSave();
        es.setActivated(rt.isActivated());
        es.setFirstSpawnAt(rt.getFirstSpawnAt());
        es.setRemainingSpawns(rt.getRemainingSpawns());
        es.setNextSpawnAt(rt.getNextSpawnAt());
        es.setExhausted(rt.isExhausted());
        es.setGroupVolleyFired(rt.isGroupVolleyFired());
        return es;
    }

    private static List<PlantSave> capturePlants(GameModel model) {
        List<PlantSave> out = new ArrayList<>();
        for (PlantInstance plant : model.getAllPlants()) {
            if (plant == null || plant.getDefinition() == null || plant.getPosition() == null) {
                continue;
            }
            out.add(capturePlant(plant));
        }
        return out;
    }

    private static PlantSave capturePlant(PlantInstance plant) {
        PlantSave ps = new PlantSave();
        ps.setDefinitionName(plant.getDefinition().getName());
        ps.setRow(plant.getPosition().getY());
        ps.setCol(plant.getPosition().getX());
        ps.setState(plant.getState());
        ps.setCurrentHp(plant.getCurrentHP());
        ps.setArmorHp(plant.getArmorHP());
        ps.setArmorMaxHp(plant.getArmorMaxHP());
        ps.setLevel(plant.getLevel());
        ps.setCurrentRecharge(plant.getCurrentRecharge());
        ps.setPlantFoodActive(plant.isPlantFoodActive());
        ps.setPlantFoodDurationRemaining(plant.getPlantFoodDurationRemaining());
        ps.setLifespanRemaining(plant.getLifespanRemaining());
        ps.setLifespanTotal(plant.getLifespanTotal());
        ps.setStackCount(plant.getStackCount());
        fillPlantStatus(ps, plant);
        ps.setAbilities(captureAbilities(plant));
        return ps;
    }

    private static void fillPlantStatus(PlantSave ps, PlantInstance plant) {
        ps.setImitateTarget(plant.getImitateTarget());
        ps.setTransformCountdown(plant.getTransformCountdown());
        ps.setIceHp(plant.getIceHp());
        ps.setOctopusCoating(plant.hasOctopusCoating());
        ps.setFreezeHitCount(plant.getFreezeHitCount());
    }

    private static List<AbilitySave> captureAbilities(PlantInstance plant) {
        List<AbilitySave> abilities = new ArrayList<>();
        for (AbilityState state : plant.getAbilityStates().values()) {
            if (state == null || state.getAbilityType() == null) {
                continue;
            }
            abilities.add(captureAbility(state));
        }
        return abilities;
    }

    private static AbilitySave captureAbility(AbilityState state) {
        AbilitySave as = new AbilitySave();
        as.setAbilityType(state.getAbilityType());
        as.setCooldownRemaining(state.getCooldownRemaining());
        as.setChargeProgress(state.getChargeProgress());
        as.setActive(state.isActive());
        as.setArmed(state.isArmed());
        as.setArmedElapsed(state.getArmedElapsed());
        as.setGrowthStage(state.getGrowthStage());
        as.setDigesting(state.isDigesting());
        as.setDigestRemaining(state.getDigestRemaining());
        as.setShotOrdinal(state.getShotOrdinal());
        return as;
    }

    private static List<ZombieSave> captureZombies(GameModel model) {
        WaveManager wm = model.getWaveManager();
        List<ZombieSave> out = new ArrayList<>();
        for (ZombieInstance zombie : model.getZombies()) {
            if (zombie == null || zombie.getDefinition() == null) {
                continue;
            }
            out.add(captureZombie(zombie, wm));
        }
        return out;
    }

    private static ZombieSave captureZombie(ZombieInstance zombie, WaveManager wm) {
        ZombieSave zs = new ZombieSave();
        zs.setDefinitionName(zombie.getDefinition().getName());
        Point grid = zombie.getGridPosition();
        FloatPoint cont = zombie.getContinuousPosition();
        zs.setGridCol(grid == null ? 0 : grid.getX());
        zs.setGridRow(grid == null ? 0 : grid.getY());
        zs.setContinuousX(cont == null ? zs.getGridCol() : cont.getX());
        zs.setContinuousY(cont == null ? zs.getGridRow() : cont.getY());
        zs.setState(zombie.getState());
        zs.setCurrentHp(zombie.getCurrentHP());
        zs.setCurrentSpeed(zombie.getCurrentSpeed());
        zs.setSpeedModifier(zombie.getSpeedModifier());
        zs.setGlowing(zombie.isGlowing());
        zs.setChillLevel(zombie.getChillLevel());
        zs.setChillStackTimer(zombie.getChillStackTimer());
        zs.setButtered(zombie.isButtered());
        zs.setHypnotized(zombie.isHypnotized());
        zs.setMovingBackward(zombie.isMovingBackward());
        zs.setCountsTowardCurrentWave(wm != null && wm.isCurrentWaveLiving(zombie));
        zs.setArmors(captureArmors(zombie));
        return zs;
    }

    private static List<ArmorSave> captureArmors(ZombieInstance zombie) {
        List<ArmorSave> armors = new ArrayList<>();
        if (zombie.getArmors() == null) {
            return armors;
        }
        for (Armor armor : zombie.getArmors()) {
            if (armor == null || armor.getType() == null) {
                continue;
            }
            ArmorSave as = new ArmorSave();
            as.setArmorType(armor.getType().name());
            as.setCurrentHealth(armor.getCurrentHealth());
            armors.add(as);
        }
        return armors;
    }

    private static List<SunSave> captureSuns(GameModel model) {
        List<SunSave> out = new ArrayList<>();
        for (Sun sun : model.getActiveSuns()) {
            if (sun == null) {
                continue;
            }
            SunSave ss = new SunSave();
            ss.setType(sun.getType());
            ss.setValue(sun.getValue());
            ss.setX(sun.getX());
            ss.setY(sun.getY());
            ss.setOffsetX(sun.getOffsetX());
            ss.setOffsetY(sun.getOffsetY());
            ss.setFallRemaining(sun.getFallRemaining());
            ss.setFallDuration(sun.getFallDuration());
            out.add(ss);
        }
        return out;
    }

    private static List<PlantFoodSave> capturePlantFood(GameModel model) {
        List<PlantFoodSave> out = new ArrayList<>();
        for (PlantFoodPickup pickup : model.getActivePlantFood()) {
            if (pickup == null) {
                continue;
            }
            PlantFoodSave ps = new PlantFoodSave();
            ps.setX(pickup.getX());
            ps.setY(pickup.getY());
            ps.setOffsetX(pickup.getOffsetX());
            ps.setOffsetY(pickup.getOffsetY());
            out.add(ps);
        }
        return out;
    }

    private static List<LootSave> captureLoot(GameModel model) {
        List<LootSave> out = new ArrayList<>();
        for (LootPickup loot : model.getActiveLootPickups()) {
            if (loot == null || loot.getKind() == null) {
                continue;
            }
            LootSave ls = new LootSave();
            ls.setKind(loot.getKind());
            ls.setAmount(loot.getAmount());
            ls.setX(loot.getX());
            ls.setY(loot.getY());
            ls.setOffsetX(loot.getOffsetX());
            ls.setOffsetY(loot.getOffsetY());
            out.add(ls);
        }
        return out;
    }

    private static List<GraveSave> captureGraves(GameModel model) {
        List<GraveSave> out = new ArrayList<>();
        GameMap map = model.getMap();
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                Placeable placeable = map.getCell(col, row).getPlaceable(PlacableLayer.GROUND);
                if (!(placeable instanceof Grave grave)) {
                    continue;
                }
                GraveSave gs = new GraveSave();
                gs.setRow(row);
                gs.setCol(col);
                gs.setHp(grave.getHp());
                gs.setType(grave.getType());
                out.add(gs);
            }
        }
        return out;
    }

    private static List<MowerSave> captureMowers(GameModel model) {
        List<MowerSave> out = new ArrayList<>();
        GameMap map = model.getMap();
        for (int row = 0; row < map.getRows(); row++) {
            out.add(captureMower(map.getLane(row), row));
        }
        return out;
    }

    private static MowerSave captureMower(Lane lane, int row) {
        MowerSave ms = new MowerSave();
        ms.setRow(row);
        if (lane == null || lane.getLawnMower() == null) {
            ms.setPresent(false);
            return ms;
        }
        LawnMower mower = lane.getLawnMower();
        ms.setPresent(true);
        ms.setActive(mower.isActive());
        ms.setTriggered(mower.isTriggered());
        ms.setSweeping(mower.isSweeping());
        ms.setXPosition(mower.getXPosition());
        ms.setTransitionElapsed(mower.getTransitionElapsed());
        return ms;
    }

    private static List<TerrainSave> captureTerrains(GameModel model) {
        List<TerrainSave> out = new ArrayList<>();
        GameMap map = model.getMap();
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                TerrainSave ts = captureTerrainCell(map.getCell(col, row), row, col);
                if (ts != null) {
                    out.add(ts);
                }
            }
        }
        return out;
    }

    private static TerrainSave captureTerrainCell(Cell cell, int row, int col) {
        TerrainStrategy terrain = cell.getTerrainStrategy();
        GroundType ground = cell.getGroundType();
        if (!isSpecialTerrain(terrain, ground)) {
            return null;
        }
        TerrainSave ts = new TerrainSave();
        ts.setRow(row);
        ts.setCol(col);
        ts.setGroundType(ground);
        fillTerrainKind(ts, terrain);
        return ts;
    }

    private static boolean isSpecialTerrain(TerrainStrategy terrain, GroundType ground) {
        return terrain instanceof IceTerrainStrategy
                || terrain instanceof FireTerrainStrategy
                || terrain instanceof CraterTerrainStrategy
                || ground == GroundType.ICE
                || ground == GroundType.FIRE
                || ground == GroundType.CRATER;
    }

    private static void fillTerrainKind(TerrainSave ts, TerrainStrategy terrain) {
        if (terrain instanceof IceTerrainStrategy ice) {
            ts.setKind("ICE");
            ts.setIceHp(ice.getHp());
            ts.setIceMelted(ice.isMelted());
            Placeable contained = ice.getContainedEntity();
            if (contained instanceof ZombieInstance frozen && frozen.getDefinition() != null) {
                ts.setFrozenZombieName(frozen.getDefinition().getName());
            }
        } else if (terrain instanceof FireTerrainStrategy fire) {
            ts.setKind("FIRE");
            ts.setFireRemaining(fire.getRemainingSeconds());
        } else if (terrain instanceof CraterTerrainStrategy) {
            ts.setKind("CRATER");
        } else {
            ts.setKind("NONE");
        }
    }
}
