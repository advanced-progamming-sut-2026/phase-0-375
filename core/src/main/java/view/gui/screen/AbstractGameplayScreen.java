package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import model.app.App;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import view.gui.PvzGdxGame;
import view.gui.assets.PamPlantClipDurations;
import view.gui.assets.PamPlantProjectileOrigins;
import view.gui.assets.PvzAssets;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.ScreenShake;
import view.gui.ui.ToastBanner;

/**
 * Dual-viewport base for lawn gameplay.
 * World virtual size is LEFT+center only; {@link ExtendViewport} keeps that
 * fully visible and snaps top/bottom on wide screens, letting TEXTURE_RIGHT
 * spill past the right edge instead of forcing it into frame.
 */
public abstract class AbstractGameplayScreen implements Screen {
    /** Same virtual size as the FrontLawn camera base (left + center). */
    protected static final float UI_WIDTH = LawnLayout.WORLD_WIDTH;
    protected static final float UI_HEIGHT = LawnLayout.WORLD_HEIGHT;
    private static final float MAX_DELTA = 1f / 30f;

    protected final PvzGdxGame game;
    protected final Skin skin;
    protected final PvzAssets assets;

    protected final OrthographicCamera worldCamera;
    protected final Viewport worldViewport;
    protected final OrthographicCamera uiCamera;
    protected final Viewport uiViewport;
    protected final Stage uiStage;
    protected final ToastBanner toast;
    protected final ScreenShake screenShake = new ScreenShake();
    private final Matrix4 identityTransform = new Matrix4();

    private final Vector3 unprojectTmp = new Vector3();
    private InputProcessor worldInput;

    protected AbstractGameplayScreen(PvzGdxGame game) {
        this.game = game;
        this.skin = game.skin;
        this.assets = game.ensureAssets();

        worldCamera = new OrthographicCamera();
        worldViewport = new ExtendViewport(LawnLayout.WORLD_WIDTH, LawnLayout.WORLD_HEIGHT, worldCamera);
        worldCamera.position.set(LawnLayout.WORLD_WIDTH * 0.5f, LawnLayout.WORLD_HEIGHT * 0.5f, 0f);
        worldCamera.update();

        uiCamera = new OrthographicCamera();
        uiViewport = new ExtendViewport(UI_WIDTH, UI_HEIGHT, uiCamera);
        uiCamera.position.set(UI_WIDTH * 0.5f, UI_HEIGHT * 0.5f, 0f);
        uiCamera.update();
        uiStage = new Stage(uiViewport, game.batch);
        toast = new ToastBanner(skin);
        uiStage.addActor(toast);
    }

    /** Optional lawn / world click handler (UI stage is tried first). */
    protected void setWorldInput(InputProcessor processor) {
        this.worldInput = processor;
    }

    protected InputProcessor createCellPickInput(LawnLayout layout, CellPickListener listener) {
        return createCellPickInput(layout, listener, null);
    }

    protected InputProcessor createCellPickInput(
            LawnLayout layout, CellPickListener listener, CellHoverListener hoverListener) {
        return new InputAdapter() {
            private final int[] cell = new int[2];

            private boolean updateHover(int screenX, int screenY) {
                if (hoverListener == null) {
                    return false;
                }
                worldViewport.unproject(unprojectTmp.set(screenX, screenY, 0f));
                if (!layout.worldToCell(unprojectTmp.x, unprojectTmp.y, cell)) {
                    hoverListener.onCellHover(-1, -1);
                    return false;
                }
                hoverListener.onCellHover(cell[0], cell[1]);
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                return updateHover(screenX, screenY);
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                updateHover(screenX, screenY);
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                worldViewport.unproject(unprojectTmp.set(screenX, screenY, 0f));
                if (!layout.worldToCell(unprojectTmp.x, unprojectTmp.y, cell)) {
                    return false;
                }
                return listener.onCellPicked(cell[0], cell[1]);
            }
        };
    }

