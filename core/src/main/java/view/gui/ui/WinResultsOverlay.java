package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import pvz.libpvz.textures.TextureBank;

/**
 * Win chrome after black fade: YOU WON scale-up + CONTINUE / EXIT.
 */
public final class WinResultsOverlay extends Table {
    public static final String ATLAS_GROUP = "UI_Joust_MatchResult";
    public static final String ATLAS_PAGE = "ATLASIMAGE_ATLAS_UI_JOUST_MATCHRESULT_768_00";
    public static final String YOU_WON_ID =
        "IMAGE_UI_JOUST_MATCH_RESULTS_YOU_WON_TEXT_YOU_WON_TEXT_715X216";

    private static final float BTN_W = 240f;
    private static final float BTN_H = 64f;
    private static final float YOU_WON_W = 520f;
    private static final float START_SCALE = 0.2f;
    private static final float GROW_SEC = 0.7f;

    private final Container<Image> youWonWrap;
    private final Table buttons;
    private boolean started;

    public WinResultsOverlay(Skin skin, TextureBank textures, Runnable onContinue, Runnable onExit) {
        setFillParent(true);
        setTouchable(Touchable.childrenOnly);
        top().padTop(48f);

        ensureAtlas(textures);

        TextureRegion region = textures == null ? null : textures.region(YOU_WON_ID);
        Image youWon = region == null ? new Image() : new Image(new TextureRegionDrawable(region));
        float h = region == null || region.getRegionWidth() <= 0
            ? YOU_WON_W * 0.3f
            : YOU_WON_W * (region.getRegionHeight() / (float) region.getRegionWidth());
        youWon.setSize(YOU_WON_W, h);
        youWonWrap = new Container<>(youWon);
        youWonWrap.size(YOU_WON_W, h);
        youWonWrap.setTransform(true);
        youWonWrap.setScale(START_SCALE);
        youWonWrap.getColor().a = 0f;

        TextButton cont = new TextButton("CONTINUE", skin, "purple");
        TextButton exit = new TextButton("EXIT TO MAP", skin, "brown");
        cont.addListener(change(onContinue));
        exit.addListener(change(onExit));
        buttons = new Table();
        buttons.add(cont).width(BTN_W).height(BTN_H).padRight(20f);
        buttons.add(exit).width(BTN_W).height(BTN_H);
        buttons.getColor().a = 0f;
        buttons.setTouchable(Touchable.disabled);

        add(youWonWrap).size(YOU_WON_W, h).expandX().center().padBottom(36f).row();
        add(buttons).expand().bottom().padBottom(72f);
    }

    public void play() {
        if (started) {
            return;
        }
        started = true;
        youWonWrap.pack();
        youWonWrap.setOrigin(youWonWrap.getWidth() * 0.5f, youWonWrap.getHeight() * 0.5f);
        youWonWrap.setScale(START_SCALE);
        youWonWrap.getColor().a = 1f;
        youWonWrap.clearActions();
        youWonWrap.addAction(Actions.sequence(
            Actions.scaleTo(1f, 1f, GROW_SEC, Interpolation.fade),
            Actions.run(this::showButtons)));
    }

    private void showButtons() {
        buttons.setTouchable(Touchable.enabled);
        buttons.clearActions();
        buttons.addAction(Actions.fadeIn(0.25f));
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
