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

class HunterSnowballTest {

    @Test
    void barrageThrowsThenFreezesAndExplorerDoesNotThrow() {
        PlantInstance plant = plant(3);
        ZombieInstance hunter = hunter(6);
        ShootBehavior shoot = (ShootBehavior) hunter.getBehavior(ZombieBehaviorType.SHOOT);
        BehaviorContext lawn = stubContext(plant);

        shoot.setCastTimer(ShootBehavior.HUNTER_BARRAGE_INTERVAL);
        shoot.execute(hunter, lawn, 0.01f);
        assertTrue(shoot.isThrowing());
        assertEquals(ZombieState.SPECIAL_ACTION, hunter.getState());
        assertEquals(3, shoot.getSnowballsRemainingInBarrage());
        assertEquals(0, shoot.getSnowballSplatSeq());

        shoot.execute(hunter, lawn, ShootBehavior.HUNTER_SNOWBALL_INTERVAL);
        assertEquals(1, shoot.getSnowballSplatSeq());
        assertEquals(3, shoot.getLastSnowballSplatAt().getX());
        assertFalse(plant.isFrozen());

        shoot.execute(hunter, lawn, ShootBehavior.HUNTER_SNOWBALL_INTERVAL);
        shoot.execute(hunter, lawn, ShootBehavior.HUNTER_SNOWBALL_INTERVAL);
        assertTrue(plant.isFrozen());
        assertEquals(3, shoot.getSnowballSplatSeq());
        assertEquals(0, shoot.getSnowballsRemainingInBarrage());
        assertTrue(shoot.isThrowing());

        shoot.execute(hunter, lawn, ShootBehavior.HUNTER_SNOWBALL_INTERVAL);
        assertFalse(shoot.isThrowing());
        assertEquals(ZombieState.WALKING, hunter.getState());

        ZombieInstance explorer = explorer();
        ShootBehavior torch = (ShootBehavior) explorer.getBehavior(ZombieBehaviorType.SHOOT);
        torch.setCastTimer(ShootBehavior.HUNTER_BARRAGE_INTERVAL);
        torch.execute(explorer, lawn, 0.01f);
        assertFalse(torch.isThrowing());
        assertEquals(ZombieState.WALKING, explorer.getState());
    }

    @Test
    void rangeTrapImmuneAndNoEmptyBarrage() {
        ZombieInstance hunter = hunter(6);
        ShootBehavior shoot = (ShootBehavior) hunter.getBehavior(ZombieBehaviorType.SHOOT);

        PlantInstance far = plant("Peashooter", 1, List.of());
        shoot.setCastTimer(ShootBehavior.HUNTER_BARRAGE_INTERVAL);
        shoot.execute(hunter, stubContext(far), 0.01f);
        assertFalse(shoot.isThrowing());
        assertEquals(ZombieState.WALKING, hunter.getState());

        PlantInstance edge = plant("Peashooter", 2, List.of());
        fireBarrage(shoot, hunter, stubContext(edge));
        assertTrue(edge.isFrozen());

        PlantInstance spike = plant("Spikeweed", 5, List.of(PlantTags.TRAP));
        PlantInstance behind = plant("Peashooter", 3, List.of());
        fireBarrage(shoot, hunter, stubContext(spike, behind));
        assertFalse(spike.isFrozen());
        assertTrue(behind.isFrozen());

        PlantInstance torchwood = plant("Torchwood", 4, List.of(PlantTags.FIRE));
        fireBarrage(shoot, hunter, stubContext(torchwood));
        assertFalse(torchwood.isFrozen());
        assertEquals(4, shoot.getLastSnowballSplatAt().getX());

        PlantInstance melon = plant("Winter Melon", 4, List.of(PlantTags.ICE));
        fireBarrage(shoot, hunter, stubContext(melon));
        assertFalse(melon.isFrozen());

        PlantInstance frozen = plant("Wall-nut", 5, List.of());
        frozen.freeze();
        PlantInstance rear = plant("Peashooter", 3, List.of());
        fireBarrage(shoot, hunter, stubContext(frozen, rear));
        assertFalse(rear.isFrozen());
    }

    private static void fireBarrage(ShootBehavior shoot, ZombieInstance hunter, BehaviorContext lawn) {
        shoot.setSnowballsRemainingInBarrage(0);
        shoot.setCastTimer(ShootBehavior.HUNTER_BARRAGE_INTERVAL);
        shoot.execute(hunter, lawn, 0.01f);
        assertTrue(shoot.isThrowing());
        shoot.execute(hunter, lawn, ShootBehavior.HUNTER_SNOWBALL_INTERVAL);
        shoot.execute(hunter, lawn, ShootBehavior.HUNTER_SNOWBALL_INTERVAL);
        shoot.execute(hunter, lawn, ShootBehavior.HUNTER_SNOWBALL_INTERVAL);
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

    private static ZombieInstance explorer() {
        Zombie definition = new Zombie(
                "ZombieExplorer", 250, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 250, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SHOOT));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        zombie.setGridPosition(new Point(6, 0));
        zombie.setContinuousPosition(new FloatPoint(6, 0));
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
