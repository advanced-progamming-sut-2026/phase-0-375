package model.data.quest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw DTO that mirrors one entry in {@code quests.json}.
 * Used by {@link QuestLoader} to deserialize quest data before
 * converting it to domain {@link model.quest.Quest} objects.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestDataEntry {

    @JsonProperty("name")
    private String name;

    /** DAILY | MAIN | EPIC — maps to {@link model.enums.QuestCategory}. */
    @JsonProperty("category")
    private String category;

    @JsonProperty("condition")
    private String condition;

    /** CURRENCY | UNLOCKABLE | INVENTORY — maps to {@link model.enums.QuestRewardType}. */
    @JsonProperty("rewardType")
    private String rewardType;

    @JsonProperty("rewardCoinAmount")
    private int rewardCoinAmount;

    @JsonProperty("rewardGemAmount")
    private int rewardGemAmount;

    /** Name of the unlockable entity (plant, chapter, etc.), or null. */
    @JsonProperty("rewardUnlockableName")
    private String rewardUnlockableName;

    /** Name of the inventory item (e.g. "seed_packet"), or null. */
    @JsonProperty("rewardInventoryItem")
    private String rewardInventoryItem;

    /** Amount of the inventory item granted. */
    @JsonProperty("rewardInventoryItemAmount")
    private int rewardInventoryItemAmount;

    /** CRITICAL | HIGH | MEDIUM | LOW — maps to {@link model.enums.QuestPriority}. */
    @JsonProperty("priority")
    private String priority;

    /**
     * Variable parameters for this quest, e.g. "3000-4000-5000" means
     * the quest is instantiated 3 times with sun_amount = 3000, 4000, 5000.
     * Null or empty means the quest has no variable instances.
     */
    @JsonProperty("variable")
    private String variable;

    /**
     * Optional note describing the reward formula (e.g. "sun_amount / 100 coins",
     * "20 - n seed packets"). Not stored in QuestReward — only used by the loader
     * to compute concrete reward values for variable quests.
     */
    @JsonProperty("rewardNote")
    private String rewardNote;

    // --- Getters ---

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getCondition() { return condition; }
    public String getRewardType() { return rewardType; }
    public int getRewardCoinAmount() { return rewardCoinAmount; }
    public int getRewardGemAmount() { return rewardGemAmount; }
    public String getRewardUnlockableName() { return rewardUnlockableName; }
    public String getRewardInventoryItem() { return rewardInventoryItem; }
    public int getRewardInventoryItemAmount() { return rewardInventoryItemAmount; }
    public String getPriority() { return priority; }
    public String getVariable() { return variable; }
    public String getRewardNote() { return rewardNote; }

    // --- Setters ---

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setRewardType(String rewardType) { this.rewardType = rewardType; }
    public void setRewardCoinAmount(int rewardCoinAmount) { this.rewardCoinAmount = rewardCoinAmount; }
    public void setRewardGemAmount(int rewardGemAmount) { this.rewardGemAmount = rewardGemAmount; }
    public void setRewardUnlockableName(String rewardUnlockableName) { this.rewardUnlockableName = rewardUnlockableName; }
    public void setRewardInventoryItem(String rewardInventoryItem) { this.rewardInventoryItem = rewardInventoryItem; }
    public void setRewardInventoryItemAmount(int rewardInventoryItemAmount) { this.rewardInventoryItemAmount = rewardInventoryItemAmount; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setVariable(String variable) { this.variable = variable; }
    public void setRewardNote(String rewardNote) { this.rewardNote = rewardNote; }
}