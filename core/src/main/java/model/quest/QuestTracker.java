package model.quest;

import model.app.App;
import model.data.quest.QuestLoader;
import model.enums.PlantCategory;
import model.game.core.GameModel;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.user.User;
import model.user.persistance.UserSync;

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
        UserSync.persistQuestProgressFromCurrentUser();
    }

    /** A single quest's evaluation rule (Strategy pattern). */
    @FunctionalInterface
    private interface QuestRule {
        void apply(QuestContext c);
    }

    /** Everything a rule needs to evaluate one quest at level end. */
    private record QuestContext(String name, String value, int target, GameModel model,
                                boolean won, Map<String, Integer> progress) {
        List<Plant> planted() { return model.getPlantsPlaced(); }
        int intValue() { return QuestTracker.parseInt(value, -1); }
        boolean wonWithPlants() { return won && !planted().isEmpty(); }
        void add(int delta) { QuestTracker.add(progress, name, delta, target); }
        void best(int candidate) { QuestTracker.best(progress, name, candidate, target); }
        void complete() { QuestTracker.markComplete(progress, name, target); }
        void completeIf(boolean condition) { if (condition) complete(); }
    }

    /** Rule registry: quest base name -> evaluation strategy. */
    private static final Map<String, QuestRule> RULES = buildRules();

    private static Map<String, QuestRule> buildRules() {
        Map<String, QuestRule> rules = new HashMap<>();
        registerCounterRules(rules);
        registerCompletionRules(rules);
        return rules;
    }

    /** Quests that accumulate a counter or track a best run. */
    private static void registerCounterRules(Map<String, QuestRule> rules) {
        rules.put("Daily Sun Collector",
                c -> c.add(c.model().getSunCollected()));
        rules.put("Mowing Time",
                c -> c.add(c.model().getMowerKills()));
        rules.put("Chapter Hunter", c -> {
            if (c.model().getChapter() != null && c.model().getChapter().name().equalsIgnoreCase(c.value())) {
                c.add(c.model().getZombiesKilled());
            }
        });
        // daily variant: count kills dealt exclusively by today's plant
        rules.put("Plant Specialist",
                c -> c.add(c.model().getExclusivePlantKills(c.value())));
        // count kills dealt exclusively by Cactus
        rules.put("Only Cactus",
                c -> c.add(c.model().getExclusivePlantKills("Cactus")));
        rules.put("Quick Strike",
                c -> c.best(c.model().getKillsWithin30s()));
        rules.put("Professional Destroyer",
                c -> c.best(countCategory(c.planted(), PlantCategory.EXPLOSIVE)));
        rules.put("Almost Victorious",
                c -> c.add(c.model().getNoMowerFirstColumnKills()));
        rules.put("Win Streak", c -> {
            if (c.won() && c.model().getDifficulty() >= MAX_DIFFICULTY) c.add(1);
            else c.progress().put(c.name(), 0); // streak broken
        });
    }

    /** Quests that complete outright when their condition holds at level end. */
    private static void registerCompletionRules(Map<String, QuestRule> rules) {
        rules.put("Frugal Planter",
                c -> c.completeIf(c.won() && c.model().getPlantsLost() <= c.intValue()));
        rules.put("Defense Master",
                c -> c.completeIf(c.won() && c.model().getSunAmount() == 0));
        rules.put("Symmetry",
                c -> c.completeIf(c.wonWithPlants() && isGardenSymmetric(c.model())));
        // every zombie killed this level must have died solely to today's family
        rules.put("Family Slaughter",
                c -> c.completeIf(c.model().getZombiesKilled() > 0
                && c.model().getExclusiveFamilyKills(c.value()) == c.model().getZombiesKilled()));
        rules.put("Bloom in Constraints",
                c -> c.completeIf(c.won() && countCategoryName(c.planted(), c.value()) == 0));
        rules.put("Night or Morning",
                c -> c.completeIf(c.won() && !c.model().isNightLevel() && allShrooms(c.planted())));
        // never more than 3 sun producers on the field at the same time
        rules.put("Cloudy Day",
                c -> c.completeIf(c.wonWithPlants() && c.model().getMaxSunProducersAtOnce() <= 3));
        // no mirrored pair may hold the same plant (empty cells ignored)
        rules.put("No OCD",
                c -> c.completeIf(c.wonWithPlants() && !hasAnySymmetricPair(c.model())));
        rules.put("One Column Less",
                c -> c.completeIf(c.won() && !c.model().getColumnsPlanted().contains(c.intValue())));
        rules.put("Defenseless Row",
                c -> c.completeIf(c.won() && !c.model().getRowsPlanted().contains(c.intValue())));
        rules.put("Defenseless Cross",
                c -> c.completeIf(c.won()
                && !c.model().getColumnsPlanted().contains(c.intValue())
                && !c.model().getRowsPlanted().contains(c.intValue())));
    }

    /** Looks up the quest's rule by base name and applies it (unknown quests are ignored). */
    private static void evaluate(Quest quest, GameModel model, boolean won, Map<String, Integer> progress) {
        QuestRule rule = RULES.get(baseName(quest.getName()));
        if (rule == null) return;
        int target = quest.getProgress() != null ? quest.getProgress().getTargetValue() : 1;
        rule.apply(new QuestContext(quest.getName(), quest.getVariable(), target, model, won, progress));
    }

    // ---- plant list helpers ----

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
