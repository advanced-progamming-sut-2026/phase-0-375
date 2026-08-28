package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import controller.SettingsMenuController;
import model.app.App;
import model.game.level.LevelConfig;
import model.user.User;
import pvz.libpvz.textures.TextureBank;
import view.gui.audio.GameAudio;

import java.util.List;

/**
 * In-game pause overlay: objectives card, music/SFX sliders, resume/restart/exit.
 * Outer frame matches {@link LevelObjectivesOverlay}; toppers come from TextureBank.
 */
public final class PauseMenuOverlay {

    public static final String ATLAS_GROUP = "UI_PauseMenu_768";
    public static final String ATLAS_PAGE = "ATLASIMAGE_ATLAS_UI_PAUSEMENU_768_00";
    public static final String WINDOWTOPPER_ID = "IMAGE_UI_PAUSEMENU_WINDOWTOPPER";
    public static final String SUNFLOWER_TOPPER_ID = "IMAGE_UI_PAUSEMENU_SUNFLOWER_TOPPER";
    /** PvzSkin TenPatch {@code image_ui_pausemenu_blank_card_10}. */
    public static final String BLANK_CARD = "image_ui_pausemenu_blank_card";

    /** Native 768 sizes. */
    public static final float WINDOWTOPPER_W = 551f;
    public static final float WINDOWTOPPER_H = 108f;
    public static final float SUNFLOWER_W = 124f;
    public static final float SUNFLOWER_H = 103f;
    /** Clockwise lean ("tilted to the right") in Scene2D degrees. */
    public static final float SUNFLOWER_TILT_DEG = -15f;

    private static final float PANEL_MIN_W = 792f; // 720 * 1.1
    private static final float CARD_MIN_H = 160f;
    private static final float TOPPER_SCALE = 1.15f;
    private static final float BTN_W = 240f;
    private static final float BTN_H = 64f;
    private static final Color OBJECTIVE_COLOR = new Color(0.22f, 0.12f, 0.04f, 1f);

    private static Texture pixel;

    private PauseMenuOverlay() {}

