package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import model.game.core.GameModel;
import pvz.libpvz.textures.TextureBank;

import java.util.Set;

/**
 * I, Zombie brains on the house side of each lane.
 */
public final class BrainLaneRenderer {
    public static final String ATLAS_GROUP = "ZombieTreadmillBrainGroup_768";
    public static final String ATLAS_PAGE = "ATLASIMAGE_ATLAS_ZOMBIETREADMILLBRAINGROUP_768_00";
    public static final String BRAIN_ID = "IMAGE_ZOMBIE_POWER_BRAIN_PROJECTILE_POWER_BRAIN_PROJECTILE_112X82";

    private static final float DRAW_W = 56f;
    private static final float DRAW_H = 41f;
    private static final float X_INSET = 56f;

    private final TextureBank textures;

    public BrainLaneRenderer(TextureBank textures) {
        this.textures = textures;
    }

    public void ensureLoaded() {
        if (textures == null) {
            return;
        }
        textures.loadSync(ATLAS_GROUP);
        textures.loadSync(ATLAS_PAGE);
    }

    public void draw(Batch batch, LawnLayout layout, GameModel model) {
        if (batch == null || layout == null || textures == null) {
            return;
        }
        TextureRegion brain = textures.region(BRAIN_ID);
        if (brain == null) {
            return;
        }
        Set<Integer> breached = model == null ? Set.of() : model.getBreachedRows();
        float cx = LawnLayout.LAWN_ORIGIN_X - X_INSET;
        for (int row = 0; row < layout.rows(); row++) {
            if (breached != null && breached.contains(row)) {
                continue;
            }
            float[] xy = layout.centerOf(row, 0);
            float x = cx - DRAW_W * 0.5f;
            float y = xy[1] - DRAW_H * 0.35f;
            batch.draw(brain, x, y, DRAW_W, DRAW_H);
        }
    }
}
