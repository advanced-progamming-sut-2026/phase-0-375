package view.gui.lawn;

import model.enums.Chapter;
import model.enums.LevelType;
import model.game.level.LevelConfig;
import model.game.level.ProtectedPlantTile;
import model.game.level.RegularLevel;
import model.game.level.special.SaveOurSeedsLevel;
import model.game.map.Point;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectTileRendererTest {

    @Test
    void constantsMatchRequirements() {
        assertEquals("IMAGE_BACKGROUNDS_PROTECT_TILE_PROTECT_TILE_112X125", ProtectTileRenderer.PROTECT_TILE_IMAGE_ID);
        assertEquals("ProtectThePlantChallengeModule_768", ProtectTileRenderer.ATLAS_GROUP);
        assertEquals("ATLASIMAGE_ATLAS_PROTECTTHEPLANTCHALLENGEMODULE_768_00", ProtectTileRenderer.ATLAS_PAGE);
    }

    @Test
    void detectsSaveOurSeedsLevels() {
        assertFalse(ProtectTileRenderer.isProtectLevel(null));

        LevelConfig regularConfig = new LevelConfig();
        regularConfig.setLevelId(1);
        regularConfig.setRows(5);
        regularConfig.setColumns(9);
        regularConfig.setChapter(Chapter.ANCIENT_EGYPT);
        regularConfig.setLevelType(LevelType.NORMAL);
        RegularLevel regularLevel = new RegularLevel(regularConfig);
        assertFalse(ProtectTileRenderer.isProtectLevel(regularLevel));

        LevelConfig sosConfig = new LevelConfig();
        sosConfig.setLevelId(3);
        sosConfig.setRows(5);
        sosConfig.setColumns(9);
        sosConfig.setChapter(Chapter.ANCIENT_EGYPT);
        sosConfig.setLevelType(LevelType.SAVE_OUR_SEEDS);
        SaveOurSeedsLevel sosLevel = new SaveOurSeedsLevel(sosConfig);
        assertTrue(ProtectTileRenderer.isProtectLevel(sosLevel));

        LevelConfig customProtectConfig = new LevelConfig();
        customProtectConfig.setLevelId(2);
        customProtectConfig.setRows(5);
        customProtectConfig.setColumns(9);
        customProtectConfig.setChapter(Chapter.ANCIENT_EGYPT);
        customProtectConfig.setLevelType(LevelType.NORMAL);
        customProtectConfig.setProtectedPlants(List.of(new ProtectedPlantTile(new Point(1, 1), "Wall-nut")));
        RegularLevel customLevel = new RegularLevel(customProtectConfig);
        assertTrue(ProtectTileRenderer.isProtectLevel(customLevel));
    }

    @Test
    void extractsProtectedTilePositionsCorrectly() {
        LevelConfig sosConfig = new LevelConfig();
        sosConfig.setLevelId(3);
        sosConfig.setRows(5);
        sosConfig.setColumns(9);
        sosConfig.setChapter(Chapter.ANCIENT_EGYPT);
        sosConfig.setLevelType(LevelType.SAVE_OUR_SEEDS);
        sosConfig.setProtectedPlants(List.of(
                new ProtectedPlantTile(new Point(1, 1), "Wall-nut"),
                new ProtectedPlantTile(new Point(1, 3), "Peashooter")
        ));
        SaveOurSeedsLevel sosLevel = new SaveOurSeedsLevel(sosConfig);

        List<Point> positions = ProtectTileRenderer.getProtectedTilePositions(sosLevel);
        assertEquals(2, positions.size());
        assertEquals(1, positions.get(0).getX());
        assertEquals(1, positions.get(0).getY());
        assertEquals(1, positions.get(1).getX());
        assertEquals(3, positions.get(1).getY());
    }

    @Test
    void extractsLegacyProtectedPlantPositions() {
        LevelConfig sosConfig = new LevelConfig();
        sosConfig.setLevelId(3);
        sosConfig.setRows(5);
        sosConfig.setColumns(9);
        sosConfig.setChapter(Chapter.ANCIENT_EGYPT);
        sosConfig.setProtectedPlantPositions(List.of(new Point(2, 4)));
        RegularLevel level = new RegularLevel(sosConfig);

        List<Point> positions = ProtectTileRenderer.getProtectedTilePositions(level);
        assertEquals(1, positions.size());
        assertEquals(2, positions.get(0).getX());
        assertEquals(4, positions.get(0).getY());
    }

    @Test
    void tileCenteringCalculations() {
        LawnLayout layout = LawnLayout.frontLawnDefault();
        float[] center = layout.centerOf(1, 1);
        float w = 72f;
        float h = 80f;
        float drawX = center[0] - w * 0.5f;
        float drawY = center[1] - h * 0.5f;

        assertEquals(layout.centerX(1) - w * 0.5f, drawX, 1e-4f);
        assertEquals(layout.centerY(1) - h * 0.5f, drawY, 1e-4f);
    }
}
