package view.gui.anim.plant;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.EffectPamPaths;

import java.util.List;
import java.util.Map;

/**
 * Attack/death explosion PAM bursts for implemented explosive plants.
 */
public final class ExplosivePlantFx {
    public enum Layer { BACK, FRONT }

    public enum Placement { AT_PLANT, ALONG_LANE }

    public record Spec(String pamPath, String clipName, Layer layer, Placement placement) {
        public Spec(String pamPath, String clipName, Layer layer) {
            this(pamPath, clipName, layer, Placement.AT_PLANT);
        }
    }

    private static final String[] JALAPENO_CLIPS = {"idle", "idle2", "idle3"};

    private static final Map<String, List<Spec>> BY_NAME = Map.of(
            "Potato Mine", List.of(
                    new Spec(EffectPamPaths.POTATO_MINE_EXPLOSION, "animation", Layer.FRONT)),
            "Primal Potato Mine", List.of(
                    new Spec(EffectPamPaths.PRIMAL_POTATO_MINE_EXPLOSION, "animation", Layer.FRONT)),
            "Cherry Bomb", List.of(
                    new Spec(EffectPamPaths.CHERRY_BOMB_EXPLOSION_REAR, EffectPamPaths.CHERRY_BOMB_CLIP, Layer.BACK),
                    new Spec(EffectPamPaths.CHERRY_BOMB_EXPLOSION_TOP, EffectPamPaths.CHERRY_BOMB_CLIP, Layer.FRONT)),
            "Jalapeno", List.of(
                    new Spec(EffectPamPaths.JALAPENO_FIRE, "idle", Layer.FRONT, Placement.ALONG_LANE)),
            "Explode-o-nut", List.of(
                    new Spec(EffectPamPaths.GENERIC_EXPLOSION_FRONT, "animation", Layer.FRONT))
    );

    private ExplosivePlantFx() {}

    public static boolean isDeathDetonator(PlantInstance plant) {
        return "Explode-o-nut".equals(plantName(plant));
    }

    /**
     * True when this plant's current pose is the detonation clip (not Explode-o-nut,
     * which detonates on death with no attack presentation).
     */
    public static boolean shouldSpawn(PlantInstance plant, AnimPose pose) {
        if (plant == null || pose == null || isDeathDetonator(plant)) {
            return false;
        }
        if (!BY_NAME.containsKey(plantName(plant))) {
            return false;
        }
        if (pose.role() == PlantAnimRole.ATTACK) {
            return true;
        }
        String clip = pose.clipName();
        return "attack".equalsIgnoreCase(clip);
    }

    public static List<Spec> specsFor(PlantInstance plant) {
        return specsForName(plantName(plant));
    }

    public static List<Spec> specsForName(String definitionName) {
        List<Spec> specs = BY_NAME.get(definitionName);
        return specs == null ? List.of() : specs;
    }

    public static String jalapenoClip(int column) {
        return JALAPENO_CLIPS[Math.floorMod(column, JALAPENO_CLIPS.length)];
    }

    private static String plantName(PlantInstance plant) {
        if (plant == null || plant.getDefinition() == null) {
            return null;
        }
        return plant.getDefinition().getName();
    }
}
