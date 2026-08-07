package model.quest;

/**
 * Tracks the progress of a quest for a specific player.
 * Holds the current progress value and the target value needed
 * to complete the quest.
 */
public class QuestProgress {
    private int currentValue;
    private int targetValue;

    public QuestProgress(int currentValue, int targetValue) {
        this.currentValue = currentValue;
        this.targetValue = targetValue;
    }

    /**
     * Increase the current progress by the given amount.
     *
     * @param amount the value to add to current progress
     */
    public void increase(int amount) {
        currentValue += amount;
        if (currentValue > targetValue) {
            currentValue = targetValue;
        }
    }

    /**
     * Checks whether the quest is complete.
     *
     * @return true if currentValue >= targetValue
     */
    public boolean isComplete() {
        return currentValue >= targetValue;
    }

    /**
     * Resets the progress to zero (e.g. for daily quests).
     */
    public void reset() {
        currentValue = 0;
    }

    // --- Getters ---

    public int getCurrentValue() {
        return currentValue;
    }

    public int getTargetValue() {
        return targetValue;
    }

    // --- Setters ---

    public void setCurrentValue(int currentValue) {
        this.currentValue = currentValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }
}
