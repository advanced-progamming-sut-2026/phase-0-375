package view.gui.assets;

/**
 * Ancient Egypt Season Map layout. Tune only these values for Egypt.
 */
public final class EgyptSeasonMapLayout extends SeasonMapLayout {
    public EgyptSeasonMapLayout() {
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
                new float[][] {
                        { 360f, 280f },   // L1  W, H  (special start island)
                        { 240f, 180f },   // L2  W, H
                        { 240f, 180f },   // L3  W, H
                        { 240f, 180f },   // L4  W, H
                        { 420f, 460f },   // L5  W, H  (zomboss)
                },

                // ── platformOffsetXy ──────────────────────────────────────
                // Island slide relative to the same-index nodeXy (orb stays put).
                // { dX, dY }  — +X = platform right, +Y = platform up.
                new float[][] {
                        { -85f, 125f },   // L1  dX, dY
                        { 0f, 60f },      // L2  dX, dY
                        { 0f, 85f },      // L3  dX, dY
                        { 0f, 55f },      // L4  dX, dY
                        { -10f, 120f },   // L5  dX, dY
                });
    }
}
