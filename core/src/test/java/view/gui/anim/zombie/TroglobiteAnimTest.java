package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.PushableItemType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.item.pushable.IceBlock;
import model.zombie.behavior.PushBehavior;
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

class TroglobiteAnimTest {

    @Test
    void pushPlaysWhileShovingWalkFallsThrough() {
        ZombieInstance zombie = troglobite();
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                "ZOMBIE_ICEAGE_TROGLOBITE",
                "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_TROGLOBITE/ZOMBIE_ICEAGE_TROGLOBITE.PAM",
                Map.of("walk", 1f, "eat", 1f, "die", 1f, "push", 1f, "particles", 1f));
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        TroglobiteAnim.register(overrides);

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK));

        push.setPhase(PushBehavior.PushPhase.PUSHING);
        AnimPose pose = overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK);
        assertEquals(TroglobiteAnim.PUSH_CLIP, pose.clipName());
        assertFalse(pose.loop());
        assertEquals(ZombieAnimRole.EATING, pose.role());

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.DIE));
    }

    private static ZombieInstance troglobite() {
        IceBlock block = new IceBlock(600);
        Zombie definition = new Zombie(
                TroglobiteAnim.DEFINITION_NAME, 470, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.FROSTBITE_CAVES, 600, 1, List.of(),
                PushableItemType.ICE_BLOCK, null,
                List.of(ZombieBehaviorType.PUSH));
        ZombieInstance zombie = new ZombieInstance(definition, List.of(), block);
        block.setPusher(zombie);
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }
}
