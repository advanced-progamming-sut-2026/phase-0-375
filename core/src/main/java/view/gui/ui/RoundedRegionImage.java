package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Stretches a texture region over the actor bounds but cuts selected corners to
 * a rounded rectangle, so it can sit against a rounded frame without covering
 * the frame's curved corners.
 *
 * <p>The rounding is done by slicing the corner bands into ~1 unit tall
 * horizontal strips and shrinking each strip by the circle inset, so no shader,
 * mask texture or frame buffer is needed.</p>
 */
public final class RoundedRegionImage extends Actor {
    private final TextureRegion region;
    private float radius;
    private boolean topLeft;
    private boolean topRight;
    private boolean bottomLeft;
    private boolean bottomRight;

    /** All four corners rounded. */
    public RoundedRegionImage(TextureRegion region, float radius) {
        this(region, radius, true, true, true, true);
    }

    /**
     * @param topLeft / topRight / bottomLeft / bottomRight which corners to round
     */
    public RoundedRegionImage(TextureRegion region, float radius,
                              boolean topLeft, boolean topRight,
                              boolean bottomLeft, boolean bottomRight) {
        this.region = region;
        this.radius = radius;
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
    }

    /** Corner radius in UI units. */
    public void setRadius(float radius) { this.radius = radius; }

    public float getRadius() { return radius; }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (region == null) return;
        Color c = getColor();
        batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);

        float w = getWidth();
        float h = getHeight();
        float r = Math.min(radius, Math.min(w, h) * 0.5f);
        boolean any = topLeft || topRight || bottomLeft || bottomRight;
        if (r <= 0f || !any) {
            slice(batch, getX(), getY(), w, h);
            return;
        }

        float topR = (topLeft || topRight) ? r : 0f;
        float botR = (bottomLeft || bottomRight) ? r : 0f;

        // Straight middle band, full width.
        float midY = getY() + botR;
        float midH = h - topR - botR;
        if (midH > 0f) {
            slice(batch, getX(), midY, w, midH);
        }

        int steps = Math.max(1, (int) Math.ceil(r));
        float step = r / steps;
        for (int i = 0; i < steps; i++) {
            float t = (i + 0.5f) * step; // distance from the corner centre line
            float inset = r - (float) Math.sqrt(Math.max(0f, r * r - t * t));

            if (topR > 0f) {
                float left = topLeft ? inset : 0f;
                float right = topRight ? inset : 0f;
                float sw = w - left - right;
                if (sw > 0f) {
                    slice(batch, getX() + left, getY() + h - r + i * step, sw, step);
                }
            }
            if (botR > 0f) {
                float left = bottomLeft ? inset : 0f;
                float right = bottomRight ? inset : 0f;
                float sw = w - left - right;
                if (sw > 0f) {
                    slice(batch, getX() + left, getY() + r - (i + 1) * step, sw, step);
                }
            }
        }
    }

    /** Draws the sub-rectangle of the region that matches the given screen rect. */
    private void slice(Batch batch, float sx, float sy, float sw, float sh) {
        float w = getWidth();
        float h = getHeight();
        float fx0 = (sx - getX()) / w;
        float fx1 = (sx + sw - getX()) / w;
        float fy0 = (sy - getY()) / h;
        float fy1 = (sy + sh - getY()) / h;

        float u = region.getU();
        float u2 = region.getU2();
        float v = region.getV();      // image top edge
        float v2 = region.getV2();    // image bottom edge
        Texture tex = region.getTexture();

        // SpriteBatch maps the bottom-left vertex to (u, v), so the bottom of the
        // rect must carry the image's bottom coordinate.
        batch.draw(tex, sx, sy, sw, sh,
            u + (u2 - u) * fx0, v2 + (v - v2) * fy0,
            u + (u2 - u) * fx1, v2 + (v - v2) * fy1);
    }
}
