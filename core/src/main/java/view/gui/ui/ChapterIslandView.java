package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.ChapterIslandArt;
import view.gui.assets.PvzAssets;

/**
 * Draws one adventure chapter thumbnail ({@code IMAGE_UI_UNIVERSE_WORLDS_*}).
 * Locked chapters dim and show a padlock badge. Respects actor scale / alpha.
 */
public final class ChapterIslandView extends Actor {
    private static final float LOCKED_ALPHA = 0.5f;
    /** Fixed body height in UI px at scale 1 so every locked chapter matches. */
    private static final float LOCK_BODY_HEIGHT = 70f;

    private final PvzAssets assets;
    private String imageId;
    private boolean unlocked = true;
    /** Fraction of this actor's box the art should fill (0–1). */
    private float fill = 0.85f;

    public ChapterIslandView(PvzAssets assets) {
        this.assets = assets;
        setSize(720f, 520f);
        setOrigin(getWidth() * 0.5f, getHeight() * 0.5f);
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    /** How much of the actor box to fill (e.g. {@code 0.7} = 70%). */
    public void setFill(float fill) {
        this.fill = Math.max(0.1f, Math.min(1f, fill));
    }

    @Override
    public void sizeChanged() {
        setOrigin(getWidth() * 0.5f, getHeight() * 0.5f);
    }

    /**
     * Hit-test in unscaled local space so side cards (scaled down) don't steal
     * clicks from nearby nav buttons with an oversized full-size box.
     */
    @Override
    public Actor hit(float x, float y, boolean touchable) {
        if (touchable && getTouchable() != com.badlogic.gdx.scenes.scene2d.Touchable.enabled) {
            return null;
        }
        float ox = getOriginX();
        float oy = getOriginY();
        float sx = getScaleX();
        float sy = getScaleY();
        if (sx == 0f || sy == 0f) {
            return null;
        }
        float localX = (x - ox) / sx + ox;
        float localY = (y - oy) / sy + oy;
        return localX >= 0 && localX < getWidth() && localY >= 0 && localY < getHeight()
                ? this
                : null;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (imageId == null || assets == null) {
            return;
        }
        TextureBank textures = assets.textures;
        TextureRegion region = textures.region(imageId);
        if (region == null) {
            return;
        }

        float scaleX = getScaleX();
        float scaleY = getScaleY();
        float cx = getX() + getOriginX() + (getWidth() * 0.5f - getOriginX()) * scaleX;
        float cy = getY() + getOriginY() + (getHeight() * 0.5f - getOriginY()) * scaleY;

        float a = parentAlpha * getColor().a * (unlocked ? 1f : LOCKED_ALPHA);
        Color old = batch.getColor();
        batch.setColor(old.r, old.g, old.b, a);

        float fit = Math.min(getWidth() / region.getRegionWidth(), getHeight() / region.getRegionHeight()) * fill;
        float w = region.getRegionWidth() * fit * scaleX;
        float h = region.getRegionHeight() * fit * scaleY;
        batch.draw(region, cx - w * 0.5f, cy - h * 0.5f, w, h);

        if (!unlocked) {
            float lockAlpha = parentAlpha * getColor().a;
            drawLock(batch, textures, cx, cy, Math.min(scaleX, scaleY), lockAlpha, old);
        }

        batch.setColor(old);
    }

    /** Body first, then shackle on top — same size on every chapter. */
    private static void drawLock(Batch batch, TextureBank textures, float cx, float cy,
                                 float actorScale, float alpha, Color old) {
        TextureRegion body = textures.region(ChapterIslandArt.LOCK_BODY);
        TextureRegion shackle = textures.region(ChapterIslandArt.LOCK_SHACKLE);
        if (body == null) {
            return;
        }

        float scale = (LOCK_BODY_HEIGHT * actorScale) / body.getRegionHeight();
        float bodyW = body.getRegionWidth() * scale;
        float bodyH = body.getRegionHeight() * scale;

        float shackW = 0f;
        float shackH = 0f;
        if (shackle != null) {
            shackW = shackle.getRegionWidth() * scale;
            shackH = shackle.getRegionHeight() * scale;
        }

        float overlap = shackH * 0.35f;
        float totalH = bodyH + Math.max(0f, shackH - overlap);
        float baseY = cy - totalH * 0.5f;

        batch.setColor(old.r, old.g, old.b, alpha);
        batch.draw(body, cx - bodyW * 0.5f, baseY, bodyW, bodyH);
        if (shackle != null) {
            float shackY = baseY + bodyH - overlap;
            batch.draw(shackle, cx - shackW * 0.5f, shackY, shackW, shackH);
        }
    }
}
