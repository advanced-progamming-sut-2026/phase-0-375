package view.gui.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.CollectionMenuController;
import controller.PlantSelectionMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.minigame.MiniGameLevel;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.user.User;
import view.gui.PvzGdxGame;
import view.gui.anim.SpritesheetClipCache;
import view.gui.assets.SheetPacketPortraits;
import view.gui.audio.GameAudio;
import view.gui.audio.MusicTracks;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.WaterUnderlayerRenderer;
import view.gui.ui.PlantChooserPanel;
import view.gui.ui.ResourceBar;
import view.gui.ui.SeedPacketActor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pre-game plant picker over the chapter lawn. Seed packets via {@code UI_SeedPackets}.
 */
public final class PlantSelectionScreen extends AbstractGameplayScreen {
    private static final int MAX_SLOTS = 8;

    private final PlantSelectionMenuController controller = PlantSelectionMenuController.getInstance();
    private final CollectionMenuController collection = CollectionMenuController.getInstance();
    private final Chapter returnChapter;
    private final LawnBackgroundRenderer lawnBackground;
    private final WaterUnderlayerRenderer waterUnderlayer;
    private final boolean allowsChoosing;

    private Table slotColumn;
    private PlantChooserPanel chooser;
    private ResourceBar resourceBar;
    private SpritesheetClipCache sheetClips;
    private String inspected;

    public PlantSelectionScreen(PvzGdxGame game, Chapter returnChapter) {
        super(game);
        this.returnChapter = returnChapter;
        App.getInstance().setCurrentMenu(MenuType.PLANT_SELECTION);
        assets.textures.loadSync("UI_SeedPackets_768");
        assets.textures.loadSync("ATLASIMAGE_ATLAS_UI_SEEDPACKETS_768_00");
        assets.textures.loadSync("UI_AlwaysLoaded_Uncompressed_768");

        Chapter chapter = currentChapter();
        lawnBackground = new LawnBackgroundRenderer(
                assets.textures, LawnBackgroundRenderer.Style.forChapter(chapter));
        lawnBackground.ensureLoaded();
        waterUnderlayer = chapter == Chapter.BIG_WAVE_BEACH
                ? new WaterUnderlayerRenderer(assets, lawnLayout())
                : null;
        allowsChoosing = plantChoiceAllowed();
        if (assets != null && assets.root != null) {
            sheetClips = new SpritesheetClipCache(assets.root);
        }
        GameAudio.get().play(MusicTracks.chooseSeeds(chapter));
        buildHud();
        refreshPackets();
    }

    private void buildHud() {
        addResourceHud();
        addSlotColumn();
        addChooserOrNote();
        addBottomBar();
        toast.toFront();
    }

    private void addResourceHud() {
        Table topRight = new Table();
        topRight.setFillParent(true);
        topRight.setTouchable(Touchable.childrenOnly);
        topRight.top().right().pad(12f);
        resourceBar = new ResourceBar(skin, game.assets != null ? game.assets.textures : null);
        topRight.add(resourceBar);
        uiStage.addActor(topRight);
    }

    private void addSlotColumn() {
        Table left = new Table();
        left.setFillParent(true);
        left.setTouchable(Touchable.childrenOnly);
        left.top().left().pad(16f);
        slotColumn = new Table();
        left.add(slotColumn);
        uiStage.addActor(left);
    }

    private void addChooserOrNote() {
        Table mid = new Table();
        mid.setFillParent(true);
        mid.setTouchable(Touchable.childrenOnly);
        mid.top().left().padLeft(16f + SeedPacketActor.PACKET_WIDTH + 16f).padTop(8f).padBottom(72f);
        if (allowsChoosing) {
            chooser = new PlantChooserPanel(skin, assets, chooserListener());
            mid.add(chooser).width(680f).growY();
        } else {
            Label note = new Label(
                    "This level picks plants for you. Press Let's Rock to continue.",
                    skin, "secondary");
            note.setWrap(true);
            mid.add(note).width(420f);
        }
        uiStage.addActor(mid);
    }

