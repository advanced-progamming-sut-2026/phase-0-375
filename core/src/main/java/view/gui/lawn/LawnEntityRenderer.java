package view.gui.lawn;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import model.app.App;
import model.enums.Chapter;
import model.enums.PlacableLayer;
import model.enums.PlantState;
import model.enums.SunType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.game.core.GameModel;
import model.game.core.IceWindGust;
import model.game.core.LaneSlide;
import model.game.core.SandstormSpawn;
import model.game.core.WaterEmerge;
import model.game.level.minigame.bowling.BowlingWalnut;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.beghouled.BeghouledLevel;
import model.game.level.minigame.vasebreaker.Vase;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.GameMap;
import model.game.map.Point;
import model.plant.ability.ExplosiveAbility;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.projectile.Splash;
import model.item.Grave;
import model.item.GridItem;
import model.item.LootPickup;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.enums.LootPickupKind;
import model.enums.GroundType;
import model.enums.SlideDirection;
import model.game.map.terrain.CraterTerrainStrategy;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.map.terrain.SlideTerrainStrategy;
import model.item.placeable.Placeable;
import model.item.pushable.ArcadeMachine;
import model.item.pushable.Barrel;
import model.item.pushable.IceBlock;
import model.item.pushable.Piano;
import model.item.pushable.Pushable;
import model.zombie.armor.Armor;
import model.zombie.behavior.BarrelRollerBehavior;
import model.zombie.behavior.BuffBehavior;
import model.zombie.behavior.FishBehavior;
import model.zombie.behavior.FlyBehavior;
import model.zombie.behavior.JuggleBehavior;
import model.zombie.behavior.JumpBehavior;
import model.zombie.behavior.PushBehavior;
import model.zombie.behavior.ShootBehavior;
import model.zombie.behavior.StealSunBehavior;
import model.zombie.behavior.SummonBehavior;
import model.zombie.behavior.SwimBehavior;
import model.zombie.behavior.ThrowImpBehavior;
import model.zombie.behavior.TransformBehavior;
import model.zombie.behavior.zombotany.ZombotanyJalapenoBehavior;
import model.zombie.behavior.zombotany.ZombotanySquashBehavior;
import model.zombie.behavior.zomboss.DarkZombossBehavior;
import model.zombie.behavior.zomboss.ZombossAction;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPendingImpact;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.textures.TextureBank;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.AnimPose;
import view.gui.anim.AnimScale;
import view.gui.anim.GraveAnim;
import view.gui.anim.IceWindAnim;
import view.gui.anim.PamClipCache;
import view.gui.anim.SlideTileAnim;
import view.gui.anim.SpritesheetClipCache;
import view.gui.anim.bowling.BowlingWalnutAnim;
import view.gui.anim.vase.VaseBreakerAnim;
import view.gui.anim.SandstormAnim;
import view.gui.anim.plant.ExplosivePlantFx;
import view.gui.assets.PlantSpritesheetCatalog;
import view.gui.anim.plant.MeleePlantFx;
import view.gui.anim.plant.PlantAnimAdapter;
import view.gui.anim.plant.exclusive.SquashAnim;
import view.gui.anim.projectile.ProjectileAnimAdapter;
import view.gui.anim.zombie.BarrelRollerAnim;
import view.gui.anim.zombie.DarkKingAnim;
import view.gui.anim.zombie.DarkZombossAnim;
import view.gui.anim.zombie.FishermanAnim;
import view.gui.anim.zombie.GargantuarAnim;
import view.gui.anim.zombie.HunterAnim;
import view.gui.anim.zombie.JugglerAnim;
import view.gui.anim.zombie.OctopusAnim;
import view.gui.anim.zombie.SnorkelerAnim;
import view.gui.anim.zombie.SunshineAnim;
import view.gui.anim.plant.PlantFreezeAnim;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.anim.zombie.TroglobiteAnim;
import view.gui.anim.zombie.WizardAnim;
import view.gui.anim.zombie.ZombieAnimAdapter;
import view.gui.anim.zombie.ZombieAnimRole;
import view.gui.anim.zombie.ZombieFootfallCurve;
import view.gui.anim.zombie.ZombieGait;
import view.gui.anim.zombie.ZombieGaitProfiles;
import view.gui.anim.zombie.ZombotanyAnim;
import view.gui.assets.BeghouledArt;
import view.gui.assets.EffectPamPaths;
import view.gui.assets.PamCatalog;
import view.gui.assets.ProjectilePamPaths;
import view.gui.assets.PvzAssets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Draws plants, zombies, and projectiles on the lawn via libPVZ PAM clips.
 *
 * <p>Pipeline: model entity → {@link PlantAnimAdapter} / {@link ZombieAnimAdapter} /
 * {@link ProjectileAnimAdapter} → {@link AnimPose} → {@link PamClipCache} /
 * {@link SpritesheetClipCache} → {@code PamPlayer.draw} or sheet blit.
 *
 * <p>Draw order is back-to-front by lawn row (row 0 is the top of the screen),
 * then graves → plants → props → zombies within a lane so lower rows cover
 * taller sprites from the rows above.
 *
 * <p>TODO: mowers and grid props.
 */
public final class LawnEntityRenderer {
    private static final float NO_PHASE = -1f;
    private static final float HIT_FLASH_SEC = 0.12f;
    /** Peas drop this much; eat DPS is ~1 HP/frame and must pulse, not stay white. */
    static final int HIT_FLASH_CHUNK = 8;
    /** Min gap between chew pulses on plants (~one eat chomp). */
    static final float CHEW_FLASH_COOLDOWN = 0.65f;
    private static final float ARMOR_POP_FADE = 0.55f;
    private static final float ARMOR_POP_HOP = 1.4f;
    private static final float ARMOR_POP_GRAVITY = -9f;
    private static final float ARMOR_POP_BACK_TILES = 0.2f;
    /** Head ({@code particle_head}) arc stays inside the death tile (centre ± half a cell). */
    private static final float HEAD_THROW_BACK_TILES = 0.2f;
    private static final float HEAD_THROW_HOP_TILES = 0.45f;
    private static final float POP_BOUNCE = 0.4f;
    private static final String[] DEATH_PARTS_EGYPT = {
        "zombie_egypt_skull", "zombie_egypt_jaw",
        "zombie_egypt_arm_outer_lower", "zombie_egypt_hand_outer_01"};
    private static final String[] DEATH_PARTS = {
        "zombie_skull", "zombie_jaw",
        "zombie_arm_outer_lower", "zombie_arms_outer_upper"};
    /** All-Star {@code particles} group — default-hidden; {@code drawPart} to drop it. */
    private static final String ALLSTAR_PARTICLES = "_particles";
    /** Head / arm groups on the All-Star die body that {@link #ALLSTAR_PARTICLES} stands in for. */
    private static final String[] ALLSTAR_HEAD_PARTS = {
        "_particles", "particle_head", "particle_arm",
        "zombie_skull", "zombie_jaw", "allstar_head_helmet_particle",
        "zombie_arm_outer_lower", "zombie_arms_outer_upper"};
    /** The Gargantuar sheds its whole head as one group instead of separate bits. */
    private static final String GARGANTUAR_HEAD = "Gargantuar_Head_Particle";
    /** Head pieces on the die body that {@link #GARGANTUAR_HEAD} stands in for. */
    private static final String[] GARGANTUAR_HEAD_PARTS = {
        "Zombie_gargantuar_head", "Zombie_gargantuar_jaw",
        "Zombie_gargantuar_headBehind", "Zombie_gargantuar_head_Dress_Back"};
    /** Imp {@code particles} group — the whole clip is the detached head. */
    private static final String IMP_HEAD = "particle_head";
    private static final String[] IMP_HEAD_PARTS = {
        "zombie_imp_skull", "zombie_imp_jaw", "_zombie_imp_head_top"};
    /** Arcade {@code particles} groups — default-hidden; {@code drawPart} to drop them. */
    private static final String[] ARCADE_PARTICLE_PARTS = {"particle_head", "particle_arm"};
    /** Head / arm on the Arcade die body that the particle groups stand in for. */
    private static final String[] ARCADE_HEAD_PARTS = {
        "particle_head", "particle_arm",
        "zombie_skull", "zombie_jaw",
        "zombie_arm_outer_lower", "zombie_arm_outer_upper", "zombie_arms_outer_upper",
        "zombie_hand_outer", "zombie_troglobite_hand_oute_push"};

    /** Hunter {@code die} body parts that {@link HunterAnim#DEATH_PARTICLE_PARTS} stand in for. */
    private static final String[] HUNTER_HEAD_PARTS = {
        "particle_head", "particle_hand",
        "zombie_skull", "zombie_jaw",
        "zombie_arm_outer_lower", "zombie_arms_outer_upper"};
    /** Fallback live-body parts to hide after {@code particle_arm} drops at half HP. */
    private static final String[] LOST_HAND_BODY_PARTS = {
        "particle_hand", "particle_arm", "particle_arm_01", "particle_arm_02",
        "zombie_arm_outer_lower", "zombie_arm_outer_upper", "zombie_arms_outer_upper",
        "zombie_hand_outer", "zombie_hand_outer_01", "zombie_hand_outer_02", "zombie_hand_outer_03",
        "zombie_troglobite_hand_oute_push", "zombie_troglobite_hand_outer",
        "zombie_troglobite_arm_outer_lower", "zombie_troglobite_arm_outer_upper",
        "zombie_egypt_arm_outer_lower", "zombie_egypt_arm_outer_upper",
        "zombie_egypt_arms_outer_upper", "zombie_egypt_hand_outer_01"};
    /** Detached limb groups on {@code particles}; {@code particle_hand} is the fallback. */
    private static final String[] ARM_PARTICLE_NAMES = {
        "particle_arm", "particle_arm_01", "particle_arm_02", "particle_hand"};
    /** PAM overlay layers that ride the skull; keep them off thrown heads. */
    private static final String[] INK_BUTTER_PARTS = {
        "butter", "ink", "_butter", "_ink", "zombie_butter", "zombie_ink"};
    /** Sibling/child bits that must not fly with {@code particle_head}. */
    private static final String[] HEAD_POP_HIDE = {
        "particle_arm", "particle_hand",
        "zombie_arm_outer_lower", "zombie_arms_outer_upper",
        "zombie_egypt_arm_outer_lower", "zombie_egypt_hand_outer_01",
        "zombie_jaw", "zombie_egypt_jaw"};

    /** Arcade cabinet effect PAM (not a zombie body). */
    private static final String ARCADE_CABINET_PAM = "80S_ARCADE_CABINET";
    /** Pianist’s piano — {@code 768/FULL/ZOMBIE/PIANO/PIANO.PAM}. */
    private static final String PIANO_PAM = "PIANO";
    /** Piano {@code particles} parts that scatter on {@code die}. */
    private static final String[] PIANO_PARTICLE_PARTS = {
        "particle_jar_01", "particle_jar_02",
        "particle_key_01", "particle_key_02",
        "particle_note_01", "particle_note_02"};
    /** Fire/explosion death PAMs under EFFECTS — clip name is {@code animation}, not {@code die}. */
    private static final String JANE_ASH_PAM = "ZOMBIE_LOSTCITY_JANE_ASH";
    private static final String BIG_ASH_PAM = "ZOMBIE_BIG_ASH";
    private static final String GARGANTUAR_ASH_PAM = "ZOMBIE_GARGANTUAR_ASH";
    private static final String IMP_ASH_PAM = "ZOMBIE_IMP_ASH";
    private static final String ZOMBIE_ASH_PAM = "ZOMBIE_ASH";
    /** Crystal Skull laser — EFFECTS PAM, clip {@code laser_beam}. */
    private static final String CRYSTALSKULL_BEAM_PAM = "CRYSTALSKULL_BEAM";
    /** Lawn collectible — EFFECTS PAM. Yellow/normal is clip {@code animation}. */
    private static final String SUN_PAM = "SUN";
    private static final String SUN_BOMB_PAM = "SUN_BOMB";
    /** Glowing-zombie / ground plant-food PAM under EFFECTS. */
    private static final String PLANTFOOD_PICKUP_PAM = "PLANTFOOD_PICKUP";
    private static final String COIN_GOLD_PAM = "COIN_GOLD";
    private static final String COIN_SILVER_PAM = "COIN_SILVER";
    private static final String COIN_DIAMOND_PAM = "COIN_DIAMOND";
    private static final String FLOWER_POT_REGION =
        "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_122X161";
    private static final float FLOWER_POT_DRAW_H = 78f;
    /** Soft green base + pulse amplitude for glowing zombies. */
    private static final float GLOW_BASE = 0.22f;
    private static final float GLOW_PULSE = 0.28f;
    private static final float GLOW_HZ = 0.55f;
    /** Ground burst when Prospector's dynamite explodes. Clips {@code animation} + {@code animation2}. */
    private static final String PROSPECTOR_BLAST_PAM = "ZOMBIE_PROSPECTOR_BLAST_OFF";
    private static final String[] PROSPECTOR_BLAST_CLIPS = {"animation", "animation2"};
    /** Glow on {@code attack} that cues the beam at {@link StealSunBehavior#ATTACK_BEAM_AT}. */
    private static final String CRYSTALSKULL_GLOW_PART = "zombie_egypt_ra_staff_whiteglow";
    /** Held crystal skull (Ra staff mesh) — beam's right edge tracks this part's left. */
    private static final String[] CRYSTALSKULL_SKULL_PARTS = {
        "zombie_egypt_ra_staff", CRYSTALSKULL_GLOW_PART, "zombie_skull"};
    private static final String[] CRYSTALSKULL_BEAM_PARTS = {"laser_beam", "beam"};
    /** Outstretched pushing hand on Arcade and Troglobite {@code push}. */
    private static final String ARCADE_HAND_PART = "zombie_troglobite_hand_oute_push";

    private final LawnLayout layout;
    private final PlantAnimAdapter plantAdapter;
    private final ZombieAnimAdapter zombieAdapter;
    private final ProjectileAnimAdapter projectileAdapter;
    private final PamClipCache clips;
    private final SpritesheetClipCache sheetClips;
    private final PlantSpritesheetCatalog plantSheets;
    private final PamPlayer player;
    private final PamCatalog catalog;
    private final TextureBank textures;

    private TextureRegion flowerPotRegion;

    private final IdentityHashMap<Object, AnimClock> clocks = new IdentityHashMap<>();
    private final IdentityHashMap<ClipRef, ZombieFootfallCurve> footfalls = new IdentityHashMap<>();
    /** Left-edge canvas X of the pushing hand, one sample per push-clip frame. */
    private final IdentityHashMap<ClipRef, float[]> arcadePushHandX = new IdentityHashMap<>();
    /** Crystal Skull / beam part names that actually exist on the loaded PAM. */
    private String crystalSkullPart;
    private String crystalBeamPart;
    private final IdentityHashMap<Object, HitFlash> hitFlashes = new IdentityHashMap<>();
    /** Body HP has crossed half; {@code particle_arm} already hopped off. */
    private final IdentityHashMap<ZombieInstance, Boolean> lostHands = new IdentityHashMap<>();
    /** Outer-arm part names on the live PAM, cached after the first half-HP pop. */
    private final Map<String, String[]> lostArmBodyByPam = new HashMap<>();
    private final IdentityHashMap<ZombieInstance, Chapter> artChapters = new IdentityHashMap<>();
    private final List<ArmorPop> armorPops = new ArrayList<>();
    private final List<DeathFx> deathFx = new ArrayList<>();
    private final IdentityHashMap<ZombieInstance, LiveSnap> lastLive = new IdentityHashMap<>();
    private final IdentityHashMap<Pushable, LiveSnap> lastCabinets = new IdentityHashMap<>();
    private final IdentityHashMap<Cell, LiveSnap> lastTerrainIce = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, LiveSnap> lastPlants = new IdentityHashMap<>();
    private final IdentityHashMap<Grave, LiveSnap> lastGraves = new IdentityHashMap<>();
    /** World-pixel skull alignment for a thrown Imp; lerped to 0 as it lands. */
    private final IdentityHashMap<ZombieInstance, float[]> tossAlign = new IdentityHashMap<>();
    private final List<BlastFx> prospectorBlasts = new ArrayList<>();
    private final IdentityHashMap<ZombieInstance, Boolean> prospectorBlastSpawned = new IdentityHashMap<>();
    private final List<BlastFx> hunterSplats = new ArrayList<>();
    private final IdentityHashMap<ZombieInstance, Integer> hunterSplatSeq = new IdentityHashMap<>();
    /** World origin of a flying octopus at release (PAM canvas centre). */
    private final IdentityHashMap<ShootBehavior.OctopusShot, float[]> octopusAlign = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, OctopusCoatFx> octopusCoats = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, LiveSnap> lastPlantIce = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, Float> plantIceIntro = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, Object> plantIceClocks = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, Object> plantChillClocks = new IdentityHashMap<>();
    private final IdentityHashMap<ZombieInstance, Object> zombieChillClocks = new IdentityHashMap<>();
    private final IdentityHashMap<ZombieInstance, Float> zombieDangerElapsed = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, SheepFx> sheepFx = new IdentityHashMap<>();
    private final IdentityHashMap<Grave, Float> graveEmerge = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, Boolean> explosionSpawned = new IdentityHashMap<>();
    private final IdentityHashMap<ZombieInstance, Boolean> jalapenoFireSpawned = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, Integer> armorBreakFxEpoch = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, Integer> meleeAttackFxEpoch = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, Boolean> meleePlantFoodFxSpawned = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, OneShotFx> meleeIdlePulses = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, PlantFoodFx> plantFoodFx = new IdentityHashMap<>();
    private final IdentityHashMap<Cell, FireTileFx> fireTileFx = new IdentityHashMap<>();
    private boolean fireImpactPamReady;
    private final IdentityHashMap<PlantInstance, float[]> deathBlastSeen = new IdentityHashMap<>();
    private final List<OneShotFx> backEffects = new ArrayList<>();
    private final List<OneShotFx> frontEffects = new ArrayList<>();
    /** Egypt sandstorms in flight, keyed by their model record. */
    private final IdentityHashMap<SandstormSpawn, SandstormFx> sandstorms = new IdentityHashMap<>();
    /** Zombies spawned under a still-fading sandstorm; hidden until the outro ends. */
    private final Set<ZombieInstance> sandstormConcealed =
            Collections.newSetFromMap(new IdentityHashMap<>());
    /** The sandstorm PAM was force-loaded once so intro/loop/outro timings are known. */
    private boolean sandstormPamReady;
    /** Frostbite Caves ice winds in flight, keyed by their model record. */
    private final IdentityHashMap<IceWindGust, IceWindFx> iceWinds = new IdentityHashMap<>();
    /** The chill-wind PAM was force-loaded once so the sweep can start instantly. */
    private boolean iceWindPamReady;
    /** Frostbite slide tiles keyed by their cell; idle loop + active bursts. */
    private final IdentityHashMap<Cell, SlideTileFx> slideTiles = new IdentityHashMap<>();
    /** Both slider PAMs were force-loaded once so activations play instantly. */
    private boolean slideTilePamReady;
    /** Slides still gliding between lanes, keyed by their zombie. */
    private final IdentityHashMap<ZombieInstance, LaneSlide> laneGlides =
            new IdentityHashMap<>();
    /** Low-tide ambushes still surfacing, keyed by their zombie. */
    private final IdentityHashMap<ZombieInstance, WaterEmerge> waterEmerges =
            new IdentityHashMap<>();
    private final List<SunFlight> sunFlights = new ArrayList<>();
    private final List<PlantFoodFlight> plantFoodFlights = new ArrayList<>();
    private final List<LootFlight> lootFlights = new ArrayList<>();
    private final Set<Object> seenThisFrame =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private final float[] xyTmp = new float[3];
    private final Matrix4 batchTransform = new Matrix4();
    private final Matrix4 popTransform = new Matrix4();
    private final Map<String, Boolean> popVis = new HashMap<>();

    private List<BowlingWalnut> bowlingWalnuts = List.of();
    private final Map<String, Float> vaseAge = new HashMap<>();
    private final IdentityHashMap<PlantInstance, BeghouledMotion> beghouledMotion = new IdentityHashMap<>();
    private static final float BEGHOULED_MOVE_SEC = 0.22f;
    private TextureRegion craterRegion;

    private final DebugEntityOverlay entityOverlay;
    private FishermanDrownShader drownShader;
    private HitFlashShader hitFlashShader;
    private GlowGreenShader glowGreenShader;
    private ChillBlueShader chillBlueShader;
    private DangerRedShader dangerRedShader;
    private float snorkelRippleTime;
    private final Set<String> snorkelRippleLoaded = new HashSet<>();
    private ScreenShake screenShake;

    private LawnMowerRenderer mowerRenderer;

    // --- End-level lose/win FX (clocks tick via {@link #tickEndLevel}, not world delta) ---
    private static final float END_FADE_SEC = 0.75f;

    private enum EndMode { NONE, LOSE, WIN }

    private EndMode endMode = EndMode.NONE;
    private float endFade;
    private float endAnimDelta;
    private Texture whitePixel;

    public LawnEntityRenderer(PvzAssets assets, LawnLayout layout, DebugEntityOverlay entityOverlay) {
        this(assets, layout,
                new PlantAnimAdapter(assets.pamCatalog, assets.plantSheets),
                new ZombieAnimAdapter(assets.pamCatalog, assets.plantSheets),
                entityOverlay);
    }

    public LawnEntityRenderer(PvzAssets assets, LawnLayout layout,
                              PlantAnimAdapter plantAdapter, ZombieAnimAdapter zombieAdapter,
                              DebugEntityOverlay entityOverlay) {
        this.layout = layout;
        this.plantAdapter = plantAdapter;
        this.zombieAdapter = zombieAdapter;
        this.projectileAdapter = new ProjectileAnimAdapter(assets.plantSheets);
        this.player = assets.player;
        this.clips = new PamClipCache(assets.player);
        this.sheetClips = new SpritesheetClipCache(assets.root);
        this.plantSheets = assets.plantSheets;
        this.catalog = assets.pamCatalog;
        this.textures = assets.textures;
        this.entityOverlay = entityOverlay;
        this.mowerRenderer = new LawnMowerRenderer(assets, layout);
    }

    public void resetMowers(model.enums.Chapter chapter, boolean playIntro) {
        if (mowerRenderer != null) {
            mowerRenderer.reset(chapter, playIntro);
        }
    }

    public boolean isMowerIntroPlaying() {
        return mowerRenderer != null && mowerRenderer.isIntroPlaying();
    }

    public void tickMowerIntro(float delta) {
        if (mowerRenderer != null) {
            mowerRenderer.tickIntro(delta);
        }
    }

    public void tickMowers(GameModel model, float delta) {
        if (mowerRenderer != null) {
            mowerRenderer.tick(model, delta);
        }
    }

    public void drawMowers(Batch batch, GameModel model, float delta, int row) {
        if (mowerRenderer != null) {
            mowerRenderer.drawRow(batch, model, delta, row);
        }
    }

    public void setScreenShake(ScreenShake screenShake) {
        this.screenShake = screenShake;
    }

    /** Debug sandbox: biome basics use this chapter's PAM instead of the lawn chapter. */
    public void setArtChapter(ZombieInstance zombie, Chapter chapter) {
        if (zombie == null) {
            return;
        }
        if (chapter == null) {
            artChapters.remove(zombie);
        } else {
            artChapters.put(zombie, chapter);
        }
    }

    public LawnEntityRenderer(PamCatalog catalog, PvzAssets assets, LawnLayout layout,
                              DebugEntityOverlay entityOverlay) {
        this(assets, layout,
                new PlantAnimAdapter(catalog, assets.plantSheets),
                new ZombieAnimAdapter(catalog, assets.plantSheets),
                entityOverlay);
    }

