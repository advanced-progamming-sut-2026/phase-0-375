package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.UiRegions;

/**
 * Preloads and draws main-menu art from libPVZ {@link TextureBank}.
 */
public final class MainMenuArt {
    private boolean loaded;

    public void ensureLoaded(TextureBank textures) {
        if (loaded) {
            return;
        }
        textures.loadSync(UiRegions.ATLAS_MAIN_MENU_BACKGROUND);
        textures.loadSync(UiRegions.ATLAS_UI_MAIN_MENU);
        textures.loadSync(UiRegions.ATLAS_UI_MAIN_MENU_LOGO);
        textures.loadSync(UiRegions.ATLAS_UI_ALWAYS_LOADED);
        loaded = true;
    }

    public TextureRegion region(TextureBank textures, String id) {
        ensureLoaded(textures);
        return textures.region(id);
    }

    /** Full-bleed cosmic main-menu background. */
    public void drawBackground(Batch batch, TextureBank textures, float width, float height) {
        drawBackground(batch, textures, width, height, Color.WHITE);
    }

    /** Full-bleed background with a multiply tint (e.g. turquoise for Adventure). */
    public void drawBackground(Batch batch, TextureBank textures, float width, float height, Color tint) {
        TextureRegion bg = region(textures, UiRegions.MAIN_MENU_BACKGROUND);
        if (bg == null) {
            return;
        }
        Color old = batch.getColor();
        batch.setColor(tint != null ? tint : Color.WHITE);
        batch.draw(bg, 0f, 0f, width, height);
        batch.setColor(old);
    }
}
