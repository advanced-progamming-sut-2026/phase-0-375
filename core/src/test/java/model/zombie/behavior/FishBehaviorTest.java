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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishBehaviorTest {

    private static final float TICK = 0.1f;
    private static final int COLS = 9;

    @Test
    void introThenIdleThenCastHooksThenReel() {
        PlantInstance plant = plant(5);
        Lawn lawn = new Lawn(plant);
        ZombieInstance zombie = fisherman(8);
        FishBehavior fish = (FishBehavior) zombie.getBehavior(ZombieBehaviorType.FISH);

        fish.execute(zombie, lawn.context(), TICK);
        assertEquals(FishBehavior.FishPhase.INTRO, fish.getPhase());
        assertEquals(ZombieState.SPAWNING, zombie.getState());

        runFor(fish, zombie, lawn.context(), FishBehavior.INTRO_DURATION);
        assertEquals(FishBehavior.FishPhase.IDLE, fish.getPhase());
        assertEquals(ZombieState.SPECIAL_ACTION, zombie.getState());
        assertEquals(5, plant.getPosition().getX());

        fish.setCastTimer(FishBehavior.DELAY_BETWEEN_CASTING);
        fish.execute(zombie, lawn.context(), TICK);
        assertEquals(FishBehavior.FishPhase.CASTING, fish.getPhase());
        assertEquals(5, plant.getPosition().getX());
        assertFalse(fish.isPlantHooked());

        runFor(fish, zombie, lawn.context(), FishBehavior.CAST_DURATION);
        assertTrue(fish.isPlantHooked());
        assertEquals(6, plant.getPosition().getX());

        runFor(fish, zombie, lawn.context(), FishBehavior.DELAY_BEFORE_REELING);
        assertEquals(FishBehavior.FishPhase.REELING, fish.getPhase());

        runFor(fish, zombie, lawn.context(), FishBehavior.REEL_DURATION);
        assertEquals(FishBehavior.FishPhase.IDLE, fish.getPhase());
        assertEquals(ZombieState.SPECIAL_ACTION, zombie.getState());
    }

    @Test
    void blockedTileDestroysThePlant() {
        PlantInstance hooked = plant(7);
        PlantInstance wall = plant(8);
        Lawn lawn = new Lawn(hooked, wall);
        ZombieInstance zombie = fisherman(8);
        FishBehavior fish = (FishBehavior) zombie.getBehavior(ZombieBehaviorType.FISH);
        fish.setPhase(FishBehavior.FishPhase.IDLE);
        fish.setCastTimer(FishBehavior.DELAY_BETWEEN_CASTING);

        fish.execute(zombie, lawn.context(), TICK);
        runFor(fish, zombie, lawn.context(), FishBehavior.CAST_DURATION);
        assertTrue(lawn.destroyed.contains(hooked));
        assertEquals(8, wall.getPosition().getX());
    }

    @Test
    void idleDoesNotCastWithoutAPlant() {
        Lawn lawn = new Lawn();
        ZombieInstance zombie = fisherman(8);
        FishBehavior fish = (FishBehavior) zombie.getBehavior(ZombieBehaviorType.FISH);
        fish.setPhase(FishBehavior.FishPhase.IDLE);
        fish.setCastTimer(FishBehavior.DELAY_BETWEEN_CASTING);
        fish.execute(zombie, lawn.context(), TICK);
        assertEquals(FishBehavior.FishPhase.IDLE, fish.getPhase());
    }

    private static void runFor(FishBehavior fish, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            fish.execute(zombie, context, TICK);
        }
    }

    private static ZombieInstance fisherman(int col) {
        Zombie definition = new Zombie(
                "ZombieBeachFisherman", 1000, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.BIG_WAVE_BEACH, 150, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.FISH));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setGridPosition(new Point(col, 0));
        zombie.setContinuousPosition(new FloatPoint(col, 0));
        return zombie;
    }

    private static PlantInstance plant(int col) {
        PlantInstance p = new PlantInstance(new Plant(
                1, "Peashooter", PlantCategory.SHOOTER, List.of(), 50, 300, 0,
                0f, 0f, null, 0f, null, 0f, null));
        p.setPosition(new Point(col, 0));
        return p;
    }

    private static final class Lawn {
        final List<PlantInstance> plants = new ArrayList<>();
        final List<PlantInstance> destroyed = new ArrayList<>();

        Lawn(PlantInstance... planted) {
            plants.addAll(List.of(planted));
        }

        BehaviorContext context() {
            Lawn lawn = this;
            return (BehaviorContext) Proxy.newProxyInstance(
                    BehaviorContext.class.getClassLoader(),
                    new Class<?>[]{BehaviorContext.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getColumnCount" -> COLS;
                        case "getPlantsInLane" -> List.copyOf(lawn.plants);
                        case "getPlantAt" -> {
                            int row = (int) args[0];
                            int col = (int) args[1];
                            PlantInstance found = null;
                            for (PlantInstance p : lawn.plants) {
                                if (p.getPosition() != null
                                        && p.getPosition().getY() == row
                                        && p.getPosition().getX() == col) {
                                    found = p;
                                    break;
                                }
                            }
                            yield found;
                        }
                        case "movePlant" -> {
                            PlantInstance p = (PlantInstance) args[0];
                            p.setPosition(new Point((int) args[2], (int) args[1]));
                            yield true;
                        }
                        case "destroyPlant" -> {
                            PlantInstance p = (PlantInstance) args[0];
                            lawn.plants.remove(p);
                            lawn.destroyed.add(p);
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
