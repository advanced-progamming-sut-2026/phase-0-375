package model.quest;

import model.enums.QuestCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the player's travel log, which contains all quests
 * organized by category. Handles navigation between pages
 * and tracks which quests are active or completed.
 */
public class TravelLog {
    private List<Quest> quests;
    private QuestCategory currentPage;
    private List<Quest> completedQuests;

    public TravelLog() {
        this.quests = new ArrayList<>();
        this.currentPage = QuestCategory.MAIN;
        this.completedQuests = new ArrayList<>();
    }

    /**
     * Switches to the specified page in the travel log.
     *
     * @param pageName the category page to display
     */
    public void changePage(QuestCategory pageName) {}

    /**
     * Shows quests belonging to the current page, sorted by priority.
     *
     * @return list of quests on the current page
     */
    public List<Quest> showCurrentPage() {
        return Collections.emptyList();
    }

    /**
     * Shows all daily quests.
     *
     * @return list of daily quests
     */
    public List<Quest> showDailyQuests() {
        return Collections.emptyList();
    }

    /**
     * Shows all main (non-daily, non-epic) quests.
     *
     * @return list of main quests
     */
    public List<Quest> showMainQuests() {
        return Collections.emptyList();
    }

    /**
     * Shows all epic quests.
     *
     * @return list of epic quests
     */
    public List<Quest> showEpicQuests() {
        return Collections.emptyList();
    }

    /**
     * Adds a new quest to the travel log.
     *
     * @param quest the quest to add
     */
    public void addQuest(Quest quest) {}

    /**
     * Removes a quest from the travel log.
     *
     * @param quest the quest to remove
     * @return true if the quest was removed
     */
    public boolean removeQuest(Quest quest) {
        return false;
    }

    /**
     * Marks a quest as completed and grants its reward.
     *
     * @param quest the quest to complete
     */
    public void completeQuest(Quest quest) {}

    /**
     * Refreshes daily quests at the start of a new day.
     * Old daily quests are removed and new ones are generated.
     */
    public void refreshDailyQuests() {}

    /**
     * Returns quests sorted by priority (critical first, then high,
     * medium, and low).
     *
     * @return sorted list of quests
     */
    public List<Quest> getSortedByPriority() {
        return Collections.emptyList();
    }

    // --- Getters ---

    public List<Quest> getQuests() {
        return Collections.unmodifiableList(quests);
    }

    public QuestCategory getCurrentPage() {
        return currentPage;
    }

    public List<Quest> getCompletedQuests() {
        return Collections.unmodifiableList(completedQuests);
    }

    // --- Setters ---

    public void setQuests(List<Quest> quests) {
        this.quests = quests;
    }

    public void setCurrentPage(QuestCategory currentPage) {
        this.currentPage = currentPage;
    }

    public void setCompletedQuests(List<Quest> completedQuests) {
        this.completedQuests = completedQuests;
    }
}
