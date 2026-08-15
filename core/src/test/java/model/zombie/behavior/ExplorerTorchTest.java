package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
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

class ExplorerTorchTest {

    private static final float TICK = 0.1f;
    private static final int COL = 5;

    @Test
    void litTorchDestroysPlantCloserThanOneTile() {
        PlantInstance victim = plant("Peashooter", 4, List.of());
        ZombieInstance zombie = explorer(COL);
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);

        shoot.execute(zombie, stubContext(victim), TICK);
        assertTrue(shoot.isTorchLit());
        assertEquals(400, victim.getCurrentHP(), "exactly one tile ahead is out of reach");

        zombie.setContinuousPosition(new FloatPoint(4.9f, 0));
        shoot.execute(zombie, stubContext(victim), TICK);
        assertEquals(0, victim.getCurrentHP());
    }

    @Test
    void iceInReachExtinguishesWithoutBurnFireRelightsAndBurns() {
        PlantInstance ice = plant("IcebergLettuce", 4, List.of(PlantTags.ICE));
        PlantInstance fire = plant("Snapdragon", 4, List.of(PlantTags.FIRE));
        PlantInstance victim = plant("Peashooter", 4, List.of());
        ZombieInstance zombie = explorer(4.9f);
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);

        shoot.execute(zombie, stubContext(ice), TICK);
        assertFalse(shoot.isTorchLit());
        assertEquals(400, ice.getCurrentHP());

        shoot.execute(zombie, stubContext(victim), TICK);
        assertEquals(400, victim.getCurrentHP());

        shoot.execute(zombie, stubContext(fire), TICK);
        assertTrue(shoot.isTorchLit());
        assertEquals(0, fire.getCurrentHP());
    }

    @Test
    void hunterDoesNotUseTorch() {
        Zombie definition = new Zombie(
                "ZombieIceAgeHunter", 250, 0.2f, 100f, ZombieSize.NORMAL,
                Chapter.FROSTBITE_CAVES, 250, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SHOOT));
        ZombieInstance hunter = new ZombieInstance(definition);
        ShootBehavior shoot = (ShootBehavior) hunter.getBehavior(ZombieBehaviorType.SHOOT);
        assertFalse(shoot.isExplorer(hunter));
    }

    private static ZombieInstance explorer(float x) {
        Zombie definition = new Zombie(
                "ZombieExplorer", 250, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 250, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SHOOT));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setGridPosition(new Point((int) x, 0));
        zombie.setContinuousPosition(new FloatPoint(x, 0));
        return zombie;
    }

    private static PlantInstance plant(String name, int col, List<PlantTags> tags) {
        PlantInstance p = new PlantInstance(new Plant(
                1, name, PlantCategory.SHOOTER, tags, 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
        p.setPosition(new Point(col, 0));
        return p;
    }

    private static BehaviorContext stubContext(PlantInstance plant) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "getPlantsInLane" -> List.of(plant);
                    case "destroyPlant" -> {
                        ((PlantInstance) args[0]).setCurrentHP(0);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
