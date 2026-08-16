package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.zombie.behavior.BuffBehavior;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class DarkKingAnimTest {

    private static final PamCatalog.PamEntry ENTRY = new PamCatalog.PamEntry(
            "ZOMBIE_DARK_KING",
            "768/FULL/ZOMBIE/ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM",
            Map.of("intro", 3.2333f, "idle", 4f, "idle2", 2.9333f, "special", 4f, "die", 3.1667f));

    @Test
    void clipsFollowKingPhaseAndDieFallsThrough() {
        ZombieInstance zombie = king();
        BuffBehavior buff = (BuffBehavior) zombie.getBehavior(ZombieBehaviorType.BUFF);
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        DarkKingAnim.register(overrides);

        AnimPose intro = overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.WALK);
        assertEquals(DarkKingAnim.INTRO_CLIP, intro.clipName());
        assertFalse(intro.loop());

        buff.setPhase(BuffBehavior.KingPhase.IDLE);
        buff.setIdle2(false);
        AnimPose idle = overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.IDLE);
        assertEquals(DarkKingAnim.IDLE_CLIP, idle.clipName());
        assertTrue(idle.loop());

        buff.setIdle2(true);
        assertEquals(DarkKingAnim.IDLE2_CLIP,
                overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.IDLE).clipName());

        buff.setPhase(BuffBehavior.KingPhase.SPECIAL);
        AnimPose special = overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.IDLE);
        assertEquals(DarkKingAnim.SPECIAL_CLIP, special.clipName());
        assertFalse(special.loop());
        assertEquals(ZombieAnimRole.EATING, special.role());

        assertNull(overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.DIE));
    }

    private static ZombieInstance king() {
        Zombie definition = new Zombie(
                "ZombieDarkKing", 1000, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.DARK_AGES, 750, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.BUFF));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.SPAWNING);
        return zombie;
    }
}
