package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.GameplayMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.GameState;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.Level;
import model.item.LootPickup;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.plant.PlantFactory;
import model.user.User;
import view.gui.PvzGdxGame;
import view.gui.lawn.DebugEntityOverlay;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnEntityRenderer;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.LawnRowColHighlight;
import view.gui.lawn.WaterUnderlayerRenderer;
import view.gui.ui.CoinHud;
import view.gui.ui.LootRewardPopup;
import view.gui.ui.PlantFoodBankHud;
import view.gui.ui.ReadySetPlantBanner;
import view.gui.ui.SeedPacketActor;
import view.gui.ui.SunHud;
import view.gui.ui.WaveAnnounceBanner;
import view.gui.ui.WaveProgressHud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-game lawn. Chapter backgrounds use the same left/center/right camera as debug FrontLawn.
 */
public final class GameplayScreen extends AbstractGameplayScreen {
    private final GameplayMenuController gameplay = GameplayMenuController.getInstance();
    private final LawnLayout lawnLayout;
    private final LawnBackgroundRenderer lawnBackground;
    private final WaterUnderlayerRenderer waterUnderlayer;
    private final LawnEntityRenderer entityRenderer;
    private final DebugEntityOverlay entityOverlay;
    private final Vector2 logoTmp = new Vector2();
    private final Vector2 stageToScreen = new Vector2();
    private final Vector3 worldTmp = new Vector3();
    private final float[] sunPosTmp = new float[2];
    private final int[] cellTmp = new int[2];

    private SunHud sunHud;
    private CoinHud coinHud;
    private PlantFoodBankHud plantFoodBank;
    private LootRewardPopup lootRewardPopup;
    private ReadySetPlantBanner readySetPlant;
    private WaveAnnounceBanner waveAnnounce;
    private WaveProgressHud waveProgress;
    private Table packetColumn;
    private LawnRowColHighlight rowColHighlight;
    private String previewPlant;
    private float previewTime;
    private int hoverCol = -1;
    private int hoverRow = -1;
    private List<String> shownPackets = List.of();

    private boolean plantfoodMode;
    private boolean shovelMode;
    private ImageButton shovelButton;
    private Cursor hiddenCursor;
    private TextureRegion plantfoodCursorRegion;
    private TextureRegion shovelCursorRegion;
    private final Vector3 cursorUnprojectTmp = new Vector3();

    private static final String SHOVEL_CURSOR_ID = "IMAGE_UI_HUD_INGAME_SHOVEL_ICON";
    /** Native 768 {@code IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON} size. */
    private static final float SHOVEL_SIZE = 79f;

    public GameplayScreen(PvzGdxGame game) {
        super(game);
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);
        lawnLayout = lawnLayout();
        Chapter chapter = currentChapter();
        lawnBackground = new LawnBackgroundRenderer(
            assets.textures, LawnBackgroundRenderer.Style.forChapter(chapter));
        lawnBackground.ensureLoaded();
        waterUnderlayer = chapter == Chapter.BIG_WAVE_BEACH
            ? new WaterUnderlayerRenderer(assets, lawnLayout)
            : null;
        entityOverlay = new DebugEntityOverlay(lawnLayout, resolveFont());
        entityRenderer = new LawnEntityRenderer(assets, lawnLayout, entityOverlay);
        entityRenderer.setScreenShake(screenShake);
        entityRenderer.resetMowers(chapter, lawnMowersEnabled());
        assets.textures.loadSync("UI_SeedPackets_768");
        assets.textures.loadSync("ATLASIMAGE_ATLAS_UI_SEEDPACKETS_768_00");

        assets.textures.loadSync(PlantFoodBankHud.ATLAS_GROUP);
        assets.textures.loadSync(PlantFoodBankHud.ATLAS_PAGE_0);
        assets.textures.loadSync(PlantFoodBankHud.ATLAS_PAGE_1);
        assets.textures.loadSync("ZENGARDENGROUP_768");
        assets.textures.loadSync("ATLASIMAGE_ATLAS_ZENGARDENGROUP_768_00");
        setWorldInput(createWorldClickInput(lawnLayout, this::onWorldClick, this::onCellHover));
        buildHud();
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

