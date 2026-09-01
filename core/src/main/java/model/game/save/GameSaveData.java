package model.game.save;

import model.enums.Chapter;
import model.enums.GameState;
import model.enums.GroundType;
import model.enums.LootPickupKind;
import model.enums.MiniGameType;
import model.enums.PlantAbilityType;
import model.enums.PlantState;
import model.enums.SunType;
import model.enums.WaveManagerPhase;
import model.enums.WaveState;
import model.enums.ZombieState;
import model.item.Grave.GraveType;

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

    // --- Nested DTOs ---

    public static class AbilitySave {
        private PlantAbilityType abilityType;
        private float cooldownRemaining;
        private float chargeProgress;
        private boolean active = true;
        private boolean armed;
        private float armedElapsed;
        private int growthStage;
        private boolean digesting;
        private float digestRemaining;
        private int shotOrdinal;

        public PlantAbilityType getAbilityType() { return abilityType; }
        public void setAbilityType(PlantAbilityType abilityType) { this.abilityType = abilityType; }
        public float getCooldownRemaining() { return cooldownRemaining; }
        public void setCooldownRemaining(float cooldownRemaining) { this.cooldownRemaining = cooldownRemaining; }
        public float getChargeProgress() { return chargeProgress; }
        public void setChargeProgress(float chargeProgress) { this.chargeProgress = chargeProgress; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public boolean isArmed() { return armed; }
        public void setArmed(boolean armed) { this.armed = armed; }
        public float getArmedElapsed() { return armedElapsed; }
        public void setArmedElapsed(float armedElapsed) { this.armedElapsed = armedElapsed; }
        public int getGrowthStage() { return growthStage; }
        public void setGrowthStage(int growthStage) { this.growthStage = growthStage; }
        public boolean isDigesting() { return digesting; }
        public void setDigesting(boolean digesting) { this.digesting = digesting; }
        public float getDigestRemaining() { return digestRemaining; }
        public void setDigestRemaining(float digestRemaining) { this.digestRemaining = digestRemaining; }
        public int getShotOrdinal() { return shotOrdinal; }
        public void setShotOrdinal(int shotOrdinal) { this.shotOrdinal = shotOrdinal; }
    }

    public static class PlantSave {
        private String definitionName;
        private int row;
        private int col;
        private PlantState state = PlantState.IDLE;
        private int currentHp;
        private int armorHp;
        private int armorMaxHp;
        private int level = 1;
        private float currentRecharge;
        private boolean plantFoodActive;
        private float plantFoodDurationRemaining;
        private float lifespanRemaining = -1f;
        private float lifespanTotal;
        private int stackCount = 1;
        private String imitateTarget;
        private float transformCountdown = -1f;
        private int iceHp;
        private boolean octopusCoating;
        private int freezeHitCount;
        private List<AbilitySave> abilities = new ArrayList<>();

        public String getDefinitionName() { return definitionName; }
        public void setDefinitionName(String definitionName) { this.definitionName = definitionName; }
        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }
        public int getCol() { return col; }
        public void setCol(int col) { this.col = col; }
        public PlantState getState() { return state; }
        public void setState(PlantState state) { this.state = state; }
        public int getCurrentHp() { return currentHp; }
        public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
        public int getArmorHp() { return armorHp; }
        public void setArmorHp(int armorHp) { this.armorHp = armorHp; }
        public int getArmorMaxHp() { return armorMaxHp; }
        public void setArmorMaxHp(int armorMaxHp) { this.armorMaxHp = armorMaxHp; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public float getCurrentRecharge() { return currentRecharge; }
        public void setCurrentRecharge(float currentRecharge) { this.currentRecharge = currentRecharge; }
        public boolean isPlantFoodActive() { return plantFoodActive; }
        public void setPlantFoodActive(boolean plantFoodActive) { this.plantFoodActive = plantFoodActive; }
        public float getPlantFoodDurationRemaining() { return plantFoodDurationRemaining; }
        public void setPlantFoodDurationRemaining(float plantFoodDurationRemaining) {
            this.plantFoodDurationRemaining = plantFoodDurationRemaining;
        }
        public float getLifespanRemaining() { return lifespanRemaining; }
        public void setLifespanRemaining(float lifespanRemaining) { this.lifespanRemaining = lifespanRemaining; }
        public float getLifespanTotal() { return lifespanTotal; }
        public void setLifespanTotal(float lifespanTotal) { this.lifespanTotal = lifespanTotal; }
        public int getStackCount() { return stackCount; }
        public void setStackCount(int stackCount) { this.stackCount = stackCount; }
        public String getImitateTarget() { return imitateTarget; }
        public void setImitateTarget(String imitateTarget) { this.imitateTarget = imitateTarget; }
        public float getTransformCountdown() { return transformCountdown; }
        public void setTransformCountdown(float transformCountdown) { this.transformCountdown = transformCountdown; }
        public int getIceHp() { return iceHp; }
        public void setIceHp(int iceHp) { this.iceHp = iceHp; }
        public boolean isOctopusCoating() { return octopusCoating; }
        public void setOctopusCoating(boolean octopusCoating) { this.octopusCoating = octopusCoating; }
        public int getFreezeHitCount() { return freezeHitCount; }
        public void setFreezeHitCount(int freezeHitCount) { this.freezeHitCount = freezeHitCount; }
        public List<AbilitySave> getAbilities() { return abilities; }
        public void setAbilities(List<AbilitySave> abilities) {
            this.abilities = abilities == null ? new ArrayList<>() : abilities;
        }
    }

    public static class ArmorSave {
        private String armorType;
        private int currentHealth;

        public String getArmorType() { return armorType; }
        public void setArmorType(String armorType) { this.armorType = armorType; }
        public int getCurrentHealth() { return currentHealth; }
        public void setCurrentHealth(int currentHealth) { this.currentHealth = currentHealth; }
    }

    public static class ZombieSave {
        private String definitionName;
        private int gridCol;
        private int gridRow;
        private float continuousX;
        private float continuousY;
        private ZombieState state = ZombieState.WALKING;
        private int currentHp;
        private float currentSpeed;
        private float speedModifier = 1f;
        private boolean glowing;
        private int chillLevel;
        private float chillStackTimer;
        private boolean buttered;
        private boolean hypnotized;
        private boolean movingBackward;
        private boolean countsTowardCurrentWave;
        private List<ArmorSave> armors = new ArrayList<>();

        public String getDefinitionName() { return definitionName; }
        public void setDefinitionName(String definitionName) { this.definitionName = definitionName; }
        public int getGridCol() { return gridCol; }
        public void setGridCol(int gridCol) { this.gridCol = gridCol; }
        public int getGridRow() { return gridRow; }
        public void setGridRow(int gridRow) { this.gridRow = gridRow; }
        public float getContinuousX() { return continuousX; }
        public void setContinuousX(float continuousX) { this.continuousX = continuousX; }
        public float getContinuousY() { return continuousY; }
        public void setContinuousY(float continuousY) { this.continuousY = continuousY; }
        public ZombieState getState() { return state; }
        public void setState(ZombieState state) { this.state = state; }
        public int getCurrentHp() { return currentHp; }
        public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
        public float getCurrentSpeed() { return currentSpeed; }
        public void setCurrentSpeed(float currentSpeed) { this.currentSpeed = currentSpeed; }
        public float getSpeedModifier() { return speedModifier; }
        public void setSpeedModifier(float speedModifier) { this.speedModifier = speedModifier; }
        public boolean isGlowing() { return glowing; }
        public void setGlowing(boolean glowing) { this.glowing = glowing; }
        public int getChillLevel() { return chillLevel; }
        public void setChillLevel(int chillLevel) { this.chillLevel = chillLevel; }
        public float getChillStackTimer() { return chillStackTimer; }
        public void setChillStackTimer(float chillStackTimer) { this.chillStackTimer = chillStackTimer; }
        public boolean isButtered() { return buttered; }
        public void setButtered(boolean buttered) { this.buttered = buttered; }
        public boolean isHypnotized() { return hypnotized; }
        public void setHypnotized(boolean hypnotized) { this.hypnotized = hypnotized; }
        public boolean isMovingBackward() { return movingBackward; }
        public void setMovingBackward(boolean movingBackward) { this.movingBackward = movingBackward; }
        public boolean isCountsTowardCurrentWave() { return countsTowardCurrentWave; }
        public void setCountsTowardCurrentWave(boolean countsTowardCurrentWave) {
            this.countsTowardCurrentWave = countsTowardCurrentWave;
        }
        public List<ArmorSave> getArmors() { return armors; }
        public void setArmors(List<ArmorSave> armors) {
            this.armors = armors == null ? new ArrayList<>() : armors;
        }
    }

    public static class SunSave {
        private SunType type = SunType.NORMAL;
        private int value;
        private int x;
        private int y;
        private float offsetX;
        private float offsetY;
        private float fallRemaining;
        private float fallDuration;

        public SunType getType() { return type; }
        public void setType(SunType type) { this.type = type; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public float getOffsetX() { return offsetX; }
        public void setOffsetX(float offsetX) { this.offsetX = offsetX; }
        public float getOffsetY() { return offsetY; }
        public void setOffsetY(float offsetY) { this.offsetY = offsetY; }
        public float getFallRemaining() { return fallRemaining; }
        public void setFallRemaining(float fallRemaining) { this.fallRemaining = fallRemaining; }
        public float getFallDuration() { return fallDuration; }
        public void setFallDuration(float fallDuration) { this.fallDuration = fallDuration; }
    }

    public static class PlantFoodSave {
        private int x;
        private int y;
        private float offsetX;
        private float offsetY;

        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public float getOffsetX() { return offsetX; }
        public void setOffsetX(float offsetX) { this.offsetX = offsetX; }
        public float getOffsetY() { return offsetY; }
        public void setOffsetY(float offsetY) { this.offsetY = offsetY; }
    }

    public static class LootSave {
        private LootPickupKind kind;
        private int amount;
        private int x;
        private int y;
        private float offsetX;
        private float offsetY;

        public LootPickupKind getKind() { return kind; }
        public void setKind(LootPickupKind kind) { this.kind = kind; }
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public float getOffsetX() { return offsetX; }
        public void setOffsetX(float offsetX) { this.offsetX = offsetX; }
        public float getOffsetY() { return offsetY; }
        public void setOffsetY(float offsetY) { this.offsetY = offsetY; }
    }

    public static class GraveSave {
        private int row;
        private int col;
        private int hp;
        private GraveType type = GraveType.PLAIN;

        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }
        public int getCol() { return col; }
        public void setCol(int col) { this.col = col; }
        public int getHp() { return hp; }
        public void setHp(int hp) { this.hp = hp; }
        public GraveType getType() { return type; }
        public void setType(GraveType type) { this.type = type; }
    }

    public static class MowerSave {
        private int row;
        private boolean present = true;
        private boolean active = true;
        private boolean triggered;
        private boolean sweeping;
        private float xPosition;
        private float transitionElapsed;

        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }
        public boolean isPresent() { return present; }
        public void setPresent(boolean present) { this.present = present; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public boolean isTriggered() { return triggered; }
        public void setTriggered(boolean triggered) { this.triggered = triggered; }
        public boolean isSweeping() { return sweeping; }
        public void setSweeping(boolean sweeping) { this.sweeping = sweeping; }
        public float getXPosition() { return xPosition; }
        public void setXPosition(float xPosition) { this.xPosition = xPosition; }
        public float getTransitionElapsed() { return transitionElapsed; }
        public void setTransitionElapsed(float transitionElapsed) { this.transitionElapsed = transitionElapsed; }
    }

    public static class TerrainSave {
        private int row;
        private int col;
        private GroundType groundType;
        private String kind; // ICE / FIRE / CRATER / NONE
        private int iceHp;
        private boolean iceMelted;
        private String frozenZombieName;
        private float fireRemaining;

        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }
        public int getCol() { return col; }
        public void setCol(int col) { this.col = col; }
        public GroundType getGroundType() { return groundType; }
        public void setGroundType(GroundType groundType) { this.groundType = groundType; }
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public int getIceHp() { return iceHp; }
        public void setIceHp(int iceHp) { this.iceHp = iceHp; }
        public boolean isIceMelted() { return iceMelted; }
        public void setIceMelted(boolean iceMelted) { this.iceMelted = iceMelted; }
        public String getFrozenZombieName() { return frozenZombieName; }
        public void setFrozenZombieName(String frozenZombieName) { this.frozenZombieName = frozenZombieName; }
        public float getFireRemaining() { return fireRemaining; }
        public void setFireRemaining(float fireRemaining) { this.fireRemaining = fireRemaining; }
    }

    public static class EntryRuntimeSave {
        private boolean activated;
        private float firstSpawnAt;
        private int remainingSpawns;
        private float nextSpawnAt;
        private boolean exhausted;
        private boolean groupVolleyFired;

        public boolean isActivated() { return activated; }
        public void setActivated(boolean activated) { this.activated = activated; }
        public float getFirstSpawnAt() { return firstSpawnAt; }
        public void setFirstSpawnAt(float firstSpawnAt) { this.firstSpawnAt = firstSpawnAt; }
        public int getRemainingSpawns() { return remainingSpawns; }
        public void setRemainingSpawns(int remainingSpawns) { this.remainingSpawns = remainingSpawns; }
        public float getNextSpawnAt() { return nextSpawnAt; }
        public void setNextSpawnAt(float nextSpawnAt) { this.nextSpawnAt = nextSpawnAt; }
        public boolean isExhausted() { return exhausted; }
        public void setExhausted(boolean exhausted) { this.exhausted = exhausted; }
        public boolean isGroupVolleyFired() { return groupVolleyFired; }
        public void setGroupVolleyFired(boolean groupVolleyFired) { this.groupVolleyFired = groupVolleyFired; }
    }

    public static class WaveSave {
        private WaveState state = WaveState.PENDING;
        private float waveClock;
        private List<EntryRuntimeSave> entries = new ArrayList<>();

        public WaveState getState() { return state; }
        public void setState(WaveState state) { this.state = state; }
        public float getWaveClock() { return waveClock; }
        public void setWaveClock(float waveClock) { this.waveClock = waveClock; }
        public List<EntryRuntimeSave> getEntries() { return entries; }
        public void setEntries(List<EntryRuntimeSave> entries) {
            this.entries = entries == null ? new ArrayList<>() : entries;
        }
    }

    public static class WaveManagerSave {
        private int currentWaveIndex;
        private WaveManagerPhase phase = WaveManagerPhase.WAITING_FOR_NEXT_WAVE;
        private float interWaveTimer;
        private int currentWaveTotal;
        private int currentWaveKilled;
        private float maxReportedProgress;
        private List<WaveSave> waves = new ArrayList<>();

        public int getCurrentWaveIndex() { return currentWaveIndex; }
        public void setCurrentWaveIndex(int currentWaveIndex) { this.currentWaveIndex = currentWaveIndex; }
        public WaveManagerPhase getPhase() { return phase; }
        public void setPhase(WaveManagerPhase phase) { this.phase = phase; }
        public float getInterWaveTimer() { return interWaveTimer; }
        public void setInterWaveTimer(float interWaveTimer) { this.interWaveTimer = interWaveTimer; }
        public int getCurrentWaveTotal() { return currentWaveTotal; }
        public void setCurrentWaveTotal(int currentWaveTotal) { this.currentWaveTotal = currentWaveTotal; }
        public int getCurrentWaveKilled() { return currentWaveKilled; }
        public void setCurrentWaveKilled(int currentWaveKilled) { this.currentWaveKilled = currentWaveKilled; }
        public float getMaxReportedProgress() { return maxReportedProgress; }
        public void setMaxReportedProgress(float maxReportedProgress) {
            this.maxReportedProgress = maxReportedProgress;
        }
        public List<WaveSave> getWaves() { return waves; }
        public void setWaves(List<WaveSave> waves) {
            this.waves = waves == null ? new ArrayList<>() : waves;
        }
    }
}
