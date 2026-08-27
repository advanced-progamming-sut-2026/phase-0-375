package view.gui.assets;

/**
 * Big Wave Beach Season Map layout. Tune only these values for Beach —
 * Egypt / Frostbite layouts are unaffected.
 */
public final class BeachSeasonMapLayout extends SeasonMapLayout {
    public BeachSeasonMapLayout() {
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
                // L1 ANIM27; L2 ANIM10; L3 ANIM12; L4 ANIM17; L5 zomboss.
                new float[][] {
                        { 480f, 336f },   // L1  W, H  (ANIM27 ~1362×953)
                        { 240f, 220f },   // L2  W, H  (ANIM10 295×271)
                        { 240f, 300f },   // L3  W, H  (ANIM12 335×420)
                        { 250f, 200f },   // L4  W, H  (ANIM17 321×255)
                        { 456f, 552f },   // L5  W, H  (Zomboss 905×1096)
                },

                // ── platformOffsetXy ──────────────────────────────────────
                // Island slide relative to the same-index nodeXy (orb stays put).
                // { dX, dY }  — +X = platform right, +Y = platform up.
                new float[][] {
                        { -540f, 630f },       // L1  dX, dY
                        { 0f, 70f },       // L2  dX, dY
                        { -15f, 78f },       // L3  dX, dY
                        { 3f, 57f },       // L4  dX, dY
                        { 0f, 170f },       // L5  dX, dY
                });
    }
}
