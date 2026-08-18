package view.gui.anim.plant.exclusive;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.PamVisibility;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

import java.util.Map;

public final class WallNutFamilyAnim {
    private static final String[] WALLNUT_BODY = {"idle", "damage", "damage2", "damage3"};
    private static final String[] TALLNUT_BODY = {"idle", "damage", "damage2"};
    private static final String[] ENDURIAN_BODY = {"idle", "damage", "damage2", "damage3"};
    private static final String[] ENDURIAN_ATTACK = {
            "attack_loop", "attack_loop_damage", "attack_loop_damage2", "attack_loop_damage3"};
    private static final String[] EXPLODE_BODY = {"idle", "damage", "damage2", "damage3"};
    private static final String[] PUMPKIN_BODY = {"idle", "idle2", "idle3"};
    private static final String[] PUMPKIN_ARMORED = {
            "idle_plantfood", "idle_plantfood2", "idle_plantfood3", "idle_plantfood4"};
    private static final String[] GARLIC_BODY = {"idle", "idle_damage", "idle_damage2"};
    private static final String[] GARLIC_BODY_ALT = {"idle2", "idle2_damage", "idle2_damage2"};
    private static final String[] SWEET_BODY = {"idle", "idle_damage", "idle_damage2", "idle_damage3"};
    private static final String[] SWEET_BODY_ALT = {"idle2", "idle2_damage", "idle2_damage2", "idle2_damage3"};

    private static final String WALLNUT_ARMOR_PARENT = "_wallnut_armor_states";
    private static final String[] WALLNUT_ARMOR_STAGES = {
            "wallnut_plantfood_armor_01",
            "wallnut_plantfood_armor_02",
            "wallnut_plantfood_armor_03"};
    private static final String TALLNUT_ARMOR_PARENT = "_tallnut_plantfood_armor";
    private static final String[] TALLNUT_ARMOR_STAGES = {
            "tallnut_plantfood_armor_norm",
            "tallnut_plantfood_armor_damage_01",
            "tallnut_plantfood_armor_damage_02"};
    private static final String[] ENDURIAN_ARMOR_ALWAYS = {
            "endurian_plantfood_armor",
            "PF_armor_1",
            "PF_spike1", "PF_spike2", "PF_spike3", "PF_spike4",
            "PF_spike5", "PF_spike6", "PF_spike7", "PF_spike8"};
    private static final String[] ENDURIAN_ARMOR_STAGES = {
            "armor_3", "armor_damage_1", "armor_damage_2", "armor_damage_3"};

