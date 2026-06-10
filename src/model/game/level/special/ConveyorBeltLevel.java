package model.game.level.special;

import model.enums.Chapter;
import model.enums.LevelType;
import model.game.GameModel;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.rule.GameRules;
import model.game.wave.Wave;

import java.util.List;

public class ConveyorBeltLevel extends Level {

    public ConveyorBeltLevel(LevelConfig config) {
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
