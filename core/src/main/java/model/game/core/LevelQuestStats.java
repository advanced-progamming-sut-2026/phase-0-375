package model.game.core;

import model.enums.PlantCategory;
import model.game.map.GameMap;
import model.game.map.Lane;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-level statistics used for quest tracking, extracted from
 * {@link GameModel} by composition: the model owns one instance and
 * delegates all quest-stat bookkeeping to it.
 */
final class LevelQuestStats {

    private int sunCollected;
    private boolean lawnMowerUsed;
    private float firstZombieSpawnTime = -1f;
    private int killsWithin30s;
    private int noMowerFirstColumnKills;
    private int mowerKills;
    private final List<Plant> plantsPlaced = new ArrayList<>();
    private final Set<Integer> rowsPlanted = new HashSet<>();
    private final Set<Integer> columnsPlanted = new HashSet<>();
    private int maxSunProducersAtOnce; // peak simultaneous sun producers on the field
    private final Map<String, Integer> exclusivePlantKills = new HashMap<>();
    private final Map<PlantCategory, Integer> exclusiveFamilyKills = new HashMap<>();

    void onSunCollected(int value) { sunCollected += value; }

    int getSunCollected() { return sunCollected; }

    void markLawnMowerUsed() { lawnMowerUsed = true; }

    boolean isLawnMowerUsed() { return lawnMowerUsed; }

    int getKillsWithin30s() { return killsWithin30s; }

    int getNoMowerFirstColumnKills() { return noMowerFirstColumnKills; }

    int getMowerKills() { return mowerKills; }

    List<Plant> getPlantsPlaced() { return plantsPlaced; }

    Set<Integer> getRowsPlanted() { return rowsPlanted; }

    Set<Integer> getColumnsPlanted() { return columnsPlanted; }

    int getMaxSunProducersAtOnce() { return maxSunProducersAtOnce; }

    Map<String, Integer> getExclusivePlantKillsMap() { return exclusivePlantKills; }

    Map<PlantCategory, Integer> getExclusiveFamilyKillsMap() { return exclusiveFamilyKills; }

    /** Notes the first zombie spawn time for the 30-second kill counter. */
    void onZombieSpawned(float elapsedSeconds) {
        if (firstZombieSpawnTime < 0) {
            firstZombieSpawnTime = elapsedSeconds;
        }
    }

    /** Records placement bookkeeping (placed list, used rows/columns, sun-producer peak). */
    void onPlantPlaced(GameModel model, Plant definition, int row, int col) {
        plantsPlaced.add(definition);
        rowsPlanted.add(row);
        columnsPlanted.add(col);
        updateMaxSunProducers(model); // count can only grow on placement
    }

    /** Marks a row/column as used when a plant moves into it. */
    void markRowColumnPlanted(int row, int col) {
        rowsPlanted.add(row);
        columnsPlanted.add(col);
    }

    /** Kills where the zombie was damaged exclusively by the named plant. */
    int getExclusivePlantKills(String plantName) {
        if (plantName == null) return 0;
        for (Map.Entry<String, Integer> e : exclusivePlantKills.entrySet()) {
            if (plantName.equalsIgnoreCase(e.getKey())) return e.getValue();
        }
        return 0;
    }

    /** Kills where the zombie was damaged exclusively by plants of the named family. */
    int getExclusiveFamilyKills(String categoryName) {
        if (categoryName == null) return 0;
        for (Map.Entry<PlantCategory, Integer> e : exclusiveFamilyKills.entrySet()) {
            if (e.getKey().name().equalsIgnoreCase(categoryName)) return e.getValue();
        }
        return 0;
    }

    /** Records a kill with timing/position details for quest tracking. */
    void onZombieKilled(ZombieInstance zombie, float elapsedSeconds, GameMap gameMap) {
        if (firstZombieSpawnTime >= 0 && elapsedSeconds - firstZombieSpawnTime <= 30f) {
            killsWithin30s++;
        }
        if (zombie == null) return;
        if (zombie.isKilledByMower()) mowerKills++;
        // exclusive-kill bookkeeping for plant/family quests
        if (!zombie.isNonPlantDamaged()) {
            if (zombie.getPlantDamagers().size() == 1) {
                exclusivePlantKills.merge(zombie.getPlantDamagers().iterator().next(), 1, Integer::sum);
            }
            if (zombie.getPlantDamagerFamilies().size() == 1) {
                exclusiveFamilyKills.merge(zombie.getPlantDamagerFamilies().iterator().next(), 1, Integer::sum);
            }
        }
        if (zombie.getGridPosition() == null) return;
        if (zombie.getGridX() <= 0) {
            Lane lane = gameMap.getLane(zombie.getGridY());
            if (lane == null || !lane.hasActiveLawnMower()) {
                noMowerFirstColumnKills++;
            }
        }
    }

    /** Recounts sun producers on the field and updates the peak. */
    private void updateMaxSunProducers(GameModel model) {
        int count = 0;
        for (int r = 0; r < model.getRowCount(); r++) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                PlantInstance p = model.getPlantAt(r, c);
                if (p != null && p.getDefinition() != null
                        && p.getDefinition().getCategory() == PlantCategory.SUN_PRODUCER) {
                    count++;
                }
            }
        }
        if (count > maxSunProducersAtOnce) {
            maxSunProducersAtOnce = count;
        }
    }
}
