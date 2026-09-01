package model.game.save;

import model.app.App;
import model.data.level.LevelRegistry;
import model.data.minigame.MiniGameRegistry;
import model.enums.ArmorType;
import model.enums.Chapter;
import model.enums.GameState;
import model.enums.GroundType;
import model.enums.MenuType;
import model.enums.MiniGameType;
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
import model.user.User;
import model.zombie.ZombieFactory;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Captures / restores an in-progress {@link GameModel} for Save and Exit.
 */
public final class GameSaveService {

    private static GameSaveService instance;

    private final GameSaveRepository repository;

    private GameSaveService(GameSaveRepository repository) {
        this.repository = repository;
    }

    public static synchronized GameSaveService getInstance() {
        if (instance == null) {
            instance = new GameSaveService(new GameSaveRepository());
        }
        return instance;
    }

    /** Test hook: replaces the singleton (and its repository). */
    public static synchronized void resetForTests(GameSaveRepository repository) {
        instance = new GameSaveService(repository == null ? new GameSaveRepository() : repository);
    }

    public GameSaveRepository getRepository() {
        return repository;
    }

    public Optional<GameSaveData> findSaveForCurrentUser() {
        User user = App.getInstance().getCurrentUser();
        if (user == null) {
            return Optional.empty();
        }
        return repository.load(user.getUsername());
    }

    public boolean hasSaveForAdventure(Chapter chapter, int levelId) {
        return findSaveForCurrentUser()
                .filter(s -> s.getMode() == GameSaveData.Mode.ADVENTURE)
                .filter(s -> s.getChapter() == chapter && s.getLevelId() == levelId)
                .isPresent();
    }

    public boolean hasSaveForMiniGame(MiniGameType type, int stage) {
        return findSaveForCurrentUser()
                .filter(s -> s.getMode() == GameSaveData.Mode.MINI_GAME)
                .filter(s -> s.getMiniGameType() == type && s.getMiniGameStage() == stage)
                .isPresent();
    }

    public boolean hasScoreSave() {
        return findSaveForCurrentUser()
                .filter(s -> s.getMode() == GameSaveData.Mode.SCORE)
                .isPresent();
    }

    public void clearCurrentUserSave() {
        User user = App.getInstance().getCurrentUser();
        if (user != null) {
            repository.delete(user.getUsername());
        }
    }

    /**
     * Snapshots the active session to disk. Does not clear {@link App} session state.
     */
    public void saveCurrentGame() throws IOException {
        User user = App.getInstance().getCurrentUser();
        GameModel model = App.getInstance().getCurrentGameModel();
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (user == null || model == null) {
            throw new IllegalStateException("No active game to save.");
        }
        GameSaveData data = capture(user.getUsername(), model, loop);
        repository.save(data);
    }

