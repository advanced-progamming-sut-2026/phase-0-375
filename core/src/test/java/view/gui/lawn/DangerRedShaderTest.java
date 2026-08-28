package view.gui.lawn;

import model.enums.Chapter;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DangerRedShaderTest {

    @Test
    void dangerInactiveWhenZombieIsFarAway() {
        assertFalse(DangerRedShader.isZombieInDangerZone(null));

        ZombieInstance zombie = createZombie(5.0f, 2);
        assertFalse(DangerRedShader.isZombieInDangerZone(zombie));

        zombie.setContinuousPosition(new FloatPoint(2.5f, 2));
        zombie.setGridPosition(new Point(2, 2));
        assertFalse(DangerRedShader.isZombieInDangerZone(zombie));
    }

    @Test
    void dangerActiveWhenZombieInFirstTwoColumns() {
        // Column 1 (< 2.0)
        ZombieInstance zombie = createZombie(1.8f, 2);
        assertTrue(DangerRedShader.isZombieInDangerZone(zombie));

        // Column 0 (< 1.0)
        zombie.setContinuousPosition(new FloatPoint(0.4f, 2));
        zombie.setGridPosition(new Point(0, 2));
        assertTrue(DangerRedShader.isZombieInDangerZone(zombie));
    }

    @Test
    void dangerInactiveWhenZombieIsDyingHypnotizedOrBackward() {
        ZombieInstance zombie = createZombie(0.5f, 2);
        assertTrue(DangerRedShader.isZombieInDangerZone(zombie));

        // Dying state
        zombie.setState(ZombieState.DYING);
        assertFalse(DangerRedShader.isZombieInDangerZone(zombie));

        // Hypnotized
        ZombieInstance hypnotizedZombie = createZombie(0.5f, 2);
        hypnotizedZombie.hypnotise();
        assertFalse(DangerRedShader.isZombieInDangerZone(hypnotizedZombie));

        // Moving backward
        ZombieInstance backwardZombie = createZombie(0.5f, 2);
        backwardZombie.setMovingBackward(true);
        assertFalse(DangerRedShader.isZombieInDangerZone(backwardZombie));

        // Dead / 0 HP
        ZombieInstance deadZombie = createZombie(0.5f, 2);
        deadZombie.takeDamage(9999);
        assertFalse(DangerRedShader.isZombieInDangerZone(deadZombie));
    }

    @Test
    void dangerStrengthStartsWithLesserToneAndPulses() {
        // At start (t = 0.0), intensity should be a softer/lesser red tone (~0.20 - 0.25)
        float startStrength = DangerRedShader.dangerStrength(0.0);
        assertTrue(startStrength <= 0.25f, "Start tone should be lesser: " + startStrength);
        assertTrue(startStrength >= 0.15f, "Start tone should be visible: " + startStrength);

        // At peak of pulse, intensity rises significantly
        float maxStrength = 0f;
        for (double t = 0; t <= 2.0; t += 0.02) {
            float strength = DangerRedShader.dangerStrength(t);
            if (strength > maxStrength) {
                maxStrength = strength;
            }
        }
        assertTrue(maxStrength >= 0.80f, "Peak pulse should be strong: " + maxStrength);
        assertTrue(maxStrength > startStrength * 3.0f, "Peak should be significantly higher than start tone");
    }

    private static ZombieInstance createZombie(float x, int y) {
        Zombie definition = new Zombie(
                "ZombieBasic", 100, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 100, 1, List.of(), null, null, List.of());
        ZombieInstance instance = new ZombieInstance(definition);
        instance.setContinuousPosition(new FloatPoint(x, y));
        instance.setGridPosition(new Point((int) Math.floor(x), y));
        instance.setState(ZombieState.WALKING);
        return instance;
    }
}
