package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.BorderedTable;
import view.gui.assets.UiRegions;
import view.gui.audio.GameAudio;
import view.gui.screen.AbstractMenuScreen;

/**
 * Season-map confirm: ask before entering a level. Play at the bottom, X on top of the card.
 */
public final class LevelEnterOverlay {
    private static final float FADE_IN = 0.18f;
    private static final float FADE_OUT = 0.15f;
    public static float CLOSE_SIZE = 56f;
    /** Offset from the card’s top-right corner. +X = right, +Y = up. */
    public static float CLOSE_SHIFT_X = 0f;
    public static float CLOSE_SHIFT_Y = 0f;
    private static final String CLOSE_UP = "IMAGE_UI_GENERIC_CLOSE_CIRCLE";
    private static final String CLOSE_DOWN = "IMAGE_UI_GENERIC_CLOSE_CIRCLE_DOWN";
    private static Texture pixel;

    private LevelEnterOverlay() {}

    public static Table show(Stage stage, Skin skin, TextureBank textures,
                             String seasonName, int levelId, Runnable onPlay) {
        if (textures != null) {
            textures.loadSync(UiRegions.ATLAS_UI_ALWAYS_LOADED);
            textures.loadSync("ATLASIMAGE_ATLAS_UI_ALWAYSLOADED_768_01");
        }
        GameAudio.get().playOverlayOpen();
        Table overlay = dimOverlay();
        Runnable closer = () -> dismiss(overlay, null);
        overlay.setUserObject(closer);
        BorderedTable card = enterCard(skin, overlay, seasonName, levelId, onPlay);
        Table closeLayer = closeLayer(skin, textures, closer);
        Stack root = new Stack();
        root.add(card);
        root.add(closeLayer);
        overlay.add(root).width(560f).pad(24f);
        overlay.getColor().a = 0f;
        overlay.addAction(Actions.fadeIn(FADE_IN));
        stage.addActor(overlay);
        return overlay;
    }

    private static Table dimOverlay() {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setName(AbstractMenuScreen.OVERLAY_NAME);
        overlay.setBackground(new TextureRegionDrawable(whitePixel())
                .tint(new Color(0f, 0f, 0f, 0.55f)));
        overlay.setTouchable(Touchable.enabled);
        return overlay;
    }

    private static BorderedTable enterCard(Skin skin, Table overlay, String seasonName,
                                           int levelId, Runnable onPlay) {
        BorderedTable card = new BorderedTable();
        card.pad(28f, 32f, 24f, 32f);
        String season = seasonName != null && !seasonName.isBlank() ? seasonName : "Season";
        Label title = new Label(season + " — Level " + levelId, skin, "big");
        title.setColor(Color.BLACK);
        title.setAlignment(Align.center);
        title.setWrap(true);
        card.add(title).growX().center().padTop(8f).padBottom(12f).padLeft(48f).padRight(48f).row();
        Label body = new Label("Do you want to enter this level?", skin, "medium");
        body.setColor(Color.BLACK);
        body.setWrap(true);
        body.setAlignment(Align.center);
        card.add(body).width(420f).padBottom(28f).row();
        TextButton play = new TextButton("Play", skin, "purple");
        SkinFonts.scaleButton(play, skin, "purple", 1.25f);
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dismiss(overlay, onPlay);
            }
        });
        card.add(play).width(220f).height(64f).padBottom(4f);
        return card;
    }

    private static Table closeLayer(Skin skin, TextureBank textures, Runnable closer) {
        Table closeLayer = new Table();
        closeLayer.setFillParent(true);
        closeLayer.setTouchable(Touchable.childrenOnly);
        closeLayer.top().right();
        TextureRegion up = textures != null ? textures.region(CLOSE_UP) : null;
        TextureRegion down = textures != null ? textures.region(CLOSE_DOWN) : null;
        Actor closeBtn;
        if (up != null) {
            closeBtn = new AtlasImageButton(up, down, CLOSE_SIZE, closer);
        } else {
            TextButton fallback = new TextButton("X", skin, "brown");
            fallback.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    closer.run();
                }
            });
            closeBtn = fallback;
        }
        closeLayer.add(closeBtn).size(CLOSE_SIZE)
                .padRight(-CLOSE_SHIFT_X)
                .padTop(-CLOSE_SHIFT_Y);
        return closeLayer;
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
                })));
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
}