    /**
     * Rebuilds {@link App}'s game model/loop from the current user's save and
     * switches to {@link MenuType#IN_GAME}.
     */
    public void resumeSavedGame() throws IOException {
        User user = App.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No logged-in user.");
        }
        GameSaveData data = repository.load(user.getUsername())
                .orElseThrow(() -> new IllegalStateException("No saved game."));
        restoreIntoApp(data);
    }

    public GameSaveData capture(String username, GameModel model, PvZGameLoop loop) {
        GameSaveData data = new GameSaveData();
        data.setUsername(username);
        data.setSavedAtEpochMs(System.currentTimeMillis());

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

        data.setSelectedPlants(model.getSelectedPlants() == null
                ? new ArrayList<>()
                : new ArrayList<>(model.getSelectedPlants()));
        data.setImitaterCopyTarget(model.getImitaterCopyTarget());
        data.setSunAmount(model.getSunAmount());
        data.setPlantFoodCount(model.getPlantFoodCount());
        data.setPersistentPlantFood(model.getPersistentPlantFood());
        data.setSeedCooldowns(model.getSeedCooldownsSnapshot());
        data.setSeedCooldownsDisabled(model.areSeedCooldownsDisabled());
        data.setElapsedSeconds(model.getElapsedSeconds());
        data.setCurrentTick(model.getTick());
        data.setGameState(GameState.PAUSED);
        data.setDifficultyLevel(model.getDifficulty());
        data.setDiamondCount(model.getDiamondCount());
        data.setCoinCount(model.getCoinCount());
        data.setFlowerPotCount(model.getFlowerPotCount());
        data.setHouseBreached(model.isHouseBreached());
        data.setBreachedRows(new java.util.HashSet<>(model.getBreachedRows()));
        data.setZombiesKilled(model.getZombiesKilled());
        data.setPlantsLost(model.getPlantsLost());

        if (loop != null && loop.getSunFallSystem() != null) {
            data.setSunFallElapsed(loop.getSunFallSystem().getElapsedSeconds());
            data.setSunFallDropTimer(loop.getSunFallSystem().getDropTimer());
            data.setSkyDropEnabled(loop.getSunFallSystem().isSkyDropEnabled());
        }
        if (model.getTideState() != null) {
            data.setTideDynamicColumns(model.getTideState().getDynamicColumns());
        }

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
        return data;
    }

    public void restoreIntoApp(GameSaveData data) throws IOException {
        Level level = createLevel(data);
        GameModel model = new GameModel(level);
        App.getInstance().setCurrentGameModel(model);
        App.getInstance().setCurrentGameLoop(null);

        level.onStart();
        apply(data, model);

        PvZGameLoop loop = new PvZGameLoop(model);
        if (loop.getSunFallSystem() != null) {
            loop.getSunFallSystem().restoreTimers(
                    data.getSunFallElapsed(),
                    data.getSunFallDropTimer(),
                    data.isSkyDropEnabled());
        }
        loop.resume();
        model.setGameState(GameState.RUNNING);
        App.getInstance().setCurrentGameLoop(loop);
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);
    }

    private static Level createLevel(GameSaveData data) throws IOException {
        return switch (data.getMode()) {
            case MINI_GAME -> {
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
                yield level;
            }
            case SCORE -> ScoreLevelGenerator.createDailyLevel();
            default -> {
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
                yield level;
            }
        };
    }

    private void apply(GameSaveData data, GameModel model) {
        model.clearBoardForRestore();
        model.restoreResources(data.getSunAmount(), data.getPlantFoodCount(), data.getPersistentPlantFood());
        model.restoreSeedCooldowns(data.getSeedCooldowns(), data.isSeedCooldownsDisabled());
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
        model.setSelectedPlants(data.getSelectedPlants() == null
                ? new ArrayList<>()
                : new ArrayList<>(data.getSelectedPlants()));
        if (data.getImitaterCopyTarget() != null) {
            model.setImitaterCopyTarget(data.getImitaterCopyTarget());
        }
        model.setGameState(GameState.PAUSED);
        if (model.getTideState() != null && model.getTideState().isActive()) {
            model.getTideState().setDynamicColumns(data.getTideDynamicColumns());
            model.getTideState().applyToMap(model.getMap());
        }

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

    private static GameSaveData.WaveManagerSave captureWaveManager(WaveManager wm) {
        GameSaveData.WaveManagerSave save = new GameSaveData.WaveManagerSave();
        save.setCurrentWaveIndex(wm.getCurrentWaveIndex());
        save.setPhase(wm.getPhase());
        save.setInterWaveTimer(wm.getInterWaveTimer());
        save.setCurrentWaveTotal(wm.getCurrentWaveTotal());
        save.setCurrentWaveKilled(wm.getCurrentWaveKilled());
        save.setMaxReportedProgress(wm.getMaxReportedProgress());
        List<GameSaveData.WaveSave> waves = new ArrayList<>();
        for (Wave wave : wm.getWaves()) {
            GameSaveData.WaveSave ws = new GameSaveData.WaveSave();
            ws.setState(wave.getState());
            ws.setWaveClock(wave.getWaveClock());
            List<GameSaveData.EntryRuntimeSave> entries = new ArrayList<>();
            for (EntryRuntime rt : wave.getRuntimeEntries()) {
                GameSaveData.EntryRuntimeSave es = new GameSaveData.EntryRuntimeSave();
                es.setActivated(rt.isActivated());
                es.setFirstSpawnAt(rt.getFirstSpawnAt());
                es.setRemainingSpawns(rt.getRemainingSpawns());
                es.setNextSpawnAt(rt.getNextSpawnAt());
                es.setExhausted(rt.isExhausted());
                es.setGroupVolleyFired(rt.isGroupVolleyFired());
                entries.add(es);
            }
            ws.setEntries(entries);
            waves.add(ws);
        }
        save.setWaves(waves);
        return save;
    }

    private static void applyWaveManager(GameSaveData data, GameModel model,
                                         List<ZombieInstance> waveLiving) {
        WaveManager wm = model.getWaveManager();
        GameSaveData.WaveManagerSave save = data.getWaveManager();
        if (wm == null || save == null) {
            return;
        }
        List<Wave> waves = wm.getWaves();
        List<GameSaveData.WaveSave> savedWaves = save.getWaves();
        int n = Math.min(waves.size(), savedWaves == null ? 0 : savedWaves.size());
        for (int i = 0; i < n; i++) {
            Wave wave = waves.get(i);
            GameSaveData.WaveSave ws = savedWaves.get(i);
            wave.setState(ws.getState());
            wave.setWaveClock(ws.getWaveClock());
            List<EntryRuntime> runtimes = wave.getRuntimeEntries();
            List<GameSaveData.EntryRuntimeSave> entries = ws.getEntries();
            int m = Math.min(runtimes.size(), entries == null ? 0 : entries.size());
            for (int j = 0; j < m; j++) {
                EntryRuntime rt = runtimes.get(j);
                GameSaveData.EntryRuntimeSave es = entries.get(j);
                rt.setActivated(es.isActivated());
                rt.setFirstSpawnAt(es.getFirstSpawnAt());
                rt.setRemainingSpawns(es.getRemainingSpawns());
                rt.setNextSpawnAt(es.getNextSpawnAt());
                rt.setExhausted(es.isExhausted());
                rt.setGroupVolleyFired(es.isGroupVolleyFired());
            }
        }
        wm.restoreFromSave(
                save.getCurrentWaveIndex(),
                save.getPhase(),
                save.getInterWaveTimer(),
                save.getCurrentWaveTotal(),
                save.getCurrentWaveKilled(),
                save.getMaxReportedProgress(),
                waveLiving);
    }

    private static List<GameSaveData.PlantSave> capturePlants(GameModel model) {
        List<GameSaveData.PlantSave> out = new ArrayList<>();
        for (PlantInstance plant : model.getAllPlants()) {
            if (plant == null || plant.getDefinition() == null || plant.getPosition() == null) {
                continue;
            }
            GameSaveData.PlantSave ps = new GameSaveData.PlantSave();
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
            ps.setImitateTarget(plant.getImitateTarget());
            ps.setTransformCountdown(plant.getTransformCountdown());
            ps.setIceHp(plant.getIceHp());
            ps.setOctopusCoating(plant.hasOctopusCoating());
            ps.setFreezeHitCount(plant.getFreezeHitCount());
            List<GameSaveData.AbilitySave> abilities = new ArrayList<>();
            for (AbilityState state : plant.getAbilityStates().values()) {
                if (state == null || state.getAbilityType() == null) {
                    continue;
                }
                GameSaveData.AbilitySave as = new GameSaveData.AbilitySave();
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
                abilities.add(as);
            }
            ps.setAbilities(abilities);
            out.add(ps);
        }
        return out;
    }

    private static void applyPlants(GameSaveData data, GameModel model) {
        ensurePlantFactory();
        for (GameSaveData.PlantSave ps : data.getPlants()) {
            if (ps == null || ps.getDefinitionName() == null) {
                continue;
            }
            PlantInstance plant = PlantFactory.createInstance(ps.getDefinitionName(), Math.max(1, ps.getLevel()));
            if (plant == null) {
                continue;
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
            for (GameSaveData.AbilitySave as : ps.getAbilities()) {
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
            model.restorePlant(plant, ps.getRow(), ps.getCol());
        }
    }

    private static List<GameSaveData.ZombieSave> captureZombies(GameModel model) {
        WaveManager wm = model.getWaveManager();
        List<GameSaveData.ZombieSave> out = new ArrayList<>();
        for (ZombieInstance zombie : model.getZombies()) {
            if (zombie == null || zombie.getDefinition() == null) {
                continue;
            }
            GameSaveData.ZombieSave zs = new GameSaveData.ZombieSave();
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
            List<GameSaveData.ArmorSave> armors = new ArrayList<>();
            if (zombie.getArmors() != null) {
                for (Armor armor : zombie.getArmors()) {
                    if (armor == null || armor.getType() == null) {
                        continue;
                    }
                    GameSaveData.ArmorSave as = new GameSaveData.ArmorSave();
                    as.setArmorType(armor.getType().name());
                    as.setCurrentHealth(armor.getCurrentHealth());
                    armors.add(as);
                }
            }
            zs.setArmors(armors);
            out.add(zs);
        }
        return out;
    }

    private static List<ZombieInstance> applyZombies(GameSaveData data, GameModel model) {
        ensureZombieFactory();
        List<ZombieInstance> waveLiving = new ArrayList<>();
        for (GameSaveData.ZombieSave zs : data.getZombies()) {
            if (zs == null || zs.getDefinitionName() == null) {
                continue;
            }
            ZombieInstance zombie = ZombieFactory.createInstance(zs.getDefinitionName());
            if (zombie == null) {
                continue;
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
            if (zombie.getArmors() != null && zs.getArmors() != null) {
                for (GameSaveData.ArmorSave as : zs.getArmors()) {
                    if (as == null || as.getArmorType() == null) {
                        continue;
                    }
                    ArmorType type;
                    try {
                        type = ArmorType.valueOf(as.getArmorType());
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    for (Armor armor : zombie.getArmors()) {
                        if (armor != null && armor.getType() == type) {
                            armor.setCurrentHealth(as.getCurrentHealth());
                            break;
                        }
                    }
                }
            }
            model.restoreZombie(zombie);
            if (zs.isCountsTowardCurrentWave()) {
                waveLiving.add(zombie);
            }
        }
        return waveLiving;
    }

    private static List<GameSaveData.SunSave> captureSuns(GameModel model) {
        List<GameSaveData.SunSave> out = new ArrayList<>();
        for (Sun sun : model.getActiveSuns()) {
            if (sun == null) {
                continue;
            }
            GameSaveData.SunSave ss = new GameSaveData.SunSave();
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

    private static void applySuns(GameSaveData data, GameModel model) {
        for (GameSaveData.SunSave ss : data.getSuns()) {
            if (ss == null) {
                continue;
            }
            Sun sun = new Sun(ss.getType(), ss.getValue(), ss.getX(), ss.getY());
            sun.setOffset(ss.getOffsetX(), ss.getOffsetY());
            sun.setFall(ss.getFallRemaining(), ss.getFallDuration());
            model.spawnSun(sun);
        }
    }

    private static List<GameSaveData.PlantFoodSave> capturePlantFood(GameModel model) {
        List<GameSaveData.PlantFoodSave> out = new ArrayList<>();
        for (PlantFoodPickup pickup : model.getActivePlantFood()) {
            if (pickup == null) {
                continue;
            }
            GameSaveData.PlantFoodSave ps = new GameSaveData.PlantFoodSave();
            ps.setX(pickup.getX());
            ps.setY(pickup.getY());
            ps.setOffsetX(pickup.getOffsetX());
            ps.setOffsetY(pickup.getOffsetY());
            out.add(ps);
        }
        return out;
    }

    private static void applyPlantFood(GameSaveData data, GameModel model) {
        for (GameSaveData.PlantFoodSave ps : data.getPlantFoodPickups()) {
            if (ps == null) {
                continue;
            }
            model.spawnPlantFood(new PlantFoodPickup(ps.getX(), ps.getY(), ps.getOffsetX(), ps.getOffsetY()));
        }
    }

    private static List<GameSaveData.LootSave> captureLoot(GameModel model) {
        List<GameSaveData.LootSave> out = new ArrayList<>();
        for (LootPickup loot : model.getActiveLootPickups()) {
            if (loot == null || loot.getKind() == null) {
                continue;
            }
            GameSaveData.LootSave ls = new GameSaveData.LootSave();
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

    private static void applyLoot(GameSaveData data, GameModel model) {
        for (GameSaveData.LootSave ls : data.getLootPickups()) {
            if (ls == null || ls.getKind() == null) {
                continue;
            }
            model.spawnLootPickup(new LootPickup(
                    ls.getKind(), ls.getAmount(), ls.getX(), ls.getY(),
                    ls.getOffsetX(), ls.getOffsetY()));
        }
    }

    private static List<GameSaveData.GraveSave> captureGraves(GameModel model) {
        List<GameSaveData.GraveSave> out = new ArrayList<>();
        GameMap map = model.getMap();
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                Placeable placeable = map.getCell(col, row).getPlaceable(PlacableLayer.GROUND);
                if (!(placeable instanceof Grave grave)) {
                    continue;
                }
                GameSaveData.GraveSave gs = new GameSaveData.GraveSave();
                gs.setRow(row);
                gs.setCol(col);
                gs.setHp(grave.getHp());
                gs.setType(grave.getType());
                out.add(gs);
            }
        }
        return out;
    }

    private static void applyGraves(GameSaveData data, GameModel model) {
        for (GameSaveData.GraveSave gs : data.getGraves()) {
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

    private static List<GameSaveData.MowerSave> captureMowers(GameModel model) {
        List<GameSaveData.MowerSave> out = new ArrayList<>();
        GameMap map = model.getMap();
        for (int row = 0; row < map.getRows(); row++) {
            Lane lane = map.getLane(row);
            GameSaveData.MowerSave ms = new GameSaveData.MowerSave();
            ms.setRow(row);
            if (lane == null || lane.getLawnMower() == null) {
                ms.setPresent(false);
                out.add(ms);
                continue;
            }
            LawnMower mower = lane.getLawnMower();
            ms.setPresent(true);
            ms.setActive(mower.isActive());
            ms.setTriggered(mower.isTriggered());
            ms.setSweeping(mower.isSweeping());
            ms.setXPosition(mower.getXPosition());
            ms.setTransitionElapsed(mower.getTransitionElapsed());
            out.add(ms);
        }
        return out;
    }

    private static void applyMowers(GameSaveData data, GameModel model) {
        GameMap map = model.getMap();
        for (GameSaveData.MowerSave ms : data.getMowers()) {
            if (ms == null) {
                continue;
            }
            Lane lane = map.getLane(ms.getRow());
            if (lane == null) {
                continue;
            }
            if (!ms.isPresent()) {
                lane.clearLawnMower();
                continue;
            }
            LawnMower mower = lane.getLawnMower();
            if (mower == null) {
                mower = new LawnMower();
                lane.setLawnMower(mower);
            }
            mower.restore(ms.isActive(), ms.isTriggered(), ms.isSweeping(),
                    ms.getXPosition(), ms.getTransitionElapsed());
        }
    }

    private static List<GameSaveData.TerrainSave> captureTerrains(GameModel model) {
        List<GameSaveData.TerrainSave> out = new ArrayList<>();
        GameMap map = model.getMap();
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                Cell cell = map.getCell(col, row);
                TerrainStrategy terrain = cell.getTerrainStrategy();
                GroundType ground = cell.getGroundType();
                if (!(terrain instanceof IceTerrainStrategy)
                        && !(terrain instanceof FireTerrainStrategy)
                        && !(terrain instanceof CraterTerrainStrategy)
                        && ground != GroundType.ICE
                        && ground != GroundType.FIRE
                        && ground != GroundType.CRATER) {
                    continue;
                }
                GameSaveData.TerrainSave ts = new GameSaveData.TerrainSave();
                ts.setRow(row);
                ts.setCol(col);
                ts.setGroundType(ground);
                if (terrain instanceof IceTerrainStrategy ice) {
                    ts.setKind("ICE");
                    ts.setIceHp(ice.getHp());
                    ts.setIceMelted(ice.isMelted());
                    Placeable contained = ice.getContainedEntity();
                    if (contained instanceof ZombieInstance frozen
                            && frozen.getDefinition() != null) {
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
                out.add(ts);
            }
        }
        return out;
    }

    private static void applyTerrains(GameSaveData data, GameModel model) {
        ensureZombieFactory();
        for (GameSaveData.TerrainSave ts : data.getTerrains()) {
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
            String kind = ts.getKind() == null ? "" : ts.getKind().toUpperCase(Locale.ROOT);
            switch (kind) {
                case "ICE" -> {
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
