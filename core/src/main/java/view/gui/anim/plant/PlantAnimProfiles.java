package view.gui.anim.plant;

/**
 * Plant-team registration hub for exclusive plant animation profiles.
 *
 * <p>Prefer one class per special plant with {@code register(PlantAnimOverrides)},
 * then call it from {@link #registerAll}.
 */
public final class PlantAnimProfiles {
    private PlantAnimProfiles() {}

    public static void registerAll(PlantAnimOverrides overrides) {
        if (overrides == null) {
            return;
        }
        // TODO: SunshroomAnim.register(overrides);
        // TODO: PotatoMineAnim.register(overrides);
        // TODO: ChardGuardAnim.register(overrides);
        // TODO: DoomshroomAnim.register(overrides);
    }
}
