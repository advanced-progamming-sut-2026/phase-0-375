package view.gui.assets;

import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.plant.definition.Plant;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.graphics.Color;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeedPacketIdsTest {
    @Test
    void compactNameMatchesAtlasSuffix() {
        assertEquals("IMAGE_UI_PACKETS_PEASHOOTER", SeedPacketIds.portraitId("Peashooter"));
        assertEquals("IMAGE_UI_PACKETS_KERNELPULT", SeedPacketIds.portraitId("Kernel-pult"));
        assertEquals("IMAGE_UI_PACKETS_WALLNUT", SeedPacketIds.portraitId("Wall-nut"));
        assertEquals("IMAGE_UI_PACKETS_TWINSUNFLOWER", SeedPacketIds.portraitId("Twin Sunflower"));
    }

    @Test
    void familyIconUsesPlantCategoryNotPlantName() {
        assertEquals(
            "IMAGE_UI_PACKETS_MINTFAM_PEASHOOTER",
            SeedPacketIds.familyIconId(plant("Peashooter", PlantCategory.SHOOTER)));
        assertEquals(
            "IMAGE_UI_PACKETS_MINTFAM_MELEE",
            SeedPacketIds.familyIconId(plant("Bonk Choy", PlantCategory.MELEE)));
        assertEquals(
            "IMAGE_UI_PACKETS_MINTFAM_DEFENSE",
            SeedPacketIds.familyIconId(plant("Wall-nut", PlantCategory.WALL_NUT)));
        assertEquals(
            "IMAGE_UI_PACKETS_MINTFAM_SHARP",
            SeedPacketIds.familyIconId(plant("Pierce-mint", PlantCategory.STRIKE_THROUGH)));
    }

    @Test
    void familyIconPrefersElementalTags() {
        assertEquals(
            "IMAGE_UI_PACKETS_MINTFAM_FIRE",
            SeedPacketIds.familyIconId(plant("Fire Peashooter", PlantCategory.SHOOTER, PlantTags.FIRE)));
        assertEquals(
            "IMAGE_UI_PACKETS_MINTFAM_POISON",
            SeedPacketIds.familyIconId(plant("Goo Peashooter", PlantCategory.SHOOTER, PlantTags.POISON)));
    }

    @Test
    void familyColorMatchesFamilySuffix() {
        Plant peashooter = plant("Peashooter", PlantCategory.SHOOTER);
        Color c = SeedPacketIds.familyColor(peashooter);
        assertEquals(0.24f, c.r, 0.01f);
        assertEquals(0.62f, c.g, 0.01f);
    }

    @Test
    void knownMismatchesUseAliases() {
        assertEquals("IMAGE_UI_PACKETS_ICEBURG", SeedPacketIds.portraitId("Iceberg Lettuce"));
        assertEquals("IMAGE_UI_PACKETS_CHERRY_BOMB", SeedPacketIds.portraitId("Cherry Bomb"));
        assertEquals("IMAGE_UI_PACKETS_PEPPERMINT", SeedPacketIds.portraitId("Pierce-mint"));
        assertEquals("IMAGE_UI_PACKETS_MEGAGATLING", SeedPacketIds.portraitId("Mega Gatling Pea"));
        assertEquals("IMAGE_UI_PACKETS_WALLNUT", SeedPacketIds.portraitId("Giant Wall-nut"));
        assertEquals("IMAGE_UI_PACKETS_EXPLODEONUT", SeedPacketIds.portraitId("Explode-o-nut"));
    }

    private static Plant plant(String name, PlantCategory category, PlantTags... tags) {
        return new Plant(0, name, category, List.of(tags), 0, 0, 0, 0f, 0f,
            null, 0f, null, 0f, null);
    }
}
