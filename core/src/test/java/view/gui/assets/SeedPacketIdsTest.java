package view.gui.assets;

import org.junit.jupiter.api.Test;

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
    void knownMismatchesUseAliases() {
        assertEquals("IMAGE_UI_PACKETS_ICEBURG", SeedPacketIds.portraitId("Iceberg Lettuce"));
        assertEquals("IMAGE_UI_PACKETS_CHERRY_BOMB", SeedPacketIds.portraitId("Cherry Bomb"));
        assertEquals("IMAGE_UI_PACKETS_PEPPERMINT", SeedPacketIds.portraitId("Pierce-mint"));
        assertEquals("IMAGE_UI_PACKETS_MEGAGATLING", SeedPacketIds.portraitId("Mega Gatling Pea"));
        assertEquals("IMAGE_UI_PACKETS_WALLNUT", SeedPacketIds.portraitId("Giant Wall-nut"));
        assertEquals("IMAGE_UI_PACKETS_EXPLODEONUT", SeedPacketIds.portraitId("Explode-o-nut"));
    }
}
