package view.gui.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.PlantSelectionMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.level.Level;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.user.User;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.theme.AdventureTheme;
import view.gui.ui.ResourceBar;
import view.gui.ui.SelectableMenuCard;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-game plant picker. Uses {@link PlantSelectionMenuController}; plant icons via
 * {@link AdventureTheme#plantIcon(String)} when assets are wired later.
 */
public final class PlantSelectionScreen extends AbstractMenuScreen {
    private static final int MAX_SLOTS = 8;

    private final PlantSelectionMenuController controller = PlantSelectionMenuController.getInstance();
    private final Chapter returnChapter;

    private Label selectedLabel;
    private Table availableList;
    private boolean allowsChoosing;

    public PlantSelectionScreen(PvzGdxGame game, Chapter returnChapter) {
        super(game);
        this.returnChapter = returnChapter;
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.PLANT_SELECTION);

        Table top = new Table();
        top.setFillParent(true);
        top.top();
        top.add(new ResourceBar(skin)).expandX().right().pad(12f);
        stage.addActor(top);

        allowsChoosing = plantChoiceAllowed();

        BorderedTable card = new BorderedTable();
        card.pad(24f);
        card.add(new Label("Choose your plants", skin, "big")).padBottom(8f).row();

        Level level = currentLevel();
        String mission = level == null
                ? "Prepare your loadout"
                : level.getConfig().getChapter() + " · Level " + level.getConfig().getLevelId()
                + " · " + level.getConfig().getLevelType();
        card.add(new Label(mission, skin, "secondary")).padBottom(12f).row();

        selectedLabel = new Label("", skin, "medium");
        selectedLabel.setWrap(true);
        card.add(selectedLabel).growX().padBottom(12f).row();

        if (!allowsChoosing) {
            card.add(new Label(
                    "This level picks plants for you (conveyor / locked set). Press Let's Rock to continue.",
                    skin,
                    "secondary")).growX().padBottom(16f).row();
        } else {
            availableList = new Table();
            rebuildAvailableList();
            ScrollPane plantScroll = new ScrollPane(availableList, skin);
            plantScroll.setFadeScrollBars(false);
            plantScroll.setScrollingDisabled(true, false);
            card.add(plantScroll).growX().height(420f).padBottom(12f).row();
        }

        TextButton start = new TextButton("Let's Rock", skin);
        start.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.startGame();
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(openGameplay(game));
                }
            }
        });
        card.add(start).width(260f).height(56f).padBottom(8f).row();

        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuExit();
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    clearTransientGame();
                    if (returnChapter != null) {
                        game.setScreen(new ChapterLevelsScreen(game, returnChapter));
                    } else {
                        game.setScreen(new AdventureScreen(game));
                    }
                }
            }
        });
        card.add(back).width(220f).height(52f);

        ScrollPane scroll = new ScrollPane(card, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        Table root = new Table();
        root.setFillParent(true);
        root.add(scroll).width(760f).maxHeight(UI_HEIGHT - 64f);
        stage.addActor(root);

        refreshSelectedLabel();
    }

    private void rebuildAvailableList() {
        availableList.clearChildren();
        CommandResult<List<String>> available = controller.showAvailablePlants();
        List<String> names = available.getData() == null ? List.of() : available.getData();
        GameModel model = App.getInstance().getCurrentGameModel();
        List<String> selected = model == null || model.getSelectedPlants() == null
                ? List.of()
                : model.getSelectedPlants();

        User user = App.getInstance().getCurrentUser();
        for (String name : names) {
            boolean isSelected = selected.contains(name);
            int cost = plantCost(name);
            boolean boosted = user != null && user.getPlantBoosts() != null
                    && Boolean.TRUE.equals(user.getPlantBoosts().get(name));
            String subtitle = "Cost " + cost + (boosted ? " · Boosted" : "");
            String action = isSelected ? "Remove" : "Add";
            SelectableMenuCard row = new SelectableMenuCard(skin, name, subtitle, action);
            row.setArt(AdventureTheme.get().plantIcon(name));
            final String plantName = name;
            final boolean selectedNow = isSelected;
            row.onAction(() -> {
                CommandResult<Void> r = selectedNow
                        ? controller.removePlant(plantName)
                        : controller.addPlant(plantName);
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    rebuildAvailableList();
                    refreshSelectedLabel();
                }
            });
            availableList.add(row).growX().padBottom(8f).row();

            if (!isSelected) {
                TextButton boost = new TextButton("Boost (2 gems)", skin, "brown");
                boost.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        CommandResult<Void> r = controller.boostPlant(plantName);
                        showToast(r.getMessage(), !r.isSuccess());
                        if (r.isSuccess()) {
                            rebuildAvailableList();
                            refreshSelectedLabel();
                        }
                    }
                });
                availableList.add(boost).width(200f).height(40f).right().padBottom(12f).row();
            }
        }
        if (names.isEmpty()) {
            availableList.add(new Label("No unlocked plants available.", skin, "medium")).row();
        }
    }

    private void refreshSelectedLabel() {
        GameModel model = App.getInstance().getCurrentGameModel();
        List<String> selected = model == null || model.getSelectedPlants() == null
                ? new ArrayList<>()
                : new ArrayList<>(model.getSelectedPlants());
        int empty = Math.max(0, MAX_SLOTS - selected.size());
        StringBuilder sb = new StringBuilder("Selected (" + selected.size() + "/" + MAX_SLOTS + "): ");
        if (selected.isEmpty()) {
            sb.append(allowsChoosing ? "none" : "level-controlled");
        } else {
            sb.append(String.join(", ", selected));
        }
        if (allowsChoosing && empty > 0) {
            sb.append(" · ").append(empty).append(" empty slot(s)");
        }
        selectedLabel.setText(sb.toString());
    }

    private static int plantCost(String name) {
        try {
            if (!PlantFactory.hasDefinition(name)) {
                return 0;
            }
            Plant plant = PlantFactory.getDefinition(name);
            return plant.getCost();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    private static boolean plantChoiceAllowed() {
        GameModel model = App.getInstance().getCurrentGameModel();
        return model == null
                || model.getCurrentLevel() == null
                || model.getCurrentLevel().getConfig() == null
                || model.getCurrentLevel().getConfig().getRules() == null
                || model.getCurrentLevel().getConfig().getRules().isAllowsChoosingPlants();
    }

    private static Level currentLevel() {
        GameModel model = App.getInstance().getCurrentGameModel();
        return model == null ? null : model.getCurrentLevel();
    }

    private static Screen openGameplay(PvzGdxGame game) {
        Level level = currentLevel();
        Chapter chapter = level == null || level.getConfig() == null
                ? null
                : level.getConfig().getChapter();
        if (LawnBackgroundRenderer.Style.forChapter(chapter) != LawnBackgroundRenderer.Style.FRONT_LAWN) {
            return new GameplayScreen(game);
        }
        return new GameplayStubScreen(game);
    }

    private static void clearTransientGame() {
        App app = App.getInstance();
        app.setCurrentGameModel(null);
        app.setCurrentGameLoop(null);
    }
}