    public void draw(Batch batch, GameModel model, float delta) {
        if (model == null) {
            return;
        }
        snorkelRippleTime += Math.max(0f, delta);
        seenThisFrame.clear();
        Set<ZombieInstance> alive = new HashSet<>(model.getZombies());
        for (ZombieInstance zombie : lastLive.keySet()) {
            if (!alive.contains(zombie)) {
                spawnDeath(model, zombie, lastLive.get(zombie));
            }
        }
        lastLive.entrySet().removeIf(e -> !alive.contains(e.getKey()));

        Set<Pushable> liveCabinets = new HashSet<>();
        for (ZombieInstance zombie : model.getZombies()) {
            collectLiveCabinet(zombie.getPushableItem(), liveCabinets);
        }
        if (model.getOrphanedPushables() != null) {
            for (Pushable orphan : model.getOrphanedPushables()) {
                collectLiveCabinet(orphan, liveCabinets);
            }
        }
        for (Pushable cabinet : lastCabinets.keySet()) {
            if (!liveCabinets.contains(cabinet)) {
                spawnCabinetDeath(lastCabinets.get(cabinet));
            }
        }
        lastCabinets.entrySet().removeIf(e -> !liveCabinets.contains(e.getKey()));
        Set<Cell> liveIce = syncTerrainIce(model);
        Set<Cell> liveSlides = syncSlideTiles(model);
        harvestSlideStarts(model);

        // Win fade: hold plant idle (and plant ghosts) still under the dim.
        float plantDelta = endMode == EndMode.WIN ? 0f : delta;

        List<PlantInstance> plants = model.getAllPlants();
        IdentityHashMap<PlantInstance, float[]> deathBlastNow = new IdentityHashMap<>();
        for (PlantInstance plant : plants) {
            maybeSpawnPlantExplosion(plant, deathBlastNow);
            maybeSpawnMeleeFx(plant);
            updateMeleeIdlePulse(plant);
        }
        spawnMissingDeathBlasts(deathBlastNow);
        deathBlastSeen.clear();
        deathBlastSeen.putAll(deathBlastNow);
        harvestBeghouledClears(model);
        drawEffects(batch, backEffects, delta);
        prepareBowlingWalnuts(model);
        updateSandstorms(model, delta);
        updateIceWinds(model, delta);
        updateLaneGlides(model);
        updateWaterEmerges(model);

        Set<PlantInstance> livePlants = Collections.newSetFromMap(new IdentityHashMap<>());
        livePlants.addAll(plants);
        for (PlantInstance plant : plants) {
            if (plantRow(plant) < 0) {
                drawPlant(batch, plant, plantDelta);
            }
        }

        GameMap map = model.getMap();
        int rows = map != null ? map.getRows() : layout.rows();
        // Row 0 is the top of the screen; later rows paint over it.
        for (int row = 0; row < rows; row++) {
            drawCraters(batch, model, row);
            drawFireTiles(batch, model, delta, row);
            drawGraves(batch, model, delta, row);
            drawGraveGhosts(batch, delta, row);
            drawVases(batch, model, delta, row, rows);
            for (PlantInstance plant : plants) {
                int lane = plantRow(plant);
                if (lane >= 0 && clampRow(lane, rows) == row) {
                    drawPlant(batch, plant, plantDelta);
                }
            }
            drawPlantGhosts(batch, livePlants, plantDelta, row);
            for (Pushable cabinet : liveCabinets) {
                int lane = cabinet.getRow();
                if (lane >= 0 && clampRow(lane, rows) == row) {
                    drawPushable(batch, model, cabinet, delta);
                }
            }
            drawSlideTiles(batch, liveSlides, delta, row);
            drawTerrainIce(batch, model, liveIce, delta, row);
            for (ZombieInstance zombie : model.getZombies()) {
                if (clampRow(zombieRow(zombie), rows) == row
                        && !sandstormConcealed.contains(zombie)
                        && !drawsAboveLawn(zombie)) {
                    Chapter skin = artChapterFor(zombie, model.getChapter());
                    drawZombie(batch, zombie, skin, delta);
                }
            }
            drawBowlingWalnuts(batch, delta, row, rows);
            drawDeathFx(batch, delta, row);
            drawArmorPops(batch, delta, row);
            drawHunterSplats(batch, delta, row);
            drawProspectorBlasts(batch, delta, row);
            drawMowers(batch, model, delta, row);
        }
        for (ZombieInstance zombie : model.getZombies()) {
            if (drawsAboveLawn(zombie) && !sandstormConcealed.contains(zombie)) {
                Chapter skin = artChapterFor(zombie, model.getChapter());
                drawZombie(batch, zombie, skin, delta);
            }
        }
        drawSandstorms(batch);
        drawIceWinds(batch);
        drawOctopi(batch, model, delta);
        drawZombossFireballs(batch, model, delta);
        drawSuns(batch, model, delta);
        drawPlantFood(batch, model, delta);
        drawLoot(batch, model, delta);
        if (model.getProjectiles() != null) {
            for (Projectile projectile : model.getProjectiles()) {
                drawProjectile(batch, projectile, delta);
            }
        }
        harvestProjectileHits(model);
        harvestRadioactiveSunExplosions(model);
        drawEffects(batch, frontEffects, delta);

        pruneVaseAge(model);
        clocks.keySet().removeIf(key -> !seenThisFrame.contains(key));
        beghouledMotion.keySet().removeIf(plant -> !seenThisFrame.contains(plant));
        explosionSpawned.keySet().removeIf(plant -> !seenThisFrame.contains(plant));
        jalapenoFireSpawned.keySet().removeIf(zombie -> !seenThisFrame.contains(zombie));
        armorBreakFxEpoch.keySet().removeIf(plant -> !seenThisFrame.contains(plant));
        meleeAttackFxEpoch.keySet().removeIf(plant -> !seenThisFrame.contains(plant));
        meleePlantFoodFxSpawned.keySet().removeIf(plant -> !seenThisFrame.contains(plant));
        meleeIdlePulses.entrySet().removeIf(entry -> {
            if (seenThisFrame.contains(entry.getKey())) {
                return false;
            }
            backEffects.remove(entry.getValue());
            frontEffects.remove(entry.getValue());
            return true;
        });
        plantFoodFx.keySet().removeIf(plant -> !seenThisFrame.contains(plant));
        for (PlantInstance plant : new ArrayList<>(lastPlantIce.keySet())) {
            if (!seenThisFrame.contains(plant)) {
                plantIceIntro.remove(plant);
                plantIceClocks.remove(plant);
                spawnIceShatter(lastPlantIce.remove(plant));
            }
        }
        plantChillClocks.keySet().removeIf(plant -> !seenThisFrame.contains(plant));
        zombieChillClocks.keySet().removeIf(zombie -> !seenThisFrame.contains(zombie) || !zombie.isFrozen());
        zombieDangerElapsed.keySet().removeIf(zombie -> !seenThisFrame.contains(zombie));
        graveEmerge.keySet().removeIf(grave -> !seenThisFrame.contains(grave));
        sheepFx.keySet().removeIf(plant -> !seenThisFrame.contains(plant)
            && !plant.isTransformed());
        hitFlashes.entrySet().removeIf(e ->
            !seenThisFrame.contains(e.getKey()) && e.getValue().remaining <= 0f);
        lostHands.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        tossAlign.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        prospectorBlastSpawned.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        hunterSplatSeq.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        octopusAlign.keySet().removeIf(shot -> !shot.isFlying());
        Set<ZombieInstance> keepArt = new HashSet<>(model.getZombies());
        collectIcedOccupants(model, keepArt);
        artChapters.keySet().removeIf(zombie -> !keepArt.contains(zombie));

        drawEndLevel(batch, model);
    }

    /** Advances lose/win black fade (world PAM keeps ticking separately). */
    public void tickEndLevel(float delta) {
        if (endMode == EndMode.NONE || delta <= 0f) {
            return;
        }
        endAnimDelta = delta;
        endFade = Math.min(1f, endFade + delta / END_FADE_SEC);
    }

    public void beginLoseFade() {
        endMode = EndMode.LOSE;
        endFade = 0f;
    }

    public void beginWinFade() {
        endMode = EndMode.WIN;
        endFade = 0f;
    }

    public float loseFadeAlpha() {
        return endMode == EndMode.LOSE ? endFade : 0f;
    }

    public boolean isLoseFadeDone() {
        return endMode == EndMode.LOSE && endFade >= 1f;
    }

    public boolean isWinFadeDone() {
        return endMode == EndMode.WIN && endFade >= 1f;
    }

    private void drawEndLevel(Batch batch, GameModel model) {
        if (endMode == EndMode.NONE) {
            return;
        }
        endAnimDelta = 0f;
        drawBlackFade(batch, endFade);
    }

    private void drawBlackFade(Batch batch, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        Texture pixel = whitePixel();
        Color prev = batch.getColor();
        batch.setColor(0f, 0f, 0f, Math.min(1f, alpha));
        // Cover left+center and spill into the right panel.
        batch.draw(pixel, 0f, 0f, LawnLayout.WORLD_WIDTH + LawnLayout.TEXTURE_RIGHT_WIDTH,
            LawnLayout.WORLD_HEIGHT);
        batch.setColor(prev);
    }

    private Texture whitePixel() {
        if (whitePixel == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            whitePixel = new Texture(pm);
            pm.dispose();
        }
        return whitePixel;
    }

