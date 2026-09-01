package view.gui.screen.gameplay;

import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import controller.GameplayMenuController;
import model.app.App;
import model.game.level.minigame.beghouled.BeghouledLevel;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.level.special.ConveyorBeltLevel;
import model.game.level.special.SaveOurSeedsLevel;
import model.game.level.special.ScoreLevel;
import model.network.client.NetworkClient;
import model.network.enums.PlayerRole;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.matchmaking.MatchFoundPacket;
import model.network.util.GameStateSnapshotApplier;
import model.user.User;
import view.gui.anim.PamClipCache;
import view.gui.anim.SpritesheetClipCache;
import view.gui.audio.GameplayMusic;
import view.gui.lawn.BrainLaneRenderer;
import view.gui.lawn.DeadLineRenderer;
import view.gui.lawn.DebugEntityOverlay;
import view.gui.lawn.LawnBackgroundRenderer;
import view.gui.lawn.LawnEntityRenderer;
import view.gui.lawn.LawnGridRenderer;
import view.gui.lawn.LawnLayout;
import view.gui.lawn.LawnRowColHighlight;
import view.gui.lawn.NecromancyTileRenderer;
import view.gui.lawn.ProtectTileRenderer;
import view.gui.lawn.WaterUnderlayerRenderer;
import view.gui.ui.BeghouledMatchHud;
import view.gui.ui.CoinHud;
import view.gui.ui.ConveyorBeltHud;
import view.gui.ui.InviteReceivedOverlay;
import view.gui.ui.LootRewardPopup;
import view.gui.ui.LoseResultsOverlay;
import view.gui.ui.LoveYourPlantsHud;
import view.gui.ui.MyopointAwardFeed;
import view.gui.ui.MyopointHud;
import view.gui.ui.MyopointResultsOverlay;
import view.gui.ui.PlantFoodBankHud;
import view.gui.ui.ReactionBubbleWidget;
import view.gui.ui.ReadySetPlantBanner;
import view.gui.ui.SunHud;
import view.gui.ui.TimedProgressHud;
import view.gui.ui.WaveAnnounceBanner;
import view.gui.ui.WaveProgressHud;
import view.gui.ui.WinResultsOverlay;
import view.gui.ui.ZombossHpHud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Mutable lawn-session state shared by gameplay collaborators. */
public final class GameplayContext {
    public static final String SHOVEL_CURSOR_ID = "IMAGE_UI_HUD_INGAME_SHOVEL_ICON";
    public static final float SHOVEL_SIZE = 79f;
    public static final float PAUSE_BTN_SIZE = 70f;
    public static final float CURSOR_SIZE = 64f;

    public final GameplayView view;
    public final GameplayMenuController gameplay = GameplayMenuController.getInstance();
    public final Vector2 logoTmp = new Vector2();
    public final Vector2 stageToScreen = new Vector2();
    public final Vector3 worldTmp = new Vector3();
    public final float[] sunPosTmp = new float[2];
    public final int[] cellTmp = new int[2];
    public final Vector3 cursorUnprojectTmp = new Vector3();
    public final GameplayMusic gameplayMusic = new GameplayMusic();
    public final List<Actor> hudRoots = new ArrayList<>();

    public final boolean bowlingMode;
    public final boolean conveyorMode;
    public final boolean vaseBreakerMode;
    public final boolean beghouledMode;
    public final boolean iZombieMode;
    public final boolean couchPlayMode;
    public final boolean multiplayerMode;
    public final boolean multiplayerPlantSide;
    public final boolean useZombiePackets;
    public final boolean scoreMode;
    public final boolean saveOurSeedsMode;
    public final NetworkClient multiplayerClient;
    public final PlayerRole multiplayerRole;
    public final User multiplayerUser;
    public final String multiplayerOpponent;
    public final GameStateSnapshotApplier snapshotApplier;

    public LawnLayout lawnLayout;
    public LawnBackgroundRenderer lawnBackground;
    public WaterUnderlayerRenderer waterUnderlayer;
    public LawnEntityRenderer entityRenderer;
    public DebugEntityOverlay entityOverlay;
    public ProtectTileRenderer protectTileRenderer;
    public NecromancyTileRenderer necromancyTiles;
    public LawnGridRenderer lawnGridRenderer;
    public DeadLineRenderer deadLineRenderer;
    public BrainLaneRenderer brainLaneRenderer;
    public LawnRowColHighlight rowColHighlight;

