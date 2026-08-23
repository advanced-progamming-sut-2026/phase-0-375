package view.gui.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;

/**
 * Soft black→transparent letterbox fades for menu edges (world-map style).
 */
public final class EdgeFadeOverlay {
    private final Texture fade;
    private final float height;

    public EdgeFadeOverlay(float heightPx) {
        this.height = Math.max(8f, heightPx);
        int h = Math.max(8, Math.round(heightPx));
        Pixmap pm = new Pixmap(1, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) {
            // Pixmap y=0 is top: opaque black → transparent toward bottom of strip.
            float t = y / (float) (h - 1);
            float a = (1f - t) * (1f - t); // ease-out so the fade softens quickly
            pm.setColor(0f, 0f, 0f, a);
            pm.drawPixel(0, y);
        }
        fade = new Texture(pm);
        fade.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
    }

    public void draw(Batch batch, float screenW, float screenH) {
        // Top: opaque at screen top
        batch.draw(fade, 0f, screenH - height, screenW, height);
        // Bottom: flip so opaque is at screen bottom
        batch.draw(fade,
                0f, 0f, screenW, height,
                0, 0, fade.getWidth(), fade.getHeight(),
                false, true);
    }

    public void dispose() {
        fade.dispose();
    }
}
