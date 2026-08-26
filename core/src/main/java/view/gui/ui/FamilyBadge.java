package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import model.plant.definition.Plant;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.AlmanacArt;
import view.gui.assets.SeedPacketIds;

/**
 * Mint-family badge: colored circle behind the white/transparent {@code MINTFAM_*} icon.
 */
public final class FamilyBadge extends Stack {
    private static Texture circle;

    private final Image background;
    private final Image icon;

    public FamilyBadge(TextureBank textures, float size) {
        background = new Image(new TextureRegionDrawable(new TextureRegion(circleTexture())));
        background.setScaling(Scaling.fit);
        background.setColor(Color.WHITE);

        icon = new Image();
        icon.setScaling(Scaling.fit);

        add(background);
        add(icon);
        setSize(size, size);
    }

    public void setPlant(TextureBank textures, Plant plant) {
        Color tint = SeedPacketIds.familyColor(plant);
        background.setColor(tint != null ? tint : Color.GRAY);

        String id = SeedPacketIds.familyIconId(plant);
        TextureRegion region = id != null ? textures.region(id) : null;
        if (region == null) {
            region = textures.region(AlmanacArt.ICON_FAMILY);
        }
        icon.setDrawable(region != null ? new TextureRegionDrawable(region) : null);
        icon.setVisible(region != null);
    }

    private static Texture circleTexture() {
        if (circle != null) {
            return circle;
        }
        int size = 64;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        float r = size * 0.5f;
        float r2 = r * r;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x + 0.5f - r;
                float dy = y + 0.5f - r;
                if (dx * dx + dy * dy <= r2) {
                    pm.drawPixel(x, y);
                }
            }
        }
        circle = new Texture(pm);
        pm.dispose();
        return circle;
    }
}
