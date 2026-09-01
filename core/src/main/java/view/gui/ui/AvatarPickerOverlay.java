package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import controller.ProfileMenuController;
import controller.result.CommandResult;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.BorderedTable;
import view.gui.assets.AvatarArt;
import view.gui.audio.GameAudio;
import view.gui.screen.AbstractMenuScreen;

import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Grid picker for the 30 joust avatars.
 */
public final class AvatarPickerOverlay {
    private static final float FADE_IN = 0.11f;
    private static final float FADE_OUT = 0.07f;
    private static final Color DIM = new Color(0f, 0f, 0f, 0.55f);
    private static final float CELL = 72f;
    private static final int COLS = 6;

    private static Texture pixel;

    private AvatarPickerOverlay() {}

    public static Table create(Skin skin, TextureBank textures, int currentAvatarId,
                               BiConsumer<String, Boolean> toast, IntConsumer onSaved) {
        GameAudio.get().playOverlayOpen();
        Table overlay = dimOverlay();
        int[] selected = {AvatarArt.normalize(currentAvatarId)};
        TextureRegion selectedRing = AvatarArt.region(textures, AvatarArt.SELECTED);
        BorderedTable card = new BorderedTable();
        card.pad(24f);
        Label title = new Label("Choose avatar", skin, "big");
        title.setColor(CollectionEntryOverlay.INK);
        card.add(title).padBottom(14f).row();
        card.add(avatarGrid(textures, selectedRing, selected)).padBottom(16f).row();
        Table actions = new Table();
        TextButton save = styledButton(skin, "Save", "green", 0.9f);
        TextButton cancel = styledButton(skin, "Cancel", "brown", 0.9f);
        actions.add(save).width(160f).height(50f).padRight(12f);
        actions.add(cancel).width(160f).height(50f);
        card.add(actions);
        Runnable closer = () -> dismiss(overlay, null);
        overlay.setUserObject(closer);
        bindPickerActions(save, cancel, closer, overlay, selected, toast, onSaved);
        overlay.add(card).width(560f).pad(40f);
        fadeIn(overlay);
        UiMotion.fadeSlideIn(card, 0.3f);
        return overlay;
    }

    private static Table dimOverlay() {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setName(AbstractMenuScreen.OVERLAY_NAME);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(DIM));
        overlay.setTouchable(Touchable.enabled);
        return overlay;
    }

    private static Table avatarGrid(TextureBank textures, TextureRegion selectedRing, int[] selected) {
        Table grid = new Table();
        grid.defaults().pad(4f);
        java.util.List<StackSlot> slots = new java.util.ArrayList<>();
        for (int id = AvatarArt.MIN_ID; id <= AvatarArt.MAX_ID; id++) {
            final int avatarId = id;
            StackSlot slot = new StackSlot(textures, avatarId, selectedRing);
            slot.refresh(selected[0] == avatarId);
            slot.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    GameAudio.get().playNavClick();
                    selected[0] = avatarId;
                    for (StackSlot entry : slots) {
                        entry.refresh(entry.avatarId == selected[0]);
                    }
                }
            });
            slots.add(slot);
            grid.add(slot).size(CELL, CELL);
            if ((id - AvatarArt.MIN_ID + 1) % COLS == 0) {
                grid.row();
            }
        }
        return grid;
    }

    private static void bindPickerActions(TextButton save, TextButton cancel, Runnable closer,
                                          Table overlay, int[] selected,
                                          BiConsumer<String, Boolean> toast, IntConsumer onSaved) {
        cancel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closer.run();
            }
        });
        save.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> result =
                        ProfileMenuController.getInstance().changeAvatar(selected[0]);
                toast.accept(result.getMessage(), !result.isSuccess());
                if (result.isSuccess()) {
                    dismiss(overlay, () -> onSaved.accept(selected[0]));
                }
            }
        });
    }

    private static TextButton styledButton(Skin skin, String text, String style, float scale) {
        TextButton button = new TextButton(text, skin, style);
        SkinFonts.scaleButton(button, skin, style, scale);
        UiMotion.bindPressScale(button);
        return button;
    }

    private static void fadeIn(Table overlay) {
        overlay.getColor().a = 0f;
        overlay.addAction(Actions.fadeIn(FADE_IN));
    }

    private static void dismiss(Table overlay, Runnable after) {
        overlay.setTouchable(Touchable.disabled);
        overlay.clearActions();
        overlay.addAction(Actions.sequence(
                Actions.fadeOut(FADE_OUT),
                Actions.run(() -> {
                    overlay.remove();
                    if (after != null) {
                        after.run();
                    }
                })
        ));
    }

    private static Texture whitePixel() {
        if (pixel == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
            pixmap.dispose();
        }
        return pixel;
    }

    /** One grid cell: portrait + optional selection ring. */
    private static final class StackSlot extends Stack {
        private final int avatarId;
        private final Image ring;

        private StackSlot(TextureBank textures, int avatarId, TextureRegion selectedRing) {
            this.avatarId = avatarId;
            setTouchable(Touchable.enabled);

            AvatarPortrait portrait = new AvatarPortrait(textures, avatarId, CELL, false);
            add(portrait);

            ring = new Image();
            ring.setScaling(Scaling.fit);
            ring.setTouchable(Touchable.disabled);
            if (selectedRing != null) {
                ring.setDrawable(new TextureRegionDrawable(selectedRing));
            }
            ring.setVisible(false);
            add(ring);
        }

        private void refresh(boolean selected) {
            ring.setVisible(selected);
        }
    }
}