    private PlantChooserPanel.Listener chooserListener() {
        return new PlantChooserPanel.Listener() {
            @Override
            public void onToggle(String plantName, boolean locked) {
                inspected = plantName;
                if (!locked) {
                    toggle(plantName, selectedPlants().contains(plantName));
                } else {
                    refreshPackets();
                }
            }

            @Override
            public void onUpgrade(String plantName) {
                CommandResult<Void> r = collection.upgradePlant(plantName);
                showPurchaseResult(r);
                if (r.isSuccess()) {
                    refreshPackets();
                }
            }

            @Override
            public void onBoost(String plantName) {
                CommandResult<Void> r = controller.boostPlant(plantName);
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    refreshPackets();
                }
            }
        };
    }

    private void addBottomBar() {
        Table bottom = new Table();
        bottom.setFillParent(true);
        bottom.setTouchable(Touchable.childrenOnly);
        bottom.bottom().pad(16f);
        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(change(this::goBack));
        TextButton start = new TextButton("Let's Rock", skin);
        start.addListener(change(this::startGame));
        bottom.add(back).width(180f).height(48f).left().expandX();
        bottom.add(start).width(240f).height(52f).right();
        uiStage.addActor(bottom);
    }

    private ChangeListener change(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        };
    }

    private void goBack() {
        Level level = currentLevel();
        boolean isMini = level instanceof MiniGameLevel;
        CommandResult<Void> r = controller.menuExit();
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            clearTransientGame();
            if (isMini) {
                App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);
                game.setScreen(new QuestsScreen(game, QuestsScreen.Tab.MINI_GAMES));
            } else if (returnChapter != null) {
                game.setScreen(new ChapterLevelsScreen(game, returnChapter));
            } else {
                game.setScreen(new AdventureScreen(game));
            }
        }
    }

    private void startGame() {
        CommandResult<Void> r = controller.startGame();
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(openGameplay(game));
        }
    }

    @Override
    protected void onBack() {
        goBack();
    }

    @Override
    protected void onConfirm() {
        startGame();
    }

    private void refreshPackets() {
        slotColumn.clearChildren();
        List<String> selected = selectedPlants();
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i < selected.size()) {
                String name = selected.get(i);
                SeedPacketActor packet = packet(name, false, false);
                packet.onClick(() -> toggle(name, true));
                slotColumn.add(packet).size(SeedPacketActor.PACKET_WIDTH, SeedPacketActor.PACKET_HEIGHT)
                        .padBottom(6f).row();
            } else {
                slotColumn.add(SeedPacketActor.empty(assets.textures, skin))
                        .size(SeedPacketActor.PACKET_WIDTH, SeedPacketActor.PACKET_HEIGHT)
                        .padBottom(6f).row();
            }
        }

        if (!allowsChoosing || chooser == null) {
            if (resourceBar != null) {
                resourceBar.refresh();
            }
            return;
        }

        List<String> names = allPlantNames();
        if (inspected == null && !names.isEmpty()) {
            inspected = firstUnlocked(names);
        }
        List<SeedPacketActor> cards = new ArrayList<>();
        for (String name : names) {
            boolean locked = !unlocked(name);
            boolean picked = selected.contains(name);
            SeedPacketActor packet = packet(name, picked && !locked, locked);
            packet.setInspected(name.equals(inspected));
            packet.onClick(() -> {
                inspected = name;
                if (locked) {
                    refreshPackets();
                } else {
                    toggle(name, picked);
                }
            });
            cards.add(packet);
        }
        chooser.setGrid(cards);
        chooser.inspect(inspected, unlocked(inspected), boosted(inspected), plantLevel(inspected));
        if (resourceBar != null) {
            resourceBar.refresh();
        }
    }

    private SeedPacketActor packet(String name, boolean dimmed, boolean locked) {
        SeedPacketActor packet = new SeedPacketActor(
                assets.textures, skin, name, plantCost(name), plantLevel(name), boosted(name), locked);
        SheetPacketPortraits.applyIfNeeded(packet, name, assets, sheetClips);
        packet.setDimmed(dimmed);
        return packet;
    }

    private void toggle(String name, boolean currentlySelected) {
        CommandResult<Void> r = currentlySelected
                ? controller.removePlant(name)
                : controller.addPlant(name);
        showToast(r.getMessage(), !r.isSuccess());
        refreshPackets();
    }

    @Override
    protected void updateLogic(float delta) {}

    @Override
    protected void renderWorld(float delta) {
        lawnBackground.draw(game.batch);
        if (waterUnderlayer != null) {
            waterUnderlayer.draw(game.batch, App.getInstance().getCurrentGameModel(), delta);
        }
    }

    private static LawnLayout lawnLayout() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int rows = model != null ? model.getMap().getRows() : LawnLayout.DEFAULT_ROWS;
        int cols = model != null ? model.getMap().getCols() : LawnLayout.DEFAULT_COLS;
        return new LawnLayout(rows, cols);
    }

    private static List<String> selectedPlants() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null || model.getSelectedPlants() == null) {
            return List.of();
        }
        return model.getSelectedPlants();
    }

    private static List<String> allPlantNames() {
        try {
            List<String> names = new ArrayList<>();
            for (Plant plant : PlantFactory.getAllDefinitions()) {
                names.add(plant.getName());
            }
            return names;
        } catch (IllegalStateException e) {
            return List.of();
        }
    }

    private static String firstUnlocked(List<String> names) {
        for (String name : names) {
            if (unlocked(name)) {
                return name;
            }
        }
        return names.isEmpty() ? null : names.get(0);
    }

    private static boolean unlocked(String name) {
        if (name == null) {
            return false;
        }
        User user = App.getInstance().getCurrentUser();
        return user != null && user.getUnlockedPlants() != null && user.getUnlockedPlants().contains(name);
    }

    private static boolean boosted(String name) {
        if (name == null) {
            return false;
        }
        User user = App.getInstance().getCurrentUser();
        Map<String, Boolean> boosts = user == null ? null : user.getPlantBoosts();
        return boosts != null && Boolean.TRUE.equals(boosts.get(name));
    }

    private static int plantCost(String name) {
        try {
            if (!PlantFactory.hasDefinition(name)) {
                return 0;
            }
            return PlantFactory.getDefinition(name).getCost();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    private static int plantLevel(String name) {
        if (name == null) {
            return 1;
        }
        User user = App.getInstance().getCurrentUser();
        Map<String, Integer> levels = user == null ? null : user.getPlantLevels();
        if (levels == null) {
            return 1;
        }
        Integer level = levels.get(name);
        return level == null || level < 1 ? 1 : level;
    }

    private static boolean plantChoiceAllowed() {
        GameModel model = App.getInstance().getCurrentGameModel();
        return model == null
                || model.getCurrentLevel() == null
                || model.getCurrentLevel().getConfig() == null
                || model.getCurrentLevel().getConfig().getRules() == null
                || model.getCurrentLevel().getConfig().getRules().isAllowsChoosingPlants();
    }

    private static Chapter currentChapter() {
        Level level = currentLevel();
        return level == null || level.getConfig() == null ? null : level.getConfig().getChapter();
    }

    private static Level currentLevel() {
        GameModel model = App.getInstance().getCurrentGameModel();
        return model == null ? null : model.getCurrentLevel();
    }

    private static Screen openGameplay(PvzGdxGame game) {
        return LevelObjectivesScreen.openGameplay(game);
    }

    private static void clearTransientGame() {
        App app = App.getInstance();
        app.setCurrentGameModel(null);
        app.setCurrentGameLoop(null);
    }

    @Override
    public void dispose() {
        if (chooser != null) {
            chooser.dispose();
        }
        if (sheetClips != null) {
            sheetClips.dispose();
            sheetClips = null;
        }
        super.dispose();
    }
}
