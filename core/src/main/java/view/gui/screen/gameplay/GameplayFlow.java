package view.gui.screen.gameplay;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import controller.GameMenuController;
import controller.MainMenuController;
import controller.PlantSelectionMenuController;
import controller.TravelLogMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.GameState;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;
import model.game.level.special.ScoreLevel;
import model.game.save.GameSaveService;
import model.item.LootPickup;
import model.network.client.NetworkClient;
import model.network.enums.ReactionType;
import model.network.packet.InviteReceivedPacket;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.game.GameStateSnapshotPacket;
import view.gui.audio.GameAudio;
import view.gui.audio.GameSfx;
import view.gui.screen.AdventureScreen;
import view.gui.screen.ChapterLevelsScreen;
import view.gui.screen.GameplayScreen;
import view.gui.screen.LevelObjectivesScreen;
import view.gui.screen.MultiplayerMatchBootstrap;
import view.gui.screen.QuestsScreen;
import view.gui.ui.InviteReceivedOverlay;
import view.gui.ui.LoseResultsOverlay;
import view.gui.ui.MyopointResultsOverlay;
import view.gui.ui.PauseMenuOverlay;
import view.gui.ui.ReactionPresets;
import view.gui.ui.WinResultsOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pause, win/lose overlays, restart/exit, and multiplayer reactions. */
public final class GameplayFlow {
    private final GameplayContext ctx;

    public GameplayFlow(GameplayContext ctx) {
        this.ctx = ctx;
    }

    public void openPauseMenu() {
        if (ctx.pauseMenuOpen || ctx.endSequenceActive) {
            return;
        }
        GameAudio.get().playSfx(GameSfx.PAUSE);
        ctx.pauseMenuOpen = true;
        if (ctx.pauseButton != null) {
            ctx.pauseButton.setChecked(true);
        }
        pauseLoopIfRunning();
        LevelConfig config = GameplayLevelQueries.currentLevel() == null
            ? null : GameplayLevelQueries.currentLevel().getConfig();
        ctx.pauseOverlay = PauseMenuOverlay.create(
            ctx.view.skin, ctx.view.assets.textures, config,
            this::closePauseMenu, this::restartLevel, this::saveAndExit);
        ctx.view.uiStage.addActor(ctx.pauseOverlay);
        ctx.view.toast.toFront();
    }

