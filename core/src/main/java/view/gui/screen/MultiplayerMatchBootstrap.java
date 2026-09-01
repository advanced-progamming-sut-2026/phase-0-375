package view.gui.screen;

import model.app.App;
import model.data.minigame.MiniGameRegistry;
import model.enums.MenuType;
import model.enums.MiniGameType;
import model.game.core.GameModel;
import model.game.level.minigame.MiniGameLevel;
import model.network.client.NetworkClient;
import model.network.enums.PlayerRole;
import model.network.packet.matchmaking.MatchFoundPacket;
import model.user.User;
import view.gui.PvzGdxGame;

import java.io.IOException;
import java.util.List;

/**
 * Boots a local display {@link GameModel} for networked I, Zombie and opens
 * {@link GameplayScreen} in multiplayer mode.
 */
public final class MultiplayerMatchBootstrap {

    /** Default plant loadout for the plant-side multiplayer player. */
    public static final List<String> PLANT_SIDE_ROSTER = List.of(
            "Peashooter",
            "Sunflower",
            "Wall-nut",
            "Potato Mine",
            "Chomper"
    );

    private MultiplayerMatchBootstrap() {}

    public static void open(PvzGdxGame game, NetworkClient client, MatchFoundPacket match) {
        if (game == null || match == null) {
            return;
        }
        ensureMiniGameRegistry();
        MiniGameLevel level;
        try {
            level = MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        } catch (IOException | RuntimeException e) {
            System.err.println("[MultiplayerMatchBootstrap] Failed to build I, Zombie level: " + e.getMessage());
            return;
        }
        if (level == null) {
            return;
        }

        // Display-only model: do not run onStart() — server snapshot is authority.
        GameModel model = new GameModel(level);
        model.setSelectedPlants(List.copyOf(PLANT_SIDE_ROSTER));

        App.getInstance().setCurrentGameModel(model);
        App.getInstance().setCurrentGameLoop(null);
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);

        PlayerRole role = match.getAssignedRole() != null ? match.getAssignedRole() : PlayerRole.PLANT;
        User user = App.getInstance().getCurrentUser();
        game.setScreen(new GameplayScreen(game, client, user, match, role));
    }

    private static void ensureMiniGameRegistry() {
        try {
            MiniGameRegistry.getInstance();
        } catch (IllegalStateException e) {
            try {
                MiniGameRegistry.init("/assets/data/minigames/minigames.json");
            } catch (IOException | RuntimeException ignored) {
                // GameplayScreen will still open if createMiniGame somehow works later.
            }
        }
    }
}
