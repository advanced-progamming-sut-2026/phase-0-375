package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.SeedPacketIds;

/**
 * Dynamic PvZ2 seed packet compositor. It places IMAGE_UI_PACKETS_&lt;plant&gt;
 * over IMAGE_UI_PACKETS_EMPTY_PACKET, so the same component can be reused by the
 * shop, the greenhouse, mini games and any other packet-like UI.
 *
 * <p>Nothing about the look is hard coded: every caller decides how big the
 * finished packet is drawn and how the plant portrait sits on it. Defaults are
 * neutral (scale 1, plant centred), so a plain
 * {@code new SeedPacketComposite(bank, plant, w, h)} behaves like a normal
 * packet and each screen only overrides what it needs:</p>
 *
 * <pre>
 * new SeedPacketComposite(bank, "Peashooter", 130f, 125f)
 *         .setCompositeScale(1.23f)
 *         .setPlantScale(.70f)
 *         .setPlantShift(0f, .06f);
 * </pre>
 */
public final class SeedPacketComposite extends Stack {
    /** Neutral defaults, used when a caller does not override them. */
    public static final float DEFAULT_COMPOSITE_SCALE = 1f;
    public static final float DEFAULT_PLANT_SCALE = 1f;
    public static final float DEFAULT_PLANT_SHIFT_X = 0f;
    public static final float DEFAULT_PLANT_SHIFT_Y = 0f;

    private final TextureBank textures;
    private final Image base;
    private final Image plant;
    private final Container<Image> plantHolder;

    private float compositeScale = DEFAULT_COMPOSITE_SCALE;
    private float plantScale = DEFAULT_PLANT_SCALE;
    private float plantShiftX = DEFAULT_PLANT_SHIFT_X;
    private float plantShiftY = DEFAULT_PLANT_SHIFT_Y;

    public SeedPacketComposite(TextureBank textures, String plantName, float width, float height) {
        this.textures = textures;
        setTouchable(Touchable.disabled);
        setTransform(true);            // required for setScale to have any effect

        base = image(SeedPacketIds.EMPTY);
        base.setFillParent(true);
        plant = image(SeedPacketIds.portraitId(plantName));

        // Stack forces every direct child to the full packet rect, so the plant
        // lives in a Container that owns its own size and offset instead.
        plantHolder = new Container<>(plant);
        plantHolder.setTouchable(Touchable.disabled);

        add(base);
        add(plantHolder);

        // Sized last: setSize fires sizeChanged(), which needs the children.
        setSize(width, height);
        applyLayout();
    }

    /** Uniform scale of the finished composite, packet and plant together. */
    public SeedPacketComposite setCompositeScale(float scale) {
        this.compositeScale = scale;
        applyLayout();
        return this;
    }

    /** Plant portrait size as a fraction of the packet; 1f fills the packet. */
    public SeedPacketComposite setPlantScale(float scale) {
        this.plantScale = scale;
        applyLayout();
        return this;
    }

    /**
     * Plant nudge as a fraction of the packet size.
     *
     * @param x positive moves right
     * @param y positive moves up
     */
    public SeedPacketComposite setPlantShift(float x, float y) {
        this.plantShiftX = x;
        this.plantShiftY = y;
        applyLayout();
        return this;
    }

    public float getCompositeScale() { return compositeScale; }

    public float getPlantScale() { return plantScale; }

    /** Replaces only the dynamic plant layer while preserving the packet frame. */
    public void setPlant(String plantName) {
        TextureRegion region = textures.region(SeedPacketIds.portraitId(plantName));
        plant.setDrawable(region == null ? null : new TextureRegionDrawable(region));
        plant.setVisible(region != null);
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        // The enclosing cell assigns bounds after construction, so the plant box
        // and the scale origin are recomputed whenever the packet is resized.
        applyLayout();
    }

    private void applyLayout() {
        if (plantHolder == null) return;   // sizeChanged() can fire mid-construction
        float width = getWidth();
        float height = getHeight();
        plantHolder.size(width * plantScale, height * plantScale);
        float shiftX = width * plantShiftX;
        float shiftY = height * plantShiftY;
        plantHolder.padLeft(shiftX).padRight(-shiftX);
        plantHolder.padBottom(shiftY).padTop(-shiftY);
        setOrigin(Align.center);
        setScale(compositeScale);
    }

    private Image image(String id) {
        TextureRegion region = id == null ? null : textures.region(id);
        Image image = region == null ? new Image() : new Image(new TextureRegionDrawable(region));
        image.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }
}
