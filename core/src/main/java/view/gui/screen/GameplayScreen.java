package view.gui.screen;

import model.app.App;
import model.enums.MenuType;
import model.network.client.NetworkClient;
import model.network.enums.PlayerRole;
import model.network.packet.InviteReceivedPacket;
import model.network.packet.matchmaking.MatchFoundPacket;
import model.user.User;
import view.gui.PvzGdxGame;
import view.gui.screen.gameplay.GameplayBootstrap;
import view.gui.screen.gameplay.GameplayContext;
import view.gui.screen.gameplay.GameplayView;

/**
 * In-game lawn. Chapter backgrounds use the same left/center/right camera as debug FrontLawn.
 */
public final class GameplayScreen extends AbstractGameplayScreen {
    private final GameplayContext ctx;

    public GameplayScreen(PvzGdxGame game) {
        this(game, null, null, null, null);
    }

    public GameplayScreen(
            PvzGdxGame game,
            NetworkClient networkClient,
            User user,
            MatchFoundPacket match,
            PlayerRole role
    ) {
        super(game);
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);
        ctx = new GameplayContext(view(), networkClient, user, match, role);
        GameplayBootstrap.finish(ctx, this::setWorldInput, this::createWorldClickInput);
    }

    public void openInviteOverlay(InviteReceivedPacket packet) {
        ctx.flow.openInviteOverlay(packet);
    }

    public void closeInviteOverlay() {
        ctx.flow.closeInviteOverlay();
    }

    @Override
    public void show() {
        super.show();
        ctx.flow.registerReactions();
    }

    @Override
    protected void onBack() {
        if (ctx.couchPlayMode && ctx.zombieDropMode) {
            ctx.placement.cancelZombieDrop();
        }
    }

    @Override
    protected void onConfirm() {
        if (ctx.couchPlayMode && ctx.zombieDropMode) {
            ctx.placement.confirmZombieDrop();
        }
    }

    @Override
    protected boolean freezeWorld() {
        return ctx.pauseMenuOpen || ctx.invitePauseActive;
    }

    @Override
    protected void updateLogic(float delta) {
        ctx.logic.update(delta);
    }

    @Override
    protected void renderWorld(float delta) {
        ctx.worldRenderer.render(delta);
    }

    @Override
    protected void renderGraphics(float delta) {
        super.renderGraphics(delta);
        ctx.worldRenderer.drawArmedCursors();
    }

    @Override
    public void hide() {
        ctx.lifecycle.hide();
        super.hide();
    }

    @Override
    public void dispose() {
        ctx.lifecycle.dispose();
        super.dispose();
    }

    private GameplayView view() {
        return new GameplayView(
            game, skin, assets, uiStage, worldViewport, uiViewport, uiCamera, screenShake, toast,
            this::showToast, this::showPurchaseResult);
    }
}
