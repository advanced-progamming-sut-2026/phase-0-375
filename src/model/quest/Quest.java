package model.quest;

import model.enums.QuestCategory;
import model.enums.QuestPriority;

/**
 * A definition of a quest in the game.
 * Each quest has a name, category, completion condition,
 * reward, priority, and optional variable parameters.
 */
public class Quest {
    private String name;
    private QuestCategory category;
    private String conditionDescription;
    private QuestReward reward;
    private QuestPriority priority;
    private String variable;
    private QuestProgress progress;

    public Quest(String name, QuestCategory category, String conditionDescription,
                 QuestReward reward, QuestPriority priority, String variable,
                 QuestProgress progress) {
        this.name = name;
        this.category = category;
        this.conditionDescription = conditionDescription;
        this.reward = reward;
        this.priority = priority;
        this.variable = variable;
        this.progress = progress;
    }

    /**
     * Updates the progress of this quest based on a game event.
     *
     * @param progressValue the amount to add to current progress
     */
    public void updateProgress(int progressValue) {}

    /**
     * Checks whether this quest's completion condition is met.
     *
     * @return true if the quest is complete
     */
    public boolean checkCompletion() {
        return false;
    }

    /**
     * Grants the quest reward to the player if the quest is complete.
     */
    public void complete() {}

    /**
     * Returns the current progress amount.
     *
     * @return the current progress value
     */
    public int getProgressAmount() {
        return progress != null ? progress.getCurrentValue() : 0;
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public QuestCategory getCategory() {
        return category;
    }

    public String getConditionDescription() {
        return conditionDescription;
    }

    public QuestReward getReward() {
        return reward;
    }

    public QuestPriority getPriority() {
        return priority;
    }

    public String getVariable() {
        return variable;
    }

    public QuestProgress getProgress() {
        return progress;
    }

    // --- Setters ---

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(QuestCategory category) {
        this.category = category;
    }

    public void setConditionDescription(String conditionDescription) {
        this.conditionDescription = conditionDescription;
    }

    public void setReward(QuestReward reward) {
        this.reward = reward;
    }

    public void setPriority(QuestPriority priority) {
        this.priority = priority;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public void setProgress(QuestProgress progress) {
        this.progress = progress;
    }
}
