package model.game.level;

import model.enums.Chapter;
import model.enums.LevelType;
import model.enums.SlideDirection;
import model.game.map.Point;
import model.game.rule.EndGameCondition;
import model.game.rule.GameRules;
import model.game.wave.Wave;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LevelConfig {
    private Chapter chapter;
    private int levelId;
    private int rows;
    private int columns;
    private LevelType levelType;
    private GameRules rules;
    private EndGameCondition endGameCondition;
    private List<Wave> waves;
    private boolean isCompleted;
    private int starsEarned;
    private List<Point> initialGraves;
    private List<Point> initialIceBlocks;
    private Map<Point, SlideDirection> slideTiles;
    private List<Point> necromancyTiles;

    // Additional configuration for special levels
    private int deadLineColumn;                    // for Dead Line levels (-1 = no dead line)
    private List<Point> protectedPlantPositions;   // for Save Our Seeds: positions of pre-placed plants
    private String protectedPlantName;             // Save Our Seeds: (null = default)
    private List<ProtectedPlantTile> protectedPlants;  // for Save Our Seeds: which plant sits on which tile
    private List<String> forcedPlants;             // Locked Plants (forced set): the seeds the player must start with
    private Set<String> restrictedFamilies;        // for Locked Plants (family pick): families limited to one pick
    private boolean allFamiliesRestricted;         // for Locked Plants (family pick): every family limited to one pick
    private List<String> conveyorPlants;           // for Conveyor Belt: delivery pool, cycled in order
    private float conveyorIntervalSeconds;         // for Conveyor Belt: seconds between deliveries
    private int conveyorCapacity;                  // for Conveyor Belt: max seed packets waiting on the belt
    private List<Point> waterTiles;                // explicit water tile positions
    private List<Point> lowTideTiles;              // Big Wave Beach: cells that may ambush when submerged
    /// Big Wave Beach: max columns (from right) the tide may flood (-1 = static water only)
    private int tideLimitColumn = -1;
    private boolean hasNightEffect;                // for Night Ops (no sun from sky)

    // --- Getters ---

    public Chapter getChapter() {
        return chapter;
    }

    public int getLevelId() {
        return levelId;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public LevelType getLevelType() {
        return levelType;
    }

    public EndGameCondition getEndGameCondition() {
        return endGameCondition;
    }

    public GameRules getRules() {
        return rules;
    }

    public List<Wave> getWaves() {
        return waves;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public int getStarsEarned() {
        return starsEarned;
    }

    public List<Point> getInitialGraves() {
        return initialGraves;
    }

    public List<Point> getInitialIceBlocks() {
        return initialIceBlocks;
    }

    public Map<Point, SlideDirection> getSlideTiles() {
        return slideTiles;
    }

    public List<Point> getNecromancyTiles() {
        return necromancyTiles;
    }

    public int getDeadLineColumn() {
        return deadLineColumn;
    }

    public String getProtectedPlantName() {
        return protectedPlantName;
    }

    public List<ProtectedPlantTile> getProtectedPlants() {
        return protectedPlants;
    }

    public List<String> getForcedPlants() {
        return forcedPlants;
    }

    public Set<String> getRestrictedFamilies() {
        return restrictedFamilies;
    }

    public boolean isAllFamiliesRestricted() {
        return allFamiliesRestricted;
    }

    public List<String> getConveyorPlants() {
        return conveyorPlants;
    }

    public float getConveyorIntervalSeconds() {
        return conveyorIntervalSeconds;
    }

    public int getConveyorCapacity() {
        return conveyorCapacity;
    }

    public List<Point> getProtectedPlantPositions() {
        return protectedPlantPositions;
    }

    public List<Point> getWaterTiles() {
        return waterTiles;
    }

    public List<Point> getLowTideTiles() {
        return lowTideTiles;
    }

    public int getTideLimitColumn() {
        return tideLimitColumn;
    }

    public boolean isHasNightEffect() {
        return hasNightEffect;
    }

    // --- Setters ---

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public void setLevelId(int levelId) {
        this.levelId = levelId;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public void setLevelType(LevelType levelType) {
        this.levelType = levelType;
    }

    public void setEndGameCondition(EndGameCondition endGameCondition) {
        this.endGameCondition = endGameCondition;
    }

    public void setRules(GameRules rules) {
        this.rules = rules;
    }

    public void setWaves(List<Wave> waves) {
        this.waves = waves;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public void setStarsEarned(int starsEarned) {
        this.starsEarned = starsEarned;
    }

    public void setInitialGraves(List<Point> initialGraves) {
        this.initialGraves = initialGraves;
    }

    public void setInitialIceBlocks(List<Point> initialIceBlocks) {
        this.initialIceBlocks = initialIceBlocks;
    }

    public void setSlideTiles(Map<Point, SlideDirection> slideTiles) {
        this.slideTiles = slideTiles;
    }

    public void setNecromancyTiles(List<Point> necromancyTiles) {
        this.necromancyTiles = necromancyTiles;
    }

    public void setDeadLineColumn(int deadLineColumn) {
        this.deadLineColumn = deadLineColumn;
    }

    public void setProtectedPlantName(String protectedPlantName) {
        this.protectedPlantName = protectedPlantName;
    }

    public void setProtectedPlants(List<ProtectedPlantTile> protectedPlants) {
        this.protectedPlants = protectedPlants;
    }

    public void setForcedPlants(List<String> forcedPlants) {
        this.forcedPlants = forcedPlants;
    }

    public void setRestrictedFamilies(Set<String> restrictedFamilies) {
        this.restrictedFamilies = restrictedFamilies;
    }

    public void setAllFamiliesRestricted(boolean allFamiliesRestricted) {
        this.allFamiliesRestricted = allFamiliesRestricted;
    }

    public void setConveyorPlants(List<String> conveyorPlants) {
        this.conveyorPlants = conveyorPlants;
    }

    public void setConveyorIntervalSeconds(float conveyorIntervalSeconds) {
        this.conveyorIntervalSeconds = conveyorIntervalSeconds;
    }

    public void setConveyorCapacity(int conveyorCapacity) {
        this.conveyorCapacity = conveyorCapacity;
    }

    public void setProtectedPlantPositions(List<Point> protectedPlantPositions) {
        this.protectedPlantPositions = protectedPlantPositions;
    }

    public void setWaterTiles(List<Point> waterTiles) {
        this.waterTiles = waterTiles;
    }

    public void setLowTideTiles(List<Point> lowTideTiles) {
        this.lowTideTiles = lowTideTiles;
    }

    public void setTideLimitColumn(int tideLimitColumn) {
        this.tideLimitColumn = tideLimitColumn;
    }

    public void setHasNightEffect(boolean hasNightEffect) {
        this.hasNightEffect = hasNightEffect;
    }
}