    private static LawnLayout lawnLayout() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int rows = model != null ? model.getMap().getRows() : LawnLayout.DEFAULT_ROWS;
        int cols = model != null ? model.getMap().getCols() : LawnLayout.DEFAULT_COLS;
        return new LawnLayout(rows, cols);
    }

    private static Chapter currentChapter() {
        Level level = currentLevel();
        return level == null || level.getConfig() == null ? null : level.getConfig().getChapter();
    }

    private static Level currentLevel() {
        GameModel model = App.getInstance().getCurrentGameModel();
        return model == null ? null : model.getCurrentLevel();
    }

    private boolean isPregame() {
        return entityRenderer.isMowerIntroPlaying()
            || (readySetPlant != null && readySetPlant.isPlaying());
    }

    private static boolean lawnMowersEnabled() {
        Level level = currentLevel();
        return level != null
            && level.getConfig() != null
            && level.getConfig().getRules() != null
            && level.getConfig().getRules().isLawnMowersEnabled();
    }

    private void buildHud() {
        GameModel model = App.getInstance().getCurrentGameModel();
        boolean sunBank = SunHud.showFor(model);

        Table left = new Table();
        left.setFillParent(true);
        left.setTouchable(Touchable.childrenOnly);
        left.top().left().pad(8f);
        if (sunBank) {
            sunHud = new SunHud(skin);
            sunHud.setAmount(model == null ? 0 : model.getSunAmount());
            left.add(sunHud).left().padBottom(8f).row();
        }
        packetColumn = new Table();
        left.add(packetColumn).left().top();
        uiStage.addActor(left);

        Table topRight = new Table();
        topRight.setFillParent(true);
        topRight.setTouchable(Touchable.childrenOnly);
        topRight.top().right().pad(12f);

        coinHud = new CoinHud(skin, assets.textures);
        coinHud.setAmount(model == null ? 0 : model.getCoinCount());
        topRight.add(coinHud).right().padBottom(8f).row();

        TextButton back = new TextButton("Back to levels", skin, "brown");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                exitToLevels();
            }
        });
        topRight.add(back).width(220f).height(48f);
        uiStage.addActor(topRight);

        if (WaveProgressHud.showFor(model)) {
            waveProgress = new WaveProgressHud(skin, assets.textures);
            Table topCenter = new Table();
            topCenter.setFillParent(true);
            topCenter.setTouchable(Touchable.childrenOnly);
            topCenter.top().padTop(8f);
            topCenter.add(waveProgress).top();
            uiStage.addActor(topCenter);
        }

        lootRewardPopup = new LootRewardPopup(skin);
        Table rewardAnchor = new Table();
        rewardAnchor.setFillParent(true);
        rewardAnchor.top().padTop(72f);
        rewardAnchor.add(lootRewardPopup).top();
        uiStage.addActor(rewardAnchor);

        plantFoodBank = new PlantFoodBankHud(skin, assets.textures);
        plantFoodBank.onPlantFoodButton(() -> setPlantfoodMode(!plantfoodMode));
        Table bottomLeft = new Table();
        bottomLeft.setFillParent(true);
        bottomLeft.setTouchable(Touchable.childrenOnly);
        bottomLeft.bottom().left().pad(8f);
        bottomLeft.add(plantFoodBank).left().bottom();
        uiStage.addActor(bottomLeft);

        buildBottomRight(model);

        readySetPlant = new ReadySetPlantBanner(skin);
        uiStage.addActor(readySetPlant);
        readySetPlant.play();

        waveAnnounce = new WaveAnnounceBanner(skin);
        uiStage.addActor(waveAnnounce);

        toast.toFront();
        refreshPackets();
    }

    private void buildBottomRight(GameModel model) {
        boolean debug = model != null && model.isDebugMode();
        boolean shovel = shovelEnabled(model);
        if (!debug && !shovel) {
            return;
        }
        Table bottomRight = new Table();
        bottomRight.setFillParent(true);
        bottomRight.setTouchable(Touchable.childrenOnly);
        bottomRight.bottom().right().pad(8f);

        if (debug) {
            TextButton addSun = new TextButton("+100 sun", skin, "brown");
            addSun.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    CommandResult<Void> result = gameplay.cheatAddSuns(100);
                    showToast(result.getMessage(), !result.isSuccess());
                    if (sunHud != null && result.isSuccess()) {
                        sunHud.setAmount(App.getInstance().getCurrentGameModel().getSunAmount());
                    }
                    refreshPacketChrome();
                }
            });

            TextButton addPlantFood = new TextButton("+1 plant food", skin, "brown");
            addPlantFood.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    CommandResult<Void> result = gameplay.cheatAddPlantFood();
                    showToast(result.getMessage(), !result.isSuccess());
                    if (plantFoodBank != null && result.isSuccess()) {
                        plantFoodBank.setCount(App.getInstance().getCurrentGameModel().getPlantFoodCount());
                    }
                    refreshPacketChrome();
                }
            });

            Table cheats = new Table();
            cheats.add(addSun).width(160f).height(44f).padRight(8f);
            cheats.add(addPlantFood).width(180f).height(44f);
            bottomRight.add(cheats).right().padBottom(8f).row();
        }

        if (shovel) {
            shovelButton = new ImageButton(skin, "ingame_shovel");
            shovelButton.setProgrammaticChangeEvents(false);
            shovelButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setShovelMode(!shovelMode);
                }
            });
            bottomRight.add(shovelButton).size(SHOVEL_SIZE).right();
        }
        uiStage.addActor(bottomRight);
    }

    private void refreshPackets() {
        List<String> selected = selectedPlants();
        shownPackets = new ArrayList<>(selected);
        packetColumn.clearChildren();
        for (String name : selected) {
            SeedPacketActor packet = new SeedPacketActor(
                assets.textures, skin, name, plantCost(name), plantLevel(name),
                boosted(name), false);
            packet.onDragPlant(new SeedPacketActor.DragPlant() {
                @Override
                public void dragStart(SeedPacketActor packet) {
                    previewPlant = name;
                    previewTime = 0f;
                    entityRenderer.preloadPlantIdle(name);
                    stageToScreen.set(packet.getWidth() * 0.5f, packet.getHeight() * 0.5f);
                    packet.localToStageCoordinates(stageToScreen);
                    followPlantDrag(stageToScreen.x, stageToScreen.y);
                }

                @Override
                public void drag(SeedPacketActor packet, float stageX, float stageY) {
                    followPlantDrag(stageX, stageY);
                }

                @Override
                public void dragEnd(SeedPacketActor packet, float stageX, float stageY) {
                    dropPlant(name, stageX, stageY);
                    previewPlant = null;
                    hoverCol = -1;
                    hoverRow = -1;
                }
            });
            packetColumn.add(packet)
                .size(SeedPacketActor.PACKET_WIDTH, SeedPacketActor.PACKET_HEIGHT)
                .padBottom(6f).row();
        }
        refreshPacketChrome();
    }

    private void refreshPacketChrome() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int sun = model == null ? 0 : model.getSunAmount();
        for (Actor actor : packetColumn.getChildren()) {
            if (!(actor instanceof SeedPacketActor packet) || packet.plantName() == null) {
                continue;
            }
            String name = packet.plantName();
            boolean ready = model == null || model.isSeedReady(name);
            boolean afford = plantCost(name) <= sun;
            packet.setDimmed(!ready || !afford);
        }
    }

    private void followPlantDrag(float stageX, float stageY) {
        stageToWorld(stageX, stageY);
        if (lawnLayout.worldToCell(worldTmp.x, worldTmp.y, cellTmp)) {
            hoverCol = cellTmp[0];
            hoverRow = cellTmp[1];
        } else {
            hoverCol = -1;
            hoverRow = -1;
        }
    }

    private void stageToWorld(float stageX, float stageY) {
        stageToScreen.set(stageX, stageY);
        uiStage.stageToScreenCoordinates(stageToScreen);
        worldViewport.unproject(worldTmp.set(stageToScreen.x, stageToScreen.y, 0f));
    }

    private void dropPlant(String plantName, float stageX, float stageY) {
        if (isPregame()) {
            return;
        }
        stageToWorld(stageX, stageY);
        if (!lawnLayout.worldToCell(worldTmp.x, worldTmp.y, cellTmp)) {
            return;
        }
        CommandResult<Void> result = gameplay.plant(plantName, cellTmp[0], cellTmp[1]);
        showToast(result.getMessage(), !result.isSuccess());
        refreshPacketChrome();
    }

    private boolean onWorldClick(float worldX, float worldY) {
        if (plantfoodMode) {
            tryFeedPlantFood(worldX, worldY);
            setPlantfoodMode(false);
            return true;
        }
        if (shovelMode) {
            tryPluck(worldX, worldY);
            setShovelMode(false);
            return true;
        }
        return tryCollectPlantFood(worldX, worldY) || tryCollectSun(worldX, worldY);
    }

    private void onCellHover(int col, int row) {
        if (!plantfoodMode && !shovelMode) {
            return;
        }
        hoverCol = col;
        hoverRow = row;
    }

    private void tryFeedPlantFood(float worldX, float worldY) {
        if (!lawnLayout.worldToCell(worldX, worldY, cellTmp)) {
            return;
        }
        CommandResult<Void> result = gameplay.feed(cellTmp[0], cellTmp[1]);
        showToast(result.getMessage(), !result.isSuccess());
    }

    private void tryPluck(float worldX, float worldY) {
        if (!lawnLayout.worldToCell(worldX, worldY, cellTmp)) {
            return;
        }
        CommandResult<Void> result = gameplay.pluck(cellTmp[0], cellTmp[1]);
        showToast(result.getMessage(), !result.isSuccess());
    }

    private void setPlantfoodMode(boolean armed) {
        if (armed == plantfoodMode) {
            return;
        }
        if (armed) {
            setShovelMode(false);
        }
        plantfoodMode = armed;
        if (plantFoodBank != null) {
            plantFoodBank.setButtonChecked(plantfoodMode);
        }
        applyArmedCursor();
    }

    private void setShovelMode(boolean armed) {
        if (armed == shovelMode) {
            return;
        }
        if (armed) {
            setPlantfoodMode(false);
        }
        shovelMode = armed;
        if (shovelButton != null) {
            shovelButton.setChecked(shovelMode);
        }
        applyArmedCursor();
    }

    private void applyArmedCursor() {
        if (plantfoodMode || shovelMode) {
            hideOsCursor();
            previewPlant = null;
            previewTime = 0f;
        } else {
            restoreOsCursor();
            hoverCol = -1;
            hoverRow = -1;
        }
    }

    private void hideOsCursor() {
        if (hiddenCursor == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            hiddenCursor = Gdx.graphics.newCursor(pixmap, 0, 0);
            pixmap.dispose();
        }
        if (hiddenCursor != null) {
            Gdx.graphics.setCursor(hiddenCursor);
        }
    }

    private void restoreOsCursor() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    }

    private boolean tryCollectPlantFood(float worldX, float worldY) {
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
        entityRenderer.writePlantFoodDrawPos(food, sunPosTmp);
        float destX = sunPosTmp[0];
        float destY = sunPosTmp[1];
        if (plantFoodBank != null) {
            plantFoodBank.logoCenter(logoTmp);
            destX = logoTmp.x;
            destY = logoTmp.y;
            plantFoodBank.setCount(model.getPlantFoodCount());
        }
        entityRenderer.startPlantFoodCollect(food, sunPosTmp[0], sunPosTmp[1], destX, destY);
        return true;
    }

    private boolean tryCollectSun(float worldX, float worldY) {
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
            sunHud.setAmount(model.getSunAmount());
        }
        entityRenderer.startSunCollect(sun, sunPosTmp[0], sunPosTmp[1], destX, destY);
        refreshPacketChrome();
        return true;
    }

    private void exitToLevels() {
        Chapter chapter = currentChapter();
        App.getInstance().setCurrentGameModel(null);
        App.getInstance().setCurrentGameLoop(null);
        App.getInstance().setCurrentMenu(MenuType.GAME);
        if (chapter != null) {
            game.setScreen(new ChapterLevelsScreen(game, chapter));
        } else {
            game.setScreen(new AdventureScreen(game));
        }
    }

    @Override
    protected void updateLogic(float delta) {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (isPregame()) {
            entityRenderer.tickMowerIntro(delta);
        } else {
            PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
            if (loop != null && loop.getGameState() == GameState.RUNNING) {
                loop.update(delta);
            }
            if (model != null) {
                entityRenderer.tickMowers(model, delta);
            }
        }
        if (model != null) {
            String waveText = model.consumeWaveAnnouncement();
            if (waveText != null && waveAnnounce != null) {
                waveAnnounce.show(waveText);
            }
        }
        if (waveProgress != null) {
            waveProgress.sync(model);
        }
        if (sunHud != null && model != null) {
            sunHud.setAmount(model.getSunAmount());
        }
        if (plantFoodBank != null && model != null) {
            plantFoodBank.setCount(model.getPlantFoodCount());
        }
        autoCollectLoot(model);
        if (previewPlant != null) {
            previewTime += delta;
        }
        if (!selectedPlants().equals(shownPackets)) {
            refreshPackets();
        } else {
            refreshPacketChrome();
        }
    }

    @Override
    protected void renderWorld(float delta) {
        lawnBackground.draw(game.batch);
        if (waterUnderlayer != null) {
            waterUnderlayer.draw(game.batch, App.getInstance().getCurrentGameModel(), delta);
        }
        boolean highlight = (previewPlant != null || plantfoodMode || shovelMode) && hoverCol >= 0;
        if (highlight) {
            if (rowColHighlight == null) {
                rowColHighlight = new LawnRowColHighlight();
            }
            rowColHighlight.draw(game.batch, lawnLayout, hoverCol, hoverRow);
        }
        entityRenderer.draw(game.batch, App.getInstance().getCurrentGameModel(), delta);
        if (previewPlant != null) {
            entityRenderer.drawPlantIdle(game.batch, previewPlant, worldTmp.x, worldTmp.y, previewTime);
        }
    }

    @Override
    protected void renderGraphics(float delta) {
        super.renderGraphics(delta);
        if (plantfoodMode) {
            if (plantfoodCursorRegion == null) {
                plantfoodCursorRegion = assets.textures.region(PlantFoodBankHud.CURSOR_ID);
            }
            drawHudCursor(plantfoodCursorRegion, CURSOR_SIZE);
        } else if (shovelMode) {
            if (shovelCursorRegion == null) {
                shovelCursorRegion = assets.textures.region(SHOVEL_CURSOR_ID);
            }
            float w = shovelCursorRegion == null || shovelCursorRegion.getRegionWidth() <= 0
                ? CURSOR_SIZE
                : shovelCursorRegion.getRegionWidth();
            drawHudCursor(shovelCursorRegion, w);
        }
    }

    /** Display size of the cursor image, in UI units (matches the 768 atlas scale). */
    private static final float CURSOR_SIZE = 64f;

    private void drawHudCursor(TextureRegion region, float w) {
        if (region == null) {
            return;
        }
        cursorUnprojectTmp.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        uiViewport.unproject(cursorUnprojectTmp);
        float h = region.getRegionWidth() <= 0f
            ? w
            : w * (region.getRegionHeight() / (float) region.getRegionWidth());
        game.batch.setProjectionMatrix(uiCamera.combined);
        game.batch.begin();
        game.batch.draw(
            region,
            cursorUnprojectTmp.x - w * 0.5f,
            cursorUnprojectTmp.y - h * 0.5f,
            w, h);
        game.batch.end();
    }

    private void autoCollectLoot(GameModel model) {
        if (model == null || coinHud == null) {
            return;
        }
        List<LootPickup> pending = model.getActiveLootPickups();
        if (pending == null || pending.isEmpty()) {
            return;
        }
        for (LootPickup loot : new ArrayList<>(pending)) {
            model.removeLootPickup(loot);
            entityRenderer.writeLootDrawPos(loot, sunPosTmp);
            float x0 = sunPosTmp[0];
            float y0 = sunPosTmp[1];
            coinHud.logoCenter(logoTmp);
            entityRenderer.startLootCollect(
                loot, x0, y0, logoTmp.x, logoTmp.y,
                () -> {
                    model.applyLootPickup(loot);
                    coinHud.setAmount(model.getCoinCount());
                    if (lootRewardPopup != null) {
                        lootRewardPopup.show(loot);
                    }
                });
        }
    }

    private static boolean shovelEnabled(GameModel model) {
        if (model == null) {
            return true;
        }
        Level level = model.getCurrentLevel();
        if (level == null || level.getConfig() == null || level.getConfig().getRules() == null) {
            return true;
        }
        return level.getConfig().getRules().isShovelEnabled();
    }

    private static List<String> selectedPlants() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null || model.getSelectedPlants() == null) {
            return List.of();
        }
        return model.getSelectedPlants();
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
        User user = App.getInstance().getCurrentUser();
        Map<String, Integer> levels = user == null ? null : user.getPlantLevels();
        if (name == null || levels == null) {
            return 1;
        }
        Integer level = levels.get(name);
        return level == null || level < 1 ? 1 : level;
    }

    private static boolean boosted(String name) {
        User user = App.getInstance().getCurrentUser();
        Map<String, Boolean> boosts = user == null ? null : user.getPlantBoosts();
        return name != null && boosts != null && Boolean.TRUE.equals(boosts.get(name));
    }

    @Override
    public void hide() {
        // If the player leaves gameplay while the plant-food cursor is armed,
        // restore the OS cursor so the rest of the app is usable.
        if (plantfoodMode) {
            setPlantfoodMode(false);
        }
        if (shovelMode) {
            setShovelMode(false);
        }
        super.hide();
    }

    @Override
    public void dispose() {
        entityOverlay.dispose();
        if (rowColHighlight != null) {
            rowColHighlight.dispose();
        }
        restoreOsCursor();
        if (hiddenCursor != null) {
            hiddenCursor.dispose();
            hiddenCursor = null;
        }
        super.dispose();
    }
}
