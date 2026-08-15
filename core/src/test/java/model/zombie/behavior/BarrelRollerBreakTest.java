package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.PushableItemType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.item.pushable.Barrel;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;
import view.gui.anim.AnimPose;
import view.gui.anim.zombie.BarrelRollerAnim;
import view.gui.anim.zombie.ZombieAnimOverrides;
import view.gui.anim.zombie.ZombieAnimRole;
import view.gui.assets.PamCatalog;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarrelRollerBreakTest {

    @Test
    void barrelBreakSpawnsTwoPirateImps() {
        Barrel barrel = new Barrel(600);
        ZombieInstance zombie = roller(barrel);
        List<ZombieInstance> spawned = new ArrayList<>();
        BarrelRollerBehavior roller = (BarrelRollerBehavior) zombie.getBehavior(
                ZombieBehaviorType.BARREL_ROLLER);
        BehaviorContext context = stub(spawned);

        roller.execute(zombie, context, 0.1f);
        assertTrue(roller.hadPushable());

        barrel.takeDamage(600);
        barrel.onDestroyed();
        roller.execute(zombie, context, 0.1f);

        assertEquals(2, spawned.size());
        assertEquals("ZombiePirateImp", spawned.get(0).getDefinition().getName());
        assertEquals("ZombiePirateImp", spawned.get(1).getDefinition().getName());
        assertEquals("ZombiePirateImp", BarrelRollerBehavior.IMP_NAME);
        assertTrue(roller.hasSpawnedImps());
        float a = spawned.get(0).getContinuousX();
        float b = spawned.get(1).getContinuousX();
        assertTrue(Math.abs(a - 4f) <= BarrelRollerBehavior.IMP_SPAWN_JITTER + 1e-4f);
        assertTrue(Math.abs(b - 4f) <= BarrelRollerBehavior.IMP_SPAWN_JITTER + 1e-4f);
        assertNotEquals(a, b, 0f);
    }

    @Test
    void unarmedClipsArePhaseTwo() {
        ZombieInstance zombie = roller(null);
        ZombieAnimOverrides overrides = ZombieAnimOverrides.createDefault();
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                "ZOMBIE_PIRATE_BARREL_PUSHER", "pusher.pam", Map.of());
        AnimPose walk = overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK);
        AnimPose eat = overrides.tryResolve(zombie, entry, ZombieAnimRole.EATING);
        AnimPose die = overrides.tryResolve(zombie, entry, ZombieAnimRole.DIE);
        assertEquals("walk2", walk.clipName());
        assertEquals("eat2", eat.clipName());
        assertEquals("die2", die.clipName());
    }

    @Test
    void armedWalkFallsThroughToDefaults() {
        Barrel barrel = new Barrel(600);
        ZombieInstance zombie = roller(barrel);
        ZombieAnimOverrides overrides = ZombieAnimOverrides.createDefault();
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                "ZOMBIE_PIRATE_BARREL_PUSHER", "pusher.pam", Map.of());
        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK));
        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.EATING));
        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.DIE));
        assertTrue(BarrelRollerAnim.hasBarrel(zombie));
    }

    private static ZombieInstance roller(Barrel barrel) {
        Zombie definition = new Zombie(
                "ZombieBarrelRoller", 190, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 300, 1, List.of(),
                PushableItemType.BARREL, null,
                List.of(ZombieBehaviorType.PUSH, ZombieBehaviorType.BARREL_ROLLER));
        ZombieInstance zombie = new ZombieInstance(definition, List.of(), barrel);
        if (barrel != null) {
            barrel.setPusher(zombie);
            barrel.setPosition(new model.game.map.Point(4, 0));
        }
        return zombie;
    }

    private static BehaviorContext stub(List<ZombieInstance> spawned) {
        Zombie impDef = new Zombie(
                "ZombiePirateImp", 190, 0.22f, 200f, ZombieSize.IMP,
                Chapter.ANCIENT_EGYPT, 50, 0, List.of(), null, null, List.of());
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "spawnZombieAt" -> {
                        ZombieInstance imp = new ZombieInstance(impDef);
                        int row = (int) args[1];
                        int col = (int) args[2];
                        imp.setGridPosition(new Point(col, row));
                        imp.setContinuousPosition(new FloatPoint(col, row));
                        spawned.add(imp);
                        yield imp;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
