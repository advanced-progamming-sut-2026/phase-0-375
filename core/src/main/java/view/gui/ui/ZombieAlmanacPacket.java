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
    private final Image baseReady;
    private final Image baseSelected;
    private final Image portrait;

    public ZombieAlmanacPacket(TextureBank textures, float width, float height) {
        this.textures = textures;
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
