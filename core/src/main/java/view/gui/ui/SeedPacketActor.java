package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.SeedPacketIds;

/**
 * PvZ2 seed packet: READY / BOOST / EMPTY chrome, plant portrait, sun cost, level, SELECT hover.
 */
public final class SeedPacketActor extends WidgetGroup {
    /** Native {@code IMAGE_UI_PACKETS_READY} size at 768. */
    public static final float PACKET_WIDTH = 119f;
    public static final float PACKET_HEIGHT = 75f;
    private static final float EMPTY_ALPHA = 0.55f;
    private static final float LOCKED_ALPHA = 0.45f;

    private static final Color COOLDOWN_SHADE_COLOR = new Color(0f, 0f, 0f, 0.55f);
    private static Texture whitePixelTexture;

    private final TextureBank textures;
    private final Image chrome;
    private Image portrait;
    private boolean portraitOverride;
    private final Image lock;
    private final Image select;
    private final Image cooldownShade;
    private final String plantName;
    private final String chromeId;
    private boolean inspected;
    private boolean hovered;
    private boolean dragging;
    private boolean dimmed;
    private float cooldownFraction; // 0 = ready, 1 = just used; shade height scales with this
    private Runnable onClick;
    private DragPlant dragPlant;
    private Label expiryLabel;

    public SeedPacketActor(TextureBank textures, Skin skin, String plantName, int sunCost, int level) {
        this(textures, skin, plantName, sunCost, level, false, false, true);
    }

    public SeedPacketActor(
            TextureBank textures, Skin skin, String plantName, int sunCost, int level,
            boolean boosted, boolean locked) {
        this(textures, skin, plantName, sunCost, level, boosted, locked, true);
    }

    /**
     * @param showCostAndLevel false for conveyor / bowling packets (no sun cost or LVL).
     */
    public SeedPacketActor(
            TextureBank textures, Skin skin, String plantName, int sunCost, int level,
            boolean boosted, boolean locked, boolean showCostAndLevel) {
        this.textures = textures;
        this.plantName = plantName;
        this.chromeId = plantName == null
                ? SeedPacketIds.EMPTY
                : (boosted ? SeedPacketIds.BOOST : SeedPacketIds.READY);
        setSize(PACKET_WIDTH, PACKET_HEIGHT);
        setTouchable(Touchable.enabled);
        chrome = image(textures, chromeId);
        chrome.setFillParent(true);
        styleChrome(locked);
        addActor(chrome);
        addPortraitAndCost(skin, sunCost, level, locked, showCostAndLevel);
        cooldownShade = cooldownShadeImage();
        addActor(cooldownShade);
        layoutCooldownShade();
        lock = locked ? lockImage(textures, skin) : null;
        if (lock != null) {
            addActor(lock);
        }
        select = image(textures, SeedPacketIds.SELECT);
        select.setFillParent(true);
        select.setVisible(false);
        select.setTouchable(Touchable.disabled);
        addActor(select);
        addListener(new PacketInput());
    }

    private void styleChrome(boolean locked) {
        if (plantName == null) {
            chrome.getColor().a = EMPTY_ALPHA;
            setTouchable(Touchable.disabled);
        } else if (locked) {
            chrome.getColor().a = LOCKED_ALPHA;
        }
    }

    private void addPortraitAndCost(Skin skin, int sunCost, int level,
                                    boolean locked, boolean showCostAndLevel) {
        if (plantName == null) {
            portrait = null;
            return;
        }
        portrait = image(textures, SeedPacketIds.portraitId(plantName));
        float ph = portrait.getPrefHeight() > 0 ? portrait.getPrefHeight() : PACKET_HEIGHT * 0.82f;
        float pw = portrait.getPrefWidth() > 0 ? portrait.getPrefWidth() : PACKET_WIDTH * 0.52f;
        portrait.setSize(pw, ph);
        portrait.setPosition(4f, Math.max(2f, (PACKET_HEIGHT - ph) * 0.5f));
        if (locked) {
            portrait.getColor().a = LOCKED_ALPHA;
        }
        addActor(portrait);
        if (!showCostAndLevel) {
            return;
        }
        float textW = PACKET_WIDTH * 0.52f;
        float textX = PACKET_WIDTH * 0.44f;
        addActor(packetLabel(skin, "LVL " + Math.max(1, level), 1.2f,
                textX, PACKET_HEIGHT - 18f, textW, 16f));
        if (!locked) {
            addActor(packetLabel(skin, String.valueOf(sunCost), 2f,
                    textX, 4f, textW, 18f));
        }
    }

