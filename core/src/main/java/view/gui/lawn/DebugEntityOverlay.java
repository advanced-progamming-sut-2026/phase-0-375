package view.gui.lawn;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Disposable;
import model.game.core.GameModel;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

/**
 * Debug-only lawn markers (colored quads + names) until PAM entity art is wired.
 */
public final class DebugEntityOverlay implements Disposable {
    private static final Color PLANT_COLOR = new Color(0.25f, 0.78f, 0.30f, 0.85f);
    private static final Color ZOMBIE_COLOR = new Color(0.85f, 0.22f, 0.22f, 0.85f);

    private final LawnLayout layout;
    private final Texture pixel;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    public DebugEntityOverlay(LawnLayout layout, BitmapFont font) {
        this.layout = layout;
        this.font = font;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
    }

    public void draw(Batch batch, GameModel model) {
        if (model == null) {
            return;
        }
        for (PlantInstance plant : model.getAllPlants()) {
            Point pos = plant.getPosition();
            if (pos == null) {
                continue;
            }
            String name = plant.getDefinition() != null ? plant.getDefinition().getName() : "?";
            float[] xy = layout.centerOf(pos.getY(), pos.getX());
            drawMarker(batch, xy[0], xy[1], PLANT_COLOR, name);
        }
        for (ZombieInstance zombie : model.getZombies()) {
            FloatPoint cont = zombie.getContinuousPosition();
            Point grid = zombie.getGridPosition();
            float row;
            float progressX;
            if (cont != null) {
                progressX = cont.getX();
                row = cont.getY();
            } else if (grid != null) {
                progressX = grid.getX();
                row = grid.getY();
            } else {
                continue;
            }
            String name = zombie.getDefinition() != null ? zombie.getDefinition().getName() : "?";
            float[] xy = layout.centerOf(Math.round(row), progressX);
            drawMarker(batch, xy[0], xy[1], ZOMBIE_COLOR, name);
        }
    }

    private void drawMarker(Batch batch, float cx, float cy, Color color, String name) {
        float size = 36f;
        batch.setColor(color);
        batch.draw(pixel, cx - size * 0.5f, cy - size * 0.5f, size, size);
        batch.setColor(Color.WHITE);

        if (font != null && name != null) {
            glyphLayout.setText(font, name);
            font.setColor(Color.WHITE);
            font.draw(batch, name, cx - glyphLayout.width * 0.5f, cy + size * 0.5f + glyphLayout.height + 2f);
        }
    }

    @Override
    public void dispose() {
        pixel.dispose();
    }
}
