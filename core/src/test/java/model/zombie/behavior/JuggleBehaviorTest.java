package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.projectile.Splash;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JuggleBehaviorTest {

    private static final float TICK = 0.1f;
    private static final int COL = 6;
    private static final int ROW = 2;
    private static final float SPEED = 0.2f;

    @Test
    void peaStartsSpinupThenLoopsSpinThenSpindown() {
        ZombieInstance zombie = juggler();
        JuggleBehavior juggle = juggleOf(zombie);
        Pellet pea = peaAt(COL);
        BehaviorContext context = stubContext(pea);

        juggle.execute(zombie, context, TICK);
        assertEquals(JuggleBehavior.JugglePhase.SPINUP, juggle.getPhase());
        assertTrue(juggle.isSpinning());
        assertEquals(0f, juggle.getClipTimer(), 1e-4f);
        assertEquals(ZombieState.WALKING, zombie.getState());
        assertTrue(pea.isReflected());
        assertEquals(SPEED * JuggleBehavior.DEFAULT_SPIN_SPEED_MULTIPLIER,
                zombie.getCurrentSpeed(), 1e-4f);

        runFor(juggle, zombie, stubContext(), JuggleBehavior.SPINUP_DURATION);
        assertEquals(JuggleBehavior.JugglePhase.SPIN, juggle.getPhase());
        assertTrue(juggle.isSpinning());

        runFor(juggle, zombie, stubContext(), JuggleBehavior.SPIN_TIMEOUT - TICK);
        assertEquals(JuggleBehavior.JugglePhase.SPIN, juggle.getPhase());

        juggle.execute(zombie, stubContext(), TICK);
        assertEquals(JuggleBehavior.JugglePhase.SPINDOWN, juggle.getPhase());
        assertFalse(juggle.isSpinning());
        assertEquals(0f, juggle.getClipTimer(), 1e-4f);
        assertEquals(SPEED, zombie.getCurrentSpeed(), 1e-4f);

        runFor(juggle, zombie, stubContext(), JuggleBehavior.SPINDOWN_DURATION);
        assertEquals(JuggleBehavior.JugglePhase.IDLE, juggle.getPhase());
        assertEquals(ZombieState.WALKING, zombie.getState());
    }

    @Test
    void peaDuringSpindownRestartsSpinup() {
        ZombieInstance zombie = juggler();
        JuggleBehavior juggle = juggleOf(zombie);

        juggle.execute(zombie, stubContext(peaAt(COL)), TICK);
        runFor(juggle, zombie, stubContext(), JuggleBehavior.SPINUP_DURATION);
        runFor(juggle, zombie, stubContext(), JuggleBehavior.SPIN_TIMEOUT);
        assertEquals(JuggleBehavior.JugglePhase.SPINDOWN, juggle.getPhase());

        Pellet again = peaAt(COL);
        juggle.execute(zombie, stubContext(again), TICK);
        assertEquals(JuggleBehavior.JugglePhase.SPINUP, juggle.getPhase());
        assertEquals(0f, juggle.getClipTimer(), 1e-4f);
        assertTrue(again.isReflected());
    }

    @Test
    void lobPassesThrough() {
        ZombieInstance zombie = juggler();
        JuggleBehavior juggle = juggleOf(zombie);
        Splash lob = new Splash(40, new FloatPoint(COL, ROW), ROW, 3f);

        juggle.execute(zombie, stubContext(lob), TICK);
        assertEquals(JuggleBehavior.JugglePhase.IDLE, juggle.getPhase());
        assertFalse(lob.isReflected());
        assertEquals(0, juggle.getReflectedCount());
    }

    private static void runFor(JuggleBehavior juggle, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            juggle.execute(zombie, context, TICK);
        }
    }

    private static JuggleBehavior juggleOf(ZombieInstance zombie) {
        return (JuggleBehavior) zombie.getBehavior(ZombieBehaviorType.JUGGLE);
    }

    private static ZombieInstance juggler() {
        Zombie definition = new Zombie(
                "ZombieDarkJuggler", 420, SPEED, 100f, ZombieSize.NORMAL,
                Chapter.DARK_AGES, 450, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.JUGGLE));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setGridPosition(new Point(COL, ROW));
        zombie.setContinuousPosition(new FloatPoint(COL, ROW));
        zombie.setState(ZombieState.WALKING);
        return zombie;
    }

    private static Pellet peaAt(int col) {
        return new Pellet(20, new FloatPoint(col, ROW), ROW, 5f);
    }

    private static BehaviorContext stubContext(Projectile... shots) {
        List<Projectile> lane = new ArrayList<>();
        if (shots != null) {
            for (Projectile shot : shots) {
                if (shot != null) {
                    lane.add(shot);
                }
            }
        }
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> {
                    if ("getProjectilesInLane".equals(method.getName())) {
                        return lane;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
