package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.PlantCategory;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.LevelConfig;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.user.User;
import model.zombie.ZombieFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlantSelectionMenuController extends AppMenuController {
    private static final String PLANTS_JSON = "/assets/data/plants/plants.json";

    private static PlantSelectionMenuController instance = null;

    private static final int DEFAULT_MAX_SELECTION = 8;

    private PlantSelectionMenuController() {
        ensureDefinitionsLoaded();
    }

    private static List<String> getPlantsNames() {
        return PlantFactory.getAllDefinitions()
                .stream()
                .map(Plant::getName)
                .collect(Collectors.toList());
    }

    public static PlantSelectionMenuController getInstance() {
        if (instance == null) instance = new PlantSelectionMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("No menus reachable from plant selection.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.GAME);
        return CommandResult.success("Returned to game menu.");
    }

    public CommandResult<List<String>> showAllPlants() {
        return CommandResult.successWithData("All plants:", new ArrayList<>(getPlantsNames()));
    }

    private void ensureDefinitionsLoaded() {
        try {
            PlantFactory.getAllDefinitions();
        } catch (IllegalStateException notLoaded) {
            tryInitPlants();
        }
    }

    private void tryInitPlants() {
        try {
            PlantFactory.init(PLANTS_JSON);
        } catch (IOException e) {
            System.err.println("[PlantSelectionMenuController] Could not load plant definitions: " + e.getMessage());
        }
    }

    public CommandResult<List<String>> showAvailablePlants() {
        User user = App.getInstance().getCurrentUser();
        List<String> available = new ArrayList<>();
        if (user.getUnlockedPlants() != null) {
            for (String name : getPlantsNames()) {
                if (user.getUnlockedPlants().contains(name)) {
                    available.add(name);
                }
            }
        }
        return CommandResult.successWithData("Available plants (" + available.size() + "):", available);
    }

    public CommandResult<Void> addPlant(String type) {
        if (!getPlantsNames().contains(type)) {
            return CommandResult.error("Unknown plant: '" + type + "'.");
        }
        User user = App.getInstance().getCurrentUser();
        if (user.getUnlockedPlants() == null || !user.getUnlockedPlants().contains(type)) {
            return CommandResult.error("Plant '" + type + "' is not unlocked.");
        }

        GameModel model = App.getInstance().getCurrentGameModel();
        if (!plantChoiceAllowed(model)) {
            return CommandResult.error("Plant selection is locked for this level.");
        }
        String familyLockError = familyLockError(model, type);
        if (familyLockError != null) {
            return CommandResult.error(familyLockError);
        }
        if (sunProducerBanned(model, type)) {
            return CommandResult.error("Sun-producing plants are not allowed in this level.");
        }

        List<String> selected = model.getSelectedPlants();
        if (selected == null) {
            selected = new ArrayList<>();
            model.setSelectedPlants(selected);
        }

        if (selected.contains(type)) {
            return CommandResult.error("'" + type + "' is already selected.");
        }
        if (selected.size() >= DEFAULT_MAX_SELECTION) {
            return CommandResult.error("Maximum " + DEFAULT_MAX_SELECTION + " plants already selected.");
        }

        selected.add(type);
        model.setImitaterCopyTarget(type);
        return CommandResult.success("'" + type + "' added to selection.");
    }

    public CommandResult<Void> removePlant(String type) {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (!plantChoiceAllowed(model)) {
            return CommandResult.error("Plant selection is locked for this level.");
        }

        List<String> selected = model.getSelectedPlants();
        if (selected == null || !selected.contains(type)) {
            return CommandResult.error("'" + type + "' is not selected.");
        }
        selected.remove(type);
        return CommandResult.success("'" + type + "' removed from selection.");
    }

    public CommandResult<Void> boostPlant(String type) {
        if (!getPlantsNames().contains(type)) {
            return CommandResult.error("Unknown plant: '" + type + "'.");
        }
        User user = App.getInstance().getCurrentUser();
        if (user.getUnlockedPlants() == null || !user.getUnlockedPlants().contains(type)) {
            return CommandResult.error("Plant '" + type + "' is not unlocked.");
        }
        if (user.getGems() < 2) {
            return CommandResult.error("Need 2 gems, have " + user.getGems() + ".");
        }

        if (user.getPlantBoosts() == null) {
            user.setPlantBoosts(new HashMap<>());
        }
        var repo = App.getInstance().getUserRepository();
        if (repo == null || !repo.spendGems(user.getUsername(), 2)) {
            return CommandResult.error("Need 2 gems, have " + user.getGems() + ".");
        }
        repo.storePlantBoost(user.getUsername(), type);
        return CommandResult.success("'" + type + "' boosted for this level!");
    }

    /** Error when adding this plant would give a restricted family a second pick, else null. */
    private static String familyLockError(GameModel model, String type) {
        if (model == null || model.getCurrentLevel() == null || model.getCurrentLevel().getConfig() == null) {
            return null;
        }
        LevelConfig config = model.getCurrentLevel().getConfig();
        boolean allRestricted = config.isAllFamiliesRestricted();
        Set<String> restrictedFamilies = config.getRestrictedFamilies();
        if (!allRestricted && (restrictedFamilies == null || restrictedFamilies.isEmpty())) {
            return null;
        }
        try {
            PlantCategory family = familyOf(type);
            if (family == null) {
                return null;
            }
            if (!allRestricted && !restrictedFamilies.contains(family.name())) {
                return null; // this family is not restricted
            }
            List<String> selected = model.getSelectedPlants();
            if (selected == null) {
                return null;
            }
            for (String selectedName : selected) {
                if (family == familyOf(selectedName)) {
                    return "Only one plant from the " + family.name() + " family is allowed; '"
                            + selectedName + "' is already picked.";
                }
            }
        } catch (IllegalStateException factoryNotReady) {
            return null; // fail-open: don't block selection if definitions are unavailable
        }
        return null;
    }

    /** True when the level bans sun-producing plants and this plant is one. */
    private static boolean sunProducerBanned(GameModel model, String type) {
        if (model == null || model.getCurrentLevel() == null || model.getCurrentLevel().getConfig() == null
                || model.getCurrentLevel().getConfig().getRules() == null
                || model.getCurrentLevel().getConfig().getRules().isSunProducingPlantsAllowed()) {
            return false;
        }
        try {
            return familyOf(type) == PlantCategory.SUN_PRODUCER;
        } catch (IllegalStateException factoryNotReady) {
            return false; // fail-open: don't block selection if definitions are unavailable
        }
    }

    /** The plant's family, or null when unknown. */
    private static PlantCategory familyOf(String plantName) {
        if (plantName == null || !PlantFactory.hasDefinition(plantName)) {
            return null;
        }
        return PlantFactory.getDefinition(plantName).getCategory();
    }

    /** Whether the current level lets the player edit the seed selection. */
    private static boolean plantChoiceAllowed(GameModel model) {
        return model == null
                || model.getCurrentLevel() == null
                || model.getCurrentLevel().getConfig() == null
                || model.getCurrentLevel().getConfig().getRules() == null
                || model.getCurrentLevel().getConfig().getRules().isAllowsChoosingPlants();
    }

    public CommandResult<Void> startGame() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return CommandResult.error("No level loaded. Enter a chapter first.");
        }

        List<String> selected = model.getSelectedPlants();
        if (plantChoiceAllowed(model) && (selected == null || selected.isEmpty())) {
            return CommandResult.error("Select at least one plant before starting.");
        }
        if (selected == null) {
            model.setSelectedPlants(new ArrayList<>());
        }

        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop == null) {
            loop = new PvZGameLoop(model);
            App.getInstance().setCurrentGameLoop(loop);
        }
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);
        return CommandResult.success("Game started!");
    }
}
