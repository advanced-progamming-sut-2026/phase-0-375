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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.ZombiePacketIds;

/**
 * I, Zombie roster packet: READY chrome, zombie portrait, sun cost, SELECT hover.
 */
public final class ZombiePacketActor extends WidgetGroup {
    public static final float PACKET_WIDTH = 72f;
    public static final float PACKET_HEIGHT = 101f;

    private final TextureBank textures;
    private final Image chrome;
    private final Image portrait;
    private final Image select;
    private final String zombieName;
    private final String portraitId;
    private boolean hovered;
    private boolean dragging;
    private boolean dimmed;
    private DragZombie dragZombie;

    public ZombiePacketActor(TextureBank textures, Skin skin, String zombieName, int sunCost) {
        this.textures = textures;
        this.zombieName = zombieName;
        this.portraitId = ZombiePacketIds.portraitId(zombieName);
        setSize(PACKET_WIDTH, PACKET_HEIGHT);
        setTouchable(Touchable.enabled);

        chrome = image(textures, ZombiePacketIds.READY);
        chrome.setFillParent(true);
        addActor(chrome);

        portrait = image(textures, portraitId);
        layoutPortrait(portrait);
        addActor(portrait);

        float textW = PACKET_WIDTH * 0.55f;
        float textX = PACKET_WIDTH * 0.40f;
        addActor(packetLabel(skin, String.valueOf(Math.max(0, sunCost)), 1.5f,
                textX, 4f, textW, 16f));

        select = image(textures, ZombiePacketIds.SELECT);
        select.setFillParent(true);
        select.setVisible(false);
        select.setTouchable(Touchable.disabled);
        addActor(select);

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1 && zombieName != null) {
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
                if (dragZombie == null || dimmed || zombieName == null) {
                    return dragZombie != null;
                }
                dragging = true;
                refreshSelect();
                event.stop();
                dragZombie.dragStart(ZombiePacketActor.this);
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (dragging && dragZombie != null) {
                    dragZombie.drag(ZombiePacketActor.this, event.getStageX(), event.getStageY());
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (!dragging || dragZombie == null) {
                    return;
                }
                dragging = false;
                refreshSelect();
                dragZombie.dragEnd(ZombiePacketActor.this, event.getStageX(), event.getStageY());
            }
        });
    }

    /** Drag a roster zombie onto the lawn to place it. */
    public interface DragZombie {
        void dragStart(ZombiePacketActor packet);

        void drag(ZombiePacketActor packet, float stageX, float stageY);

        void dragEnd(ZombiePacketActor packet, float stageX, float stageY);
    }

    public void setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
        getColor().a = dimmed ? 0.55f : 1f;
    }

    public void onDragZombie(DragZombie dragZombie) {
        this.dragZombie = dragZombie;
    }

    public String zombieName() {
        return zombieName;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        bind(chrome, ZombiePacketIds.READY);
        if (portrait != null) {
            bindPortrait();
        }
        bind(select, ZombiePacketIds.SELECT);
    }

    private void refreshSelect() {
        select.setVisible((hovered || dragging) && zombieName != null);
    }

    @Override
    public float getPrefWidth() {
        return PACKET_WIDTH;
    }

    @Override
    public float getPrefHeight() {
        return PACKET_HEIGHT;
    }

    private void bindPortrait() {
        if (portrait.getDrawable() != null || portraitId == null) {
            return;
        }
        TextureRegion region = textures.region(portraitId);
        if (region != null) {
            portrait.setDrawable(new TextureRegionDrawable(region));
            layoutPortrait(portrait, region.getRegionWidth(), region.getRegionHeight());
        }
    }

    private void bind(Image image, String id) {
        if (image.getDrawable() != null || id == null) {
            return;
        }
        TextureRegion region = textures.region(id);
        if (region != null) {
            image.setDrawable(new TextureRegionDrawable(region));
        }
    }

    private static void layoutPortrait(Image portrait) {
        float ph = portrait.getPrefHeight() > 0 ? portrait.getPrefHeight() : PACKET_HEIGHT * 0.65f;
        float pw = portrait.getPrefWidth() > 0 ? portrait.getPrefWidth() : PACKET_WIDTH * 0.55f;
        layoutPortrait(portrait, pw, ph);
    }

    /** Centers the portrait in the READY window, leaving room for the cost. */
    private static void layoutPortrait(Image portrait, float nativeW, float nativeH) {
        float costReserve = PACKET_HEIGHT * 0.22f;
        float sidePad = PACKET_WIDTH * 0.08f;
        float maxW = PACKET_WIDTH - sidePad * 2f;
        float maxH = PACKET_HEIGHT - costReserve - sidePad;
        float scale = Math.min(maxW / nativeW, maxH / nativeH);
        float w = nativeW * scale;
        float h = nativeH * scale;
        portrait.setSize(w, h);
        portrait.setPosition(
                (PACKET_WIDTH - w) * 0.5f,
                costReserve + (maxH - h) * 0.5f);
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

    private static Image image(TextureBank textures, String id) {
        TextureRegion region = id == null ? null : textures.region(id);
        Image image = region == null ? new Image() : new Image(new TextureRegionDrawable(region));
        image.setTouchable(Touchable.disabled);
        return image;
    }
}
