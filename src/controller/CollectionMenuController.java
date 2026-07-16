package controller;

import controller.result.CommandResult;
import model.app.App;
import model.collection.Collection;
import model.enums.ArmorType;
import model.enums.MenuType;
import model.enums.ZombieBehaviorType;
import model.plant.PlantFactory;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;
import model.user.User;
import model.zombie.ZombieFactory;
import model.zombie.definition.Zombie;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CollectionMenuController extends AppMenuController {
    private static final String PLANTS_JSON = "/assets/data/plants/plants.json";
    private static final String ZOMBIES_JSON = "/assets/data/zombies/zombies.json";
    private static final String ARMOR_JSON = "/assets/data/armor/ArmorTypeData.json";
    private static final int PURCHASE_COST_COINS = 2000;

    private static CollectionMenuController instance = null;

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

    // ── Collection wiring ──

    /**
     * Builds a {@link Collection} seeded with the current user's unlocked
     * plants/zombies and every plant/zombie definition known to the game,
     * loading {@link PlantFactory} / {@link ZombieFactory} on demand if
     * they have not been initialized yet (e.g. the player opened the
     * collection menu before ever starting a level).
     */
    private Collection buildCollection(User user) {
        ensureDefinitionsLoaded();
        // old saves may miss these sets; init them so purchases persist
        if (user.getUnlockedPlants() == null) {
            user.setUnlockedPlants(new HashSet<>());
        }
        if (user.getUnlockedZombies() == null) {
            user.setUnlockedZombies(new HashSet<>());
        }
        Collection collection = new Collection(user.getUnlockedPlants(), user.getUnlockedZombies());
        for (Plant plant : safePlantDefinitions()) {
            collection.registerPlant(plant);
        }
        for (Zombie zombie : safeZombieDefinitions()) {
            collection.registerZombie(zombie);
        }
        collection.loadPlantLevels(user.getPlantLevels());
        return collection;
    }

    private void ensureDefinitionsLoaded() {
        try {
            PlantFactory.getAllDefinitions();
        } catch (IllegalStateException notLoaded) {
            tryInitPlants();
        }
        try {
            ZombieFactory.getAllDefinitions();
        } catch (RuntimeException notLoaded) {
            tryInitZombies();
        }
    }

    private void tryInitPlants() {
        try {
            PlantFactory.init(PLANTS_JSON);
        } catch (IOException e) {
            System.err.println("[CollectionMenuController] Could not load plant definitions: " + e.getMessage());
        }
    }

    private void tryInitZombies() {
        try {
            ZombieFactory.init(ZOMBIES_JSON, ARMOR_JSON);
        } catch (IOException e) {
            System.err.println("[CollectionMenuController] Could not load zombie definitions: " + e.getMessage());
        }
    }

    private List<Plant> safePlantDefinitions() {
        try {
            return PlantFactory.getAllDefinitions();
        } catch (IllegalStateException notLoaded) {
            return Collections.emptyList();
        }
    }

    private List<Zombie> safeZombieDefinitions() {
        try {
            return ZombieFactory.getAllDefinitions();
        } catch (RuntimeException notLoaded) {
            return Collections.emptyList();
        }
    }

    private void persistPlantLevel(User user, Collection collection, String plantName) {
        if (user.getPlantLevels() == null) {
            user.setPlantLevels(new HashMap<>());
        }
        user.getPlantLevels().put(plantName, collection.getPlantLevel(plantName));
    }

    // ── Show commands ──

    /** Returns the list of plant names the user has unlocked. */
    public CommandResult<List<String>> showPlants() {
        User user = App.getInstance().getCurrentUser();
        Collection collection = buildCollection(user);
        List<Plant> owned = collection.getOwnedPlants();
        if (owned.isEmpty()) {
            return CommandResult.successWithData("No plants unlocked yet.", new ArrayList<>());
        }
        List<String> lines = new ArrayList<>();
        for (Plant plant : owned) {
            lines.add(plant.getName() + " (Level " + collection.getPlantLevel(plant.getName()) + ")");
        }
        Collections.sort(lines);
        return CommandResult.successWithData("Unlocked plants (" + lines.size() + "):", lines);
    }

    /** Returns the list of all plant names in the game. */
    public CommandResult<List<String>> showAllPlants() {
        User user = App.getInstance().getCurrentUser();
        Collection collection = buildCollection(user);
        List<String> lines = new ArrayList<>();
        for (Plant plant : collection.getAllPlants()) {
            String tag = collection.ownsPlant(plant.getName())
                    ? "[UNLOCKED L" + collection.getPlantLevel(plant.getName()) + "]" : "[LOCKED]";
            lines.add(plant.getName() + " " + tag);
        }
        Collections.sort(lines);
        return CommandResult.successWithData("All plants (" + lines.size() + "):", lines);
    }

    /** Returns the list of zombie names the user has encountered (unlocked). */
    public CommandResult<List<String>> showZombies() {
        User user = App.getInstance().getCurrentUser();
        Collection collection = buildCollection(user);
        List<Zombie> owned = collection.getOwnedZombies();
        if (owned.isEmpty()) {
            return CommandResult.successWithData("No zombies discovered yet.", new ArrayList<>());
        }
        List<String> names = new ArrayList<>();
        for (Zombie zombie : owned) {
            names.add(zombie.getName());
        }
        Collections.sort(names);
        return CommandResult.successWithData("Discovered zombies (" + names.size() + "):", names);
    }

    /** Returns the list of all zombie names in the game. */
    public CommandResult<List<String>> showAllZombies() {
        User user = App.getInstance().getCurrentUser();
        Collection collection = buildCollection(user);
        List<String> lines = new ArrayList<>();
        for (Zombie zombie : collection.getAllZombies()) {
            String tag = collection.ownsZombie(zombie.getName()) ? "[DISCOVERED]" : "[UNDISCOVERED]";
            lines.add(zombie.getName() + " " + tag);
        }
        Collections.sort(lines);
        return CommandResult.successWithData("All zombies (" + lines.size() + "):", lines);
    }

    /** Returns a full text description of one plant. */
    public CommandResult<String> showPlant(String plantName) {
        User user = App.getInstance().getCurrentUser();
        Collection collection = buildCollection(user);
        Plant plant = collection.getPlant(plantName);
        if (plant == null) {
            return errorTyped("Unknown plant: '" + plantName + "'.");
        }
        String details = describePlant(plant, collection);
        return CommandResult.successWithData(details, details);
    }

    /** Returns a full text description of one zombie. */
    public CommandResult<String> showZombie(String zombieName) {
        User user = App.getInstance().getCurrentUser();
        Collection collection = buildCollection(user);
        Zombie zombie = collection.getZombie(zombieName);
        if (zombie == null) {
            return errorTyped("Unknown zombie: '" + zombieName + "'.");
        }
        String details = describeZombie(zombie, collection);
        return CommandResult.successWithData(details, details);
    }

    // ── Formatting helpers ──

    private String describePlant(Plant plant, Collection collection) {
        boolean owned = collection.ownsPlant(plant.getName());
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(plant.getName()).append(" ===\n");
        sb.append("Status: ").append(owned ? "UNLOCKED (Level " + collection.getPlantLevel(plant.getName()) + ")"
                : "LOCKED").append('\n');
        sb.append("Category: ").append(plant.getCategory()).append('\n');
        sb.append("Tags: ").append(plant.getTags()).append('\n');
        sb.append("Cost: ").append(plant.getCost()).append(" sun | Base HP: ").append(plant.getBaseHP())
                .append(" | Damage: ").append(plant.getDamage()).append('\n');
        sb.append("Recharge: ").append(plant.getRechargeTime()).append("s | Action interval: ")
                .append(plant.getActionInterval()).append("s\n");
        sb.append("Ability: ").append(plant.getAbilityType()).append(" (value ")
                .append(plant.getAbilityValue()).append(")\n");
        sb.append("Plant Food: ").append(plant.getPlantFoodType()).append(" (value ")
                .append(plant.getPlantFoodValue()).append(")\n");
        appendUpgradeInfo(sb, plant, collection, owned);
        return sb.toString();
    }

    private void appendUpgradeInfo(StringBuilder sb, Plant plant, Collection collection, boolean owned) {
        if (!owned) {
            return;
        }
        int level = collection.getPlantLevel(plant.getName());
        if (level >= Collection.MAX_PLANT_LEVEL) {
            sb.append("Already at max level.\n");
            return;
        }
        PlantLevels levels = plant.getLevels();
        LevelUpgrade next = levels != null ? levels.getUpgrade(level + 1) : null;
        sb.append("Next upgrade (Level ").append(level + 1).append("): ")
                .append(collection.getUpgradeSeedCost(plant.getName())).append(" seed packets + ")
                .append(collection.getUpgradeCoinCost(plant.getName())).append(" coins");
        if (next != null) {
            sb.append(" -> ").append(next.getType()).append(' ').append(next.getValue());
        }
        sb.append('\n');
    }

    private String describeZombie(Zombie zombie, Collection collection) {
        boolean discovered = collection.ownsZombie(zombie.getName());
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(zombie.getName()).append(" ===\n");
        sb.append("Status: ").append(discovered ? "DISCOVERED" : "UNDISCOVERED").append('\n');
        sb.append("Base HP: ").append(zombie.getBaseHP()).append(" | Speed: ").append(zombie.getSpeed())
                .append(" | Eat DPS: ").append(zombie.getEatDPS()).append('\n');
        sb.append("Size: ").append(zombie.getSize()).append(" | Chapter: ")
                .append(zombie.getChapter() != null ? zombie.getChapter() : "any").append('\n');
        sb.append("Wave point cost: ").append(zombie.getWavePointCost()).append(" | Weight: ")
                .append(zombie.getWeight()).append('\n');
        List<ArmorType> armor = zombie.getArmorTypes();
        sb.append("Armor: ").append(armor == null || armor.isEmpty() ? "none" : armor).append('\n');
        List<ZombieBehaviorType> behaviors = zombie.getBehaviors();
        sb.append("Behaviors: ").append(behaviors == null || behaviors.isEmpty() ? "none" : behaviors).append('\n');
        return sb.toString();
    }

    // ── Action commands ──

    public CommandResult<Void> upgradePlant(String plantName) {
        User user = App.getInstance().getCurrentUser();
        Collection collection = buildCollection(user);
        Plant plant = collection.getPlant(plantName);
        if (plant == null) {
            return CommandResult.error("Unknown plant: '" + plantName + "'.");
        }
        plantName = plant.getName(); // canonical casing
        if (!collection.ownsPlant(plantName)) {
            return CommandResult.error("Plant '" + plantName + "' is not unlocked yet.");
        }
        if (collection.getPlantLevel(plantName) >= Collection.MAX_PLANT_LEVEL) {
            return CommandResult.error("'" + plantName + "' is already at the maximum level.");
        }
        return chargeAndUpgrade(user, collection, plantName);
    }

    private CommandResult<Void> chargeAndUpgrade(User user, Collection collection, String plantName) {
        int seedCost = collection.getUpgradeSeedCost(plantName);
        int coinCost = collection.getUpgradeCoinCost(plantName);

        int packets = user.getSeedPackets() != null
                ? user.getSeedPackets().getOrDefault(plantName, 0) : 0;
        if (packets < seedCost) {
            return CommandResult.error("Not enough seed packets. Need " + seedCost + ", have " + packets + ".");
        }
        if (user.getCoins() < coinCost) {
            return CommandResult.error("Not enough coins. Need " + coinCost + ", have " + user.getCoins() + ".");
        }
        if (!collection.upgradePlant(plantName)) {
            return CommandResult.error("Could not upgrade '" + plantName + "'.");
        }

        user.getSeedPackets().put(plantName, packets - seedCost);
        user.setCoins(user.getCoins() - coinCost);
        persistPlantLevel(user, collection, plantName);
        App.getInstance().getUserRepository().flush();
        return CommandResult.success("'" + plantName + "' upgraded to level "
                + collection.getPlantLevel(plantName) + "!");
    }

    public CommandResult<Void> purchasePlant(String plantName) {
        User user = App.getInstance().getCurrentUser();
        Collection collection = buildCollection(user);
        Plant plant = collection.getPlant(plantName);
        if (plant == null) {
            return CommandResult.error("Unknown plant: '" + plantName + "'.");
        }
        plantName = plant.getName(); // canonical casing
        if (collection.ownsPlant(plantName)) {
            return CommandResult.error("Plant '" + plantName + "' is already unlocked.");
        }
        if (user.getCoins() < PURCHASE_COST_COINS) {
            return CommandResult.error("Not enough coins. Need " + PURCHASE_COST_COINS
                    + ", have " + user.getCoins() + ".");
        }

        user.setCoins(user.getCoins() - PURCHASE_COST_COINS);
        collection.unlockPlant(plantName);
        persistPlantLevel(user, collection, plantName);
        App.getInstance().getUserRepository().flush();
        return CommandResult.success("'" + plantName + "' purchased successfully!");
    }
}
