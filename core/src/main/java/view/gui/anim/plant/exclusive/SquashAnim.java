package view.gui.anim.plant.exclusive;

import model.plant.ability.ExplosiveAbility;
import model.plant.ability.PlantAbility;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class SquashAnim {
    private static final float LEAP_PEAK_CELLS = 0.95f;
    private static final float LEAP_PEAK_PER_TILE = 0.15f;
    private static final float LEAP_PEAK_MAX_CELLS = 1.35f;

    private SquashAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Squash", SquashAnim::resolve);
        overrides.registerDuration("Squash", SquashAnim::durationFor);
        overrides.registerImpactFraction("Squash", SquashAnim::impactFractionFor);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }
        return switch (role) {
            case IDLE -> AnimPose.looping(entry.path(), "idle", role);
            case ATTACK -> attackPose(plant, entry, role);
            case PLANT_FOOD_ON, PLANT_FOOD, PLANT_FOOD_OFF ->
                    AnimPose.once(entry.path(),
                            jumpRight(plant) ? "plantfood_jump_down_right" : "plantfood_jump_down_left",
                            role);
            default -> null;
        };
    }

    private static AnimPose attackPose(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        boolean right = jumpRight(plant);
        String upClip = right ? "jump_up_right" : "jump_up_left";
        String downClip = right ? "jump_down_right" : "jump_down_left";
        float upDur = PamCatalog.clipDurationSeconds(entry, upClip);
        float elapsed = plant.getActiveActionElapsed();
        String clip = (upDur <= 0f || elapsed < upDur) ? upClip : downClip;
        return AnimPose.once(entry.path(), clip, role);
    }

    private static float durationFor(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (role != PlantAnimRole.ATTACK || entry == null) {
            return 0f;
        }
        return attackDurationSeconds(entry, jumpRight(plant));
    }

    private static float impactFractionFor(PlantInstance plant, PamCatalog.PamEntry entry,
                                           PlantAnimRole role) {
        if (role != PlantAnimRole.ATTACK || entry == null) {
            return 0f;
        }
        boolean right = jumpRight(plant);
        float up = PamCatalog.clipDurationSeconds(entry, right ? "jump_up_right" : "jump_up_left");
        float total = attackDurationSeconds(entry, right);
        if (total <= 0f || up <= 0f) {
            return 0.5f;
        }
        return up / total;
    }

    /** Jump-up + jump-down length for the chosen facing. */
    public static float attackDurationSeconds(PamCatalog.PamEntry entry, boolean right) {
        if (entry == null) {
            return 0f;
        }
        String up = right ? "jump_up_right" : "jump_up_left";
        String down = right ? "jump_down_right" : "jump_down_left";
        float total = Math.max(0f, PamCatalog.clipDurationSeconds(entry, up))
                + Math.max(0f, PamCatalog.clipDurationSeconds(entry, down));
        return total;
    }

    /** World-space leap progress 0..1 */
    public static float leapTravelFraction(PlantInstance plant, PamCatalog.PamEntry entry) {
        if (plant == null) {
            return 0f;
        }
        return leapTravelFraction(plant.getActiveActionElapsed(), entry, jumpRight(plant));
    }

    /** World-space leap progress 0..1 for a known attack elapsed time. */
    public static float leapTravelFraction(float elapsed, PamCatalog.PamEntry entry, boolean right) {
        if (entry == null) {
            return 0f;
        }
        float up = PamCatalog.clipDurationSeconds(entry, right ? "jump_up_right" : "jump_up_left");
        if (up <= 0f) {
            float total = attackDurationSeconds(entry, right);
            return total <= 0f ? 0f : Math.min(1f, elapsed / total);
        }
        return Math.min(1f, elapsed / up);
    }

    /** Extra height above the lawn in cell units while leaping. */
    public static float leapVisualHeightCells(PlantInstance plant, PamCatalog.PamEntry entry,
                                             float travelTiles) {
        if (plant == null) {
            return 0f;
        }
        return leapVisualHeightCells(plant.getActiveActionElapsed(), entry, travelTiles,
                jumpRight(plant));
    }

    /** Extra leap height for a known attack elapsed time. */
    public static float leapVisualHeightCells(float elapsed, PamCatalog.PamEntry entry,
                                             float travelTiles, boolean right) {
        if (entry == null) {
            return 0f;
        }
        float total = attackDurationSeconds(entry, right);
        if (total <= 0f) {
            return 0f;
        }
        float t = elapsed / total;
        if (t <= 0f || t >= 1f) {
            return 0f;
        }
        float peak = Math.min(LEAP_PEAK_MAX_CELLS,
                LEAP_PEAK_CELLS + Math.max(0f, travelTiles) * LEAP_PEAK_PER_TILE);
        return 4f * peak * t * (1f - t);
    }

    private static boolean jumpRight(PlantInstance plant) {
        if (plant == null) {
            return true;
        }
        PlantAbility ability = plant.getAbilityStrategy();
        if (ability instanceof ExplosiveAbility explosive) {
            return explosive.isSmashJumpRight();
        }
        return true;
    }
}
