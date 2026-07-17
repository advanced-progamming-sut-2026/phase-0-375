package model.data.quest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.enums.QuestCategory;
import model.enums.QuestPriority;
import model.enums.QuestRewardType;
import model.quest.Quest;
import model.quest.QuestProgress;
import model.quest.QuestReward;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * If the entry has a variable with dash-separated numeric values,
     * it is expanded into multiple quests — one per variable value.
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
                    String condition = entry.getCondition()
                            .replace("sun_amount", String.valueOf(value))
                            .replaceAll("\\bn\\b", String.valueOf(value));

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
                // Non-numeric variable (e.g. "each game chapter", "plant_families"):
                // single quest with the variable stored as-is.
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

        return quests;
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
