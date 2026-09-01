package view.gui.screen.gameplay;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.Viewport;
import controller.result.CommandResult;
import view.gui.PvzGdxGame;
import view.gui.assets.PvzAssets;
import view.gui.lawn.ScreenShake;
import view.gui.ui.ToastBanner;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** UI toolkit handles the lawn screen already constructed. */
public final class GameplayView {
    public final PvzGdxGame game;
    public final Skin skin;
    public final PvzAssets assets;
    public final Stage uiStage;
    public final Viewport worldViewport;
    public final Viewport uiViewport;
    public final OrthographicCamera uiCamera;
    public final ScreenShake screenShake;
    public final ToastBanner toast;
    public final BiConsumer<String, Boolean> showToast;
    public final Consumer<CommandResult<?>> showPurchaseResult;

    public GameplayView(
            PvzGdxGame game,
            Skin skin,
            PvzAssets assets,
            Stage uiStage,
            Viewport worldViewport,
            Viewport uiViewport,
            OrthographicCamera uiCamera,
            ScreenShake screenShake,
            ToastBanner toast,
            BiConsumer<String, Boolean> showToast,
            Consumer<CommandResult<?>> showPurchaseResult
    ) {
        this.game = game;
        this.skin = skin;
        this.assets = assets;
        this.uiStage = uiStage;
        this.worldViewport = worldViewport;
        this.uiViewport = uiViewport;
        this.uiCamera = uiCamera;
        this.screenShake = screenShake;
        this.toast = toast;
        this.showToast = showToast;
        this.showPurchaseResult = showPurchaseResult;
    }

    public void toast(String message, boolean error) {
        showToast.accept(message, error);
    }

    public void purchase(CommandResult<?> result) {
        showPurchaseResult.accept(result);
    }
}