    private WallNutFamilyAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Wall-nut", WallNutFamilyAnim::wallNut);
        overrides.register("Tall-nut", WallNutFamilyAnim::tallNut);
        overrides.register("Endurian", WallNutFamilyAnim::endurian);
        overrides.register("Garlic", WallNutFamilyAnim::garlic);
        overrides.register("Sweet Potato", WallNutFamilyAnim::sweetPotato);
        overrides.register("Explode-o-nut", WallNutFamilyAnim::explodeONut);
        overrides.register("Pumpkin", WallNutFamilyAnim::pumpkin);
        overrides.register("Sun Bean", WallNutFamilyAnim::sunBean);
    }

    private static AnimPose wallNut(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        return nutWithHelmet(plant, entry, role, WALLNUT_BODY, WALLNUT_ARMOR_PARENT, WALLNUT_ARMOR_STAGES, true);
    }

    private static AnimPose explodeONut(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        return nutWithHelmet(plant, entry, role, EXPLODE_BODY, WALLNUT_ARMOR_PARENT, WALLNUT_ARMOR_STAGES, true);
    }

    private static AnimPose tallNut(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        return nutWithHelmet(plant, entry, role, TALLNUT_BODY, TALLNUT_ARMOR_PARENT, TALLNUT_ARMOR_STAGES, false);
    }

    private static AnimPose nutWithHelmet(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role,
                                          String[] bodyClips, String parent, String[] armorStages,
                                          boolean hasPlantFoodOn) {
        Map<String, Boolean> vis = stagedArmorVis(plant, parent, armorStages);
        if (role == PlantAnimRole.PLANT_FOOD_ON && hasPlantFoodOn) {
            return AnimPose.once(entry.path(), "plantfood_on", role, vis);
        }
        String clip = pick(bodyClips, damageStage(plant, bodyClips.length));
        return AnimPose.looping(entry.path(), clip, role, vis);
    }

    private static AnimPose endurian(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        Map<String, Boolean> vis = endurianArmorVis(plant);
        int stage = damageStage(plant, ENDURIAN_BODY.length);
        return switch (role) {
            case ATTACK -> AnimPose.once(entry.path(), pick(ENDURIAN_ATTACK, stage), role, vis);
            case PLANT_FOOD_ON -> AnimPose.once(entry.path(), "plantfood_on", role, vis);
            default -> AnimPose.looping(entry.path(), pick(ENDURIAN_BODY, stage), role, vis);
        };
    }

    private static AnimPose garlic(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (isPlantFoodRole(role)) {
            return AnimPose.once(entry.path(), "plantfood", role);
        }
        String[] clips = alt(plant) ? GARLIC_BODY_ALT : GARLIC_BODY;
        return AnimPose.looping(entry.path(), pick(clips, damageStage(plant, clips.length)), role);
    }

    private static AnimPose sweetPotato(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (isPlantFoodRole(role)) {
            return AnimPose.once(entry.path(), "plantfood", role);
        }
        String[] clips = alt(plant) ? SWEET_BODY_ALT : SWEET_BODY;
        return AnimPose.looping(entry.path(), pick(clips, damageStage(plant, clips.length)), role);
    }

    private static AnimPose pumpkin(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.hasArmor()) {
            String clip = pick(PUMPKIN_ARMORED, armorStage(plant, PUMPKIN_ARMORED.length));
            return AnimPose.looping(entry.path(), clip, role);
        }
        String clip = pick(PUMPKIN_BODY, damageStage(plant, PUMPKIN_BODY.length));
        return AnimPose.looping(entry.path(), clip, role);
    }

    private static AnimPose sunBean(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        return switch (role) {
            case PLANT_FOOD_ON -> AnimPose.once(entry.path(), "plantfood_on", role);
            case PLANT_FOOD, PLANT_FOOD_OFF -> AnimPose.looping(entry.path(), "plantfood", role);
            default -> AnimPose.looping(entry.path(), alt(plant) ? "idle2" : "idle", role);
        };
    }

    private static Map<String, Boolean> stagedArmorVis(PlantInstance plant, String parent, String[] stages) {
        if (plant == null || !plant.hasArmor() || stages == null || stages.length == 0) {
            return null;
        }
        String stagePart = pick(stages, armorStage(plant, stages.length));
        if (parent == null || parent.isBlank()) {
            return PamVisibility.show(stagePart);
        }
        return PamVisibility.show(parent, stagePart);
    }

    private static Map<String, Boolean> endurianArmorVis(PlantInstance plant) {
        if (plant == null || !plant.hasArmor()) {
            return null;
        }
        String[] parts = new String[ENDURIAN_ARMOR_ALWAYS.length + 1];
        System.arraycopy(ENDURIAN_ARMOR_ALWAYS, 0, parts, 0, ENDURIAN_ARMOR_ALWAYS.length);
        parts[parts.length - 1] = pick(ENDURIAN_ARMOR_STAGES, armorStage(plant, ENDURIAN_ARMOR_STAGES.length));
        return PamVisibility.show(parts);
    }

    /** 0 = healthy … {@code stages-1} = most damaged, from remaining body HP. */
    static int damageStage(PlantInstance plant, int stages) {
        return healthStage(plant.getCurrentHP(), plant.getMaxHP(), stages);
    }

    static int armorStage(PlantInstance plant, int stages) {
        return healthStage(plant.getArmorHP(), Math.max(1, plant.getArmorMaxHP()), stages);
    }

    private static int healthStage(int current, int max, int stages) {
        if (stages <= 1) return 0;
        float frac = max <= 0 ? 0f : current / (float) max;
        frac = Math.max(0f, Math.min(1f, frac));
        int stage = (int) ((1f - frac) * stages);
        if (frac >= 0.999f) {
            return 0;
        }
        return Math.min(stages - 1, Math.max(0, stage));
    }

    private static String pick(String[] clips, int stage) {
        if (clips == null || clips.length == 0) {
            return "idle";
        }
        int i = Math.max(0, Math.min(clips.length - 1, stage));
        return clips[i];
    }

    private static boolean alt(PlantInstance plant) {
        return (System.identityHashCode(plant) & 1) == 1;
    }

    private static boolean isPlantFoodRole(PlantAnimRole role) {
        return role == PlantAnimRole.PLANT_FOOD_ON
                || role == PlantAnimRole.PLANT_FOOD
                || role == PlantAnimRole.PLANT_FOOD_OFF;
    }
}
