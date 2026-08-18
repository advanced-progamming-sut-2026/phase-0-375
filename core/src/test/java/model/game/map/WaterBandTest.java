package model.game.map;

import model.enums.GroundType;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.map.terrain.WaterTerrainStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterBandTest {

    @Test
    void defaultBandFloodsLastThreeColumns() {
        GameMap map = new GameMap(5, 9);
        WaterBand.applyFromRight(map, WaterBand.DEFAULT_COLUMNS);

        assertEquals(3, WaterBand.columnsFromRight(map));
        assertFalse(WaterBand.isFlooded(map.getCell(5, 0)));
        assertTrue(WaterBand.isFlooded(map.getCell(6, 0)));
        assertTrue(WaterBand.isFlooded(map.getCell(8, 4)));
        assertInstanceOf(WaterTerrainStrategy.class, map.getCell(7, 2).getTerrainStrategy());
        assertEquals(GroundType.WATER, map.getCell(6, 1).getGroundType());
    }

    @Test
    void clearDriesTheBand() {
        GameMap map = new GameMap(5, 9);
        WaterBand.applyFromRight(map, 3);
        WaterBand.applyFromRight(map, 0);

        assertEquals(0, WaterBand.columnsFromRight(map));
        assertEquals(GroundType.NORMAL, map.getCell(8, 0).getGroundType());
    }

    @Test
    void iceCellsStayPut() {
        GameMap map = new GameMap(5, 9);
        Cell iced = map.getCell(8, 2);
        iced.setGroundType(GroundType.ICE);
        iced.setTerrainStrategy(new IceTerrainStrategy());

        WaterBand.applyFromRight(map, 3);

        assertEquals(GroundType.ICE, iced.getGroundType());
        assertInstanceOf(IceTerrainStrategy.class, iced.getTerrainStrategy());
        assertTrue(WaterBand.isFlooded(map.getCell(8, 0)));
    }

    @Test
    void nudgeMovesTheLineOneColumn() {
        GameMap map = new GameMap(5, 9);
        WaterBand.applyFromRight(map, 3);
        assertEquals(4, WaterBand.nudgeFromRight(map, 1));
        assertTrue(WaterBand.isFlooded(map.getCell(5, 0)));
        assertEquals(3, WaterBand.nudgeFromRight(map, -1));
        assertFalse(WaterBand.isFlooded(map.getCell(5, 0)));
        assertTrue(WaterBand.isFlooded(map.getCell(6, 0)));
    }
}
