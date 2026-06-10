package model.game.level.special;

import model.game.GameModel;
import model.game.level.Level;
import model.game.level.LevelConfig;

public class NightOpsLevel extends Level {

    public NightOpsLevel(LevelConfig config) {
        super(config);
    }

    @Override
    public boolean canStart() {
        return false;
    }

    @Override
    public void onStart() {

    }

    @Override
    public void tick(float deltaTime) {

    }

    @Override
    public void onWaveCleared(int waveNumber) {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onFail() {

    }

    @Override
    public boolean checkWinCondition(GameModel model) {
        return false;
    }

    @Override
    public boolean checkLossCondition(GameModel model) {
        return false;
    }
}
