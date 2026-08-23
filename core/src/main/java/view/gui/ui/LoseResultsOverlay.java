package view.gui.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import pvz.libpvz.textures.TextureBank;

/**
 * Lose chrome on a world-drawn black fade: brains text scale-up, brain art, RETRY / EXIT.
 */
public final class LoseResultsOverlay extends Table {
    public static final String ATLAS_GROUP = "UI_GameOver";
    public static final String ATLAS_PAGE = "ATLASIMAGE_ATLAS_UI_GAMEOVER_768_00";
    public static final String BRAIN_ID = "IMAGE_UI_GAMEOVER_FAIL_SCREEN_BRAIN_ONLY";

    private static final String MESSAGE = "THE ZOMBIES\nATE YOUR\nBRAINS!";
    /** Baked into the FreeType glyphs — do not tint the Label or the outline vanishes. */
    private static final Color FILL = new Color(0.42f, 0.95f, 0.22f, 1f);
    private static final Color OUTLINE = new Color(1f, 1f, 1f, 1f);
    private static final float BTN_W = 240f;
    private static final float BTN_H = 64f;
    private static final float BRAIN_W = 340f;
    /**
     * Glyphs are rasterized huge; animation only ever scales ≤1 so Linear
     * downsampling stays sharp (upscaling a small atlas is what looked pixelated).
     */
    private static final int TITLE_SIZE = 120;
    private static final float START_SCALE = 0.18f;
    private static final float PEAK_SCALE = 0.58f;
    private static final float GROW_SEC = 0.7f;
    private static final float BRAIN_FADE_SEC = 0.4f;
    private static final int FONT_GEN = 3;

    private static BitmapFont titleFont;
    private static int builtGen;

    private final Container<Label> textWrap;
    private final Image brain;
    private final Table buttons;
    private boolean started;

    public LoseResultsOverlay(Skin skin, TextureBank textures, Runnable onRetry, Runnable onExit) {
        setFillParent(true);
        setTouchable(Touchable.childrenOnly);
        center().padTop(24f);

        ensureAtlas(textures);

        // White Label tint: fill+outline are already in the glyph colors.
        Label.LabelStyle style = new Label.LabelStyle(titleFont(), Color.WHITE);
        Label label = new Label(MESSAGE, style);
        label.setAlignment(Align.center);
        textWrap = new Container<>(label);
        textWrap.setTransform(true);
        textWrap.setScale(START_SCALE);
        textWrap.getColor().a = 0f;
        float textSlotH = TITLE_SIZE * PEAK_SCALE * 3.6f;

        TextureRegion brainRegion = textures == null ? null : textures.region(BRAIN_ID);
        brain = brainRegion == null ? new Image() : new Image(new TextureRegionDrawable(brainRegion));
        float brainH = brainRegion == null || brainRegion.getRegionWidth() <= 0
            ? BRAIN_W * 0.65f
            : BRAIN_W * (brainRegion.getRegionHeight() / (float) brainRegion.getRegionWidth());
        brain.setSize(BRAIN_W, brainH);
        brain.getColor().a = 0f;

        TextButton retry = new TextButton("RETRY", skin, "purple");
        TextButton exit = new TextButton("EXIT TO MAP", skin, "brown");
        retry.addListener(change(onRetry));
        exit.addListener(change(onExit));
        buttons = new Table();
        buttons.add(retry).width(BTN_W).height(BTN_H).padRight(20f);
        buttons.add(exit).width(BTN_W).height(BTN_H);
        buttons.getColor().a = 0f;
        buttons.setTouchable(Touchable.disabled);

        Table stack = new Table();
        stack.add(textWrap).height(textSlotH).padBottom(16f).row();
        stack.add(brain).size(BRAIN_W, brainH).padBottom(28f).row();
        stack.add(buttons);
        add(stack);
    }

    /** Call once the world black fade has finished. */
    public void play() {
        if (started) {
            return;
        }
        started = true;
        textWrap.pack();
        textWrap.setOrigin(textWrap.getWidth() * 0.5f, textWrap.getHeight() * 0.5f);
        textWrap.setScale(START_SCALE);
        textWrap.getColor().a = 1f;
        textWrap.clearActions();
        // Brain fades in while the title is still growing so the screen isn't text-only.
        brain.clearActions();
        brain.addAction(Actions.delay(0.35f, Actions.fadeIn(BRAIN_FADE_SEC)));
        textWrap.addAction(Actions.sequence(
            Actions.scaleTo(PEAK_SCALE, PEAK_SCALE, GROW_SEC, Interpolation.fade),
            Actions.run(this::showButtons)));
    }

    private void showButtons() {
        buttons.setTouchable(Touchable.enabled);
        buttons.clearActions();
        buttons.addAction(Actions.fadeIn(0.25f));
    }

    private static BitmapFont titleFont() {
        if (titleFont != null && builtGen == FONT_GEN) {
            return titleFont;
        }
        if (titleFont != null) {
            titleFont.dispose();
            titleFont = null;
        }
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
            Gdx.files.classpath("skin/HOUSE OF TERROR.TTF"));
        FreeTypeFontGenerator.FreeTypeFontParameter p =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = TITLE_SIZE;
        p.color = FILL;
        p.borderWidth = 3f;
        p.borderColor = OUTLINE;
        p.borderStraight = false;
        p.spaceX = 4;
        p.spaceY = 4;
        p.minFilter = TextureFilter.Linear;
        p.magFilter = TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "!";
        titleFont = gen.generateFont(p);
        gen.dispose();
        titleFont.setUseIntegerPositions(false);
        titleFont.getData().setLineHeight(TITLE_SIZE * 1.12f);
        builtGen = FONT_GEN;
        return titleFont;
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

    private static void ensureAtlas(TextureBank textures) {
        if (textures == null) {
            return;
        }
        textures.loadSync(ATLAS_GROUP);
        textures.loadSync(ATLAS_PAGE);
    }
}
