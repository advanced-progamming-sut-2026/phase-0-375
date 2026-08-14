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

class AllStarSmashTest {

    private static final float TICK = 0.1f;

    /** Run → tackle → kick; plant dies at 0.53s of kick, then walk. */
    @Test
    void chargeTacklesThenKicksThenWalks() {
        PlantInstance plant = new PlantInstance(new Plant(
                1, "Wallnut", PlantCategory.WALL_NUT, List.of(), 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
        Zombie definition = new Zombie(
                "ZombieModernAllStar", 1100, 0.16f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 1000, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SMASH));
        definition.putBehaviorProp("RunningSpeedScale", 1.0f);
        ZombieInstance allStar = new ZombieInstance(definition);
        allStar.setGridPosition(new Point(3, 0));
        SmashBehavior smash = (SmashBehavior) allStar.getBehavior(ZombieBehaviorType.SMASH);
        BehaviorContext context = stubContext(plant);

        allStar.setContinuousPosition(new FloatPoint(3.7f, 0));
        smash.execute(allStar, context, TICK);
        assertEquals(SmashBehavior.AllStarPhase.RUNNING, smash.getAllStarPhase());
        assertEquals(0.16f * SmashBehavior.ALL_STAR_BEFORE_SMASH_SPEED_MODIFIER,
                allStar.getCurrentSpeed(), 1e-4f);

        allStar.setContinuousPosition(new FloatPoint(3.5f, 0));
        smash.execute(allStar, context, TICK);
        assertEquals(SmashBehavior.AllStarPhase.TACKLING, smash.getAllStarPhase());
        assertEquals(ZombieState.SPECIAL_ACTION, allStar.getState());
        assertTrue(plant.getCurrentHP() > 0, "plant lives through tackle");

        runFor(smash, allStar, context, SmashBehavior.ALL_STAR_TACKLE_DURATION);
        assertEquals(SmashBehavior.AllStarPhase.KICKING, smash.getAllStarPhase());
        assertTrue(plant.getCurrentHP() > 0, "plant lives until kick impact");

        final float step = 0.01f;
        float elapsed = 0f;
        while (elapsed + step < SmashBehavior.ALL_STAR_KICK_IMPACT_AT) {
            smash.execute(allStar, context, step);
            elapsed += step;
            assertTrue(plant.getCurrentHP() > 0, "plant lives before 0.53s of kick");
        }
        smash.execute(allStar, context, step);
        assertTrue(plant.getCurrentHP() <= 0, "plant dies at 0.53s of kick");
        assertEquals(SmashBehavior.AllStarPhase.KICKING, smash.getAllStarPhase());

        runFor(smash, allStar, context, SmashBehavior.ALL_STAR_KICK_DURATION);
        assertEquals(SmashBehavior.AllStarPhase.WALKING, smash.getAllStarPhase());
        assertEquals(ZombieState.WALKING, allStar.getState());
        assertTrue(smash.hasSmashedOnce());
        assertEquals(0.16f * 1.0f, allStar.getCurrentSpeed(), 1e-4f);
    }

    private static void runFor(SmashBehavior smash, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            smash.execute(zombie, context, TICK);
        }
    }

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
