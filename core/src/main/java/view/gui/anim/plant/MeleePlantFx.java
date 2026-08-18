package view.gui.anim.plant;

import model.enums.PlantState;
import model.plant.ability.MeleeAbility;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.EffectPamPaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Melee pulse / tile-hit PAM bursts (Kiwibeast, Phat Beet).
 */
public final class MeleePlantFx {
    public enum Layer { BACK, FRONT }

    public enum Kind { IDLE_PULSE, ATTACK_PULSE, PLANT_FOOD_PULSE, TILE_HIT }

    public record Spec(String pamPath, String clipName, Layer layer, Kind kind) {}

    private static final String CLIP = EffectPamPaths.MELEE_PULSE_CLIP;

    private static final Map<String, List<Spec>> BY_NAME = Map.of(
            "Kiwibeast", List.of(
                    new Spec(EffectPamPaths.KIWIBEAST_ATTACK_PULSE, CLIP, Layer.BACK, Kind.ATTACK_PULSE),
                    new Spec(EffectPamPaths.KIWIBEAST_PF_PULSE, CLIP, Layer.BACK, Kind.PLANT_FOOD_PULSE),
                    new Spec(EffectPamPaths.KIWIBEAST_TILE_HIT, CLIP, Layer.FRONT, Kind.TILE_HIT)),
            "Phat Beet", List.of(
                    new Spec(EffectPamPaths.PHAT_BEET_IDLE_PULSE, CLIP, Layer.BACK, Kind.IDLE_PULSE),
                    new Spec(EffectPamPaths.PHAT_BEET_ATTACK_PULSE, CLIP, Layer.BACK, Kind.ATTACK_PULSE),
                    new Spec(EffectPamPaths.PHAT_BEET_PF_PULSE, CLIP, Layer.BACK, Kind.PLANT_FOOD_PULSE),
                    new Spec(EffectPamPaths.PHAT_BEET_TILE_HIT, CLIP, Layer.FRONT, Kind.TILE_HIT))
    );

    private MeleePlantFx() {}

    public static boolean shouldSpawn(PlantInstance plant, AnimPose pose) {
        if (plant == null || plant.getDefinition() == null) {
            return false;
        }
        if (!BY_NAME.containsKey(plant.getDefinition().getName())) {
            return false;
        }
        if (plant.getState() == PlantState.PLANT_FOOD) {
            return true;
        }
        return pose != null && pose.role() == PlantAnimRole.ATTACK;
    }

    public static Spec idlePulseSpec(PlantInstance plant) {
        if (plant == null || plant.getDefinition() == null) {
            return null;
        }
        List<Spec> all = BY_NAME.get(plant.getDefinition().getName());
        if (all == null) {
            return null;
        }
        for (Spec spec : all) {
            if (spec.kind() == Kind.IDLE_PULSE) {
                return spec;
            }
        }
        return null;
    }

    public static List<Spec> specsFor(PlantInstance plant, boolean plantFood) {
        if (plant == null || plant.getDefinition() == null) {
            return List.of();
        }
        List<Spec> all = BY_NAME.get(plant.getDefinition().getName());
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        Kind pulse = plantFood ? Kind.PLANT_FOOD_PULSE : Kind.ATTACK_PULSE;
        List<Spec> out = new ArrayList<>(2);
        for (Spec spec : all) {
            if (spec.kind() == pulse || spec.kind() == Kind.TILE_HIT) {
                out.add(spec);
            }
        }
        return out;
    }

    public static int tileRadius(PlantInstance plant, boolean plantFood) {
        if (plant == null) {
            return 1;
        }
        if (plant.getAbilityStrategy() instanceof MeleeAbility melee) {
            return Math.max(0, plantFood ? melee.plantFoodRadius(plant) : melee.attackRadius(plant));
        }
        return 1;
    }
}