    private void drawPlant(Batch batch, PlantInstance plant, float delta) {
        if (drawWizardSheep(batch, plant, delta)) {
            return;
        }
        Point pos = plant.getPosition();
        if (pos == null) {
            seenThisFrame.add(plant);
            tickHitFlash(plant, plantVitality(plant), delta);
            entityOverlay.drawPlant(batch, App.getInstance().getCurrentGameModel(), plant);
            return;
        }
        AnimPose pose = plantAdapter.poseFor(plant);
        if (pose == null) {
            seenThisFrame.add(plant);
            tickHitFlash(plant, plantVitality(plant), delta);
            entityOverlay.drawPlant(batch, App.getInstance().getCurrentGameModel(), plant);
            return;
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        applyBeghouledMotion(plant, xy, delta);
        applySquashLeap(plant, xy);
        float[] pfXy = layout.centerOf(pos.getY() - 0.5f, pos.getX() + 0.1f);
        String clockKey = pose.cacheKey() + "#" + plant.getActionEpoch();
        float flash = tickHitFlash(plant, plantVitality(plant), delta);
        float animDelta = plant.isFrozen() ? 0f : delta;
        float time = drawPose(batch, plant, pose, xy[0], xy[1], AnimScale.forPlant(pose), NO_PHASE,
                flash, animDelta, clockKey);
        drawPlantChill(batch, plant, xy[0], xy[1], flash, delta);
        drawPlantFreezeIce(batch, plant, xy[0], xy[1], flash, delta);
        updateAndDrawPlantFoodFx(batch, plant, pfXy[0], pfXy[1], delta);
        lastPlants.put(plant, new LiveSnap(pose, xy[0], xy[1], false, time));
    }

    /** Squash leaps from its tile onto the captured smash target during ATTACKING. */
    private void applySquashLeap(PlantInstance plant, float[] xy) {
        if (plant == null || xy == null || plant.getState() != PlantState.ATTACKING) {
            return;
        }
        if (plant.getDefinition() == null || !"Squash".equals(plant.getDefinition().getName())) {
            return;
        }
        if (!(plant.getAbilityStrategy() instanceof ExplosiveAbility explosive)
                || !explosive.hasSmashTarget()) {
            return;
        }
        PamCatalog.PamEntry entry = catalog == null ? null
                : catalog.forPlant(plant.getDefinition().getName());
        float[] to = layout.centerOf(explosive.getSmashTargetGridY(), explosive.getSmashTargetGridX());
        float dx = to[0] - xy[0];
        float dy = to[1] - xy[1];
        float travel = SquashAnim.leapTravelFraction(plant, entry);
        if (travel > 0f) {
            xy[0] += dx * travel;
            xy[1] += dy * travel;
        }
        float travelTiles = layout.cellWidth() > 0f
                ? (float) Math.sqrt(dx * dx + dy * dy) / layout.cellWidth()
                : 1f;
        xy[1] += SquashAnim.leapVisualHeightCells(plant, entry, travelTiles) * layout.cellHeight();
    }

    private void applyZombotanySquashLeap(ZombieInstance zombie, Chapter chapter, float[] xy) {
        if (zombie == null || xy == null) {
            return;
        }
        ZombotanySquashBehavior squash =
                (ZombotanySquashBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_SQUASH);
        if (squash == null || !squash.isSquashing()
                || squash.getSmashTargetGridX() < 0 || squash.getSmashTargetGridY() < 0) {
            return;
        }
        PamCatalog.PamEntry entry = catalog == null ? null
                : catalog.forZombie(zombie.getDefinition().getName(), chapter);
        float[] to = layout.centerOf(squash.getSmashTargetGridY(), squash.getSmashTargetGridX());
        float dx = to[0] - xy[0];
        float dy = to[1] - xy[1];
        float travel = SquashAnim.leapTravelFraction(squash.getAttackElapsed(), entry, true);
        if (travel > 0f) {
            xy[0] += dx * travel;
            xy[1] += dy * travel;
        }
        float travelTiles = layout.cellWidth() > 0f
                ? (float) Math.sqrt(dx * dx + dy * dy) / layout.cellWidth()
                : 1f;
        xy[1] += SquashAnim.leapVisualHeightCells(squash.getAttackElapsed(), entry, travelTiles, true)
                * layout.cellHeight();
    }

    private void maybeSpawnZombotanyJalapenoFire(ZombieInstance zombie) {
        if (zombie == null) {
            return;
        }
        ZombotanyJalapenoBehavior jala =
                (ZombotanyJalapenoBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_JALAPENO);
        if (jala == null || !jala.isAttacking()) {
            return;
        }
        if (jalapenoFireSpawned.put(zombie, Boolean.TRUE) != null) {
            return;
        }
        Point pos = new Point(0, zombie.getGridY());
        float[] xy = layout.centerOf(zombie.getGridY(), Math.max(0, zombie.getGridX()));
        spawnExplosionSpecs(ExplosivePlantFx.specsForName("Jalapeno"), pos, xy[0], xy[1]);
    }

    private void maybeSpawnPlantExplosion(PlantInstance plant, IdentityHashMap<PlantInstance, float[]> deathBlastNow) {
        Point pos = plant.getPosition();
        if (pos == null) {
            return;
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        maybeSpawnArmorBreakExplosion(plant, xy[0], xy[1]);
        if (ExplosivePlantFx.isDeathDetonator(plant)) {
            deathBlastNow.put(plant, new float[]{xy[0], xy[1]});
            return;
        }
        AnimPose pose = plantAdapter.poseFor(plant);
        if (!ExplosivePlantFx.shouldSpawn(plant, pose)) {
            return;
        }
        if (explosionSpawned.put(plant, Boolean.TRUE) != null) {
            return;
        }
        spawnExplosionSpecs(ExplosivePlantFx.specsFor(plant), pos, xy[0], xy[1]);
    }

    private void maybeSpawnArmorBreakExplosion(PlantInstance plant, float x, float y) {
        if (plant == null || plant.getArmorBreakEpoch() <= 0) {
            return;
        }
        if (!ExplosivePlantFx.isDeathDetonator(plant) && !plant.armorExplodesOnBreak()) {
            return;
        }
        int epoch = plant.getArmorBreakEpoch();
        Integer last = armorBreakFxEpoch.get(plant);
        if (last != null && last == epoch) {
            return;
        }
        armorBreakFxEpoch.put(plant, epoch);
        spawnExplosionSpecs(ExplosivePlantFx.specsFor(plant), plant.getPosition(), x, y);
    }

    private void maybeSpawnMeleeFx(PlantInstance plant) {
        Point pos = plant.getPosition();
        if (pos == null) {
            return;
        }
        seenThisFrame.add(plant);
        AnimPose pose = plantAdapter.poseFor(plant);
        if (!MeleePlantFx.shouldSpawn(plant, pose)) {
            meleePlantFoodFxSpawned.remove(plant);
            return;
        }
        boolean plantFood = plant.getState() == PlantState.PLANT_FOOD;
        if (plantFood) {
            if (meleePlantFoodFxSpawned.put(plant, Boolean.TRUE) != null) {
                return;
            }
        } else {
            meleePlantFoodFxSpawned.remove(plant);
            int epoch = plant.getActionEpoch();
            Integer last = meleeAttackFxEpoch.get(plant);
            if (last != null && last == epoch) {
                return;
            }
            meleeAttackFxEpoch.put(plant, epoch);
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        int radius = MeleePlantFx.tileRadius(plant, plantFood);
        for (MeleePlantFx.Spec spec : MeleePlantFx.specsFor(plant, plantFood)) {
            if (spec.kind() == MeleePlantFx.Kind.TILE_HIT) {
                spawnMeleeTileHits(spec, pos, radius);
            } else {
                addEffect(toExplosiveLayer(spec.layer()), spec.pamPath(), spec.clipName(),
                        xy[0], xy[1], AnimScale.PLANT, false);
            }
        }
    }

    private void updateMeleeIdlePulse(PlantInstance plant) {
        MeleePlantFx.Spec spec = MeleePlantFx.idlePulseSpec(plant);
        Point pos = plant.getPosition();
        boolean show = spec != null && pos != null
                && plant.getState() != PlantState.ATTACKING
                && plant.getState() != PlantState.PLANT_FOOD
                && plant.getState() != PlantState.DYING;
        if (!show) {
            removeMeleeIdlePulse(plant);
            return;
        }
        if (meleeIdlePulses.containsKey(plant)) {
            return;
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        OneShotFx fx = addEffect(toExplosiveLayer(spec.layer()), spec.pamPath(), spec.clipName(),
                xy[0], xy[1], AnimScale.PLANT, true);
        meleeIdlePulses.put(plant, fx);
    }

    private void removeMeleeIdlePulse(PlantInstance plant) {
        OneShotFx fx = meleeIdlePulses.remove(plant);
        if (fx == null) {
            return;
        }
        backEffects.remove(fx);
        frontEffects.remove(fx);
    }

    private void spawnMeleeTileHits(MeleePlantFx.Spec spec, Point pos, int radius) {
        int row = pos.getY();
        int col = pos.getX();
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                int r = row + dr;
                int c = col + dc;
                if (r < 0 || c < 0 || r >= layout.rows() || c >= layout.cols()) {
                    continue;
                }
                float[] tile = layout.centerOf(r, c);
                addEffect(toExplosiveLayer(spec.layer()), spec.pamPath(), spec.clipName(),
                        tile[0], tile[1], AnimScale.PLANT, false);
            }
        }
    }

    private static ExplosivePlantFx.Layer toExplosiveLayer(MeleePlantFx.Layer layer) {
        return layer == MeleePlantFx.Layer.BACK ? ExplosivePlantFx.Layer.BACK : ExplosivePlantFx.Layer.FRONT;
    }

    private void spawnMissingDeathBlasts(IdentityHashMap<PlantInstance, float[]> deathBlastNow) {
        for (var entry : deathBlastSeen.entrySet()) {
            if (deathBlastNow.containsKey(entry.getKey())) {
                continue;
            }
            float[] xy = entry.getValue();
            spawnExplosionSpecs(ExplosivePlantFx.specsFor(entry.getKey()), null, xy[0], xy[1]);
        }
    }

    private void spawnExplosionSpecs(List<ExplosivePlantFx.Spec> specs, Point pos, float x, float y) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        if (screenShake != null) {
            screenShake.pulse();
        }
        for (ExplosivePlantFx.Spec spec : specs) {
            if (spec.placement() == ExplosivePlantFx.Placement.ALONG_LANE && pos != null) {
                int row = pos.getY();
                for (int col = 0; col < layout.cols(); col++) {
                    float[] tile = layout.centerOf(row, col);
                    addEffect(spec.layer(), spec.pamPath(), ExplosivePlantFx.jalapenoClip(col),
                            tile[0], tile[1], AnimScale.PLANT, false);
                }
            } else {
                addEffect(spec.layer(), spec.pamPath(), spec.clipName(), x, y, AnimScale.PLANT, false);
            }
        }
    }

    private OneShotFx addEffect(ExplosivePlantFx.Layer layer, String pamPath, String clipName,
                                float x, float y, float scale, boolean loop) {
        OneShotFx fx = new OneShotFx(pamPath, clipName, x, y, scale, loop);
        if (layer == ExplosivePlantFx.Layer.BACK) {
            backEffects.add(fx);
        } else {
            frontEffects.add(fx);
        }
        return fx;
    }

    private void harvestProjectileHits(GameModel model) {
        List<Projectile> hits = model.drainProjectileHits();
        for (Projectile projectile : hits) {
            if (projectile == null) {
                continue;
            }
            projectileWorldCenter(projectile, xyTmp);
            spawnProjectileHit(projectile, xyTmp[0], xyTmp[1]);
        }
    }

    private void spawnProjectileHit(Projectile projectile, float x, float y) {
        ProjectilePamPaths.HitPam hit = ProjectilePamPaths.hitFor(projectile);
        if (hit == null || hit.path() == null) {
            return;
        }
        String clip = hit.clip() != null ? hit.clip() : ProjectilePamPaths.CLIP_PREFERENCES[0];
        frontEffects.add(new OneShotFx(hit.path(), clip, x, y, AnimScale.PROJECTILE, false));
    }

    private void drawEffects(Batch batch, List<OneShotFx> effects, float delta) {
        Iterator<OneShotFx> it = effects.iterator();
        while (it.hasNext()) {
            OneShotFx fx = it.next();
            ClipRef ref = clips.getOrLoad(fx.pamPath, fx.clipName);
            if (ref == null) {
                continue;
            }
            if (!fx.started) {
                fx.started = true;
                fx.time = 0f;
                fx.duration = effectClipDurationSeconds(ref, fx.pamPath, fx.clipName);
            } else {
                fx.time += delta;
            }
            player.draw(batch, ref, fx.time, fx.x, fx.y, fx.scale, fx.scale, fx.loop);
            if (!fx.loop && fx.duration > 0f && fx.time >= fx.duration) {
                it.remove();
            }
        }
    }

    private float effectClipDurationSeconds(ClipRef ref, String pamPath, String clipName) {
        float seconds = player.clipDurationSeconds(pamPath, clipName);
        if (seconds > 0f) {
            return seconds;
        }
        if (ref != null && ref.duration > 0f) {
            return ref.duration;
        }
        return 1.5f;
    }

    /**
     * Phase machine for every active Egypt sandstorm: intro plays as the storm
     * starts moving, loop repeats until touchdown, then outro fades it away
     * once over the landed zombie. Also collects the freshly landed zombies
     * that must stay hidden behind their storm's outro.
     */
    private void updateSandstorms(GameModel model, float delta) {
        sandstormConcealed.clear();
        if (model.getSandstorms().isEmpty()) {
            sandstorms.clear();
            return;
        }
        for (SandstormSpawn storm : model.getSandstorms()) {
            SandstormFx fx = sandstorms.get(storm);
            if (fx == null) {
                fx = beginSandstorm(storm);
            }
            fx.clock += delta;
            float progress = storm.travelProgress();
            fx.x = fx.startX + (fx.targetX - fx.startX) * progress;
            if (!storm.hasLanded()) {
                fx.visible = true;
                if (fx.introDuration > 0f && fx.clock < fx.introDuration) {
                    fx.clip = SandstormAnim.INTRO_CLIP;
                    fx.clipTime = fx.clock;
                    fx.loop = false;
                } else {
                    fx.clip = SandstormAnim.LOOP_CLIP;
                    fx.clipTime = fx.clock - fx.introDuration;
                    fx.loop = true;
                }
            } else {
                if (!fx.landedSeen) {
                    fx.landedSeen = true;
                    fx.outroClock = 0f;
                }
                fx.outroClock += delta;
                fx.clip = SandstormAnim.OUTRO_CLIP;
                fx.clipTime = Math.min(fx.outroClock, fx.outroDuration);
                fx.loop = false;
                fx.visible = fx.outroClock < fx.outroDuration;
                if (fx.visible && storm.getSpawned() != null) {
                    sandstormConcealed.add(storm.getSpawned());
                }
            }
            cacheStormScale(fx);
        }
        Set<SandstormSpawn> live = Collections.newSetFromMap(new IdentityHashMap<>());
        live.addAll(model.getSandstorms());
        sandstorms.keySet().removeIf(storm -> !live.contains(storm));
    }

    /** First sighting of a model sandstorm: resolve placement + clip timings. */
    private SandstormFx beginSandstorm(SandstormSpawn storm) {
        if (!sandstormPamReady) {
            sandstormPamReady = true;
            clips.preloadSync(SandstormAnim.PAM_PATH, SandstormAnim.LOOP_CLIP);
        }
        SandstormFx fx = new SandstormFx();
        // Materialises past the zombie entry edge, off-screen right.
        fx.startX = layout.centerOf(storm.getLane(), layout.cols())[0]
                + SandstormAnim.START_MARGIN_PX;
        float[] target = layout.centerOf(storm.getLane(), storm.getColumn());
        fx.targetX = target[0];
        // Raised half a tile so the dust cloud doesn't sink into the lane below.
        fx.y = target[1] + layout.cellHeight() * 0.5f;
        fx.introDuration = player.clipDurationSeconds(
                SandstormAnim.PAM_PATH, SandstormAnim.INTRO_CLIP);
        fx.outroDuration = player.clipDurationSeconds(
                SandstormAnim.PAM_PATH, SandstormAnim.OUTRO_CLIP);
        if (fx.outroDuration <= 0f) {
            fx.outroDuration = 0.8f;
        }
        fx.outroDuration = Math.min(fx.outroDuration, SandstormSpawn.OUTRO_SECONDS);
        sandstorms.put(storm, fx);
        return fx;
    }

    /** Storm art is scaled to cover a fixed number of lawn cells in height. */
    private void cacheStormScale(SandstormFx fx) {
        if (fx.scale > 0f || fx.clip == null) {
            return;
        }
        Rectangle bounds = player.bounds(SandstormAnim.PAM_PATH, fx.clip);
        fx.scale = bounds != null && bounds.height > 0f
                ? layout.cellHeight() * SandstormAnim.HEIGHT_CELLS / bounds.height
                : AnimScale.LAWN;
    }

    /** Draws the storms above lawn content; their zombie hides underneath. */
    private void drawSandstorms(Batch batch) {
        for (SandstormFx fx : sandstorms.values()) {
            if (!fx.visible || fx.clip == null || fx.scale <= 0f) {
                continue;
            }
            ClipRef ref = clips.getOrLoad(SandstormAnim.PAM_PATH, fx.clip);
            if (ref == null) {
                continue;
            }
            player.draw(batch, ref, fx.clipTime, fx.x, fx.y, fx.scale, fx.scale, fx.loop);
        }
    }

    /**
     * Syncs the view gusts with the model's ice winds; each gust sweeps
     * right-to-left across its lane for {@link IceWindGust#SWEEP_SECONDS}.
     */
    private void updateIceWinds(GameModel model, float delta) {
        if (model.getIceWinds().isEmpty()) {
            iceWinds.clear();
            return;
        }
        for (IceWindGust gust : model.getIceWinds()) {
            IceWindFx fx = iceWinds.get(gust);
            if (fx == null) {
                fx = beginIceWind(gust);
            }
            fx.clock += delta;
            float progress = gust.progress();
            fx.x = fx.startX + (fx.endX - fx.startX) * progress;
        }
        Set<IceWindGust> live = Collections.newSetFromMap(new IdentityHashMap<>());
        live.addAll(model.getIceWinds());
        iceWinds.keySet().removeIf(gust -> !live.contains(gust));
    }

    /** First sighting of a model ice wind: resolve placement + art scale. */
    private IceWindFx beginIceWind(IceWindGust gust) {
        if (!iceWindPamReady) {
            iceWindPamReady = true;
            clips.preloadSync(IceWindAnim.PAM_PATH, IceWindAnim.CLIP);
        }
        IceWindFx fx = new IceWindFx();
        // Enters past the zombie entry edge and exits past the house edge.
        fx.startX = layout.centerOf(gust.getLane(), layout.cols())[0]
                + IceWindAnim.START_MARGIN_PX;
        fx.endX = layout.centerOf(gust.getLane(), 0)[0]
                - IceWindAnim.START_MARGIN_PX;
        fx.y = layout.centerY(gust.getLane());
        Rectangle bounds = player.bounds(IceWindAnim.PAM_PATH, IceWindAnim.CLIP);
        fx.scale = bounds != null && bounds.height > 0f
                ? layout.cellHeight() * IceWindAnim.HEIGHT_CELLS / bounds.height
                : AnimScale.LAWN;
        iceWinds.put(gust, fx);
        return fx;
    }

    /** Draws the ice winds above lawn content so frost puffs ride over plants. */
    private void drawIceWinds(Batch batch) {
        for (IceWindFx fx : iceWinds.values()) {
            if (fx.scale <= 0f) {
                continue;
            }
            ClipRef ref = clips.getOrLoad(IceWindAnim.PAM_PATH, IceWindAnim.CLIP);
            if (ref == null) {
                continue;
            }
            player.draw(batch, ref, fx.clock, fx.x, fx.y, fx.scale, fx.scale, true);
        }
    }

    private void drawProjectile(Batch batch, Projectile projectile, float delta) {
        if (projectile == null) {
            return;
        }
        AnimPose pose = projectileAdapter.poseFor(projectile);
        if (pose == null) {
            entityOverlay.drawProjectile(batch, projectile);
            return;
        }
        projectileWorldCenter(projectile, xyTmp);
        drawPose(batch, projectile, pose, xyTmp[0], xyTmp[1], AnimScale.forProjectile(pose), NO_PHASE,
                0f, delta, pose.cacheKey());
    }

    private void projectileWorldCenter(Projectile projectile, float[] out) {
        float[] xy = layout.centerOf(projectile.getY(), projectile.getX());
        out[0] = xy[0];
        out[1] = xy[1];
        if (projectile instanceof Splash splash) {
            out[1] += splash.getVisualHeight() * layout.cellHeight();
        }
    }

    private void prepareBowlingWalnuts(GameModel model) {
        if (!(model.getCurrentLevel() instanceof WallnutBowlingLevel bowling)) {
            bowlingWalnuts = List.of();
            return;
        }
        harvestBowlingExplosions(bowling);
        List<BowlingWalnut> active = bowling.getActiveWalnuts();
        bowlingWalnuts = active;
        for (BowlingWalnut walnut : active) {
            if (walnut != null) {
                seenThisFrame.add(walnut);
            }
        }
    }

    public void preloadVases() {
        for (String pam : VaseBreakerAnim.allVasePams()) {
            clips.preloadSync(pam,
                    VaseBreakerAnim.CLIP_DROP,
                    VaseBreakerAnim.CLIP_IDLE,
                    VaseBreakerAnim.CLIP_BREAK);
        }
        clips.preloadSync(VaseBreakerAnim.GARGANTUAR_ZOMBIE,
                "idle", "walk", "eat", "smash_left", "fire", "cannon_fire", "die");
    }

    public void preloadCraters() {
        textures.loadSync(BeghouledArt.ATLAS_GROUP);
        textures.loadSync(BeghouledArt.ATLAS_PAGE);
        craterRegion = textures.region(BeghouledArt.CRATER_TILE);
        if (craterRegion == null) {
            craterRegion = textures.region(BeghouledArt.CRATER_LARGE);
        }
    }

    private void harvestBeghouledClears(GameModel model) {
        if (!(model.getCurrentLevel() instanceof BeghouledLevel beghouled)) {
            return;
        }
        for (int[] cell : beghouled.consumeLastClearedCells()) {
            if (cell == null || cell.length < 2) {
                continue;
            }
            float[] xy = layout.centerOf(cell[0], cell[1]);
            frontEffects.add(new OneShotFx(
                    EffectPamPaths.PLANTFOOD_FX, EffectPamPaths.PLANTFOOD_FX_ON,
                    xy[0], xy[1], AnimScale.PLANT * 0.85f, false));
        }
    }

    private void drawCraters(Batch batch, GameModel model, int row) {
        GameMap map = model.getMap();
        if (map == null) {
            return;
        }
        TextureRegion crater = ensureCraterRegion();
        if (crater == null) {
            return;
        }
        float w = crater.getRegionWidth();
        float h = crater.getRegionHeight();
        int cols = Math.min(layout.cols(), map.getCols());
        for (int c = 0; c < cols; c++) {
            Cell cell = map.getCell(c, row);
            if (cell == null || !isCraterCell(cell)) {
                continue;
            }
            float[] xy = layout.centerOf(row, c);
            batch.draw(crater, xy[0] - w * 0.5f, xy[1] - h * 0.4f, w, h);
        }
    }

    private static boolean isCraterCell(Cell cell) {
        return cell.getGroundType() == GroundType.CRATER
                || cell.getTerrainStrategy() instanceof CraterTerrainStrategy;
    }

    private void drawFireTiles(Batch batch, GameModel model, float delta, int row) {
        GameMap map = model != null ? model.getMap() : null;
        if (map == null || batch == null || player == null) {
            return;
        }
        preloadFireImpactPam();
        float scale = AnimScale.PLANT * 0.8f;

        int cols = Math.min(layout.cols(), map.getCols());
        for (int c = 0; c < cols; c++) {
            Cell cell = map.getCell(c, row);
            if (cell == null || cell.getGroundType() != GroundType.FIRE) {
                continue;
            }
            FireTileFx fx = fireTileFx.get(cell);
            if (fx == null) {
                fx = new FireTileFx();
                fireTileFx.put(cell, fx);
            }
            if (fx.phase == FireTileFxPhase.OUTRO) {
                fx.phase = FireTileFxPhase.INTRO;
                fx.time = 0f;
            }
            drawFireTileFx(batch, fx, row, c, scale, delta);
        }

        List<Cell> finished = null;
        for (Map.Entry<Cell, FireTileFx> e : fireTileFx.entrySet()) {
            Cell cell = e.getKey();
            if (cell == null || cell.getRow() != row) {
                continue;
            }
            if (cell.getGroundType() == GroundType.FIRE) {
                continue;
            }
            FireTileFx fx = e.getValue();
            if (fx.phase != FireTileFxPhase.OUTRO) {
                fx.phase = FireTileFxPhase.OUTRO;
                fx.time = 0f;
            }
            boolean done = drawFireTileFx(batch, fx, row, cell.getColumn(), scale, delta);
            if (done) {
                if (finished == null) {
                    finished = new ArrayList<>();
                }
                finished.add(cell);
            }
        }
        if (finished != null) {
            for (Cell cell : finished) {
                fireTileFx.remove(cell);
            }
        }
    }

    private boolean drawFireTileFx(Batch batch, FireTileFx fx, int row, int col,
                                   float scale, float delta) {
        String clip = fireTileFxClip(fx.phase);
        ClipRef ref = clips.getOrLoad(EffectPamPaths.POWER_UP_FIRE_IMPACT, clip);
        if (ref == null) {
            return fx.phase == FireTileFxPhase.OUTRO;
        }
        float duration = effectClipDurationSeconds(ref, EffectPamPaths.POWER_UP_FIRE_IMPACT, clip);
        boolean loop = fx.phase == FireTileFxPhase.IDLE;
        float[] xy = layout.centerOf(row, col);
        player.draw(batch, ref, fx.time, xy[0], xy[1], scale, scale, loop);
        fx.time += Math.max(0f, delta);

        if (fx.phase == FireTileFxPhase.INTRO && duration > 0f && fx.time >= duration) {
            fx.phase = FireTileFxPhase.IDLE;
            fx.time = 0f;
        } else if (fx.phase == FireTileFxPhase.INTRO && duration <= 0f) {
            fx.phase = FireTileFxPhase.IDLE;
            fx.time = 0f;
        } else if (fx.phase == FireTileFxPhase.OUTRO && duration > 0f && fx.time >= duration) {
            return true;
        } else if (fx.phase == FireTileFxPhase.OUTRO && duration <= 0f) {
            return true;
        }
        return false;
    }

    private void preloadFireImpactPam() {
        if (fireImpactPamReady) {
            return;
        }
        clips.getOrLoad(EffectPamPaths.POWER_UP_FIRE_IMPACT, EffectPamPaths.POWER_UP_FIRE_IMPACT_INTRO);
        clips.getOrLoad(EffectPamPaths.POWER_UP_FIRE_IMPACT, EffectPamPaths.POWER_UP_FIRE_IMPACT_IDLE);
        clips.getOrLoad(EffectPamPaths.POWER_UP_FIRE_IMPACT, EffectPamPaths.POWER_UP_FIRE_IMPACT_OUTRO);
        fireImpactPamReady = true;
    }

    private static String fireTileFxClip(FireTileFxPhase phase) {
        return switch (phase) {
            case INTRO -> EffectPamPaths.POWER_UP_FIRE_IMPACT_INTRO;
            case IDLE -> EffectPamPaths.POWER_UP_FIRE_IMPACT_IDLE;
            case OUTRO -> EffectPamPaths.POWER_UP_FIRE_IMPACT_OUTRO;
        };
    }

    private enum FireTileFxPhase {
        INTRO, IDLE, OUTRO
    }

    private static final class FireTileFx {
        FireTileFxPhase phase = FireTileFxPhase.INTRO;
        float time;
    }

    private void drawZombossFireballs(Batch batch, GameModel model, float delta) {
        if (batch == null || model == null || catalog == null || player == null) {
            return;
        }
        ZombieInstance boss = model.findZomboss();
        if (boss == null) {
            return;
        }
        ZombossBehavior behavior = (ZombossBehavior) boss.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (behavior == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName("ZOMBOSS_DARK_FIREBALL");
        if (entry == null) {
            return;
        }
        ClipRef fall = clips.getOrLoad(entry.path(), "fall");
        if (fall == null) {
            return;
        }
        float scale = AnimScale.PLANT * 0.9f;
        for (ZombossPendingImpact impact : behavior.getPendingImpacts()) {
            if (impact == null || impact.isResolved()) {
                continue;
            }
            float[] target = layout.centerOf(impact.getRow(), impact.getCol());
            float[] origin = layout.centerOf(boss.getGridY(), boss.getGridX());
            float t = impact.progress01();
            float x = origin[0] + (target[0] - origin[0]) * t;
            float y = origin[1] + (target[1] - origin[1]) * t
                    + (float) Math.sin(t * Math.PI) * layout.cellHeight() * 1.4f;
            float duration = Math.max(0.05f, fall.duration);
            float state = t * duration;
            player.draw(batch, fall, state, x, y, scale, scale, false);
        }
    }

    private TextureRegion ensureCraterRegion() {
        if (craterRegion != null) {
            return craterRegion;
        }
        craterRegion = textures.region(BeghouledArt.CRATER_TILE);
        if (craterRegion == null) {
            craterRegion = textures.region(BeghouledArt.CRATER_LARGE);
        }
        return craterRegion;
    }

    private void applyBeghouledMotion(PlantInstance plant, float[] xy, float delta) {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (!(model != null && model.getCurrentLevel() instanceof BeghouledLevel)) {
            beghouledMotion.remove(plant);
            return;
        }
        BeghouledMotion motion = beghouledMotion.get(plant);
        if (motion == null) {
            motion = new BeghouledMotion();
            motion.toX = xy[0];
            motion.toY = xy[1];
            motion.fromX = xy[0];
            motion.fromY = xy[1] + layout.cellHeight() * 1.15f;
            motion.t = 0f;
            beghouledMotion.put(plant, motion);
        } else {
            float dx = xy[0] - motion.toX;
            float dy = xy[1] - motion.toY;
            if (dx * dx + dy * dy > 0.25f) {
                float u = beghouledEase(motion.t);
                motion.fromX = motion.fromX + (motion.toX - motion.fromX) * u;
                motion.fromY = motion.fromY + (motion.toY - motion.fromY) * u;
                motion.toX = xy[0];
                motion.toY = xy[1];
                motion.t = 0f;
            } else {
                motion.toX = xy[0];
                motion.toY = xy[1];
            }
        }
        motion.t = Math.min(1f, motion.t + Math.max(0f, delta) / BEGHOULED_MOVE_SEC);
        float u = beghouledEase(motion.t);
        xy[0] = motion.fromX + (motion.toX - motion.fromX) * u;
        xy[1] = motion.fromY + (motion.toY - motion.fromY) * u;
    }

    private static float beghouledEase(float t) {
        float u = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - u) * (1f - u);
    }

    private static final class BeghouledMotion {
        float fromX;
        float fromY;
        float toX;
        float toY;
        float t;
    }

    public void playVaseBreak(String pamPath, int col, int row) {
        if (pamPath == null) {
            return;
        }
        float[] xy = layout.centerOf(row, col);
        frontEffects.add(new OneShotFx(
                pamPath, VaseBreakerAnim.CLIP_BREAK, xy[0], xy[1], AnimScale.PLANT, false));
        vaseAge.remove(vaseKey(col, row));
    }

    private void drawVases(Batch batch, GameModel model, float delta, int row, int rows) {
        if (!(model.getCurrentLevel() instanceof VaseBreakerLevel level)) {
            return;
        }
        for (Vase vase : level.getVases()) {
            if (vase.isBroken() || vase.getPosition() == null) {
                continue;
            }
            int col = vase.getPosition().getX();
            int vaseRow = vase.getPosition().getY();
            if (clampRow(vaseRow, rows) != row) {
                continue;
            }
            String key = vaseKey(col, vaseRow);
            float age = vaseAge.getOrDefault(key, 0f) + Math.max(0f, delta);
            vaseAge.put(key, age);
            String pam = VaseBreakerAnim.pamPath(vase);
            boolean dropping = age < VaseBreakerAnim.DROP_SECONDS;
            String clip = dropping ? VaseBreakerAnim.CLIP_DROP : VaseBreakerAnim.CLIP_IDLE;
            float time = dropping ? age : age - VaseBreakerAnim.DROP_SECONDS;
            ClipRef ref = clips.getOrLoad(pam, clip);
            if (ref == null) {
                continue;
            }
            float[] xy = layout.centerOf(vaseRow, col);
            player.draw(batch, ref, time, xy[0], xy[1],
                    AnimScale.PLANT, AnimScale.PLANT, !dropping);
        }
    }

    private void pruneVaseAge(GameModel model) {
        if (!(model.getCurrentLevel() instanceof VaseBreakerLevel level)) {
            vaseAge.clear();
            return;
        }
        HashSet<String> live = new HashSet<>();
        for (Vase vase : level.getVases()) {
            if (!vase.isBroken() && vase.getPosition() != null) {
                live.add(vaseKey(vase.getPosition().getX(), vase.getPosition().getY()));
            }
        }
        vaseAge.keySet().removeIf(key -> !live.contains(key));
    }

    private static String vaseKey(int col, int row) {
        return col + "," + row;
    }

    private void drawBowlingWalnuts(Batch batch, float delta, int row, int rows) {
        if (bowlingWalnuts.isEmpty()) {
            return;
        }
        for (BowlingWalnut walnut : bowlingWalnuts) {
            if (walnut == null) {
                continue;
            }
            if (clampRow(Math.round(walnut.getY()), rows) != row) {
                continue;
            }
            drawBowlingWalnut(batch, walnut, delta);
        }
    }

    private void harvestBowlingExplosions(WallnutBowlingLevel bowling) {
        for (FloatPoint point : bowling.drainExplosions()) {
            if (point == null) {
                continue;
            }
            float[] xy = layout.centerOf(point.getY(), point.getX());
            spawnExplosionSpecs(ExplosivePlantFx.specsForName("Explode-o-nut"), null, xy[0], xy[1]);
        }
    }

    private void drawBowlingWalnut(Batch batch, BowlingWalnut walnut, float delta) {
        String plantName = BowlingWalnutAnim.artPlantName(walnut);
        ClipRef ref = plantIdleClip(plantName);
        if (ref == null) {
            if (entityOverlay != null) {
                entityOverlay.drawProjectile(batch, walnut);
            }
            return;
        }
        AnimClock clock = clocks.computeIfAbsent(walnut, k -> new AnimClock());
        clock.time += Math.max(0f, delta);
        float[] xy = layout.centerOf(walnut.getY(), walnut.getX());
        float scale = BowlingWalnutAnim.scale(walnut);
        float degrees = BowlingWalnutAnim.rollDegrees(walnut, clock.time);
        batch.flush();
        batchTransform.set(batch.getTransformMatrix());
        popTransform.set(batchTransform)
            .translate(xy[0], xy[1], 0f)
            .rotate(0f, 0f, 1f, degrees)
            .translate(-xy[0], -xy[1], 0f);
        batch.setTransformMatrix(popTransform);
        player.draw(batch, ref, 0f, xy[0], xy[1], scale, scale, true);
        batch.flush();
        batch.setTransformMatrix(batchTransform);
    }

    /** Idle PAM at a world point — drag-to-plant cursor ghost. */
    public void drawPlantIdle(Batch batch, String plantName, float x, float y, float time) {
        drawPlantIdle(batch, plantName, x, y, time, AnimScale.PLANT);
    }

    public void drawPlantIdle(Batch batch, String plantName, float x, float y, float time, float scale) {
        String name = resolveIdlePlantName(plantName);
        ClipRef ref = plantIdleClip(name);
        if (ref != null) {
            player.draw(batch, ref, time, x, y, scale, scale, true);
            return;
        }
        PlantSpritesheetCatalog.ClipSpec spec = plantIdleSheetSpec(name);
        if (spec == null || sheetClips == null) {
            return;
        }
        SpritesheetClipCache.SheetAnim sheet = sheetClips.getOrLoad(spec);
        if (sheet == null || sheet.animation() == null) {
            return;
        }
        TextureRegion frame = sheet.animation().getKeyFrame(time, true);
        if (frame == null) {
            return;
        }
        float sheetScale = AnimScale.PLANT_SHEET;
        float w = frame.getRegionWidth() * sheetScale;
        float h = frame.getRegionHeight() * sheetScale;
        batch.draw(frame, x - w * 0.5f, y - h * 0.5f, w, h);
    }

    public void preloadPlantIdle(String plantName) {
        String name = resolveIdlePlantName(plantName);
        if (plantIdleClip(name) != null) {
            return;
        }
        PlantSpritesheetCatalog.ClipSpec spec = plantIdleSheetSpec(name);
        if (spec != null && sheetClips != null) {
            sheetClips.getOrLoad(spec);
        }
    }

    public void drawZombieIdle(Batch batch, String zombieName, float x, float y, float time,
                               Chapter chapter) {
        if (zombieName == null || catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.forZombie(zombieName, chapter);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, "idle", "walk", "idle2", "idle1");
        if (clip == null) {
            return;
        }
        ClipRef ref = clips.getOrLoad(entry.path(), clip);
        if (ref != null) {
            float sx = ZombotanyAnim.isPlantHeadName(zombieName) ? -AnimScale.ZOMBIE : AnimScale.ZOMBIE;
            Map<String, Boolean> visibility = ZombieAnimAdapter.almanacArmorVisibility(zombieName, entry);
            if (visibility != null) {
                player.draw(batch, ref, time, x, y, sx, AnimScale.ZOMBIE, true, visibility);
            } else {
                player.draw(batch, ref, time, x, y, sx, AnimScale.ZOMBIE, true);
            }
        }
    }

    public void preloadZombieIdle(String zombieName, Chapter chapter) {
        if (zombieName == null || catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.forZombie(zombieName, chapter);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, "idle", "walk", "idle2", "idle1");
        if (clip != null) {
            clips.getOrLoad(entry.path(), clip);
        }
    }

    private static String resolveIdlePlantName(String plantName) {
        if ("Giant Wall-nut".equalsIgnoreCase(plantName)) {
            return "Wall-nut";
        }
        return plantName;
    }

    private ClipRef plantIdleClip(String plantName) {
        if (plantName == null || catalog == null) {
            return null;
        }
        PamCatalog.PamEntry entry = catalog.forPlant(plantName);
        if (entry == null) {
            return null;
        }
        String clip = catalog.resolveClip(entry, "idle", "idle2", "idle1", "loop");
        return clip == null ? null : clips.getOrLoad(entry.path(), clip);
    }

    private PlantSpritesheetCatalog.ClipSpec plantIdleSheetSpec(String plantName) {
        if (plantName == null || plantSheets == null) {
            return null;
        }
        PlantSpritesheetCatalog.ClipSpec spec = plantSheets.resolveClip(plantName, "idle", "idle2");
        if (spec != null) {
            return spec;
        }
        return plantSheets.idleFallback(plantName);
    }

    private void drawGraves(Batch batch, GameModel model, float delta, int row) {
        GameMap map = model.getMap();
        if (map == null || catalog == null) {
            return;
        }
        Chapter chapter = model.getChapter();
        int cols = map.getCols();
        for (int col = 0; col < cols; col++) {
            Cell cell = map.getCell(col, row);
            if (cell == null) {
                continue;
            }
            if (cell.getPlaceable(PlacableLayer.GROUND) instanceof Grave grave
                && !grave.isDestroyed()) {
                PamCatalog.PamEntry entry = catalog.byName(GraveAnim.pamFor(chapter, grave));
                if (entry == null) {
                    continue;
                }
                drawGrave(batch, grave, row, col, entry.path(), delta);
            }
        }
    }

    private void drawGrave(Batch batch, Grave grave, int row, int col,
                           String path, float delta) {
        ClipRef ref = clips.getOrLoad(path, GraveAnim.clipFor(grave));
        if (ref == null) {
            return;
        }
        seenThisFrame.add(grave);
        float u = tickGraveEmerge(grave, delta);
        float[] xy = layout.centerOf(row, col);
        float flash = tickHitFlash(grave, grave.getHp(), delta);
        drawSquashStretch(batch, ref, 0f, xy[0], xy[1], AnimScale.PLANT, u, false, flash);
        lastGraves.put(grave, new LiveSnap(
            AnimPose.looping(path, GraveAnim.clipFor(grave), ZombieAnimRole.IDLE),
            xy[0], xy[1], false, u));
    }

    private float tickGraveEmerge(Grave grave, float delta) {
        float u = graveEmerge.getOrDefault(grave, 0f);
        graveEmerge.put(grave, Math.min(1f, u + delta / GraveAnim.EMERGE_DURATION));
        return u;
    }

    /** Killing blow removes the plant/grave before draw; hold last pose through the flash. */
    private void drawPlantGhosts(Batch batch, Set<PlantInstance> live, float delta, int row) {
        for (PlantInstance plant : new ArrayList<>(lastPlants.keySet())) {
            if (live.contains(plant)) {
                continue;
            }
            LiveSnap snap = lastPlants.get(plant);
            if (snap == null || layout.rowAt(snap.y) != row) {
                continue;
            }
            float flash = tickHitFlash(plant, 0, delta);
            if (flash <= 0f || snap.pose == null) {
                lastPlants.remove(plant);
                continue;
            }
            drawPose(batch, plant, snap.pose, snap.x, snap.y, AnimScale.forPlant(snap.pose), NO_PHASE, flash, 0f);
        }
    }

    private void drawGraveGhosts(Batch batch, float delta, int row) {
        for (Grave grave : new ArrayList<>(lastGraves.keySet())) {
            if (seenThisFrame.contains(grave)) {
                continue;
            }
            LiveSnap snap = lastGraves.get(grave);
            if (snap == null || layout.rowAt(snap.y) != row) {
                continue;
            }
            float flash = tickHitFlash(grave, 0, delta);
            if (flash <= 0f || snap.pose == null) {
                lastGraves.remove(grave);
                continue;
            }
            ClipRef ref = clips.getOrLoad(snap.pose.pamPath(), snap.pose.clipName());
            if (ref == null) {
                lastGraves.remove(grave);
                continue;
            }
            seenThisFrame.add(grave);
            drawSquashStretch(batch, ref, 0f, snap.x, snap.y, AnimScale.PLANT, snap.time, false,
                flash);
        }
    }

    /** Tomb pop (and wizard plant vanish/emerge). {@code u} 0 is pancake, 1 is rest. */
    private void drawSquashStretch(Batch batch, ClipRef ref, float time,
                                   float x, float y, float baseScale, float u, boolean loop) {
        drawSquashStretch(batch, ref, time, x, y, baseScale, u, loop, 0f);
    }

    private void drawSquashStretch(Batch batch, ClipRef ref, float time,
                                   float x, float y, float baseScale, float u, boolean loop,
                                   float flash) {
        float sxN = GraveAnim.scaleX(u);
        float syN = GraveAnim.scaleY(u);
        float yPin = y + (syN - 1f) * baseScale * (GraveAnim.CANVAS * 0.5f);
        float sx = baseScale * sxN;
        float sy = baseScale * syN;
        player.draw(batch, ref, time, x, yPin, sx, sy, loop);
        overlayHitFlash(batch, flash, () -> player.draw(batch, ref, time, x, yPin, sx, sy, loop));
    }

    /**
     * @return true if this plant was drawn as a vanishing plant, sheep, or emerging plant
     */
    private boolean drawWizardSheep(Batch batch, PlantInstance plant, float delta) {
        SheepFx fx = sheepFx.get(plant);
        if (plant.isTransformed()) {
            if (fx == null) {
                fx = new SheepFx();
                fx.idleClip = ThreadLocalRandom.current().nextBoolean()
                    ? WizardAnim.IDLE2_CLIP : WizardAnim.IDLE3_CLIP;
                sheepFx.put(plant, fx);
            }
        } else if (fx != null && fx.phase != SheepPhase.LEAVE && fx.phase != SheepPhase.EMERGE) {
            if (fx.phase == SheepPhase.VANISH) {
                fx.phase = SheepPhase.EMERGE;
                fx.time = Math.max(0f, GraveAnim.EMERGE_DURATION - fx.time);
            } else {
                fx.phase = SheepPhase.LEAVE;
                fx.time = 0f;
            }
        }
        if (fx == null) {
            return false;
        }
        seenThisFrame.add(plant);
        float flash = tickHitFlash(plant, plantVitality(plant), delta);
        Point pos = plant.getPosition();
        if (pos == null) {
            sheepFx.remove(plant);
            return false;
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        preloadWizardSheepening();
        switch (fx.phase) {
            case VANISH -> {
                if (drawPlantPop(batch, plant, xy[0], xy[1], 1f - popU(fx.time), flash)) {
                    fx.time += delta;
                    if (fx.time >= GraveAnim.EMERGE_DURATION) {
                        fx.phase = SheepPhase.APPEAR;
                        fx.time = 0f;
                    }
                    return true;
                }
                fx.phase = SheepPhase.APPEAR;
                fx.time = 0f;
                return drawSheepening(batch, xy[0], xy[1], fx, flash, delta);
            }
            case APPEAR, IDLE, LEAVE -> {
                return drawSheepening(batch, xy[0], xy[1], fx, flash, delta);
            }
            case EMERGE -> {
                if (drawPlantPop(batch, plant, xy[0], xy[1], popU(fx.time), flash)) {
                    fx.time += delta;
                    if (fx.time >= GraveAnim.EMERGE_DURATION) {
                        sheepFx.remove(plant);
                    }
                    return true;
                }
                sheepFx.remove(plant);
                return false;
            }
        }
        return false;
    }

    private static float popU(float time) {
        return Math.max(0f, Math.min(1f, time / GraveAnim.EMERGE_DURATION));
    }

    private boolean drawPlantPop(Batch batch, PlantInstance plant,
                                 float x, float y, float u, float flash) {
        AnimPose pose = plantAdapter.poseFor(plant);
        if (pose == null || pose.isSpritesheet()) {
            return false;
        }
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return false;
        }
        drawSquashStretch(batch, ref, 0f, x, y, AnimScale.PLANT * pose.scale(), u, pose.loop(),
            flash);
        return true;
    }

    private boolean drawSheepening(Batch batch, float x, float y, SheepFx fx,
                                   float flash, float delta) {
        PamCatalog.PamEntry entry = catalog == null ? null : catalog.byName(WizardAnim.SHEEPENING_PAM);
        if (entry == null) {
            fx.time += delta;
            return true;
        }
        String clip = switch (fx.phase) {
            case APPEAR -> WizardAnim.APPEAR_CLIP;
            case LEAVE -> WizardAnim.LEAVE_CLIP;
            default -> fx.idleClip;
        };
        boolean loop = fx.phase == SheepPhase.IDLE;
        ClipRef ref = clips.getOrLoad(entry.path(), catalog.resolveClip(entry, clip));
        if (ref == null) {
            fx.time += delta;
            return true;
        }
        player.draw(batch, ref, fx.time, x, y, AnimScale.PLANT, AnimScale.PLANT, loop);
        overlayHitFlash(batch, flash, () ->
            player.draw(batch, ref, fx.time, x, y, AnimScale.PLANT, AnimScale.PLANT, loop));
        fx.time += delta;
        if (fx.phase == SheepPhase.APPEAR && fx.time >= ref.duration) {
            fx.phase = SheepPhase.IDLE;
            fx.time = 0f;
        } else if (fx.phase == SheepPhase.LEAVE && fx.time >= ref.duration) {
            fx.phase = SheepPhase.EMERGE;
            fx.time = 0f;
        }
        return true;
    }

    private void preloadWizardSheepening() {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(WizardAnim.SHEEPENING_PAM);
        if (entry == null) {
            return;
        }
        clips.getOrLoad(entry.path(), catalog.resolveClip(entry, WizardAnim.APPEAR_CLIP));
        clips.getOrLoad(entry.path(), catalog.resolveClip(entry, WizardAnim.LEAVE_CLIP));
        clips.getOrLoad(entry.path(), catalog.resolveClip(entry, WizardAnim.IDLE2_CLIP));
        clips.getOrLoad(entry.path(), catalog.resolveClip(entry, WizardAnim.IDLE3_CLIP));
    }

    private void drawSuns(Batch batch, GameModel model, float delta) {
        IdentityHashMap<Sun, StealSunBehavior.SunPull> pulled = pulledSuns(model);
        for (ZombieInstance zombie : model.getZombies()) {
            StealSunBehavior steal = (StealSunBehavior) zombie.getBehavior(ZombieBehaviorType.STEAL_SUN);
            if (steal == null || steal.getPulls().isEmpty()) {
                continue;
            }
            if (!zombieWorldCenter(zombie, xyTmp)) {
                continue;
            }
            float destX = xyTmp[0];
            float destY = xyTmp[1];
            for (StealSunBehavior.SunPull pull : steal.getPulls()) {
                Sun sun = pull.sun();
                if (sun == null) {
                    continue;
                }
                float[] start = pullWorld(pull);
                float u = Math.max(0f, Math.min(1f, pull.t()));
                u = u * u * (3f - 2f * u);
                drawSunToken(batch, sun,
                    start[0] + (destX - start[0]) * u,
                    start[1] + (destY - start[1]) * u,
                    delta);
            }
        }
        List<Sun> tokens = model.getActiveSuns();
        if (tokens == null) {
            drawSunFlights(batch, delta);
            return;
        }
        for (Sun sun : tokens) {
            if (pulled.containsKey(sun)) {
                continue;
            }
            writeSunDrawPos(sun, xyTmp);
            drawSunToken(batch, sun, xyTmp[0], xyTmp[1], delta);
        }
        drawSunFlights(batch, delta);
    }

    public Sun pickSun(GameModel model, float worldX, float worldY) {
        if (model == null || model.getActiveSuns() == null) {
            return null;
        }
        IdentityHashMap<Sun, StealSunBehavior.SunPull> pulled = pulledSuns(model);
        Sun best = null;
        float bestD = 0f;
        for (Sun sun : model.getActiveSuns()) {
            if (pulled.containsKey(sun)) {
                continue;
            }
            writeSunDrawPos(sun, xyTmp);
            if (!SunCollect.hits(xyTmp[0], xyTmp[1], worldX, worldY)) {
                continue;
            }
            float dx = worldX - xyTmp[0];
            float dy = worldY - xyTmp[1];
            float d = dx * dx + dy * dy;
            if (best == null || d < bestD) {
                best = sun;
                bestD = d;
            }
        }
        return best;
    }

    public void writeSunDrawPos(Sun sun, float[] out) {
        float[] dest = sunWorld(sun);
        float x = dest[0];
        float y = dest[1];
        if (sun.isFalling()) {
            float t = Math.max(0f, Math.min(1f, sun.fallProgress()));
            t = t * t * (3f - 2f * t);
            float[] start = sun.hasOrigin()
                ? originWorld(sun)
                : new float[]{dest[0], LawnLayout.WORLD_HEIGHT};
            x = start[0] + (dest[0] - start[0]) * t;
            y = start[1] + (dest[1] - start[1]) * t;
        }
        out[0] = x;
        out[1] = y;
    }

    public void startSunCollect(Sun sun, float x0, float y0, float x1, float y1) {
        if (sun == null) {
            return;
        }
        sunFlights.add(new SunFlight(sun, x0, y0, x1, y1));
    }

    private IdentityHashMap<Sun, StealSunBehavior.SunPull> pulledSuns(GameModel model) {
        IdentityHashMap<Sun, StealSunBehavior.SunPull> pulled = new IdentityHashMap<>();
        if (model.getZombies() == null) {
            return pulled;
        }
        for (ZombieInstance zombie : model.getZombies()) {
            StealSunBehavior steal = (StealSunBehavior) zombie.getBehavior(ZombieBehaviorType.STEAL_SUN);
            if (steal == null || steal.getPulls().isEmpty()) {
                continue;
            }
            for (StealSunBehavior.SunPull pull : steal.getPulls()) {
                if (pull.sun() != null) {
                    pulled.put(pull.sun(), pull);
                }
            }
        }
        return pulled;
    }

    private void drawSunFlights(Batch batch, float delta) {
        for (int i = sunFlights.size() - 1; i >= 0; i--) {
            SunFlight flight = sunFlights.get(i);
            flight.elapsed += delta;
            if (SunCollect.done(flight.elapsed)) {
                sunFlights.remove(i);
                continue;
            }
            float x = flight.x1;
            float y = flight.y1;
            float sx = 1f;
            float sy = 1f;
            if (SunCollect.flying(flight.elapsed)) {
                float u = SunCollect.flyU(flight.elapsed);
                x = flight.x0 + (flight.x1 - flight.x0) * u;
                y = flight.y0 + (flight.y1 - flight.y0) * u;
            } else {
                float u = SunCollect.vanishU(flight.elapsed);
                sx = GraveAnim.scaleX(u);
                sy = GraveAnim.scaleY(u);
            }
            drawSunToken(batch, flight.sun, x, y, delta, sx, sy);
        }
    }

    private float[] pullWorld(StealSunBehavior.SunPull pull) {
        float[] xy = layout.centerOf(pull.startRow(), pull.startCol());
        xy[0] += pull.startOffsetX() * layout.cellWidth();
        xy[1] += pull.startOffsetY() * layout.cellHeight();
        if (pull.startFallDuration() > 0f && pull.startFallRemaining() > 0f) {
            float t = 1f - pull.startFallRemaining() / pull.startFallDuration();
            t = Math.max(0f, Math.min(1f, t));
            xy[1] = LawnLayout.WORLD_HEIGHT + (xy[1] - LawnLayout.WORLD_HEIGHT) * t;
        }
        return xy;
    }

    private float[] sunWorld(Sun sun) {
        float[] xy = layout.centerOf(sun.getY(), sun.getX());
        xy[0] += sun.getOffsetX() * layout.cellWidth();
        xy[1] += sun.getOffsetY() * layout.cellHeight();
        return xy;
    }

    private float[] originWorld(Sun sun) {
        return layout.centerOf(sun.getOriginY(), sun.getOriginX());
    }

    private void drawSunToken(Batch batch, Sun sun, float x, float y, float delta) {
        drawSunToken(batch, sun, x, y, delta, 1f, 1f);
    }

    private void drawSunToken(Batch batch, Sun sun, float x, float y, float delta,
                              float sxN, float syN) {
        if (catalog == null) {
            return;
        }
        String pamName = sunPam(sun);
        PamCatalog.PamEntry entry = catalog.byName(pamName);
        if (entry == null && !SUN_PAM.equals(pamName)) {
            entry = catalog.byName(SUN_PAM);
        }
        if (entry == null) {
            return;
        }
        String[] preferred = preferredClips(sun);
        String clip = catalog.resolveClip(entry, preferred);
        boolean loop = sun == null || !sun.isTransitioning();
        AnimPose pose = loop ? AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE)
                             : AnimPose.once(entry.path(), clip, ZombieAnimRole.IDLE);
        float baseScale = AnimScale.SUN * sunScale(sun);
        float stateTime;
        if (sxN == 1f && syN == 1f) {
            stateTime = drawPose(batch, sun, pose, x, y, baseScale, NO_PHASE, 0f, delta);
        } else {
            seenThisFrame.add(sun);
            ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
            if (ref == null) {
                return;
            }
            stateTime = advanceClock(sun, pose.cacheKey(), delta);
            player.draw(batch, ref, stateTime, x, y,
                baseScale * sxN, baseScale * syN, pose.loop());
        }
        if (sun != null && sun.isTransitioning()) {
            float dur = PamCatalog.clipDurationSeconds(entry, clip);
            if (dur <= 0f) {
                dur = 0.5f;
            }
            if (stateTime >= dur) {
                sun.completeTransition();
            }
        }
    }

