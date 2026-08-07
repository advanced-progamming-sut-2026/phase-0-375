package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import view.gui.PvzGdxGame;
import view.gui.ui.ToastBanner;

/**
 * Scene2D-only menu base (no world / PAM). Clamps delta and hosts a toast layer.
 */
public abstract class AbstractMenuScreen implements Screen {
    protected static final float UI_WIDTH = 1920f;
    protected static final float UI_HEIGHT = 1080f;
    private static final float MAX_DELTA = 1f / 30f;

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

    @Override
    public void show() {
        stage.clear();
        toast.clearMessage();
        buildUi();
        stage.addActor(toast); // keep toast above screen chrome
        Gdx.input.setInputProcessor(stage);
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
        toast.show(message, error);
    }
}
