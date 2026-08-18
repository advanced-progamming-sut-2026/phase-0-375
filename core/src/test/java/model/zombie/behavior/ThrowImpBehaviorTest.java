package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowImpBehaviorTest {

    private static final float TICK = 0.05f;

    @Test
    void fireThenCannonThenImpFliesToThirdColumn() {
        Zombie gargDef = new Zombie(
                "ZombieGargantuar", 3600, 0.15f, 100f, ZombieSize.LARGE,
                Chapter.ANCIENT_EGYPT, 10, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SMASH, ZombieBehaviorType.THROW_IMP));
        ZombieInstance garg = new ZombieInstance(gargDef);
        garg.setGridPosition(new Point(6, 1));
        garg.setContinuousPosition(new FloatPoint(6.2f, 1));
        garg.setCurrentHP(1800); // 50%

        AtomicReference<ZombieInstance> spawned = new AtomicReference<>();
        ThrowImpBehavior toss = (ThrowImpBehavior) garg.getBehavior(ZombieBehaviorType.THROW_IMP);
        BehaviorContext context = stubContext(spawned);

        toss.execute(garg, context, TICK);
        assertTrue(toss.hasThrownImp());
        assertEquals(ThrowImpBehavior.ThrowPhase.FIRE, toss.getThrowPhase());
        assertEquals(ZombieState.SPECIAL_ACTION, garg.getState());
        assertFalse(toss.hasReleasedImp());

        runFor(toss, garg, context, ThrowImpBehavior.FIRE_DURATION);
        assertEquals(ThrowImpBehavior.ThrowPhase.CANNON, toss.getThrowPhase());
        assertFalse(toss.hasReleasedImp());

        runFor(toss, garg, context, ThrowImpBehavior.RELEASE_AT);
        assertTrue(toss.hasReleasedImp());
        ZombieInstance imp = spawned.get();
        assertNotNull(imp);
        ThrowImpBehavior.Flight flight = ThrowImpBehavior.flightOf(imp);
        assertNotNull(flight);
        assertTrue(flight.isFlying());
        assertEquals(ZombieState.SPECIAL_ACTION, imp.getState());

        runFor(toss, garg, context, ThrowImpBehavior.CANNON_DURATION);
        assertFalse(toss.isThrowing());
        assertEquals(ZombieState.WALKING, garg.getState());

        for (float t = 0f; t < ThrowImpBehavior.FLIGHT_DURATION; t += TICK) {
            flight.execute(imp, context, TICK);
        }
        assertTrue(flight.isLanding());
        assertEquals(ThrowImpBehavior.DEFAULT_IMP_TARGET_COLUMN, imp.getGridX(), 0);
        assertEquals(ThrowImpBehavior.DEFAULT_IMP_TARGET_COLUMN, imp.getContinuousX(), 0.01f);

        for (float t = 0f; t < ThrowImpBehavior.LAND_DURATION; t += TICK) {
            flight.execute(imp, context, TICK);
        }
        assertFalse(flight.isLanding());
        assertEquals(ZombieState.WALKING, imp.getState());
    }

    private static void runFor(ThrowImpBehavior toss, ZombieInstance garg,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            toss.execute(garg, context, TICK);
        }
    }

    private static BehaviorContext stubContext(AtomicReference<ZombieInstance> spawned) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "spawnZombieAt" -> {
                        Zombie impDef = new Zombie(
                                "ZombieImp", 190, 0.2f, 200f, ZombieSize.IMP,
                                Chapter.ANCIENT_EGYPT, 1, 1, List.of(), null, null,
                                List.of());
                        ZombieInstance imp = new ZombieInstance(impDef);
                        spawned.set(imp);
                        yield imp;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
