package model.game;

import model.enums.Chapter;
import model.enums.SlideDirection;

import java.util.List;
import java.util.Map;

public class LevelConfig {
    private Chapter chapter;
    private int levelId;
    private int rows;
    private int columns;
    private List<Point> initialGraves;
    private List<Point> initialIceBlocks;
    private Map<Point, SlideDirection> slideTiles;
    private List<Point> necromancyTiles;

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public int getLevelId() {
        return levelId;
    }

    public void setLevelId(int levelId) {
        this.levelId = levelId;
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

    public List<Point> getInitialGraves() {
        return initialGraves;
    }

    public void setInitialGraves(List<Point> initialGraves) {
        this.initialGraves = initialGraves;
    }

    public List<Point> getInitialIceBlocks() {
        return initialIceBlocks;
    }

    public void setInitialIceBlocks(List<Point> initialIceBlocks) {
        this.initialIceBlocks = initialIceBlocks;
    }

    public Map<Point, SlideDirection> getSlideTiles() {
        return slideTiles;
    }

    public void setSlideTiles(Map<Point, SlideDirection> slideTiles) {
        this.slideTiles = slideTiles;
    }

    public List<Point> getNecromancyTiles() {
        return necromancyTiles;
    }

    public void setNecromancyTiles(List<Point> necromancyTiles) {
        this.necromancyTiles = necromancyTiles;
    }
}
