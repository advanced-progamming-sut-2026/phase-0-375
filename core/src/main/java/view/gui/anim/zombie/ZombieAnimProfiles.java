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
        GargantuarAnim.register(overrides);
        ImpAnim.register(overrides);
        AllStarAnim.register(overrides);
        ArcadeAnim.register(overrides);
        CrystalSkullAnim.register(overrides);
        RaAnim.register(overrides);
        ProspectorAnim.register(overrides);
        PianoAnim.register(overrides);
        NewspaperAnim.register(overrides);
        BarrelRollerAnim.register(overrides);
        ExplorerAnim.register(overrides);
        HunterAnim.register(overrides);
        TombRaiserAnim.register(overrides);
        DodoAnim.register(overrides);
        TroglobiteAnim.register(overrides);
    }
}
