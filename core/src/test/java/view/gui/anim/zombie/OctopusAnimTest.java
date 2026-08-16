package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.PlantCategory;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.behavior.ShootBehavior;
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

class OctopusAnimTest {

    @Test
    void tossPlaysWhileThrowingWalkFallsThroughAndHeldHidesAfterRelease() {
        PlantInstance plant = pea(3);
        ZombieInstance zombie = octopus();
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                "ZOMBIE_BEACH_OCTOPUS",
                "768/FULL/ZOMBIE/ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM",
                Map.of("walk", 1f, "eat", 1f, "die", 1f, "toss", 1f));
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        OctopusAnim.register(overrides);
        BehaviorContext lawn = lawn(plant);

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK));

        shoot.setCastTimer(ShootBehavior.OCTOPUS_THROW_INTERVAL);
        shoot.execute(zombie, lawn, 0.01f);
        AnimPose toss = overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK);
        assertEquals(OctopusAnim.TOSS_CLIP, toss.clipName());
        assertFalse(toss.loop());
        assertEquals(ZombieAnimRole.EATING, toss.role());
        assertTrue(toss.visibility() == null
                || !Boolean.FALSE.equals(toss.visibility().get(OctopusAnim.HELD_PART)));

        shoot.execute(zombie, lawn, ShootBehavior.OCTOPUS_RELEASE_AT);
        AnimPose released = overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK);
        assertEquals(Boolean.FALSE, released.visibility().get(OctopusAnim.HELD_PART));

        AnimPose die = overrides.tryResolve(zombie, entry, ZombieAnimRole.DIE);
        assertEquals("die", die.clipName());
        assertEquals(Boolean.FALSE, die.visibility().get(OctopusAnim.HELD_PART));
    }

    private static ZombieInstance octopus() {
        Zombie definition = new Zombie(
                "ZombieBeachOctopus", 910, 0.12f, 100f, ZombieSize.NORMAL,
                Chapter.BIG_WAVE_BEACH, 900, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SHOOT));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        zombie.setGridPosition(new Point(6, 0));
        zombie.setContinuousPosition(new FloatPoint(6, 0));
        return zombie;
    }

    private static PlantInstance pea(int col) {
        PlantInstance p = new PlantInstance(new Plant(
                1, "Peashooter", PlantCategory.SHOOTER, List.of(), 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
        p.setPosition(new Point(col, 0));
        return p;
    }

    private static BehaviorContext lawn(PlantInstance plant) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "getPlantsInLane" -> List.of(plant);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
