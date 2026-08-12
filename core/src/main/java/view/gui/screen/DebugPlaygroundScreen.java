package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import model.enums.GameState;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.zombie.ZombieFactory;
import model.zombie.definition.Zombie;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.lawn.DebugEntityOverlay;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnEntityRenderer;
import view.gui.lawn.LawnLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Debug FrontLawn playground: tool panel + free plant/zombie placement, no win/lose.
 */
public final class DebugPlaygroundScreen extends AbstractGameplayScreen {
    private enum Tool { PLANT, ZOMBIE, SHOVEL, FEED, COLLECT_SUN }

    private static final Color HOVER_PLANT = new Color(0.35f, 1f, 0.45f, 0.35f);
    private static final Color HOVER_ZOMBIE = new Color(1f, 0.35f, 0.35f, 0.35f);
    private static final Color HOVER_SHOVEL = new Color(1f, 0.85f, 0.2f, 0.35f);
    private static final Color HOVER_FEED = new Color(0.7f, 0.4f, 1f, 0.35f);
    private static final Color HOVER_SUN = new Color(1f, 0.9f, 0.2f, 0.35f);
    private static final Color HOVER_BORDER = new Color(1f, 1f, 1f, 0.9f);

    private final GameplayMenuController gameplay = GameplayMenuController.getInstance();
    private final LawnLayout lawnLayout;
    private final LawnBackgroundRenderer lawnBackground;
    private final LawnEntityRenderer entityRenderer;
    private final DebugEntityOverlay entityOverlay;

    private Table pickerPanel;
    private Label statusLabel;
    private Tool tool = Tool.PLANT;
    private String selectedPlant = "Sunflower";
    private String selectedZombie = "ZombieDefault";
    private boolean paused;
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
        lawnBackground = new LawnBackgroundRenderer(assets.textures);
        lawnBackground.ensureLoaded();

        BitmapFont font = resolveFont();
        entityOverlay = new DebugEntityOverlay(lawnLayout, font);
        entityRenderer = new LawnEntityRenderer(assets, lawnLayout, entityOverlay);

        List<String> plants = plantNames();
        if (!plants.isEmpty()) {
            selectedPlant = plants.get(0);
        }
        List<String> zombies = zombieNames();
        if (!zombies.isEmpty()) {
            selectedZombie = zombies.contains("ZombieDefault") ? "ZombieDefault" : zombies.get(0);
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

        setWorldInput(createCellPickInput(lawnLayout, this::onCellPicked, this::onCellHover));
        buildHud();
        refreshStatus();
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

        statusLabel = new Label("", skin, "secondary");
        statusLabel.setWrap(true);
        topLeft.add(statusLabel).width(720f).left().padBottom(8f).row();

        Table panel = new Table();
        panel.defaults().pad(3f);

        // Row 1 — tools that pick entities
        panel.add(toolButton("Plant", Tool.PLANT, true)).width(120f).height(44f);
        panel.add(toolButton("Zombie", Tool.ZOMBIE, true)).width(120f).height(44f);
        panel.add(toolButton("Shovel", Tool.SHOVEL, false)).width(120f).height(44f);
        panel.row();

        // Row 2 — cell tools
        panel.add(toolButton("Feed", Tool.FEED, false)).width(120f).height(44f);
        panel.add(toolButton("Collect sun", Tool.COLLECT_SUN, false)).width(150f).height(44f);
        panel.row();

        // Row 3 — cheats
        panel.add(actionButton("+1000 sun", this::cheatAddSun, "purple")).width(140f).height(44f);
        panel.add(actionButton("+PF", this::cheatAddPf, "purple")).width(90f).height(44f);
        panel.add(actionButton("Nuke", this::cheatNuke, "purple")).width(100f).height(44f);
        panel.row();

        // Row 4 — sim control
        panel.add(actionButton("Pause/Resume", this::togglePause, "purple")).width(160f).height(44f);
        panel.add(actionButton("Exit", this::exitToAdventure, "brown")).width(100f).height(44f);

        topLeft.add(panel).left();
        uiStage.addActor(topLeft);

        pickerPanel = new Table();
        pickerPanel.setVisible(false);
        pickerPanel.setFillParent(true);
        uiStage.addActor(pickerPanel);
        toast.toFront();
    }

    private TextButton toolButton(String label, Tool next, boolean openPicker) {
        TextButton button = new TextButton(label, skin, "purple");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tool = next;
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
        clearHover();

        BorderedTable card = new BorderedTable();
        card.pad(16f);
        card.add(new Label(plants ? "Choose a plant" : "Choose a zombie", skin, "big")).padBottom(12f).row();

        Table list = new Table();
        List<String> names = plants ? plantNames() : zombieNames();
        TextureRegionDrawable avatarDrawable =
                new TextureRegionDrawable(new TextureRegion(placeholderAvatar));

        for (String name : names) {
            Table row = new Table();
            row.add(new Image(avatarDrawable)).size(48f).padRight(12f);
            row.add(new Label(name, skin, "medium")).expandX().left();

            TextButton pick = new TextButton("Select", skin, "brown");
            String selectedName = name;
            pick.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (plants) {
                        selectedPlant = selectedName;
                        tool = Tool.PLANT;
                    } else {
                        selectedZombie = selectedName;
                        tool = Tool.ZOMBIE;
                    }
                    pickerPanel.setVisible(false);
                    pickerPanel.clear();
                    refreshStatus();
                    showToast("Selected " + selectedName, false);
                }
            });
            row.add(pick).width(120f).height(40f);
            list.add(row).growX().padBottom(6f).row();
        }

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        card.add(scroll).width(560f).height(520f).row();

        TextButton close = new TextButton("Close", skin, "brown");
        close.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                pickerPanel.setVisible(false);
                pickerPanel.clear();
                refreshStatus();
            }
        });
        card.add(close).width(160f).height(48f).padTop(12f);

        Table center = new Table();
        center.setFillParent(true);
        center.add(card);
        pickerPanel.addActor(center);
        pickerPanel.toFront();
        toast.toFront();
    }

    private void refreshStatus() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int sun = model != null ? model.getSunAmount() : 0;
        int pf = model != null ? model.getPlantFoodCount() : 0;
        statusLabel.setText(
                "TOOL: " + tool.name()
                        + " | Plant: " + selectedPlant
                        + " | Zombie: " + selectedZombie
                        + " | Sun: " + sun
                        + " | PF: " + pf
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

    private static List<String> zombieNames() {
        List<String> names = new ArrayList<>();
        try {
            for (Zombie zombie : ZombieFactory.getAllDefinitions()) {
                if (zombie != null && zombie.getName() != null && !zombie.getName().isBlank()) {
                    names.add(zombie.getName());
                }
            }
        } catch (IllegalStateException ignored) {
            // factory not ready
        }
        names.sort(Comparator.naturalOrder());
        return names;
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

    private boolean onCellPicked(int col, int row) {
        if (pickerPanel.isVisible()) {
            return false;
        }
        CommandResult<Void> result = switch (tool) {
            case PLANT -> gameplay.plant(selectedPlant, col, row);
            case ZOMBIE -> gameplay.cheatSpawnZombie(selectedZombie, col, row);
            case SHOVEL -> gameplay.pluck(col, row);
            case FEED -> gameplay.feed(col, row);
            case COLLECT_SUN -> gameplay.collectSun(col, row);
        };
        showToast(result.getMessage(), !result.isSuccess());
        refreshStatus();
        return true;
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
    }

    @Override
    protected void renderWorld(float delta) {
        lawnBackground.draw(game.batch);
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
