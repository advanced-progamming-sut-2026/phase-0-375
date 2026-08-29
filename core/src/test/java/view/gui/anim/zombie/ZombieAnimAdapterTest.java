package view.gui.anim.zombie;

import org.junit.jupiter.api.Test;
import view.gui.assets.PamCatalog;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieAnimAdapterTest {

    private static final PamCatalog.PamEntry EGYPT_BASIC_ENTRY = new PamCatalog.PamEntry(
            "ZOMBIE_EGYPT_BASIC",
            "768/FULL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM",
            Map.of("idle", 2.5f, "walk", 3.0f));

    private static final PamCatalog.PamEntry DARK_BASIC_ENTRY = new PamCatalog.PamEntry(
            "ZOMBIE_DARK_BASIC",
            "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM",
            Map.of("idle", 2.5f, "walk", 3.0f));

    @Test
    void nullDefinitionReturnsNull() {
        assertNull(ZombieAnimAdapter.almanacArmorVisibility(null, EGYPT_BASIC_ENTRY));
    }

    @Test
    void defaultZombieHasNoArmorVisibility() {
        assertNull(ZombieAnimAdapter.almanacArmorVisibility("ZombieDefault", EGYPT_BASIC_ENTRY));
    }

    @Test
    void armor1IncludesConeAndStates() {
        Map<String, Boolean> vis = ZombieAnimAdapter.almanacArmorVisibility("ZombieArmor1", EGYPT_BASIC_ENTRY);
        assertNotNull(vis);
        assertTrue(vis.getOrDefault("zombie_armor_cone_norm", false));
        assertTrue(vis.getOrDefault("_zombie_egypt_armor1_states", false));
    }

    @Test
    void armor2IncludesBucketAndStates() {
        Map<String, Boolean> vis = ZombieAnimAdapter.almanacArmorVisibility("ZombieArmor2", EGYPT_BASIC_ENTRY);
        assertNotNull(vis);
        assertTrue(vis.getOrDefault("zombie_armor_bucket_norm", false));
        assertTrue(vis.getOrDefault("_zombie_egypt_armor2_states", false));
    }

    @Test
    void armor4IncludesBrick() {
        Map<String, Boolean> vis = ZombieAnimAdapter.almanacArmorVisibility("ZombieArmor4", DARK_BASIC_ENTRY);
        assertNotNull(vis);
        assertTrue(vis.getOrDefault("zombie_armor_brick_norm", false));
    }

    @Test
    void darkArmor3IncludesCrownAndShoulderArmor() {
        Map<String, Boolean> vis = ZombieAnimAdapter.almanacArmorVisibility("ZombieDarkArmor3", DARK_BASIC_ENTRY);
        assertNotNull(vis);
        assertTrue(vis.getOrDefault("zombie_armor_crown_norm", false)
                || vis.getOrDefault("_zombie_armor_crown_states", false)
                || vis.getOrDefault("zombie_shoulder_armor", false));
    }
}