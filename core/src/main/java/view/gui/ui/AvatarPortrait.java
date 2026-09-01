package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.AvatarArt;

/**
 * Square avatar face with optional PvZ2 joust border frame.
 */
public final class AvatarPortrait extends Stack {
    private final Image face;

    public AvatarPortrait(TextureBank textures, int avatarId, float size, boolean border) {
        face = new Image();
        face.setScaling(Scaling.fit);
        face.setTouchable(Touchable.disabled);
        add(face);

        if (border) {
            TextureRegion frame = AvatarArt.region(textures, AvatarArt.BORDER);
            if (frame != null) {
                Image borderImage = new Image(new TextureRegionDrawable(frame));
                borderImage.setScaling(Scaling.fit);
                borderImage.setTouchable(Touchable.disabled);
                add(borderImage);
            }
        }

        setSize(size, size);
        setTouchable(Touchable.disabled);
        setAvatarId(textures, avatarId);
    }

    public void setAvatarId(TextureBank textures, int avatarId) {
        TextureRegion region = AvatarArt.faceRegion(textures, avatarId);
        if (region != null) {
            face.setDrawable(new TextureRegionDrawable(region));
        }
    }
}
