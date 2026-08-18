package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.item.Grave;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TombRaiserSummonTest {

    private static final float TICK = 0.1f;
    private static final int ROWS = 5;
    private static final int COLS = 9;

    @Test
    void castSpawnsTwoGravesAndPlaysPower() {
        ZombieInstance zombie = raiser();
        SummonBehavior summon = (SummonBehavior) zombie.getBehavior(ZombieBehaviorType.SUMMON);
        Lawn lawn = new Lawn();
        BehaviorContext context = stubContext(lawn);

        summon.setCastTimer(SummonBehavior.TIME_BETWEEN_RAISINGS);
        summon.execute(zombie, context, TICK);
        assertEquals(0, lawn.countOwned(zombie));
        assertTrue(summon.isRaising());
        assertEquals(ZombieState.SPECIAL_ACTION, zombie.getState());

        runFor(summon, zombie, context, SummonBehavior.POWER_DURATION);
        assertEquals(2, lawn.countOwned(zombie));
        assertFalse(summon.isRaising());
        assertEquals(ZombieState.WALKING, zombie.getState());
    }

    @Test
    void capIsSixPerRaiser() {
        ZombieInstance zombie = raiser();
        SummonBehavior summon = (SummonBehavior) zombie.getBehavior(ZombieBehaviorType.SUMMON);
        Lawn lawn = new Lawn();
        fillOwned(lawn, zombie, SummonBehavior.TOMB_CAP);
        BehaviorContext context = stubContext(lawn);

        summon.setCastTimer(SummonBehavior.TIME_BETWEEN_RAISINGS);
        summon.execute(zombie, context, TICK);
        assertEquals(SummonBehavior.TOMB_CAP, lawn.countOwned(zombie));
        assertFalse(summon.isRaising());
        assertEquals(ZombieState.WALKING, zombie.getState());
    }

    @Test
    void fifthGraveLeavesRoomForOneMore() {
        ZombieInstance zombie = raiser();
        SummonBehavior summon = (SummonBehavior) zombie.getBehavior(ZombieBehaviorType.SUMMON);
        Lawn lawn = new Lawn();
        fillOwned(lawn, zombie, 5);
        BehaviorContext context = stubContext(lawn);

        summon.setCastTimer(SummonBehavior.TIME_BETWEEN_RAISINGS);
        summon.execute(zombie, context, TICK);
        assertEquals(5, lawn.countOwned(zombie));
        assertTrue(summon.isRaising());

        runFor(summon, zombie, context, SummonBehavior.POWER_DURATION);
        assertEquals(SummonBehavior.TOMB_CAP, lawn.countOwned(zombie));
        assertFalse(summon.isRaising());
    }

    @Test
    void skipsTilesWithPlants() {
        ZombieInstance zombie = raiser();
        SummonBehavior summon = (SummonBehavior) zombie.getBehavior(ZombieBehaviorType.SUMMON);
        Lawn lawn = new Lawn();
        lawn.plants[0][0] = true;
        BehaviorContext context = stubContext(lawn);

        summon.setCastTimer(SummonBehavior.TIME_BETWEEN_RAISINGS);
        summon.execute(zombie, context, TICK);
        runFor(summon, zombie, context, SummonBehavior.POWER_DURATION);
        assertEquals(2, lawn.countOwned(zombie));
        assertTrue(lawn.graves[0][0] == null);
    }

    @Test
    void otherRaisersTombsDoNotCount() {
        ZombieInstance zombie = raiser();
        ZombieInstance other = raiser();
        SummonBehavior summon = (SummonBehavior) zombie.getBehavior(ZombieBehaviorType.SUMMON);
        Lawn lawn = new Lawn();
        fillOwned(lawn, other, SummonBehavior.TOMB_CAP);
        BehaviorContext context = stubContext(lawn);

        summon.setCastTimer(SummonBehavior.TIME_BETWEEN_RAISINGS);
        summon.execute(zombie, context, TICK);
        assertEquals(0, lawn.countOwned(zombie));
        assertTrue(summon.isRaising());

        runFor(summon, zombie, context, SummonBehavior.POWER_DURATION);
        assertEquals(2, lawn.countOwned(zombie));
        assertEquals(SummonBehavior.TOMB_CAP, lawn.countOwned(other));
        assertFalse(summon.isRaising());
    }

    private static void runFor(SummonBehavior summon, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            summon.execute(zombie, context, TICK);
        }
    }

    private static void fillOwned(Lawn lawn, ZombieInstance raiser, int n) {
        int placed = 0;
        for (int row = 0; row < ROWS && placed < n; row++) {
            for (int col = 0; col < COLS && placed < n; col++) {
                Grave grave = new Grave();
                grave.setRaiser(raiser);
                lawn.graves[row][col] = grave;
                placed++;
            }
        }
    }

    private static ZombieInstance raiser() {
        Zombie definition = new Zombie(
                "ZombieTombRaiser", 380, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 300, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SUMMON));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }

    private static BehaviorContext stubContext(Lawn lawn) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRowCount" -> ROWS;
                    case "getColumnCount" -> COLS;
                    case "countGravesRaisedBy" -> lawn.countOwned((ZombieInstance) args[0]);
                    case "spawnGraveAt" -> {
                        int row = (int) args[0];
                        int col = (int) args[1];
                        if (lawn.hasPlant(row, col)) {
                            yield false;
                        }
                        ZombieInstance owner = args.length > 2 ? (ZombieInstance) args[2] : null;
                        yield lawn.place(row, col, owner);
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class Lawn {
        final Grave[][] graves = new Grave[ROWS][COLS];
        final boolean[][] plants = new boolean[ROWS][COLS];

        boolean hasPlant(int row, int col) {
            return row >= 0 && col >= 0 && row < ROWS && col < COLS && plants[row][col];
        }

        boolean place(int row, int col, ZombieInstance raiser) {
            if (row < 0 || col < 0 || row >= ROWS || col >= COLS || graves[row][col] != null) {
                return false;
            }
            Grave grave = new Grave();
            grave.setRaiser(raiser);
            graves[row][col] = grave;
            return true;
        }

        int countOwned(ZombieInstance raiser) {
            int n = 0;
            for (Grave[] line : graves) {
                for (Grave grave : line) {
                    if (grave != null && grave.getRaiser() == raiser) {
                        n++;
                    }
                }
            }
            return n;
        }
    }
}