    public SunHud sunHud;
    public SunHud zombieSunHud;
    public CoinHud coinHud;
    public BeghouledMatchHud beghouledMatchHud;
    public LoveYourPlantsHud loveYourPlantsHud;
    public MyopointHud myopointHud;
    public MyopointAwardFeed myopointAwardFeed;
    public PlantFoodBankHud plantFoodBank;
    public LootRewardPopup lootRewardPopup;
    public ReadySetPlantBanner readySetPlant;
    public WaveAnnounceBanner waveAnnounce;
    public WaveProgressHud waveProgress;
    public TimedProgressHud timedProgressHud;
    public ZombossHpHud zombossHpHud;
    public TimedProgressHud multiplayerMatchTimer;
    public Table packetColumn;
    public Table zombiePacketColumn;
    public ConveyorBeltHud conveyorHud;
    public SpritesheetClipCache sheetClips;
    public ImageButton shovelButton;
    public ImageButton pauseButton;
    public Table pauseOverlay;
    public InviteReceivedOverlay inviteOverlay;
    public ReactionBubbleWidget localReactionBubble;
    public ReactionBubbleWidget remoteReactionBubble;
    public PamClipCache reactionPamClips;
    public Consumer<ReactionPacket> reactionPacketHandler;
    public LoseResultsOverlay loseOverlay;
    public WinResultsOverlay winOverlay;
    public MyopointResultsOverlay myopointResultsOverlay;

    public String previewPlant;
    public float previewTime;
    public int hoverCol = -1;
    public int hoverRow = -1;
    public List<String> shownPackets = List.of();
    public int swapFromCol = -1;
    public int swapFromRow = -1;
    public boolean swapDragging;
    public boolean zombieDropMode;
    public String dropZombieName;
    public int dropCol = -1;
    public int dropRow = -1;
    public boolean plantfoodMode;
    public boolean shovelMode;
    public boolean pauseMenuOpen;
    public boolean invitePauseActive;
    public boolean endSequenceActive;
    public boolean wasPregame = true;
    public boolean multiplayerForfeitSent;
    public Cursor hiddenCursor;
    public TextureRegion plantfoodCursorRegion;
    public TextureRegion shovelCursorRegion;

    public GameplayPackets packets;
    public GameplayPlacement placement;
    public GameplayFlow flow;
    public GameplayLogic logic;
    public GameplayWorldRenderer worldRenderer;
    public GameplayCursors cursors;
    public GameplayHudBuilder hudBuilder;
    public GameplayLifecycle lifecycle;

    public GameplayContext(
            GameplayView view,
            NetworkClient networkClient,
            User user,
            MatchFoundPacket match,
            PlayerRole role
    ) {
        this.view = view;
        this.multiplayerClient = networkClient;
        this.multiplayerUser = user;
        this.multiplayerRole = role;
        this.multiplayerOpponent = match != null ? match.getOpponentUsername() : null;
        this.multiplayerMode = networkClient != null && role != null;
        this.multiplayerPlantSide = multiplayerMode && role == PlayerRole.PLANT;
        this.snapshotApplier = multiplayerMode ? new GameStateSnapshotApplier() : null;
        this.lawnLayout = GameplayLevelQueries.lawnLayout();
        var level = GameplayLevelQueries.currentLevel();
        this.bowlingMode = level instanceof WallnutBowlingLevel;
        this.conveyorMode = level instanceof ConveyorBeltLevel || bowlingMode;
        this.vaseBreakerMode = level instanceof VaseBreakerLevel;
        this.beghouledMode = level instanceof BeghouledLevel;
        this.iZombieMode = level instanceof IZombieLevel;
        var bootModel = GameplayLevelQueries.model();
        this.couchPlayMode = iZombieMode && !multiplayerMode && bootModel != null && bootModel.isCouchPlay();
        this.useZombiePackets = iZombieMode && !multiplayerPlantSide;
        this.scoreMode = level instanceof ScoreLevel;
        this.saveOurSeedsMode = level instanceof SaveOurSeedsLevel
            || ProtectTileRenderer.isProtectLevel(level);
        attach();
    }

    private void attach() {
        packets = new GameplayPackets(this);
        placement = new GameplayPlacement(this);
        flow = new GameplayFlow(this);
        logic = new GameplayLogic(this);
        worldRenderer = new GameplayWorldRenderer(this);
        cursors = new GameplayCursors(this);
        hudBuilder = new GameplayHudBuilder(this);
        lifecycle = new GameplayLifecycle(this);
    }

    public boolean isPregame() {
        boolean mowerIntro = entityRenderer != null && entityRenderer.isMowerIntroPlaying();
        return mowerIntro || (readySetPlant != null && readySetPlant.isPlaying());
    }

    public int currentTotalCoins() {
        User current = App.getInstance().getCurrentUser();
        if (current != null) {
            return current.getCoins();
        }
        var gameModel = GameplayLevelQueries.model();
        return gameModel == null ? 0 : gameModel.getCoinCount();
    }
}
