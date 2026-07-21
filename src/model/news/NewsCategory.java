package model.news;

public enum NewsCategory {
    PLANT_UNLOCKED("Plant Unlocked"),
    ZOMBIE_UNLOCKED("Zombie Discovered"),
    MINIGAME_UNLOCKED("Mini-Game Unlocked"),
    LEVEL_UNLOCKED("Level Unlocked");

    private final String label;

    NewsCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}