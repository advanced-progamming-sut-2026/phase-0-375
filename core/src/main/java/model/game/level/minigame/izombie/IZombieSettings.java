package model.game.level.minigame.izombie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tunable configuration for one I, Zombie stage, populated from the
 * I, Zombie specific keys in minigames.json
 */
public class IZombieSettings {

    /** A pre-planted plant at a fixed board position. */
    public static class PlantPlacement {
        private final String plant;
        private final int row;
        private final int col;

        public PlantPlacement(String plant, int row, int col) {
            this.plant = plant;
            this.row = row;
            this.col = col;
        }

        public String getPlant() { return plant; }
        public int getRow() { return row; }
        public int getCol() { return col; }
    }

    /** Placeable roster: zombie definition name -> sun cost. */
    private final Map<String, Integer> zombieCosts = new LinkedHashMap<>();
    /** Pre-planted defense: each plant at its configured position. */
    private final List<PlantPlacement> plantLayout = new ArrayList<>();
    /** Column of the red line; zombies may only be placed at or right of it. */
    private int redLineColumn = 3;
    /** Definition name of the stationary sun-producing zombie (one per lane). */
    private String sunZombie = "ZombieIZombieSun";

    public Map<String, Integer> getZombieCosts() {
        return Collections.unmodifiableMap(zombieCosts);
    }

    public void addPlaceableZombie(String zombieName, int cost) {
        if (zombieName != null && !zombieName.isBlank()) {
            zombieCosts.put(zombieName, Math.max(0, cost));
        }
    }

    /** Cheapest roster cost; drives the out-of-sun loss condition. */
    public int minZombieCost() {
        int min = Integer.MAX_VALUE;
        for (int cost : zombieCosts.values()) {
            min = Math.min(min, cost);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    public List<PlantPlacement> getPlantLayout() {
        return Collections.unmodifiableList(plantLayout);
    }

    public void addPlantPlacement(String plant, int row, int col) {
        if (plant != null && !plant.isBlank()) {
            plantLayout.add(new PlantPlacement(plant, row, col));
        }
    }

    public int getRedLineColumn() {
        return redLineColumn;
    }

    public void setRedLineColumn(int redLineColumn) {
        this.redLineColumn = redLineColumn;
    }

    public String getSunZombie() {
        return sunZombie;
    }

    public void setSunZombie(String sunZombie) {
        this.sunZombie = sunZombie;
    }
}
