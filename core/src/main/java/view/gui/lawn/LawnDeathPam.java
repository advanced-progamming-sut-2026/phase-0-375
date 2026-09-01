package view.gui.lawn;

import model.enums.ZombieSize;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.zombie.OctopusAnim;
import view.gui.anim.zombie.TroglobiteAnim;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/** PAM-name and particle-part classifiers used when spawning death FX. */
final class LawnDeathPam {
    private LawnDeathPam() {
    }

    static String ashPamFor(ZombieInstance zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            return LawnEntityDrawConstants.ZOMBIE_ASH_PAM;
        }
        String name = zombie.getDefinition().getName();
        if ("ZombieLostCityJane".equals(name)) {
            return LawnEntityDrawConstants.JANE_ASH_PAM;
        }
        if ("ZombieArcade".equals(name)
                || TroglobiteAnim.DEFINITION_NAME.equals(name)
                || OctopusAnim.DEFINITION_NAME.equals(name)) {
            return LawnEntityDrawConstants.BIG_ASH_PAM;
        }
        ZombieSize size = zombie.getDefinition().getSize();
        if (size == ZombieSize.LARGE) {
            return LawnEntityDrawConstants.GARGANTUAR_ASH_PAM;
        }
        if (size == ZombieSize.IMP) {
            return LawnEntityDrawConstants.IMP_ASH_PAM;
        }
        return LawnEntityDrawConstants.ZOMBIE_ASH_PAM;
    }

    static boolean isGargantuar(String pam) {
        return pam != null && pam.toUpperCase().contains("GARGANTUAR")
                && !pam.toUpperCase().contains("IMP");
    }

    static boolean isImp(String pam) {
        return pam != null && pam.toUpperCase().contains("IMP");
    }

    static boolean isAllStar(String pam) {
        return pam != null && pam.toUpperCase().contains("ALLSTAR");
    }

    static boolean isArcadeZombie(String pam) {
        return pam != null && pam.toUpperCase().contains("ZOMBIE_80S_ARCADE");
    }

    static boolean isPianoProp(String pam) {
        if (pam == null) {
            return false;
        }
        String upper = pam.toUpperCase();
        return upper.contains("/PIANO/PIANO")
                || (upper.endsWith("PIANO.PAM") && !upper.contains("ZOMBIE_PIANO"));
    }

    static boolean isProspector(String pam) {
        if (pam == null) {
            return false;
        }
        String upper = pam.toUpperCase();
        return upper.contains("ZOMBIE_PROSPECTOR")
                && !upper.contains("BLAST")
                && !upper.contains("SMOKE");
    }

    static boolean isIceAgeHunter(String pam) {
        return pam != null && pam.toUpperCase().contains("ZOMBIE_ICEAGE_HUNTER");
    }

    static boolean popsHeadAndArm(String pam) {
        return isArcadeZombie(pam) || isProspector(pam) || isIceAgeHunter(pam);
    }

    static String deathHeadGroup(String pam) {
        if (isGargantuar(pam)) {
            return LawnEntityDrawConstants.GARGANTUAR_HEAD;
        }
        if (isImp(pam)) {
            return LawnEntityDrawConstants.IMP_HEAD;
        }
        if (isAllStar(pam)) {
            return LawnEntityDrawConstants.ALLSTAR_PARTICLES;
        }
        return null;
    }

    static String[] deathHeadParts(String pam) {
        if (isGargantuar(pam)) {
            return LawnEntityDrawConstants.GARGANTUAR_HEAD_PARTS;
        }
        if (isImp(pam)) {
            return LawnEntityDrawConstants.IMP_HEAD_PARTS;
        }
        if (isAllStar(pam)) {
            return LawnEntityDrawConstants.ALLSTAR_HEAD_PARTS;
        }
        if (isIceAgeHunter(pam)) {
            return LawnEntityDrawConstants.HUNTER_HEAD_PARTS;
        }
        if (popsHeadAndArm(pam)) {
            return LawnEntityDrawConstants.ARCADE_HEAD_PARTS;
        }
        return null;
    }

    static boolean egyptDeathParts(String pam) {
        String upper = pam.toUpperCase();
        return upper.contains("EGYPT") || upper.contains("EXPLORER");
    }

    static void collectLostArmBodyParts(PamPlayer.AnimationPart node, List<String> names) {
        if (node == null) {
            return;
        }
        if (isArmPopPart(node.name)) {
            names.add(node.name);
        }
        if (node.children == null) {
            return;
        }
        for (int i = 0; i < node.children.size(); i++) {
            Object child = node.children.get(i);
            if (child instanceof PamPlayer.AnimationPart part) {
                collectLostArmBodyParts(part, names);
            }
        }
    }

    static boolean isHeadParticlePart(String part) {
        return "particle_head".equals(part)
                || LawnEntityDrawConstants.ALLSTAR_PARTICLES.equals(part)
                || LawnEntityDrawConstants.GARGANTUAR_HEAD.equals(part);
    }

    static boolean isHeadPopPart(String part) {
        return isHeadParticlePart(part)
                || (part != null && part.contains("skull"));
    }

    static void hideInkButter(Map<String, Boolean> vis) {
        for (String part : LawnEntityDrawConstants.INK_BUTTER_PARTS) {
            vis.put(part, Boolean.FALSE);
        }
    }

    static boolean isParticleLimb(String part) {
        return isParticleArmPart(part) || isParticleHandPart(part);
    }

    static boolean isParticleArmPart(String part) {
        return part != null && part.startsWith("particle_arm");
    }

    static boolean isParticleHandPart(String part) {
        return part != null && part.startsWith("particle_hand");
    }

    static boolean isArmPopPart(String part) {
        if (part == null || part.contains("bone")) {
            return false;
        }
        return isParticleLimb(part)
                || part.contains("arm_outer")
                || part.contains("arms_outer")
                || part.contains("hand_outer");
    }

    static boolean isHandParticlePart(String part) {
        return isArmPopPart(part);
    }

    static boolean atOrBelowHalfHp(ZombieInstance zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            return false;
        }
        int max = zombie.getDefinition().getBaseHP();
        return max > 0 && zombie.getCurrentHP() * 2 <= max;
    }

    static float randomHeadThrowDir() {
        return randomHeadThrowDir(ThreadLocalRandom.current());
    }

    static float randomHeadThrowDir(Random rng) {
        return rng.nextBoolean() ? 1f : -1f;
    }
}
