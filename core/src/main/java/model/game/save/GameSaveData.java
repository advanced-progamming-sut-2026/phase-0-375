package model.game.save;

import model.enums.Chapter;
import model.enums.GameState;
import model.enums.MiniGameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Jackson-friendly snapshot of an in-progress level, written by
 * {@link GameSaveService} when the player chooses Save and Exit.
 */
public class GameSaveData {

    public enum Mode {
        ADVENTURE,
        MINI_GAME,
        SCORE
    }

    private String username;
    private long savedAtEpochMs;
    private Mode mode = Mode.ADVENTURE;

    private Chapter chapter;
    private int levelId;
    private MiniGameType miniGameType;
    private int miniGameStage;

    private List<String> selectedPlants = new ArrayList<>();
    private String imitaterCopyTarget;

    private int sunAmount;
    private int plantFoodCount;
    private int persistentPlantFood;
    private Map<String, Float> seedCooldowns = new HashMap<>();
    private boolean seedCooldownsDisabled;

    private float elapsedSeconds;
    private long currentTick;
    private GameState gameState = GameState.PAUSED;
    private int difficultyLevel;

    private int diamondCount;
    private int coinCount;
    private int flowerPotCount;

    private boolean houseBreached;
    private Set<Integer> breachedRows = new HashSet<>();
    private int zombiesKilled;
    private int plantsLost;

    private float sunFallElapsed;
    private float sunFallDropTimer;
    private boolean skyDropEnabled = true;

    private int tideDynamicColumns;

