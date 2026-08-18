package view.gui.lawn;

import model.enums.Chapter;
import model.enums.PlantCategory;
import model.enums.ZombieSize;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LawnEntityDrawOrderTest {

    @Test
    void rowZeroIsAboveLaterRowsSoLaterRowsPaintOnTop() {
        LawnLayout layout = LawnLayout.frontLawnDefault();
        assertTrue(layout.centerY(0) > layout.centerY(4));
        assertEquals(0, layout.rowAt(layout.centerY(0)));
        assertEquals(4, layout.rowAt(layout.centerY(4)));
        assertEquals(0, LawnEntityRenderer.clampRow(0, 5));
        assertEquals(4, LawnEntityRenderer.clampRow(4, 5));
        assertTrue(LawnEntityRenderer.clampRow(4, 5) > LawnEntityRenderer.clampRow(0, 5));
    }

    @Test
    void entityLaneFollowsGridY() {
        PlantInstance plant = new PlantInstance(new Plant(
                1, "Peashooter", PlantCategory.SHOOTER, List.of(), 50, 300, 0,
                0f, 0f, null, 0f, null, 0f, null));
        plant.setPosition(new Point(3, 4));
        assertEquals(4, LawnEntityRenderer.plantRow(plant));

        ZombieInstance zombie = new ZombieInstance(new Zombie(
                "ZombieBasic", 100, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 100, 1, List.of(), null, null, List.of()));
        zombie.setGridPosition(new Point(8, 0));
        assertEquals(0, LawnEntityRenderer.zombieRow(zombie));
        zombie.setContinuousPosition(new FloatPoint(8f, 2.4f));
        assertEquals(2, LawnEntityRenderer.zombieRow(zombie));
    }
}
