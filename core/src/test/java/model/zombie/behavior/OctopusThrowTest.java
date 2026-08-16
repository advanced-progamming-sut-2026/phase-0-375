package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctopusThrowTest {

    @Test
    void tossReleasesThenFreezesOnLandAndHunterStillThrows() {
        PlantInstance plant = plant(3);
        ZombieInstance octopus = octopus(6);
        ShootBehavior shoot = (ShootBehavior) octopus.getBehavior(ZombieBehaviorType.SHOOT);
        BehaviorContext lawn = stubContext(plant);

        shoot.setCastTimer(ShootBehavior.OCTOPUS_THROW_INTERVAL);
        shoot.execute(octopus, lawn, 0.01f);
        assertTrue(shoot.isOctopusThrowing());
        assertEquals(ZombieState.SPECIAL_ACTION, octopus.getState());
        assertFalse(plant.isFrozen());
        assertEquals(0, shoot.getOctopusShots().size());

        shoot.execute(octopus, lawn, ShootBehavior.OCTOPUS_RELEASE_AT);
        assertTrue(shoot.hasReleasedOctopus());
        assertEquals(1, shoot.getOctopusShots().size());
        assertFalse(plant.isFrozen());

        shoot.execute(octopus, lawn, ShootBehavior.OCTOPUS_FLIGHT_DURATION);
        assertTrue(plant.isFrozen());
        assertTrue(plant.hasOctopusCoating());
        assertEquals(0, shoot.getOctopusShots().size());
        assertTrue(shoot.isOctopusThrowing());

        float left = ShootBehavior.OCTOPUS_TOSS_DURATION
                - ShootBehavior.OCTOPUS_RELEASE_AT
                - ShootBehavior.OCTOPUS_FLIGHT_DURATION;
        shoot.execute(octopus, lawn, left);
        assertFalse(shoot.isOctopusThrowing());
        assertEquals(ZombieState.WALKING, octopus.getState());

        ZombieInstance hunter = hunter(6);
        ShootBehavior snow = (ShootBehavior) hunter.getBehavior(ZombieBehaviorType.SHOOT);
        snow.setCastTimer(ShootBehavior.HUNTER_BARRAGE_INTERVAL);
        snow.execute(hunter, lawn, 0.01f);
        assertTrue(snow.isThrowing());
        assertFalse(snow.isOctopusThrowing());
    }

    @Test
    void skipsFrozenAndDeadTargetDoesNotFreeze() {
        ZombieInstance octopus = octopus(6);
        ShootBehavior shoot = (ShootBehavior) octopus.getBehavior(ZombieBehaviorType.SHOOT);

        PlantInstance frozen = plant("Wall-nut", 5, List.of());
        frozen.freeze();
        PlantInstance rear = plant("Peashooter", 3, List.of());
        tossUntilLand(shoot, octopus, stubContext(frozen, rear));
        assertFalse(frozen.hasOctopusCoating());
        assertTrue(rear.hasOctopusCoating());

        PlantInstance doomed = plant(4);
        ZombieInstance thrower = octopus(6);
        ShootBehavior second = (ShootBehavior) thrower.getBehavior(ZombieBehaviorType.SHOOT);
        second.setCastTimer(ShootBehavior.OCTOPUS_THROW_INTERVAL);
        second.execute(thrower, stubContext(doomed), 0.01f);
        second.execute(thrower, stubContext(doomed), ShootBehavior.OCTOPUS_RELEASE_AT);
        doomed.setCurrentHP(0);
        second.execute(thrower, stubContext(doomed), ShootBehavior.OCTOPUS_FLIGHT_DURATION);
        assertFalse(doomed.isFrozen());
        assertFalse(doomed.hasOctopusCoating());
    }

    private static void tossUntilLand(ShootBehavior shoot, ZombieInstance octopus,
                                      BehaviorContext lawn) {
        shoot.setCastTimer(ShootBehavior.OCTOPUS_THROW_INTERVAL);
        shoot.execute(octopus, lawn, 0.01f);
        shoot.execute(octopus, lawn, ShootBehavior.OCTOPUS_RELEASE_AT);
        shoot.execute(octopus, lawn, ShootBehavior.OCTOPUS_FLIGHT_DURATION);
    }

    private static ZombieInstance octopus(int col) {
        Zombie definition = new Zombie(
                "ZombieBeachOctopus", 910, 0.12f, 100f, ZombieSize.NORMAL,
                Chapter.BIG_WAVE_BEACH, 900, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SHOOT));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        zombie.setGridPosition(new Point(col, 0));
        zombie.setContinuousPosition(new FloatPoint(col, 0));
        return zombie;
    }

    private static ZombieInstance hunter(int col) {
        Zombie definition = new Zombie(
                "ZombieIceAgeHunter", 700, 0.12f, 100f, ZombieSize.NORMAL,
                Chapter.FROSTBITE_CAVES, 500, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SHOOT));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        zombie.setGridPosition(new Point(col, 0));
        zombie.setContinuousPosition(new FloatPoint(col, 0));
        return zombie;
    }

    private static PlantInstance plant(int col) {
        return plant("Peashooter", col, List.of());
    }

    private static PlantInstance plant(String name, int col, List<PlantTags> tags) {
        PlantInstance p = new PlantInstance(new Plant(
                1, name, PlantCategory.SHOOTER, tags, 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
        p.setPosition(new Point(col, 0));
        return p;
    }

    private static BehaviorContext stubContext(PlantInstance... plants) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "getPlantsInLane" -> List.of(plants);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
