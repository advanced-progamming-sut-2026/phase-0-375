package view.gui.lawn;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Disposable;

/**
 * Draws red grid lines across the playable lawn when enabled in Settings.
 */
public final class LawnGridRenderer implements Disposable {
    private static final Color RED_LINE = new Color(0.95f, 0.15f, 0.15f, 0.75f);
    public static final float LINE_THICKNESS = 2f;

    private final Texture pixel;

    public LawnGridRenderer() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void draw(Batch batch, LawnLayout layout) {
        if (batch == null || layout == null) {
            return;
        }
        int rows = layout.rows();
        int cols = layout.cols();
        float originX = LawnLayout.LAWN_ORIGIN_X;
        float originY = LawnLayout.LAWN_ORIGIN_Y;
        float gridW = LawnLayout.GRID_WIDTH;
        float gridH = LawnLayout.GRID_HEIGHT;
        float halfLine = LINE_THICKNESS * 0.5f;

        Color prev = batch.getColor();
        float pr = prev.r, pg = prev.g, pb = prev.b, pa = prev.a;
        batch.setColor(RED_LINE);

        // Vertical lines (for each column border from 0 to cols)
        for (int c = 0; c <= cols; c++) {
            float x;
            if (c == 0) {
                x = originX;
            } else if (c == cols) {
                x = originX + gridW - LINE_THICKNESS;
            } else {
                x = layout.cellLeft(c) - halfLine;
            }
            batch.draw(pixel, x, originY, LINE_THICKNESS, gridH);
        }

        // Horizontal lines (for each row border from 0 to rows)
        for (int r = 0; r <= rows; r++) {
            float y;
            if (r == 0) {
                y = originY + gridH - LINE_THICKNESS;
            } else if (r == rows) {
                y = originY;
            } else {
                y = layout.cellBottom(r - 1) - halfLine;
            }
            batch.draw(pixel, originX, y, gridW, LINE_THICKNESS);
        }

        batch.setColor(pr, pg, pb, pa);
    }

    @Override
    public void dispose() {
        pixel.dispose();
    }
}
