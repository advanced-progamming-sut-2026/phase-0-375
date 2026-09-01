package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.result.CommandResult;
import view.gui.PvzGdxGame;
import view.gui.audio.GameAudio;
import view.gui.audio.GameSfx;
import view.gui.ui.ToastBanner;

/**
 * Scene2D-only menu base (no world / PAM). Clamps delta and hosts a toast layer.
 * <p>
 * Keyboard: Escape → dismiss top {@code pvz-overlay} or {@link #onBack()};
 * Enter → {@link #onConfirm()}; Left/Right → {@link #onLeft()}/{@link #onRight()}.
 */
public abstract class AbstractMenuScreen implements Screen {
    protected static final float UI_WIDTH = 1920f;
    protected static final float UI_HEIGHT = 1080f;
    private static final float MAX_DELTA = 1f / 30f;

    /** Actor name for dimmed modal overlays so Escape can dismiss them. */
    public static final String OVERLAY_NAME = "pvz-overlay";

    protected final PvzGdxGame game;
    protected final Skin skin;
    protected final Stage stage;
    protected final ToastBanner toast;

    protected AbstractMenuScreen(PvzGdxGame game) {
        this.game = game;
        this.skin = game.skin;
        this.stage = new Stage(new FitViewport(UI_WIDTH, UI_HEIGHT), game.batch);
        this.toast = new ToastBanner(skin);
        stage.addActor(toast);
    }

    protected abstract void buildUi();

    /** Escape when no overlay is open. Default: no-op. */
    protected void onBack() {}

    /** Enter / keypad-enter. Default: no-op. */
    protected void onConfirm() {}

    /** Left arrow / A. Default: no-op. */
    protected void onLeft() {}

    /** Right arrow / D. Default: no-op. */
    protected void onRight() {}

    @Override
    public void show() {
        stage.clear();
        toast.clearMessage();
        buildUi();
        stage.addActor(toast);
        installMenuKeys();
        Gdx.input.setInputProcessor(stage);
    }

    private void installMenuKeys() {
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    if (dismissTopOverlay()) {
                        return true;
                    }
                    onBack();
                    return true;
                }
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER
                        || keycode == Input.Keys.SPACE) {
                    onConfirm();
                    return true;
                }
                if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
                    onLeft();
                    return true;
                }
                if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
                    onRight();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Closes the topmost actor named {@link #OVERLAY_NAME}.
     * Prefer a {@link Runnable} in {@link Actor#getUserObject()} (fade dismiss);
     * otherwise removes the actor.
     */
    protected boolean dismissTopOverlay() {
        var actors = stage.getActors();
        for (int i = actors.size - 1; i >= 0; i--) {
            Actor actor = actors.get(i);
            if (!OVERLAY_NAME.equals(actor.getName())) {
                continue;
            }
            Object user = actor.getUserObject();
            if (user instanceof Runnable closer) {
                closer.run();
            } else {
                actor.remove();
            }
            return true;
        }
        return false;
    }

    @Override
    public void render(float delta) {
        if (delta > MAX_DELTA) {
            delta = MAX_DELTA;
        }
        Gdx.gl.glClearColor(0.12f, 0.18f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    protected void showToast(String message, boolean error) {
        if (error) {
            GameAudio.get().playSfx(GameSfx.ERROR);
        }
        toast.show(message, error);
    }

    /** Stage used by overlays (invites, matchmaking). */
    public Stage getStage() {
        return stage;
    }

    /** Purchase / upgrade feedback (092 or error) without double-playing error on toast. */
    protected void showPurchaseResult(CommandResult<?> result) {
        if (result == null) {
            return;
        }
        GameAudio.get().feedbackPurchase(result);
        String message = result.getMessage();
        if (message != null && !message.isBlank()) {
            toast.show(message, !result.isSuccess());
        }
    }
}
