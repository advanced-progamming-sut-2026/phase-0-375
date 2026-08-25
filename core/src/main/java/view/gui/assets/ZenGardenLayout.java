package view.gui.assets;

/**
 * Zen Garden background layout in texture pixel space (top-left origin).
 *
 * <p>Atlas region is 1750×774 with baked black pillars. Cover-scale crops those
 * and fills the viewport. Pot centers are in <b>full region</b> coords (not
 * content-cropped), matching {@link #REGION_W}×{@link #REGION_H} cover draw.
 *
 * <p>Tune {@link #SLOT_X} / {@link #SLOT_Y} if a pot drifts off its platform.
 */
public final class ZenGardenLayout {
    public static final float REGION_W = 1750f;
    public static final float REGION_H = 774f;

    /**
     * Platform centers in full region pixel space (top-left origin).
     * Index = model col/row minus 1. Row 0 = back (y=1), row 2 = front (y=3).
     */
    public static final float[] SLOT_X = {620f, 795f, 960f, 1130f};
    /** Slightly below first guess so pot base sits on bamboo. */
    public static final float[] SLOT_Y = {330f, 490f, 655f};

    public static final class CoverTransform {
        public final float scale;
        public final float originX;
        public final float originY;
        public final float drawW;
        public final float drawH;

        public CoverTransform(float viewportW, float viewportH) {
            scale = Math.max(viewportW / REGION_W, viewportH / REGION_H);
            drawW = REGION_W * scale;
            drawH = REGION_H * scale;
            originX = (viewportW - drawW) * 0.5f;
            originY = (viewportH - drawH) * 0.5f;
        }

        public float screenX(float regionX) {
            return originX + regionX * scale;
        }

        public float screenY(float regionY) {
            return originY + (REGION_H - regionY) * scale;
        }
    }

    private ZenGardenLayout() {}
}
