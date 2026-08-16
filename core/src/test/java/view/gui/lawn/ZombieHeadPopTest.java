package view.gui.lawn;

import model.enums.Chapter;
import model.enums.ZombieSize;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;
import view.gui.anim.AnimPose;
import view.gui.anim.zombie.ZombieAnimRole;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieHeadPopTest {

    @Test
    void particleHeadIsTheThrownPartArmsAreNot() {
        assertTrue(LawnEntityRenderer.isHeadParticlePart("particle_head"));
        assertFalse(LawnEntityRenderer.isHeadParticlePart("particle_arm"));
        assertFalse(LawnEntityRenderer.isHeadPopPart("particle_arm"));
        assertTrue(LawnEntityRenderer.isArmPopPart("particle_arm"));
        assertTrue(LawnEntityRenderer.isParticleArmPart("particle_arm_01"));
        assertTrue(LawnEntityRenderer.isArmPopPart("particle_arm_01"));
        assertFalse(LawnEntityRenderer.isHeadParticlePart("particle_hand"));
        assertTrue(LawnEntityRenderer.isHandParticlePart("particle_hand"));
        assertTrue(LawnEntityRenderer.isArmPopPart("zombie_arm_outer_lower"));
        assertTrue(LawnEntityRenderer.isArmPopPart("zombie_egypt_ra_arm_outer_lower"));
        assertFalse(LawnEntityRenderer.isArmPopPart("zombie_arm_outer_upper_bone"));
        assertFalse(LawnEntityRenderer.isHandParticlePart("particle_head"));
    }

    @Test
    void handDropsAtHalfBodyHpNotBefore() {
        ZombieInstance zombie = basic(100);
        assertFalse(LawnEntityRenderer.atOrBelowHalfHp(zombie));
        zombie.setCurrentHP(51);
        assertFalse(LawnEntityRenderer.atOrBelowHalfHp(zombie));
        zombie.setCurrentHP(50);
        assertTrue(LawnEntityRenderer.atOrBelowHalfHp(zombie));
        zombie.setCurrentHP(1);
        assertTrue(LawnEntityRenderer.atOrBelowHalfHp(zombie));
    }

    @Test
    void lostHandHidesParticleHandOnTheBody() {
        AnimPose pose = AnimPose.looping("pam", "walk", ZombieAnimRole.WALK)
                .withHiddenParts("particle_hand");
        assertEquals(Boolean.FALSE, pose.visibility().get("particle_hand"));
    }

    @Test
    void headThrowDirHitsForwardAndBack() {
        boolean forward = false;
        boolean back = false;
        Random rng = new Random(0);
        for (int i = 0; i < 64; i++) {
            float dir = LawnEntityRenderer.randomHeadThrowDir(rng);
            assertEquals(1f, Math.abs(dir), 0f);
            if (dir > 0f) {
                forward = true;
            } else {
                back = true;
            }
        }
        assertTrue(forward && back);
    }

    private static ZombieInstance basic(int hp) {
        Zombie definition = new Zombie(
                "ZombieBasic", hp, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 100, 1, List.of(), null, null, List.of());
        return new ZombieInstance(definition);
    }
}
