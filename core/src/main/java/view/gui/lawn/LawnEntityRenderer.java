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
import model.zombie.behavior.zomboss.BeachZombossBehavior;
import model.zombie.behavior.zomboss.BeachZombossPendingShark;
import model.zombie.behavior.zomboss.DarkZombossBehavior;
import model.zombie.behavior.zomboss.EgyptZombossBehavior;
import model.zombie.behavior.zomboss.IceZombossBehavior;
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
import view.gui.anim.zombie.BeachZombossAnim;
import view.gui.anim.zombie.DarkZombossAnim;
import view.gui.anim.zombie.EgyptZombossAnim;
import view.gui.anim.zombie.IceZombossAnim;
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
import view.gui.audio.GameAudio;
import view.gui.audio.GameplayCombatSfx;
import view.gui.audio.GameSfx;
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

    final LawnPlantFxRenderer plantFx;
    final LawnPlantStatusFxRenderer plantStatus;
    final LawnPickupRenderer pickup;
    final LawnTerrainFxRenderer terrain;
    final LawnPropRenderer prop;
    final LawnZombossFxRenderer zomboss;
    final LawnZombossClock zombossClock;
    final LawnDeathFxRenderer deaths;
    final LawnDeathSpawn deathSpawn;
    final LawnZombieDrawRenderer zombieDraw;
    final LawnZombieSpecialDraw zombieSpecial;
    final LawnZombotanyDraw zombotany;
    final LawnOctopusDraw octopus;
    final LawnEntityFramePass framePass;

    enum EndMode { NONE, LOSE, WIN }

    final LawnLayout layout;

    final PlantAnimAdapter plantAdapter;

    final ZombieAnimAdapter zombieAdapter;

    final ProjectileAnimAdapter projectileAdapter;

    final PamClipCache clips;

    final SpritesheetClipCache sheetClips;

    final PlantSpritesheetCatalog plantSheets;

    final PamPlayer player;

    final PamCatalog catalog;

    final TextureBank textures;

    TextureRegion flowerPotRegion;

    final IdentityHashMap<Object, AnimClock> clocks = new IdentityHashMap<>();

    /** Stable clock identity for Zombotany plant-head overlays (separate from the body clock). */
    final IdentityHashMap<ZombieInstance, Object> zombotanyHeadClocks = new IdentityHashMap<>();

    final IdentityHashMap<ClipRef, ZombieFootfallCurve> footfalls = new IdentityHashMap<>();

    /** Left-edge canvas X of the pushing hand, one sample per push-clip frame. */
    final IdentityHashMap<ClipRef, float[]> arcadePushHandX = new IdentityHashMap<>();

    /** Crystal Skull / beam part names that actually exist on the loaded PAM. */
    String crystalSkullPart;

    String crystalBeamPart;

    final IdentityHashMap<Object, HitFlash> hitFlashes = new IdentityHashMap<>();

    /** Body HP has crossed half; {@code particle_arm} already hopped off. */
    final IdentityHashMap<ZombieInstance, Boolean> lostHands = new IdentityHashMap<>();

    /** Outer-arm part names on the live PAM, cached after the first half-HP pop. */
    final Map<String, String[]> lostArmBodyByPam = new HashMap<>();

    final IdentityHashMap<ZombieInstance, Chapter> artChapters = new IdentityHashMap<>();

    final List<ArmorPop> armorPops = new ArrayList<>();

    final List<DeathFx> deathFx = new ArrayList<>();

    final IdentityHashMap<ZombieInstance, LiveSnap> lastLive = new IdentityHashMap<>();

    final IdentityHashMap<Pushable, LiveSnap> lastCabinets = new IdentityHashMap<>();

    final IdentityHashMap<Cell, LiveSnap> lastTerrainIce = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, LiveSnap> lastPlants = new IdentityHashMap<>();

    final IdentityHashMap<Grave, LiveSnap> lastGraves = new IdentityHashMap<>();

    /** World-pixel skull alignment for a thrown Imp; lerped to 0 as it lands. */
    final IdentityHashMap<ZombieInstance, float[]> tossAlign = new IdentityHashMap<>();

    final List<BlastFx> prospectorBlasts = new ArrayList<>();

    final IdentityHashMap<ZombieInstance, Boolean> prospectorBlastSpawned = new IdentityHashMap<>();

    final List<BlastFx> hunterSplats = new ArrayList<>();

    final IdentityHashMap<ZombieInstance, Integer> hunterSplatSeq = new IdentityHashMap<>();

    /** World origin of a flying octopus at release (PAM canvas centre). */
    final IdentityHashMap<ShootBehavior.OctopusShot, float[]> octopusAlign = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, OctopusCoatFx> octopusCoats = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, LiveSnap> lastPlantIce = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, Float> plantIceIntro = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, Object> plantIceClocks = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, Object> plantChillClocks = new IdentityHashMap<>();

    final IdentityHashMap<ZombieInstance, Object> zombieChillClocks = new IdentityHashMap<>();

    final IdentityHashMap<ZombieInstance, Float> zombieDangerElapsed = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, SheepFx> sheepFx = new IdentityHashMap<>();

    final IdentityHashMap<Grave, Float> graveEmerge = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, Boolean> explosionSpawned = new IdentityHashMap<>();

    final IdentityHashMap<ZombieInstance, Boolean> jalapenoFireSpawned = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, Integer> armorBreakFxEpoch = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, Integer> meleeAttackFxEpoch = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, Boolean> meleePlantFoodFxSpawned = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, OneShotFx> meleeIdlePulses = new IdentityHashMap<>();

    final IdentityHashMap<PlantInstance, PlantFoodFx> plantFoodFx = new IdentityHashMap<>();

    final IdentityHashMap<Cell, FireTileFx> fireTileFx = new IdentityHashMap<>();

    boolean fireImpactPamReady;

    final IdentityHashMap<PlantInstance, float[]> deathBlastSeen = new IdentityHashMap<>();

    final List<OneShotFx> backEffects = new ArrayList<>();

    final List<OneShotFx> frontEffects = new ArrayList<>();

    /** Egypt sandstorms in flight, keyed by their model record. */
    final IdentityHashMap<SandstormSpawn, SandstormFx> sandstorms = new IdentityHashMap<>();

    /** Zombies spawned under a still-fading sandstorm; hidden until the outro ends. */
    final Set<ZombieInstance> sandstormConcealed =
            Collections.newSetFromMap(new IdentityHashMap<>());

    /** The sandstorm PAM was force-loaded once so intro/loop/outro timings are known. */
    boolean sandstormPamReady;

    /** Frostbite Caves ice winds in flight, keyed by their model record. */
    final IdentityHashMap<IceWindGust, IceWindFx> iceWinds = new IdentityHashMap<>();

    /** The chill-wind PAM was force-loaded once so the sweep can start instantly. */
    boolean iceWindPamReady;

    /** Frostbite slide tiles keyed by their cell; idle loop + active bursts. */
    final IdentityHashMap<Cell, SlideTileFx> slideTiles = new IdentityHashMap<>();

    /** Both slider PAMs were force-loaded once so activations play instantly. */
    boolean slideTilePamReady;

    /** Slides still gliding between lanes, keyed by their zombie. */
    final IdentityHashMap<ZombieInstance, LaneSlide> laneGlides =
            new IdentityHashMap<>();

    /** Low-tide ambushes still surfacing, keyed by their zombie. */
    final IdentityHashMap<ZombieInstance, WaterEmerge> waterEmerges =
            new IdentityHashMap<>();

    final List<SunFlight> sunFlights = new ArrayList<>();

    final List<PlantFoodFlight> plantFoodFlights = new ArrayList<>();

    final List<LootFlight> lootFlights = new ArrayList<>();

    final Set<Object> seenThisFrame =
        Collections.newSetFromMap(new IdentityHashMap<>());

    final float[] xyTmp = new float[3];

    final Matrix4 batchTransform = new Matrix4();

    final Matrix4 popTransform = new Matrix4();

    final Map<String, Boolean> popVis = new HashMap<>();

    List<BowlingWalnut> bowlingWalnuts = List.of();

    final Map<String, Float> vaseAge = new HashMap<>();

    final IdentityHashMap<PlantInstance, BeghouledMotion> beghouledMotion = new IdentityHashMap<>();

    static final float BEGHOULED_MOVE_SEC = 0.22f;

    TextureRegion craterRegion;

    final DebugEntityOverlay entityOverlay;

    FishermanDrownShader drownShader;

    HitFlashShader hitFlashShader;

    GlowGreenShader glowGreenShader;

    ChillBlueShader chillBlueShader;

    DangerRedShader dangerRedShader;

    float snorkelRippleTime;

    final Set<String> snorkelRippleLoaded = new HashSet<>();

    ScreenShake screenShake;

    LawnMowerRenderer mowerRenderer;

    // --- End-level lose/win FX (clocks tick via {@link #tickEndLevel}, not world delta) ---
    static final float END_FADE_SEC = 0.75f;

    EndMode endMode = EndMode.NONE;

    float endFade;

    Texture whitePixel;

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
        this.plantFx = new LawnPlantFxRenderer(this);
        this.plantStatus = new LawnPlantStatusFxRenderer(this);
        this.pickup = new LawnPickupRenderer(this);
        this.terrain = new LawnTerrainFxRenderer(this);
        this.prop = new LawnPropRenderer(this);
        this.zomboss = new LawnZombossFxRenderer(this);
        this.zombossClock = new LawnZombossClock(this);
        this.deaths = new LawnDeathFxRenderer(this);
        this.deathSpawn = new LawnDeathSpawn(this);
        this.zombieDraw = new LawnZombieDrawRenderer(this);
        this.zombieSpecial = new LawnZombieSpecialDraw(this);
        this.zombotany = new LawnZombotanyDraw(this);
        this.octopus = new LawnOctopusDraw(this);
        this.framePass = new LawnEntityFramePass(this);
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


    /** Advances lose/win black fade (world PAM keeps ticking separately). */
    public void tickEndLevel(float delta) {
        if (endMode == EndMode.NONE || delta <= 0f) {
            return;
        }
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


    void drawEndLevel(Batch batch, GameModel model) {
        if (endMode == EndMode.NONE) {
            return;
        }
        drawBlackFade(batch, endFade);
    }


    void drawBlackFade(Batch batch, float alpha) {
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


    Texture whitePixel() {
        if (whitePixel == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            whitePixel = new Texture(pm);
            pm.dispose();
        }
        return whitePixel;
    }


    Chapter artChapterFor(ZombieInstance zombie, Chapter lawn) {
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


    boolean zombieWorldCenter(ZombieInstance zombie, float[] out) {
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


    static boolean drawsAboveLawn(ZombieInstance zombie) {
        return zombie != null && zombie.hasBehavior(ZombieBehaviorType.ZOMBOSS);
    }


    static int clampRow(int row, int rows) {
        if (rows <= 0 || row < 0) {
            return 0;
        }
        return Math.min(rows - 1, row);
    }


    float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, pose.cacheKey(), 0f, 0f, 0f);
    }


    float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta,
                          String clockKey) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, clockKey, 0f, 0f, 0f);
    }


    float drawPose(Batch batch, Object entity, AnimPose pose,
                           float x, float y, float baseScale, float phase, float flash, float delta,
                           float glow) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, pose.cacheKey(), glow, 0f, 0f);
    }


    float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta,
                          String clockKey, float glow) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, clockKey, glow, 0f, 0f);
    }


    float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta,
                          String clockKey, float glow, float chill) {
        return drawPose(batch, entity, pose, x, y, baseScale, phase, flash, delta, clockKey, glow, chill, 0f);
    }


    float drawPose(Batch batch, Object entity, AnimPose pose,
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


    float drawSheetPose(Batch batch, Object entity, AnimPose pose,
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


    void drawClip(Batch batch, ClipRef ref, AnimPose pose,
                          float stateTime, float x, float y, float scale) {
        float sx = pose.flipX() ? -scale : scale;
        if (pose.visibility() == null) {
            player.draw(batch, ref, stateTime, x, y, sx, scale, pose.loop());
        } else {
            player.draw(batch, ref, stateTime, x, y, sx, scale, pose.loop(), pose.visibility());
        }
    }


    void drawSheet(Batch batch, SpritesheetClipCache.SheetAnim sheet, AnimPose pose,
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
    float tickHitFlash(ZombieInstance zombie, float delta) {
        return tickHitFlash(zombie, vitality(zombie), delta);
    }


    float tickHitFlash(Object entity, int vitality, float delta) {
        HitFlash flash = hitFlashes.get(entity);
        if (flash == null) {
            flash = new HitFlash();
            flash.vitality = vitality;
            hitFlashes.put(entity, flash);
        } else {
            boolean chewGate = entity instanceof PlantInstance;
            if (shouldRestartHitFlash(flash.vitality, vitality, flash.remaining,
                chewGate ? flash.quiet : 0f)) {
                flash.remaining = LawnEntityDrawConstants.HIT_FLASH_SEC;
                if (chewGate) {
                    flash.quiet = LawnEntityDrawConstants.CHEW_FLASH_COOLDOWN;
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
        float strength = flash.remaining / LawnEntityDrawConstants.HIT_FLASH_SEC;
        flash.remaining -= delta;
        return strength;
    }


    /** Chew ticks are 1 HP; peas restart immediately. Plants also wait {@code quiet}. */
    static boolean shouldRestartHitFlash(int prevHp, int hp, float remaining, float quiet) {
        if (hp >= prevHp) {
            return false;
        }
        if (prevHp - hp >= LawnEntityDrawConstants.HIT_FLASH_CHUNK) {
            return true;
        }
        return remaining <= 0f && quiet <= 0f;
    }


    void overlayHitFlash(Batch batch, float flash, Runnable draw) {
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


    static int vitality(ZombieInstance zombie) {
        int hp = zombie.getCurrentHP();
        List<Armor> armors = zombie.getArmors();
        if (armors != null) {
            for (Armor armor : armors) {
                hp += armor.getCurrentHealth();
            }
        }
        return hp;
    }


    float advanceClock(Object entity, String clipKey, float delta) {
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
    void stampClockClip(Object entity, String clipKey) {
        AnimClock clock = clockFor(entity);
        if (!clipKey.equals(clock.clipKey)) {
            clock.clipKey = clipKey;
            clock.time = 0f;
        }
    }


    AnimClock clockFor(Object entity) {
        AnimClock clock = clocks.get(entity);
        if (clock == null) {
            clock = new AnimClock();
            clocks.put(entity, clock);
        }
        return clock;
    }


    FishermanDrownShader drownShader() {
        if (drownShader == null) {
            drownShader = new FishermanDrownShader();
        }
        return drownShader;
    }


    HitFlashShader hitFlashShader() {
        if (hitFlashShader == null) {
            hitFlashShader = new HitFlashShader();
        }
        return hitFlashShader;
    }


    GlowGreenShader glowGreenShader() {
        if (glowGreenShader == null) {
            glowGreenShader = new GlowGreenShader();
        }
        return glowGreenShader;
    }


    ChillBlueShader chillBlueShader() {
        if (chillBlueShader == null) {
            chillBlueShader = new ChillBlueShader();
        }
        return chillBlueShader;
    }


    DangerRedShader dangerRedShader() {
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
        float wave = 0.5f + 0.5f * (float) Math.sin(
                seconds * Math.PI * 2.0 * LawnEntityDrawConstants.GLOW_HZ);
        return LawnEntityDrawConstants.GLOW_BASE + LawnEntityDrawConstants.GLOW_PULSE * wave;
    }



    public void draw(Batch batch, GameModel model, float delta) {
        framePass.draw(batch, model, delta);
    }

    public void preloadVases() {
        prop.preloadVases();
    }

    public void preloadCraters() {
        terrain.preloadCraters();
    }

    public void playVaseBreak(String pamPath, int col, int row) {
        prop.playVaseBreak(pamPath, col, row);
    }

    public void drawPlantIdle(Batch batch, String plantName, float x, float y, float time) {
        plantFx.drawPlantIdle(batch, plantName, x, y, time);
    }

    public void drawPlantIdle(Batch batch, String plantName, float x, float y, float time, float scale) {
        plantFx.drawPlantIdle(batch, plantName, x, y, time, scale);
    }

    public void preloadPlantIdle(String plantName) {
        plantFx.preloadPlantIdle(plantName);
    }

    public void drawZombieIdle(Batch batch, String zombieName, float x, float y, float time,
                               Chapter chapter) {
        zombieDraw.drawZombieIdle(batch, zombieName, x, y, time, chapter);
    }

    public void preloadZombieIdle(String zombieName, Chapter chapter) {
        zombieDraw.preloadZombieIdle(zombieName, chapter);
    }

    public Sun pickSun(GameModel model, float worldX, float worldY) {
        return pickup.pickSun(model, worldX, worldY);
    }

    public void writeSunDrawPos(Sun sun, float[] out) {
        pickup.writeSunDrawPos(sun, out);
    }

    public void startSunCollect(Sun sun, float x0, float y0, float x1, float y1) {
        pickup.startSunCollect(sun, x0, y0, x1, y1);
    }

    public PlantFoodPickup pickPlantFood(GameModel model, float worldX, float worldY) {
        return pickup.pickPlantFood(model, worldX, worldY);
    }

    public void writePlantFoodDrawPos(PlantFoodPickup food, float[] out) {
        pickup.writePlantFoodDrawPos(food, out);
    }

    public void startPlantFoodCollect(PlantFoodPickup food, float x0, float y0, float x1, float y1) {
        pickup.startPlantFoodCollect(food, x0, y0, x1, y1);
    }

    public void writeLootDrawPos(LootPickup loot, float[] out) {
        pickup.writeLootDrawPos(loot, out);
    }

    public void startLootCollect(LootPickup loot, float x0, float y0, float x1, float y1,
                                 Runnable onComplete) {
        pickup.startLootCollect(loot, x0, y0, x1, y1, onComplete);
    }

    public void drainPendingLootFlights() {
        pickup.drainPendingLootFlights();
    }

    static String sunPam(Sun sun) {
        return LawnPickupRenderer.sunPam(sun);
    }

    static float sunScale(Sun sun) {
        return LawnPickupRenderer.sunScale(sun);
    }

    static String[] preferredClips(Sun sun) {
        return LawnPickupRenderer.preferredClips(sun);
    }

    static String sunClip(Sun sun) {
        return LawnPickupRenderer.sunClip(sun);
    }

    static String ashPamFor(ZombieInstance zombie) {
        return LawnDeathPam.ashPamFor(zombie);
    }

    static boolean isHeadParticlePart(String part) {
        return LawnDeathPam.isHeadParticlePart(part);
    }

    static boolean isHeadPopPart(String part) {
        return LawnDeathPam.isHeadPopPart(part);
    }

    static boolean isParticleLimb(String part) {
        return LawnDeathPam.isParticleLimb(part);
    }

    static boolean isParticleArmPart(String part) {
        return LawnDeathPam.isParticleArmPart(part);
    }

    static boolean isParticleHandPart(String part) {
        return LawnDeathPam.isParticleHandPart(part);
    }

    static boolean isArmPopPart(String part) {
        return LawnDeathPam.isArmPopPart(part);
    }

    static boolean isHandParticlePart(String part) {
        return LawnDeathPam.isHandParticlePart(part);
    }

    static boolean atOrBelowHalfHp(ZombieInstance zombie) {
        return LawnDeathPam.atOrBelowHalfHp(zombie);
    }

    static float randomHeadThrowDir() {
        return LawnDeathPam.randomHeadThrowDir();
    }

    static float randomHeadThrowDir(Random rng) {
        return LawnDeathPam.randomHeadThrowDir(rng);
    }

}
