package view.gui.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import view.gui.assets.PvzAssets;

/**
 * Fun hub intro: portrait flies in, spinning and scaling to a final pose.
 * Tune start/end position and scale via the public static fields below.
 */
public final class ZarrabiIntro implements Disposable {
    private static final String RELATIVE =
            "Exports/AI generated/Gemini_Generated_Image_norcf5norcf5norc-removebg-preview.png";

    // ── Tunables (UI space: 1920×1080, position = bottom-left of the portrait) ──
    /** Width of the portrait when {@link #END_SCALE} is {@code 1}. Height follows aspect. */
    public static float BASE_WIDTH = 260f;
    /** Starting bottom-left X (negative = off the left edge). */
    public static float START_X = -100f;
    /** Starting bottom-left Y. */
    public static float START_Y = 1700f;
    /** Final bottom-left X. */
    public static float END_X = 830f;
    /** Final bottom-left Y. */
    public static float END_Y = 380f;
    /** Scale at spawn (1 = {@link #BASE_WIDTH}). */
    public static float START_SCALE = 0.1f;
    /** Scale when the intro finishes. */
    public static float END_SCALE = 2.6f;
    /** Flight duration in seconds. */
    public static float DURATION = 2f;
    /** Full rotations during the flight (ends upright). */
    public static float SPINS = 4f;

    private Texture texture;
    private Group root;

    public Group play(float uiWidth, float uiHeight, FileHandle assetsRoot) {
        FileHandle file = resolve(assetsRoot);
        if (file == null || !file.exists()) {
            return null;
        }
        texture = new Texture(file);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        float aspect = texture.getHeight() / (float) texture.getWidth();
        float baseW = Math.max(1f, BASE_WIDTH);
        float baseH = baseW * aspect;

        Image image = new Image(new TextureRegionDrawable(texture));
        image.setSize(baseW, baseH);
        image.setPosition(0f, 0f);
        image.setTouchable(Touchable.disabled);

        root = new Group();
        root.setTransform(true);
        root.setTouchable(Touchable.disabled);
        root.setSize(baseW, baseH);
        root.setOrigin(Align.center);
        root.addActor(image);
        root.setScale(START_SCALE);
        root.setRotation(0f);
        root.setPosition(START_X, START_Y);

        root.addAction(Actions.parallel(
                Actions.moveTo(END_X, END_Y, DURATION, Interpolation.sineOut),
                Actions.scaleTo(END_SCALE, END_SCALE, DURATION, Interpolation.sineOut),
                Actions.rotateBy(-360f * SPINS, DURATION, Interpolation.sineOut)
        ));
        return root;
    }

    private static FileHandle resolve(FileHandle assetsRoot) {
        if (assetsRoot != null) {
            FileHandle fromRoot = assetsRoot.child(RELATIVE);
            if (fromRoot.exists()) {
                return fromRoot;
            }
        }
        return PvzAssets.resolveAsset(RELATIVE);
    }

    @Override
    public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        root = null;
    }
}
