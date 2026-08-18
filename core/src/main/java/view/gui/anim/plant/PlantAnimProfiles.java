package view.gui.anim.plant;

import view.gui.anim.plant.exclusive.*;

/**
 * Registration hub for exclusive plant animations.
 *
 * <p>Default clips live in {@link PlantAnimAdapter}. One class per plant that
 * needs a non-default clip, then {@link #registerAll}.
 */
public final class PlantAnimProfiles {
    private PlantAnimProfiles() {}

    public static void registerAll(PlantAnimOverrides overrides) {
        if (overrides == null) {
            return;
        }
        PuffShroomAnim.register(overrides);
        FumeShroomAnim.register(overrides);
        SplitPeaAnim.register(overrides);
        PeapodAnim.register(overrides);
        BowlingBulbAnim.register(overrides);
        KernelPultAnim.register(overrides);
        MagnetShroomAnim.register(overrides);
        CaulipowerAnim.register(overrides);
        TorchwoodAnim.register(overrides);
        HypnoShroomAnim.register(overrides);
        ImitaterAnim.register(overrides);
        LilyPadAnim.register(overrides);
        MintAnim.register(overrides);
        PotatoMineAnim.register(overrides);
        DoomshroomAnim.register(overrides);
        SquashAnim.register(overrides);
        TangleKelpAnim.register(overrides);
        GraveBusterAnim.register(overrides);
        BonkChoyAnim.register(overrides);
        WasabiWhipAnim.register(overrides);
        PhatBeetAnim.register(overrides);
        KiwibeastAnim.register(overrides);
        ChomperAnim.register(overrides);
        SunshroomAnim.register(overrides);
        GoldBloomAnim.register(overrides);
        WallNutFamilyAnim.register(overrides);
    }
}
