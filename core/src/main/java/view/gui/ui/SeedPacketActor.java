package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
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

    private final TextureBank textures;
    private final Image chrome;
    private final Image portrait;
    private final Image lock;
    private final Image select;
    private final String plantName;
    private final String chromeId;
    private boolean inspected;
    private boolean hovered;
    private boolean dragging;
    private boolean dimmed;
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
        if (plantName == null) {
            chrome.getColor().a = EMPTY_ALPHA;
            setTouchable(Touchable.disabled);
        } else if (locked) {
            chrome.getColor().a = LOCKED_ALPHA;
        }
        addActor(chrome);

        if (plantName != null) {
            String portraitRegion = SeedPacketIds.portraitId(plantName);
            portrait = image(textures, portraitRegion);
            float ph = portrait.getPrefHeight() > 0 ? portrait.getPrefHeight() : PACKET_HEIGHT * 0.82f;
            float pw = portrait.getPrefWidth() > 0 ? portrait.getPrefWidth() : PACKET_WIDTH * 0.52f;
            portrait.setSize(pw, ph);
            portrait.setPosition(4f, Math.max(2f, (PACKET_HEIGHT - ph) * 0.5f));
            if (locked) {
                portrait.getColor().a = LOCKED_ALPHA;
            }
            addActor(portrait);

            if (showCostAndLevel) {
                BitmapFont font = SkinFonts.outlined(skin, "secondary");
                Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);
                float textW = PACKET_WIDTH * 0.52f;
                float textX = PACKET_WIDTH * 0.44f;
                addActor(packetLabel("LVL " + Math.max(1, level), style, 1.2f,
                        textX, PACKET_HEIGHT - 18f, textW, 16f));
                if (!locked) {
                    addActor(packetLabel(String.valueOf(sunCost), style, 2f,
                            textX, 4f, textW, 18f));
                }
            }
        } else {
            portrait = null;
        }

        lock = locked ? lockImage(textures, skin) : null;
        if (lock != null) {
            addActor(lock);
        }

        select = image(textures, SeedPacketIds.SELECT);
        select.setFillParent(true);
        select.setVisible(false);
        select.setTouchable(Touchable.disabled);
        addActor(select);

        addListener(new InputListener() {
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
        });
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
        BitmapFont font = SkinFonts.outlined(skin, "secondary");
        Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);
        expiryLabel = packetLabel("0s", style, 1.6f,
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

    public String plantName() {
        return plantName;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        bind(chrome, chromeId);
        if (portrait != null) {
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

    private static Label packetLabel(
            String text, Label.LabelStyle style, float scale,
            float x, float y, float width, float height) {
        SkinFonts.linear(style.font);
        Label label = new Label(text, style);
        label.setFontScale(scale);
        label.setAlignment(Align.right);
        label.setSize(width, height);
        label.setPosition(x, y);
        label.setTouchable(Touchable.disabled);
        return label;
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
