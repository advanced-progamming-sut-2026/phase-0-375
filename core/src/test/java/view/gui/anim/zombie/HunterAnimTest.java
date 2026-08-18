package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.zombie.behavior.ShootBehavior;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class HunterAnimTest {

    @Test
    void throwPlaysWhileThrowingWalkFallsThrough() {
        ZombieInstance zombie = hunter();
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                "ZOMBIE_ICEAGE_HUNTER",
                "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM",
                Map.of("walk", 1f, "eat", 1f, "die", 1f, "throw", 1f, "particles", 1f));
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        HunterAnim.register(overrides);

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK));

        shoot.setSnowballsRemainingInBarrage(3);
        AnimPose pose = overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK);
        assertEquals(HunterAnim.THROW_CLIP, pose.clipName());
        assertFalse(pose.loop());
        assertEquals(ZombieAnimRole.EATING, pose.role());

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.DIE));
    }

    private static ZombieInstance hunter() {
        Zombie definition = new Zombie(
                "ZombieIceAgeHunter", 700, 0.12f, 100f, ZombieSize.NORMAL,
                Chapter.FROSTBITE_CAVES, 500, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SHOOT));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }
}
