package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.PlantCategory;
import model.enums.SunType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.map.Point;
import model.item.Sun;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalSkullStealSunTest {

    private static final float TICK = 0.1f;
    private static final int ZOMBIE_COL = 5;
    private static final int PLANT_COL = 3;

    @Test
    void powerUpPowerPowerDownThenLaserAtAttackBeam() {
        PlantInstance plant = wallnut();
        ZombieInstance skull = crystalSkull();
        StealSunBehavior steal = (StealSunBehavior) skull.getBehavior(ZombieBehaviorType.STEAL_SUN);
        List<Integer> spent = new ArrayList<>();
        BehaviorContext context = stubContext(plant, spent, null);

        steal.execute(skull, context, TICK);
        assertEquals(StealSunBehavior.TurquoisePhase.POWER_UP, steal.getTurquoisePhase());
        assertEquals(ZombieState.SPECIAL_ACTION, skull.getState());

        runUntil(steal, skull, context, StealSunBehavior.TurquoisePhase.POWER);
        assertTrue(plant.getCurrentHP() > 0, "plant lives through the charge");

        float powerElapsed = 0f;
        while (powerElapsed + TICK < StealSunBehavior.POWER_DURATION) {
            steal.execute(skull, context, TICK);
            powerElapsed += TICK;
            assertEquals(StealSunBehavior.TurquoisePhase.POWER, steal.getTurquoisePhase(),
                    "power loops 5s");
        }
        steal.execute(skull, context, TICK);
        assertEquals(StealSunBehavior.TurquoisePhase.POWER_DOWN, steal.getTurquoisePhase());
        assertTrue(plant.getCurrentHP() > 0, "plant lives through power_down");

        runUntil(steal, skull, context, StealSunBehavior.TurquoisePhase.ATTACK);
        assertTrue(plant.getCurrentHP() > 0, "plant lives until the glow fires");

        final float step = 0.01f;
        float elapsed = 0f;
        while (elapsed + step < StealSunBehavior.ATTACK_BEAM_AT) {
            steal.execute(skull, context, step);
            elapsed += step;
            assertTrue(plant.getCurrentHP() > 0, "plant lives before 0.63s of attack");
        }
        steal.execute(skull, context, step);
        assertTrue(plant.getCurrentHP() <= 0, "plant dies at 0.63s of attack");
        assertTrue(steal.hasFiredLaser());
        assertEquals(StealSunBehavior.TurquoisePhase.ATTACK, steal.getTurquoisePhase());

        runUntil(steal, skull, context, StealSunBehavior.TurquoisePhase.WALKING);
        assertEquals(ZombieState.WALKING, skull.getState());
        assertFalse(spent.isEmpty(), "stole sun during the charge");
    }

    @Test
    void deathReturnsHalfStolenSun() {
        PlantInstance plant = wallnut();
        ZombieInstance skull = crystalSkull();
        StealSunBehavior steal = (StealSunBehavior) skull.getBehavior(ZombieBehaviorType.STEAL_SUN);
        List<Sun> dropped = new ArrayList<>();
        BehaviorContext context = stubContext(plant, new ArrayList<>(), dropped);

        runFor(steal, skull, context, 2f);
        int stolen = steal.getStolenSunAmount();
        assertTrue(stolen > 0);

        steal.onZombieDeath(skull, context);
        int returned = 0;
        for (Sun sun : dropped) {
            returned += sun.getValue();
        }
        assertEquals(stolen / 2, returned);
        assertEquals(0, steal.getStolenSunAmount());
    }

    @Test
    void raStillCapturesGroundSun() {
        Zombie definition = new Zombie(
                "ZombieRa", 200, 0.2f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 150, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.STEAL_SUN));
        definition.putBehaviorProp("MaxClaimedSunCurrency", 5000);
        ZombieInstance ra = new ZombieInstance(definition);
        ra.setGridPosition(new Point(4, 0));
        StealSunBehavior steal = (StealSunBehavior) ra.getBehavior(ZombieBehaviorType.STEAL_SUN);

        List<Sun> ground = new ArrayList<>();
        ground.add(new Sun(SunType.NORMAL, 50, 3, 0));
        BehaviorContext context = (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getActiveSuns" -> ground;
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        steal.execute(ra, context, TICK);
        assertTrue(ground.isEmpty());
        assertEquals(50, steal.getStolenSunAmount());
        assertEquals(StealSunBehavior.TurquoisePhase.WALKING, steal.getTurquoisePhase());
    }

    private static void runUntil(StealSunBehavior steal, ZombieInstance zombie,
                                 BehaviorContext context, StealSunBehavior.TurquoisePhase phase) {
        for (int i = 0; i < 200 && steal.getTurquoisePhase() != phase; i++) {
            steal.execute(zombie, context, TICK);
        }
        assertEquals(phase, steal.getTurquoisePhase());
    }

    private static void runFor(StealSunBehavior steal, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            steal.execute(zombie, context, TICK);
        }
    }

    private static ZombieInstance crystalSkull() {
        Zombie definition = new Zombie(
                "ZombieCrystalSkull", 250, 0.185f, 100f, ZombieSize.NORMAL,
                null, 500, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.STEAL_SUN));
        definition.putBehaviorProp("ChargingTime", 5f);
        definition.putBehaviorProp("ChargingTimeDecrementPerFiveSun", 0f);
        definition.putBehaviorProp("LaserBeamLength", 220);
        definition.putBehaviorProp("LaserBeamDamage", 4001);
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setGridPosition(new Point(ZOMBIE_COL, 0));
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }

    private static PlantInstance wallnut() {
        return new PlantInstance(new Plant(
                1, "Wallnut", PlantCategory.WALL_NUT, List.of(), 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
    }

    private static BehaviorContext stubContext(PlantInstance plant, List<Integer> spent,
                                               List<Sun> dropped) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "getPlantAt" -> (int) args[1] == PLANT_COL ? plant : null;
                    case "spendSun" -> {
                        spent.add((int) args[0]);
                        yield true;
                    }
                    case "damagePlant" -> {
                        ((PlantInstance) args[0]).setCurrentHP(
                                ((PlantInstance) args[0]).getCurrentHP() - (int) args[1]);
                        yield null;
                    }
                    case "spawnSun" -> {
                        if (dropped != null) {
                            dropped.add((Sun) args[0]);
                        }
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