    protected InputProcessor createWorldClickInput(
            LawnLayout layout, WorldClickListener listener, CellHoverListener hoverListener) {
        return new InputAdapter() {
            private final int[] cell = new int[2];

            private boolean updateHover(int screenX, int screenY) {
                if (hoverListener == null) {
                    return false;
                }
                worldViewport.unproject(unprojectTmp.set(screenX, screenY, 0f));
                if (!layout.worldToCell(unprojectTmp.x, unprojectTmp.y, cell)) {
                    hoverListener.onCellHover(-1, -1);
                    return false;
                }
                hoverListener.onCellHover(cell[0], cell[1]);
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                return updateHover(screenX, screenY);
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                updateHover(screenX, screenY);
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                worldViewport.unproject(unprojectTmp.set(screenX, screenY, 0f));
                return listener.onWorldClick(unprojectTmp.x, unprojectTmp.y);
            }
        };
    }

    @FunctionalInterface
    public interface WorldClickListener {
        /** @return true if the click was consumed */
        boolean onWorldClick(float worldX, float worldY);
    }

    @FunctionalInterface
    public interface CellPickListener {
        /** @return true if the click was consumed */
        boolean onCellPicked(int col, int row);
    }

    @FunctionalInterface
    public interface CellHoverListener {
        /** Pass {@code (-1, -1)} when the pointer leaves the lawn. */
        void onCellHover(int col, int row);
    }

    @Override
    public void show() {
        wirePlantPresentation();
        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(uiStage);
        if (worldInput != null) {
            mux.addProcessor(worldInput);
        }
        Gdx.input.setInputProcessor(mux);
    }

    /** Lets timed plant actions use real PAM clip lengths and muzzle part bounds. */
    private void wirePlantPresentation() {
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop == null || assets == null || assets.pamCatalog == null) {
            return;
        }
        loop.setPlantClipDurations(new PamPlantClipDurations(assets.pamCatalog, assets.plantSheets));
        GameModel model = App.getInstance().getCurrentGameModel();
        LawnLayout layout = (model == null)
                ? LawnLayout.frontLawnDefault()
                : new LawnLayout(model.getRowCount(), model.getColumnCount());
        loop.setPlantProjectileOrigins(new PamPlantProjectileOrigins(assets, layout));
    }

    @Override
    public void render(float delta) {
        if (delta > MAX_DELTA) {
            delta = MAX_DELTA;
        }
        updateLogic(delta);
        renderGraphics(delta);
    }

    protected abstract void updateLogic(float delta);

    protected abstract void renderWorld(float delta);

    /**
     * When true, world sim/draw receive {@code 0} delta so PAM clocks and
     * underlayers freeze; the UI stage still acts so overlays stay interactive.
     */
    protected boolean freezeWorld() {
        return false;
    }

    protected void renderGraphics(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.07f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float worldDelta = freezeWorld() ? 0f : delta;
        screenShake.update(worldDelta);
        worldViewport.apply(false);
        anchorCameraLeft(worldCamera);
        screenShake.apply(worldCamera);
        game.batch.setProjectionMatrix(worldCamera.combined);
        game.batch.begin();
        game.batch.setColor(Color.WHITE);
        game.batch.setTransformMatrix(identityTransform);
        renderWorld(worldDelta);
        game.batch.end();

        uiViewport.apply(false);
        anchorCameraLeft(uiCamera);
        screenShake.apply(uiCamera);
        uiStage.act(delta);
        uiStage.draw();
        anchorCameraLeft(worldCamera);
        anchorCameraLeft(uiCamera);
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height, false);
        uiViewport.update(width, height, false);
        anchorCameraLeft(worldCamera);
        anchorCameraLeft(uiCamera);
    }

    /**
     * Keep LEFT+center pinned: any {@link ExtendViewport} extra width shows on the
     * right (TEXTURE_RIGHT), never by shifting the house off-screen.
     */
    private static void anchorCameraLeft(OrthographicCamera camera) {
        camera.position.set(camera.viewportWidth * 0.5f, LawnLayout.WORLD_HEIGHT * 0.5f, 0f);
        camera.update();
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
        uiStage.dispose();
    }

    protected void showToast(String message, boolean error) {
        toast.show(message, error);
    }
}
