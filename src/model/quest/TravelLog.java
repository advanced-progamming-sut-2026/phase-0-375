package model.quest;

import model.data.quest.QuestLoader;
import model.enums.QuestCategory;
import model.enums.QuestPriority;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private boolean viewingMiniGamePage;

    public TravelLog() {
        this.quests = new ArrayList<>();
        this.currentPage = QuestCategory.MAIN;
        this.completedQuests = new ArrayList<>();
        this.viewingMiniGamePage = false;
    }

    /**
     * Switches to the specified page in the travel log.
     *
     * @param pageName the category page to display
     */
    public void changePage(QuestCategory pageName) {
        this.currentPage = pageName;
    }

    /**
     * Shows quests belonging to the current page, sorted by priority.
     *
     * @return list of quests on the current page
     */
    public List<Quest> showCurrentPage() {
        return sortByPriority(filterByCategory(currentPage));
    }

    /**
     * Shows all daily quests.
     *
     * @return list of daily quests
     */
    public List<Quest> showDailyQuests() {
        return sortByPriority(filterByCategory(QuestCategory.DAILY));
    }

    /**
     * Shows all main (non-daily, non-epic) quests.
     *
     * @return list of main quests
     */
    public List<Quest> showMainQuests() {
        return sortByPriority(filterByCategory(QuestCategory.MAIN));
    }

    /**
     * Shows all epic quests.
     *
     * @return list of epic quests
     */
    public List<Quest> showEpicQuests() {
        return sortByPriority(filterByCategory(QuestCategory.EPIC));
    }

    /**
     * Adds a new quest to the travel log.
     *
     * @param quest the quest to add
     */
    public void addQuest(Quest quest) {
        if (quest != null) {
            quests.add(quest);
        }
    }

    /**
     * Removes a quest from the travel log.
     *
     * @param quest the quest to remove
     * @return true if the quest was removed
     */
    public boolean removeQuest(Quest quest) {
        return quests.remove(quest);
    }

    /**
     * Marks a quest as completed and grants its reward.
     *
     * @param quest the quest to complete
     */
    public void completeQuest(Quest quest) {
        if (quest == null || !quests.contains(quest)) {
            return;
        }
        quest.complete();
        quests.remove(quest);
        completedQuests.add(quest);
    }

    /**
     * Refreshes daily quests at the start of a new day.
     * Old daily quests (active or completed) are removed and new ones
     * are generated from the quest definitions file.
     */
    public void refreshDailyQuests(String questsPath) {
        quests.removeIf(q -> q.getCategory() == QuestCategory.DAILY);
        completedQuests.removeIf(q -> q.getCategory() == QuestCategory.DAILY);
        try {
            List<Quest> allQuests = new QuestLoader().load(questsPath);
            for (Quest q : allQuests) {
                if (q.getCategory() == QuestCategory.DAILY) {
                    quests.add(q);
                }
            }
        } catch (IOException e) {
            System.err.println("[TravelLog] Failed to refresh daily quests: " + e.getMessage());
        }
    }

    /**
     * Returns quests sorted by priority (critical first, then high,
     * medium, and low).
     *
     * @return sorted list of quests
     */
    public List<Quest> getSortedByPriority() {
        return sortByPriority(quests);
    }

    // --- Helpers ---

    private List<Quest> filterByCategory(QuestCategory category) {
        List<Quest> out = new ArrayList<>();
        for (Quest q : quests) {
            if (q.getCategory() == category) {
                out.add(q);
            }
        }
        return out;
    }

    private List<Quest> sortByPriority(List<Quest> source) {
        List<Quest> out = new ArrayList<>(source);
        out.sort(Comparator.comparingInt(q -> priorityRank(q.getPriority())));
        return out;
    }

    private int priorityRank(QuestPriority p) {
        if (p == null) {
            return 99;
        }
        return switch (p) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
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

    public boolean isViewingMiniGamePage() {
        return viewingMiniGamePage;
    }

    public void setViewingMiniGamePage(boolean viewingMiniGamePage) {
        this.viewingMiniGamePage = viewingMiniGamePage;
    }

    public void setCompletedQuests(List<Quest> completedQuests) {
        this.completedQuests = completedQuests;
    }
}
