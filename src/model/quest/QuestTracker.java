package model.quest;

import model.app.App;
import model.data.quest.QuestLoader;
import model.enums.PlantCategory;
import model.game.core.GameModel;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.user.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluates quest conditions against a finished level and persists progress
 * on the user (claim model: rewards are still taken via 'complete quest').
 */
public final class QuestTracker {

    private static final String QUESTS_JSON = "/assets/data/quests/quests.json";
    private static final int MAX_DIFFICULTY = 5;

    private QuestTracker() {
    }

    /** Called exactly once when a level finishes (won or lost). */
    public static void onLevelEnd(GameModel model, boolean won) {
        if (model == null) return;
        User user = App.getInstance().getCurrentUser();
        if (user == null) return;

        List<Quest> quests;
        try {
            quests = new QuestLoader().load(QUESTS_JSON);
        } catch (IOException e) {
            return;
        }

        if (user.getQuestProgress() == null) {
            user.setQuestProgress(new HashMap<>());
        }
        Map<String, Integer> progress = user.getQuestProgress();
        Map<String, Boolean> status = user.getQuestStatus();

        for (Quest quest : quests) {
            // already claimed quests are skipped
            if (status != null && Boolean.TRUE.equals(status.get(quest.getName()))) continue;
            evaluate(quest, model, won, progress);
        }
        App.getInstance().getUserRepository().flush();
    }

    private static void evaluate(Quest quest, GameModel model, boolean won, Map<String, Integer> progress) {
        String name = quest.getName();
        String base = baseName(name);
        String value = quest.getVariable();
        int target = quest.getProgress() != null ? quest.getProgress().getTargetValue() : 1;
        boolean plantsOnly = !model.isLawnMowerUsed();
        List<Plant> planted = model.getPlantsPlaced();

        switch (base) {
            case "Daily Sun Collector":
                add(progress, name, model.getSunCollected(), target);
                break;
            case "Mowing Time":
                add(progress, name, model.getMowerKills(), target);
                break;
            case "Chapter Hunter":
                if (model.getChapter() != null && model.getChapter().name().equalsIgnoreCase(value)) {
                    add(progress, name, model.getZombiesKilled(), target);
                }
                break;
            case "Professional Plant Opener":
                // daily variant: count kills dealt exclusively by today's plant
                add(progress, name, model.getExclusivePlantKills(value), target);
                break;
            case "Only Cactus":
                // count kills dealt exclusively by Cactus
                add(progress, name, model.getExclusivePlantKills("Cactus"), target);
                break;
            case "Economic Plant Eater":
                if (won && model.getPlantsLost() <= parseInt(value, -1)) markComplete(progress, name, target);
                break;
            case "Defense Master":
                if (won && model.getSunAmount() == 0) markComplete(progress, name, target);
                break;
            case "Speed of Action":
                best(progress, name, model.getKillsWithin30s(), target);
                break;
            case "Professional Destroyer":
                best(progress, name, countCategory(planted, PlantCategory.EXPLOSIVE), target);
                break;
            case "Symmetry":
                if (won && !planted.isEmpty() && isGardenSymmetric(model)) markComplete(progress, name, target);
                break;
            case "Family Slaughter":
                // every zombie killed this level must have died solely to today's family
                if (model.getZombiesKilled() > 0
                        && model.getExclusiveFamilyKills(value) == model.getZombiesKilled()) {
                    markComplete(progress, name, target);
                }
                break;
            case "Bloom in Constraints":
                if (won && countCategoryName(planted, value) == 0) markComplete(progress, name, target);
                break;
            case "Night or Morning":
                if (won && !model.isNightLevel() && allShrooms(planted)) markComplete(progress, name, target);
                break;
            case "Cloudy Day":
                // never more than 3 sun producers on the field at the same time
                if (won && !planted.isEmpty() && model.getMaxSunProducersAtOnce() <= 3) {
                    markComplete(progress, name, target);
                }
                break;
            case "Win Streak":
                if (won && model.getDifficulty() >= MAX_DIFFICULTY) add(progress, name, 1, target);
                else progress.put(name, 0); // streak broken
                break;
            case "Almost Victorious":
                add(progress, name, model.getNoMowerFirstColumnKills(), target);
                break;
            case "No OCD":
                // no mirrored pair may hold the same plant (empty cells ignored)
                if (won && !planted.isEmpty() && !hasAnySymmetricPair(model)) {
                    markComplete(progress, name, target);
                }
                break;
            case "One Column Less":
                if (won && !model.getColumnsPlanted().contains(parseInt(value, -1))) {
                    markComplete(progress, name, target);
                }
                break;
            case "Defenseless Row":
                if (won && !model.getRowsPlanted().contains(parseInt(value, -1))) {
                    markComplete(progress, name, target);
                }
                break;
            case "Defenseless Cross": {
                int index = parseInt(value, -1);
                if (won && !model.getColumnsPlanted().contains(index)
                        && !model.getRowsPlanted().contains(index)) {
                    markComplete(progress, name, target);
                }
                break;
            }
            default:
                break;
        }
    }

