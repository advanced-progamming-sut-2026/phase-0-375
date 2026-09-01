package view.gui.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/** Corner anchoring and native sizing for {@link ReactionBubbleWidget}. */
public final class ReactionBubbleLayout {
    public static final String BUBBLE_ATLAS = "SpeechBubble2";
    public static final float PAD_X = 14f;
    /** Extra inset so the mirrored tail + panel width stay on-screen. */
    public static final float PAD_X_RIGHT = 42f;
    public static final float PAD_Y = 16f;
    public static final float PLANT_PAD_Y = 72f;

    public static final float PANEL_W = 420f;
    public static final float PANEL_H = 280f;

    /** Fallback caps when content measurement is unavailable. */
    public static final float PREVIEW_MAX_W = 300f;
    public static final float PREVIEW_MAX_H = 220f;
    public static final float PREVIEW_SIDE_PAD = 18f;
    public static final float PREVIEW_TOP_PAD = 14f;

    public static final float ICON_SCALE = 0.5f;
    public static final float OPEN_SEC = 0.32f;
    public static final float PREVIEW_SEC = 0.22f;
    public static final float PREVIEW_HOLD_SEC = 2.5f;
    public static final float POP_SEC = 0.3f;

    public enum Corner {
        /** Tail bottom-left; plant local + zombie receives from plant. */
        BOTTOM_LEFT,
        /** Tail bottom-right (mirrored); zombie local + plant receives from zombie. */
        BOTTOM_RIGHT
    }

    public record Metrics(float nativeW, float nativeH) {}

    public record PreviewSize(float width, float height) {}

    /** Bubble layout size that wraps {@code contentW}×{@code contentH} plus tail padding. */
    public static PreviewSize previewBubbleSize(Metrics metrics, float contentW, float contentH) {
        float tailPad = Math.max(28f, metrics.nativeH() * 0.22f);
        float w = contentW + PREVIEW_SIDE_PAD * 2f;
        float h = contentH + PREVIEW_TOP_PAD + tailPad;
        float minW = metrics.nativeW() * ICON_SCALE;
        float minH = metrics.nativeH() * ICON_SCALE;
        w = Math.max(minW, Math.min(PREVIEW_MAX_W, w));
        h = Math.max(minH, Math.min(PREVIEW_MAX_H, h));
        return new PreviewSize(w, h);
    }

    private ReactionBubbleLayout() {}

    public static Metrics loadMetrics(Skin skin) {
        Drawable bg = UiDrawables.tenPatch(skin, BUBBLE_ATLAS);
        if (bg == null) {
            return new Metrics(200f, 120f);
        }
        float w = bg.getMinWidth() > 0f ? bg.getMinWidth() : 200f;
        float h = bg.getMinHeight() > 0f ? bg.getMinHeight() : 120f;
        return new Metrics(w, h);
    }

    public static Drawable bubbleBackground(Skin skin) {
        return bubbleBackground(skin, Corner.BOTTOM_LEFT);
    }

    public static Drawable bubbleBackground(Skin skin, Corner corner) {
        Drawable bg = UiDrawables.tenPatch(skin, BUBBLE_ATLAS);
        return isRightCorner(corner) ? UiDrawables.mirrored(bg) : bg;
    }

    public static boolean isRightCorner(Corner corner) {
        return corner == Corner.BOTTOM_RIGHT;
    }

    /** Screen X for a widget whose bubble child has {@link #applyTailOrigin} set. */
    public static float tailAnchorX(Corner corner, float stageW, float layoutW) {
        float padX = isRightCorner(corner) ? PAD_X_RIGHT : PAD_X;
        return tailAnchorX(corner, stageW, padX, layoutW);
    }

    /** Screen X for a widget whose bubble child has {@link #applyTailOrigin} set. */
    public static float tailAnchorX(Corner corner, float stageW, float padX, float layoutW) {
        return corner == Corner.BOTTOM_LEFT ? padX : stageW - padX - layoutW;
    }

    /** Anchors {@code actor}'s tail corner to the screen corner; call after size/scale changes. */
    public static void anchor(Actor actor, Corner corner, float stageW, float padX, float padY) {
        float layoutW = actor.getWidth();
        float x = tailAnchorX(corner, stageW, padX, layoutW);
        actor.setPosition(x, padY);
    }

    /** Transform origin at the bubble tail corner. */
    public static void applyTailOrigin(Group actor, Corner corner, float layoutW) {
        actor.setTransform(true);
        if (corner == Corner.BOTTOM_LEFT) {
            actor.setOrigin(0f, 0f);
        } else {
            actor.setOrigin(layoutW, 0f);
        }
    }

}
