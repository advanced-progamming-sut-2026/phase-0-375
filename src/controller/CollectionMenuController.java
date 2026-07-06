package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CollectionMenuController extends AppMenuController {
    private static CollectionMenuController instance = null;

    /**
     * TODO: PlantRegistry integration.
     * Stub list of all plant names in the game.
     * Replace with PlantRegistry.getInstance().getAllPlantNames() once merged.
     */
    private static final List<String> ALL_PLANT_NAMES = Arrays.asList(
            "Peashooter", "Sunflower", "Wall-nut", "Potato Mine", "Snow Pea",
            "Chomper", "Repeater", "Cherry Bomb", "Tall-nut", "Spikeweed",
            "Spikerock", "Torchwood", "Puff-shroom", "Fume-shroom", "Sun-shroom",
            "Cabbage-pult", "Melon-pult", "Winter Melon", "Kernel-pult", "Popcorn",
            "Jalapeno", "Squash", "Lily Pad", "Tangle Kelp", "Bloomerang",
            "Citron", "E.M.Peach", "Laser Bean", "Starfruit", "Chili Bean",
            "Pea Pod", "Lightning Reed", "Magnifying Grass", "Tile Turnip",
            "Garlic", "Endurian", "Hypno-shroom", "Stunion", "Plantern",
            "Pepper-pult", "Hot Potato", "Gold Leaf"
    );

    /**
     * TODO: ZombieRegistry / ZombieLoader integration.
     * Stub list of all zombie names in the game.
     * Replace once a zombie data registry is available.
     */
    private static final List<String> ALL_ZOMBIE_NAMES = Arrays.asList(
            "Basic Zombie", "Conehead", "Buckethead", "Knight", "Blockhead",
            "Flag Zombie", "Newspaper Zombie", "Screendoor Zombie", "Football Zombie",
            "Gargantuar", "Imp", "Ra Zombie", "Explorer Zombie", "Tombraiser",
            "Dodo Rider", "Hunter Zombie", "Troglobite", "Prospector Zombie",
            "Fisherman Zombie", "Snorkel Zombie", "Octopus Zombie",
            "Jester Zombie", "Wizard Zombie", "King Zombie", "Imp Dragon",
            "Pianist Zombie", "Arcade Zombie", "Parasol Zombie", "Turquoise Zombie",
            "Barrel Roller Zombie", "All Star Zombie"
    );

    private CollectionMenuController() {}

    public static CollectionMenuController getInstance() {
        if (instance == null) instance = new CollectionMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("No menus reachable from collection.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.GAME);
        return CommandResult.success("Returned to game menu.");
    }

    // ── Helper for typed error returns ──

    @SuppressWarnings("unchecked")
    private static <T> CommandResult<T> errorTyped(String message) {
        return (CommandResult<T>) CommandResult.error(message);
    }

    // ── Show commands ──

    /**
     * Returns the list of plant names the user has unlocked.
     */
    public CommandResult<List<String>> showPlants() {
        User user = App.getInstance().getCurrentUser();
        Set<String> unlocked = user.getUnlockedPlants();
        if (unlocked == null || unlocked.isEmpty()) {
            return CommandResult.successWithData("No plants unlocked yet.", new ArrayList<>());
        }
        List<String> sorted = new ArrayList<>(unlocked);
        java.util.Collections.sort(sorted);
        return CommandResult.successWithData("Unlocked plants (" + sorted.size() + "):", sorted);
    }

    /**
     * TODO: PlantRegistry integration.
     * Returns the stub list of all plants.
     */
    public CommandResult<List<String>> showAllPlants() {
        return CommandResult.successWithData("All plants (" + ALL_PLANT_NAMES.size() + "):",
                new ArrayList<>(ALL_PLANT_NAMES));
    }

    /**
     * Returns the list of zombie names the user has encountered (unlocked).
     */
    public CommandResult<List<String>> showZombies() {
        User user = App.getInstance().getCurrentUser();
        Set<String> unlocked = user.getUnlockedZombies();
        if (unlocked == null || unlocked.isEmpty()) {
            return CommandResult.successWithData("No zombies discovered yet.", new ArrayList<>());
        }
        List<String> sorted = new ArrayList<>(unlocked);
        java.util.Collections.sort(sorted);
        return CommandResult.successWithData("Discovered zombies (" + sorted.size() + "):", sorted);
    }

    /**
     * TODO: ZombieRegistry integration.
     * Returns the stub list of all zombies.
     */
    public CommandResult<List<String>> showAllZombies() {
        return CommandResult.successWithData("All zombies (" + ALL_ZOMBIE_NAMES.size() + "):",
                new ArrayList<>(ALL_ZOMBIE_NAMES));
    }

    /**
     * TODO: PlantRegistry integration.
     * Stub: always returns success with the plant name as data.
     * Replace with PlantRegistry.getInstance().getPlant(plantName) once merged.
     */
    public CommandResult<String> showPlant(String plantName) {
        if (!ALL_PLANT_NAMES.contains(plantName)) {
            return errorTyped("Unknown plant: '" + plantName + "'.");
        }
        // Check if the user has unlocked this plant
        User user = App.getInstance().getCurrentUser();
        boolean unlocked = user.getUnlockedPlants() != null && user.getUnlockedPlants().contains(plantName);
        String status = unlocked ? "[UNLOCKED]" : "[LOCKED]";
        return CommandResult.successWithData(plantName + " " + status, plantName);
    }

    /**
     * TODO: ZombieRegistry integration.
     * Stub: always returns success with the zombie name as data.
     * Replace with ZombieLoader/ZombieRegistry lookup once merged.
     */
    public CommandResult<String> showZombie(String zombieName) {
        if (!ALL_ZOMBIE_NAMES.contains(zombieName)) {
            return errorTyped("Unknown zombie: '" + zombieName + "'.");
        }
        User user = App.getInstance().getCurrentUser();
        boolean discovered = user.getUnlockedZombies() != null && user.getUnlockedZombies().contains(zombieName);
        String status = discovered ? "[DISCOVERED]" : "[UNDISCOVERED]";
        return CommandResult.successWithData(zombieName + " " + status, zombieName);
    }

    // ── Action commands ──

    /**
     * TODO: PlantRegistry integration.
     * Stub: uses hardcoded upgrade costs (level 2: 10 seed packets + 500 coins).
     * Replace with real PlantLevels/LevelUpgrade data once PlantRegistry is merged.
     */
    public CommandResult<Void> upgradePlant(String plantName) {
        if (!ALL_PLANT_NAMES.contains(plantName)) {
            return CommandResult.error("Unknown plant: '" + plantName + "'.");
        }
        User user = App.getInstance().getCurrentUser();
        if (user.getUnlockedPlants() == null || !user.getUnlockedPlants().contains(plantName)) {
            return CommandResult.error("Plant '" + plantName + "' is not unlocked yet.");
        }

        // Stub: level 2 costs 10 seed packets + 500 coins
        int seedCost = 10;
        int coinCost = 500;

        int packets = user.getSeedPackets() != null
                ? user.getSeedPackets().getOrDefault(plantName, 0) : 0;
        if (packets < seedCost) {
            return CommandResult.error("Not enough seed packets. Need " + seedCost
                    + ", have " + packets + ".");
        }
        if (user.getCoins() < coinCost) {
            return CommandResult.error("Not enough coins. Need " + coinCost
                    + ", have " + user.getCoins() + ".");
        }

        user.getSeedPackets().put(plantName, packets - seedCost);
        user.setCoins(user.getCoins() - coinCost);
        App.getInstance().getUserRepository().flush();
        return CommandResult.success("'" + plantName + "' upgraded to level 2!");
    }

    public CommandResult<Void> purchasePlant(String plantName) {
        if (!ALL_PLANT_NAMES.contains(plantName)) {
            return CommandResult.error("Unknown plant: '" + plantName + "'.");
        }
        User user = App.getInstance().getCurrentUser();
        if (user.getUnlockedPlants() != null && user.getUnlockedPlants().contains(plantName)) {
            return CommandResult.error("Plant '" + plantName + "' is already unlocked.");
        }

        int cost = 2000;
        if (user.getCoins() < cost) {
            return CommandResult.error("Not enough coins. Need " + cost
                    + ", have " + user.getCoins() + ".");
        }

        user.setCoins(user.getCoins() - cost);
        user.getUnlockedPlants().add(plantName);
        App.getInstance().getUserRepository().flush();
        return CommandResult.success("'" + plantName + "' purchased successfully!");
    }
}
