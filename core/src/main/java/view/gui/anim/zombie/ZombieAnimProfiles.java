package view.gui.anim.zombie;

/**
 * Zombie-team registration hub for exclusive zombie animation profiles.
 *
 * <p>Prefer one class per special zombie with {@code register(ZombieAnimOverrides)},
 * then call it from {@link #registerAll}.
 */
public final class ZombieAnimProfiles {
    private ZombieAnimProfiles() {}

    public static void registerAll(ZombieAnimOverrides overrides) {
        if (overrides == null) {
            return;
        }
        // TODO: NewspaperAnim.register(overrides);
        // TODO: GargantuarAnim.register(overrides);
        // TODO: ExplorerAnim.register(overrides);
    }
}
