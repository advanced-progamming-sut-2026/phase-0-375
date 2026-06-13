package model.game.level.special;

import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.LevelConfig;

public class PlantWhatYouGetLevel extends Level {

    public PlantWhatYouGetLevel(LevelConfig config) {
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