    private void harvestRadioactiveSunExplosions(GameModel model) {
        if (model == null || catalog == null) {
            return;
        }
        List<Point> explosions = model.drainRadioactiveSunExplosions();
        if (explosions == null || explosions.isEmpty()) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(SUN_BOMB_PAM);
        if (entry == null) {
            entry = catalog.byName(SUN_PAM);
        }
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, "attack", "animation");
        for (Point pt : explosions) {
            float[] xy = layout.centerOf(pt.getY(), pt.getX());
            frontEffects.add(new OneShotFx(entry.path(), clip, xy[0], xy[1], AnimScale.SUN * 1.25f, false));
        }
    }

    static String sunPam(Sun sun) {
        if (sun == null || sun.getType() == null) {
            return SUN_PAM;
        }
        if (sun.getType() == SunType.RADIOACTIVE || sun.isTransitioning() || sun.isTransitioned()) {
            return SUN_BOMB_PAM;
        }
        return SUN_PAM;
    }

    static float sunScale(Sun sun) {
        if (sun == null || sun.getType() == null) {
            return 1f;
        }
        if (sun.isTransitioned()) {
            return 1f;
        }
        return switch (sun.getType()) {
            case SPECIAL -> 1.10f;
            case RADIOACTIVE -> 1.25f;
            default -> 1f;
        };
    }

    static String[] preferredClips(Sun sun) {
        if (sun == null || sun.getType() == null) {
            return new String[]{"animation"};
        }
        if (sun.isTransitioning()) {
            return new String[]{"transition", "animation"};
        }
        if (sun.isTransitioned()) {
            return new String[]{"normalSunIdle", "animation"};
        }
        if (sun.getType() == SunType.RADIOACTIVE) {
            if (sun.isFalling()) {
                float p = sun.fallProgress();
                if (p < 0.333f) {
                    return new String[]{"animation", "animation2", "animation3", "blue"};
                } else if (p < 0.666f) {
                    return new String[]{"animation2", "animation3", "animation", "blue"};
                } else {
                    return new String[]{"animation3", "animation2", "animation", "blue"};
                }
            }
            return new String[]{"animation3", "animation2", "animation", "blue"};
        }
        if (sun.getType() == SunType.SPECIAL) {
            return new String[]{"red", "animation"};
        }
        return new String[]{"animation"};
    }

    static String sunClip(Sun sun) {
        String[] preferred = preferredClips(sun);
        return preferred[0];
    }

    private void drawPlantFood(Batch batch, GameModel model, float delta) {
        List<PlantFoodPickup> tokens = model.getActivePlantFood();
        if (tokens != null && !tokens.isEmpty()) {
            for (PlantFoodPickup food : tokens) {
                writePlantFoodDrawPos(food, xyTmp);
                drawPlantFoodToken(batch, food, xyTmp[0], xyTmp[1], delta, "idle");
            }
        }
        drawPlantFoodFlights(batch, delta);
    }

    public PlantFoodPickup pickPlantFood(GameModel model, float worldX, float worldY) {
        if (model == null || model.getActivePlantFood() == null) {
            return null;
        }
        PlantFoodPickup best = null;
        float bestD = 0f;
        for (PlantFoodPickup food : model.getActivePlantFood()) {
            writePlantFoodDrawPos(food, xyTmp);
            if (!SunCollect.hits(xyTmp[0], xyTmp[1], worldX, worldY)) {
                continue;
            }
            float dx = worldX - xyTmp[0];
            float dy = worldY - xyTmp[1];
            float d = dx * dx + dy * dy;
            if (best == null || d < bestD) {
                best = food;
                bestD = d;
            }
        }
        return best;
    }

    public void writePlantFoodDrawPos(PlantFoodPickup food, float[] out) {
        float[] xy = layout.centerOf(food.getY(), food.getX());
        out[0] = xy[0] + food.getOffsetX() * layout.cellWidth();
        out[1] = xy[1] + food.getOffsetY() * layout.cellHeight();
    }

    /**
     * Kicks off the collect-flight animation: the orb lerps from
     * {@code (x0, y0)} to {@code (x1, y1)} (the HUD logo) and then plays a
     * grave-style squash/stretch vanish at the destination. The pickup has
     * already been removed from the model by the caller, so the flight is
     * purely cosmetic.
     */
    public void startPlantFoodCollect(PlantFoodPickup food, float x0, float y0, float x1, float y1) {
        if (food == null) {
            return;
        }
        plantFoodFlights.add(new PlantFoodFlight(food, x0, y0, x1, y1));
    }

    private void drawPlantFoodFlights(Batch batch, float delta) {
        for (int i = plantFoodFlights.size() - 1; i >= 0; i--) {
            PlantFoodFlight flight = plantFoodFlights.get(i);
            flight.elapsed += delta;
            if (PlantFoodCollect.done(flight.elapsed)) {
                plantFoodFlights.remove(i);
                continue;
            }
            float x = flight.x1;
            float y = flight.y1;
            float sx = 1f;
            float sy = 1f;
            if (PlantFoodCollect.flying(flight.elapsed)) {
                float u = PlantFoodCollect.flyU(flight.elapsed);
                x = flight.x0 + (flight.x1 - flight.x0) * u;
                y = flight.y0 + (flight.y1 - flight.y0) * u;
            } else {
                float u = PlantFoodCollect.vanishU(flight.elapsed);
                sx = GraveAnim.scaleX(u);
                sy = GraveAnim.scaleY(u);
            }
            drawPlantFoodToken(batch, flight.food, x, y, delta, sx, sy, "idle");
        }
    }

    private void drawGlowingPlantFoodOverlay(Batch batch, ZombieInstance zombie,
                                             float x, float y, float delta) {
        drawPlantFoodToken(batch, zombie, x, y, delta, "animation2", "animation");
    }

    private void drawPlantFoodToken(Batch batch, Object clockKey, float x, float y, float delta,
                                    String... clipPrefs) {
        drawPlantFoodToken(batch, clockKey, x, y, delta, 1f, 1f, clipPrefs);
    }

    /**
     * Scale-aware variant used by {@link #drawPlantFoodFlights} so the orb can
     * squash/stretch into the bank logo the same way a collected sun does.
     */
    private void drawPlantFoodToken(Batch batch, Object clockKey, float x, float y, float delta,
                                    float sxN, float syN, String... clipPrefs) {
        if (catalog == null || clipPrefs == null || clipPrefs.length == 0) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(PLANTFOOD_PICKUP_PAM);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, clipPrefs);
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        if (sxN == 1f && syN == 1f) {
            drawPose(batch, clockKey, pose, x, y, AnimScale.SUN, NO_PHASE, 0f, delta);
            return;
        }
        seenThisFrame.add(clockKey);
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return;
        }
        float stateTime = advanceClock(clockKey, pose.cacheKey(), delta);
        player.draw(batch, ref, stateTime, x, y,
            AnimScale.SUN * sxN, AnimScale.SUN * syN, pose.loop());
    }

    private void drawLoot(Batch batch, GameModel model, float delta) {
        List<LootPickup> tokens = model == null ? null : model.getActiveLootPickups();
        if (tokens != null) {
            for (LootPickup loot : tokens) {
                writeLootDrawPos(loot, xyTmp);
                drawLootToken(batch, loot, xyTmp[0], xyTmp[1], delta, 1f, 1f);
            }
        }
        drawLootFlights(batch, delta);
    }

    public void writeLootDrawPos(LootPickup loot, float[] out) {
        float[] xy = layout.centerOf(loot.getY(), loot.getX());
        out[0] = xy[0] + loot.getOffsetX() * layout.cellWidth();
        out[1] = xy[1] + loot.getOffsetY() * layout.cellHeight();
    }

    public void startLootCollect(LootPickup loot, float x0, float y0, float x1, float y1,
                                 Runnable onComplete) {
        if (loot == null) {
            return;
        }
        lootFlights.add(new LootFlight(loot, x0, y0, x1, y1, onComplete));
    }

    public void drainPendingLootFlights() {
        for (int i = lootFlights.size() - 1; i >= 0; i--) {
            LootFlight flight = lootFlights.get(i);
            if (!flight.done && flight.onComplete != null) {
                flight.onComplete.run();
                flight.done = true;
            }
        }
        lootFlights.clear();
    }

    private void drawLootFlights(Batch batch, float delta) {
        for (int i = lootFlights.size() - 1; i >= 0; i--) {
            LootFlight flight = lootFlights.get(i);
            flight.elapsed += delta;
            if (LootCollect.done(flight.elapsed)) {
                if (!flight.done && flight.onComplete != null) {
                    flight.onComplete.run();
                    flight.done = true;
                }
                lootFlights.remove(i);
                continue;
            }
            float x = flight.x1;
            float y = flight.y1;
            float sx = 1f;
            float sy = 1f;
            if (LootCollect.flying(flight.elapsed)) {
                float u = LootCollect.flyU(flight.elapsed);
                x = flight.x0 + (flight.x1 - flight.x0) * u;
                y = flight.y0 + (flight.y1 - flight.y0) * u;
            } else {
                float u = LootCollect.vanishU(flight.elapsed);
                sx = GraveAnim.scaleX(u);
                sy = GraveAnim.scaleY(u);
            }
            drawLootToken(batch, flight.loot, x, y, delta, sx, sy);
        }
    }

    private void drawLootToken(Batch batch, LootPickup loot, float x, float y, float delta) {
        drawLootToken(batch, loot, x, y, delta, 1f, 1f);
    }

    private void drawLootToken(Batch batch, LootPickup loot, float x, float y, float delta,
                               float sxN, float syN) {
        if (loot == null) {
            return;
        }
        if (loot.getKind() == LootPickupKind.FLOWER_POT) {
            drawFlowerPotToken(batch, loot, x, y, sxN, syN);
            return;
        }
        if (catalog == null) {
            return;
        }
        String pamName = switch (loot.getKind()) {
            case COIN_GOLD -> COIN_GOLD_PAM;
            case COIN_SILVER -> COIN_SILVER_PAM;
            case DIAMOND -> COIN_DIAMOND_PAM;
            case FLOWER_POT -> null;
        };
        String clip = loot.getKind() == LootPickupKind.DIAMOND ? "idle" : "animation";
        PamCatalog.PamEntry entry = pamName == null ? null : catalog.byName(pamName);
        if (entry == null) {
            return;
        }
        String resolved = catalog.resolveClip(entry, clip);
        AnimPose pose = AnimPose.looping(entry.path(), resolved, ZombieAnimRole.IDLE);
        float scale = loot.getKind() == LootPickupKind.DIAMOND
            ? AnimScale.LOOT_GEM
            : AnimScale.LOOT_COIN;
        if (sxN == 1f && syN == 1f) {
            drawPose(batch, loot, pose, x, y, scale, NO_PHASE, 0f, delta);
            return;
        }
        seenThisFrame.add(loot);
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return;
        }
        float stateTime = advanceClock(loot, pose.cacheKey(), delta);
        player.draw(batch, ref, stateTime, x, y,
            scale * sxN, scale * syN, pose.loop());
    }

    private void drawFlowerPotToken(Batch batch, LootPickup loot, float x, float y,
                                    float sxN, float syN) {
        if (textures == null) {
            return;
        }
        if (flowerPotRegion == null) {
            flowerPotRegion = textures.region(FLOWER_POT_REGION);
        }
        if (flowerPotRegion == null) {
            return;
        }
        seenThisFrame.add(loot);
        float h = FLOWER_POT_DRAW_H * syN;
        float w = flowerPotRegion.getRegionHeight() <= 0
            ? h
            : h * (flowerPotRegion.getRegionWidth() / (float) flowerPotRegion.getRegionHeight());
        w *= sxN;
        batch.draw(flowerPotRegion, x - w * 0.5f, y - h * 0.5f, w, h);
    }

    private void drawPushable(Batch batch, GameModel model, Pushable item, float delta) {
        if (item instanceof Piano) {
            drawPiano(batch, item, delta);
        } else if (item instanceof Barrel) {
            drawBarrel(batch, item, delta);
        } else if (item instanceof IceBlock) {
            drawIceBlock(batch, model, item, delta);
        } else {
            drawCabinet(batch, item, delta);
        }
    }

    private static void collectLiveCabinet(Pushable item, Set<Pushable> live) {
        if (item instanceof ArcadeMachine cabinet
            && !cabinet.isDestroyed()
            && cabinet.getPosition() != null) {
            live.add(cabinet);
            return;
        }
        if (item instanceof Piano piano
            && piano.getPosition() != null
            && piano.getPusher() != null
            && !piano.getPusher().isDead()) {
            live.add(piano);
            return;
        }
        if (item instanceof Barrel barrel
            && !barrel.isDestroyed()
            && barrel.getPosition() != null) {
            live.add(barrel);
            return;
        }
        if (item instanceof IceBlock ice
            && !ice.isDestroyed()
            && ice.getPosition() != null) {
            live.add(ice);
        }
    }

    private void drawCabinet(Batch batch, Pushable cabinet, float delta) {
        Point pos = cabinet.getPosition();
        if (pos == null || catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(ARCADE_CABINET_PAM);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, "idle");
        String damage = cabinetDamagePart(cabinet);
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE, damage);
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        float x = xy[0] + arcadeArmPushDeltaX(cabinet);
        float time = drawPose(batch, cabinet, pose, x, xy[1], AnimScale.PLANT, NO_PHASE,
            tickHitFlash(cabinet, itemHp(cabinet), delta), delta);
        lastCabinets.put(cabinet, new LiveSnap(pose, x, xy[1], false, time));
    }

    /** Pushed ice cube: same arm-follow as the arcade cabinet, ice PAM on the grid cell. */
    private void drawIceBlock(Batch batch, GameModel model, Pushable ice, float delta) {
        Point pos = ice.getPosition();
        if (pos == null || catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(TroglobiteAnim.ICE_PAM);
        if (entry == null) {
            return;
        }
        clips.getOrLoad(entry.path(), "idle");
        preloadIceBreak();
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        float x = xy[0] + arcadeArmPushDeltaX(ice);
        if (ice instanceof IceBlock block
            && block.getContainedEntity() instanceof ZombieInstance occupant) {
            drawIcedZombieIdle(batch, occupant, model, x, xy[1], delta);
        }
        String clip = catalog.resolveClip(entry, "idle");
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        float time = drawPose(batch, ice, pose, x, xy[1], AnimScale.ZOMBIE, NO_PHASE,
            tickHitFlash(ice, itemHp(ice), delta), delta);
        lastCabinets.put(ice, new LiveSnap(pose, x, xy[1], false, time));
    }

    /**
     * Frostbite ice tiles: occupant {@code idle} behind {@link TroglobiteAnim#ICE_PAM}.
     */
    private Set<Cell> syncTerrainIce(GameModel model) {
        GameMap map = model.getMap();
        Set<Cell> live = new HashSet<>();
        if (map == null || catalog == null) {
            return live;
        }
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                Cell cell = model.getCellAt(row, col);
                if (!isLiveTerrainIce(cell)) {
                    continue;
                }
                live.add(cell);
            }
        }
        for (Cell cell : lastTerrainIce.keySet()) {
            if (!live.contains(cell)) {
                spawnIceShatter(lastTerrainIce.get(cell));
            }
        }
        lastTerrainIce.entrySet().removeIf(e -> !live.contains(e.getKey()));
        return live;
    }

    private void drawTerrainIce(Batch batch, GameModel model, Set<Cell> live, float delta, int row) {
        for (Cell cell : live) {
            if (cell.getRow() == row) {
                drawTerrainIceCell(batch, model, cell, delta);
            }
        }
    }

    private static boolean isLiveTerrainIce(Cell cell) {
        if (cell == null || cell.getGroundType() != GroundType.ICE) {
            return false;
        }
        if (!(cell.getTerrainStrategy() instanceof IceTerrainStrategy ice)) {
            return false;
        }
        return !ice.isMelted();
    }

    /**
     * Frostbite slide tiles: every cell with a {@link SlideTerrainStrategy}
     * keeps a visual state; stale entries (level teardown) are dropped.
     */
    private Set<Cell> syncSlideTiles(GameModel model) {
        GameMap map = model.getMap();
        Set<Cell> live = new HashSet<>();
        if (map == null) {
            slideTiles.clear();
            return live;
        }
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                Cell cell = model.getCellAt(row, col);
                if (cell != null
                        && cell.getTerrainStrategy() instanceof SlideTerrainStrategy) {
                    live.add(cell);
                    slideTiles.computeIfAbsent(cell, key -> beginSlideTile(key));
                }
            }
        }
        slideTiles.keySet().removeIf(cell -> !live.contains(cell));
        return live;
    }

    /** First sighting of a model slide tile: preload art + resolve scale. */
    private SlideTileFx beginSlideTile(Cell cell) {
        if (!slideTilePamReady) {
            slideTilePamReady = true;
            clips.preloadSync(SlideTileAnim.DOWN_PAM_PATH,
                    SlideTileAnim.IDLE_CLIP, SlideTileAnim.START_CLIP, SlideTileAnim.END_CLIP);
            clips.preloadSync(SlideTileAnim.UP_PAM_PATH,
                    SlideTileAnim.IDLE_CLIP, SlideTileAnim.START_CLIP, SlideTileAnim.END_CLIP);
        }
        SlideTileFx fx = new SlideTileFx();
        String pam = pamFor(cell);
        // The slider PAM's origin is its middle: fit the art over one tile so
        // that origin lands exactly on the tile centre when drawn there.
        Rectangle bounds = player.bounds(pam, SlideTileAnim.IDLE_CLIP);
        fx.scale = bounds != null && bounds.width > 0f && bounds.height > 0f
                ? Math.max(layout.cellWidth() / bounds.width,
                        layout.cellHeight() / bounds.height)
                : AnimScale.LAWN;
        return fx;
    }

    /** Consumes model slide cues and kicks each hit tile's active burst. */
    private void harvestSlideStarts(GameModel model) {
        for (Point cue : model.drainSlideStarts()) {
            Cell cell = model.getCellAt(cue.getY(), cue.getX());
            SlideTileFx fx = cell != null ? slideTiles.get(cell) : null;
            if (fx == null) {
                continue;
            }
            fx.phase = SlideTileFx.Phase.ACTIVE_START;
            fx.clock = 0f;
        }
    }

    /**
     * Mirrors the model's in-flight lane glides so {@link #zombieWorldCenter}
     * can drift a sliding zombie between lanes instead of snapping it.
     */
    private void updateLaneGlides(GameModel model) {
        if (model.getLaneSlides().isEmpty()) {
            laneGlides.clear();
            return;
        }
        laneGlides.clear();
        for (LaneSlide glide : model.getLaneSlides()) {
            laneGlides.put(glide.getZombie(), glide);
        }
        Set<ZombieInstance> live = new HashSet<>(model.getZombies());
        laneGlides.keySet().removeIf(zombie -> !live.contains(zombie));
    }

    /** Mirrors the model's surfacing ambush zombies for the water mask. */
    private void updateWaterEmerges(GameModel model) {
        if (model.getWaterEmerges().isEmpty()) {
            waterEmerges.clear();
            return;
        }
        waterEmerges.clear();
        for (WaterEmerge emerge : model.getWaterEmerges()) {
            waterEmerges.put(emerge.getZombie(), emerge);
        }
        Set<ZombieInstance> live = new HashSet<>(model.getZombies());
        waterEmerges.keySet().removeIf(zombie -> !live.contains(zombie));
    }

    private void drawSlideTiles(Batch batch, Set<Cell> live, float delta, int row) {
        for (Cell cell : live) {
            if (cell.getRow() != row) {
                continue;
            }
            SlideTileFx fx = slideTiles.get(cell);
            if (fx == null || fx.scale <= 0f) {
                continue;
            }
            String pam = pamFor(cell);
            String clip = switch (fx.phase) {
                case IDLE -> SlideTileAnim.IDLE_CLIP;
                case ACTIVE_START -> SlideTileAnim.START_CLIP;
                case ACTIVE_END -> SlideTileAnim.END_CLIP;
            };
            ClipRef ref = clips.getOrLoad(pam, clip);
            if (ref == null) {
                continue;
            }
            // The PAM's (0,0) axis sits on its middle, so drawing at the tile
            // centre puts exactly that point on the middle of the tile.
            float[] xy = layout.centerOf(cell.getRow(), cell.getColumn());
            player.draw(batch, ref, fx.clock, xy[0], xy[1], fx.scale, fx.scale,
                    fx.phase == SlideTileFx.Phase.IDLE);
            fx.clock += delta;
            if (fx.phase != SlideTileFx.Phase.IDLE) {
                float duration = player.clipDurationSeconds(pam, clip);
                if (duration > 0f && fx.clock >= duration) {
                    fx.phase = fx.phase == SlideTileFx.Phase.ACTIVE_START
                            ? SlideTileFx.Phase.ACTIVE_END
                            : SlideTileFx.Phase.IDLE;
                    fx.clock = 0f;
                }
            }
        }
    }

    private static String pamFor(Cell cell) {
        SlideTerrainStrategy slide = (SlideTerrainStrategy) cell.getTerrainStrategy();
        return slide.getSlideDirection() == SlideDirection.UP
                ? SlideTileAnim.UP_PAM_PATH
                : SlideTileAnim.DOWN_PAM_PATH;
    }

    private void drawTerrainIceCell(Batch batch, GameModel model, Cell cell, float delta) {
        IceTerrainStrategy ice = (IceTerrainStrategy) cell.getTerrainStrategy();
        float[] xy = layout.centerOf(cell.getRow(), cell.getColumn());
        Placeable occupant = ice.getContainedEntity();
        if (occupant instanceof ZombieInstance zombie) {
            drawIcedZombieIdle(batch, zombie, model, xy[0], xy[1], delta);
        }
        PamCatalog.PamEntry entry = catalog.byName(TroglobiteAnim.ICE_PAM);
        if (entry == null) {
            return;
        }
        preloadIceBreak();
        String clip = catalog.resolveClip(entry, "idle");
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        float time = drawPose(batch, cell, pose, xy[0], xy[1], AnimScale.ZOMBIE, NO_PHASE,
            tickHitFlash(cell, ice.getHp(), delta), delta);
        lastTerrainIce.put(cell, new LiveSnap(pose, xy[0], xy[1], false, time));
    }

    private void drawIcedZombieIdle(Batch batch, ZombieInstance zombie, GameModel model,
                                    float x, float y, float delta) {
        if (zombie == null || zombie.getDefinition() == null) {
            return;
        }
        Chapter skin = artChapterFor(zombie, model.getChapter());
        PamCatalog.PamEntry entry = catalog.forZombie(zombie.getDefinition().getName(), skin);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, "idle", "walk");
        if (clip == null) {
            return;
        }
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE,
            ZombieAnimAdapter.armorVisibility(zombie, entry));
        if (ZombotanyAnim.isPlantHead(zombie)) {
            pose = pose.flipped();
        }
        drawPose(batch, zombie, pose, x, y, AnimScale.ZOMBIE, NO_PHASE, 0f, 0f, pose.cacheKey(), 0f, 1.0f);
    }

    private static void collectIcedOccupants(GameModel model, Set<ZombieInstance> into) {
        GameMap map = model.getMap();
        if (map == null) {
            return;
        }
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                Cell cell = model.getCellAt(row, col);
                if (!(cell != null && cell.getTerrainStrategy() instanceof IceTerrainStrategy ice)) {
                    continue;
                }
                if (ice.getContainedEntity() instanceof ZombieInstance zombie) {
                    into.add(zombie);
                }
            }
        }
        for (ZombieInstance walker : model.getZombies()) {
            addIceOccupant(walker.getPushableItem(), into);
        }
        if (model.getOrphanedPushables() != null) {
            for (Pushable orphan : model.getOrphanedPushables()) {
                addIceOccupant(orphan, into);
            }
        }
    }

    private static void addIceOccupant(Pushable item, Set<ZombieInstance> into) {
        if (item instanceof IceBlock block
            && block.getContainedEntity() instanceof ZombieInstance zombie) {
            into.add(zombie);
        }
    }

    /** Piano rides the pianist’s cell centre — no extra offset. */
    private void drawPiano(Batch batch, Pushable piano, float delta) {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(PIANO_PAM);
        if (entry == null) {
            return;
        }
        clips.getOrLoad(entry.path(), "die");
        clips.getOrLoad(entry.path(), "particles");
        String clip = catalog.resolveClip(entry, "play");
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        float x;
        float y;
        ZombieInstance pusher = piano.getPusher();
        if (pusher != null && zombieWorldCenter(pusher, xyTmp)) {
            x = xyTmp[0];
            y = xyTmp[1];
        } else {
            Point pos = piano.getPosition();
            if (pos == null) {
                return;
            }
            float[] xy = layout.centerOf(pos.getY(), pos.getX());
            x = xy[0];
            y = xy[1];
        }
        float time = drawPose(batch, piano, pose, x, y, AnimScale.ZOMBIE, NO_PHASE,
            tickHitFlash(piano, itemHp(piano), delta), delta);
        lastCabinets.put(piano, new LiveSnap(pose, x, y, false, time));
    }

    /**
     * Barrel art lives in the pusher's walk/eat. After he dies, freeze those
     * barrel parts at the last {@code partBounds} pose. Separate barrel PAM is
     * only the break clip.
     */
    private void drawBarrel(Batch batch, Pushable barrel, float delta) {
        Point pos = barrel.getPosition();
        if (pos == null || catalog == null) {
            return;
        }
        ZombieInstance pusher = barrel.getPusher();
        if (pusher != null && !pusher.isDead()) {
            rememberLiveBarrel(barrel, pusher);
            return;
        }
        LiveSnap leftover = lastCabinets.get(barrel);
        if (leftover != null && leftover.pose != null
            && BarrelRollerAnim.isPusherPam(leftover.pose.pamPath())) {
            seenThisFrame.add(barrel);
            drawBarrelParts(batch, leftover, tickHitFlash(barrel, itemHp(barrel), delta));
            lastCabinets.put(barrel, leftover);
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(BarrelRollerAnim.BARREL_PAM);
        if (entry == null) {
            return;
        }
        clips.getOrLoad(entry.path(), "die");
        String clip = catalog.resolveClip(entry, "roll");
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        float time = drawPose(batch, barrel, pose, xy[0], xy[1], AnimScale.ZOMBIE, NO_PHASE,
            tickHitFlash(barrel, itemHp(barrel), delta), delta);
        lastCabinets.put(barrel, new LiveSnap(pose, xy[0], xy[1], false, time));
    }

    /** Cache break-FX origin at the barrel parts, not the grid cell. */
    private void rememberLiveBarrel(Pushable barrel, ZombieInstance pusher) {
        PamCatalog.PamEntry entry = catalog.byName(BarrelRollerAnim.BARREL_PAM);
        if (entry != null) {
            clips.getOrLoad(entry.path(), "die");
        }
        LiveSnap body = lastLive.get(pusher);
        if (body != null && body.pose != null) {
            lastCabinets.put(barrel, new LiveSnap(body.pose, body.x, body.y, body.backward, body.time));
            return;
        }
        Point pos = barrel.getPosition();
        if (pos == null) {
            return;
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        AnimPose pose = entry == null
            ? null
            : AnimPose.looping(entry.path(), "roll", ZombieAnimRole.IDLE);
        if (pose != null) {
            lastCabinets.put(barrel, new LiveSnap(pose, xy[0], xy[1], false, 0f));
        }
    }

    private static float leftoverHoldPhase(LiveSnap leftover, ClipRef ref) {
        if (ref == null || ref.duration <= 0f) {
            return 0f;
        }
        float phase = leftover.time / ref.duration;
        if (leftover.pose != null && leftover.pose.loop()) {
            phase -= (float) Math.floor(phase);
            if (phase < 0f) {
                phase = 0f;
            }
        } else if (phase > 1f) {
            phase = 1f;
        }
        return phase;
    }

    /** Whitelist barrel PAM parts so the pusher body never draws on the leftover. */
    private void drawBarrelParts(Batch batch, LiveSnap leftover, float flash) {
        if (leftover == null || leftover.pose == null) {
            return;
        }
        paintBarrelParts(batch, leftover);
        overlayHitFlash(batch, flash, () -> paintBarrelParts(batch, leftover));
    }

    private void paintBarrelParts(Batch batch, LiveSnap leftover) {
        if (leftover == null || leftover.pose == null) {
            return;
        }
        ClipRef ref = clips.getOrLoad(leftover.pose.pamPath(), leftover.pose.clipName());
        float t = leftover.time;
        if (ref != null && ref.duration > 0f) {
            t = leftoverHoldPhase(leftover, ref) * ref.duration;
        }
        float s = AnimScale.ZOMBIE * leftover.pose.scale();
        float sx = leftover.pose.flipX() ? -s : s;
        batchTransform.set(batch.getTransformMatrix());
        popTransform.set(batchTransform)
            .translate(leftover.x, leftover.y, 0f)
            .scale(sx, s, 1f)
            .translate(-leftover.x, -leftover.y, 0f);
        batch.setTransformMatrix(popTransform);
        for (String part : BarrelRollerAnim.BARREL_PARTS) {
            player.drawPart(batch, leftover.pose.pamPath(), leftover.pose.clipName(),
                t, leftover.x, leftover.y, part);
        }
        batch.setTransformMatrix(batchTransform);
    }

    /** HP → {@code arcade_cabinet_damage0} (pristine) … {@code damage5} (almost gone). */
    private static String cabinetDamagePart(Pushable cabinet) {
        if (!(cabinet instanceof ArcadeMachine machine)) {
            return "arcade_cabinet_damage0";
        }
        int hp = machine.getHp();
        int max = machine.getMaxHp();
        int idx = 0;
        if (max > 0 && hp < max) {
            idx = Math.min(5, (max - hp) * 6 / max);
        }
        return "arcade_cabinet_damage" + idx;
    }

    /**
     * World-X delta so the cabinet follows the pushing hand by the same amount
     * the hand actually travels — not a full extra tile.
     */
    private float arcadeArmPushDeltaX(Pushable cabinet) {
        ZombieInstance pusher = cabinet.getPusher();
        if (pusher == null || pusher.getDefinition() == null) {
            return 0f;
        }
        PushBehavior push = (PushBehavior) pusher.getBehavior(ZombieBehaviorType.PUSH);
        if (push == null || !push.isPushing()) {
            return 0f;
        }
        PamCatalog.PamEntry entry = catalog.forZombie(pusher.getDefinition().getName(), null);
        if (entry == null) {
            return 0f;
        }
        ClipRef clip = clips.getOrLoad(entry.path(), "push");
        if (clip == null || clip.duration <= 0f) {
            return 0f;
        }
        float[] xs = arcadePushHandCurve(clip);
        if (xs == null) {
            return 0f;
        }
        float t = Math.min(push.getPushTimer(), clip.duration);
        return (sampleCurve(xs, t / clip.duration) - xs[0]) * AnimScale.ZOMBIE;
    }

    /** Canvas-local left-edge X of {@link #ARCADE_HAND_PART}, one value per clip frame. */
    private float[] arcadePushHandCurve(ClipRef clip) {
        float[] cached = arcadePushHandX.get(clip);
        if (cached != null) {
            return cached.length == 0 ? null : cached;
        }
        float[] xs = leftEdgeCurve(clip, ARCADE_HAND_PART);
        arcadePushHandX.put(clip, xs == null ? new float[0] : xs);
        return xs;
    }

    private float[] leftEdgeCurve(ClipRef clip, String part) {
        Rectangle[] frames = player.partBoundsByFrame(clip, part);
        if (frames == null || frames.length < 2) {
            return null;
        }
        float[] xs = new float[frames.length];
        float last = Float.NaN;
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] != null) {
                last = frames[i].x;
            }
            xs[i] = last;
        }
        if (Float.isNaN(last)) {
            return null;
        }
        float first = last;
        for (int i = 0; i < xs.length; i++) {
            if (!Float.isNaN(xs[i])) {
                first = xs[i];
                break;
            }
        }
        for (int i = 0; i < xs.length; i++) {
            if (Float.isNaN(xs[i])) {
                xs[i] = first;
            }
        }
        return xs;
    }

    private static float sampleCurve(float[] xs, float phase) {
        if (xs.length == 1) {
            return xs[0];
        }
        float p = phase < 0f ? 0f : Math.min(phase, 1f);
        float at = p * (xs.length - 1);
        int i = (int) at;
        if (i >= xs.length - 1) {
            return xs[xs.length - 1];
        }
        return xs[i] + (xs[i + 1] - xs[i]) * (at - i);
    }

    private void spawnCabinetDeath(LiveSnap snap) {
        if (snap == null || snap.pose == null) {
            return;
        }
        String pam = snap.pose.pamPath();
        if (isPianoProp(pam)) {
            spawnPianoDeath(snap, pam);
            return;
        }
        if (BarrelRollerAnim.isBarrelPropPam(pam) || BarrelRollerAnim.isPusherPam(pam)) {
            spawnBarrelBreak(snap);
            return;
        }
        if (TroglobiteAnim.isIcePropPam(pam)) {
            spawnIceShatter(snap);
            return;
        }
        String clip = firstLoadedClip(pam, "death", snap.pose.clipName());
        addFlashingDeath(AnimPose.once(pam, clip, ZombieAnimRole.DIE, snap.pose.visibility()),
            snap.x, snap.y);
    }

    private void preloadIceBreak() {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(TroglobiteAnim.ICE_BREAK_PAM);
        if (entry != null) {
            clips.getOrLoad(entry.path(), catalog.resolveClip(entry, "animation"));
        }
    }

    private void drawPlantChill(Batch batch, PlantInstance plant,
                                float x, float y, float flash, float delta) {
        int hits = plant.getFreezeHitCount();
        boolean show = !plant.isFrozen() && hits > 0 && !plant.hasOctopusCoating();
        if (!show) {
            plantChillClocks.remove(plant);
            return;
        }
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(PlantFreezeAnim.CHILL_PAM);
        if (entry == null) {
            return;
        }
        String preferredClip = hits == 1 ? PlantFreezeAnim.CHILL_STAGE1_CLIP : PlantFreezeAnim.CHILL_STAGE2_CLIP;
        String clip = catalog.resolveClip(entry, preferredClip);
        AnimPose pose = AnimPose.looping(entry.path(), clip, PlantAnimRole.IDLE);
        Object chillClock = plantChillClocks.computeIfAbsent(plant, p -> new Object());
        String clockKey = pose.cacheKey() + "#plant-chill-stage" + hits;
        drawPose(batch, chillClock, pose, x, y, AnimScale.PLANT, NO_PHASE,
                flash, delta, clockKey);
    }

    private void drawZombieFreezeChill(Batch batch, ZombieInstance zombie,
                                       float x, float y, float flash, float delta) {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(PlantFreezeAnim.CHILL_PAM);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, PlantFreezeAnim.CHILL_STAGE2_CLIP);
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        Object chillClock = zombieChillClocks.computeIfAbsent(zombie, z -> new Object());
        String clockKey = pose.cacheKey() + "#zombie-frozen-chill-stage2";
        drawPose(batch, chillClock, pose, x, y, AnimScale.ZOMBIE, NO_PHASE,
                flash, delta, clockKey);
    }

    private void drawPlantFreezeIce(Batch batch, PlantInstance plant,
                                    float x, float y, float flash, float delta) {
        boolean show = plant.isFrozen() && !plant.hasOctopusCoating();
        if (!show) {
            plantIceIntro.remove(plant);
            plantIceClocks.remove(plant);
            LiveSnap prev = lastPlantIce.remove(plant);
            if (prev != null) {
                spawnIceShatter(prev);
            }
            return;
        }
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(PlantFreezeAnim.ICE_PAM);
        if (entry == null) {
            return;
        }
        preloadIceBreak();
        float startDur = plantIceStartDuration(entry.path());
        Float intro = plantIceIntro.get(plant);
        boolean playStart = intro == null || intro < startDur;
        AnimPose pose;
        String clockKey;
        if (playStart) {
            String clip = catalog.resolveClip(entry, PlantFreezeAnim.START_CLIP);
            pose = AnimPose.once(entry.path(), clip, PlantAnimRole.SPECIAL, null);
            clockKey = pose.cacheKey() + "#plant-ice-start";
        } else {
            String clip = catalog.resolveClip(entry, PlantFreezeAnim.IDLE_CLIP);
            pose = AnimPose.looping(entry.path(), clip, PlantAnimRole.IDLE);
            clockKey = pose.cacheKey() + "#plant-ice-idle";
        }
        Object iceClock = plantIceClocks.computeIfAbsent(plant, p -> new Object());
        float time = drawPose(batch, iceClock, pose, x, y, AnimScale.PLANT, NO_PHASE,
                flash, delta, clockKey);
        if (playStart) {
            plantIceIntro.put(plant, Math.min(time, startDur));
        }
        lastPlantIce.put(plant, new LiveSnap(pose, x, y, false, time));
    }

    private float plantIceStartDuration(String path) {
        ClipRef start = clips.getOrLoad(path, PlantFreezeAnim.START_CLIP);
        return start == null ? 0.9f : start.duration;
    }

    private void spawnIceShatter(LiveSnap snap) {
        if (snap == null) {
            return;
        }
        preloadIceBreak();
        PamCatalog.PamEntry entry = catalog == null ? null : catalog.byName(TroglobiteAnim.ICE_BREAK_PAM);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, "animation");
        addFlashingDeath(AnimPose.once(entry.path(), clip, ZombieAnimRole.DIE, null),
            snap.x, snap.y);
    }

    private void spawnBarrelBreak(LiveSnap snap) {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(BarrelRollerAnim.BARREL_PAM);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, "die");
        float x = snap.x;
        float y = snap.y;
        if (snap.pose != null && BarrelRollerAnim.isPusherPam(snap.pose.pamPath())) {
            float[] xy = barrelWorldCenter(snap);
            x = xy[0];
            y = xy[1];
        }
        addFlashingDeath(AnimPose.once(entry.path(), clip, ZombieAnimRole.DIE, null),
            x, y);
    }

    private void addFlashingDeath(AnimPose pose, float x, float y) {
        DeathFx fx = new DeathFx(pose, x, y);
        fx.hitFlash = HIT_FLASH_SEC;
        deathFx.add(fx);
    }

    private float[] barrelWorldCenter(LiveSnap snap) {
        Rectangle bounds = barrelPartBounds(snap.pose.pamPath(), snap.pose.clipName(), snap.time);
        if (bounds == null) {
            return new float[]{snap.x, snap.y};
        }
        float s = AnimScale.ZOMBIE * snap.pose.scale();
        float localX = bounds.x + bounds.width * 0.5f;
        float localY = bounds.y + bounds.height * 0.5f;
        if (snap.pose.flipX()) {
            localX = -localX;
        }
        return new float[]{snap.x + localX * s, snap.y - localY * s};
    }

    private Rectangle barrelPartBounds(String pam, String clip, float time) {
        if (pam == null || clip == null) {
            return null;
        }
        clips.getOrLoad(pam, clip);
        Rectangle union = null;
        for (String part : BarrelRollerAnim.BARREL_PARTS) {
            Rectangle r = player.partBounds(pam, clip, time, part);
            if (r == null) {
                continue;
            }
            if (union == null) {
                union = new Rectangle(r);
            } else {
                union.merge(r);
            }
        }
        if (union != null) {
            return union;
        }
        ClipRef ref = clips.getOrLoad(pam, clip);
        if (ref == null) {
            return null;
        }
        for (String part : BarrelRollerAnim.BARREL_PARTS) {
            Rectangle[] frames = player.partBoundsByFrame(ref, part);
            if (frames == null) {
                continue;
            }
            for (Rectangle r : frames) {
                if (r == null) {
                    continue;
                }
                if (union == null) {
                    union = new Rectangle(r);
                } else {
                    union.merge(r);
                }
                break;
            }
        }
        return union;
    }

    private void spawnPianoDeath(LiveSnap snap, String pam) {
        String dieClip = firstLoadedClip(pam, "die", snap.pose.clipName());
        Map<String, Boolean> vis = new HashMap<>();
        if (snap.pose.visibility() != null) {
            vis.putAll(snap.pose.visibility());
        }
        vis.put("_particles", Boolean.FALSE);
        for (String part : PIANO_PARTICLE_PARTS) {
            vis.put(part, Boolean.FALSE);
        }
        deathFx.add(new DeathFx(
            AnimPose.once(pam, dieClip, ZombieAnimRole.DIE, vis),
            snap.x, snap.y));
        ClipRef dieRef = clips.getOrLoad(pam, dieClip);
        float hold = dieRef != null ? dieRef.duration : 0f;
        String particleClip = firstLoadedClip(pam, "particles", null);
        if (particleClip == null) {
            return;
        }
        float dir = snap.backward ? -1f : 1f;
        for (int i = 0; i < PIANO_PARTICLE_PARTS.length; i++) {
            float back = 0.1f + i * 0.1f;
            float hop = 0.85f + (i % 2) * 0.3f;
            addLimbPop(pam, particleClip, PIANO_PARTICLE_PARTS[i],
                snap.x, snap.y, 0f, dir, back, hop, hold, false);
        }
    }

    private void drawZombie(Batch batch, ZombieInstance zombie, Chapter chapter, float delta) {
        AnimPose pose = zombieAdapter.poseFor(zombie, chapter);
        if (pose == null) {
            entityOverlay.drawZombie(batch, App.getInstance().getCurrentGameModel(), zombie);
            return;
        }
        boolean backward = zombie.isMovingBackward();
        if (backward) {
            pose = pose.withFlipX(true);
        }
        if (!zombieWorldCenter(zombie, xyTmp)) {
            entityOverlay.drawZombie(batch, App.getInstance().getCurrentGameModel(), zombie);
            return;
        }
        applyZombotanySquashLeap(zombie, chapter, xyTmp);

        if (!zombie.isFrozen()) {
            restartArcadePushClock(zombie, pose);
            restartProspectorJumpClock(zombie, pose);
            restartTombRaiseClock(zombie, pose);
            restartDodoFlyClock(zombie, pose);
            restartHunterThrowClock(zombie, pose);
            restartJugglerSpinClock(zombie, pose);
            restartOctopusTossClock(zombie, pose);
            restartFishermanClock(zombie, pose);
            restartDarkKingClock(zombie, pose);
            restartDarkZombossClock(zombie, pose);
            restartWizardSheepClock(zombie, pose);
            spawnHunterSplat(zombie);
        }

        float x = xyTmp[0];
        float y = xyTmp[1];
        if (SunshineAnim.isSunshine(zombie)) {
            y += SunshineAnim.drawOffsetY(layout.cellHeight());
        }
        if (DarkZombossAnim.isDarkZomboss(zombie)) {
            y -= layout.cellHeight();
        }
        float modelX = x;
        ThrowImpBehavior.Flight flight = ThrowImpBehavior.flightOf(zombie);
        if (flight != null) {
            alignToss(zombie, flight, pose, x, y);
            float t = flight.progress();
            float[] align = tossAlign.get(zombie);
            if (align != null) {
                x += align[0] * (1f - t);
                y += align[1] * (1f - t);
            }
            y += flight.heightTiles() * layout.cellHeight();
        }
        JumpBehavior jump = (JumpBehavior) zombie.getBehavior(ZombieBehaviorType.JUMP);
        if (jump != null) {
            y += jump.heightPx();
            if (jump.getPhase() == JumpBehavior.JumpPhase.COUNTDOWN) {
                clips.getOrLoad(pose.pamPath(), "blastoff");
                clips.getOrLoad(pose.pamPath(), "fly");
                clips.getOrLoad(pose.pamPath(), "land");
                preloadProspectorBlast();
            } else if (jump.getPhase() == JumpBehavior.JumpPhase.JUMPING) {
                spawnProspectorBlast(zombie, jump);
            }
        }
        float phase = NO_PHASE;
        ZombieGait gait = gaitFor(zombie);
        ClipRef ref = pose.isSpritesheet() ? null : clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref != null && jump != null && jump.getPhase() == JumpBehavior.JumpPhase.JUMPING
            && ref.duration > 0f) {
            // Fuse hit → blastoff this frame. Do not let the gait walk clock finish first.
            phase = prospectorJumpPhase(jump, pose.clipName(), ref.duration);
        } else if (ref != null && gait.enabled() && ZombieAnimAdapter.isDistanceDriven(zombie, pose)) {
            // Walking is driven by travel, so a cycle always covers exactly one step and
            // ground_swatch can be held still. Every other pose stays on the wall clock.
            float travel = backward ? xyTmp[2] : -xyTmp[2];
            phase = gait.phaseAt(travel);
            float holdBack = gait.footLockOffsetTiles(phase, footfallFor(gait, ref)) * layout.cellWidth();
            x += holdBack;
        } else {
            float zombossPhase = darkZombossClipPhase(zombie, pose, ref);
            if (zombossPhase >= 0f) {
                phase = zombossPhase;
            }
        }
        float standY = y;
        GameModel model = App.getInstance().getCurrentGameModel();
        Rectangle snorkelMask = null;
        float snorkelWaterY = Float.NaN;
        float rippleX = x;
        float baseScale = AnimScale.forZombie(pose);
        float scale = baseScale * pose.scale();
        SwimBehavior swim = SnorkelerAnim.isSnorkelerPam(pose.pamPath())
            ? (SwimBehavior) zombie.getBehavior(ZombieBehaviorType.SWIM)
            : null;
        if (swim != null && (swim.isSubmerged() || swim.isSurfaced()) && swim.getRise() < 1f - 1e-3f) {
            float measureT = phase >= 0f && ref != null ? phase * ref.duration : 0f;
            Rectangle skull = ref != null ? partAt(ref, measureT, SnorkelerAnim.SKULL_PART) : null;
            if (skull == null) {
                skull = partAt(clips.getOrLoad(pose.pamPath(), "walk"), 0f, SnorkelerAnim.SKULL_PART);
            }
            snorkelWaterY = SnorkelerAnim.waterLineY(layout, zombie.getGridY());
            y = SnorkelerAnim.drawOriginY(standY, snorkelWaterY, skull, scale, swim.getRise());
            snorkelMask = FishermanAnim.drownMaskWorld(layout, x, zombie.getGridY(), snorkelWaterY);
            rippleX = SnorkelerAnim.skullCenterWorldX(x, skull, scale, pose.flipX());
        } else {
            WaterEmerge emerge = waterEmerges.get(zombie);
            if (emerge != null && emerge.progress() < 1f - 1e-3f) {
                snorkelWaterY = SnorkelerAnim.waterLineY(layout, zombie.getGridY());
                float measureT = phase >= 0f && ref != null ? phase * ref.duration : 0f;
                Rectangle box = pose.isSpritesheet()
                        ? null
                        : player.bounds(pose.pamPath(), pose.clipName());
                float artTop = box != null
                        ? standY - box.y * scale
                        : standY + layout.cellHeight();
                float extraSink = FishermanAnim.emergeExtraSink(layout.cellHeight(), zombie);
                y = WaterEmerge.drawOriginY(
                        standY, snorkelWaterY, artTop, emerge.progress(), extraSink);
                snorkelMask = FishermanAnim.drownMaskWorld(
                        layout, modelX, zombie.getGridY(), snorkelWaterY,
                        FishermanAnim.emergeMaskWidthTiles(zombie),
                        FishermanAnim.EMERGE_MASK_BELOW_TILES);
                rippleX = modelX;
            } else if (shouldRippleOnWater(zombie, model, swim, jump)) {
                snorkelWaterY = SnorkelerAnim.waterLineY(layout, zombie.getGridY());
                rippleX = modelX;
            }
        }
        if (snorkelMask != null) {
            drownShader().begin(batch, snorkelMask);
        }
        maybePopLostHand(zombie, pose, x, y);
        if (lostHands.containsKey(zombie)) {
            pose = pose.withHiddenParts(lostArmBodyParts(pose.pamPath()));
        }
        float glow = zombie.isGlowing() && snorkelMask == null ? glowStrength() : 0f;
        float chill = (zombie.isChilled() || zombie.isFrozen()) && snorkelMask == null ? 1.0f : 0f;
        float danger;
        if (DangerRedShader.isZombieInDangerZone(zombie) && snorkelMask == null) {
            float elapsed = zombieDangerElapsed.getOrDefault(zombie, 0f) + delta;
            zombieDangerElapsed.put(zombie, elapsed);
            danger = DangerRedShader.dangerStrength(elapsed);
        } else {
            zombieDangerElapsed.remove(zombie);
            danger = 0f;
        }
        float animDelta = zombie.isFrozen() ? 0f : delta;
        float time = drawPose(batch, zombie, pose, x, y, baseScale, phase,
            tickHitFlash(zombie, delta), animDelta, pose.cacheKey(), glow, chill, danger);
        if (zombie.isFrozen()) {
            drawZombieFreezeChill(batch, zombie, x, y, tickHitFlash(zombie, delta), delta);
        }
        maybeSpawnZombotanyJalapenoFire(zombie);
        maybeGargantuarWalkStomp(zombie, pose, time);
        if (snorkelMask != null) {
            drownShader().end(batch);
        }
        if (!Float.isNaN(snorkelWaterY)) {
            drawSnorkelRipple(batch, pose, zombie, rippleX, snorkelWaterY);
        }
        if (zombie.isGlowing()) {
            drawGlowingPlantFoodOverlay(batch, zombie, x, y, delta);
        }
        popBrokenArmor(zombie, pose, x, y);
        lastLive.put(zombie, new LiveSnap(pose, x, y,
            zombie.isMovingBackward() || pose.flipX(), time));
        captureOctopusRelease(zombie, pose, x, y, time);
        maybeDrawCrystalSkullBeam(batch, pose, x, y, time);
        syncBarrelFront(zombie, pose, time);
        flashPushedBarrel(batch, zombie, delta);
    }

    /** Art-measured tiles from zombie origin to the barrel centre. */
    private void syncBarrelFront(ZombieInstance zombie, AnimPose pose, float time) {
        if (!(zombie.getPushableItem() instanceof Barrel) || pose == null) {
            return;
        }
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        if (push == null) {
            return;
        }
        Rectangle bounds = barrelPartBounds(pose.pamPath(), pose.clipName(), time);
        if (bounds == null) {
            return;
        }
        float localCenterX = bounds.x + bounds.width * 0.5f;
        float tiles = -localCenterX * AnimScale.ZOMBIE * pose.scale() / layout.cellWidth();
        push.setBarrelFrontOffsetTiles(tiles);
    }

    /** Barrel art rides the pusher PAM; additive-flash just those parts when the barrel is hit. */
    private void flashPushedBarrel(Batch batch, ZombieInstance zombie, float delta) {
        if (zombie.isDead()
            || !(zombie.getPushableItem() instanceof Barrel barrel)
            || barrel.isDestroyed()) {
            return;
        }
        seenThisFrame.add(barrel);
        float flash = tickHitFlash(barrel, itemHp(barrel), delta);
        LiveSnap snap = lastLive.get(zombie);
        if (snap == null) {
            return;
        }
        overlayHitFlash(batch, flash, () -> paintBarrelParts(batch, snap));
    }

    /** Kick the EFFECTS PAM load during {@code power_up} so {@code laser_beam} is ready at 0.63s. */
    private void preloadCrystalSkullBeam() {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry beam = catalog.byName(CRYSTALSKULL_BEAM_PAM);
        if (beam != null) {
            clips.getOrLoad(beam.path(), catalog.resolveClip(beam, "laser_beam"));
        }
    }

    private void preloadProspectorBlast() {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry blast = catalog.byName(PROSPECTOR_BLAST_PAM);
        if (blast == null) {
            return;
        }
        for (String clip : PROSPECTOR_BLAST_CLIPS) {
            clips.getOrLoad(blast.path(), catalog.resolveClip(blast, clip));
        }
    }

    /** Ground burst at the fuse tile; stays put while the body flies. */
    private void spawnProspectorBlast(ZombieInstance zombie, JumpBehavior jump) {
        if (zombie == null || jump == null || prospectorBlastSpawned.containsKey(zombie)) {
            return;
        }
        preloadProspectorBlast();
        PamCatalog.PamEntry blast = catalog == null ? null : catalog.byName(PROSPECTOR_BLAST_PAM);
        if (blast == null) {
            return;
        }
        float[] xy = layout.centerOf(zombie.getGridY(), jump.getLaunchX());
        prospectorBlasts.add(new BlastFx(blast.path(), xy[0], xy[1]));
        prospectorBlastSpawned.put(zombie, Boolean.TRUE);
        if (screenShake != null) {
            screenShake.pulse();
        }
    }

    private void drawProspectorBlasts(Batch batch, float delta, int row) {
        float scale = AnimScale.ZOMBIE;
        for (int i = prospectorBlasts.size() - 1; i >= 0; i--) {
            BlastFx fx = prospectorBlasts.get(i);
            if (layout.rowAt(fx.y) != row) {
                continue;
            }
            float maxDuration = 0f;
            boolean drew = false;
            for (String clip : PROSPECTOR_BLAST_CLIPS) {
                ClipRef ref = clips.getOrLoad(fx.pamPath, clip);
                if (ref == null) {
                    continue;
                }
                maxDuration = Math.max(maxDuration, ref.duration);
                if (fx.time < ref.duration) {
                    player.draw(batch, ref, fx.time, fx.x, fx.y, scale, scale, false);
                    drew = true;
                }
            }
            if (!drew && (maxDuration <= 0f || fx.time >= maxDuration)) {
                prospectorBlasts.remove(i);
                continue;
            }
            fx.time += delta;
        }
    }

    /**
     * {@code CRYSTALSKULL_BEAM} starts when {@code zombie_egypt_ra_staff_whiteglow} fires
     * at {@link StealSunBehavior#ATTACK_BEAM_AT} of {@code attack}. The beam's right edge
     * sits on the skull's left and follows that part each frame.
     */
    private void maybeDrawCrystalSkullBeam(Batch batch, AnimPose pose, float x, float y, float time) {
        if (pose == null || catalog == null) {
            return;
        }
        if ("power_up".equals(pose.clipName()) || "power".equals(pose.clipName())
            || "power_down".equals(pose.clipName())) {
            preloadCrystalSkullBeam();
            return;
        }
        if (!"attack".equals(pose.clipName()) || time < StealSunBehavior.ATTACK_BEAM_AT) {
            return;
        }
        PamCatalog.PamEntry beam = catalog.byName(CRYSTALSKULL_BEAM_PAM);
        if (beam == null) {
            return;
        }
        String clip = catalog.resolveClip(beam, "laser_beam");
        ClipRef beamRef = clips.getOrLoad(beam.path(), clip);
        if (beamRef == null) {
            return;
        }
        float beamTime = time - StealSunBehavior.ATTACK_BEAM_AT;
        if (beamTime > beamRef.duration) {
            return;
        }
        ClipRef attack = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (crystalSkullPart == null) {
            crystalSkullPart = firstDrawnPart(attack, CRYSTALSKULL_SKULL_PARTS);
        }
        if (crystalBeamPart == null) {
            crystalBeamPart = firstDrawnPart(beamRef, CRYSTALSKULL_BEAM_PARTS);
        }
        Rectangle skull = partAt(attack, time, crystalSkullPart);
        Rectangle beamBox = partAt(beamRef, beamTime, crystalBeamPart);
        if (beamBox == null) {
            beamBox = player.bounds(beam.path(), clip);
        }
        float s = AnimScale.ZOMBIE;
        float bx = x;
        float by = y;
        if (skull != null && beamBox != null) {
            bx = x + (skull.x - (beamBox.x + beamBox.width)) * s;
            by = y + ((beamBox.y + beamBox.height * 0.5f) - (skull.y + skull.height * 0.5f)) * s;
        } else if (skull != null) {
            bx = x + skull.x * s;
            by = y - (skull.y + skull.height * 0.5f) * s;
        }
        player.draw(batch, beamRef, beamTime, bx, by, s, s, false);
    }

    private String firstDrawnPart(ClipRef clip, String[] names) {
        if (clip == null || names == null) {
            return null;
        }
        for (String name : names) {
            Rectangle[] frames = player.partBoundsByFrame(clip, name);
            if (frames == null) {
                continue;
            }
            for (Rectangle frame : frames) {
                if (frame != null) {
                    return name;
                }
            }
        }
        return null;
    }

    /** Current-frame part box; if that frame is empty, first non-null {@code partBoundsByFrame}. */
    private Rectangle partAt(ClipRef clip, float time, String name) {
        if (clip == null || name == null) {
            return null;
        }
        Rectangle now = player.partBounds(clip, time, name);
        if (now != null) {
            return now;
        }
        Rectangle[] frames = player.partBoundsByFrame(clip, name);
        if (frames == null) {
            return null;
        }
        int i = clip.duration > 0f
            ? Math.min(frames.length - 1, Math.max(0, (int) (time / clip.duration * frames.length)))
            : 0;
        for (int k = 0; k < frames.length; k++) {
            Rectangle box = frames[(i + k) % frames.length];
            if (box != null) {
                return box;
            }
        }
        return null;
    }

    /** First frame of a new {@code push}: rewind so a second shove doesn't keep the old time. */
    private void restartArcadePushClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !"push".equals(pose.clipName())) {
            return;
        }
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        if (push == null || !push.isPushing() || push.getPushTimer() != 0f) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code fly_start} / {@code fly_end}: rewind so the next hop replays. */
    private void restartDodoFlyClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null) {
            return;
        }
        String clip = pose.clipName();
        if (!"fly_start".equals(clip) && !"fly_end".equals(clip)) {
            return;
        }
        FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);
        if (fly == null || !fly.isFlying() || fly.getFlyTimer() != 0f) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code throw}: rewind so the next barrage replays. */
    private void restartHunterThrowClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !HunterAnim.THROW_CLIP.equals(pose.clipName())) {
            return;
        }
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.isThrowing()) {
            return;
        }
        if (shoot.getSnowballsRemainingInBarrage() != ShootBehavior.HUNTER_SNOWBALLS_PER_BARRAGE
            || shoot.getSnowballTimer() != 0f) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code spinup} / {@code spindown}: rewind so the next cycle replays. */
    private void restartJugglerSpinClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null) {
            return;
        }
        String clip = pose.clipName();
        if (!JugglerAnim.SPINUP_CLIP.equals(clip) && !JugglerAnim.SPINDOWN_CLIP.equals(clip)) {
            return;
        }
        JuggleBehavior juggle = (JuggleBehavior) zombie.getBehavior(ZombieBehaviorType.JUGGLE);
        if (juggle == null || juggle.getClipTimer() != 0f) {
            return;
        }
        if (JugglerAnim.SPINUP_CLIP.equals(clip)
            && juggle.getPhase() != JuggleBehavior.JugglePhase.SPINUP) {
            return;
        }
        if (JugglerAnim.SPINDOWN_CLIP.equals(clip)
            && juggle.getPhase() != JuggleBehavior.JugglePhase.SPINDOWN) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    private void spawnHunterSplat(ZombieInstance zombie) {
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.isIceAgeHunter(zombie)) {
            return;
        }
        if (shoot.isThrowing()) {
            preloadHunterSplat();
        }
        int seq = shoot.getSnowballSplatSeq();
        int seen = hunterSplatSeq.getOrDefault(zombie, 0);
        if (seq <= seen) {
            return;
        }
        hunterSplatSeq.put(zombie, seq);
        Point at = shoot.getLastSnowballSplatAt();
        if (at == null || catalog == null) {
            return;
        }
        PamCatalog.PamEntry splat = catalog.byName(HunterAnim.SPLAT_PAM);
        if (splat == null) {
            return;
        }
        String clip = catalog.resolveClip(splat, "animation");
        float[] xy = layout.centerOf(at.getY(), at.getX());
        for (int n = seen; n < seq; n++) {
            hunterSplats.add(new BlastFx(splat.path(), clip, xy[0], xy[1]));
        }
    }

    private void preloadHunterSplat() {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry splat = catalog.byName(HunterAnim.SPLAT_PAM);
        if (splat != null) {
            clips.getOrLoad(splat.path(), catalog.resolveClip(splat, "animation"));
        }
    }

    private void drawHunterSplats(Batch batch, float delta, int row) {
        float scale = AnimScale.PLANT;
        for (int i = hunterSplats.size() - 1; i >= 0; i--) {
            BlastFx fx = hunterSplats.get(i);
            if (layout.rowAt(fx.y) != row) {
                continue;
            }
            ClipRef ref = clips.getOrLoad(fx.pamPath, fx.clip != null ? fx.clip : "animation");
            if (ref == null || fx.time >= ref.duration) {
                hunterSplats.remove(i);
                continue;
            }
            player.draw(batch, ref, fx.time, fx.x, fx.y, scale, scale, false);
            fx.time += delta;
        }
    }

    /** First frame of a new {@code toss}: rewind so the next throw replays. */
    private void restartOctopusTossClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !OctopusAnim.TOSS_CLIP.equals(pose.clipName())) {
            return;
        }
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.isOctopusThrowing() || shoot.getOctopusTossTimer() != 0f) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /**
     * Snapshot the held octopus part so the projectile PAM starts on those bounds.
     * libPVZ {@code partBounds} is PAM-local (Y-down); draw flips Y.
     */
    private void captureOctopusRelease(ZombieInstance zombie, AnimPose pose,
                                       float x, float y, float time) {
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.hasReleasedOctopus() || pose == null) {
            return;
        }
        float s = AnimScale.ZOMBIE * pose.scale();
        ClipRef toss = clips.getOrLoad(pose.pamPath(), pose.clipName());
        Rectangle from = partAt(toss, time, OctopusAnim.HELD_PART);
        if (from == null) {
            from = partAt(toss, ShootBehavior.OCTOPUS_RELEASE_AT, OctopusAnim.HELD_PART);
        }
        float heldX = x;
        float heldY = y;
        if (from != null) {
            heldX = x + (from.x + from.width * 0.5f) * s;
            heldY = y - (from.y + from.height * 0.5f) * s;
        }
        PamCatalog.PamEntry proj = catalog == null ? null : catalog.byName(OctopusAnim.PROJECTILE_PAM);
        ClipRef fly = proj == null ? null : clips.getOrLoad(proj.path(), OctopusAnim.FLY_CLIP);
        Rectangle to = partAt(fly, 0f, OctopusAnim.HELD_PART);
        float originX = heldX;
        float originY = heldY;
        if (to != null) {
            originX = heldX - (to.x + to.width * 0.5f) * s;
            originY = heldY + (to.y + to.height * 0.5f) * s;
        }
        for (ShootBehavior.OctopusShot shot : shoot.getOctopusShots()) {
            if (shot.isFlying() && !octopusAlign.containsKey(shot)) {
                octopusAlign.put(shot, new float[]{originX, originY});
            }
        }
        preloadOctopusProjectile();
    }

    private void preloadOctopusProjectile() {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry proj = catalog.byName(OctopusAnim.PROJECTILE_PAM);
        if (proj == null) {
            return;
        }
        clips.getOrLoad(proj.path(), OctopusAnim.FLY_CLIP);
        clips.getOrLoad(proj.path(), OctopusAnim.IMPACT_CLIP);
        clips.getOrLoad(proj.path(), OctopusAnim.LOOP_CLIP);
        clips.getOrLoad(proj.path(), OctopusAnim.DIE_CLIP);
    }

    private void drawOctopi(Batch batch, GameModel model, float delta) {
        preloadOctopusProjectile();
        PamCatalog.PamEntry proj = catalog == null ? null : catalog.byName(OctopusAnim.PROJECTILE_PAM);
        if (proj == null) {
            return;
        }
        String path = proj.path();
        for (ZombieInstance zombie : model.getZombies()) {
            ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
            if (shoot == null || !shoot.isBeachOctopus(zombie)) {
                continue;
            }
            if (shoot.isOctopusThrowing()) {
                clips.getOrLoad(path, OctopusAnim.FLY_CLIP);
            }
            for (ShootBehavior.OctopusShot shot : shoot.getOctopusShots()) {
                if (shot.isFlying()) {
                    drawFlyingOctopus(batch, shot, path);
                }
            }
        }
        Set<PlantInstance> plants = new HashSet<>();
        for (PlantInstance plant : model.getAllPlants()) {
            plants.add(plant);
            if (plant.hasOctopusCoating() && !octopusCoats.containsKey(plant)) {
                octopusCoats.put(plant, new OctopusCoatFx());
            }
        }
        for (PlantInstance plant : new ArrayList<>(octopusCoats.keySet())) {
            OctopusCoatFx fx = octopusCoats.get(plant);
            boolean gone = !plants.contains(plant) || plant.getCurrentHP() <= 0
                || (!plant.hasOctopusCoating() && !fx.dying);
            if (gone && !fx.dying) {
                fx.dying = true;
                fx.time = 0f;
            }
            if (!drawOctopusCoat(batch, plant, path, fx, delta)) {
                octopusCoats.remove(plant);
            }
        }
    }

    private void drawFlyingOctopus(Batch batch, ShootBehavior.OctopusShot shot, String path) {
        Point cell = shot.targetCell();
        if (cell == null) {
            return;
        }
        float[] dest = layout.centerOf(cell.getY(), cell.getX());
        float[] start = octopusAlign.get(shot);
        float x0 = dest[0];
        float y0 = dest[1];
        if (start != null) {
            x0 = start[0];
            y0 = start[1];
        } else if (shot.thrower() != null && zombieWorldCenter(shot.thrower(), xyTmp)) {
            x0 = xyTmp[0];
            y0 = xyTmp[1];
        }
        float t = shot.progress();
        float x = x0 + (dest[0] - x0) * t;
        float y = y0 + (dest[1] - y0) * t + shot.heightTiles() * layout.cellHeight();
        ClipRef ref = clips.getOrLoad(path, OctopusAnim.FLY_CLIP);
        if (ref == null) {
            return;
        }
        float scale = AnimScale.ZOMBIE;
        player.draw(batch, ref, shot.timer(), x, y, scale, scale, true);
    }

    /**
     * @return false when this overlay is finished and should be dropped
     */
    private boolean drawOctopusCoat(Batch batch, PlantInstance plant, String path,
                                    OctopusCoatFx fx, float delta) {
        Point pos = plant.getPosition();
        if (pos == null && !fx.dying) {
            return false;
        }
        float x;
        float y;
        if (pos != null) {
            float[] xy = layout.centerOf(pos.getY(), pos.getX());
            x = xy[0];
            y = xy[1];
            fx.x = x;
            fx.y = y;
        } else {
            x = fx.x;
            y = fx.y;
        }
        String clip = fx.dying ? OctopusAnim.DIE_CLIP
            : fx.time < impactDuration(path) ? OctopusAnim.IMPACT_CLIP
            : OctopusAnim.LOOP_CLIP;
        boolean loop = !fx.dying && OctopusAnim.LOOP_CLIP.equals(clip);
        ClipRef ref = clips.getOrLoad(path, clip);
        seenThisFrame.add(fx);
        int coatHp = plant.hasOctopusCoating() ? plant.getIceHp() : 0;
        float flash = tickHitFlash(fx, coatHp, delta);
        if (ref == null) {
            fx.time += delta;
            return !fx.dying;
        }
        float clipTime = OctopusAnim.LOOP_CLIP.equals(clip)
            ? Math.max(0f, fx.time - impactDuration(path))
            : fx.time;
        player.draw(batch, ref, clipTime, x, y, AnimScale.ZOMBIE, AnimScale.ZOMBIE, loop);
        overlayHitFlash(batch, flash,
            () -> player.draw(batch, ref, clipTime, x, y, AnimScale.ZOMBIE, AnimScale.ZOMBIE, loop));
        fx.time += delta;
        if (fx.dying && fx.time >= ref.duration) {
            return false;
        }
        return true;
    }

    private float impactDuration(String path) {
        ClipRef impact = clips.getOrLoad(path, OctopusAnim.IMPACT_CLIP);
        return impact == null ? 0.9667f : impact.duration;
    }

    /** First frame of a new {@code power}: rewind so the next raise doesn't keep the old time. */
    private void restartTombRaiseClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !"power".equals(pose.clipName())) {
            return;
        }
        SummonBehavior summon = (SummonBehavior) zombie.getBehavior(ZombieBehaviorType.SUMMON);
        if (summon == null || !summon.isRaising() || summon.getRaiseTimer() != 0f) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code sheep}: rewind so the next cast replays. */
    private void restartWizardSheepClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !WizardAnim.SHEEP_CLIP.equals(pose.clipName())) {
            return;
        }
        TransformBehavior transform = (TransformBehavior) zombie.getBehavior(ZombieBehaviorType.TRANSFORM);
        if (transform == null || !transform.isCasting() || transform.getSheepTimer() != 0f) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code intro}/{@code special}: rewind so the next cycle replays. */
    private void restartDarkKingClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null) {
            return;
        }
        String clip = pose.clipName();
        if (!DarkKingAnim.INTRO_CLIP.equals(clip) && !DarkKingAnim.SPECIAL_CLIP.equals(clip)) {
            return;
        }
        BuffBehavior buff = (BuffBehavior) zombie.getBehavior(ZombieBehaviorType.BUFF);
        if (buff == null || buff.getPhaseTimer() != 0f) {
            return;
        }
        boolean match = switch (buff.getPhase()) {
            case INTRO -> DarkKingAnim.INTRO_CLIP.equals(clip);
            case SPECIAL -> DarkKingAnim.SPECIAL_CLIP.equals(clip);
            case IDLE -> false;
        };
        if (!match) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    private void restartDarkZombossClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !DarkZombossAnim.isDarkZomboss(zombie)) {
            return;
        }
        String clip = pose.clipName();
        if (!DarkZombossAnim.INTRO_CLIP.equals(clip)
                && !DarkZombossAnim.STUN_START_CLIP.equals(clip)
                && !DarkZombossAnim.STUN_END_CLIP.equals(clip)
                && !"fire_attack".equals(clip)
                && !"fire_attack_end".equals(clip)
                && !"fire_bomb".equals(clip)
                && !"summoning".equals(clip)) {
            return;
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return;
        }
        float total = boss.currentPhaseDurationSeconds();
        float remaining = boss.getPhaseTimer();
        boolean phaseJustStarted = total > 0f && Math.abs(remaining - total) < 1e-3f;
        boolean stunStartJustStarted = false;
        boolean stunEndJustStarted = false;
        if (boss.getPhase() == ZombossPhase.STUNNED) {
            float elapsed = boss.phaseProgress01() * total;
            PamCatalog.PamEntry entry = catalog.forZombie(DarkZombossAnim.DEFINITION_NAME);
            float startDur = PamCatalog.clipDurationSeconds(entry, DarkZombossAnim.STUN_START_CLIP);
            float endDur = PamCatalog.clipDurationSeconds(entry, DarkZombossAnim.STUN_END_CLIP);
            if (startDur <= 0f) {
                startDur = 0.4333f;
            }
            if (endDur <= 0f) {
                endDur = 0.4667f;
            }
            stunStartJustStarted = DarkZombossAnim.STUN_START_CLIP.equals(clip) && elapsed < 1e-3f;
            float endAt = Math.max(startDur, total - endDur);
            stunEndJustStarted = DarkZombossAnim.STUN_END_CLIP.equals(clip)
                    && elapsed >= endAt && elapsed < endAt + 1e-3f + 1f / 60f;
        }
        boolean match = switch (boss.getPhase()) {
            case INTRO -> DarkZombossAnim.INTRO_CLIP.equals(clip) && phaseJustStarted;
            case STUNNED -> (DarkZombossAnim.STUN_START_CLIP.equals(clip) && stunStartJustStarted)
                    || (DarkZombossAnim.STUN_END_CLIP.equals(clip) && stunEndJustStarted);
            case ACTION -> phaseJustStarted && (
                    "fire_attack".equals(clip)
                            || "fire_bomb".equals(clip)
                            || "summoning".equals(clip));
            default -> false;
        };
        if (!match) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    private float darkZombossClipPhase(ZombieInstance zombie, AnimPose pose, ClipRef ref) {
        if (pose == null || ref == null || ref.duration <= 0f || !DarkZombossAnim.isDarkZomboss(zombie)) {
            return NO_PHASE;
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return NO_PHASE;
        }
        String clip = pose.clipName();
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        if (boss.getPhase() == ZombossPhase.INTRO
                && DarkZombossAnim.INTRO_CLIP.equals(clip)) {
            return Math.max(0f, Math.min(1f, elapsed / ref.duration));
        }
        if (boss.getPhase() == ZombossPhase.STUNNED) {
            PamCatalog.PamEntry entry = catalog.forZombie(DarkZombossAnim.DEFINITION_NAME);
            float startDur = PamCatalog.clipDurationSeconds(entry, DarkZombossAnim.STUN_START_CLIP);
            float endDur = PamCatalog.clipDurationSeconds(entry, DarkZombossAnim.STUN_END_CLIP);
            if (startDur <= 0f) {
                startDur = 0.4333f;
            }
            if (endDur <= 0f) {
                endDur = 0.4667f;
            }
            if (DarkZombossAnim.STUN_START_CLIP.equals(clip)) {
                return Math.max(0f, Math.min(1f, elapsed / startDur));
            }
            if (DarkZombossAnim.STUN_END_CLIP.equals(clip)) {
                float endAt = Math.max(startDur, total - endDur);
                float intoEnd = elapsed - endAt;
                return Math.max(0f, Math.min(1f, intoEnd / endDur));
            }
        }
        if (boss.getPhase() == ZombossPhase.ACTION
                && boss.getCurrentAction() == ZombossAction.BURN_ROWS) {
            if ("fire_attack".equals(clip)) {
                return Math.max(0f, Math.min(1f,
                        elapsed / DarkZombossBehavior.FIRE_ATTACK_START_SECONDS));
            }
            if ("fire_attack_end".equals(clip)) {
                float endAt = DarkZombossBehavior.burnRowsDurationSeconds()
                        - DarkZombossBehavior.FIRE_ATTACK_END_SECONDS;
                float intoEnd = elapsed - endAt;
                return Math.max(0f, Math.min(1f,
                        intoEnd / DarkZombossBehavior.FIRE_ATTACK_END_SECONDS));
            }
        }
        return NO_PHASE;
    }

    /** First frame of a new {@code intro}/{@code cast}/{@code reel}: rewind so the next cycle replays. */
    private void restartFishermanClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null) {
            return;
        }
        String clip = pose.clipName();
        if (!"intro".equals(clip) && !"cast".equals(clip) && !"reel".equals(clip)) {
            return;
        }
        FishBehavior fish = (FishBehavior) zombie.getBehavior(ZombieBehaviorType.FISH);
        if (fish == null || fish.getPhaseTimer() != 0f) {
            return;
        }
        boolean match = switch (fish.getPhase()) {
            case INTRO -> "intro".equals(clip);
            case CASTING -> "cast".equals(clip);
            case REELING -> "reel".equals(clip);
            case IDLE -> false;
        };
        if (!match) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** Fuse done: cut walk immediately and play {@code blastoff} from t=0. */
    private void restartProspectorJumpClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !"blastoff".equals(pose.clipName())) {
            return;
        }
        JumpBehavior jump = (JumpBehavior) zombie.getBehavior(ZombieBehaviorType.JUMP);
        if (jump == null || jump.getPhase() != JumpBehavior.JumpPhase.JUMPING) {
            return;
        }
        if (jump.getTravelTimer() > JumpBehavior.BLASTOFF_DURATION) {
            return;
        }
        AnimClock clock = clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** 0 at the start of this jump clip; not the leftover walk gait phase. */
    private static float prospectorJumpPhase(JumpBehavior jump, String clip, float duration) {
        float local = jump.getTravelTimer();
        if ("fly".equals(clip)) {
            local -= JumpBehavior.BLASTOFF_DURATION;
        } else if ("land".equals(clip)) {
            local -= JumpBehavior.BLASTOFF_DURATION + JumpBehavior.FLY_DURATION;
        }
        return Math.max(0f, local) / duration;
    }

    private Chapter artChapterFor(ZombieInstance zombie, Chapter lawn) {
        Chapter skin = artChapters.get(zombie);
        if (skin != null) {
            return skin;
        }
        ThrowImpBehavior.Flight flight = ThrowImpBehavior.flightOf(zombie);
        if (flight != null && flight.thrower() != null) {
            Chapter fromThrower = artChapters.get(flight.thrower());
            if (fromThrower != null) {
                return fromThrower;
            }
        }
        return lawn;
    }

    /**
     * Shift the Imp so {@code zombie_imp_skull} sits where it left the Gargantuar.
     * Stored in world pixels and faded out along the flight so it lands on the tile centre.
     *
     * <p>libPVZ {@code partBounds} is PAM-local (Y-down from the canvas centre). Draw flips Y,
     * so world Y is {@code originY - localY * scale}.
     */
    private void alignToss(ZombieInstance imp, ThrowImpBehavior.Flight flight,
                           AnimPose pose, float impX, float impY) {
        if (!flight.isFlying() || tossAlign.containsKey(imp) || pose == null) {
            return;
        }
        LiveSnap garg = lastLive.get(flight.thrower());
        if (garg == null || garg.pose == null) {
            return;
        }
        float s = AnimScale.ZOMBIE;
        float gargTime = "cannon_fire".equals(garg.pose.clipName())
            ? garg.time : ThrowImpBehavior.RELEASE_AT;
        Rectangle from = skullBounds(garg.pose.pamPath(), "cannon_fire", gargTime);
        Rectangle to = skullBounds(pose.pamPath(), pose.clipName(), 0f);
        if (from == null || to == null) {
            return;
        }
        float gargSkullX = garg.x + (from.x + from.width * 0.5f) * s;
        float gargSkullY = garg.y - (from.y + from.height * 0.5f) * s;
        float impSkullX = impX + (to.x + to.width * 0.5f) * s;
        float impSkullY = impY - (to.y + to.height * 0.5f) * s;
        tossAlign.put(imp, new float[]{gargSkullX - impSkullX, gargSkullY - impSkullY});
    }

    private Rectangle skullBounds(String pam, String clip, float time) {
        Rectangle bounds = player.partBounds(pam, clip, time, "zombie_imp_skull");
        if (bounds == null) {
            bounds = player.partBounds(pam, clip, time, "_zombie_imp_head_top");
        }
        return bounds;
    }

    private static ZombieGait gaitFor(ZombieInstance zombie) {
        return ZombieGaitProfiles.forZombie(
            zombie.getDefinition() == null ? null : zombie.getDefinition().getName());
    }

    /** Measuring walks every frame of the clip, so each walk cycle is read from the art once. */
    private ZombieFootfallCurve footfallFor(ZombieGait gait, ClipRef walkClip) {
        ZombieFootfallCurve footfall = footfalls.get(walkClip);
        if (footfall == null) {
            footfall = gait.measureFootfall(player, walkClip);
            footfalls.put(walkClip, footfall);
        }
        return footfall;
    }

    private boolean zombieWorldCenter(ZombieInstance zombie, float[] out) {
        FloatPoint cont = zombie.getContinuousPosition();
        Point grid = zombie.getGridPosition();
        float row;
        float progressX;
        if (cont != null) {
            progressX = cont.getX();
            row = cont.getY();
        } else if (grid != null) {
            progressX = grid.getX();
            row = grid.getY();
        } else {
            return false;
        }
        // Slide glide: drift the sprite between lanes instead of snapping.
        LaneSlide glide = laneGlides.get(zombie);
        if (glide != null && glide.progress() < 1f) {
            row = glide.getFromRow()
                    + (glide.getToRow() - glide.getFromRow()) * glide.progress();
        }
        float[] xy = layout.centerOf(row, progressX);
        out[0] = xy[0];
        out[1] = xy[1];
        out[2] = progressX;
        return true;
    }

    /** Grid lane; {@code -1} if the plant is not on a cell. */
    static int plantRow(PlantInstance plant) {
        Point pos = plant.getPosition();
        return pos == null ? -1 : pos.getY();
    }

    /** Lane the zombie is drawn in; continuous Y wins so a lane-swap stays sorted. */
    static int zombieRow(ZombieInstance zombie) {
        FloatPoint cont = zombie.getContinuousPosition();
        if (cont != null) {
            return Math.round(cont.getY());
        }
        Point grid = zombie.getGridPosition();
        return grid == null ? -1 : grid.getY();
    }

    private static boolean drawsAboveLawn(ZombieInstance zombie) {
        return zombie != null && zombie.hasBehavior(ZombieBehaviorType.ZOMBOSS);
    }

    static int clampRow(int row, int rows) {
        if (rows <= 0 || row < 0) {
            return 0;
        }
        return Math.min(rows - 1, row);
    }

    private float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, pose.cacheKey(), 0f, 0f, 0f);
    }

    private float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta,
                          String clockKey) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, clockKey, 0f, 0f, 0f);
    }

    private float drawPose(Batch batch, Object entity, AnimPose pose,
                           float x, float y, float baseScale, float phase, float flash, float delta,
                           float glow) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, pose.cacheKey(), glow, 0f, 0f);
    }

    private float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta,
                          String clockKey, float glow) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, clockKey, glow, 0f, 0f);
    }

    private float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta,
                          String clockKey, float glow, float chill) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, clockKey, glow, chill, 0f);
    }

    private float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta,
                          String clockKey, float glow, float chill, float dangerRed) {
        seenThisFrame.add(entity);
        if (pose.isSpritesheet()) {
            return drawSheetPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, clockKey);
        }
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return 0f;
        }
        float stateTime = phase >= 0f
                ? phase * ref.duration
                : advanceClock(entity, clockKey, delta);
        if (phase >= 0f) {
            // Gait skips the wall clock; still stamp the clip so the next one-shot
            // (Arcade {@code push}, All-Star {@code tackle}, …) restarts from 0.
            stampClockClip(entity, clockKey);
        }
        if (pose.reverse() && ref.duration > 0f) {
            stateTime = Math.max(0f, ref.duration - Math.min(stateTime, ref.duration));
        }
        float scale = baseScale * pose.scale();
        float drawTime = stateTime;
        Runnable body = () -> drawClip(batch, ref, pose, drawTime, x, y, scale);
        if (glow > 0f) {
            glowGreenShader().begin(batch, glow);
            body.run();
            glowGreenShader().end(batch);
        } else if (chill > 0f) {
            chillBlueShader().begin(batch, chill);
            body.run();
            chillBlueShader().end(batch);
        } else if (dangerRed > 0f) {
            dangerRedShader().begin(batch, dangerRed);
            body.run();
            dangerRedShader().end(batch);
        } else {
            body.run();
        }
        overlayHitFlash(batch, flash, body);
        return stateTime;
    }

    private float drawSheetPose(Batch batch, Object entity, AnimPose pose,
                                float x, float y, float baseScale, float phase, float flash,
                                float delta, String clockKey) {
        PlantSpritesheetCatalog.ClipSpec spec =
                plantSheets == null ? null : plantSheets.byCacheKey(pose.clipName());
        SpritesheetClipCache.SheetAnim sheet =
                spec == null ? null : sheetClips.getOrLoad(spec);
        if (sheet == null) {
            return 0f;
        }
        float duration = sheet.duration();
        float stateTime = phase >= 0f
                ? phase * duration
                : advanceClock(entity, clockKey, delta);
        if (phase >= 0f) {
            stampClockClip(entity, clockKey);
        }
        if (pose.reverse() && duration > 0f) {
            stateTime = Math.max(0f, duration - Math.min(stateTime, duration));
        }
        float scale = baseScale * pose.scale();
        float drawTime = stateTime;
        drawSheet(batch, sheet, pose, drawTime, x, y, scale);
        overlayHitFlash(batch, flash, () -> drawSheet(batch, sheet, pose, drawTime, x, y, scale));
        return stateTime;
    }

    private void drawClip(Batch batch, ClipRef ref, AnimPose pose,
                          float stateTime, float x, float y, float scale) {
        float sx = pose.flipX() ? -scale : scale;
        if (pose.visibility() == null) {
            player.draw(batch, ref, stateTime, x, y, sx, scale, pose.loop());
        } else {
            player.draw(batch, ref, stateTime, x, y, sx, scale, pose.loop(), pose.visibility());
        }
    }

    private void drawSheet(Batch batch, SpritesheetClipCache.SheetAnim sheet, AnimPose pose,
                           float stateTime, float x, float y, float scale) {
        TextureRegion frame = sheet.animation().getKeyFrame(stateTime, pose.loop());
        if (frame == null) {
            return;
        }
        float w = frame.getRegionWidth() * scale;
        float h = frame.getRegionHeight() * scale;
        float sx = pose.flipX() ? -1f : 1f;
        batch.draw(frame, x - w * 0.5f, y - h * 0.5f, w * 0.5f, h * 0.5f, w, h, sx, 1f, 0f);
    }

    /** White additive flash while HP dropped since last frame. */
    private float tickHitFlash(ZombieInstance zombie, float delta) {
        return tickHitFlash(zombie, vitality(zombie), delta);
    }

    private float tickHitFlash(Object entity, int vitality, float delta) {
        HitFlash flash = hitFlashes.get(entity);
        if (flash == null) {
            flash = new HitFlash();
            flash.vitality = vitality;
            hitFlashes.put(entity, flash);
        } else {
            boolean chewGate = entity instanceof PlantInstance;
            if (shouldRestartHitFlash(flash.vitality, vitality, flash.remaining,
                chewGate ? flash.quiet : 0f)) {
                flash.remaining = HIT_FLASH_SEC;
                if (chewGate) {
                    flash.quiet = CHEW_FLASH_COOLDOWN;
                }
            }
            flash.vitality = vitality;
            if (chewGate) {
                flash.quiet = Math.max(0f, flash.quiet - delta);
            }
        }
        if (flash.remaining <= 0f) {
            return 0f;
        }
        float strength = flash.remaining / HIT_FLASH_SEC;
        flash.remaining -= delta;
        return strength;
    }

    /** Chew ticks are 1 HP; peas restart immediately. Plants also wait {@code quiet}. */
    static boolean shouldRestartHitFlash(int prevHp, int hp, float remaining, float quiet) {
        if (hp >= prevHp) {
            return false;
        }
        if (prevHp - hp >= HIT_FLASH_CHUNK) {
            return true;
        }
        return remaining <= 0f && quiet <= 0f;
    }

    private void overlayHitFlash(Batch batch, float flash, Runnable draw) {
        if (flash <= 0f) {
            return;
        }
        hitFlashShader().begin(batch, flash);
        draw.run();
        hitFlashShader().end(batch);
    }

    /** Plant HP, plus hunter-ice coating when that ice has no octopus overlay. */
    static int plantVitality(PlantInstance plant) {
        int hp = plant.getCurrentHP();
        if (plant.isFrozen() && !plant.hasOctopusCoating()) {
            hp += plant.getIceHp();
        }
        return hp;
    }

    static int itemHp(Pushable item) {
        return item instanceof GridItem grid ? grid.getHp() : 0;
    }

    private static int vitality(ZombieInstance zombie) {
        int hp = zombie.getCurrentHP();
        List<Armor> armors = zombie.getArmors();
        if (armors != null) {
            for (Armor armor : armors) {
                hp += armor.getCurrentHealth();
            }
        }
        return hp;
    }

    private float advanceClock(Object entity, String clipKey, float delta) {
        AnimClock clock = clockFor(entity);
        if (!clipKey.equals(clock.clipKey)) {
            clock.clipKey = clipKey;
            clock.time = 0f;
        } else {
            clock.time += delta;
        }
        return clock.time;
    }

    /** Gait walks never call {@link #advanceClock}; stamp so the next one-shot restarts. */
    private void stampClockClip(Object entity, String clipKey) {
        AnimClock clock = clockFor(entity);
        if (!clipKey.equals(clock.clipKey)) {
            clock.clipKey = clipKey;
            clock.time = 0f;
        }
    }

    private AnimClock clockFor(Object entity) {
        AnimClock clock = clocks.get(entity);
        if (clock == null) {
            clock = new AnimClock();
            clocks.put(entity, clock);
        }
        return clock;
    }

    private static final class AnimClock {
        String clipKey;
        float time;
    }

    /** {@code particle_arm} hops off {@code particles} at half HP. */
    private void maybePopLostHand(ZombieInstance zombie, AnimPose pose, float x, float y) {
        if (zombie == null || pose == null || pose.isSpritesheet() || lostHands.containsKey(zombie)) {
            return;
        }
        if (!atOrBelowHalfHp(zombie)) {
            return;
        }
        String pam = pose.pamPath();
        if (firstLoadedClip(pam, "particles", null) == null) {
            return;
        }
        List<String> arms = particleArmParts(pam);
        if (arms.isEmpty()) {
            for (String part : particleParts(pam)) {
                if (isArmPopPart(part)) {
                    arms.add(part);
                }
            }
        }
        if (arms.isEmpty()) {
            return;
        }
        float dir = zombie.isMovingBackward() || pose.flipX() ? -1f : 1f;
        for (String part : arms) {
            addLimbPop(pam, "particles", part, x, y, 0f, dir, 0.15f, 0.85f, 0f, false);
        }
        lostHands.put(zombie, Boolean.TRUE);
    }

    /** Helm/bucket/brick/crown/shoulder: last damage sprite hops off when the piece leaves. */
    private void popBrokenArmor(ZombieInstance zombie, AnimPose pose, float x, float y) {
        HitFlash flash = hitFlashes.get(zombie);
        if (flash == null) {
            flash = new HitFlash();
            flash.vitality = vitality(zombie);
            hitFlashes.put(zombie, flash);
        }
        List<Armor> living = new ArrayList<>();
        List<Armor> armors = zombie.getArmors();
        if (armors != null) {
            for (Armor armor : armors) {
                if (armor != null && !armor.isDestroyed() && armor.popLayer() != null) {
                    living.add(armor);
                }
            }
        }
        if (flash.prevDroppables != null) {
            for (Armor armor : flash.prevDroppables) {
                if (living.contains(armor)) {
                    continue;
                }
                String part = armor.popLayer();
                if (part == null) {
                    continue;
                }
                float dir = zombie.isMovingBackward() ? -1f : 1f;
                float hopTime = 2f * ARMOR_POP_HOP / -ARMOR_POP_GRAVITY;
                armorPops.add(new ArmorPop(
                    pose.pamPath(), pose.clipName(), part,
                    x, y, y - layout.cellHeight() * 0.5f,
                    dir * ARMOR_POP_BACK_TILES * layout.cellWidth() / hopTime,
                    ARMOR_POP_HOP * layout.cellHeight(),
                    ARMOR_POP_GRAVITY * layout.cellHeight()));
            }
        }
        flash.prevDroppables = living;
    }

    private void drawArmorPops(Batch batch, float delta, int row) {
        for (int i = armorPops.size() - 1; i >= 0; i--) {
            ArmorPop pop = armorPops.get(i);
            if (layout.rowAt(pop.groundY + layout.cellHeight() * 0.5f) != row) {
                continue;
            }
            pop.life += delta;
            if (!pop.grounded) {
                pop.vy += pop.gravity * delta;
                pop.x += pop.vx * delta;
                pop.y += pop.vy * delta;
                if (pop.y <= pop.groundY) {
                    pop.y = pop.groundY;
                    if (pop.bounces < pop.maxBounces && -pop.vy > layout.cellHeight() * 0.25f) {
                        pop.vy = -pop.vy * POP_BOUNCE;
                        pop.vx *= 0.55f;
                        pop.bounces++;
                    } else {
                        pop.vx = 0f;
                        pop.vy = 0f;
                        pop.grounded = true;
                    }
                }
            } else if (pop.life >= pop.hold) {
                pop.fade += delta;
                if (pop.fade >= ARMOR_POP_FADE) {
                    armorPops.remove(i);
                    continue;
                }
            }
            float alpha = pop.grounded ? 1f - pop.fade / ARMOR_POP_FADE : 1f;
            batch.setColor(1f, 1f, 1f, alpha);
            float s = AnimScale.ZOMBIE;
            batchTransform.set(batch.getTransformMatrix());
            popTransform.set(batchTransform)
                .translate(pop.x, pop.y, 0f)
                .scale(s, s, 1f)
                .translate(-pop.x, -pop.y, 0f);
            batch.setTransformMatrix(popTransform);
            clips.getOrLoad(pop.pamPath, pop.clipName);
            if (pop.part == null || isHeadPopPart(pop.part)) {
                ClipRef ref = clips.getOrLoad(pop.pamPath, pop.clipName);
                if (ref != null) {
                    player.draw(batch, ref, pop.clipTime, pop.x, pop.y, 1f, 1f, false,
                        headPopVis(pop.part));
                }
            } else {
                player.drawPart(batch, pop.pamPath, pop.clipName, pop.clipTime, pop.x, pop.y, pop.part);
            }
            batch.setTransformMatrix(batchTransform);
            batch.setColor(Color.WHITE);
        }
    }

    private static final class HitFlash {
        int vitality;
        float remaining;
        /** Seconds before a chew-sized drop may pulse again (plants only). */
        float quiet;
        List<Armor> prevDroppables;
    }

    private static final class ArmorPop {
        final String pamPath;
        final String clipName;
        /** PAM part to draw; {@code null} draws the whole clip (Gargantuar {@code particles}). */
        final String part;
        final float groundY;
        final float gravity;
        float clipTime;
        float x;
        float y;
        float vx;
        float vy;
        float fade;
        /** Seconds since the pop spawned. */
        float life;
        /** Don't start fading before this many seconds have passed, even once grounded. */
        float hold;
        boolean grounded;
        int bounces;
        int maxBounces = 2;

        ArmorPop(String pamPath, String clipName, String part,
                 float x, float y, float groundY, float vx, float vy, float gravity) {
            this.pamPath = pamPath;
            this.clipName = clipName;
            this.part = part;
            this.x = x;
            this.y = y;
            this.groundY = groundY;
            this.vx = vx;
            this.vy = vy;
            this.gravity = gravity;
        }
    }

    private static final class SunFlight {
        final Sun sun;
        final float x0;
        final float y0;
        final float x1;
        final float y1;
        float elapsed;

        SunFlight(Sun sun, float x0, float y0, float x1, float y1) {
            this.sun = sun;
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }
    }

    /** Cosmetic in-flight plant-food collect animation (mirror of {@link SunFlight}). */
    private static final class PlantFoodFlight {
        final PlantFoodPickup food;
        final float x0;
        final float y0;
        final float x1;
        final float y1;
        float elapsed;

        PlantFoodFlight(PlantFoodPickup food, float x0, float y0, float x1, float y1) {
            this.food = food;
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }
    }

    private static final class LootFlight {
        final LootPickup loot;
        final float x0;
        final float y0;
        final float x1;
        final float y1;
        final Runnable onComplete;
        float elapsed;
        boolean done;

        LootFlight(LootPickup loot, float x0, float y0, float x1, float y1, Runnable onComplete) {
            this.loot = loot;
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.onComplete = onComplete;
        }
    }

    private static final class LiveSnap {
        final AnimPose pose;
        final float x;
        final float y;
        final boolean backward;
        final float time;

        LiveSnap(AnimPose pose, float x, float y, boolean backward, float time) {
            this.pose = pose;
            this.x = x;
            this.y = y;
            this.backward = backward;
            this.time = time;
        }
    }

    private static final class DeathFx {
        final AnimPose pose;
        final float x;
        final float y;
        final boolean drown;
        float time;
        float drownWaterY = Float.NaN;
        float snorkelRise;
        float hitFlash;
        float holdSeconds = Float.NaN;

        DeathFx(AnimPose pose, float x, float y) {
            this(pose, x, y, false);
        }

        DeathFx(AnimPose pose, float x, float y, boolean drown) {
            this.pose = pose;
            this.x = x;
            this.y = y;
            this.drown = drown;
        }

        float hold(ClipRef ref) {
            if (!Float.isNaN(holdSeconds)) {
                return Math.max(0f, holdSeconds);
            }
            return ref != null ? ref.duration : 0f;
        }
    }

    private static final class BlastFx {
        final String pamPath;
        final String clip;
        final float x;
        final float y;
        float time;

        BlastFx(String pamPath, float x, float y) {
            this(pamPath, null, x, y);
        }

        BlastFx(String pamPath, String clip, float x, float y) {
            this.pamPath = pamPath;
            this.clip = clip;
            this.x = x;
            this.y = y;
        }
    }

    private static final class OctopusCoatFx {
        float time;
        float x;
        float y;
        boolean dying;
    }

    private enum SheepPhase {
        VANISH, APPEAR, IDLE, LEAVE, EMERGE
    }

    private static final class SheepFx {
        SheepPhase phase = SheepPhase.VANISH;
        float time;
        String idleClip = WizardAnim.IDLE2_CLIP;
    }

    private void spawnDeath(GameModel model, ZombieInstance zombie, LiveSnap snap) {
        if (snap == null || snap.pose == null) {
            return;
        }
        if (trySpawnAsh(zombie, snap)) {
            return;
        }
        if (snap.pose.isSpritesheet()) {
            AnimPose fade = AnimPose.sheetOnce(
                    snap.pose.pamPath(), snap.pose.clipName(), ZombieAnimRole.DIE);
            if (snap.pose.flipX()) {
                fade = fade.flipped();
            }
            DeathFx fx = new DeathFx(fade, snap.x, snap.y, false);
            fx.holdSeconds = 0.4f;
            deathFx.add(fx);
            return;
        }
        // Plant-head PAMs have no die clip — falling back to idle would leave them
        // bobbing on the lawn for a full idle cycle. Fade the last pose instead.
        if (ZombotanyAnim.isPlantHead(zombie)) {
            AnimPose fade = AnimPose.once(
                snap.pose.pamPath(), snap.pose.clipName(), ZombieAnimRole.DIE, snap.pose.visibility());
            if (snap.pose.flipX()) {
                fade = fade.flipped();
            }
            DeathFx fx = new DeathFx(fade, snap.x, snap.y, false);
            fx.holdSeconds = 0f;
            deathFx.add(fx);
            return;
        }
        boolean barrelLeft = attachBarrelLeftover(model, zombie, snap);
        if (barrelLeft) {
            return;
        }
        String pam = snap.pose.pamPath();
        String dieClip = BarrelRollerAnim.isUnarmedClip(snap.pose.clipName())
            ? firstLoadedClip(pam, "die2", snap.pose.clipName())
            : firstLoadedClip(pam, "die", snap.pose.clipName());
        List<String> bits = particleParts(pam);
        Map<String, Boolean> vis = new HashMap<>();
        if (snap.pose.visibility() != null) {
            vis.putAll(snap.pose.visibility());
        }
        for (String part : bits) {
            vis.put(part, Boolean.FALSE);
        }
        hideInkButter(vis);
        String[] bodyHead = deathHeadParts(pam);
        if (bodyHead != null) {
            for (String part : bodyHead) {
                vis.put(part, Boolean.FALSE);
            }
        }
        AnimPose diePose = AnimPose.once(pam, dieClip, ZombieAnimRole.DIE, vis.isEmpty() ? null : vis);
        if (snap.pose.flipX()) {
            diePose = diePose.flipped();
        }
        boolean drown = FishermanAnim.isFishermanPam(pam);
        if (SnorkelerAnim.isSnorkelerPam(pam)) {
            SwimBehavior swim = (SwimBehavior) zombie.getBehavior(ZombieBehaviorType.SWIM);
            if (swim != null && (swim.isSubmerged() || swim.isSurfaced()) && swim.getRise() < 1f) {
                drown = true;
                DeathFx fx = new DeathFx(diePose, snap.x, snap.y, true);
                fx.snorkelRise = swim.getRise();
                fx.drownWaterY = SnorkelerAnim.waterLineY(layout, zombie.getGridY());
                deathFx.add(fx);
                return;
            }
        }
        if (drown) {
            deathFx.add(new DeathFx(diePose, snap.x, snap.y, true));
            return;
        }

        deathFx.add(new DeathFx(diePose, snap.x, snap.y, false));

        // The head lies on the ground until the body has finished collapsing, then both fade together.
        ClipRef dieRef = clips.getOrLoad(pam, dieClip);
        String headGroup = deathHeadGroup(pam);
        float hold = dieRef != null && (headGroup != null || popsHeadAndArm(pam))
            ? dieRef.duration : 0f;
        float dir = snap.backward ? -1f : 1f;
        if (headGroup != null && firstLoadedClip(pam, "particles", null) != null) {
            // Gargantuar/Imp: the clip is already just the head; drawPart would whitelist butter.
            // All-Star: {_particles} is default-hidden, so the clip must be drawn via drawPart.
            addLimbPop(pam, "particles", headGroup, snap.x, snap.y, 0f,
                randomHeadThrowDir(), HEAD_THROW_BACK_TILES, HEAD_THROW_HOP_TILES, hold,
                !isAllStar(pam));
            return;
        }
        for (int i = 0; i < bits.size(); i++) {
            String part = bits.get(i);
            if (lostHands.containsKey(zombie) && isArmPopPart(part)) {
                continue;
            }
            boolean head = isHeadParticlePart(part);
            float throwDir = head ? randomHeadThrowDir() : dir;
            float back = head ? HEAD_THROW_BACK_TILES : 0.1f + i * 0.1f;
            float hop = head ? HEAD_THROW_HOP_TILES : 0.85f + (i % 2) * 0.3f;
            addLimbPop(pam, "particles", part, snap.x, snap.y, 0f, throwDir, back, hop, hold, false);
        }
    }

    /**
     * Freeze the last live barrel parts on the orphan. Body still plays {@code die}.
     */
    private boolean attachBarrelLeftover(GameModel model, ZombieInstance zombie, LiveSnap snap) {
        if (zombie == null || zombie.getDefinition() == null
            || !BarrelRollerAnim.DEFINITION_NAME.equals(zombie.getDefinition().getName())) {
            return false;
        }
        if (snap == null || snap.pose == null || BarrelRollerAnim.isUnarmedClip(snap.pose.clipName())) {
            return false;
        }
        Barrel barrel = findOrphanBarrel(model, zombie);
        if (barrel == null) {
            return false;
        }
        clips.getOrLoad(snap.pose.pamPath(), snap.pose.clipName());
        AnimPose held = AnimPose.once(snap.pose.pamPath(), snap.pose.clipName(),
            ZombieAnimRole.IDLE, null);
        if (snap.pose.flipX()) {
            held = held.flipped();
        }
        lastCabinets.put(barrel, new LiveSnap(held, snap.x, snap.y, snap.backward, snap.time));
        return true;
    }

    private Barrel findOrphanBarrel(GameModel model, ZombieInstance zombie) {
        Barrel fromOrphans = matchBarrel(
            model == null ? null : model.getOrphanedPushables(), zombie);
        if (fromOrphans != null) {
            return fromOrphans;
        }
        for (Pushable item : lastCabinets.keySet()) {
            if (item instanceof Barrel barrel
                && !barrel.isDestroyed()
                && barrel.getPosition() != null) {
                return barrel;
            }
        }
        return null;
    }

    private static Barrel matchBarrel(Iterable<Pushable> items, ZombieInstance zombie) {
        if (items == null || zombie == null) {
            return null;
        }
        BarrelRollerBehavior roller = (BarrelRollerBehavior) zombie.getBehavior(
            ZombieBehaviorType.BARREL_ROLLER);
        int row = roller != null ? roller.getLastBarrelRow() : zombie.getGridY();
        int col = roller != null ? roller.getLastBarrelCol() : -1;
        Barrel fallback = null;
        for (Pushable item : items) {
            if (!(item instanceof Barrel barrel)
                || barrel.isDestroyed()
                || barrel.getPosition() == null) {
                continue;
            }
            if (col >= 0 && barrel.getRow() == row && barrel.getCol() == col) {
                return barrel;
            }
            if (barrel.getRow() == row) {
                fallback = barrel;
            }
        }
        return fallback;
    }

    /** Incineration PAM lives under EFFECTS and has clip {@code animation}, not {@code die}. */
    private boolean trySpawnAsh(ZombieInstance zombie, LiveSnap snap) {
        if (zombie == null || !zombie.isBlownUp() || catalog == null) {
            return false;
        }
        PamCatalog.PamEntry ash = catalog.byName(ashPamFor(zombie));
        if (ash == null) {
            return false;
        }
        String clip = catalog.resolveClip(ash, "animation");
        deathFx.add(new DeathFx(
            AnimPose.once(ash.path(), clip, ZombieAnimRole.DIE, null),
            snap.x, snap.y));
        return true;
    }

    static String ashPamFor(ZombieInstance zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            return ZOMBIE_ASH_PAM;
        }
        String name = zombie.getDefinition().getName();
        if ("ZombieLostCityJane".equals(name)) {
            return JANE_ASH_PAM;
        }
        if ("ZombieArcade".equals(name)
            || TroglobiteAnim.DEFINITION_NAME.equals(name)
            || OctopusAnim.DEFINITION_NAME.equals(name)) {
            return BIG_ASH_PAM;
        }
        ZombieSize size = zombie.getDefinition().getSize();
        if (size == ZombieSize.LARGE) {
            return GARGANTUAR_ASH_PAM;
        }
        if (size == ZombieSize.IMP) {
            return IMP_ASH_PAM;
        }
        return ZOMBIE_ASH_PAM;
    }

    private void maybeGargantuarWalkStomp(ZombieInstance zombie, AnimPose pose, float time) {
        if (screenShake == null || pose == null || pose.isSpritesheet()
                || !"walk".equals(pose.clipName())) {
            return;
        }
        if (!isGargantuar(pose.pamPath())) {
            return;
        }
        LiveSnap prev = lastLive.get(zombie);
        float prevTime = prev != null && "walk".equals(prev.pose.clipName()) ? prev.time : -1f;
        ClipRef walk = clips.getOrLoad(pose.pamPath(), pose.clipName());
        float duration = walk != null ? walk.duration : 0f;
        if (GargantuarAnim.crossedWalkStomp(prevTime, time, duration)) {
            screenShake.pulse();
        }
    }

    private static boolean isGargantuar(String pam) {
        return pam != null && pam.toUpperCase().contains("GARGANTUAR") && !pam.toUpperCase().contains("IMP");
    }

    private static boolean isImp(String pam) {
        return pam != null && pam.toUpperCase().contains("IMP");
    }

    private static boolean isAllStar(String pam) {
        return pam != null && pam.toUpperCase().contains("ALLSTAR");
    }

    private static boolean isArcadeZombie(String pam) {
        return pam != null && pam.toUpperCase().contains("ZOMBIE_80S_ARCADE");
    }

    private static boolean isPianoProp(String pam) {
        if (pam == null) {
            return false;
        }
        String upper = pam.toUpperCase();
        return upper.contains("/PIANO/PIANO")
            || (upper.endsWith("PIANO.PAM") && !upper.contains("ZOMBIE_PIANO"));
    }

    private static boolean isProspector(String pam) {
        if (pam == null) {
            return false;
        }
        String upper = pam.toUpperCase();
        return upper.contains("ZOMBIE_PROSPECTOR")
            && !upper.contains("BLAST")
            && !upper.contains("SMOKE");
    }

    private static boolean isIceAgeHunter(String pam) {
        return pam != null && pam.toUpperCase().contains("ZOMBIE_ICEAGE_HUNTER");
    }

    private static boolean popsHeadAndArm(String pam) {
        return isArcadeZombie(pam) || isProspector(pam) || isIceAgeHunter(pam);
    }

    /** {@code particles} group used for ground Y; the clip itself is drawn whole. */
    private static String deathHeadGroup(String pam) {
        if (isGargantuar(pam)) {
            return GARGANTUAR_HEAD;
        }
        if (isImp(pam)) {
            return IMP_HEAD;
        }
        if (isAllStar(pam)) {
            return ALLSTAR_PARTICLES;
        }
        return null;
    }

    /** Head pieces to hide on the {@code die} body so they aren't drawn twice. */
    private static String[] deathHeadParts(String pam) {
        if (isGargantuar(pam)) {
            return GARGANTUAR_HEAD_PARTS;
        }
        if (isImp(pam)) {
            return IMP_HEAD_PARTS;
        }
        if (isAllStar(pam)) {
            return ALLSTAR_HEAD_PARTS;
        }
        if (isIceAgeHunter(pam)) {
            return HUNTER_HEAD_PARTS;
        }
        if (popsHeadAndArm(pam)) {
            return ARCADE_HEAD_PARTS;
        }
        return null;
    }

    private String firstLoadedClip(String pam, String preferred, String fallback) {
        if (preferred == null) {
            return fallback;
        }
        return clips.getOrLoad(pam, preferred) != null ? preferred : fallback;
    }

    private static boolean egyptDeathParts(String pam) {
        String upper = pam.toUpperCase();
        return upper.contains("EGYPT") || upper.contains("EXPLORER");
    }

    /** Skull / jaw / outer arm on {@code particles}. Egypt uses biome-prefixed names. */
    private List<String> particleParts(String pam) {
        List<String> bits = new ArrayList<>();
        if (firstLoadedClip(pam, "particles", null) == null) {
            return bits;
        }
        if (popsHeadAndArm(pam)) {
            bits.addAll(List.of(ARCADE_PARTICLE_PARTS));
            return bits;
        }
        String[] names = deathHeadGroup(pam) != null ? new String[]{deathHeadGroup(pam)}
            : egyptDeathParts(pam)
            ? DEATH_PARTS_EGYPT : DEATH_PARTS;
        boolean particleHead = partDrawn(clips.getOrLoad(pam, "particles"), "particle_head");
        if (particleHead && deathHeadGroup(pam) == null) {
            bits.add("particle_head");
        }
        List<String> armParticles = particleArmParts(pam);
        bits.addAll(armParticles);
        boolean particleLimb = !armParticles.isEmpty();
        for (String part : names) {
            if (particleHead && (part.contains("skull") || part.contains("jaw"))) {
                continue;
            }
            if (particleLimb && isArmPopPart(part) && !isParticleLimb(part)) {
                continue;
            }
            if (partDrawn(clips.getOrLoad(pam, "particles"), part)) {
                bits.add(part);
            }
        }
        return bits;
    }

    /** Detached {@code particle_arm} (or {@code particle_hand} if that group is missing). */
    private List<String> particleArmParts(String pam) {
        List<String> arms = new ArrayList<>();
        List<String> hands = new ArrayList<>();
        ClipRef particles = clips.getOrLoad(pam, "particles");
        if (particles == null) {
            return arms;
        }
        for (String name : ARM_PARTICLE_NAMES) {
            if (!partDrawn(particles, name)) {
                continue;
            }
            if (isParticleHandPart(name)) {
                hands.add(name);
            } else {
                arms.add(name);
            }
        }
        return arms.isEmpty() ? hands : arms;
    }

    private boolean partDrawn(ClipRef clip, String name) {
        if (clip == null || name == null) {
            return false;
        }
        Rectangle[] frames = player.partBoundsByFrame(clip, name);
        if (frames == null) {
            return false;
        }
        for (Rectangle frame : frames) {
            if (frame != null) {
                return true;
            }
        }
        return false;
    }

    private String[] lostArmBodyParts(String pam) {
        if (pam == null) {
            return LOST_HAND_BODY_PARTS;
        }
        String[] cached = lostArmBodyByPam.get(pam);
        if (cached != null) {
            return cached.length == 0 ? LOST_HAND_BODY_PARTS : cached;
        }
        List<String> names = new ArrayList<>();
        collectLostArmBodyParts(player.getParts(pam), names);
        cached = names.toArray(String[]::new);
        lostArmBodyByPam.put(pam, cached);
        return cached.length == 0 ? LOST_HAND_BODY_PARTS : cached;
    }

    private static void collectLostArmBodyParts(PamPlayer.AnimationPart node, List<String> names) {
        if (node == null) {
            return;
        }
        if (isArmPopPart(node.name)) {
            names.add(node.name);
        }
        if (node.children == null) {
            return;
        }
        for (int i = 0; i < node.children.size(); i++) {
            Object child = node.children.get(i);
            if (child instanceof PamPlayer.AnimationPart part) {
                collectLostArmBodyParts(part, names);
            }
        }
    }

    /** {@code particles} part that is the detached head, thrown on a random parabola. */
    static boolean isHeadParticlePart(String part) {
        return "particle_head".equals(part)
            || ALLSTAR_PARTICLES.equals(part)
            || GARGANTUAR_HEAD.equals(part);
    }

    static boolean isHeadPopPart(String part) {
        return isHeadParticlePart(part)
            || (part != null && part.contains("skull"));
    }

    private static void hideInkButter(Map<String, Boolean> vis) {
        for (String part : INK_BUTTER_PARTS) {
            vis.put(part, Boolean.FALSE);
        }
    }

    private Map<String, Boolean> headPopVis(String part) {
        popVis.clear();
        hideInkButter(popVis);
        for (String hide : HEAD_POP_HIDE) {
            popVis.put(hide, Boolean.FALSE);
        }
        if (part != null) {
            popVis.put(part, Boolean.TRUE);
        }
        return popVis;
    }

    static boolean isParticleLimb(String part) {
        return isParticleArmPart(part) || isParticleHandPart(part);
    }

    static boolean isParticleArmPart(String part) {
        return part != null && part.startsWith("particle_arm");
    }

    static boolean isParticleHandPart(String part) {
        return part != null && part.startsWith("particle_hand");
    }

    static boolean isArmPopPart(String part) {
        if (part == null || part.contains("bone")) {
            return false;
        }
        return isParticleLimb(part)
            || part.contains("arm_outer")
            || part.contains("arms_outer")
            || part.contains("hand_outer");
    }

    static boolean isHandParticlePart(String part) {
        return isArmPopPart(part);
    }

    /** Body HP only — armor (cone, bucket) does not delay the arm drop. */
    static boolean atOrBelowHalfHp(ZombieInstance zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            return false;
        }
        int max = zombie.getDefinition().getBaseHP();
        return max > 0 && zombie.getCurrentHP() * 2 <= max;
    }

    /** +1 or −1 so the head flies toward the house or back toward the spawn. */
    static float randomHeadThrowDir() {
        return randomHeadThrowDir(ThreadLocalRandom.current());
    }

    static float randomHeadThrowDir(Random rng) {
        return rng.nextBoolean() ? 1f : -1f;
    }

    private void addLimbPop(String pam, String clip, String part,
                            float originX, float originY, float time,
                            float dir, float backTiles, float hopTiles, float hold,
                            boolean wholeClip) {
        float s = AnimScale.ZOMBIE;
        clips.getOrLoad(pam, clip);
        Rectangle bounds = player.partBounds(pam, clip, time, part);
        float groundY = originY - layout.cellHeight() * 0.5f;
        if (bounds != null) {
            groundY = originY - layout.cellHeight() * 0.5f + (bounds.y + bounds.height) * s;
        }
        float hopTime = 2f * hopTiles / -ARMOR_POP_GRAVITY;
        ArmorPop pop = new ArmorPop(
            pam, clip, wholeClip ? null : part,
            originX, originY, groundY,
            dir * backTiles * layout.cellWidth() / hopTime,
            hopTiles * layout.cellHeight(),
            ARMOR_POP_GRAVITY * layout.cellHeight());
        pop.hold = hold;
        pop.clipTime = time;
        if (isHeadParticlePart(part) || isArmPopPart(part)) {
            pop.maxBounces = 0;
        }
        armorPops.add(pop);
    }

    private void drawDeathFx(Batch batch, float delta, int row) {
        for (int i = deathFx.size() - 1; i >= 0; i--) {
            DeathFx fx = deathFx.get(i);
            if (layout.rowAt(fx.y) != row) {
                continue;
            }
            ClipRef ref = fx.pose.isSpritesheet()
                    ? null
                    : clips.getOrLoad(fx.pose.pamPath(), fx.pose.clipName());
            float hold = fx.pose.isSpritesheet()
                    ? (fx.holdSeconds > 0f ? fx.holdSeconds : 0.4f)
                    : fx.hold(ref);
            if (fx.time >= hold + ARMOR_POP_FADE) {
                deathFx.remove(i);
                continue;
            }
            if (!fx.pose.isSpritesheet() && ref == null) {
                fx.time += delta;
                continue;
            }
            // Hold the collapsed last frame and fade it out instead of popping it off-screen.
            float scale = AnimScale.forZombie(fx.pose) * fx.pose.scale();
            float alpha = 1f - Math.max(0f, fx.time - hold) / ARMOR_POP_FADE;
            float time = Math.min(fx.time, hold);
            batch.setColor(1f, 1f, 1f, alpha);
            if (fx.pose.isSpritesheet()) {
                drawPose(batch, fx, fx.pose, fx.x, fx.y, AnimScale.forZombie(fx.pose), NO_PHASE,
                        0f, 0f);
                batch.setColor(1f, 1f, 1f, 1f);
                fx.time += delta;
                continue;
            }
            if (fx.drown) {
                freezeDrownWaterY(fx, ref, scale);
            }
            Rectangle mask = fx.drown && !Float.isNaN(fx.drownWaterY)
                ? FishermanAnim.drownMaskWorld(layout, fx.x,
                SnorkelerAnim.isSnorkelerPam(fx.pose.pamPath())
                    ? FishermanAnim.rowAt(layout, fx.drownWaterY)
                    : FishermanAnim.rowAt(layout, fx.y),
                fx.drownWaterY)
                : null;
            Rectangle sprite = fx.drown
                ? FishermanAnim.spriteWorld(fx.x, fx.y,
                player.bounds(fx.pose.pamPath(), fx.pose.clipName()), scale, fx.pose.flipX())
                : null;
            boolean clipBody = mask != null && (sprite == null || FishermanAnim.overlaps(mask, sprite));
            if (clipBody) {
                drownShader().begin(batch, mask);
            }
            drawClip(batch, ref, fx.pose, time, fx.x, fx.y, scale);
            if (clipBody) {
                drownShader().end(batch);
            }
            float hit = fx.hitFlash / HIT_FLASH_SEC;
            overlayHitFlash(batch, hit * alpha,
                () -> drawClip(batch, ref, fx.pose, time, fx.x, fx.y, scale));
            batch.setColor(Color.WHITE);
            fx.hitFlash = Math.max(0f, fx.hitFlash - delta);
            fx.time += delta;
        }
    }

    private FishermanDrownShader drownShader() {
        if (drownShader == null) {
            drownShader = new FishermanDrownShader();
        }
        return drownShader;
    }

    private HitFlashShader hitFlashShader() {
        if (hitFlashShader == null) {
            hitFlashShader = new HitFlashShader();
        }
        return hitFlashShader;
    }

    private GlowGreenShader glowGreenShader() {
        if (glowGreenShader == null) {
            glowGreenShader = new GlowGreenShader();
        }
        return glowGreenShader;
    }

    private ChillBlueShader chillBlueShader() {
        if (chillBlueShader == null) {
            chillBlueShader = new ChillBlueShader();
        }
        return chillBlueShader;
    }

    private DangerRedShader dangerRedShader() {
        if (dangerRedShader == null) {
            dangerRedShader = new DangerRedShader();
        }
        return dangerRedShader;
    }

    /** Subtle green with a slow pulse flash. */
    static float glowStrength() {
        return glowStrength(System.nanoTime() * 1e-9);
    }

    static float glowStrength(double seconds) {
        float wave = 0.5f + 0.5f * (float) Math.sin(seconds * Math.PI * 2.0 * GLOW_HZ);
        return GLOW_BASE + GLOW_PULSE * wave;
    }

    private void drawSnorkelRipple(Batch batch, AnimPose pose, ZombieInstance zombie,
                                   float rippleX, float tileWaterY) {
        String rippleName = SnorkelerAnim.rippleName(zombie);
        PamCatalog.PamEntry entry = catalog == null ? null : catalog.byName(rippleName);
        String path = entry != null ? entry.path() : SnorkelerAnim.ripplePath(zombie);
        String clip = entry != null
            ? catalog.resolveClip(entry, SnorkelerAnim.RIPPLE_CLIP, "ripple_exit")
            : SnorkelerAnim.RIPPLE_CLIP;
        ClipRef ripple = clips.getOrLoad(path, clip);
        if (ripple == null && !snorkelRippleLoaded.contains(path)) {
            snorkelRippleLoaded.add(path);
            clips.preloadSync(path, clip);
            ripple = clips.getOrLoad(path, clip);
        }
        if (ripple == null) {
            return;
        }
        boolean gargantuar = zombie.getDefinition() != null
                && zombie.getDefinition().getSize() == ZombieSize.LARGE;
        float rippleScale = AnimScale.ZOMBIE * (gargantuar ? 1f : pose.scale());
        float anchorY = tileWaterY;
        if (gargantuar) {
            // Tile waterline is one cell low for the large unit; same anchor as normal zombies.
            anchorY += layout.cellHeight();
        }
        Rectangle clipBox = player.bounds(path, clip);
        float ry = SnorkelerAnim.rippleDrawY(anchorY, clipBox, rippleScale);
        player.draw(batch, ripple, snorkelRippleTime, rippleX, ry, rippleScale, rippleScale, true);
    }

    /** Ripple-only on shallow tiles after emerge; no foot mask (that hid the whole body). */
    private boolean shouldRippleOnWater(ZombieInstance zombie, GameModel model,
                                        SwimBehavior swim, JumpBehavior jump) {
        if (model == null || zombie == null || zombie.isDead()) {
            return false;
        }
        if (waterEmerges.containsKey(zombie)) {
            return false;
        }
        if (jump != null && jump.getPhase() == JumpBehavior.JumpPhase.JUMPING) {
            return false;
        }
        if (swim != null && (swim.isSubmerged() || swim.isSurfaced())) {
            return false;
        }
        return model.isWaterTile(zombie.getGridY(), zombie.getGridX());
    }

    private void freezeDrownWaterY(DeathFx fx, ClipRef die, float scale) {
        if (!Float.isNaN(fx.drownWaterY)) {
            return;
        }
        if (SnorkelerAnim.isSnorkelerPam(fx.pose.pamPath())) {
            return;
        }
        Rectangle tube = partAt(die, 0f, FishermanAnim.INNERTUBE_PART);
        if (tube == null) {
            tube = partAt(die, 0f, "zombie_innertube_layer");
        }
        if (tube == null) {
            tube = partAt(clips.getOrLoad(fx.pose.pamPath(), "idle"), 0f, FishermanAnim.INNERTUBE_PART);
        }
        if (tube != null) {
            fx.drownWaterY = FishermanAnim.waterY(fx.y, tube, scale);
        }
    }

    private static final class OneShotFx {
        final String pamPath;
        final String clipName;
        final float x;
        final float y;
        final float scale;
        final boolean loop;
        float time;
        float duration;
        boolean started;

        OneShotFx(String pamPath, String clipName, float x, float y, float scale, boolean loop) {
            this.pamPath = pamPath;
            this.clipName = clipName;
            this.x = x;
            this.y = y;
            this.scale = scale > 0f ? scale : 1f;
            this.loop = loop;
        }
    }

    private enum PlantFoodFxPhase {
        ON, LOOP, OFF
    }

    private static final class PlantFoodFx {
        PlantFoodFxPhase phase = PlantFoodFxPhase.ON;
        float time;
    }

    private void updateAndDrawPlantFoodFx(Batch batch, PlantInstance plant, float x, float y, float delta) {
        boolean active = plant.isPlantFoodActive() || plant.getState() == PlantState.PLANT_FOOD;
        PlantFoodFx fx = plantFoodFx.get(plant);
        if (!active && fx == null) {
            return;
        }
        if (fx == null) {
            fx = new PlantFoodFx();
            plantFoodFx.put(plant, fx);
            preloadPlantFoodFx();
        }
        if (!active && fx.phase != PlantFoodFxPhase.OFF) {
            fx.phase = PlantFoodFxPhase.OFF;
            fx.time = 0f;
        }

        String clip = plantFoodFxClip(fx.phase);
        ClipRef ref = clips.getOrLoad(EffectPamPaths.PLANTFOOD_FX, clip);
        if (ref == null) {
            if (!active) {
                plantFoodFx.remove(plant);
            }
            return;
        }
        float duration = effectClipDurationSeconds(ref, EffectPamPaths.PLANTFOOD_FX, clip);
        boolean loop = fx.phase == PlantFoodFxPhase.LOOP;
        fx.time += delta;
        player.draw(batch, ref, fx.time, x, y, AnimScale.PLANT, AnimScale.PLANT, loop);

        if (fx.phase == PlantFoodFxPhase.ON && duration > 0f && fx.time >= duration) {
            fx.phase = PlantFoodFxPhase.LOOP;
            fx.time = 0f;
        } else if (fx.phase == PlantFoodFxPhase.OFF && duration > 0f && fx.time >= duration) {
            plantFoodFx.remove(plant);
        } else if (fx.phase == PlantFoodFxPhase.OFF && duration <= 0f) {
            plantFoodFx.remove(plant);
        } else if (fx.phase == PlantFoodFxPhase.ON && duration <= 0f) {
            fx.phase = active ? PlantFoodFxPhase.LOOP : PlantFoodFxPhase.OFF;
            fx.time = 0f;
        }
    }

    private void preloadPlantFoodFx() {
        clips.getOrLoad(EffectPamPaths.PLANTFOOD_FX, EffectPamPaths.PLANTFOOD_FX_ON);
        clips.getOrLoad(EffectPamPaths.PLANTFOOD_FX, EffectPamPaths.PLANTFOOD_FX_LOOP);
        clips.getOrLoad(EffectPamPaths.PLANTFOOD_FX, EffectPamPaths.PLANTFOOD_FX_OFF);
    }

    private static String plantFoodFxClip(PlantFoodFxPhase phase) {
        return switch (phase) {
            case ON -> EffectPamPaths.PLANTFOOD_FX_ON;
            case LOOP -> EffectPamPaths.PLANTFOOD_FX_LOOP;
            case OFF -> EffectPamPaths.PLANTFOOD_FX_OFF;
        };
    }

    /** Per-sandstorm visual state: placement, clip choice and clocks. */
    private static final class SandstormFx {
        float startX;
        float targetX;
        float y;
        float x;
        float scale = -1f;
        float introDuration;
        float outroDuration;
        float clock;
        float outroClock;
        boolean landedSeen;
        boolean visible;
        boolean loop;
        float clipTime;
        String clip;
    }

    /** Per-ice-wind visual state: sweep endpoints and playback clock. */
    private static final class IceWindFx {
        float startX;
        float endX;
        float y;
        float x;
        float scale = -1f;
        float clock;
    }

    /** Per-slide-tile visual state: idle loop or active burst playback. */
    private static final class SlideTileFx {
        enum Phase { IDLE, ACTIVE_START, ACTIVE_END }

        Phase phase = Phase.IDLE;
        float clock;
        float scale = -1f;
    }
}
