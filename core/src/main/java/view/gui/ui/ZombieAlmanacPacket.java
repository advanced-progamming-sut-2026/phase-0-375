package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.AlmanacZombiePacketIds;

/**
 * Almanac zombie packet: {@link AlmanacZombiePacketIds#READY} or {@link #SELECTED}
 * frame behind the portrait (portrait always on top). Use {@link #setPressed(boolean)}
 * while the pointer is down — not as a full-card overlay.
 */
public final class ZombieAlmanacPacket extends Stack {
    private final TextureBank textures;
    private final float packetW;
    private final float packetH;
    private final Image baseReady;
    private final Image baseSelected;
    private final Image portrait;

    public ZombieAlmanacPacket(TextureBank textures, float width, float height) {
        this.textures = textures;
        this.packetW = width;
        this.packetH = height;
        setTouchable(Touchable.disabled);
        baseReady = frameImage(AlmanacZombiePacketIds.READY);
        baseSelected = frameImage(AlmanacZombiePacketIds.SELECTED);
        baseSelected.setVisible(false);
        portrait = image(null);
        portrait.setFillParent(true);
        add(baseReady);
        add(baseSelected);
        add(portrait);
        setSize(width, height);
    }

    public ZombieAlmanacPacket show(String zombieName, boolean discovered) {
        portrait.setFillParent(true);
        portrait.setScaling(Scaling.fit);
        if (!discovered) {
            portrait.setDrawable(null);
            portrait.setVisible(false);
            return this;
        }
        TextureRegion region = textures.region(AlmanacZombiePacketIds.portraitId(zombieName));
        portrait.setDrawable(region == null ? null : new TextureRegionDrawable(region));
        portrait.setVisible(region != null);
        return this;
    }

    /**
     * Use a spritesheet frame when no {@code IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_*} portrait exists.
     */
    public void setPortraitOverride(TextureRegion region) {
        setPortraitOverride(region, 1f, 0f, 0f);
    }

    /**
     * @param scaleMul extra multiplier on top of the fit-to-packet bounds
     * @param offsetX extra X after horizontal centering (positive = right)
     * @param offsetY extra Y after vertical centering (positive = up)
     */
    public void setPortraitOverride(TextureRegion region, float scaleMul, float offsetX, float offsetY) {
        if (portrait == null || region == null) {
            return;
        }
        float maxH = packetH * 0.78f;
        float maxW = packetW * 0.78f;
        float fit = Math.min(maxW / Math.max(1, region.getRegionWidth()),
                maxH / Math.max(1, region.getRegionHeight()));
        float scale = fit * Math.max(0.1f, scaleMul);
        float pw = region.getRegionWidth() * scale;
        float ph = region.getRegionHeight() * scale;
        portrait.setFillParent(false);
        portrait.setScaling(Scaling.stretch);
        portrait.setDrawable(new TextureRegionDrawable(region));
        portrait.setSize(pw, ph);
        portrait.setPosition(
                (packetW - pw) * 0.5f + offsetX,
                Math.max(2f, (packetH - ph) * 0.5f) + offsetY);
        portrait.setVisible(true);
    }

    /** Swaps READY / SELECTED background while pointer is held. */
    public void setPressed(boolean pressed) {
        baseReady.setVisible(!pressed);
        baseSelected.setVisible(pressed);
    }

    private Image frameImage(String id) {
        Image image = image(id);
        image.setFillParent(true);
        return image;
    }

    private Image image(String id) {
        TextureRegion region = id == null ? null : textures.region(id);
        Image image = region == null ? new Image() : new Image(new TextureRegionDrawable(region));
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }
}