    private Image cooldownShadeImage() {
        Image shade = new Image(new TextureRegionDrawable(new TextureRegion(whitePixel())));
        shade.setColor(COOLDOWN_SHADE_COLOR);
        shade.setTouchable(Touchable.disabled);
        shade.setVisible(false);
        return shade;
    }

    private final class PacketInput extends InputListener {
        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (pointer == -1 && plantName != null) {
                hovered = true;
                refreshSelect();
            }
        }

        @Override
        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            if (pointer == -1) {
                hovered = false;
                refreshSelect();
            }
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (dragPlant != null) {
                if (dimmed || plantName == null) {
                    return true;
                }
                dragging = true;
                refreshSelect();
                event.stop();
                dragPlant.dragStart(SeedPacketActor.this);
                return true;
            }
            if (onClick != null) {
                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        public void touchDragged(InputEvent event, float x, float y, int pointer) {
            if (dragging && dragPlant != null) {
                dragPlant.drag(SeedPacketActor.this, event.getStageX(), event.getStageY());
            }
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            if (!dragging || dragPlant == null) {
                return;
            }
            dragging = false;
            refreshSelect();
            dragPlant.dragEnd(SeedPacketActor.this, event.getStageX(), event.getStageY());
        }
    }

    /** In-game: drag a packet onto the lawn to plant it. */
    public interface DragPlant {
        void dragStart(SeedPacketActor packet);

        void drag(SeedPacketActor packet, float stageX, float stageY);

        void dragEnd(SeedPacketActor packet, float stageX, float stageY);
    }

    public static SeedPacketActor empty(TextureBank textures, Skin skin) {
        return new SeedPacketActor(textures, skin, null, 0, 0);
    }

