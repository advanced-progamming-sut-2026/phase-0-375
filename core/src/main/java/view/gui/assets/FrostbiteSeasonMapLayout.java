package view.gui.assets;

/**
 * Frostbite Caves Season Map layout. Tune only these values for Ice Age —
 * Egypt lives in {@link EgyptSeasonMapLayout} and is unaffected.
 */
public final class FrostbiteSeasonMapLayout extends SeasonMapLayout {
    public FrostbiteSeasonMapLayout() {
        super(
                // ── nodeXy ────────────────────────────────────────────────
                // Marker + path joint per level. Index i = level i+1.
                // { X, Y }  — larger X = right, larger Y = up.
                // Moving a row moves the orb (and path); platform follows via offset.
                new float[][] {
                        { 320f, 480f },   // L1  X, Y
                        { 720f, 360f },   // L2  X, Y
                        { 1100f, 520f },  // L3  X, Y
                        { 1480f, 350f },  // L4  X, Y
                        { 1880f, 560f },  // L5  X, Y (stub / zomboss)
                },

                // ── platformSizeWh ────────────────────────────────────────
                // Drawn platform size per level.
                // { W, H }  — width × height in map pixels.
                // Sized for Ice Age PAM natives (ANIM3 / 12 / 26 / 10 / Zomboss).
                new float[][] {
                        { 432f, 432f },   // L1  W, H  (ANIM3 1307×1318)
                        { 240f, 300f },   // L2  W, H  (ANIM12 400×500)
                        { 260f, 195f },   // L3  W, H  (ANIM26 375×281)
                        { 240f, 300f },   // L4  W, H  (ANIM10 400×500)
                        { 418f, 506f },   // L5  W, H  (Zomboss 1055×1280)
                },

                // ── platformOffsetXy ──────────────────────────────────────
                // Island slide relative to the same-index nodeXy (orb stays put).
                // { dX, dY }  — +X = platform right, +Y = platform up.
                new float[][] {
                        { -500f, 602f },       // L1  dX, dY
                        { 0f, 58f },       // L2  dX, dY
                        { 5f, 50f },       // L3  dX, dY
                        { 0f, 40f },       // L4  dX, dY
                        { 5f, 100f },       // L5  dX, dY
                });
    }
}
