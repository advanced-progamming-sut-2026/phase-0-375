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
    /** Shared Register/Login full-bleed backdrop under {@code assets/}. */
    public static final String AUTH_BG_RELATIVE =
            "Exports/AI generated/Gemini_Generated_Image_ee6xnxee6xnxee6x.jpg";

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
        drawRegion(batch, textures, UiRegions.MAIN_MENU_BACKGROUND, width, height, tint);
    }

    /** Auth screens: content-downloading banner stretched to fill. */
    public void drawAuthBackground(Batch batch, TextureBank textures, float width, float height) {
        drawRegion(batch, textures, UiRegions.MAIN_MENU_CONTENT_DOWNLOADING, width, height, Color.WHITE);
    }

    private void drawRegion(Batch batch, TextureBank textures, String regionId,
                            float width, float height, Color tint) {
        TextureRegion bg = region(textures, regionId);
        if (bg == null) {
            return;
        }
        Color old = batch.getColor();
        batch.setColor(tint != null ? tint : Color.WHITE);
        batch.draw(bg, 0f, 0f, width, height);
        batch.setColor(old);
    }
}
