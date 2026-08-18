package model.zombie.behavior;

import model.enums.PlantCategory;
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

class JumpBehaviorTest {

    private static final float TICK = 0.1f;
    private static final int SPAWN_COL = 8;

    @Test
    void fuseThenBlastoffThenLandAtHouseWalkingRight() {
        ZombieInstance zombie = prospector(SPAWN_COL);
        JumpBehavior jump = jumpOf(zombie);
        BehaviorContext context = stubContext(null);

        assertEquals(JumpBehavior.JumpPhase.COUNTDOWN, jump.getPhase());
        assertEquals(JumpBehavior.DYNAMITE_BURNING_01, jump.dynamitePart());

        runFor(jump, zombie, context, JumpBehavior.LAUNCH_COUNTDOWN - TICK);
        assertEquals(JumpBehavior.JumpPhase.COUNTDOWN, jump.getPhase());
        assertEquals(JumpBehavior.DYNAMITE_BURNT, jump.dynamitePart());

        jump.execute(zombie, context, TICK);
        assertEquals(JumpBehavior.JumpPhase.JUMPING, jump.getPhase());
        assertEquals(ZombieState.SPECIAL_ACTION, zombie.getState());
        assertTrue(jump.hasLaunched());
        assertEquals(SPAWN_COL, zombie.getContinuousX(), 0.01f);

        runFor(jump, zombie, context, 0.2f);
        assertEquals(SPAWN_COL, zombie.getContinuousX(), 0.01f);
        assertEquals(0f, jump.heightPx(), 0.01f);

        runFor(jump, zombie, context, JumpBehavior.TIME_TO_TRAVEL);
        assertEquals(JumpBehavior.JumpPhase.REVERSED_WALK, jump.getPhase());
        assertEquals(JumpBehavior.LANDING_COLUMN, zombie.getGridX());
        assertEquals(JumpBehavior.LANDING_COLUMN, zombie.getContinuousX(), 0.01f);
        assertTrue(zombie.isMovingBackward());
        assertEquals(ZombieState.WALKING, zombie.getState());
        assertEquals(0f, jump.heightPx(), 0.01f);
    }

    @Test
    void dynamiteBurnsDownThenIceExtinguishes() {
        ZombieInstance zombie = prospector(SPAWN_COL);
        JumpBehavior jump = jumpOf(zombie);
        BehaviorContext context = stubContext(null);

        assertEquals(JumpBehavior.DYNAMITE_BURNING_01, jump.dynamitePart());
        runFor(jump, zombie, context, 2.6f);
        assertEquals(JumpBehavior.DYNAMITE_BURNING_02, jump.dynamitePart());
        runFor(jump, zombie, context, 2.5f);
        assertEquals(JumpBehavior.DYNAMITE_BURNING_03, jump.dynamitePart());

        jump.extinguish();
        assertTrue(jump.isExtinguished());
        assertEquals(JumpBehavior.DYNAMITE_EXTINGUISHED, jump.dynamitePart());
        assertEquals(JumpBehavior.JumpPhase.COUNTDOWN, jump.getPhase());

        runFor(jump, zombie, context, JumpBehavior.LAUNCH_COUNTDOWN);
        assertEquals(JumpBehavior.JumpPhase.COUNTDOWN, jump.getPhase());
        assertFalse(jump.hasLaunched());
    }

    @Test
    void frostDuringCountdownExtinguishes() {
        ZombieInstance zombie = prospector(SPAWN_COL);
        JumpBehavior jump = jumpOf(zombie);
        BehaviorContext context = stubContext(null);

        zombie.applyChill();
        jump.execute(zombie, context, TICK);
        assertTrue(jump.isExtinguished());
        assertEquals(JumpBehavior.DYNAMITE_EXTINGUISHED, jump.dynamitePart());
    }

    @Test
    void iceAfterLaunchDoesNotExtinguish() {
        ZombieInstance zombie = prospector(SPAWN_COL);
        JumpBehavior jump = jumpOf(zombie);
        BehaviorContext context = stubContext(null);

        runFor(jump, zombie, context, JumpBehavior.LAUNCH_COUNTDOWN);
        assertEquals(JumpBehavior.JumpPhase.JUMPING, jump.getPhase());
        jump.extinguish();
        assertFalse(jump.isExtinguished());
        assertTrue(jump.hasLaunched());
    }

    @Test
    void stopEatingAfterLandStaysWalking() {
        PlantInstance plant = wallnut();
        ZombieInstance zombie = prospector(0);
        zombie.setMovingBackward(true);
        JumpBehavior jump = jumpOf(zombie);
        jump.setPhase(JumpBehavior.JumpPhase.REVERSED_WALK);
        BehaviorContext context = stubContext(plant);

        jump.execute(zombie, context, TICK);
        assertEquals(ZombieState.EATING, zombie.getState());

        plant.setCurrentHP(0);
        jump.execute(zombie, context, TICK);
        assertEquals(ZombieState.WALKING, zombie.getState());
        assertFalse(zombie.isHypnotized());
    }

    private static void runFor(JumpBehavior jump, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            jump.execute(zombie, context, TICK);
        }
    }

    private static JumpBehavior jumpOf(ZombieInstance zombie) {
        return (JumpBehavior) zombie.getBehavior(ZombieBehaviorType.JUMP);
    }

    private static ZombieInstance prospector(int col) {
        Zombie definition = new Zombie(
                "ZombieProspector", 190, 0.16f, 100f, ZombieSize.NORMAL,
                null, 200, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.JUMP));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setGridPosition(new Point(col, 0));
        zombie.setContinuousPosition(new FloatPoint(col, 0));
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }

    private static PlantInstance wallnut() {
        return new PlantInstance(new Plant(
                1, "Wallnut", PlantCategory.WALL_NUT, List.of(), 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
    }

    private static BehaviorContext stubContext(PlantInstance plant) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "getPlantAt" -> plant;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
