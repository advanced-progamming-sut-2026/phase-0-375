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
    private List<Point> waterTiles;                // explicit water tile positions
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

    public List<Point> getProtectedPlantPositions() {
        return protectedPlantPositions;
    }

    public List<Point> getWaterTiles() {
        return waterTiles;
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

    public void setProtectedPlantPositions(List<Point> protectedPlantPositions) {
        this.protectedPlantPositions = protectedPlantPositions;
    }

    public void setWaterTiles(List<Point> waterTiles) {
        this.waterTiles = waterTiles;
    }

    public void setHasNightEffect(boolean hasNightEffect) {
        this.hasNightEffect = hasNightEffect;
    }
}
