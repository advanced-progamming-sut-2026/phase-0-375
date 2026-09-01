package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import controller.GameplayMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.GameState;
import model.enums.MenuType;
import model.enums.SunType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.core.SandstormSpawn;
import model.game.map.Cell;
import model.game.map.WaterBand;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.wave.Wave;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.zombie.ZombieFactory;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.assets.ZombiePamAliases;
import view.gui.lawn.DebugEntityOverlay;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnEntityRenderer;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.WaterUnderlayerRenderer;
import view.gui.ui.SunHud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Debug FrontLawn playground: tool panel + free plant/zombie placement, no win/lose.
 */
public final class DebugPlaygroundScreen extends AbstractGameplayScreen {
    private enum Tool { PLANT, ZOMBIE, ICED, LOW_TIDE, SHOVEL, FEED, COLLECT_SUN }

    private static final Color HOVER_PLANT = new Color(0.35f, 1f, 0.45f, 0.35f);
    private static final Color HOVER_ZOMBIE = new Color(1f, 0.35f, 0.35f, 0.35f);
    private static final Color HOVER_ICED = new Color(0.45f, 0.85f, 1f, 0.35f);
    private static final Color HOVER_LOW_TIDE = new Color(0.2f, 0.65f, 0.95f, 0.4f);
    private static final Color HOVER_SHOVEL = new Color(1f, 0.85f, 0.2f, 0.35f);
    private static final Color HOVER_FEED = new Color(0.7f, 0.4f, 1f, 0.35f);
    private static final Color HOVER_SUN = new Color(1f, 0.9f, 0.2f, 0.35f);
    private static final Color HOVER_BORDER = new Color(1f, 1f, 1f, 0.9f);

    private final GameplayMenuController gameplay = GameplayMenuController.getInstance();
    private final LawnLayout lawnLayout;
    private final LawnBackgroundRenderer lawnBackground;
    private final WaterUnderlayerRenderer waterUnderlayer;
    private final LawnEntityRenderer entityRenderer;
    private final DebugEntityOverlay entityOverlay;

    private Table pickerPanel;
    private Label statusLabel;
    private SunHud sunHud;
    private final Vector2 logoTmp = new Vector2();
    private final float[] sunPosTmp = new float[2];
    private final int[] cellTmp = new int[2];
    private Tool tool = Tool.PLANT;
    private String selectedPlant = "Sunflower";
    private String selectedZombie = "ZombieDefault";
    private Chapter selectedZombieChapter = Chapter.ANCIENT_EGYPT;
    private boolean icedPick;
    private boolean lowTidePick;
    private boolean paused;
    /** Sandstorms queued by the button → biome skin to apply once their zombie lands. */
    private final Map<SandstormSpawn, Chapter> sandstormSkins = new IdentityHashMap<>();
    private Texture placeholderAvatar;
    private Texture whitePixel;

    private int hoverCol = -1;
    private int hoverRow = -1;