    public void setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
        getColor().a = dimmed ? 0.55f : 1f;
    }

    /**
     * Sets how much of the packet is still covered by the recharge shade.
     * @param fraction 0 = fully ready (no shade), 1 = just used (fully covered);
     *                 values outside [0,1] are clamped
     */
    public void setCooldownFraction(float fraction) {
        this.cooldownFraction = Math.max(0f, Math.min(1f, fraction));
        layoutCooldownShade();
    }

    public float getCooldownFraction() {
        return cooldownFraction;
    }

    private void layoutCooldownShade() {
        cooldownShade.setVisible(plantName != null && cooldownFraction > 0f);
        float shadeHeight = PACKET_HEIGHT * cooldownFraction;
        cooldownShade.setSize(PACKET_WIDTH, shadeHeight);
        cooldownShade.setPosition(0f, 0f);
    }

    private static Texture whitePixel() {
        if (whitePixelTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            whitePixelTexture = new Texture(pixmap);
            pixmap.dispose();
        }
        return whitePixelTexture;
    }

    public void setInspected(boolean inspected) {
        this.inspected = inspected;
        refreshSelect();
    }

    public void onClick(Runnable onClick) {
        this.onClick = onClick;
    }

    public void onDragPlant(DragPlant dragPlant) {
        this.dragPlant = dragPlant;
    }

    public void enableExpiryTimer(Skin skin) {
        if (expiryLabel != null || plantName == null) {
            return;
        }
        expiryLabel = packetLabel(skin, "0s", 1.6f,
                PACKET_WIDTH * 0.44f, 4f, PACKET_WIDTH * 0.52f, 18f);
        addActor(expiryLabel);
    }

    public void setExpirySeconds(float seconds) {
        if (expiryLabel == null) {
            return;
        }
        int rounded = Math.max(0, Math.round(seconds));
        expiryLabel.setText(rounded + "s");
        if (seconds <= 5f) {
            expiryLabel.setColor(Color.SCARLET);
        } else {
            expiryLabel.setColor(Color.WHITE);
        }
    }

    private static Label packetLabel(
            Skin skin, String text, float scale,
            float x, float y, float width, float height) {
        BitmapFont font = SkinFonts.outlined(SkinFonts.getScaled(skin, "secondary", scale));
        Label label = new Label(text, new Label.LabelStyle(font, Color.WHITE));
        label.setAlignment(Align.right);
        label.setSize(width, height);
        label.setPosition(x, y);
        label.setTouchable(Touchable.disabled);
        return label;
    }

    public String plantName() {
        return plantName;
    }

    /**
     * Use a spritesheet frame (or any region) when no {@code IMAGE_UI_PACKETS_*} portrait exists.
     */
    public void setPortraitOverride(TextureRegion region) {
        setPortraitOverride(region, 1f, 0f, 0f);
    }

    /**
     * @param scaleMul extra multiplier on top of the fit-to-packet bounds (e.g. Cat-tail sheet art)
     */
    public void setPortraitOverride(TextureRegion region, float scaleMul) {
        setPortraitOverride(region, scaleMul, 0f, 0f);
    }

    /**
     * @param offsetX extra X after default left inset (positive = right)
     * @param offsetY extra Y after vertical centering (positive = up)
     */
    public void setPortraitOverride(TextureRegion region, float scaleMul, float offsetX, float offsetY) {
        if (portrait == null || region == null) {
            return;
        }
        float maxH = PACKET_HEIGHT * 0.82f;
        float maxW = PACKET_WIDTH * 0.52f;
        float fit = Math.min(maxW / Math.max(1, region.getRegionWidth()),
            maxH / Math.max(1, region.getRegionHeight()));
        float scale = fit * Math.max(0.1f, scaleMul);
        float pw = region.getRegionWidth() * scale;
        float ph = region.getRegionHeight() * scale;
        portrait.setDrawable(new TextureRegionDrawable(region));
        portrait.setSize(pw, ph);
        portrait.setPosition(4f + offsetX, Math.max(2f, (PACKET_HEIGHT - ph) * 0.5f) + offsetY);
        portraitOverride = true;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        bind(chrome, chromeId);
        if (portrait != null && !portraitOverride) {
            bind(portrait, SeedPacketIds.portraitId(plantName));
        }
        bind(select, SeedPacketIds.SELECT);
        if (lock != null && lock.getDrawable() == null) {
            TextureRegion region = textures.region(SeedPacketIds.LOCK);
            if (region != null) {
                lock.setDrawable(new TextureRegionDrawable(region));
                sizeLock(lock, region.getRegionWidth(), region.getRegionHeight());
            }
        }
    }

    private void refreshSelect() {
        select.setVisible((hovered || dragging || inspected) && plantName != null);
    }

    @Override
    public float getPrefWidth() {
        return PACKET_WIDTH;
    }

    @Override
    public float getPrefHeight() {
        return PACKET_HEIGHT;
    }

    private void bind(Image image, String id) {
        if (image.getDrawable() != null || id == null) {
            return;
        }
        TextureRegion region = textures.region(id);
        if (region != null) {
            image.setDrawable(new TextureRegionDrawable(region));
            if (image == portrait) {
                image.setSize(region.getRegionWidth(), region.getRegionHeight());
                image.setPosition(4f, Math.max(2f, (PACKET_HEIGHT - region.getRegionHeight()) * 0.5f));
            }
        }
    }

    private static Image lockImage(TextureBank textures, Skin skin) {
        Image image = image(textures, SeedPacketIds.LOCK);
        if (image.getDrawable() == null) {
            Drawable fallback = UiDrawables.tryNamed(skin, "image_ui_lock_small");
            if (fallback != null) {
                image.setDrawable(fallback);
            }
        }
        float w = image.getPrefWidth() > 0 ? image.getPrefWidth() : 33f;
        float h = image.getPrefHeight() > 0 ? image.getPrefHeight() : 43f;
        sizeLock(image, w, h);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private static void sizeLock(Image lock, float nativeW, float nativeH) {
        float h = Math.min(PACKET_HEIGHT - 4f, nativeH) * 0.5f;
        float w = nativeW * (h / nativeH);
        lock.setSize(w, h);
        lock.setPosition(PACKET_WIDTH - w - 4f, 4f);
    }

    private static Image image(TextureBank textures, String id) {
        TextureRegion region = id == null ? null : textures.region(id);
        Image image = region == null ? new Image() : new Image(new TextureRegionDrawable(region));
        image.setTouchable(Touchable.disabled);
        return image;
    }
}
