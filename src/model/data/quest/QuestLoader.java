package model.data.quest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.app.App;
import model.enums.PlantCategory;
import model.enums.QuestCategory;
import model.enums.QuestPriority;
import model.enums.QuestRewardType;
import model.quest.Quest;
import model.quest.QuestProgress;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.quest.QuestReward;
import model.user.User;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Loads quest definitions from {@code quests.json} and builds
 * the corresponding {@link Quest} domain objects.
 *
 * <p>If a quest entry has a non-null {@code variable} field (e.g. "3000-4000-5000"),
 * the loader expands it into multiple Quest instances — one per value — so that
 * each variant becomes a distinct quest the player can complete independently.</p>
 */
public class QuestLoader {

    /**
     * Loads all quest definitions from a classpath resource.
     *
     * @param classpathPath JSON file path on the classpath
     * @return unmodifiable list of quest definitions
     * @throws IOException if the file cannot be read or parsed
     */
    public List<Quest> load(String classpathPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<QuestDataEntry> entries;
        try (InputStream inputStream = openQuestStream(classpathPath)) {
            entries = mapper.readValue(inputStream, new TypeReference<>() {});
        }

        List<Quest> result = new ArrayList<>();
        for (QuestDataEntry entry : entries) {
            List<Quest> quests = buildQuests(entry);
            result.addAll(quests);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Builds one or more Quest objects from a single JSON entry.
     * Dash-separated variables (numeric or not) expand into one quest per value.
     * Known keywords (plant_families, column/row counts) become one
     * randomly-picked variant per day.
     */
    private List<Quest> buildQuests(QuestDataEntry entry) {
        List<Quest> quests = new ArrayList<>();

        QuestCategory category = resolveCategory(entry.getCategory());
        QuestPriority priority = resolvePriority(entry.getPriority());
        QuestReward reward = buildReward(entry);
        String variable = entry.getVariable();

        if (variable == null || variable.isEmpty()) {
            // No variable — single quest instance.
            Quest quest = new Quest(
                    entry.getName(),
                    category,
                    entry.getCondition(),
                    reward,
                    priority,
                    null,
                    new QuestProgress(0, parseTarget(entry.getCondition()))
            );
            quests.add(quest);
        } else {
            String[] parts = variable.split("-");
            boolean allNumeric = true;
            for (String part : parts) {
                try {
                    Integer.parseInt(part.trim());
                } catch (NumberFormatException e) {
                    allNumeric = false;
                    break;
                }
            }

            if (allNumeric) {
                // Numeric variables: create one quest per value.
                for (String part : parts) {
                    int value = Integer.parseInt(part.trim());
                    String questName = entry.getName() + " (" + value + ")";
                    String condition = substitute(entry.getCondition(), String.valueOf(value));

                    QuestReward variantReward = buildVariantReward(entry, value);

                    Quest quest = new Quest(
                            questName,
                            category,
                            condition,
                            variantReward,
                            priority,
                            String.valueOf(value),
                            new QuestProgress(0, Math.max(1, value))
                    );
                    quests.add(quest);
                }
            } else {
                String[] dailyValues = dailyValuesFor(variable);
                if (dailyValues != null) {
                    // Daily-random variable: one variant per day, stable within the day.
                    String value = pickDaily(dailyValues, entry.getName());
                    Quest quest = new Quest(
                            entry.getName() + " (" + value + ")",
                            category,
                            substitute(entry.getCondition(), value),
                            reward,
                            priority,
                            value,
                            new QuestProgress(0, parseTarget(entry.getCondition()))
                    );
                    quests.add(quest);
                } else if (parts.length > 1) {
                    // Non-numeric dash list (e.g. chapter names): one quest per value.
                    for (String part : parts) {
                        String value = part.trim();
                        Quest quest = new Quest(
                                entry.getName() + " (" + value + ")",
                                category,
                                substitute(entry.getCondition(), value),
                                reward,
                                priority,
                                value,
                                new QuestProgress(0, parseTarget(entry.getCondition()))
                        );
                        quests.add(quest);
                    }
                } else {
                    // Unknown variable: single quest with the variable stored as-is.
                    Quest quest = new Quest(
                            entry.getName(),
                            category,
                            entry.getCondition(),
                            reward,
                            priority,
                            variable,
                            new QuestProgress(0, parseTarget(entry.getCondition()))
                    );
                    quests.add(quest);
                }
            }
        }

        return quests;
    }

    private static final int MAP_COLUMNS = 9;
    private static final int MAP_ROWS = 5;

    /** Known daily-random variables and their possible values; null if not one of them. */
    private static String[] dailyValuesFor(String variable) {
        switch (variable.trim()) {
            case "plant_families": {
                PlantCategory[] cats = PlantCategory.values();
                String[] values = new String[cats.length];
                for (int i = 0; i < cats.length; i++) {
                    values[i] = cats[i].name();
                }
                return values;
            }
            case "column_count":
                return numberRange(MAP_COLUMNS);
            case "row_count":
                return numberRange(MAP_ROWS);
            case "min_row_and_column_count":
                return numberRange(Math.min(MAP_ROWS, MAP_COLUMNS));
            case "kill_capable_plant":
                return killCapablePlantNames();
            default:
                return null;
        }
    }

    /** Unlocked damage-dealing plants (falls back to all damage-dealing plants). */
    private static String[] killCapablePlantNames() {
        List<Plant> defs;
        try {
            defs = PlantFactory.getAllDefinitions();
        } catch (IllegalStateException notInitialised) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
                defs = PlantFactory.getAllDefinitions();
            } catch (Exception e) {
                return null;
            }
        }
        User user = App.getInstance().getCurrentUser();
        Set<String> unlocked = user != null ? user.getUnlockedPlants() : null;
        List<String> owned = new ArrayList<>();
        List<String> all = new ArrayList<>();
        for (Plant def : defs) {
            if (def == null || def.getDamage() <= 0) continue;
            all.add(def.getName());
            if (unlocked != null && unlocked.contains(def.getName())) {
                owned.add(def.getName());
            }
        }
        List<String> result = owned.isEmpty() ? all : owned;
        if (result.isEmpty()) {
            return null;
        }
        Collections.sort(result); // stable pick within the day
        return result.toArray(new String[0]);
    }

