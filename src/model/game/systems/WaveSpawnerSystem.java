package model.game.systems;

import model.enums.Chapter;

public class WaveSpawnerSystem {
    private int currentWave;
    private float waveDifficulty;
    private boolean isFinalWave;
    private Chapter chapterContext;

    public int getCurrentWave() {
        return currentWave;
    }
    public boolean isFinalWave() {
        return isFinalWave;
    }

    public void startNextWave() {

    }
    public void checkWaveProgress() {

    }
    public void spawnZombie(String zombieType, int lane) {

    }
    public void applyChapterRules(Chapter chapter) {

    }
}