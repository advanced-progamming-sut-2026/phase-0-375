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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformBehaviorTest {

    private static final float TICK = 0.1f;

    @Test
    void intervalCastsSheepThenRevertsOnDeathAndContactStillWorks() {
        PlantInstance pea = pea(3, 0);
        PlantInstance nut = pea(5, 1);
        ZombieInstance wizard = wizard(6, 2);
        TransformBehavior transform = (TransformBehavior) wizard.getBehavior(ZombieBehaviorType.TRANSFORM);
        BehaviorContext lawn = stubContext(pea, nut);

        transform.setCastTimer(TransformBehavior.TRANSFORM_INTERVAL);
        transform.execute(wizard, lawn, TICK);
        assertTrue(transform.isCasting());
        assertEquals(ZombieState.SPECIAL_ACTION, wizard.getState());
        assertTrue(pea.isTransformed() || nut.isTransformed());
        assertEquals(1, countTransformed(pea, nut));

        runFor(transform, wizard, lawn, TransformBehavior.SHEEP_DURATION);
        assertFalse(transform.isCasting());
        assertEquals(ZombieState.WALKING, wizard.getState());
        assertEquals(1, countTransformed(pea, nut));

        PlantInstance contactPlant = pea.isTransformed() ? nut : pea;
        ZombieInstance other = wizard(contactPlant.getPosition().getX(), contactPlant.getPosition().getY());
        TransformBehavior contact = (TransformBehavior) other.getBehavior(ZombieBehaviorType.TRANSFORM);
        contact.execute(other, lawn, TICK);
        assertTrue(contactPlant.isTransformed());
        assertTrue(contact.isCasting());

        wizard.fireOnDeathBehaviors(lawn);
        other.fireOnDeathBehaviors(lawn);
        assertFalse(pea.isTransformed());
        assertFalse(nut.isTransformed());
    }

    @Test
    void skipsAlreadyTransformedAndHunterStillThrows() {
        PlantInstance pea = pea(3, 0);
        pea.transform();
        ZombieInstance wizard = wizard(6, 0);
        TransformBehavior transform = (TransformBehavior) wizard.getBehavior(ZombieBehaviorType.TRANSFORM);
        transform.setCastTimer(TransformBehavior.TRANSFORM_INTERVAL);
        transform.execute(wizard, stubContext(pea), TICK);
        assertFalse(transform.isCasting());
        assertEquals(ZombieState.WALKING, wizard.getState());

        ZombieInstance hunter = hunter();
        ShootBehavior snow = (ShootBehavior) hunter.getBehavior(ZombieBehaviorType.SHOOT);
        snow.setCastTimer(ShootBehavior.HUNTER_BARRAGE_INTERVAL);
        snow.execute(hunter, stubContext(pea), 0.01f);
        assertTrue(snow.isThrowing());
        assertFalse(snow.isOctopusThrowing());
    }

    private static void runFor(TransformBehavior transform, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            transform.execute(zombie, context, TICK);
        }
    }

    private static int countTransformed(PlantInstance... plants) {
        int n = 0;
        for (PlantInstance plant : plants) {
            if (plant.isTransformed()) {
                n++;
            }
        }
        return n;
    }

    private static ZombieInstance wizard(int col, int row) {
        Zombie definition = new Zombie(
                "ZombieWizard", 490, 0.12f, 100f, ZombieSize.NORMAL,
                Chapter.DARK_AGES, 800, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.TRANSFORM));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        zombie.setGridPosition(new Point(col, row));
        zombie.setContinuousPosition(new FloatPoint(col, row));
        return zombie;
    }

    private static ZombieInstance hunter() {
        Zombie definition = new Zombie(
                "ZombieIceAgeHunter", 700, 0.12f, 100f, ZombieSize.NORMAL,
                Chapter.FROSTBITE_CAVES, 500, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SHOOT));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        zombie.setGridPosition(new Point(6, 0));
        zombie.setContinuousPosition(new FloatPoint(6, 0));
        return zombie;
    }

    private static PlantInstance pea(int col, int row) {
        PlantInstance p = new PlantInstance(new Plant(
                1, "Peashooter", PlantCategory.SHOOTER, List.of(), 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
        p.setPosition(new Point(col, row));
        return p;
    }

    private static BehaviorContext stubContext(PlantInstance... plants) {
        List<PlantInstance> all = List.of(plants);
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAllPlants" -> all;
                    case "getPlantsInLane" -> {
                        int lane = (int) args[0];
                        List<PlantInstance> inLane = new java.util.ArrayList<>();
                        for (PlantInstance plant : all) {
                            Point pos = plant.getPosition();
                            if (pos != null && pos.getY() == lane) {
                                inLane.add(plant);
                            }
                        }
                        yield inLane;
                    }
                    case "getPlantAt" -> {
                        int row = (int) args[0];
                        int col = (int) args[1];
                        PlantInstance found = null;
                        for (PlantInstance plant : all) {
                            Point pos = plant.getPosition();
                            if (pos != null && pos.getY() == row && pos.getX() == col) {
                                found = plant;
                                break;
                            }
                        }
                        yield found;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
