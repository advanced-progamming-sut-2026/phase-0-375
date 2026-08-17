package view.gui.anim.plant.exclusive;

import model.enums.PlantAbilityType;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.EffectPamPaths;
import view.gui.assets.PamCatalog;

public final class PotatoMineAnim {
    private static final float RECOVER_FALLBACK_SECONDS = 0.5f;

    private PotatoMineAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Potato Mine", PotatoMineAnim::resolve);
        overrides.register("Primal Potato Mine", PotatoMineAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }
        return switch (role) {
            case IDLE -> idlePose(plant, entry, role);
            case ATTACK -> AnimPose.once(entry.path(), "attack", role);
            case PLANT_FOOD_ON -> AnimPose.once(entry.path(), "plantfood_on", role);
            case PLANT_FOOD -> AnimPose.looping(entry.path(), "plantfood", role);
            case PLANT_FOOD_OFF -> AnimPose.once(entry.path(), "plantfood_off", role);
        };
    }

    private static AnimPose idlePose(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (!isArmed(plant)) {
            return AnimPose.looping(entry.path(), "plant_idle", role);
        }
        float recoverDur = recoverDuration(entry);
        if (recoverDur > 0f && armedElapsed(plant) < recoverDur) {
            return AnimPose.once(entry.path(), "recover", role);
        }
        return AnimPose.looping(entry.path(), "idle", role);
    }

    private static float recoverDuration(PamCatalog.PamEntry entry) {
        float seconds = PamCatalog.clipDurationSeconds(entry, "recover");
        return seconds > 0f ? seconds : RECOVER_FALLBACK_SECONDS;
    }

    private static float armedElapsed(PlantInstance plant) {
        AbilityState state = plant.getAbilityState(PlantAbilityType.DELAYED_EXPLOSIVE);
        return state == null ? 0f : state.getArmedElapsed();
    }

    /** True when the mine's explode clip is showing — spawn the explosion PAM now. */
    public static boolean shouldSpawnExplosion(PlantInstance plant, AnimPose pose) {
        if (plant == null || pose == null || plant.getDefinition() == null) {
            return false;
        }
        String name = plant.getDefinition().getName();
        if (!"Potato Mine".equals(name) && !"Primal Potato Mine".equals(name)) {
            return false;
        }
        return "attack".equalsIgnoreCase(pose.clipName());
    }

    public static String explosionPamPath() {
        return EffectPamPaths.POTATO_MINE_EXPLOSION;
    }

    private static boolean isArmed(PlantInstance plant) {
        AbilityState state = plant.getAbilityState(PlantAbilityType.DELAYED_EXPLOSIVE);
        return state != null && state.isArmed();
    }
}
