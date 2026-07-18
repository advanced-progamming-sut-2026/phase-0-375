package model.data.minigame;

import model.data.level.LevelDataEntry;

import java.util.List;

/**
 * JSON DTO for one mini-game stage in minigames.json.
 *
 * Inherits every regular level field (rows, columns, rules, waves, ...)
 * from LevelDataEntry so mini-game stages are described with the exact
 * same vocabulary as normal levels, and adds the mini-game specific keys.
 */
public class MiniGameDataEntry extends LevelDataEntry {

    private String miniGameType;
    private int stage = 1;
    private int difficultyTier = 3;
    private int coinReward = 100;

    // --- Vase Breaker specific keys ---

    /** How many of the rightmost lawn columns hold vases. */
    private int vaseColumns = 3;
    /** Vases with random contents: empty, a normal zombie, or a seed packet. */
    private int randomVaseCount;
    /** Vases guaranteed to contain a seed packet. */
    private int seedVaseCount;
    /** Vases guaranteed to contain the giant-vase zombie. */
    private int giantVaseCount;
    /** Relative odds for what a random vase holds. */
    private float randomEmptyWeight = 1f;
    private float randomZombieWeight = 2f;
    private float randomSeedWeight = 1f;
    /** Seconds a revealed seed packet stays plantable. */
    private float seedPacketExpirySeconds = 15f;
    /** Zombie definition hidden inside giant vases. */
    private String giantVaseZombie = "ZombieGargantuar";
    /** Zombie definition names drawn from when a random vase holds a zombie. */
    private List<String> vaseZombies;
    /** Plant definition names drawn from for seed packets. */
    private List<String> vasePlants;

    public String getMiniGameType() { return miniGameType; }
    public void setMiniGameType(String miniGameType) { this.miniGameType = miniGameType; }

    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }

    public int getDifficultyTier() { return difficultyTier; }
    public void setDifficultyTier(int difficultyTier) { this.difficultyTier = difficultyTier; }

    public int getCoinReward() { return coinReward; }
    public void setCoinReward(int coinReward) { this.coinReward = coinReward; }

    public int getVaseColumns() { return vaseColumns; }
    public void setVaseColumns(int vaseColumns) { this.vaseColumns = vaseColumns; }

    public int getRandomVaseCount() { return randomVaseCount; }
    public void setRandomVaseCount(int randomVaseCount) { this.randomVaseCount = randomVaseCount; }

    public int getSeedVaseCount() { return seedVaseCount; }
    public void setSeedVaseCount(int seedVaseCount) { this.seedVaseCount = seedVaseCount; }

    public int getGiantVaseCount() { return giantVaseCount; }
    public void setGiantVaseCount(int giantVaseCount) { this.giantVaseCount = giantVaseCount; }

    public float getRandomEmptyWeight() { return randomEmptyWeight; }
    public void setRandomEmptyWeight(float randomEmptyWeight) { this.randomEmptyWeight = randomEmptyWeight; }

    public float getRandomZombieWeight() { return randomZombieWeight; }
    public void setRandomZombieWeight(float randomZombieWeight) { this.randomZombieWeight = randomZombieWeight; }

    public float getRandomSeedWeight() { return randomSeedWeight; }
    public void setRandomSeedWeight(float randomSeedWeight) { this.randomSeedWeight = randomSeedWeight; }

    public float getSeedPacketExpirySeconds() { return seedPacketExpirySeconds; }
    public void setSeedPacketExpirySeconds(float seedPacketExpirySeconds) { this.seedPacketExpirySeconds = seedPacketExpirySeconds; }

    public String getGiantVaseZombie() { return giantVaseZombie; }
    public void setGiantVaseZombie(String giantVaseZombie) { this.giantVaseZombie = giantVaseZombie; }

    public List<String> getVaseZombies() { return vaseZombies; }
    public void setVaseZombies(List<String> vaseZombies) { this.vaseZombies = vaseZombies; }

    public List<String> getVasePlants() { return vasePlants; }
    public void setVasePlants(List<String> vasePlants) { this.vasePlants = vasePlants; }

    // --- I, Zombie specific keys ---

    /** Placeable zombie roster: definition name + sun cost (5 per stage). */
    private List<IZombieZombieData> placeableZombies;
    /** Pre-planted defense: each plant with its fixed row and column. */
    private List<IZombiePlantData> prePlantedPlants;
    /** Definition name of the stationary sun-producing zombie. */
    private String sunZombie = "ZombieIZombieSun";

    public List<IZombieZombieData> getPlaceableZombies() { return placeableZombies; }
    public void setPlaceableZombies(List<IZombieZombieData> placeableZombies) { this.placeableZombies = placeableZombies; }

    public List<IZombiePlantData> getPrePlantedPlants() { return prePlantedPlants; }
    public void setPrePlantedPlants(List<IZombiePlantData> prePlantedPlants) { this.prePlantedPlants = prePlantedPlants; }

    public String getSunZombie() { return sunZombie; }
    public void setSunZombie(String sunZombie) { this.sunZombie = sunZombie; }
}
