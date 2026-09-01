package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

final class UiDrawables {
    private UiDrawables() {}

    static Drawable tryDrawable(Skin skin, String name) {
        if (skin == null || name == null) {
            return null;
        }
        try {
            return skin.getDrawable(name);
        } catch (Exception e) {
            return null;
        }
    }

    /** TenPatch drawable PvzSkin registers as {@code name_10}. Stretches; do not scale the raw region. */
    static Drawable tenPatch(Skin skin, String atlasName) {
        return tryDrawable(skin, atlasName + "_10");
    }

    /** Atlas region, or the TenPatch {@code *_10} variant PvzSkin registers. */
    static Drawable tryNamed(Skin skin, String atlasName) {
        Drawable ten = tryDrawable(skin, atlasName + "_10");
        return ten != null ? ten : tryDrawable(skin, atlasName);
    }

    /** Horizontally mirrors draw only; insets stay the same (caller pads content for tail side). */
    static Drawable mirrored(Drawable delegate) {
        return delegate == null ? null : new MirroredDrawable(delegate);
    }

    private static final class MirroredDrawable extends BaseDrawable {
        private final Drawable delegate;
        private final Matrix4 saved = new Matrix4();
        private final Matrix4 flipped = new Matrix4();

        MirroredDrawable(Drawable delegate) {
            this.delegate = delegate;
            setMinWidth(delegate.getMinWidth());
            setMinHeight(delegate.getMinHeight());
            setLeftWidth(delegate.getRightWidth());
            setRightWidth(delegate.getLeftWidth());
            setTopHeight(delegate.getTopHeight());
            setBottomHeight(delegate.getBottomHeight());
        }

        @Override
        public void draw(Batch batch, float x, float y, float width, float height) {
            // getTransformMatrix() hands back the batch's live matrix, so copy before mutating.
            saved.set(batch.getTransformMatrix());
            flipped.set(saved).translate(x + width, y, 0f).scale(-1f, 1f, 1f);
            batch.setTransformMatrix(flipped);
            delegate.draw(batch, 0f, 0f, width, height);
            batch.setTransformMatrix(saved);
        }
    }
}