    // ---- plant list helpers ----

    private static boolean allNamed(List<Plant> planted, String plantName) {
        if (planted == null || planted.isEmpty()) return false;
        for (Plant p : planted) {
            if (p == null || p.getName() == null || !p.getName().equalsIgnoreCase(plantName)) return false;
        }
        return true;
    }

    private static boolean allOfCategory(List<Plant> planted, String category) {
        if (planted == null || planted.isEmpty() || category == null) return false;
        for (Plant p : planted) {
            if (p == null || p.getCategory() == null
                    || !p.getCategory().name().equalsIgnoreCase(category)) return false;
        }
        return true;
    }

    private static boolean allShrooms(List<Plant> planted) {
        if (planted == null || planted.isEmpty()) return false;
        for (Plant p : planted) {
            if (p == null || !p.isShroom()) return false;
        }
        return true;
    }

    private static int countCategory(List<Plant> planted, PlantCategory category) {
        int count = 0;
        for (Plant p : planted) {
            if (p != null && p.getCategory() == category) count++;
        }
        return count;
    }

    private static int countCategoryName(List<Plant> planted, String category) {
        int count = 0;
        for (Plant p : planted) {
            if (p != null && p.getCategory() != null
                    && p.getCategory().name().equalsIgnoreCase(category)) count++;
        }
        return count;
    }

    /** Mirror symmetry of planted types across the horizontal middle row. */
    private static boolean isGardenSymmetric(GameModel model) {
        int rows = model.getRowCount();
        int cols = model.getColumnCount();
        for (int r = 0; r < rows / 2; r++) {
            int mirror = rows - 1 - r;
            for (int c = 0; c < cols; c++) {
                if (!Objects.equals(plantNameAt(model, r, c), plantNameAt(model, mirror, c))) return false;
            }
        }
        return true;
    }

    /** True if any mirrored cell pair (across the middle row) holds the same plant (empty cells ignored). */
    private static boolean hasAnySymmetricPair(GameModel model) {
        int rows = model.getRowCount();
        int cols = model.getColumnCount();
        for (int r = 0; r < rows / 2; r++) {
            int mirror = rows - 1 - r;
            for (int c = 0; c < cols; c++) {
                String a = plantNameAt(model, r, c);
                if (a != null && a.equals(plantNameAt(model, mirror, c))) return true;
            }
        }
        return false;
    }

    private static String plantNameAt(GameModel model, int row, int col) {
        PlantInstance plant = model.getPlantAt(row, col);
        if (plant == null || plant.getDefinition() == null) return null;
        return plant.getDefinition().getName();
    }

    // ---- progress helpers ----

    private static String baseName(String questName) {
        int idx = questName.indexOf(" (");
        return idx > 0 ? questName.substring(0, idx) : questName;
    }

    private static void add(Map<String, Integer> progress, String name, int delta, int target) {
        if (delta <= 0) return;
        int current = progress.getOrDefault(name, 0);
        progress.put(name, Math.min(target, current + delta));
    }

    /** Keeps the best single-level result (for "in one level" quests). */
    private static void best(Map<String, Integer> progress, String name, int levelValue, int target) {
        if (levelValue > progress.getOrDefault(name, 0)) {
            progress.put(name, Math.min(target, levelValue));
        }
    }

    private static void markComplete(Map<String, Integer> progress, String name, int target) {
        progress.put(name, target);
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