    /** ["1", "2", ..., max]. */
    private static String[] numberRange(int max) {
        String[] values = new String[max];
        for (int i = 0; i < max; i++) {
            values[i] = String.valueOf(i);
        }
        return values;
    }

    /** Picks today's variant: stable within a day, changes every day. */
    private static String pickDaily(String[] values, String questName) {
        long seed = LocalDate.now().toEpochDay() * 31L + questName.hashCode();
        return values[new Random(seed).nextInt(values.length)];
    }

    /** Replaces known condition placeholders with the variable value. */
    private static String substitute(String condition, String value) {
        return condition
                .replace("sun_amount", value)
                .replace("family_type", value)
                .replace("plant_type", value)
                .replaceAll("\\bchapter\\b", value)
                .replaceAll("\\bn\\b", value);
    }

    /**
     * Opens quests.json, first from the classpath, then from the file system
     * (relative to the working directory). Same fallback as ZombieLoader and
     * LevelRegistry, since the assets folder is not on the classpath.
     */
    private static InputStream openQuestStream(String path) throws IOException {
        InputStream inputStream = QuestLoader.class.getResourceAsStream(path);
        if (inputStream != null) return inputStream;

        String filePath = path.startsWith("/") ? path.substring(1) : path;
        java.io.File file = new java.io.File(filePath);
        if (!file.isFile()) {
            throw new IOException("quests.json resource not found: " + path);
        }
        return new java.io.FileInputStream(file);
    }

    /** Uses the first number in the condition text as the target (default 1). */
    private int parseTarget(String condition) {
        if (condition == null) {
            return 1;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(condition);
        if (m.find()) {
            try {
                return Math.max(1, Integer.parseInt(m.group()));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 1;
    }

    /**
     * Builds a QuestReward from a raw entry, matching the
     * QuestReward(type, coinAmount, gemAmount, unlockableName, inventoryItem, inventoryItemAmount)
     * constructor signature.
     */
    private QuestReward buildReward(QuestDataEntry entry) {
        QuestRewardType type = resolveRewardType(entry.getRewardType());
        return new QuestReward(
                type,
                entry.getRewardCoinAmount(),
                entry.getRewardGemAmount(),
                entry.getRewardUnlockableName(),
                entry.getRewardInventoryItem(),
                entry.getRewardInventoryItemAmount()
        );
    }

    /**
     * Builds a variant QuestReward for numeric variable quests.
     * Handles formula-based rewards using rewardNote:
     *   "sun_amount / 100 coins" → coins = variableValue / 100
     *   "20 - n seed packets"    → inventoryItemAmount = 20 - variableValue
     */
    private QuestReward buildVariantReward(QuestDataEntry entry, int variableValue) {
        QuestRewardType type = resolveRewardType(entry.getRewardType());
        String note = entry.getRewardNote();

        int coins = entry.getRewardCoinAmount();
        int gems = entry.getRewardGemAmount();
        String inventoryItem = entry.getRewardInventoryItem();
        int inventoryItemAmount = entry.getRewardInventoryItemAmount();

        if (note != null && note.contains("sun_amount / 100")) {
            coins = variableValue / 100;
        }
        if (note != null && note.contains("20 - n")) {
            inventoryItemAmount = 20 - variableValue;
        }
        if (note != null && note.contains("n gems")) {
            gems = variableValue;
        }

        return new QuestReward(
                type, coins, gems,
                entry.getRewardUnlockableName(),
                inventoryItem, inventoryItemAmount
        );
    }

    // --- Enum resolvers ---

    private QuestCategory resolveCategory(String raw) {
        if (raw == null) return QuestCategory.DAILY;
        switch (raw.toUpperCase()) {
            case "DAILY":  return QuestCategory.DAILY;
            case "MAIN":   return QuestCategory.MAIN;
            case "EPIC":   return QuestCategory.EPIC;
            default:
                System.err.println("[QuestLoader] Unknown QuestCategory: " + raw);
                return QuestCategory.DAILY;
        }
    }

    private QuestPriority resolvePriority(String raw) {
        if (raw == null) return QuestPriority.MEDIUM;
        switch (raw.toUpperCase()) {
            case "CRITICAL": return QuestPriority.CRITICAL;
            case "HIGH":     return QuestPriority.HIGH;
            case "MEDIUM":   return QuestPriority.MEDIUM;
            case "LOW":      return QuestPriority.LOW;
            default:
                System.err.println("[QuestLoader] Unknown QuestPriority: " + raw);
                return QuestPriority.MEDIUM;
        }
    }

    private QuestRewardType resolveRewardType(String raw) {
        if (raw == null) return QuestRewardType.CURRENCY;
        switch (raw.toUpperCase()) {
            case "CURRENCY":   return QuestRewardType.CURRENCY;
            case "UNLOCKABLE": return QuestRewardType.UNLOCKABLE;
            case "INVENTORY":  return QuestRewardType.INVENTORY;
            default:
                System.err.println("[QuestLoader] Unknown QuestRewardType: " + raw);
                return QuestRewardType.CURRENCY;
        }
    }
}
