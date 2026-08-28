package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
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
import controller.GameMenuController;
import controller.GameplayMenuController;
import controller.MainMenuController;
import controller.PlantSelectionMenuController;
import controller.TravelLogMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.GameState;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;
import model.game.level.minigame.beghouled.BeghouledLevel;
import model.game.level.minigame.beghouled.BeghouledSettings;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.minigame.vasebreaker.PendingSeedPacket;
import model.game.level.minigame.vasebreaker.Vase;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.level.special.PlantWhatYouGetLevel;
import model.game.level.special.ScoreLevel;
import model.game.score.MyopointTracker;
import model.item.LootPickup;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.plant.PlantFactory;
import model.user.User;
import view.gui.PvzGdxGame;
import view.gui.anim.AnimScale;
import view.gui.anim.bowling.BowlingWalnutAnim;
import view.gui.anim.vase.VaseBreakerAnim;
import view.gui.assets.ZombiePacketIds;
import view.gui.lawn.BrainLaneRenderer;
import view.gui.assets.AdventureHudRegions;
import view.gui.lawn.DeadLineRenderer;
import view.gui.lawn.DebugEntityOverlay;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnEntityRenderer;
import view.gui.lawn.LawnGridRenderer;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.LawnRowColHighlight;
import view.gui.lawn.NecromancyTileRenderer;
import view.gui.lawn.WaterUnderlayerRenderer;
import view.gui.ui.CoinHud;
import view.gui.ui.BeghouledMatchHud;
import view.gui.ui.LootRewardPopup;
import view.gui.ui.LoseResultsOverlay;
import view.gui.ui.LoveYourPlantsHud;
import view.gui.ui.MyopointAwardFeed;
import view.gui.ui.MyopointHud;
import view.gui.ui.MyopointResultsOverlay;
import view.gui.ui.PauseMenuOverlay;
import view.gui.ui.PlantFoodBankHud;
import view.gui.ui.ReadySetPlantBanner;
import view.gui.ui.SeedPacketActor;
import view.gui.ui.SkinFonts;
import view.gui.ui.SunHud;
import view.gui.ui.WaveAnnounceBanner;
import view.gui.ui.WaveProgressHud;
import view.gui.ui.WinResultsOverlay;
import view.gui.ui.ZombiePacketActor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private BeghouledMatchHud beghouledMatchHud;
    private LoveYourPlantsHud loveYourPlantsHud;
    private MyopointHud myopointHud;
    private MyopointAwardFeed myopointAwardFeed;
    private PlantFoodBankHud plantFoodBank;
    private LootRewardPopup lootRewardPopup;
    private ReadySetPlantBanner readySetPlant;
    private WaveAnnounceBanner waveAnnounce;
    private WaveProgressHud waveProgress;
    private Table packetColumn;
    private LawnRowColHighlight rowColHighlight;
    private NecromancyTileRenderer necromancyTiles;
    private LawnGridRenderer lawnGridRenderer;
    private DeadLineRenderer deadLineRenderer;
    private BrainLaneRenderer brainLaneRenderer;
    private String previewPlant;
    private float previewTime;
    private int hoverCol = -1;
    private int hoverRow = -1;
    private List<String> shownPackets = List.of();
    private final boolean bowlingMode;
    private final boolean vaseBreakerMode;
    private final boolean beghouledMode;
    private final boolean iZombieMode;
    private final boolean scoreMode;
    private int swapFromCol = -1;
    private int swapFromRow = -1;
    private boolean swapDragging;

    private boolean plantfoodMode;
    private boolean shovelMode;
    private boolean pauseMenuOpen;
    private boolean endSequenceActive;
    private LoseResultsOverlay loseOverlay;
    private WinResultsOverlay winOverlay;
    private MyopointResultsOverlay myopointResultsOverlay;
    private final List<Actor> hudRoots = new ArrayList<>();
    private ImageButton shovelButton;
    private ImageButton pauseButton;
    private Table pauseOverlay;
    private Cursor hiddenCursor;
    private TextureRegion plantfoodCursorRegion;
    private TextureRegion shovelCursorRegion;
    private final Vector3 cursorUnprojectTmp = new Vector3();

    private static final String SHOVEL_CURSOR_ID = "IMAGE_UI_HUD_INGAME_SHOVEL_ICON";
    /** Native 768 {@code IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON} size. */
    private static final float SHOVEL_SIZE = 79f;
    /** Native 768 {@code image_ui_hud_ingame_pause_button} size. */
    private static final float PAUSE_BTN_SIZE = 70f;

    public GameplayScreen(PvzGdxGame game) {
        super(game);
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);
        lawnLayout = lawnLayout();
        bowlingMode = currentLevel() instanceof WallnutBowlingLevel;
        vaseBreakerMode = currentLevel() instanceof VaseBreakerLevel;
        beghouledMode = currentLevel() instanceof BeghouledLevel;
        iZombieMode = currentLevel() instanceof IZombieLevel;
        scoreMode = currentLevel() instanceof ScoreLevel;
        Chapter chapter = currentChapter();
        LawnBackgroundRenderer.Style lawnStyle = LawnBackgroundRenderer.Style.forChapter(chapter);
        lawnBackground = new LawnBackgroundRenderer(assets.textures, lawnStyle);
        lawnBackground.ensureLoaded();
        waterUnderlayer = chapter == Chapter.BIG_WAVE_BEACH
            ? new WaterUnderlayerRenderer(assets, lawnLayout)
            : null;
        if (chapter == Chapter.DARK_AGES) {
            necromancyTiles = new NecromancyTileRenderer();
        }
        entityOverlay = new DebugEntityOverlay(lawnLayout, resolveFont());
        entityRenderer = new LawnEntityRenderer(assets, lawnLayout, entityOverlay);
        entityRenderer.setScreenShake(screenShake);
        entityRenderer.resetMowers(chapter, lawnMowersEnabled());
        if (vaseBreakerMode) {
            entityRenderer.preloadVases();
        }
        entityRenderer.preloadCraters();
        if (bowlingMode || iZombieMode || deadLineColumn() >= 0) {
            deadLineRenderer = new DeadLineRenderer();
        }
        if (iZombieMode) {
            brainLaneRenderer = new BrainLaneRenderer(assets.textures);
            brainLaneRenderer.ensureLoaded();
            assets.textures.loadSync(ZombiePacketIds.ATLAS_GROUP);
            assets.textures.loadSync(ZombiePacketIds.ATLAS_PAGE);
        }
        assets.textures.loadSync("UI_SeedPackets_768");
        assets.textures.loadSync("ATLASIMAGE_ATLAS_UI_SEEDPACKETS_768_00");

        assets.textures.loadSync(PlantFoodBankHud.ATLAS_GROUP);
        assets.textures.loadSync(PlantFoodBankHud.ATLAS_PAGE_0);
        assets.textures.loadSync(PlantFoodBankHud.ATLAS_PAGE_1);
        assets.textures.loadSync(AdventureHudRegions.ATLAS_WORLD_MAP);
        assets.textures.loadSync(AdventureHudRegions.ATLAS_ALWAYS_LOADED);
        assets.textures.loadSync("ZENGARDENGROUP_768");
        assets.textures.loadSync("ATLASIMAGE_ATLAS_ZENGARDENGROUP_768_00");
        assets.textures.loadSync(PauseMenuOverlay.ATLAS_GROUP);
        assets.textures.loadSync(PauseMenuOverlay.ATLAS_PAGE);
        if (beghouledMode) {
            setWorldInput(createBeghouledWorldInput());
        } else {
            setWorldInput(createWorldClickInput(lawnLayout, this::onWorldClick, this::onCellHover));
        }
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
        packetColumn = new Table();
        if (iZombieMode) {
            Table topRow = new Table();
            if (sunBank) {
                sunHud = new SunHud(skin);
                sunHud.setAmount(model == null ? 0 : model.getSunAmount());
                topRow.add(sunHud).left().padRight(10f);
            }
            topRow.add(packetColumn).left().top();
            left.add(topRow).left().top();
        } else {
            if (sunBank) {
                sunHud = new SunHud(skin);
                sunHud.setAmount(model == null ? 0 : model.getSunAmount());
                Table sunRow = new Table();
                sunRow.add(sunHud).left();
                if (LoveYourPlantsHud.showFor(model)) {
                    loveYourPlantsHud = new LoveYourPlantsHud(skin, assets.textures);
                    loveYourPlantsHud.sync(model);
                    sunRow.add(loveYourPlantsHud).left().padLeft(8f);
                }
                if (model != null && model.getCurrentLevel() instanceof PlantWhatYouGetLevel lastStand
                        && lastStand.isSetupPhase()) {
                    TextButton letsRock = new TextButton("LET'S ROCK!", skin, "purple");
                    SkinFonts.scaleButton(letsRock, skin, "purple", 0.95f);
                    letsRock.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            lastStand.startWaves();
                            letsRock.remove();
                            if (waveProgress != null) {
                                waveProgress.setVisible(true);
                                waveProgress.sync(model);
                            }
                            if (readySetPlant != null) {
                                readySetPlant.play();
                            }
                        }
                    });
                    sunRow.add(letsRock).height(46f).padLeft(8f);
                }
                left.add(sunRow).left().padBottom(8f).row();
            } else if (LoveYourPlantsHud.showFor(model)) {
                loveYourPlantsHud = new LoveYourPlantsHud(skin, assets.textures);
                loveYourPlantsHud.sync(model);
                Table loveRow = new Table();
                loveRow.add(loveYourPlantsHud).left();
                left.add(loveRow).left().padBottom(8f).row();
            }
            left.add(packetColumn).left().top();
        }
        uiStage.addActor(left);
        hudRoots.add(left);

        Table topRight = new Table();
        topRight.setFillParent(true);
        topRight.setTouchable(Touchable.childrenOnly);
        topRight.top().right().pad(12f);

        coinHud = new CoinHud(skin, assets.textures);
        coinHud.setAmount(currentTotalCoins());

        pauseButton = new ImageButton(skin, "ingame_pause");
        pauseButton.setProgrammaticChangeEvents(false);
        pauseButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openPauseMenu();
            }
        });

        Table coinRow = new Table();
        coinRow.add(coinHud).padRight(8f);
        coinRow.add(pauseButton).size(PAUSE_BTN_SIZE);
        topRight.add(coinRow).right();
        uiStage.addActor(topRight);
        hudRoots.add(topRight);

        if (WaveProgressHud.showFor(model) || BeghouledMatchHud.showFor(model) || MyopointHud.showFor(model)) {
            Table topCenter = new Table();
            topCenter.setFillParent(true);
            topCenter.setTouchable(Touchable.childrenOnly);
            topCenter.top().padTop(8f);
            boolean stacked = false;
            if (WaveProgressHud.showFor(model)) {
                waveProgress = new WaveProgressHud(skin, assets.textures);
                boolean inSetup = model != null && model.getCurrentLevel() instanceof PlantWhatYouGetLevel lastStand
                        && lastStand.isSetupPhase();
                waveProgress.setVisible(!inSetup);
                topCenter.add(waveProgress).top().row();
                stacked = true;
            } else if (BeghouledMatchHud.showFor(model)) {
                beghouledMatchHud = new BeghouledMatchHud(skin);
                beghouledMatchHud.sync(model);
                topCenter.add(beghouledMatchHud).top().row();
                stacked = true;
            }
            if (MyopointHud.showFor(model)) {
                myopointHud = new MyopointHud(skin);
                myopointHud.sync(model);
                topCenter.add(myopointHud).top().padTop(stacked ? 4f : 0f).row();
                myopointAwardFeed = new MyopointAwardFeed(skin);
                topCenter.add(myopointAwardFeed).top().padTop(6f);
            }
            uiStage.addActor(topCenter);
            hudRoots.add(topCenter);
        }

        lootRewardPopup = new LootRewardPopup(skin);
        Table rewardAnchor = new Table();
        rewardAnchor.setFillParent(true);
        rewardAnchor.top().padTop(72f);
        rewardAnchor.add(lootRewardPopup).top();
        uiStage.addActor(rewardAnchor);
        hudRoots.add(rewardAnchor);

        if (plantFoodHudEnabled(model)) {
            plantFoodBank = new PlantFoodBankHud(skin, assets.textures);
            plantFoodBank.onPlantFoodButton(() -> setPlantfoodMode(!plantfoodMode));
            Table bottomLeft = new Table();
            bottomLeft.setFillParent(true);
            bottomLeft.setTouchable(Touchable.childrenOnly);
            bottomLeft.bottom().left().pad(8f);
            bottomLeft.add(plantFoodBank).left().bottom();
            uiStage.addActor(bottomLeft);
            hudRoots.add(bottomLeft);
        }

        buildBottomRight(model);

        readySetPlant = new ReadySetPlantBanner(skin);
        uiStage.addActor(readySetPlant);
        boolean inSetup = model != null && model.getCurrentLevel() instanceof PlantWhatYouGetLevel lastStand
                && lastStand.isSetupPhase();
        if (!inSetup) {
            readySetPlant.play();
        }

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

            TextButton nuke = new TextButton("Nuke", skin, "brown");
            nuke.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    CommandResult<Void> result = gameplay.releaseNuke();
                    showToast(result.getMessage(), !result.isSuccess());
                }
            });

            Table cheats = new Table();
            cheats.add(addSun).width(160f).height(44f).padRight(8f);
            cheats.add(addPlantFood).width(180f).height(44f).padRight(8f);
            cheats.add(nuke).width(100f).height(44f);
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
        hudRoots.add(bottomRight);
    }

    private void refreshPackets() {
        if (beghouledMode) {
            refreshBeghouledUpgrades();
            return;
        }
        if (iZombieMode) {
            refreshIZombiePackets();
            return;
        }
        List<String> selected = hudPlantNames();
        shownPackets = new ArrayList<>(selected);
        packetColumn.clearChildren();
        List<PendingSeedPacket> pending = vaseBreakerMode ? pendingPackets() : List.of();
        int pendingIndex = 0;
        for (String name : selected) {
            SeedPacketActor packet = (bowlingMode || vaseBreakerMode)
                ? new SeedPacketActor(assets.textures, skin, name, 0, 1, false, false, false)
                : new SeedPacketActor(
                    assets.textures, skin, name, plantCost(name), plantLevel(name),
                    boosted(name), false);
            if (vaseBreakerMode) {
                packet.enableExpiryTimer(skin);
                if (pendingIndex < pending.size()) {
                    packet.setExpirySeconds(pending.get(pendingIndex).getTimeToExpiry());
                }
                pendingIndex++;
            }
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

    private void refreshIZombiePackets() {
        List<String> roster = iZombieRosterNames();
        shownPackets = new ArrayList<>(roster);
        packetColumn.clearChildren();
        Map<String, Integer> costs = iZombieRosterCosts();
        for (String name : roster) {
            int cost = costs.getOrDefault(name, 0);
            ZombiePacketActor packet = new ZombiePacketActor(assets.textures, skin, name, cost);
            packet.onDragZombie(new ZombiePacketActor.DragZombie() {
                @Override
                public void dragStart(ZombiePacketActor packet) {
                    previewPlant = name;
                    previewTime = 0f;
                    entityRenderer.preloadZombieIdle(name, currentChapter());
                    stageToScreen.set(packet.getWidth() * 0.5f, packet.getHeight() * 0.5f);
                    packet.localToStageCoordinates(stageToScreen);
                    followPlantDrag(stageToScreen.x, stageToScreen.y);
                }

                @Override
                public void drag(ZombiePacketActor packet, float stageX, float stageY) {
                    followPlantDrag(stageX, stageY);
                }

                @Override
                public void dragEnd(ZombiePacketActor packet, float stageX, float stageY) {
                    dropZombie(name, stageX, stageY);
                    previewPlant = null;
                    hoverCol = -1;
                    hoverRow = -1;
                }
            });
            packetColumn.add(packet)
                .size(ZombiePacketActor.PACKET_WIDTH, ZombiePacketActor.PACKET_HEIGHT)
                .padRight(6f);
        }
        refreshPacketChrome();
    }

    private void refreshBeghouledUpgrades() {
        List<String> names = beghouledUpgradeFromNames();
        shownPackets = new ArrayList<>(names);
        packetColumn.clearChildren();
        Level level = currentLevel();
        if (!(level instanceof BeghouledLevel beghouled)) {
            return;
        }
        for (BeghouledSettings.UpgradeRule rule : beghouled.getSettings().getUpgrades()) {
            String from = rule.getFrom();
            SeedPacketActor packet = new SeedPacketActor(
                assets.textures, skin, from, rule.getCost(), 1, false, false, true);
            packet.onClick(() -> tryBeghouledUpgrade(from));
            packetColumn.add(packet)
                .size(SeedPacketActor.PACKET_WIDTH, SeedPacketActor.PACKET_HEIGHT)
                .padBottom(6f).row();
        }
        refreshPacketChrome();
    }

    private void tryBeghouledUpgrade(String fromType) {
        if (isPregame() || endSequenceActive) {
            return;
        }
        CommandResult<Void> result = gameplay.upgradePlant(fromType);
        showToast(result.getMessage(), !result.isSuccess());
        GameModel model = App.getInstance().getCurrentGameModel();
        if (sunHud != null && model != null) {
            sunHud.setAmount(model.getSunAmount());
        }
        if (beghouledMatchHud != null) {
            beghouledMatchHud.sync(model);
        }
        refreshPacketChrome();
    }

    private void refreshPacketChrome() {
        GameModel model = App.getInstance().getCurrentGameModel();
        int sun = model == null ? 0 : model.getSunAmount();
        if (beghouledMode) {
            Level level = currentLevel();
            Map<String, Integer> costs = beghouledUpgradeCosts(level);
            for (Actor actor : packetColumn.getChildren()) {
                if (!(actor instanceof SeedPacketActor packet) || packet.plantName() == null) {
                    continue;
                }
                Integer cost = costs.get(packet.plantName());
                packet.setDimmed(cost != null && cost > sun);
            }
            return;
        }
        if (iZombieMode) {
            Map<String, Integer> costs = iZombieRosterCosts();
            for (Actor actor : packetColumn.getChildren()) {
                if (!(actor instanceof ZombiePacketActor packet) || packet.zombieName() == null) {
                    continue;
                }
                Integer cost = costs.get(packet.zombieName());
                packet.setDimmed(cost != null && cost > sun);
            }
            return;
        }
        List<PendingSeedPacket> pending = vaseBreakerMode ? pendingPackets() : List.of();
        int pendingIndex = 0;
        for (Actor actor : packetColumn.getChildren()) {
            if (!(actor instanceof SeedPacketActor packet) || packet.plantName() == null) {
                continue;
            }
            String name = packet.plantName();
            if (bowlingMode || vaseBreakerMode) {
                packet.setDimmed(false);
                if (vaseBreakerMode && pendingIndex < pending.size()) {
                    packet.setExpirySeconds(pending.get(pendingIndex).getTimeToExpiry());
                }
                pendingIndex++;
                continue;
            }
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
        if (bowlingMode || vaseBreakerMode) {
            refreshPackets();
        } else {
            refreshPacketChrome();
        }
    }

    private void dropZombie(String zombieName, float stageX, float stageY) {
        if (isPregame()) {
            return;
        }
        stageToWorld(stageX, stageY);
        if (!lawnLayout.worldToCell(worldTmp.x, worldTmp.y, cellTmp)) {
            return;
        }
        CommandResult<Void> result = gameplay.placeZombie(zombieName, cellTmp[0], cellTmp[1]);
        showToast(result.getMessage(), !result.isSuccess());
        GameModel model = App.getInstance().getCurrentGameModel();
        if (sunHud != null && model != null) {
            sunHud.setAmount(model.getSunAmount());
        }
        refreshPacketChrome();
    }

    private boolean onWorldClick(float worldX, float worldY) {
        if (endSequenceActive) {
            return true;
        }
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
        if (vaseBreakerMode && tryBreakVase(worldX, worldY)) {
            return true;
        }
        return tryCollectPlantFood(worldX, worldY) || tryCollectSun(worldX, worldY);
    }

    private boolean tryBreakVase(float worldX, float worldY) {
        if (isPregame()) {
            return false;
        }
        if (!lawnLayout.worldToCell(worldX, worldY, cellTmp)) {
            return false;
        }
        Level level = currentLevel();
        if (!(level instanceof VaseBreakerLevel vaseLevel)) {
            return false;
        }
        int col = cellTmp[0];
        int row = cellTmp[1];
        Vase vase = vaseLevel.vaseAt(col, row);
        if (vase == null) {
            return false;
        }
        String pam = VaseBreakerAnim.pamPath(vase);
        CommandResult<Void> result = gameplay.breakVase(col, row);
        showToast(result.getMessage(), !result.isSuccess());
        if (result.isSuccess()) {
            entityRenderer.playVaseBreak(pam, col, row);
            refreshPackets();
        }
        return true;
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

    private int currentTotalCoins() {
        User user = App.getInstance().getCurrentUser();
        if (user != null) {
            return user.getCoins();
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        return model == null ? 0 : model.getCoinCount();
    }

    private void flushPendingLoot() {
        if (entityRenderer != null) {
            entityRenderer.drainPendingLootFlights();
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model != null) {
            List<LootPickup> pending = model.getActiveLootPickups();
            if (pending != null && !pending.isEmpty()) {
                for (LootPickup loot : new ArrayList<>(pending)) {
                    model.applyLootPickup(loot);
                    model.removeLootPickup(loot);
                }
            }
        }
        if (coinHud != null) {
            coinHud.setAmount(currentTotalCoins());
        }
    }

    private void exitToLevels() {
        flushPendingLoot();
        Level level = currentLevel();
        App.getInstance().setCurrentGameModel(null);
        App.getInstance().setCurrentGameLoop(null);
        if (scoreMode || level instanceof ScoreLevel) {
            App.getInstance().setCurrentMenu(MenuType.GAME);
            game.setScreen(new AdventureScreen(game));
            return;
        }
        if (bowlingMode || level instanceof MiniGameLevel) {
            App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);
            game.setScreen(new MiniGameScreen(game));
            return;
        }
        Chapter chapter = currentChapter();
        App.getInstance().setCurrentMenu(MenuType.GAME);
        if (chapter != null) {
            game.setScreen(new ChapterLevelsScreen(game, chapter));
        } else {
            game.setScreen(new AdventureScreen(game));
        }
    }

    /** After a win: load the next level in this chapter, or fall back to the map. */
    private void continueToNextLevel() {
        Level level = currentLevel();
        if (scoreMode || level instanceof ScoreLevel) {
            exitToLevels();
            return;
        }
        if (level instanceof MiniGameLevel mini) {
            String type = mini.getMiniGameType().name();
            int nextStage = mini.getStage() + 1;
            CommandResult<Void> enter = TravelLogMenuController.getInstance()
                .enterMiniGame(type, nextStage);
            if (!enter.isSuccess()) {
                exitToLevels();
                return;
            }
            game.setScreen(new LevelObjectivesScreen(game, null));
            return;
        }
        Chapter chapter = currentChapter();
        LevelConfig config = level == null ? null : level.getConfig();
        if (chapter == null || config == null) {
            exitToLevels();
            return;
        }
        int nextId = config.getLevelId() + 1;
        String chapterArg = chapter.name().toLowerCase(Locale.ROOT);
        CommandResult<Void> enter = GameMenuController.getInstance().enterChapter(chapterArg, nextId);
        if (!enter.isSuccess()) {
            exitToLevels();
            return;
        }
        game.setScreen(new LevelObjectivesScreen(game, chapter));
    }

    private void openPauseMenu() {
        if (pauseMenuOpen || endSequenceActive) {
            return;
        }
        pauseMenuOpen = true;
        if (pauseButton != null) {
            pauseButton.setChecked(true);
        }
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop != null && loop.getGameState() == GameState.RUNNING) {
            loop.pause();
        }
        LevelConfig config = currentLevel() == null ? null : currentLevel().getConfig();
        pauseOverlay = PauseMenuOverlay.create(
            skin, assets.textures, config,
            this::closePauseMenu,
            this::restartLevel,
            this::exitToLevels);
        uiStage.addActor(pauseOverlay);
        toast.toFront();
    }

    private void closePauseMenu() {
        if (!pauseMenuOpen) {
            return;
        }
        pauseMenuOpen = false;
        if (pauseOverlay != null) {
            pauseOverlay.remove();
            pauseOverlay = null;
        }
        if (pauseButton != null) {
            pauseButton.setChecked(false);
        }
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        if (loop != null && loop.getGameState() == GameState.PAUSED) {
            loop.resume();
        }
    }

    private void restartLevel() {
        flushPendingLoot();
        Level level = currentLevel();
        if (scoreMode || level instanceof ScoreLevel) {
            restartScoreGame();
            return;
        }
        if (level instanceof MiniGameLevel mini) {
            restartMiniGame(mini);
            return;
        }
        Chapter chapter = currentChapter();
        LevelConfig config = level == null ? null : level.getConfig();
        if (chapter == null || config == null) {
            showToast("Cannot restart: no level loaded.", true);
            return;
        }
        List<String> plants = new ArrayList<>(selectedPlants());
        int levelId = config.getLevelId();
        String chapterArg = chapter.name().toLowerCase(Locale.ROOT);
        CommandResult<Void> enter = GameMenuController.getInstance().enterChapter(chapterArg, levelId);
        if (!enter.isSuccess()) {
            showToast(enter.getMessage(), true);
            return;
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model != null) {
            model.setSelectedPlants(plants);
        }
        CommandResult<Void> start = PlantSelectionMenuController.getInstance().startGame();
        if (!start.isSuccess()) {
            showToast(start.getMessage(), true);
            return;
        }
        game.setScreen(new GameplayScreen(game));
    }

    private void restartScoreGame() {
        List<String> plants = new ArrayList<>(selectedPlants());
        CommandResult<Void> enter = MainMenuController.getInstance().enterScoreGame();
        if (!enter.isSuccess()) {
            showToast(enter.getMessage(), true);
            return;
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model != null) {
            model.setSelectedPlants(plants);
        }
        CommandResult<Void> start = PlantSelectionMenuController.getInstance().startGame();
        if (!start.isSuccess()) {
            showToast(start.getMessage(), true);
            return;
        }
        game.setScreen(new GameplayScreen(game));
    }

    private void restartMiniGame(MiniGameLevel mini) {
        String type = mini.getMiniGameType().name();
        int stage = mini.getStage();
        CommandResult<Void> enter = TravelLogMenuController.getInstance().enterMiniGame(type, stage);
        if (!enter.isSuccess()) {
            showToast(enter.getMessage(), true);
            return;
        }
        CommandResult<Void> start = PlantSelectionMenuController.getInstance().startGame();
        if (!start.isSuccess()) {
            showToast(start.getMessage(), true);
            return;
        }
        game.setScreen(new GameplayScreen(game));
    }

    @Override
    protected boolean freezeWorld() {
        // Pause freezes PAM; win/lose keep lawn anims running under the dim.
        return pauseMenuOpen;
    }

    @Override
    protected void updateLogic(float delta) {
        if (pauseMenuOpen) {
            return;
        }
        if (endSequenceActive) {
            entityRenderer.tickEndLevel(delta);
            if (loseOverlay != null && entityRenderer.isLoseFadeDone()) {
                loseOverlay.play();
            }
            if (winOverlay != null && entityRenderer.isWinFadeDone()) {
                winOverlay.play();
            }
            if (myopointResultsOverlay != null
                && (entityRenderer.isLoseFadeDone() || entityRenderer.isWinFadeDone())) {
                myopointResultsOverlay.play();
            }
            return;
        }
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
        maybeStartEndSequence(model);
        if (endSequenceActive) {
            entityRenderer.tickEndLevel(delta);
            if (winOverlay != null && entityRenderer.isWinFadeDone()) {
                winOverlay.play();
            }
            if (myopointResultsOverlay != null
                && (entityRenderer.isLoseFadeDone() || entityRenderer.isWinFadeDone())) {
                myopointResultsOverlay.play();
            }
            return;
        }
        if (model != null && waveAnnounce != null && !waveAnnounce.isPlaying()) {
            String waveText = model.consumeWaveAnnouncement();
            if (waveText != null) {
                waveAnnounce.show(waveText);
            }
        }
        if (waveProgress != null) {
            boolean inSetup = model != null && model.getCurrentLevel() instanceof PlantWhatYouGetLevel lastStand
                    && lastStand.isSetupPhase();
            waveProgress.setVisible(!inSetup);
            if (!inSetup) {
                waveProgress.sync(model);
            }
        }
        if (beghouledMatchHud != null) {
            beghouledMatchHud.sync(model);
        }
        if (loveYourPlantsHud != null) {
            loveYourPlantsHud.sync(model);
        }
        if (myopointHud != null) {
            myopointHud.sync(model);
        }
        syncMyopointAwards(model);
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
        if (!hudPlantNames().equals(shownPackets)) {
            refreshPackets();
        } else {
            refreshPacketChrome();
        }
    }

    private void syncMyopointAwards(GameModel model) {
        if (myopointAwardFeed == null || model == null) {
            return;
        }
        MyopointTracker tracker = null;
        if (model.getCurrentLevel() instanceof ScoreLevel scoreLevel) {
            tracker = scoreLevel.getTracker();
        } else if (model.getMyopointTracker() != null) {
            tracker = model.getMyopointTracker();
        }
        if (tracker != null) {
            myopointAwardFeed.push(tracker.drainAwardEvents());
        }
    }

    private void maybeStartEndSequence(GameModel model) {
        if (endSequenceActive || model == null) {
            return;
        }
        GameState state = model.getState();
        if (state == GameState.LOST) {
            startLoseSequence();
        } else if (state == GameState.WON) {
            startWinSequence();
        }
    }

    private void startLoseSequence() {
        flushPendingLoot();
        endSequenceActive = true;
        clearArmedModes();
        hideHud();
        if (pauseMenuOpen) {
            closePauseMenu();
        }
        entityRenderer.beginLoseFade();
        if (scoreMode && currentLevel() instanceof ScoreLevel scoreLevel) {
            myopointResultsOverlay = new MyopointResultsOverlay(
                skin, scoreLevel, false, this::restartLevel, this::exitToLevels);
            uiStage.addActor(myopointResultsOverlay);
        } else {
            loseOverlay = new LoseResultsOverlay(
                skin, assets.textures, this::restartLevel, this::exitToLevels);
            uiStage.addActor(loseOverlay);
        }
        toast.toFront();
    }

    private void startWinSequence() {
        flushPendingLoot();
        endSequenceActive = true;
        clearArmedModes();
        hideHud();
        if (pauseMenuOpen) {
            closePauseMenu();
        }
        entityRenderer.beginWinFade();
        if (scoreMode && currentLevel() instanceof ScoreLevel scoreLevel) {
            myopointResultsOverlay = new MyopointResultsOverlay(
                skin, scoreLevel, true, this::restartLevel, this::exitToLevels);
            uiStage.addActor(myopointResultsOverlay);
        } else {
            winOverlay = new WinResultsOverlay(
                skin, assets.textures, this::continueToNextLevel, this::exitToLevels);
            uiStage.addActor(winOverlay);
        }
        toast.toFront();
    }

    private void hideHud() {
        for (Actor root : hudRoots) {
            root.setVisible(false);
            root.setTouchable(Touchable.disabled);
        }
        if (readySetPlant != null) {
            readySetPlant.setVisible(false);
        }
        if (waveAnnounce != null) {
            waveAnnounce.setVisible(false);
        }
    }

    private void clearArmedModes() {
        if (plantfoodMode) {
            setPlantfoodMode(false);
        }
        if (shovelMode) {
            setShovelMode(false);
        }
        swapDragging = false;
        swapFromCol = -1;
        swapFromRow = -1;
        previewPlant = null;
        hoverCol = -1;
        hoverRow = -1;
    }

    @Override
    protected void renderWorld(float delta) {
        lawnBackground.draw(game.batch);
        if (waterUnderlayer != null) {
            waterUnderlayer.draw(game.batch, App.getInstance().getCurrentGameModel(), delta);
        }
        if (necromancyTiles != null) {
            necromancyTiles.draw(game.batch, lawnLayout,
                App.getInstance().getCurrentGameModel(), delta);
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model != null && model.isShowLawnGrid()) {
            if (lawnGridRenderer == null) {
                lawnGridRenderer = new LawnGridRenderer();
            }
            lawnGridRenderer.draw(game.batch, lawnLayout);
        }
        if (deadLineRenderer != null) {
            deadLineRenderer.draw(game.batch, lawnLayout, deadLineColumn());
        }
        if (brainLaneRenderer != null) {
            brainLaneRenderer.draw(game.batch, lawnLayout, App.getInstance().getCurrentGameModel());
        }
        boolean highlight = (previewPlant != null || plantfoodMode || shovelMode || swapDragging)
            && hoverCol >= 0;
        if (highlight && bowlingMode && previewPlant != null && !canBowlAt(hoverCol)) {
            highlight = false;
        }
        if (highlight && iZombieMode && previewPlant != null && !canPlaceIZombieAt(hoverCol)) {
            highlight = false;
        }
        if (highlight) {
            if (rowColHighlight == null) {
                rowColHighlight = new LawnRowColHighlight();
            }
            rowColHighlight.draw(game.batch, lawnLayout, hoverCol, hoverRow);
        }
        entityRenderer.draw(game.batch, App.getInstance().getCurrentGameModel(), delta);
        if (previewPlant != null) {
            if (iZombieMode) {
                entityRenderer.drawZombieIdle(
                    game.batch, previewPlant, worldTmp.x, worldTmp.y, previewTime, currentChapter());
            } else {
                float scale = bowlingMode
                    ? BowlingWalnutAnim.scale(WallnutBowlingLevel.parseWalnutType(previewPlant))
                    : AnimScale.PLANT;
                entityRenderer.drawPlantIdle(
                    game.batch, previewPlant, worldTmp.x, worldTmp.y, previewTime, scale);
            }
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
                    coinHud.setAmount(currentTotalCoins());
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

    private static boolean plantFoodHudEnabled(GameModel model) {
        if (model == null) {
            return true;
        }
        if (model.getCurrentLevel() instanceof WallnutBowlingLevel
                || model.getCurrentLevel() instanceof VaseBreakerLevel
                || model.getCurrentLevel() instanceof BeghouledLevel
                || model.getCurrentLevel() instanceof IZombieLevel) {
            return false;
        }
        Level level = model.getCurrentLevel();
        if (level == null || level.getConfig() == null || level.getConfig().getRules() == null) {
            return true;
        }
        return level.getConfig().getRules().isPlantFoodDrops();
    }

    private static int deadLineColumn() {
        Level level = currentLevel();
        if (level instanceof IZombieLevel iZombie) {
            return iZombie.redLineColumn();
        }
        if (level == null || level.getConfig() == null) {
            return -1;
        }
        int line = level.getConfig().getDeadLineColumn();
        if (line < 0 && level.getConfig().getRules() != null) {
            line = level.getConfig().getRules().getDeadLineColumn();
        }
        return line;
    }

    private boolean canBowlAt(int col) {
        Level level = currentLevel();
        if (level instanceof WallnutBowlingLevel bowling) {
            return bowling.canLaunchAtColumn(col);
        }
        return true;
    }

    private boolean canPlaceIZombieAt(int col) {
        Level level = currentLevel();
        if (level instanceof IZombieLevel iZombie) {
            return col >= iZombie.redLineColumn();
        }
        return true;
    }

    private List<String> hudPlantNames() {
        if (beghouledMode) {
            return beghouledUpgradeFromNames();
        }
        if (iZombieMode) {
            return iZombieRosterNames();
        }
        if (vaseBreakerMode) {
            List<String> names = new ArrayList<>();
            for (PendingSeedPacket packet : pendingPackets()) {
                if (packet.getPlant() != null && packet.getPlant().getName() != null) {
                    names.add(packet.getPlant().getName());
                }
            }
            return names;
        }
        return selectedPlants();
    }

    private static List<String> iZombieRosterNames() {
        Level level = currentLevel();
        if (!(level instanceof IZombieLevel iZombie)) {
            return List.of();
        }
        return new ArrayList<>(iZombie.getSettings().getZombieCosts().keySet());
    }

    private static Map<String, Integer> iZombieRosterCosts() {
        Map<String, Integer> costs = new java.util.LinkedHashMap<>();
        Level level = currentLevel();
        if (!(level instanceof IZombieLevel iZombie)) {
            return costs;
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        float penalty = model == null ? 1f : model.difficultyPenalty();
        for (Map.Entry<String, Integer> e : iZombie.getSettings().getZombieCosts().entrySet()) {
            costs.put(e.getKey(), (int) (e.getValue() * penalty));
        }
        return costs;
    }

    private static List<String> beghouledUpgradeFromNames() {
        Level level = currentLevel();
        if (!(level instanceof BeghouledLevel beghouled)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (BeghouledSettings.UpgradeRule rule : beghouled.getSettings().getUpgrades()) {
            names.add(rule.getFrom());
        }
        return names;
    }

    private static Map<String, Integer> beghouledUpgradeCosts(Level level) {
        Map<String, Integer> costs = new java.util.HashMap<>();
        if (!(level instanceof BeghouledLevel beghouled)) {
            return costs;
        }
        for (BeghouledSettings.UpgradeRule rule : beghouled.getSettings().getUpgrades()) {
            costs.put(rule.getFrom(), rule.getCost());
        }
        return costs;
    }

    private InputProcessor createBeghouledWorldInput() {
        return new InputAdapter() {
            private final int[] cell = new int[2];

            private void updateHover(int screenX, int screenY) {
                worldViewport.unproject(worldTmp.set(screenX, screenY, 0f));
                if (!lawnLayout.worldToCell(worldTmp.x, worldTmp.y, cell)) {
                    hoverCol = -1;
                    hoverRow = -1;
                    return;
                }
                hoverCol = cell[0];
                hoverRow = cell[1];
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (swapDragging) {
                    updateHover(screenX, screenY);
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (swapDragging) {
                    updateHover(screenX, screenY);
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                worldViewport.unproject(worldTmp.set(screenX, screenY, 0f));
                if (onWorldClick(worldTmp.x, worldTmp.y)) {
                    return true;
                }
                if (isPregame() || endSequenceActive || pauseMenuOpen) {
                    return false;
                }
                if (!lawnLayout.worldToCell(worldTmp.x, worldTmp.y, cell)) {
                    return false;
                }
                Level level = currentLevel();
                if (!(level instanceof BeghouledLevel beghouled)) {
                    return false;
                }
                if (beghouled.plantAt(cell[1], cell[0]) == null || beghouled.isCrater(cell[1], cell[0])) {
                    return false;
                }
                swapDragging = true;
                swapFromCol = cell[0];
                swapFromRow = cell[1];
                hoverCol = cell[0];
                hoverRow = cell[1];
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (!swapDragging) {
                    return false;
                }
                swapDragging = false;
                worldViewport.unproject(worldTmp.set(screenX, screenY, 0f));
                int fromCol = swapFromCol;
                int fromRow = swapFromRow;
                swapFromCol = -1;
                swapFromRow = -1;
                hoverCol = -1;
                hoverRow = -1;
                if (!lawnLayout.worldToCell(worldTmp.x, worldTmp.y, cell)) {
                    return true;
                }
                tryBeghouledSwap(fromCol, fromRow, cell[0], cell[1]);
                return true;
            }
        };
    }

    private void tryBeghouledSwap(int fromCol, int fromRow, int toCol, int toRow) {
        if (fromCol == toCol && fromRow == toRow) {
            return;
        }
        int dc = toCol - fromCol;
        int dr = toRow - fromRow;
        String direction;
        if (dc == 1 && dr == 0) {
            direction = "right";
        } else if (dc == -1 && dr == 0) {
            direction = "left";
        } else if (dc == 0 && dr == 1) {
            direction = "down";
        } else if (dc == 0 && dr == -1) {
            direction = "up";
        } else {
            showToast("Swap with an adjacent plant.", true);
            return;
        }
        CommandResult<Void> result = gameplay.swapPlant(fromCol, fromRow, direction);
        showToast(result.getMessage(), !result.isSuccess());
        GameModel model = App.getInstance().getCurrentGameModel();
        if (sunHud != null && model != null) {
            sunHud.setAmount(model.getSunAmount());
        }
        if (beghouledMatchHud != null) {
            beghouledMatchHud.sync(model);
        }
        refreshPacketChrome();
    }

    private static List<PendingSeedPacket> pendingPackets() {
        Level level = currentLevel();
        if (level instanceof VaseBreakerLevel vaseBreaker) {
            return vaseBreaker.getPendingSeedPackets();
        }
        return List.of();
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
        flushPendingLoot();
        super.hide();
    }

    @Override
    public void dispose() {
        entityOverlay.dispose();
        if (rowColHighlight != null) {
            rowColHighlight.dispose();
        }
        if (necromancyTiles != null) {
            necromancyTiles.dispose();
        }
        if (lawnGridRenderer != null) {
            lawnGridRenderer.dispose();
            lawnGridRenderer = null;
        }
        if (deadLineRenderer != null) {
            deadLineRenderer.dispose();
        }
        restoreOsCursor();
        if (hiddenCursor != null) {
            hiddenCursor.dispose();
            hiddenCursor = null;
        }
        super.dispose();
    }
}