    public DebugPlaygroundScreen(PvzGdxGame game) {
        super(game);
        GameModel model = App.getInstance().getCurrentGameModel();
        int rows = model != null ? model.getMap().getRows() : LawnLayout.DEFAULT_ROWS;
        int cols = model != null ? model.getMap().getCols() : LawnLayout.DEFAULT_COLS;
        lawnLayout = new LawnLayout(rows, cols);
        lawnBackground = new LawnBackgroundRenderer(assets.textures, LawnBackgroundRenderer.Style.FRONT_LAWN);
        lawnBackground.ensureLoaded();
        waterUnderlayer = new WaterUnderlayerRenderer(assets, lawnLayout);

        BitmapFont font = resolveFont();
        entityOverlay = new DebugEntityOverlay(lawnLayout, font);
        entityRenderer = new LawnEntityRenderer(assets, lawnLayout, entityOverlay);
        entityRenderer.setScreenShake(screenShake);
        entityRenderer.preloadCraters();

        List<String> plants = plantNames();
        if (!plants.isEmpty()) {
            selectedPlant = plants.get(0);
        }
        List<ZombiePick> zombies = zombiePicks();
        if (!zombies.isEmpty()) {
            ZombiePick first = zombies.get(0);
            selectedZombie = first.name;
            selectedZombieChapter = first.chapter;
        }

        Pixmap pixmap = new Pixmap(48, 48, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.35f, 0.35f, 0.38f, 1f);
        pixmap.fill();
        placeholderAvatar = new Texture(pixmap);
        pixmap.dispose();

        Pixmap pixelPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixelPix.setColor(Color.WHITE);
        pixelPix.fill();
        whitePixel = new Texture(pixelPix);
        pixelPix.dispose();

        setWorldInput(createWorldClickInput(lawnLayout, this::onWorldClick, this::onCellHover));
        buildHud();
        refreshStatus();

        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop != null && loop.getSunFallSystem() != null) {
            loop.getSunFallSystem().spawnSkySun(4, 2, SunType.NORMAL);
        }
    }

    private BitmapFont resolveFont() {
        try {
            Label.LabelStyle style = skin.get("medium", Label.LabelStyle.class);
            if (style != null && style.font != null) {
                return style.font;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return skin.get(BitmapFont.class);
    }

    private void buildHud() {
        Table topLeft = new Table();
        topLeft.setFillParent(true);
        topLeft.setTouchable(Touchable.childrenOnly);
        topLeft.top().left().pad(12f);
        if (SunHud.showFor(App.getInstance().getCurrentGameModel())) {
            sunHud = new SunHud(skin);
            topLeft.add(sunHud).left().padBottom(8f).row();
        }
        statusLabel = new Label("", skin, "secondary");
        statusLabel.setWrap(true);
        topLeft.add(statusLabel).width(720f).left().padBottom(8f).row();
        topLeft.add(toolPanel()).left();
        uiStage.addActor(topLeft);
        pickerPanel = new Table();
        pickerPanel.setVisible(false);
        pickerPanel.setFillParent(true);
        uiStage.addActor(pickerPanel);
        toast.toFront();
    }

    private Table toolPanel() {
        Table panel = new Table();
        panel.defaults().pad(3f);
        addEntityToolRow(panel);
        addCellToolRow(panel);
        addCheatRow(panel);
        addWaterRow(panel);
        addSimRow(panel);
        addSpecialRow(panel);
        return panel;
    }

    private void addEntityToolRow(Table panel) {
        panel.add(toolButton("Plant", Tool.PLANT, true)).width(120f).height(44f);
        panel.add(toolButton("Zombie", Tool.ZOMBIE, true)).width(120f).height(44f);
        panel.add(toolButton("Iced zombie", Tool.ICED, true)).width(150f).height(44f);
        panel.add(toolButton("Shovel", Tool.SHOVEL, false)).width(120f).height(44f);
        panel.row();
    }

    private void addCellToolRow(Table panel) {
        panel.add(toolButton("Feed", Tool.FEED, false)).width(120f).height(44f);
        panel.add(toolButton("Collect sun", Tool.COLLECT_SUN, false)).width(150f).height(44f);
        panel.row();
    }

    private void addCheatRow(Table panel) {
        panel.add(actionButton("+1000 sun", this::cheatAddSun, "purple")).width(140f).height(44f);
        panel.add(actionButton("+PF", this::cheatAddPf, "purple")).width(90f).height(44f);
        panel.add(actionButton("Nuke", this::cheatNuke, "purple")).width(100f).height(44f);
        panel.row();
    }

    private void addWaterRow(Table panel) {
        panel.add(actionButton("Water", this::toggleWater, "purple")).width(120f).height(44f);
        panel.add(actionButton("Water left", this::waterLeft, "purple")).width(140f).height(44f);
        panel.add(actionButton("Water right", this::waterRight, "purple")).width(150f).height(44f);
        panel.row();
    }

    private void addSimRow(Table panel) {
        panel.add(actionButton("Pause/Resume", this::togglePause, "purple")).width(160f).height(44f);
        panel.add(actionButton("Exit", this::exitToAdventure, "brown")).width(100f).height(44f);
        panel.row();
    }

    private void addSpecialRow(Table panel) {
        panel.add(actionButton("Sandstorm", this::spawnSandstorm, "purple")).width(140f).height(44f);
        panel.add(toolButton("Low tide", Tool.LOW_TIDE, true)).width(140f).height(44f);
    }

    private TextButton toolButton(String label, Tool next, boolean openPicker) {
        TextButton button = new TextButton(label, skin, "purple");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tool = next;
                icedPick = next == Tool.ICED;
                lowTidePick = next == Tool.LOW_TIDE;
                if (openPicker) {
                    openPicker(next == Tool.PLANT);
                } else {
                    refreshStatus();
                }
            }
        });
        return button;
    }

    private TextButton actionButton(String label, Runnable action, String style) {
        TextButton button = new TextButton(label, skin, style);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private void openPicker(boolean plants) {
        pickerPanel.clear();
        pickerPanel.setVisible(true);
        pickerPanel.center();
        clearHover();
        BorderedTable card = new BorderedTable();
        card.pad(16f);
        card.add(new Label(plants ? "Choose a plant" : "Choose a zombie", skin, "big"))
                .padBottom(12f).row();
        Table list = new Table();
        list.top().left();
        TextureRegionDrawable avatarDrawable =
                new TextureRegionDrawable(new TextureRegion(placeholderAvatar));
        if (plants) {
            fillPlantPicker(list, avatarDrawable);
        } else {
            fillZombiePicker(list, avatarDrawable);
        }
        ScrollPane scroll = pickerScroll(list);
        card.add(scroll).width(620f).height(480f).growX().row();
        card.add(pickerCloseButton()).width(160f).height(48f).padTop(12f);
        pickerPanel.add(card);
        pickerPanel.toFront();
        toast.toFront();
        uiStage.setScrollFocus(scroll);
        uiStage.setKeyboardFocus(scroll);
    }

    private void fillPlantPicker(Table list, TextureRegionDrawable avatarDrawable) {
        for (String name : plantNames()) {
            addPickerRow(list, avatarDrawable, name, () -> {
                selectedPlant = name;
                tool = Tool.PLANT;
                GameModel model = App.getInstance().getCurrentGameModel();
                if (model != null) {
                    model.setImitaterCopyTarget(name);
                }
                closePicker();
                refreshStatus();
                showToast("Selected " + name, false);
            });
        }
    }

    private void fillZombiePicker(Table list, TextureRegionDrawable avatarDrawable) {
        Chapter lastHeader = null;
        boolean exclusiveHeader = false;
        for (ZombiePick pick : zombiePicks()) {
            if (pick.chapter != null && pick.chapter != lastHeader) {
                lastHeader = pick.chapter;
                list.add(new Label(chapterTitle(pick.chapter), skin, "big"))
                        .left().padTop(10f).padBottom(6f).row();
            } else if (pick.chapter == null && !exclusiveHeader) {
                exclusiveHeader = true;
                list.add(new Label("Exclusive", skin, "big"))
                        .left().padTop(10f).padBottom(6f).row();
            }
            ZombiePick chosen = pick;
            addPickerRow(list, avatarDrawable, pick.label, () -> {
                selectedZombie = chosen.name;
                selectedZombieChapter = chosen.chapter;
                tool = icedPick ? Tool.ICED : (lowTidePick ? Tool.LOW_TIDE : Tool.ZOMBIE);
                closePicker();
                refreshStatus();
                showToast("Selected " + chosen.label, false);
            });
        }
    }

    private ScrollPane pickerScroll(Table list) {
        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setFlickScroll(true);
        scroll.setScrollbarsVisible(true);
        scroll.setForceScroll(false, true);
        return scroll;
    }

    private TextButton pickerCloseButton() {
        TextButton close = new TextButton("Close", skin, "brown");
        close.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closePicker();
                refreshStatus();
            }
        });
        return close;
    }

    private void closePicker() {
        uiStage.setScrollFocus(null);
        pickerPanel.setVisible(false);
        pickerPanel.clear();
    }

    private void addPickerRow(Table list, TextureRegionDrawable avatar, String label, Runnable onSelect) {
        Table row = new Table();
        row.add(new Image(avatar)).size(48f).padRight(12f);
        row.add(new Label(label, skin, "medium")).expandX().left();
        TextButton pick = new TextButton("Select", skin, "brown");
        pick.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onSelect.run();
            }
        });
        row.add(pick).width(120f).height(40f);
        list.add(row).growX().padBottom(6f).row();
    }

    private void refreshStatus() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int sun = model != null ? model.getSunAmount() : 0;
        int pf = model != null ? model.getPlantFoodCount() : 0;
        if (sunHud != null) {
            sunHud.setAmount(sun);
        }
        statusLabel.setText(
                "TOOL: " + tool.name()
                        + " | Plant: " + selectedPlant
                        + " | Zombie: " + zombieStatusLabel()
                        + " | Sun: " + sun
                        + " | PF: " + pf
                        + waterStatus()
                        + (paused ? " | PAUSED" : "")
                        + " | Click lawn to apply.");
    }

    private static List<String> plantNames() {
        List<String> names = new ArrayList<>();
        try {
            for (Plant plant : PlantFactory.getAllDefinitions()) {
                if (plant != null && plant.getName() != null && !plant.getName().isBlank()) {
                    names.add(plant.getName());
                }
            }
        } catch (IllegalStateException ignored) {
            // factory not ready
        }
        names.sort(Comparator.naturalOrder());
        return names;
    }

    private static List<ZombiePick> zombiePicks() {
        List<String> biome = new ArrayList<>();
        List<String> exclusive = new ArrayList<>();
        try {
            for (Zombie zombie : ZombieFactory.getAllDefinitions()) {
                if (zombie == null || zombie.getName() == null || zombie.getName().isBlank()) {
                    continue;
                }
                if (ZombiePamAliases.usesChapterArt(zombie.getName())) {
                    biome.add(zombie.getName());
                } else {
                    exclusive.add(zombie.getName());
                }
            }
        } catch (IllegalStateException ignored) {
            // factory not ready
        }
        biome.sort(Comparator.naturalOrder());
        exclusive.sort(Comparator.naturalOrder());
        List<ZombiePick> picks = new ArrayList<>();
        for (Chapter chapter : Chapter.values()) {
            for (String name : biome) {
                picks.add(new ZombiePick(name, chapter, name + " (" + shortChapter(chapter) + ")"));
            }
        }
        for (String name : exclusive) {
            picks.add(new ZombiePick(name, null, name));
        }
        return picks;
    }

    private String zombieStatusLabel() {
        if (selectedZombieChapter == null) {
            return selectedZombie;
        }
        return selectedZombie + " (" + shortChapter(selectedZombieChapter) + ")";
    }

    private static String shortChapter(Chapter chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> "Egypt";
            case FROSTBITE_CAVES -> "Frostbite";
            case BIG_WAVE_BEACH -> "Beach";
            case DARK_AGES -> "Dark Ages";
        };
    }

    private static String chapterTitle(Chapter chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> "Ancient Egypt";
            case FROSTBITE_CAVES -> "Frostbite Caves";
            case BIG_WAVE_BEACH -> "Big Wave Beach";
            case DARK_AGES -> "Dark Ages";
        };
    }

    private static final class ZombiePick {
        final String name;
        final Chapter chapter;
        final String label;

        ZombiePick(String name, Chapter chapter, String label) {
            this.name = name;
            this.chapter = chapter;
            this.label = label;
        }
    }

    private void onCellHover(int col, int row) {
        if (pickerPanel.isVisible()) {
            clearHover();
            return;
        }
        hoverCol = col;
        hoverRow = row;
    }

    private void clearHover() {
        hoverCol = -1;
        hoverRow = -1;
    }

    private boolean onWorldClick(float worldX, float worldY) {
        if (tryCollectPlantFood(worldX, worldY) || tryCollectSun(worldX, worldY)) {
            return true;
        }
        if (!lawnLayout.worldToCell(worldX, worldY, cellTmp)) {
            return false;
        }
        return onCellPicked(cellTmp[0], cellTmp[1]);
    }

    private boolean tryCollectPlantFood(float worldX, float worldY) {
        if (pickerPanel.isVisible()) {
            return false;
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return false;
        }
        PlantFoodPickup food = entityRenderer.pickPlantFood(model, worldX, worldY);
        if (food == null) {
            return false;
        }
        CommandResult<Void> result = gameplay.collectPlantFood(food);
        if (!result.isSuccess()) {
            return false;
        }
        refreshStatus();
        return true;
    }

    private boolean tryCollectSun(float worldX, float worldY) {
        if (pickerPanel.isVisible()) {
            return false;
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return false;
        }
        Sun sun = entityRenderer.pickSun(model, worldX, worldY);
        if (sun == null) {
            return false;
        }
        entityRenderer.writeSunDrawPos(sun, sunPosTmp);
        CommandResult<Void> result = gameplay.collectSun(sun);
        if (!result.isSuccess()) {
            return false;
        }
        float destX = sunPosTmp[0];
        float destY = sunPosTmp[1];
        if (sunHud != null) {
            sunHud.logoCenter(logoTmp);
            destX = logoTmp.x;
            destY = logoTmp.y;
        }
        entityRenderer.startSunCollect(sun, sunPosTmp[0], sunPosTmp[1], destX, destY);
        refreshStatus();
        return true;
    }

    private boolean onCellPicked(int col, int row) {
        if (pickerPanel.isVisible()) {
            return false;
        }
        CommandResult<Void> result;
        if (tool == Tool.ZOMBIE) {
            result = gameplay.cheatSpawnZombie(selectedZombie, col, row);
            if (result.isSuccess() && selectedZombieChapter != null) {
                GameModel model = App.getInstance().getCurrentGameModel();
                List<ZombieInstance> zombies = model == null ? null : model.getZombies();
                if (zombies != null && !zombies.isEmpty()) {
                    entityRenderer.setArtChapter(zombies.get(zombies.size() - 1), selectedZombieChapter);
                }
            }
        } else if (tool == Tool.ICED) {
            result = gameplay.cheatSpawnIcedZombie(selectedZombie, col, row);
            if (result.isSuccess() && selectedZombieChapter != null) {
                GameModel model = App.getInstance().getCurrentGameModel();
                Cell cell = model == null ? null : model.getCellAt(row, col);
                if (cell != null && cell.getTerrainStrategy() instanceof IceTerrainStrategy ice
                        && ice.getContainedEntity() instanceof ZombieInstance zombie) {
                    entityRenderer.setArtChapter(zombie, selectedZombieChapter);
                }
            }
        } else if (tool == Tool.LOW_TIDE) {
            result = gameplay.cheatLowTideAmbush(selectedZombie, col, row);
            if (result.isSuccess() && selectedZombieChapter != null) {
                GameModel model = App.getInstance().getCurrentGameModel();
                List<ZombieInstance> zombies = model == null ? null : model.getZombies();
                if (zombies != null && !zombies.isEmpty()) {
                    entityRenderer.setArtChapter(zombies.get(zombies.size() - 1), selectedZombieChapter);
                }
            }
        } else {
            result = switch (tool) {
                case PLANT -> gameplay.plant(selectedPlant, col, row);
                case SHOVEL -> gameplay.pluck(col, row);
                case FEED -> gameplay.feed(col, row);
                case COLLECT_SUN -> gameplay.collectSun(col, row);
                case ZOMBIE, ICED, LOW_TIDE -> gameplay.cheatSpawnZombie(selectedZombie, col, row);
            };
        }
        showToast(result.getMessage(), !result.isSuccess());
        refreshStatus();
        return true;
    }

    private String waterStatus() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int cols = model == null || model.getMap() == null
                ? 0
                : WaterBand.columnsFromRight(model.getMap());
        if (cols <= 0) {
            return "";
        }
        return " | Water: " + cols + " col";
    }

    private void toggleWater() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int current = model == null || model.getMap() == null
                ? 0
                : WaterBand.columnsFromRight(model.getMap());
        int next = current > 0 ? 0 : WaterBand.DEFAULT_COLUMNS;
        CommandResult<Void> r = gameplay.cheatSetWaterBand(next);
        showToast(r.getMessage(), !r.isSuccess());
        refreshStatus();
    }

    private void waterLeft() {
        CommandResult<Void> r = gameplay.cheatNudgeWaterBand(1);
        showToast(r.getMessage(), !r.isSuccess());
        refreshStatus();
    }

    private void waterRight() {
        CommandResult<Void> r = gameplay.cheatNudgeWaterBand(-1);
        showToast(r.getMessage(), !r.isSuccess());
        refreshStatus();
    }

    private void cheatAddSun() {
        CommandResult<Void> r = gameplay.cheatAddSuns(1000);
        showToast(r.getMessage(), !r.isSuccess());
        refreshStatus();
    }

    private void cheatAddPf() {
        CommandResult<Void> r = gameplay.cheatAddPlantFood();
        showToast(r.getMessage(), !r.isSuccess());
        refreshStatus();
    }

    private void cheatNuke() {
        CommandResult<Void> r = gameplay.releaseNuke();
        showToast(r.getMessage(), !r.isSuccess());
        refreshStatus();
    }

    /** Queues a sandstorm that carries the currently selected zombie in. */
    private void spawnSandstorm() {
        GameModel model = App.getInstance().getCurrentGameModel();
        Zombie zombie = ZombieFactory.getDefinition(selectedZombie);
        if (model == null || zombie == null) {
            showToast("No game / zombie definition for " + selectedZombie, true);
            return;
        }
        int lanes = Math.max(1, model.getMap().getRows());
        int lane = ThreadLocalRandom.current().nextInt(lanes);
        int columnsAhead = 1 + ThreadLocalRandom.current()
                .nextInt(Wave.TORNADO_MAX_COLUMNS_AHEAD);
        Chapter skin = selectedZombieChapter;
        model.queueSandstormSpawn(zombie, lane, columnsAhead);
        List<SandstormSpawn> storms = model.getSandstorms();
        SandstormSpawn storm = storms.isEmpty() ? null : storms.get(storms.size() - 1);
        if (skin != null && storm != null) {
            sandstormSkins.put(storm, skin);
        }
        showToast("Sandstorm incoming: " + zombie.getName() + " in lane "
                + (lane + 1) + ", " + columnsAhead + " column(s) ahead.", false);
        refreshStatus();
    }

    /** Biome-skin override for zombies once their debug sandstorm touches down. */
    private void applyStormSkins() {
        if (sandstormSkins.isEmpty()) {
            return;
        }
        sandstormSkins.entrySet().removeIf(entry -> {
            if (!entry.getKey().hasLanded()) {
                return false;
            }
            ZombieInstance spawned = entry.getKey().getSpawned();
            if (spawned != null) {
                entityRenderer.setArtChapter(spawned, entry.getValue());
            }
            return true;
        });
    }

    private void togglePause() {
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop == null) {
            return;
        }
        if (paused || loop.getGameState() == GameState.PAUSED) {
            loop.resume();
            paused = false;
            showToast("Resumed.", false);
        } else {
            loop.pause();
            paused = true;
            showToast("Paused.", false);
        }
        refreshStatus();
    }

    private void exitToAdventure() {
        App.getInstance().setCurrentGameModel(null);
        App.getInstance().setCurrentGameLoop(null);
        App.getInstance().setCurrentMenu(MenuType.GAME);
        game.setScreen(new AdventureScreen(game));
    }

    @Override
    protected void updateLogic(float delta) {
        if (paused) {
            return;
        }
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop != null && loop.getGameState() == GameState.RUNNING) {
            loop.update(delta);
        }
        applyStormSkins();
        if (sunHud != null) {
            GameModel model = App.getInstance().getCurrentGameModel();
            sunHud.setAmount(model == null ? 0 : model.getSunAmount());
        }
    }

    @Override
    protected void renderWorld(float delta) {
        lawnBackground.draw(game.batch);
        waterUnderlayer.draw(game.batch, App.getInstance().getCurrentGameModel(), delta);
        drawHoverHighlight();
        entityRenderer.draw(game.batch, App.getInstance().getCurrentGameModel(), delta);
    }

    private void drawHoverHighlight() {
        if (hoverCol < 0 || hoverRow < 0) {
            return;
        }
        float x = lawnLayout.cellLeft(hoverCol);
        float y = lawnLayout.cellBottom(hoverRow);
        float w = lawnLayout.cellWidth();
        float h = lawnLayout.cellHeight();

        Color fill = switch (tool) {
            case PLANT -> HOVER_PLANT;
            case ZOMBIE -> HOVER_ZOMBIE;
            case ICED -> HOVER_ICED;
            case LOW_TIDE -> HOVER_LOW_TIDE;
            case SHOVEL -> HOVER_SHOVEL;
            case FEED -> HOVER_FEED;
            case COLLECT_SUN -> HOVER_SUN;
        };

        game.batch.setColor(fill);
        game.batch.draw(whitePixel, x, y, w, h);

        float t = 3f;
        game.batch.setColor(HOVER_BORDER);
        game.batch.draw(whitePixel, x, y, w, t);
        game.batch.draw(whitePixel, x, y + h - t, w, t);
        game.batch.draw(whitePixel, x, y, t, h);
        game.batch.draw(whitePixel, x + w - t, y, t, h);
        game.batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        entityOverlay.dispose();
        if (placeholderAvatar != null) {
            placeholderAvatar.dispose();
            placeholderAvatar = null;
        }
        if (whitePixel != null) {
            whitePixel.dispose();
            whitePixel = null;
        }
        super.dispose();
    }
}
