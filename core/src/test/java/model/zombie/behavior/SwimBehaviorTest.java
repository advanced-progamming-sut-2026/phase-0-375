package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.GroundType;
import model.enums.PlantCategory;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.game.map.Cell;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwimBehaviorTest {

    private static final float TICK = 0.1f;
    private static final int COLS = 9;
    private static final int WATER_FROM = 6;

    @Test
    void divesOnWaterAndStaysSkullDeepUntilAPlant() {
        Lawn lawn = new Lawn();
        ZombieInstance zombie = snorkel(8);
        SwimBehavior swim = swimOf(zombie);

        swim.execute(zombie, lawn.context(), TICK);
        assertEquals(SwimBehavior.SwimPhase.SUBMERGED, swim.getPhase());
        assertEquals(0f, swim.getRise(), 1e-4f);

        swim.execute(zombie, lawn.context(), 1f);
        assertEquals(0f, swim.getRise(), 1e-4f);
        assertEquals(0f, swim.targetRise(zombie, lawn.context()), 1e-4f);
    }

    @Test
    void surfacesHalfwayToEatThenDivesWhenThePlantIsGone() {
        PlantInstance plant = plant(7);
        Lawn lawn = new Lawn(plant);
        ZombieInstance zombie = snorkel(7);
        zombie.setContinuousX(7.2f);
        SwimBehavior swim = swimOf(zombie);
        swim.setPhase(SwimBehavior.SwimPhase.SUBMERGED);

        swim.execute(zombie, lawn.context(), 1f);
        assertEquals(SwimBehavior.SwimPhase.SURFACED, swim.getPhase());
        assertEquals(SwimBehavior.EAT_RISE, swim.getRise(), 1e-4f);
        assertTrue(plant.getCurrentHP() < 300);

        plant.takeDamage(plant.getCurrentHP());
        swim.execute(zombie, lawn.context(), 1f);
        assertEquals(SwimBehavior.SwimPhase.SUBMERGED, swim.getPhase());
        assertEquals(0f, swim.getRise(), 1e-4f);
    }

    @Test
    void lastWaterColumnRiseFollowsWalkAndLandSnapsClear() {
        Lawn lawn = new Lawn();
        ZombieInstance zombie = snorkel(6);
        zombie.setContinuousX(6.99f);
        SwimBehavior swim = swimOf(zombie);
        swim.setPhase(SwimBehavior.SwimPhase.SUBMERGED);

        swim.execute(zombie, lawn.context(), 1f);
        assertEquals(SwimBehavior.lastColumnProgress(zombie), swim.getRise(), 1e-3f);
        assertTrue(swim.getRise() < 0.05f);

        zombie.setContinuousX(6f);
        swim.execute(zombie, lawn.context(), 1f);
        assertEquals(1f, swim.getRise(), 1e-4f);

        zombie.setGridPosition(new Point(5, 0));
        zombie.setContinuousX(5.9f);
        swim.execute(zombie, lawn.context(), TICK);
        assertEquals(SwimBehavior.SwimPhase.WALKING, swim.getPhase());
        assertEquals(1f, swim.getRise(), 1e-4f);
    }

    private static SwimBehavior swimOf(ZombieInstance zombie) {
        return (SwimBehavior) zombie.getBehavior(ZombieBehaviorType.SWIM);
    }

    private static ZombieInstance snorkel(int col) {
        Zombie definition = new Zombie(
                "ZombieBeachSnorkel", 200, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.BIG_WAVE_BEACH, 150, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SWIM));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setGridPosition(new Point(col, 0));
        zombie.setContinuousPosition(new FloatPoint(col, 0));
        return zombie;
    }

    private static PlantInstance plant(int col) {
        PlantInstance p = new PlantInstance(new Plant(
                1, "LilyPad", PlantCategory.SHOOTER, List.of(), 25, 300, 0,
                0f, 0f, null, 0f, null, 0f, null));
        p.setPosition(new Point(col, 0));
        return p;
    }

    private static final class Lawn {
        final List<PlantInstance> plants = new ArrayList<>();
        final Cell[] cells = new Cell[COLS];

        Lawn(PlantInstance... planted) {
            plants.addAll(List.of(planted));
            for (int col = 0; col < COLS; col++) {
                Cell cell = new Cell(0, col);
                cell.setGroundType(col >= WATER_FROM ? GroundType.WATER : GroundType.NORMAL);
                cells[col] = cell;
            }
        }

        BehaviorContext context() {
            Lawn lawn = this;
            return (BehaviorContext) Proxy.newProxyInstance(
                    BehaviorContext.class.getClassLoader(),
                    new Class<?>[]{BehaviorContext.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getColumnCount" -> COLS;
                        case "getRowCount" -> 1;
                        case "getCellAt" -> {
                            int col = (int) args[1];
                            yield col >= 0 && col < COLS ? lawn.cells[col] : null;
                        }
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
                        case "damagePlant" -> {
                            ((PlantInstance) args[0]).takeDamage((int) args[1]);
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
