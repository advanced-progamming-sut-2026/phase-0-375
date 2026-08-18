package model.game.core;

import model.app.App;
import model.enums.GameState;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.level.LevelConfig;
import model.game.rule.EndGameCondition;
import model.game.systems.*;
import model.game.wave.WaveManager;
import model.quest.QuestTracker;
import model.user.User;

public class PvZGameLoop implements GameLoop {

    private final GameModel gameModel;
    private GameState gameState;

    private SunFallSystem sunFallSystem;
    private PlantSystem plantSystem;
    private ProjectileSystem projectileSystem;
    private ZombieSystem zombieSystem;
    private CombatSystem combatSystem;
    private WaveManager waveManager;
    private LawnMowerSystem lawnMowerSystem;
    private PushableSystem pushableSystem;
    private TerrainSystem terrainSystem;

    private EventBus eventBus;

    public PvZGameLoop(GameModel gameModel) {
        this.gameModel = gameModel;
        this.gameState = GameState.RUNNING;

        this.eventBus = gameModel.getEventBus();

        LevelConfig levelConfig = gameModel.getCurrentLevel().getConfig();

        float sunDropRateModifier = (float) levelConfig.getRules().getSunDropRateModifier();
        boolean skyDropEnabled = levelConfig.getRules().isSkyDropEnabled()
                && !levelConfig.isHasNightEffect()
                && levelConfig.getRules().isSunFallsFromSky();
        sunDropRateModifier *= gameModel.difficultyPenalty();
        this.sunFallSystem = new SunFallSystem(gameModel, sunDropRateModifier, skyDropEnabled);

        this.plantSystem = new PlantSystem(gameModel);
        this.projectileSystem = new ProjectileSystem(gameModel, eventBus);
        this.zombieSystem = new ZombieSystem(gameModel, eventBus);
        this.combatSystem = new CombatSystem(gameModel, eventBus);
        this.waveManager = gameModel.getWaveManager();
        this.lawnMowerSystem = new LawnMowerSystem(gameModel, eventBus);
        this.pushableSystem = new PushableSystem(gameModel, eventBus);
        this.terrainSystem = new TerrainSystem(gameModel, eventBus);
    }

    @Override
    public void update(float deltaTime) {
        if (gameState != GameState.RUNNING) return;

        float scaledDelta = deltaTime * currentGameSpeed();

        sunFallSystem.tick(scaledDelta);
        plantSystem.tick(scaledDelta);
        projectileSystem.tick(scaledDelta);
        zombieSystem.tick(scaledDelta);
        combatSystem.tick(scaledDelta);
        waveManager.tick(scaledDelta);
        lawnMowerSystem.tick(scaledDelta);
        pushableSystem.tick(scaledDelta);
        terrainSystem.tick(scaledDelta);
        gameModel.tick(scaledDelta);

        // Level-specific per-tick logic (conveyor belts, mini-game physics, ...).
        if (gameModel.getCurrentLevel() != null) {
            gameModel.getCurrentLevel().tick(scaledDelta);
        }

        evaluateEndGame();


    }

    /** Settings menu game-speed (1–3); defaults to 1x when no user is logged in. */
    private static float currentGameSpeed() {
        User user = App.getInstance().getCurrentUser();
        return user == null ? 1f : user.getGameSpeed();
    }

    /** Checks the level's end-game condition and finishes the game on a verdict. */
    private void evaluateEndGame() {
        EndGameCondition condition = gameModel.getEndGameCondition();
        if (condition == null) return;

        if (condition.isGameOver(gameModel)) {
            finish(GameState.LOST, GameEvent.Type.GAME_LOST);
        } else if (condition.isWin(gameModel)) {
            finish(GameState.WON, GameEvent.Type.GAME_WON);
        }
    }

    private void finish(GameState result, GameEvent.Type eventType) {
        this.gameState = result;
        gameModel.setGameState(result);

        if (result == GameState.WON) {
            gameModel.getCurrentLevel().onComplete();
        } else {
            gameModel.getCurrentLevel().onFail();
        }
        if (eventBus != null) {
            eventBus.dispatch(new GameEvent(eventType));
        }

        QuestTracker.onLevelEnd(gameModel, result == GameState.WON);
    }

    public SunFallSystem getSunFallSystem() {
        return sunFallSystem;
    }

    @Override
    public GameState getGameState() {
        return gameState;
    }

    @Override
    public void pause() {
        this.gameState = GameState.PAUSED;
    }

    @Override
    public void resume() {
        this.gameState = GameState.RUNNING;
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    public void setPlantClipDurations(model.plant.ability.PlantClipDurations clipDurations) {
        plantSystem.setClipDurations(clipDurations);
    }

    public void setPlantProjectileOrigins(model.plant.ability.PlantProjectileOrigins projectileOrigins) {
        plantSystem.setProjectileOrigins(projectileOrigins);
    }
}