    public void closePauseMenu() {
        if (!ctx.pauseMenuOpen) {
            return;
        }
        ctx.pauseMenuOpen = false;
        if (ctx.pauseOverlay != null) {
            ctx.pauseOverlay.remove();
            ctx.pauseOverlay = null;
        }
        if (ctx.pauseButton != null) {
            ctx.pauseButton.setChecked(false);
        }
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop != null && loop.getGameState() == GameState.PAUSED) {
            loop.resume();
        }
    }

    public void maybeStartEndSequence(GameModel model) {
        if (ctx.endSequenceActive || model == null) {
            return;
        }
        GameState state = model.getState();
        if (state == GameState.LOST) {
            startLoseSequence();
        } else if (state == GameState.WON) {
            startWinSequence();
        }
    }

    public void openInviteOverlay(InviteReceivedPacket packet) {
        if (ctx.endSequenceActive || packet == null || ctx.inviteOverlay != null) {
            return;
        }
        ctx.invitePauseActive = true;
        pauseLoopIfRunning();
        NetworkClient client = App.getInstance().getNetworkClient();
        ctx.inviteOverlay = new InviteReceivedOverlay(
            ctx.view.game, ctx.view.skin, client, packet, this::closeInviteOverlay);
        ctx.view.uiStage.addActor(ctx.inviteOverlay);
        ctx.view.toast.toFront();
    }

    public void closeInviteOverlay() {
        if (!ctx.invitePauseActive && ctx.inviteOverlay == null) {
            return;
        }
        ctx.invitePauseActive = false;
        if (ctx.inviteOverlay != null) {
            ctx.inviteOverlay.remove();
            ctx.inviteOverlay = null;
        }
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (!ctx.pauseMenuOpen && loop != null && loop.getGameState() == GameState.PAUSED) {
            loop.resume();
        }
    }

    public void sendReaction(ReactionPresets.Preset preset) {
        if (preset == null || ctx.multiplayerClient == null || !ctx.multiplayerClient.isConnected()) {
            return;
        }
        String name = ctx.multiplayerUser != null ? ctx.multiplayerUser.getUsername() : "Player";
        ctx.localReactionBubble.playSendPreview(preset, () -> ctx.multiplayerClient.sendPacket(
                new ReactionPacket(name, preset.type(), preset.contentId())));
    }

    public void onReactionReceived(ReactionPacket packet) {
        if (packet == null || ctx.remoteReactionBubble == null) {
            return;
        }
        if (packet.getReactionType() == ReactionType.SURRENDER) {
            return;
        }
        String localName = ctx.multiplayerUser != null ? ctx.multiplayerUser.getUsername() : null;
        if (localName != null && localName.equals(packet.getSenderUsername())) {
            return;
        }
        ctx.remoteReactionBubble.showIncoming(packet);
    }

    public void registerReactions() {
        if (ctx.multiplayerMode && ctx.multiplayerClient != null) {
            ctx.reactionPacketHandler = this::onReactionReceived;
            ctx.multiplayerClient.registerHandler(ReactionPacket.class, ctx.reactionPacketHandler);
        }
    }

    public void flushPendingLoot() {
        if (ctx.entityRenderer != null) {
            ctx.entityRenderer.drainPendingLootFlights();
        }
        GameModel model = GameplayLevelQueries.model();
        if (model != null) {
            applyRemainingLoot(model);
        }
        if (ctx.coinHud != null) {
            ctx.coinHud.setAmount(ctx.currentTotalCoins());
        }
    }

    public void forfeitMultiplayerMatchIfActive() {
        if (ctx.multiplayerForfeitSent || !multiplayerMatchStillActive()) {
            return;
        }
        if (ctx.multiplayerClient == null || !ctx.multiplayerClient.isConnected()) {
            return;
        }
        ctx.multiplayerForfeitSent = true;
        String name = ctx.multiplayerUser != null ? ctx.multiplayerUser.getUsername() : "Player";
        ctx.multiplayerClient.sendPacket(new ReactionPacket(name, ReactionType.SURRENDER, "I yield"));
    }

    void hideHud() {
        for (Actor root : ctx.hudRoots) {
            root.setVisible(false);
            root.setTouchable(Touchable.disabled);
        }
        if (ctx.readySetPlant != null) {
            ctx.readySetPlant.setVisible(false);
        }
        if (ctx.waveAnnounce != null) {
            ctx.waveAnnounce.setVisible(false);
        }
    }

    private void startLoseSequence() {
        GameSaveService.getInstance().clearCurrentUserSave();
        ctx.gameplayMusic.playDefeat(GameplayLevelQueries.currentChapter());
        beginEndSequence();
        ctx.entityRenderer.beginLoseFade();
        if (ctx.scoreMode && GameplayLevelQueries.currentLevel() instanceof ScoreLevel scoreLevel) {
            ctx.myopointResultsOverlay = new MyopointResultsOverlay(
                ctx.view.skin, scoreLevel, false, this::restartLevel, this::exitToLevels);
            ctx.view.uiStage.addActor(ctx.myopointResultsOverlay);
        } else {
            ctx.loseOverlay = new LoseResultsOverlay(
                ctx.view.skin, ctx.view.assets.textures, this::restartLevel, this::exitToLevels);
            ctx.view.uiStage.addActor(ctx.loseOverlay);
        }
        ctx.view.toast.toFront();
    }

    private void startWinSequence() {
        GameSaveService.getInstance().clearCurrentUserSave();
        ctx.gameplayMusic.playVictory(GameplayLevelQueries.currentChapter());
        beginEndSequence();
        ctx.entityRenderer.beginWinFade();
        if (ctx.scoreMode && GameplayLevelQueries.currentLevel() instanceof ScoreLevel scoreLevel) {
            ctx.myopointResultsOverlay = new MyopointResultsOverlay(
                ctx.view.skin, scoreLevel, true, this::restartLevel, this::exitToLevels);
            ctx.view.uiStage.addActor(ctx.myopointResultsOverlay);
        } else {
            ctx.winOverlay = new WinResultsOverlay(
                ctx.view.skin, ctx.view.assets.textures, this::continueToNextLevel, this::exitToLevels);
            ctx.view.uiStage.addActor(ctx.winOverlay);
        }
        ctx.view.toast.toFront();
    }

    private void beginEndSequence() {
        flushPendingLoot();
        ctx.endSequenceActive = true;
        ctx.cursors.clearArmedModes();
        hideHud();
        if (ctx.pauseMenuOpen) {
            closePauseMenu();
        }
    }

    private void exitToLevels() {
        forfeitMultiplayerMatchIfActive();
        flushPendingLoot();
        Level level = GameplayLevelQueries.currentLevel();
        App.getInstance().setCurrentGameModel(null);
        App.getInstance().setCurrentGameLoop(null);
        if (leaveMultiplayerOrScore(level)) {
            return;
        }
        if (ctx.bowlingMode || level instanceof MiniGameLevel) {
            App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);
            ctx.view.game.setScreen(new QuestsScreen(ctx.view.game, QuestsScreen.Tab.MINI_GAMES));
            return;
        }
        goToChapterMap();
    }

    private boolean leaveMultiplayerOrScore(Level level) {
        if (ctx.multiplayerMode) {
            App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);
            ctx.view.game.setScreen(new QuestsScreen(ctx.view.game, QuestsScreen.Tab.MINI_GAMES));
            return true;
        }
        if (ctx.scoreMode || level instanceof ScoreLevel) {
            App.getInstance().setCurrentMenu(MenuType.GAME);
            ctx.view.game.setScreen(new AdventureScreen(ctx.view.game));
            return true;
        }
        return false;
    }

    private void saveAndExit() {
        if (ctx.multiplayerMode) {
            exitToLevels();
            return;
        }
        try {
            GameSaveService.getInstance().saveCurrentGame();
        } catch (Exception e) {
            ctx.view.toast("Could not save game: " + e.getMessage(), true);
            return;
        }
        Level level = GameplayLevelQueries.currentLevel();
        App.getInstance().setCurrentGameModel(null);
        App.getInstance().setCurrentGameLoop(null);
        if (ctx.scoreMode || level instanceof ScoreLevel) {
            App.getInstance().setCurrentMenu(MenuType.GAME);
            ctx.view.game.setScreen(new AdventureScreen(ctx.view.game));
            return;
        }
        saveExitNonScore(level);
    }

    private void saveExitNonScore(Level level) {
        if (ctx.bowlingMode || level instanceof MiniGameLevel) {
            App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);
            ctx.view.game.setScreen(new QuestsScreen(ctx.view.game, QuestsScreen.Tab.MINI_GAMES));
            return;
        }
        goToChapterMap();
    }

    private void goToChapterMap() {
        Chapter chapter = GameplayLevelQueries.currentChapter();
        App.getInstance().setCurrentMenu(MenuType.GAME);
        if (chapter != null) {
            ctx.view.game.setScreen(new ChapterLevelsScreen(ctx.view.game, chapter));
        } else {
            ctx.view.game.setScreen(new AdventureScreen(ctx.view.game));
        }
    }

    private void continueToNextLevel() {
        if (ctx.multiplayerMode) {
            exitToLevels();
            return;
        }
        Level level = GameplayLevelQueries.currentLevel();
        if (ctx.scoreMode || level instanceof ScoreLevel) {
            exitToLevels();
            return;
        }
        if (level instanceof MiniGameLevel mini) {
            enterNextMiniGame(mini);
            return;
        }
        enterNextAdventureLevel(level);
    }

    private void enterNextMiniGame(MiniGameLevel mini) {
        String type = mini.getMiniGameType().name();
        int nextStage = mini.getStage() + 1;
        CommandResult<Void> enter = TravelLogMenuController.getInstance().enterMiniGame(type, nextStage);
        if (!enter.isSuccess()) {
            exitToLevels();
            return;
        }
        ctx.view.game.setScreen(new LevelObjectivesScreen(ctx.view.game, null));
    }

    private void enterNextAdventureLevel(Level level) {
        Chapter chapter = GameplayLevelQueries.currentChapter();
        LevelConfig config = level == null ? null : level.getConfig();
        if (chapter == null || config == null) {
            exitToLevels();
            return;
        }
        int nextId = config.getLevelId() + 1;
        String chapterArg = chapter.name().toLowerCase(Locale.ROOT);
        CommandResult<Void> enter = GameMenuController.getInstance().enterChapter(chapterArg, nextId);
        if (!enter.isSuccess()) {
            exitToLevels();
            return;
        }
        ctx.view.game.setScreen(new LevelObjectivesScreen(ctx.view.game, chapter));
    }

    private void restartLevel() {
        if (ctx.multiplayerMode) {
            exitToLevels();
            return;
        }
        GameSaveService.getInstance().clearCurrentUserSave();
        flushPendingLoot();
        Level level = GameplayLevelQueries.currentLevel();
        if (ctx.scoreMode || level instanceof ScoreLevel) {
            restartScoreGame();
            return;
        }
        if (level instanceof MiniGameLevel mini) {
            restartMiniGame(mini);
            return;
        }
        restartAdventure(level);
    }

    private void restartAdventure(Level level) {
        Chapter chapter = GameplayLevelQueries.currentChapter();
        LevelConfig config = level == null ? null : level.getConfig();
        if (chapter == null || config == null) {
            ctx.view.toast("Cannot restart: no level loaded.", true);
            return;
        }
        List<String> plants = new ArrayList<>(GameplayLevelQueries.selectedPlants());
        String chapterArg = chapter.name().toLowerCase(Locale.ROOT);
        CommandResult<Void> enter = GameMenuController.getInstance()
            .enterChapter(chapterArg, config.getLevelId());
        if (!enter.isSuccess()) {
            ctx.view.toast(enter.getMessage(), true);
            return;
        }
        startSelectedPlants(plants);
    }

    private void restartScoreGame() {
        List<String> plants = new ArrayList<>(GameplayLevelQueries.selectedPlants());
        CommandResult<Void> enter = MainMenuController.getInstance().enterScoreGame();
        if (!enter.isSuccess()) {
            ctx.view.toast(enter.getMessage(), true);
            return;
        }
        startSelectedPlants(plants);
    }

    private void restartMiniGame(MiniGameLevel mini) {
        GameModel current = GameplayLevelQueries.model();
        if (current != null && current.isCouchPlay()) {
            CommandResult<Void> opened = MultiplayerMatchBootstrap.openCouchPlay(ctx.view.game);
            if (!opened.isSuccess()) {
                ctx.view.toast(opened.getMessage(), true);
            }
            return;
        }
        CommandResult<Void> enter = TravelLogMenuController.getInstance()
            .enterMiniGame(mini.getMiniGameType().name(), mini.getStage());
        if (!enter.isSuccess()) {
            ctx.view.toast(enter.getMessage(), true);
            return;
        }
        CommandResult<Void> start = PlantSelectionMenuController.getInstance().startGame();
        if (!start.isSuccess()) {
            ctx.view.toast(start.getMessage(), true);
            return;
        }
        ctx.view.game.setScreen(new GameplayScreen(ctx.view.game));
    }

    private void startSelectedPlants(List<String> plants) {
        GameModel model = GameplayLevelQueries.model();
        if (model != null) {
            model.setSelectedPlants(plants);
        }
        CommandResult<Void> start = PlantSelectionMenuController.getInstance().startGame();
        if (!start.isSuccess()) {
            ctx.view.toast(start.getMessage(), true);
            return;
        }
        ctx.view.game.setScreen(new GameplayScreen(ctx.view.game));
    }

    private void pauseLoopIfRunning() {
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop != null && loop.getGameState() == GameState.RUNNING) {
            loop.pause();
        }
    }

    private void applyRemainingLoot(GameModel model) {
        List<LootPickup> pending = model.getActiveLootPickups();
        if (pending == null || pending.isEmpty()) {
            return;
        }
        for (LootPickup loot : new ArrayList<>(pending)) {
            model.applyLootPickup(loot);
            model.removeLootPickup(loot);
        }
    }

    private boolean multiplayerMatchStillActive() {
        if (!ctx.multiplayerMode) {
            return false;
        }
        if (ctx.multiplayerClient != null) {
            GameStateSnapshotPacket snap = ctx.multiplayerClient.getLatestSnapshot();
            if (snap != null && snap.isGameOver()) {
                return false;
            }
        }
        GameModel model = GameplayLevelQueries.model();
        if (model != null) {
            GameState state = model.getState();
            if (state == GameState.WON || state == GameState.LOST) {
                return false;
            }
        }
        return true;
    }
}
