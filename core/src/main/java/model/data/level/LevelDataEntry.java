package model.data.level;

import java.util.List;

/** Raw JSON DTO for level definitions stored under assets/data/levels. */
public class LevelDataEntry {
    private String chapter;
    private int levelId;
    private String levelType;
    private int rows = 5;
    private int columns = 9;
    private RuleData rules;
    private List<PointData> initialGraves;
    private List<PointData> initialIceBlocks;
    private List<SlideTileData> slideTiles;
    private List<PointData> necromancyTiles;
    private List<PointData> protectedPlantPositions;
    private String protectedPlantName;
    private List<ProtectedPlantData> protectedPlants;
    private List<String> forcedPlants;
    private List<String> restrictedFamilies;
    private boolean allFamiliesRestricted;
    private List<String> conveyorPlants;
    private float conveyorIntervalSeconds = 5f;
    private int conveyorCapacity = 10;
    private List<PointData> waterTiles;
    private List<PointData> lowTideTiles;
    private int tideLimitColumn = -1;
    private int deadLineColumn = -1;
    private boolean hasNightEffect;
    private List<WaveData> waves;
    private String zomboss;

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public int getLevelId() {
        return levelId;
    }

    public void setLevelId(int levelId) {
        this.levelId = levelId;
    }

    public String getLevelType() {
        return levelType;
    }

