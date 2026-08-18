package model.zombie.behavior;

import model.enums.ArmorType;
import model.enums.Chapter;
import model.enums.PlantCategory;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.armor.Armor;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuffBehaviorTest {

    private static final float TICK = 0.1f;
    private static final int COLS = 9;

    @Test
    void introThenIdleThenSpecialKnightsPeasant() {
        ZombieInstance peasant = peasant(4, 0);
        Lawn lawn = new Lawn(peasant);
        ZombieInstance king = king(8, 0);
        BuffBehavior buff = (BuffBehavior) king.getBehavior(ZombieBehaviorType.BUFF);

        buff.execute(king, lawn.context(), TICK);
        assertEquals(BuffBehavior.KingPhase.INTRO, buff.getPhase());
        assertEquals(ZombieState.SPAWNING, king.getState());
        assertTrue(peasant.getArmors().isEmpty());

        runFor(buff, king, lawn.context(), BuffBehavior.INTRO_DURATION);
        assertEquals(BuffBehavior.KingPhase.IDLE, buff.getPhase());
        assertEquals(ZombieState.SPECIAL_ACTION, king.getState());

        buff.setKnightTimer(BuffBehavior.DEFAULT_DELAY_BETWEEN_KNIGHTINGS);
        buff.execute(king, lawn.context(), TICK);
        assertEquals(BuffBehavior.KingPhase.SPECIAL, buff.getPhase());
        assertTrue(has(peasant, ArmorType.Crown));
        assertTrue(has(peasant, ArmorType.ShoulderArmor));

        runFor(buff, king, lawn.context(), BuffBehavior.SPECIAL_DURATION);
        assertEquals(BuffBehavior.KingPhase.IDLE, buff.getPhase());
        assertEquals(ZombieState.SPECIAL_ACTION, king.getState());
    }

    @Test
    void idleDoesNotSpecialWithoutAPeasantAndWizardStillCasts() {
        Lawn lawn = new Lawn();
        ZombieInstance king = king(8, 0);
        BuffBehavior buff = (BuffBehavior) king.getBehavior(ZombieBehaviorType.BUFF);
        buff.setPhase(BuffBehavior.KingPhase.IDLE);
        buff.setKnightTimer(BuffBehavior.DEFAULT_DELAY_BETWEEN_KNIGHTINGS);
        buff.execute(king, lawn.context(), TICK);
        assertEquals(BuffBehavior.KingPhase.IDLE, buff.getPhase());

        PlantInstance pea = pea();
        ZombieInstance wizard = wizard();
        TransformBehavior transform = (TransformBehavior) wizard.getBehavior(ZombieBehaviorType.TRANSFORM);
        transform.setCastTimer(TransformBehavior.TRANSFORM_INTERVAL);
        transform.execute(wizard, wizardLawn(pea), TICK);
        assertTrue(transform.isCasting());
        assertTrue(pea.isTransformed());
    }

    private static void runFor(BuffBehavior buff, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            buff.execute(zombie, context, TICK);
        }
    }

    private static boolean has(ZombieInstance zombie, ArmorType type) {
        for (Armor armor : zombie.getArmors()) {
            if (armor.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private static ZombieInstance king(int col, int row) {
        Zombie definition = new Zombie(
                "ZombieDarkKing", 1000, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.DARK_AGES, 750, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.BUFF));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setGridPosition(new Point(col, row));
        zombie.setContinuousPosition(new FloatPoint(col, row));
        return zombie;
    }

    private static ZombieInstance peasant(int col, int row) {
        Zombie definition = new Zombie(
                "ZombieDefault", 190, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.DARK_AGES, 50, 1, List.of(), null, null,
                List.of());
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setGridPosition(new Point(col, row));
        zombie.setContinuousPosition(new FloatPoint(col, row));
        return zombie;
    }

    private static ZombieInstance wizard() {
        Zombie definition = new Zombie(
                "ZombieWizard", 490, 0.12f, 100f, ZombieSize.NORMAL,
                Chapter.DARK_AGES, 800, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.TRANSFORM));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        zombie.setGridPosition(new Point(6, 2));
        zombie.setContinuousPosition(new FloatPoint(6, 2));
        return zombie;
    }

    private static PlantInstance pea() {
        PlantInstance p = new PlantInstance(new Plant(
                1, "Peashooter", PlantCategory.SHOOTER, List.of(), 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
        p.setPosition(new Point(3, 2));
        return p;
    }

    private static BehaviorContext wizardLawn(PlantInstance plant) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAllPlants" -> List.of(plant);
                    case "getPlantAt" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class Lawn {
        final List<ZombieInstance> zombies = new ArrayList<>();

        Lawn(ZombieInstance... onLawn) {
            zombies.addAll(List.of(onLawn));
        }

        BehaviorContext context() {
            Lawn lawn = this;
            return (BehaviorContext) Proxy.newProxyInstance(
                    BehaviorContext.class.getClassLoader(),
                    new Class<?>[]{BehaviorContext.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getColumnCount" -> COLS;
                        case "getZombiesInArea" -> List.copyOf(lawn.zombies);
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
