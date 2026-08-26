package view.gui.anim.zombie;

import java.util.HashMap;
import java.util.Map;

/**
 * Zombie-team registration hub for per-zombie {@link ZombieGait} values.
 *
 * <p>Anything not registered here walks with {@link ZombieGait#DEFAULT} (measures
 * {@link ZombieGait#GROUND_SWATCH}). Add an entry when stride differs, or {@link ZombieGait#disabled()}
 * when the sheet says {@code GroundTrackName: none}.
 */
public final class ZombieGaitProfiles {
    private static final Map<String, ZombieGait> BY_NAME = createDefaults();

    private ZombieGaitProfiles() {}

    private static Map<String, ZombieGait> createDefaults() {
        Map<String, ZombieGait> gaits = new HashMap<>();
        gaits.put("ZombiePiano", ZombieGait.disabled());
        gaits.put(HunterAnim.DEFINITION_NAME, ZombieGait.of(1f / 3f));
        gaits.put("ZombotanyPeashooter", ZombieGait.disabled());
        gaits.put("ZombotanyWallnut", ZombieGait.disabled());
        gaits.put("ZombotanyJalapeno", ZombieGait.disabled());
        gaits.put("ZombotanySquash", ZombieGait.disabled());
        // TODO: gaits.put("ZombieGargantuar", ZombieGait.of(1f));
        return gaits;
    }

    public static ZombieGait forZombie(String definitionName) {
        if (definitionName == null) {
            return ZombieGait.DEFAULT;
        }
        ZombieGait gait = BY_NAME.get(definitionName);
        return gait != null ? gait : ZombieGait.DEFAULT;
    }
}