    public void setLevelType(String levelType) {
        this.levelType = levelType;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public RuleData getRules() {
        return rules;
    }

    public void setRules(RuleData rules) {
        this.rules = rules;
    }

    public List<PointData> getInitialGraves() {
        return initialGraves;
    }

    public void setInitialGraves(List<PointData> initialGraves) {
        this.initialGraves = initialGraves;
    }

    public List<PointData> getInitialIceBlocks() {
        return initialIceBlocks;
    }

    public void setInitialIceBlocks(List<PointData> initialIceBlocks) {
        this.initialIceBlocks = initialIceBlocks;
    }

    public List<SlideTileData> getSlideTiles() {
        return slideTiles;
    }

    public void setSlideTiles(List<SlideTileData> slideTiles) {
        this.slideTiles = slideTiles;
    }

    public List<PointData> getNecromancyTiles() {
        return necromancyTiles;
    }

    public void setNecromancyTiles(List<PointData> necromancyTiles) {
        this.necromancyTiles = necromancyTiles;
    }

    public List<PointData> getProtectedPlantPositions() {
        return protectedPlantPositions;
    }

    public void setProtectedPlantPositions(List<PointData> protectedPlantPositions) {
        this.protectedPlantPositions = protectedPlantPositions;
    }

    public String getProtectedPlantName() {
        return protectedPlantName;
    }

    public void setProtectedPlantName(String protectedPlantName) {
        this.protectedPlantName = protectedPlantName;
    }

    public List<ProtectedPlantData> getProtectedPlants() {
        return protectedPlants;
    }

    public void setProtectedPlants(List<ProtectedPlantData> protectedPlants) {
        this.protectedPlants = protectedPlants;
    }

    public List<String> getForcedPlants() {
        return forcedPlants;
    }

    public void setForcedPlants(List<String> forcedPlants) {
        this.forcedPlants = forcedPlants;
    }

    public List<String> getRestrictedFamilies() {
        return restrictedFamilies;
    }

    public void setRestrictedFamilies(List<String> restrictedFamilies) {
        this.restrictedFamilies = restrictedFamilies;
    }

    public boolean isAllFamiliesRestricted() {
        return allFamiliesRestricted;
    }

    public void setAllFamiliesRestricted(boolean allFamiliesRestricted) {
        this.allFamiliesRestricted = allFamiliesRestricted;
    }

    public List<String> getConveyorPlants() {
        return conveyorPlants;
    }

    public void setConveyorPlants(List<String> conveyorPlants) {
        this.conveyorPlants = conveyorPlants;
    }

    public float getConveyorIntervalSeconds() {
        return conveyorIntervalSeconds;
    }

    public void setConveyorIntervalSeconds(float conveyorIntervalSeconds) {
        this.conveyorIntervalSeconds = conveyorIntervalSeconds;
    }

    public int getConveyorCapacity() {
        return conveyorCapacity;
    }

    public void setConveyorCapacity(int conveyorCapacity) {
        this.conveyorCapacity = conveyorCapacity;
    }

    public List<PointData> getWaterTiles() {
        return waterTiles;
    }

    public void setWaterTiles(List<PointData> waterTiles) {
        this.waterTiles = waterTiles;
    }

    public List<PointData> getLowTideTiles() {
        return lowTideTiles;
    }

    public void setLowTideTiles(List<PointData> lowTideTiles) {
        this.lowTideTiles = lowTideTiles;
    }

    public int getTideLimitColumn() {
        return tideLimitColumn;
    }

    public void setTideLimitColumn(int tideLimitColumn) {
        this.tideLimitColumn = tideLimitColumn;
    }

    public int getDeadLineColumn() {
        return deadLineColumn;
    }

    public void setDeadLineColumn(int deadLineColumn) {
        this.deadLineColumn = deadLineColumn;
    }

    public boolean isHasNightEffect() {
        return hasNightEffect;
    }

    public void setHasNightEffect(boolean hasNightEffect) {
        this.hasNightEffect = hasNightEffect;
    }

    public List<WaveData> getWaves() {
        return waves;
    }

    public void setWaves(List<WaveData> waves) {
        this.waves = waves;
    }

    public String getZomboss() {
        return zomboss;
    }

    public void setZomboss(String zomboss) {
        this.zomboss = zomboss;
    }

    public static class RuleData {
        private boolean skyDropEnabled = true;
        private boolean zombiesFreezable = true;
        private int initialSun = 150;
        private double sunDropRateModifier = 1.0;
        private int minPlantsRequired = 1;
        private int maxPlantsAllowed = 8;
        private List<String> allowedPlantCategories;
        private List<Integer> forbiddenColumns;
        private List<Integer> forbiddenRows;
        private boolean sunFallsFromSky = true;
        private boolean sunProducingPlantsAllowed = true;
        private boolean plantFoodDrops = true;
        private boolean lawnMowersEnabled = true;
        private boolean shovelEnabled = true;
        private boolean allowsChoosingPlants = true;
        private boolean plantRechargeInSetup = true;
        private int deadLineColumn = -1;
        private int maxPlantDeaths = -1;
        private float timedWarLimit = -1f;
        private int timedWarTargetKills = -1;
        private float timedWarDecayInterval = 5.0f;

        public boolean isSkyDropEnabled() {
            return skyDropEnabled;
        }

        public void setSkyDropEnabled(boolean skyDropEnabled) {
            this.skyDropEnabled = skyDropEnabled;
        }

        public boolean isZombiesFreezable() {
            return zombiesFreezable;
        }

        public void setZombiesFreezable(boolean zombiesFreezable) {
            this.zombiesFreezable = zombiesFreezable;
        }

        public int getInitialSun() {
            return initialSun;
        }

        public void setInitialSun(int initialSun) {
            this.initialSun = initialSun;
        }

        public double getSunDropRateModifier() {
            return sunDropRateModifier;
        }

        public void setSunDropRateModifier(double sunDropRateModifier) {
            this.sunDropRateModifier = sunDropRateModifier;
        }

        public int getMinPlantsRequired() {
            return minPlantsRequired;
        }

        public void setMinPlantsRequired(int minPlantsRequired) {
            this.minPlantsRequired = minPlantsRequired;
        }

        public int getMaxPlantsAllowed() {
            return maxPlantsAllowed;
        }

        public void setMaxPlantsAllowed(int maxPlantsAllowed) {
            this.maxPlantsAllowed = maxPlantsAllowed;
        }

        public List<String> getAllowedPlantCategories() {
            return allowedPlantCategories;
        }

        public void setAllowedPlantCategories(List<String> allowedPlantCategories) {
            this.allowedPlantCategories = allowedPlantCategories;
        }

        public List<Integer> getForbiddenColumns() {
            return forbiddenColumns;
        }

        public void setForbiddenColumns(List<Integer> forbiddenColumns) {
            this.forbiddenColumns = forbiddenColumns;
        }

        public List<Integer> getForbiddenRows() {
            return forbiddenRows;
        }

        public void setForbiddenRows(List<Integer> forbiddenRows) {
            this.forbiddenRows = forbiddenRows;
        }

        public boolean isSunFallsFromSky() {
            return sunFallsFromSky;
        }

        public void setSunFallsFromSky(boolean sunFallsFromSky) {
            this.sunFallsFromSky = sunFallsFromSky;
        }

        public boolean isSunProducingPlantsAllowed() {
            return sunProducingPlantsAllowed;
        }

        public void setSunProducingPlantsAllowed(boolean sunProducingPlantsAllowed) {
            this.sunProducingPlantsAllowed = sunProducingPlantsAllowed;
        }

        public boolean isPlantFoodDrops() {
            return plantFoodDrops;
        }

        public void setPlantFoodDrops(boolean plantFoodDrops) {
            this.plantFoodDrops = plantFoodDrops;
        }

        public boolean isLawnMowersEnabled() {
            return lawnMowersEnabled;
        }

        public void setLawnMowersEnabled(boolean lawnMowersEnabled) {
            this.lawnMowersEnabled = lawnMowersEnabled;
        }

        public boolean isShovelEnabled() {
            return shovelEnabled;
        }

        public void setShovelEnabled(boolean shovelEnabled) {
            this.shovelEnabled = shovelEnabled;
        }

        public boolean isAllowsChoosingPlants() {
            return allowsChoosingPlants;
        }

        public void setAllowsChoosingPlants(boolean allowsChoosingPlants) {
            this.allowsChoosingPlants = allowsChoosingPlants;
        }

        public boolean isPlantRechargeInSetup() {
            return plantRechargeInSetup;
        }

        public void setPlantRechargeInSetup(boolean plantRechargeInSetup) {
            this.plantRechargeInSetup = plantRechargeInSetup;
        }

        public int getDeadLineColumn() {
            return deadLineColumn;
        }

        public void setDeadLineColumn(int deadLineColumn) {
            this.deadLineColumn = deadLineColumn;
        }

        public int getMaxPlantDeaths() {
            return maxPlantDeaths;
        }

        public void setMaxPlantDeaths(int maxPlantDeaths) {
            this.maxPlantDeaths = maxPlantDeaths;
        }

        public float getTimedWarLimit() {
            return timedWarLimit;
        }

        public void setTimedWarLimit(float timedWarLimit) {
            this.timedWarLimit = timedWarLimit;
        }

        public int getTimedWarTargetKills() {
            return timedWarTargetKills;
        }

        public void setTimedWarTargetKills(int timedWarTargetKills) {
            this.timedWarTargetKills = timedWarTargetKills;
        }

        public float getTimedWarDecayInterval() {
            return timedWarDecayInterval;
        }

        public void setTimedWarDecayInterval(float timedWarDecayInterval) {
            this.timedWarDecayInterval = timedWarDecayInterval;
        }
    }

    public static class PointData {
        private int x;
        private int y;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }

    public static class SlideTileData extends PointData {
        private String direction;

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }

    public static class ProtectedPlantData extends PointData {
        private String plant;

        public String getPlant() {
            return plant;
        }

        public void setPlant(String plant) {
            this.plant = plant;
        }
    }

    public static class WaveData {
        private int waveNumber;
        private float startDelay;
        private boolean hugeWave;
        private boolean finalWave;
        private int waveBudget = 0;
        private List<WaveEntryData> entries;
        /** Big Wave Beach: ambush cells for this wave (same set on every wave of a level). */
        private List<PointData> lowTideTiles;

        public int getWaveNumber() {
            return waveNumber;
        }

        public void setWaveNumber(int waveNumber) {
            this.waveNumber = waveNumber;
        }

        public float getStartDelay() {
            return startDelay;
        }

        public void setStartDelay(float startDelay) {
            this.startDelay = startDelay;
        }

        public boolean isHugeWave() {
            return hugeWave;
        }

        public void setHugeWave(boolean hugeWave) {
            this.hugeWave = hugeWave;
        }

        public boolean isFinalWave() {
            return finalWave;
        }

        public void setFinalWave(boolean finalWave) {
            this.finalWave = finalWave;
        }

        public int getWaveBudget() {
            return waveBudget;
        }

        public void setWaveBudget(int waveBudget) {
            this.waveBudget = waveBudget;
        }

        public List<WaveEntryData> getEntries() {
            return entries;
        }

        public void setEntries(List<WaveEntryData> entries) {
            this.entries = entries;
        }

        public List<PointData> getLowTideTiles() {
            return lowTideTiles;
        }

        public void setLowTideTiles(List<PointData> lowTideTiles) {
            this.lowTideTiles = lowTideTiles;
        }
    }

    public static class WaveEntryData {
        private List<ZombieCandidateData> pool;
        private int minCount = 1;
        private int maxCount = 1;
        private int[] allowedLanes;
        private float minSpawnDelay;
        private float maxSpawnDelay;
        private float minSpawnInterval = 1f;
        private float maxSpawnInterval = 1f;
        private String pattern = "SINGLE";
        private float streamDurationSeconds = 10f;

        public List<ZombieCandidateData> getPool() {
            return pool;
        }

        public void setPool(List<ZombieCandidateData> pool) {
            this.pool = pool;
        }

        public int getMinCount() {
            return minCount;
        }

        public void setMinCount(int minCount) {
            this.minCount = minCount;
        }

        public int getMaxCount() {
            return maxCount;
        }

        public void setMaxCount(int maxCount) {
            this.maxCount = maxCount;
        }

        public int[] getAllowedLanes() {
            return allowedLanes;
        }

        public void setAllowedLanes(int[] allowedLanes) {
            this.allowedLanes = allowedLanes;
        }

        public float getMinSpawnDelay() {
            return minSpawnDelay;
        }

        public void setMinSpawnDelay(float minSpawnDelay) {
            this.minSpawnDelay = minSpawnDelay;
        }

        public float getMaxSpawnDelay() {
            return maxSpawnDelay;
        }

        public void setMaxSpawnDelay(float maxSpawnDelay) {
            this.maxSpawnDelay = maxSpawnDelay;
        }

        public float getMinSpawnInterval() {
            return minSpawnInterval;
        }

        public void setMinSpawnInterval(float minSpawnInterval) {
            this.minSpawnInterval = minSpawnInterval;
        }

        public float getMaxSpawnInterval() {
            return maxSpawnInterval;
        }

        public void setMaxSpawnInterval(float maxSpawnInterval) {
            this.maxSpawnInterval = maxSpawnInterval;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public float getStreamDurationSeconds() {
            return streamDurationSeconds;
        }

        public void setStreamDurationSeconds(float streamDurationSeconds) {
            this.streamDurationSeconds = streamDurationSeconds;
        }
    }

    public static class ZombieCandidateData {
        private String zombie;
        private double weight = 1.0;

        public String getZombie() {
            return zombie;
        }

        public void setZombie(String zombie) {
            this.zombie = zombie;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }
}