    private WaveManagerSave waveManager = new WaveManagerSave();
    private List<PlantSave> plants = new ArrayList<>();
    private List<ZombieSave> zombies = new ArrayList<>();
    private List<SunSave> suns = new ArrayList<>();
    private List<PlantFoodSave> plantFoodPickups = new ArrayList<>();
    private List<LootSave> lootPickups = new ArrayList<>();
    private List<GraveSave> graves = new ArrayList<>();
    private List<MowerSave> mowers = new ArrayList<>();
    private List<TerrainSave> terrains = new ArrayList<>();

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public long getSavedAtEpochMs() { return savedAtEpochMs; }
    public void setSavedAtEpochMs(long savedAtEpochMs) { this.savedAtEpochMs = savedAtEpochMs; }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode == null ? Mode.ADVENTURE : mode; }

    public Chapter getChapter() { return chapter; }
    public void setChapter(Chapter chapter) { this.chapter = chapter; }

    public int getLevelId() { return levelId; }
    public void setLevelId(int levelId) { this.levelId = levelId; }

    public MiniGameType getMiniGameType() { return miniGameType; }
    public void setMiniGameType(MiniGameType miniGameType) { this.miniGameType = miniGameType; }

    public int getMiniGameStage() { return miniGameStage; }
    public void setMiniGameStage(int miniGameStage) { this.miniGameStage = miniGameStage; }

    public List<String> getSelectedPlants() { return selectedPlants; }
    public void setSelectedPlants(List<String> selectedPlants) {
        this.selectedPlants = selectedPlants == null ? new ArrayList<>() : selectedPlants;
    }

    public String getImitaterCopyTarget() { return imitaterCopyTarget; }
    public void setImitaterCopyTarget(String imitaterCopyTarget) { this.imitaterCopyTarget = imitaterCopyTarget; }

    public int getSunAmount() { return sunAmount; }
    public void setSunAmount(int sunAmount) { this.sunAmount = sunAmount; }

    public int getPlantFoodCount() { return plantFoodCount; }
    public void setPlantFoodCount(int plantFoodCount) { this.plantFoodCount = plantFoodCount; }

    public int getPersistentPlantFood() { return persistentPlantFood; }
    public void setPersistentPlantFood(int persistentPlantFood) { this.persistentPlantFood = persistentPlantFood; }

    public Map<String, Float> getSeedCooldowns() { return seedCooldowns; }
    public void setSeedCooldowns(Map<String, Float> seedCooldowns) {
        this.seedCooldowns = seedCooldowns == null ? new HashMap<>() : seedCooldowns;
    }

    public boolean isSeedCooldownsDisabled() { return seedCooldownsDisabled; }
    public void setSeedCooldownsDisabled(boolean seedCooldownsDisabled) {
        this.seedCooldownsDisabled = seedCooldownsDisabled;
    }

    public float getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(float elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }

    public long getCurrentTick() { return currentTick; }
    public void setCurrentTick(long currentTick) { this.currentTick = currentTick; }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) {
        this.gameState = gameState == null ? GameState.PAUSED : gameState;
    }

    public int getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public int getDiamondCount() { return diamondCount; }
    public void setDiamondCount(int diamondCount) { this.diamondCount = diamondCount; }

    public int getCoinCount() { return coinCount; }
    public void setCoinCount(int coinCount) { this.coinCount = coinCount; }

    public int getFlowerPotCount() { return flowerPotCount; }
    public void setFlowerPotCount(int flowerPotCount) { this.flowerPotCount = flowerPotCount; }

    public boolean isHouseBreached() { return houseBreached; }
    public void setHouseBreached(boolean houseBreached) { this.houseBreached = houseBreached; }

    public Set<Integer> getBreachedRows() { return breachedRows; }
    public void setBreachedRows(Set<Integer> breachedRows) {
        this.breachedRows = breachedRows == null ? new HashSet<>() : breachedRows;
    }

    public int getZombiesKilled() { return zombiesKilled; }
    public void setZombiesKilled(int zombiesKilled) { this.zombiesKilled = zombiesKilled; }

    public int getPlantsLost() { return plantsLost; }
    public void setPlantsLost(int plantsLost) { this.plantsLost = plantsLost; }

    public float getSunFallElapsed() { return sunFallElapsed; }
    public void setSunFallElapsed(float sunFallElapsed) { this.sunFallElapsed = sunFallElapsed; }

    public float getSunFallDropTimer() { return sunFallDropTimer; }
    public void setSunFallDropTimer(float sunFallDropTimer) { this.sunFallDropTimer = sunFallDropTimer; }

    public boolean isSkyDropEnabled() { return skyDropEnabled; }
    public void setSkyDropEnabled(boolean skyDropEnabled) { this.skyDropEnabled = skyDropEnabled; }

    public int getTideDynamicColumns() { return tideDynamicColumns; }
    public void setTideDynamicColumns(int tideDynamicColumns) { this.tideDynamicColumns = tideDynamicColumns; }

    public WaveManagerSave getWaveManager() { return waveManager; }
    public void setWaveManager(WaveManagerSave waveManager) {
        this.waveManager = waveManager == null ? new WaveManagerSave() : waveManager;
    }

    public List<PlantSave> getPlants() { return plants; }
    public void setPlants(List<PlantSave> plants) {
        this.plants = plants == null ? new ArrayList<>() : plants;
    }

    public List<ZombieSave> getZombies() { return zombies; }
    public void setZombies(List<ZombieSave> zombies) {
        this.zombies = zombies == null ? new ArrayList<>() : zombies;
    }

    public List<SunSave> getSuns() { return suns; }
    public void setSuns(List<SunSave> suns) {
        this.suns = suns == null ? new ArrayList<>() : suns;
    }

    public List<PlantFoodSave> getPlantFoodPickups() { return plantFoodPickups; }
    public void setPlantFoodPickups(List<PlantFoodSave> plantFoodPickups) {
        this.plantFoodPickups = plantFoodPickups == null ? new ArrayList<>() : plantFoodPickups;
    }

    public List<LootSave> getLootPickups() { return lootPickups; }
    public void setLootPickups(List<LootSave> lootPickups) {
        this.lootPickups = lootPickups == null ? new ArrayList<>() : lootPickups;
    }

    public List<GraveSave> getGraves() { return graves; }
    public void setGraves(List<GraveSave> graves) {
        this.graves = graves == null ? new ArrayList<>() : graves;
    }

    public List<MowerSave> getMowers() { return mowers; }
    public void setMowers(List<MowerSave> mowers) {
        this.mowers = mowers == null ? new ArrayList<>() : mowers;
    }

    public List<TerrainSave> getTerrains() { return terrains; }
    public void setTerrains(List<TerrainSave> terrains) {
        this.terrains = terrains == null ? new ArrayList<>() : terrains;
    }
}
