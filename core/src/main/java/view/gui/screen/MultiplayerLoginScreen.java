package view.gui.screen;

import model.app.App;
import model.network.client.NetworkClient;
import view.gui.PvzGdxGame;

/**
 * @deprecated Standalone multiplayer login has been removed (Requirement R1).
 * Server authentication is now handled directly upon client login in {@link LoginScreen}.
 */
@Deprecated
public final class MultiplayerLoginScreen extends AbstractMenuScreen {

    public MultiplayerLoginScreen(PvzGdxGame game) {
        super(game);
    }

    public MultiplayerLoginScreen(PvzGdxGame game, NetworkClient client) {
        super(game);
    }

    @Override
    protected void buildUi() {
        if (App.getInstance().getCurrentUser() != null) {
            game.setScreen(new MainHubScreen(game));
        } else {
            game.setScreen(new LoginScreen(game));
        }
    }
}