    public static Table create(
            Skin skin,
            TextureBank textures,
            LevelConfig config,
            Runnable onResume,
            Runnable onRestart,
            Runnable onExit) {
        ensureAtlas(textures);

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0f, 0f, 0f, 0.55f)));

        Table outer = new Table();
        Drawable outerBg = UiDrawables.tenPatch(skin, "image_ui_if_bundle_reward1_bg");
        if (outerBg != null) {
            outer.setBackground(outerBg);
        } else {
            outer.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0.55f, 0.35f, 0.12f, 1f)));
        }

        Table card = new Table();
        Drawable cardBg = UiDrawables.tenPatch(skin, BLANK_CARD);
        if (cardBg != null) {
            card.setBackground(cardBg);
        } else {
            card.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0.96f, 0.92f, 0.78f, 1f)));
        }
        card.pad(28f, 36f, 32f, 36f);
        card.defaults().left().growX();
        List<String> objectives = LevelObjectivesOverlay.objectivesFor(
                App.getInstance().getCurrentGameModel(), config);
        for (String text : objectives) {
            CheckBox cb = new CheckBox(" " + text, skin);
            cb.setChecked(false);
            cb.setTouchable(Touchable.disabled);
            SkinFonts.scaleCheckBox(cb, skin, "default", 1.15f);
            cb.getLabel().setColor(OBJECTIVE_COLOR);
            card.add(cb).padBottom(10f).row();
        }

        outer.add(card).pad(48f, 52f, 20f, 52f).minWidth(PANEL_MIN_W - 80f).minHeight(CARD_MIN_H).growX().row();
        outer.add(volumeSection(skin)).pad(12f, 64f, 40f, 64f).growX().row();

        TextButton exitBtn = new TextButton("SAVE AND EXIT", skin, "brown");
        TextButton restartBtn = new TextButton("RESTART", skin, "brown");
        TextButton resumeBtn = new TextButton("RESUME", skin, "purple");
        exitBtn.addListener(change(onExit));
        restartBtn.addListener(change(onRestart));
        resumeBtn.addListener(change(onResume));

        Table buttons = new Table();
        buttons.add(exitBtn).width(BTN_W).height(BTN_H).padRight(16f);
        buttons.add(restartBtn).width(BTN_W).height(BTN_H).padRight(16f);
        buttons.add(resumeBtn).width(BTN_W).height(BTN_H);

        float topperW = WINDOWTOPPER_W * TOPPER_SCALE;
        float topperH = WINDOWTOPPER_H * TOPPER_SCALE;

        // Panel + buttons first; topper stacked last so it draws over the brown frame.
        Table frame = new Table();
        frame.add(outer).minWidth(PANEL_MIN_W).row();
        frame.add(buttons).padTop(-BTN_H * 0.5f);

        Stack stack = new Stack();
        stack.add(frame);

        Table topperLayer = new Table();
        topperLayer.top();
        topperLayer.add(topperStack(textures, topperW, topperH))
            .size(topperW, topperH)
            .padTop(-topperH * 0.55f)
            .expandX()
            .top();
        stack.add(topperLayer);

        overlay.add(stack).pad(24f);
        return overlay;
    }

    private static Actor topperStack(TextureBank textures, float topperW, float topperH) {
        float sunW = SUNFLOWER_W * TOPPER_SCALE;
        float sunH = SUNFLOWER_H * TOPPER_SCALE;

        Stack stack = new Stack();
        stack.setSize(topperW, topperH);

        Image sunflower = regionImage(textures, SUNFLOWER_TOPPER_ID, sunW, sunH);
        if (sunflower != null) {
            Table sunPad = new Table();
            sunflower.setOrigin(Align.center);
            sunflower.setRotation(SUNFLOWER_TILT_DEG);
            sunPad.add(sunflower).size(sunW, sunH).padBottom(60f * TOPPER_SCALE);
            stack.add(sunPad);
        }

        Image window = regionImage(textures, WINDOWTOPPER_ID, topperW, topperH);
        if (window != null) {
            Table winPad = new Table();
            winPad.add(window).size(topperW, topperH).expand().bottom();
            stack.add(winPad);
        }
        return stack;
    }

    private static Table volumeSection(Skin skin) {
        SettingsMenuController settings = SettingsMenuController.getInstance();
        User user = App.getInstance().getCurrentUser();
        float music = user == null ? GameAudio.get().getMusicVolume() : user.getMusicVolume();
        float sfx = user == null ? GameAudio.get().getSfxVolume() : user.getSfxVolume();

        Slider musicSlider = new Slider(0f, 100f, 1f, false, skin, "default-horizontal");
        musicSlider.setValue(music * 100f);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameAudio.get().setMusicVolume(musicSlider.getValue() / 100f);
            }
        });
        musicSlider.addListener(persistVolume(settings, true, musicSlider));

        Slider sfxSlider = new Slider(0f, 100f, 1f, false, skin, "default-horizontal");
        sfxSlider.setValue(sfx * 100f);
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameAudio.get().setSfxVolume(sfxSlider.getValue() / 100f);
            }
        });
        sfxSlider.addListener(persistVolume(settings, false, sfxSlider));

        Table section = new Table();
        section.add(volumeRow(skin, "Music", musicSlider)).growX().padBottom(14f).row();
        section.add(volumeRow(skin, "Sound FX", sfxSlider)).growX();
        return section;
    }

    private static Table volumeRow(Skin skin, String title, Slider slider) {
        Table row = new Table();
        Label label = new Label(title, skin, "medium");
        label.setColor(Color.WHITE);
        row.add(label).width(130f).left();
        row.add(slider).growX().height(40f);
        return row;
    }

    private static ClickListener persistVolume(
            SettingsMenuController settings, boolean music, Slider slider) {
        return new ClickListener() {
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                super.touchUp(event, x, y, pointer, button);
                float volume = slider.getValue() / 100f;
                if (music) {
                    settings.setMusicVolume(volume);
                } else {
                    settings.setSfxVolume(volume);
                }
            }
        };
    }

    private static ChangeListener change(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (action != null) {
                    action.run();
                }
            }
        };
    }

    private static Image regionImage(TextureBank textures, String id, float w, float h) {
        TextureRegion region = textures == null ? null : textures.region(id);
        if (region == null) {
            return null;
        }
        Image image = new Image(new TextureRegionDrawable(region));
        image.setSize(w, h);
        return image;
    }

    private static void ensureAtlas(TextureBank textures) {
        if (textures == null) {
            return;
        }
        textures.loadSync(ATLAS_GROUP);
        textures.loadSync(ATLAS_PAGE);
    }

    private static Texture whitePixel() {
        if (pixel == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            pixel = new Texture(pm);
            pm.dispose();
        }
        return pixel;
    }
}
