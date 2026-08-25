package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Decoration image that can be dragged and scaled (layout-debug tool).
 * Scale is driven by {@link view.gui.debug.DecoLayoutDebugger} (scroll / keys).
 */
public final class DraggableDecoActor extends Image {
    private static final Color IDLE = new Color(1f, 1f, 1f, 0.95f);
    private static final Color SELECTED = new Color(1f, 1f, 0.55f, 1f);

    private final String assetId;
    private final String printName;
    private final float naturalW;
    private final float naturalH;
    private float scale;
    private float grabX;
    private float grabY;
    private boolean selected;
    private Runnable onSelected;

    public DraggableDecoActor(String assetId, String printName, TextureRegion region,
                              float x, float y, float scale) {
        super(new TextureRegionDrawable(region));
        this.assetId = assetId;
        this.printName = printName == null || printName.isBlank() ? assetId : printName;
        this.naturalW = region.getRegionWidth();
        this.naturalH = region.getRegionHeight();
        this.scale = scale;
        setTouchable(Touchable.enabled);
        applyScale();
        setPosition(x, y);
        setColor(IDLE);

        addListener(new DragListener() {
            {
                setTapSquareSize(0f);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (onSelected != null) {
                    onSelected.run();
                }
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void dragStart(InputEvent event, float x, float y, int pointer) {
                grabX = x;
                grabY = y;
                toFront();
                event.stop();
            }

            @Override
            public void drag(InputEvent event, float x, float y, int pointer) {
                Actor a = DraggableDecoActor.this;
                a.setPosition(a.getX() + x - grabX, a.getY() + y - grabY);
                event.stop();
            }

            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer) {
                event.stop();
            }
        });
    }

    public void setOnSelected(Runnable onSelected) {
        this.onSelected = onSelected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        setColor(selected ? SELECTED : IDLE);
        if (selected) {
            toFront();
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public String assetId() {
        return assetId;
    }

    public String printName() {
        return printName;
    }

    public float decoScale() {
        return scale;
    }

    /** Multiplicative nudge used by scroll wheel / keyboard. */
    public void nudgeScale(float factor) {
        setDecoScale(Math.max(0.05f, Math.min(4f, scale * factor)));
    }

    public void setDecoScale(float scale) {
        float cx = getX() + getWidth() * 0.5f;
        float cy = getY() + getHeight() * 0.5f;
        this.scale = scale;
        applyScale();
        setPosition(cx - getWidth() * 0.5f, cy - getHeight() * 0.5f);
    }

    private void applyScale() {
        setSize(naturalW * scale, naturalH * scale);
    }

    /**
     * Paste-ready line: {@code new Deco(id, x, y, scale)}.
     * Scale is the 4th argument; also echoed in the trailing comment.
     */
    public String toDecoJavaLine() {
        return String.format(
            "        new Deco(%s, %6.1ff, %6.1ff, %.2ff), // x=%.1f y=%.1f scale=%.2f",
            printName, getX(), getY(), scale, getX(), getY(), scale);
    }
}
