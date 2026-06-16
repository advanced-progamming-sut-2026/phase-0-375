package model.game.core;

import model.enums.GameState;
import model.event.EventBus;
import model.game.level.LevelConfig;
import model.game.systems.*;
import model.game.wave.WaveManager;

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

    private EventBus eventBus;

    public PvZGameLoop(GameModel gameModel) {
        this.gameModel = gameModel;
        this.gameState = GameState.RUNNING;

        this.eventBus = gameModel.getEventBus();

        LevelConfig levelConfig = gameModel.getCurrentLevel().getConfig();

        float sunDropRateModifier = (float) levelConfig.getRules().getSunDropRateModifier();
        boolean skyDropEnabled = levelConfig.getRules().isSkyDropEnabled();
        this.sunFallSystem = new SunFallSystem(sunDropRateModifier, skyDropEnabled);

        this.plantSystem = new PlantSystem(gameModel, eventBus);
        this.projectileSystem = new ProjectileSystem(gameModel, eventBus);
        this.zombieSystem = new ZombieSystem(gameModel, eventBus);
        this.combatSystem = new CombatSystem(gameModel, eventBus);
        this.waveManager = gameModel.getWaveManager();
        this.lawnMowerSystem = new LawnMowerSystem(gameModel, eventBus);
        this.pushableSystem = new PushableSystem(gameModel, eventBus);
    }

    @Override
    public void update(float deltaTime) {
        if (gameState == GameState.PAUSED) return;

        sunFallSystem.tick(deltaTime);
        plantSystem.tick(deltaTime);
        projectileSystem.tick(deltaTime);
        zombieSystem.tick(deltaTime);
        combatSystem.tick(deltaTime);
        waveManager.tick(deltaTime);
        lawnMowerSystem.tick(deltaTime);
        pushableSystem.tick(deltaTime);
        gameModel.tick(deltaTime);


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
}
