package view.gui.anim.plant;

import model.enums.PlantCategory;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import org.junit.jupiter.api.Test;
import view.gui.assets.PamCatalog;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantChillAnimTest {

    @Test
    void testPlantChillAndFreezeHits() {
        Plant definition = new Plant(
                1, "Peashooter", PlantCategory.SHOOTER, List.of(), 50, 300, 0,
                0f, 0f, null, 0f, null, 0f, null);
        PlantInstance plant = new PlantInstance(definition);

        assertEquals(0, plant.getFreezeHitCount());
        assertFalse(plant.isChilled());
        assertFalse(plant.isFrozen());

        // Hit 1: stage 1 chill
        plant.registerFreezeHit(3);
        assertEquals(1, plant.getFreezeHitCount());
        assertTrue(plant.isChilled());
        assertFalse(plant.isFrozen());

        // Hit 2: stage 2 chill
        plant.registerFreezeHit(3);
        assertEquals(2, plant.getFreezeHitCount());
        assertTrue(plant.isChilled());
        assertFalse(plant.isFrozen());

        // Hit 3: fully frozen
        plant.registerFreezeHit(3);
        assertFalse(plant.isChilled());
        assertTrue(plant.isFrozen());

        // Unfreeze resets hit count and frozen state
        plant.unfreeze();
        assertEquals(0, plant.getFreezeHitCount());
        assertFalse(plant.isChilled());
        assertFalse(plant.isFrozen());
    }

    @Test
    void testChillPamCatalogClipResolution() {
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                PlantFreezeAnim.CHILL_PAM,
                "768/FULL/EFFECTS/FROSTBITE_CHILL_PLANT/FROSTBITE_CHILL_PLANT.PAM",
                Map.of(PlantFreezeAnim.CHILL_STAGE1_CLIP, 1f, PlantFreezeAnim.CHILL_STAGE2_CLIP, 1f));

        assertEquals("FROSTBITE_CHILL_PLANT", PlantFreezeAnim.CHILL_PAM);
        assertEquals("chill_stage1", PlantFreezeAnim.CHILL_STAGE1_CLIP);
        assertEquals("chill_stage2", PlantFreezeAnim.CHILL_STAGE2_CLIP);

        PamCatalog dummyCatalog = PamCatalog.load(new com.badlogic.gdx.files.FileHandle("nonexistent"));
        assertEquals("chill_stage1", dummyCatalog.resolveClip(entry, PlantFreezeAnim.CHILL_STAGE1_CLIP));
        assertEquals("chill_stage2", dummyCatalog.resolveClip(entry, PlantFreezeAnim.CHILL_STAGE2_CLIP));
    }
}
