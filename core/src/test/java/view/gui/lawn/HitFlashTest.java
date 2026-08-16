package view.gui.lawn;

import model.enums.PlantCategory;
import model.item.pushable.ArcadeMachine;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitFlashTest {

    @Test
    void plantVitalityAddsHunterIceNotOctopusWrap() {
        PlantInstance plant = pea(400);
        assertEquals(400, LawnEntityRenderer.plantVitality(plant));
        plant.freeze();
        assertEquals(400 + PlantInstance.DEFAULT_ICE_HP, LawnEntityRenderer.plantVitality(plant));
        plant.freezeFromOctopus();
        assertEquals(400, LawnEntityRenderer.plantVitality(plant));
    }

    @Test
    void itemHpTracksPushableGridHp() {
        ArcadeMachine cabinet = new ArcadeMachine(100);
        assertEquals(100, LawnEntityRenderer.itemHp(cabinet));
        cabinet.takeDamage(25);
        assertEquals(75, LawnEntityRenderer.itemHp(cabinet));
    }

    @Test
    void chewTicksPulseHitsRestart() {
        assertFalse(LawnEntityRenderer.shouldRestartHitFlash(100, 99, 0.05f, 0f));
        assertFalse(LawnEntityRenderer.shouldRestartHitFlash(100, 99, 0f, 0.3f));
        assertTrue(LawnEntityRenderer.shouldRestartHitFlash(100, 99, 0f, 0f));
        assertTrue(LawnEntityRenderer.shouldRestartHitFlash(100, 80, 0.05f, 0.3f));
    }

    private static PlantInstance pea(int hp) {
        return new PlantInstance(new Plant(
                1, "Peashooter", PlantCategory.SHOOTER, List.of(), 50, hp, 0,
                0f, 0f, null, 0f, null, 0f, null));
    }
}
