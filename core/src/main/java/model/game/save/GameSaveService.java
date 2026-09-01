package model.game.save;

import model.app.App;
import model.enums.Chapter;
import model.enums.MiniGameType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.user.User;

import java.io.IOException;
import java.util.Optional;

/**
 * Captures / restores an in-progress {@link GameModel} for Save and Exit.
 */
public final class GameSaveService {

    private static GameSaveService instance;

    private final GameSaveRepository repository;

    private GameSaveService(GameSaveRepository repository) {
        this.repository = repository;
    }

    public static synchronized GameSaveService getInstance() {
        if (instance == null) {
            instance = new GameSaveService(new GameSaveRepository());
        }
        return instance;
    }

    /** Test hook: replaces the singleton (and its repository). */
    public static synchronized void resetForTests(GameSaveRepository repository) {
        instance = new GameSaveService(repository == null ? new GameSaveRepository() : repository);
    }

    public GameSaveRepository getRepository() {
        return repository;
    }

    public Optional<GameSaveData> findSaveForCurrentUser() {
        User user = App.getInstance().getCurrentUser();
        if (user == null) {
            return Optional.empty();
        }
        return repository.load(user.getUsername());
    }

    public boolean hasSaveForAdventure(Chapter chapter, int levelId) {
        return findSaveForCurrentUser()
                .filter(s -> s.getMode() == GameSaveData.Mode.ADVENTURE)
                .filter(s -> s.getChapter() == chapter && s.getLevelId() == levelId)
                .isPresent();
    }

    public boolean hasSaveForMiniGame(MiniGameType type, int stage) {
        return findSaveForCurrentUser()
                .filter(s -> s.getMode() == GameSaveData.Mode.MINI_GAME)
                .filter(s -> s.getMiniGameType() == type && s.getMiniGameStage() == stage)
                .isPresent();
    }

    public boolean hasScoreSave() {
        return findSaveForCurrentUser()
                .filter(s -> s.getMode() == GameSaveData.Mode.SCORE)
                .isPresent();
    }

    public void clearCurrentUserSave() {
        User user = App.getInstance().getCurrentUser();
        if (user != null) {
            repository.delete(user.getUsername());
        }
    }

    /**
     * Snapshots the active session to disk. Does not clear {@link App} session state.
     */
    public void saveCurrentGame() throws IOException {
        User user = App.getInstance().getCurrentUser();
        GameModel model = App.getInstance().getCurrentGameModel();
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (user == null || model == null) {
            throw new IllegalStateException("No active game to save.");
        }
        GameSaveData data = capture(user.getUsername(), model, loop);
        repository.save(data);
    }

    /**
     * Rebuilds {@link App}'s game model/loop from the current user's save and
     * switches to {@link model.enums.MenuType#IN_GAME}.
     */
    public void resumeSavedGame() throws IOException {
        User user = App.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No logged-in user.");
        }
        GameSaveData data = repository.load(user.getUsername())
                .orElseThrow(() -> new IllegalStateException("No saved game."));
        restoreIntoApp(data);
    }

    public GameSaveData capture(String username, GameModel model, PvZGameLoop loop) {
        return GameSaveCapture.capture(username, model, loop);
    }

    public void restoreIntoApp(GameSaveData data) throws IOException {
        GameSaveRestore.restoreIntoApp(data);
    }
}
