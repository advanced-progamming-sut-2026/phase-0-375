package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.zombie.behavior.FlyBehavior;
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

class DodoAnimTest {

    @Test
    void flyClipsFollowPhaseWalkFallsThrough() {
        ZombieInstance zombie = dodo();
        FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                "ZOMBIE_ICEAGE_DODORIDER",
                "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM",
                Map.of("walk", 3f, "eat", 3.37f, "die", 5.7f,
                        "fly_start", 0.97f, "fly_loop", 2.67f, "fly_end", 1.5f));
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        DodoAnim.register(overrides);

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK));

        fly.setPhase(FlyBehavior.FlyPhase.TAKEOFF);
        AnimPose start = overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK);
        assertEquals("fly_start", start.clipName());
        assertFalse(start.loop());
        assertEquals(ZombieAnimRole.EATING, start.role());

        fly.setPhase(FlyBehavior.FlyPhase.FLYING);
        AnimPose cruise = overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK);
        assertEquals("fly_loop", cruise.clipName());
        assertTrue(cruise.loop());
        assertEquals(ZombieAnimRole.EATING, cruise.role());

        fly.setPhase(FlyBehavior.FlyPhase.LANDING);
        AnimPose end = overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK);
        assertEquals("fly_end", end.clipName());
        assertFalse(end.loop());

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.DIE));
    }

    private static ZombieInstance dodo() {
        Zombie definition = new Zombie(
                "ZombieIceAgeDodo", 490, 0.3f, 100f, ZombieSize.NORMAL,
                Chapter.FROSTBITE_CAVES, 600, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.FLY));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }
}
