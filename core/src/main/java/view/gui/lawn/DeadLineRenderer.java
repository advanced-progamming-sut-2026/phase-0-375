package view.gui.lawn;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Disposable;

/**
 * Vertical red line at {@code deadLineColumn} (left edge of that column).
 * Used by Wall-nut Bowling and Dead Line levels.
 */
public final class DeadLineRenderer implements Disposable {
    private static final Color LINE = new Color(0.92f, 0.12f, 0.12f, 0.88f);
    static final float LINE_WIDTH = 4f;

    private final Texture pixel;

    public DeadLineRenderer() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
    }

    /**
     * World X of the line's left edge: centred on the left edge of
     * {@code deadLineColumn}.
     */
    static float lineX(LawnLayout layout, int deadLineColumn) {
        return layout.cellLeft(deadLineColumn) - LINE_WIDTH * 0.5f;
    }

    /**
     * Draws the line when {@code deadLineColumn >= 0}. Column index is the
     * first forbidden / non-plantable column (plantable cells are strictly left).
     */
    public void draw(Batch batch, LawnLayout layout, int deadLineColumn) {
        if (batch == null || layout == null || deadLineColumn < 0 || deadLineColumn > layout.cols()) {
            return;
        }
        float x = lineX(layout, deadLineColumn);
        float y = LawnLayout.LAWN_ORIGIN_Y;
        float h = LawnLayout.GRID_HEIGHT;
        Color c = batch.getColor();
        float pr = c.r, pg = c.g, pb = c.b, pa = c.a;
        batch.setColor(LINE);
        batch.draw(pixel, x, y, LINE_WIDTH, h);
        batch.setColor(pr, pg, pb, pa);
    }

    @Override
    public void dispose() {
        pixel.dispose();
    }
}
