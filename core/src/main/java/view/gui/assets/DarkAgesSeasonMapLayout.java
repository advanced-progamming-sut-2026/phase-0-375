package view.gui.assets;

/**
 * Dark Ages Season Map layout. Tune only these values for Dark Ages —
 * other season layouts are unaffected.
 */
public final class DarkAgesSeasonMapLayout extends SeasonMapLayout {
    public DarkAgesSeasonMapLayout() {
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
                        { 1900f, 540f },  // L5  X, Y (stub / zomboss)
                },

                // ── platformSizeWh ────────────────────────────────────────
                // Drawn platform size per level.
                // { W, H }  — width × height in map pixels.
                // L1 ANIM1 PAM; L2 island7; L3 ANIM10 PAM; L4 island6; L5 zomboss.
                new float[][] {
                        { 432f, 504f },   // L1  W, H  (ANIM1 1201×1413)
                        { 240f, 200f },   // L2  W, H  (ISLAND7 189×158)
                        { 250f, 255f },   // L3  W, H  (ANIM10 352×358)
                        { 240f, 180f },   // L4  W, H  (ISLAND6 181×135)
                        { 456f, 552f },   // L5  W, H  (Zomboss 905×1096)
                },

                // ── platformOffsetXy ──────────────────────────────────────
                // Island slide relative to the same-index nodeXy (orb stays put).
                // { dX, dY }  — +X = platform right, +Y = platform up.
                new float[][] {
                        { -100f, 120f },       // L1  dX, dY
                        { 0f, 60f },       // L2  dX, dY
                        { 0f, 60f },       // L3  dX, dY
                        { 0f, 50f },       // L4  dX, dY
                        { 0f, 200f },       // L5  dX, dY
                });
    }
}
