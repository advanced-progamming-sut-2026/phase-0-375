package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.zombie.behavior.BehaviorContext;
import model.zombie.behavior.SummonBehavior;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TombRaiserAnimTest {

    @Test
    void powerPlaysWhileRaisingWalkFallsThrough() {
        ZombieInstance zombie = raiser();
        SummonBehavior summon = (SummonBehavior) zombie.getBehavior(ZombieBehaviorType.SUMMON);
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                "ZOMBIE_EGYPT_TOMBRAISER",
                "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM",
                Map.of("walk", 2f, "eat", 4.1f, "die", 1.8f, "power", 3f));
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        TombRaiserAnim.register(overrides);

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK));

        summon.setCastTimer(SummonBehavior.TIME_BETWEEN_RAISINGS);
        summon.execute(zombie, emptyLawn(), 0.1f);
        assertTrue(summon.isRaising());
        AnimPose pose = overrides.tryResolve(zombie, entry, ZombieAnimRole.IDLE);
        assertEquals("power", pose.clipName());
        assertFalse(pose.loop());
        assertEquals(ZombieAnimRole.EATING, pose.role());
    }

    private static ZombieInstance raiser() {
        Zombie definition = new Zombie(
                "ZombieTombRaiser", 380, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 300, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SUMMON));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }

    private static BehaviorContext emptyLawn() {
        GraveGrid grid = new GraveGrid();
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRowCount" -> 5;
                    case "getColumnCount" -> 9;
                    case "countGravesRaisedBy" -> grid.n;
                    case "spawnGraveAt" -> {
                        grid.n++;
                        yield true;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class GraveGrid {
        int n;
    }
}
