package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.zombie.behavior.ShootBehavior;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorerAnimTest {

    @Test
    void torchPartsFollowLitFlagOnDefaultClips() {
        ZombieInstance zombie = explorer();
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                "ZOMBIE_EXPLORER",
                "768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM",
                Map.of("walk", 1f, "eat", 1f, "die", 1f));
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        ExplorerAnim.register(overrides);

        AnimPose lit = overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK);
        assertEquals("walk", lit.clipName());
        assertTrue(lit.loop());
        assertEquals(Boolean.TRUE, lit.visibility().get(ExplorerAnim.TORCH_END_LIT));
        assertEquals(Boolean.TRUE, lit.visibility().get("torch_fire_frame_01"));

        shoot.extinguishTorch();
        AnimPose out = overrides.tryResolve(zombie, entry, ZombieAnimRole.EATING);
        assertEquals("eat", out.clipName());
        assertEquals(Boolean.FALSE, out.visibility().get(ExplorerAnim.TORCH_END_LIT));
        assertEquals(Boolean.FALSE, out.visibility().get("torch_fire_frame_01"));
        assertEquals(Boolean.FALSE, out.visibility().get(ExplorerAnim.TORCH_FIRE_FIRST_FRAME));
        assertFalse(ExplorerAnim.isTorchLitPart("zombie_skull"));
        assertTrue(ExplorerAnim.isTorchLitPart("torch_fire_frame_07"));
        assertTrue(ExplorerAnim.isTorchLitPart(ExplorerAnim.TORCH_FIRE_FIRST_FRAME));
    }

    private static ZombieInstance explorer() {
        Zombie definition = new Zombie(
                "ZombieExplorer", 250, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 250, 1, java.util.List.of(), null, null,
                java.util.List.of(ZombieBehaviorType.SHOOT));
        return new ZombieInstance(definition);
    }
}
