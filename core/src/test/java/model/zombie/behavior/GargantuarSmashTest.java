package model.zombie.behavior;

import model.enums.Chapter;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class GargantuarSmashTest {

    private static final float TICK = 0.1f;

    /** Walk → eat (wind-up) → smash_left (plant dies at its first frame) → walk. */
    @Test
    void smashRunsWindupThenSwing() {
        PlantInstance plant = new PlantInstance(new Plant(
                1, "Wallnut", PlantCategory.WALL_NUT, List.of(), 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
        Zombie definition = new Zombie(
                "ZombieGargantuar", 3000, 0.2f, 100f, ZombieSize.LARGE,
                Chapter.ANCIENT_EGYPT, 10, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SMASH));
        // zombies.json ships SmashDuration=2; honouring it would freeze the raised club.
        definition.putBehaviorProp("SmashDuration", 2f);
        ZombieInstance garg = new ZombieInstance(definition);
        garg.setGridPosition(new Point(3, 0));
        SmashBehavior smash = (SmashBehavior) garg.getBehavior(ZombieBehaviorType.SMASH);
        BehaviorContext context = stubContext(plant);

        // Still short of the plant tile's facing border: nothing happens.
        garg.setContinuousPosition(new FloatPoint(3.7f, 0));
        smash.execute(garg, context, TICK);
        assertEquals(SmashBehavior.GargantuarPhase.WALKING, smash.getGargantuarPhase());

        garg.setContinuousPosition(new FloatPoint(3.5f, 0));
        smash.execute(garg, context, TICK);
        assertEquals(SmashBehavior.GargantuarPhase.WINDUP, smash.getGargantuarPhase());
        assertEquals(ZombieState.SPECIAL_ACTION, garg.getState());

        runFor(smash, garg, context, SmashBehavior.GARGANTUAR_WINDUP_DURATION);
        assertEquals(SmashBehavior.GargantuarPhase.SMASHING, smash.getGargantuarPhase(),
                "the swing starts when the eat clip ends, not when SmashDuration expires");
        assertTrue(plant.getCurrentHP() <= 0, "plant dies as the club starts coming down");
        assertEquals(ZombieState.SPECIAL_ACTION, garg.getState());

        runFor(smash, garg, context, SmashBehavior.GARGANTUAR_SMASH_DURATION);
        assertEquals(SmashBehavior.GargantuarPhase.WALKING, smash.getGargantuarPhase());
        assertEquals(ZombieState.WALKING, garg.getState());
    }

    private static void runFor(SmashBehavior smash, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            smash.execute(zombie, context, TICK);
        }
    }

    /** Only the handful of context calls the Gargantuar smash cycle makes. */
    private static BehaviorContext stubContext(PlantInstance plant) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "getPlantAt" -> (int) args[1] == 3 ? plant : null;
                    case "damagePlant" -> {
                        ((PlantInstance) args[0]).setCurrentHP(
                                ((PlantInstance) args[0]).getCurrentHP() - (int) args[1]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
