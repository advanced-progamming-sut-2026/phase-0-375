package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
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

class DodoFlyTest {

    private static final float TICK = 0.1f;

    @Test
    void nutStartsTakeoffThenLoopsThenLands() {
        ZombieInstance zombie = dodo();
        FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);
        PlantInstance nut = wallnut();

        fly.execute(zombie, stubContext(nut), TICK);
        assertTrue(fly.isFlying());
        assertEquals(FlyBehavior.FlyPhase.TAKEOFF, fly.getPhase());
        assertEquals(TICK, fly.getFlyTimer(), 1e-4f);

        runFor(fly, zombie, stubContext(nut), FlyBehavior.FLY_START_DURATION);
        assertEquals(FlyBehavior.FlyPhase.FLYING, fly.getPhase());
        assertTrue(fly.isFlying());

        fly.execute(zombie, stubContext(null), TICK);
        assertEquals(FlyBehavior.FlyPhase.LANDING, fly.getPhase());
        assertTrue(fly.isFlying());

        runFor(fly, zombie, stubContext(null), FlyBehavior.FLY_END_DURATION);
        assertEquals(FlyBehavior.FlyPhase.LANDED, fly.getPhase());
        assertFalse(fly.isFlying());
    }

    @Test
    void consecutiveNutsStayAirborne() {
        ZombieInstance zombie = dodo();
        FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);

        runFor(fly, zombie, stubContext(wallnut()), FlyBehavior.FLY_START_DURATION);
        assertEquals(FlyBehavior.FlyPhase.FLYING, fly.getPhase());

        fly.execute(zombie, stubContext(wallnut()), TICK);
        assertEquals(FlyBehavior.FlyPhase.FLYING, fly.getPhase());
        assertTrue(fly.isFlying());
    }

    @Test
    void landingOverAnotherNutResumesFly() {
        ZombieInstance zombie = dodo();
        FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);

        runFor(fly, zombie, stubContext(wallnut()), FlyBehavior.FLY_START_DURATION);
        fly.execute(zombie, stubContext(null), TICK);
        assertEquals(FlyBehavior.FlyPhase.LANDING, fly.getPhase());

        fly.execute(zombie, stubContext(wallnut()), TICK);
        assertEquals(FlyBehavior.FlyPhase.FLYING, fly.getPhase());
    }

    @Test
    void tallNutAndPeashooterStayGrounded() {
        ZombieInstance zombie = dodo();
        FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);

        fly.execute(zombie, stubContext(tallNut()), TICK);
        assertFalse(fly.isFlying());

        fly.execute(zombie, stubContext(peashooter()), TICK);
        assertFalse(fly.isFlying());
    }

    @Test
    void trapIsFlownOver() {
        ZombieInstance zombie = dodo();
        FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);

        fly.execute(zombie, stubContext(mine()), TICK);
        assertTrue(fly.isFlying());
        assertEquals(FlyBehavior.FlyPhase.TAKEOFF, fly.getPhase());
    }

    private static void runFor(FlyBehavior fly, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            fly.execute(zombie, context, TICK);
        }
    }

    private static ZombieInstance dodo() {
        Zombie definition = new Zombie(
                "ZombieIceAgeDodo", 490, 0.3f, 100f, ZombieSize.NORMAL,
                Chapter.FROSTBITE_CAVES, 600, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.FLY));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setGridPosition(new Point(4, 0));
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }

    private static PlantInstance wallnut() {
        return new PlantInstance(new Plant(
                1, "Wall-nut", PlantCategory.WALL_NUT, List.of(), 50, 4000, 0,
                0f, 0f, null, 0f, null, 0f, null));
    }

    private static PlantInstance tallNut() {
        return new PlantInstance(new Plant(
                2, "Tall-nut", PlantCategory.WALL_NUT, List.of(), 125, 8000, 0,
                0f, 0f, null, 0f, null, 0f, null));
    }

    private static PlantInstance peashooter() {
        return new PlantInstance(new Plant(
                3, "Peashooter", PlantCategory.SHOOTER, List.of(), 100, 300, 0,
                0f, 0f, null, 0f, null, 0f, null));
    }

    private static PlantInstance mine() {
        return new PlantInstance(new Plant(
                4, "Potato Mine", PlantCategory.EXPLOSIVE, List.of(PlantTags.TRAP),
                25, 300, 0, 0f, 0f, null, 0f, null, 0f, null));
    }

    private static BehaviorContext stubContext(PlantInstance plant) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRowCount" -> 5;
                    case "getColumnCount" -> 9;
                    case "getPlantAt" -> plant;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
