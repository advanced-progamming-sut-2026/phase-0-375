package view.gui.anim.plant.exclusive;

import model.enums.BowlingBulbType;
import model.plant.ability.ShooterAbility;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class BowlingBulbAnim {
    private BowlingBulbAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Bowling Bulb", BowlingBulbAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }

        if (role == PlantAnimRole.IDLE) {
            return AnimPose.looping(entry.path(), "idle", role);
        }

        if (role == PlantAnimRole.ATTACK) {
            if (!(plant.getAbilityStrategy() instanceof ShooterAbility)) {
                return null;
            }

            AbilityState state = plant.getAbilityState(plant.getDefinition().getAbilityType());
            int loadedIndex = loadedBulbIndex(state);
            BowlingBulbType loaded = ShooterAbility.bulbTypeForCycleIndex(loadedIndex);
            return bulbPose(entry, role, loaded);
        }

        if (role == PlantAnimRole.PLANT_FOOD) {
            float remaining = plant.getPlantFoodDurationRemaining();
            float onDur = PamCatalog.clipDurationSeconds(entry, "plantfood_on");
            float cyanDur = PamCatalog.clipDurationSeconds(entry, "plantfood1");
            float blueDur = PamCatalog.clipDurationSeconds(entry, "plantfood2");
            float orangeDur = PamCatalog.clipDurationSeconds(entry, "plantfood3");
            float elapsed = PlantInstance.PLANT_FOOD_DURATION - remaining;

            if (cyanDur > 0
                && elapsed - onDur <= cyanDur
                && elapsed - onDur >= 0) {
                return AnimPose.once(entry.path(), "plantfood1", role);
            }
            if (blueDur > 0
                && elapsed >= onDur + cyanDur
                && elapsed <= onDur + cyanDur + blueDur) {
                return AnimPose.once(entry.path(), "plantfood2", role);
            }
            if (orangeDur > 0
                && elapsed <= onDur + cyanDur + blueDur + orangeDur
                && elapsed >= onDur + cyanDur + blueDur) {
                return AnimPose.once(entry.path(), "plantfood3", role);
            }
            return AnimPose.looping(entry.path(), "plantfood_idle", role);
        }

        return null;
    }

    private static int loadedBulbIndex(AbilityState state) {
        int index = (state != null) ? state.getGrowthStage() : 0;
        return ((index % 3) + 3) % 3;
    }

    private static AnimPose bulbPose(PamCatalog.PamEntry entry, PlantAnimRole role,
                                     BowlingBulbType type) {
        String clip = switch (type) {
            case CYAN -> "special";
            case BLUE -> "special2";
            case ORANGE -> "special3";
        };
        return AnimPose.once(entry.path(), clip, role);
    }
}
