package view.gui.screen;

import model.network.client.NetworkClient;
import model.user.User;
import view.gui.PvzGdxGame;

/**
 * @deprecated Standalone matchmaking lobby removed (Requirement R1/R2).
 * Matchmaking now happens via {@link view.gui.ui.IZombieMatchmakingOverlay} on the Travel-Log.
 */
@Deprecated
public final class MatchmakingScreen extends AbstractMenuScreen {

    public MatchmakingScreen(PvzGdxGame game, NetworkClient client, User user) {
        super(game);
    }

    @Override
    protected void buildUi() {
        game.setScreen(new QuestsScreen(game, QuestsScreen.Tab.MINI_GAMES));
    }
}
