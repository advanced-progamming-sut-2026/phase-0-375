package view.gui.anim.zombie;

import java.util.HashMap;
import java.util.Map;

/**
 * Zombie-team registration hub for per-zombie {@link ZombieGait} values.
 *
 * <p>Anything not registered here walks with {@link ZombieGait#DEFAULT}. Add an entry when a
 * zombie's stride differs (Gargantuar covers more ground per cycle) or when it has no footfall
 * to lock at all (swimmers, fliers) — see {@link ZombieGait#disabled()}.
 */
public final class ZombieGaitProfiles {
    private static final Map<String, ZombieGait> BY_NAME = createDefaults();

    private ZombieGaitProfiles() {}

    private static Map<String, ZombieGait> createDefaults() {
        Map<String, ZombieGait> gaits = new HashMap<>();
        // TODO: gaits.put("ZombieGargantuar", ZombieGait.of(1f, "<its planted foot parts>"));
        // TODO: gaits.put("ZombieImp", ZombieGait.of(0.5f, "<its planted foot parts>"));
        // TODO: gaits.put("ZombieBeachSnorkel", ZombieGait.disabled());
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
