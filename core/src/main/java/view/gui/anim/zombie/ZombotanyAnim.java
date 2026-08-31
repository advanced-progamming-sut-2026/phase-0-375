package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zombotany.ZombotanyJalapenoBehavior;
import model.zombie.behavior.zombotany.ZombotanyPeashooterBehavior;
import model.zombie.behavior.zombotany.ZombotanySquashBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.PamVisibility;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.anim.plant.exclusive.SquashAnim;
import view.gui.assets.PamCatalog;

import java.util.Map;

/**
 * Zombotany plant-headed zombies: biome basic zombie body with the skull/jaw
 * hidden, and the assigned plant PAM drawn on the neck.
 */
public final class ZombotanyAnim {
    public static final String PEASHOOTER = "ZombotanyPeashooter";
    public static final String WALLNUT = "ZombotanyWallnut";
    public static final String JALAPENO = "ZombotanyJalapeno";
    public static final String SQUASH = "ZombotanySquash";

    public static final float HEAD_SCALE = 0.72f;

    /**
     * Where on the skull box to pin the plant center (0 = top, 1 = bottom / neck).
     * Below 0.5 drops the plant into the stump so it doesn't float.
     */
    public static final float NECK_ANCHOR = 0.78f;
    public static final float HEAD_OFFSET_Y = 15f;
    public static final float HEAD_OFFSET_X = 25f;

    private static final String[] WALLNUT_BODY = {"idle", "damage", "damage2", "damage3"};

    /** Head bits to strip from the basic zombie body. */
    private static final String[] HEAD_PARTS = {
            "zombie_skull", "zombie_jaw",
            "zombie_egypt_skull", "zombie_egypt_jaw",
            "particle_head"};

    private ZombotanyAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(PEASHOOTER, ZombotanyAnim::resolve);
        overrides.register(WALLNUT, ZombotanyAnim::resolve);
        overrides.register(JALAPENO, ZombotanyAnim::resolve);
        overrides.register(SQUASH, ZombotanyAnim::resolve);
    }

    public static boolean isPlantHead(ZombieInstance zombie) {
        return zombie != null && zombie.getDefinition() != null
                && isPlantHeadName(zombie.getDefinition().getName());
    }

    public static boolean isPlantHeadName(String definitionName) {
        return definitionName != null && definitionName.startsWith("Zombotany");
    }

    /** Plant definition name used for catalog / head art. */
    public static String plantDefinitionName(String zombotanyDefinition) {
        if (zombotanyDefinition == null) {
            return null;
        }
        return switch (zombotanyDefinition) {
            case PEASHOOTER -> "Peashooter";
            case WALLNUT -> "Wall-nut";
            case JALAPENO -> "Jalapeno";
            case SQUASH -> "Squash";
            default -> null;
        };
    }

    public static String plantDefinitionName(ZombieInstance zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            return null;
        }
        return plantDefinitionName(zombie.getDefinition().getName());
    }

    /** Hide skull / jaw / particle_head on a basic-body pose. */
    public static AnimPose withHeadHidden(AnimPose pose) {
        if (pose == null) {
            return null;
        }
        return pose.withHiddenParts(HEAD_PARTS);
    }

    public static Map<String, Boolean> headHiddenVisibility(Map<String, Boolean> base) {
        return PamVisibility.hideAlso(base, HEAD_PARTS);
    }

    /**
     * Idle / attack clip for the plant head overlay. Faces left (toward the house)
     * so peashooter shots match the zombie facing.
     */
    public static AnimPose plantHeadPose(ZombieInstance zombie, PamCatalog.PamEntry plantEntry) {
        if (zombie == null || plantEntry == null) {
            return null;
        }
        String clip = plantHeadClip(zombie, plantEntry);
        if (clip == null) {
            return null;
        }
        boolean once = isAttackingHead(zombie);
        AnimPose pose = once
                ? new AnimPose(plantEntry.path(), clip, PlantAnimRole.ATTACK, false, null, HEAD_SCALE)
                : new AnimPose(plantEntry.path(), clip, PlantAnimRole.IDLE, true, null, HEAD_SCALE);
        return pose.flipped();
    }

    private static boolean isAttackingHead(ZombieInstance zombie) {
        ZombotanyPeashooterBehavior pea =
                (ZombotanyPeashooterBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_PEASHOOTER);
        if (pea != null && pea.isAttacking()) {
            return true;
        }
        ZombotanyJalapenoBehavior jala =
                (ZombotanyJalapenoBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_JALAPENO);
        if (jala != null && jala.isAttacking()) {
            return true;
        }
        ZombotanySquashBehavior squash =
                (ZombotanySquashBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_SQUASH);
        return squash != null && squash.isSquashing();
    }

    private static String plantHeadClip(ZombieInstance zombie, PamCatalog.PamEntry entry) {
        ZombotanyPeashooterBehavior pea =
                (ZombotanyPeashooterBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_PEASHOOTER);
        if (pea != null && pea.isAttacking()) {
            return "attack";
        }
        ZombotanyJalapenoBehavior jala =
                (ZombotanyJalapenoBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_JALAPENO);
        if (jala != null && jala.isAttacking()) {
            return "attack";
        }
        ZombotanySquashBehavior squash =
                (ZombotanySquashBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_SQUASH);
        if (squash != null && squash.isSquashing()) {
            float up = PamCatalog.clipDurationSeconds(entry, "jump_up_right");
            if (up <= 0f || squash.getAttackElapsed() < up) {
                return "jump_up_right";
            }
            return "jump_down_right";
        }
        if (WALLNUT.equals(zombie.getDefinition().getName())) {
            return WALLNUT_BODY[healthStage(zombie, WALLNUT_BODY.length)];
        }
        return "idle";
    }

    /** Always fall through to default walk/eat/die; head hide is applied in the adapter. */
    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        return null;
    }

    /** Squash leap timing uses the plant PAM, not the zombie body. */
    public static PamCatalog.PamEntry squashPlantEntry(PamCatalog catalog, ZombieInstance zombie) {
        if (catalog == null) {
            return null;
        }
        String plant = plantDefinitionName(zombie);
        return plant == null ? null : catalog.forPlant(plant);
    }

    /** World-space leap fraction while the squash head is jumping. */
    public static float squashLeapTravel(ZombieInstance zombie, PamCatalog.PamEntry plantEntry) {
        ZombotanySquashBehavior squash =
                (ZombotanySquashBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_SQUASH);
        if (squash == null) {
            return 0f;
        }
        return SquashAnim.leapTravelFraction(squash.getAttackElapsed(), plantEntry, true);
    }

    public static float squashLeapHeight(ZombieInstance zombie, PamCatalog.PamEntry plantEntry,
                                         float travelTiles) {
        ZombotanySquashBehavior squash =
                (ZombotanySquashBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_SQUASH);
        if (squash == null) {
            return 0f;
        }
        return SquashAnim.leapVisualHeightCells(
                squash.getAttackElapsed(), plantEntry, travelTiles, true);
    }

    static int healthStage(ZombieInstance zombie, int stages) {
        if (zombie == null || stages <= 1) {
            return 0;
        }
        int max = zombie.getDefinition() != null ? zombie.getDefinition().getBaseHP() : 0;
        float frac = max <= 0 ? 0f : zombie.getCurrentHP() / (float) max;
        frac = Math.max(0f, Math.min(1f, frac));
        if (frac >= 0.999f) {
            return 0;
        }
        int stage = (int) ((1f - frac) * stages);
        return Math.min(stages - 1, Math.max(0, stage));
    }
}
