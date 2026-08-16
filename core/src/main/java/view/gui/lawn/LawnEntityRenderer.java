package view.gui.lawn;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import model.app.App;
import model.enums.Chapter;
import model.enums.PlacableLayer;
import model.enums.ZombieBehaviorType;
import model.game.core.GameModel;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.GameMap;
import model.game.map.Point;
import model.plant.instance.PlantInstance;
import model.item.Grave;
import model.item.Sun;
import model.enums.GroundType;
import model.game.map.terrain.IceTerrainStrategy;
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
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.AnimPose;
import view.gui.anim.AnimScale;
import view.gui.anim.GraveAnim;
import view.gui.anim.PamClipCache;
import view.gui.anim.plant.PlantAnimAdapter;
import view.gui.anim.zombie.BarrelRollerAnim;
import view.gui.anim.zombie.DarkKingAnim;
import view.gui.anim.zombie.FishermanAnim;
import view.gui.anim.zombie.GargantuarAnim;
import view.gui.anim.zombie.HunterAnim;
import view.gui.anim.zombie.JugglerAnim;
import view.gui.anim.zombie.OctopusAnim;
import view.gui.anim.zombie.SnorkelerAnim;
import view.gui.anim.zombie.TroglobiteAnim;
import view.gui.anim.zombie.WizardAnim;
import view.gui.anim.zombie.ZombieAnimAdapter;
import view.gui.anim.zombie.ZombieAnimRole;
import view.gui.anim.zombie.ZombieFootfallCurve;
import view.gui.anim.zombie.ZombieGait;
import view.gui.anim.zombie.ZombieGaitProfiles;
import view.gui.assets.PamCatalog;
import view.gui.assets.PvzAssets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Draws plants and zombies on the lawn via libPVZ PAM clips.
 *
 * <p>Pipeline: model entity → {@link PlantAnimAdapter} / {@link ZombieAnimAdapter}
 * → {@link AnimPose} → {@link PamClipCache} → {@code PamPlayer.draw}.
 *
 * <p>TODO: projectiles, plant-food FX, mowers, and grid props.
 * TODO: sort draw order by row (back → front) then Y within a lane.
 * TODO: freeze overlays from model status flags.
 */
public final class LawnEntityRenderer {
    private static final float NO_PHASE = -1f;
    private static final float HIT_FLASH_SEC = 0.12f;
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
    /** Lost City Jane fire/explosion death — clip name is {@code animation}, not {@code die}. */
    private static final String JANE_ASH_PAM = "ZOMBIE_LOSTCITY_JANE_ASH";
    /** Crystal Skull laser — EFFECTS PAM, clip {@code laser_beam}. */
    private static final String CRYSTALSKULL_BEAM_PAM = "CRYSTALSKULL_BEAM";
    /** Lawn collectible — EFFECTS PAM. Yellow/normal is clip {@code animation}. */
    private static final String SUN_PAM = "SUN";
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
    private final PamClipCache clips;
    private final PamPlayer player;
    private final PamCatalog catalog;

    private final IdentityHashMap<Object, AnimClock> clocks = new IdentityHashMap<>();
    private final IdentityHashMap<ClipRef, ZombieFootfallCurve> footfalls = new IdentityHashMap<>();
    /** Left-edge canvas X of the pushing hand, one sample per push-clip frame. */
    private final IdentityHashMap<ClipRef, float[]> arcadePushHandX = new IdentityHashMap<>();
    /** Crystal Skull / beam part names that actually exist on the loaded PAM. */
    private String crystalSkullPart;
    private String crystalBeamPart;
    private final IdentityHashMap<ZombieInstance, HitFlash> hitFlashes = new IdentityHashMap<>();
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
    /** World-pixel skull alignment for a thrown Imp; lerped to 0 as it lands. */
    private final IdentityHashMap<ZombieInstance, float[]> tossAlign = new IdentityHashMap<>();
    private final List<BlastFx> prospectorBlasts = new ArrayList<>();
    private final IdentityHashMap<ZombieInstance, Boolean> prospectorBlastSpawned = new IdentityHashMap<>();
    private final List<BlastFx> hunterSplats = new ArrayList<>();
    private final IdentityHashMap<ZombieInstance, Integer> hunterSplatSeq = new IdentityHashMap<>();
    /** World origin of a flying octopus at release (PAM canvas centre). */
    private final IdentityHashMap<ShootBehavior.OctopusShot, float[]> octopusAlign = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, OctopusCoatFx> octopusCoats = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, SheepFx> sheepFx = new IdentityHashMap<>();
    private final IdentityHashMap<Grave, Float> graveEmerge = new IdentityHashMap<>();
    private final Set<Object> seenThisFrame = new HashSet<>();
    private final float[] xyTmp = new float[3];
    private final Matrix4 batchTransform = new Matrix4();
    private final Matrix4 popTransform = new Matrix4();
    private final Map<String, Boolean> popVis = new HashMap<>();

    private final DebugEntityOverlay entityOverlay;
    private FishermanDrownShader drownShader;
    private float snorkelRippleTime;
    private boolean snorkelRippleLoaded;
    private ScreenShake screenShake;

    public LawnEntityRenderer(PvzAssets assets, LawnLayout layout, DebugEntityOverlay entityOverlay) {
        this(assets, layout,
                new PlantAnimAdapter(assets.pamCatalog),
                new ZombieAnimAdapter(assets.pamCatalog),
                entityOverlay);
    }

    public LawnEntityRenderer(PvzAssets assets, LawnLayout layout,
                              PlantAnimAdapter plantAdapter, ZombieAnimAdapter zombieAdapter,
                              DebugEntityOverlay entityOverlay) {
        this.layout = layout;
        this.plantAdapter = plantAdapter;
        this.zombieAdapter = zombieAdapter;
        this.player = assets.player;
        this.clips = new PamClipCache(assets.player);
        this.catalog = assets.pamCatalog;
        this.entityOverlay = entityOverlay;
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
        this(assets, layout, new PlantAnimAdapter(catalog), new ZombieAnimAdapter(catalog), entityOverlay);
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

        drawGraves(batch, model, delta);
        for (PlantInstance plant : model.getAllPlants()) {
            drawPlant(batch, plant, delta);
        }
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
        for (Pushable cabinet : liveCabinets) {
            if (cabinet instanceof Piano) {
                drawPiano(batch, cabinet, delta);
            } else if (cabinet instanceof Barrel) {
                drawBarrel(batch, cabinet, delta);
            } else if (cabinet instanceof IceBlock) {
                drawIceBlock(batch, model, cabinet, delta);
            } else {
                drawCabinet(batch, cabinet, delta);
            }
        }
        drawTerrainIce(batch, model, delta);
        for (ZombieInstance zombie : model.getZombies()) {
            Chapter skin = artChapterFor(zombie, model.getChapter());
            drawZombie(batch, zombie, skin, delta);
        }
        drawOctopi(batch, model, delta);
        drawHunterSplats(batch, delta);
        drawProspectorBlasts(batch, delta);
        drawDeathFx(batch, delta);
        drawArmorPops(batch, delta);
        drawSuns(batch, model, delta);

        clocks.keySet().removeIf(key -> !seenThisFrame.contains(key));
        graveEmerge.keySet().removeIf(grave -> !seenThisFrame.contains(grave));
        sheepFx.keySet().removeIf(plant -> !seenThisFrame.contains(plant)
                && !plant.isTransformed());
        hitFlashes.keySet().removeIf(key -> !seenThisFrame.contains(key));
        lostHands.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        tossAlign.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        prospectorBlastSpawned.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        hunterSplatSeq.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        octopusAlign.keySet().removeIf(shot -> !shot.isFlying());
        Set<ZombieInstance> keepArt = new HashSet<>(model.getZombies());
        collectIcedOccupants(model, keepArt);
        artChapters.keySet().removeIf(zombie -> !keepArt.contains(zombie));
    }

    private void drawPlant(Batch batch, PlantInstance plant, float delta) {
        if (drawWizardSheep(batch, plant, delta)) {
            return;
        }
        Point pos = plant.getPosition();
        if (pos == null) {
            entityOverlay.drawPlant(batch, App.getInstance().getCurrentGameModel(), plant);
            return;
        }
        AnimPose pose = plantAdapter.poseFor(plant);
        if (pose == null) {
            entityOverlay.drawPlant(batch, App.getInstance().getCurrentGameModel(), plant);
            return;
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        drawPose(batch, plant, pose, xy[0], xy[1], AnimScale.PLANT, NO_PHASE, 0f, delta);
    }

    private void drawGraves(Batch batch, GameModel model, float delta) {
        GameMap map = model.getMap();
        if (map == null || catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(GraveAnim.PAM);
        if (entry == null) {
            return;
        }
        String path = entry.path();
        int rows = map.getRows();
        int cols = map.getCols();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Cell cell = map.getCell(col, row);
                if (cell == null) {
                    continue;
                }
                if (cell.getPlaceable(PlacableLayer.GROUND) instanceof Grave grave
                        && !grave.isDestroyed()) {
                    drawGrave(batch, grave, row, col, path, delta);
                }
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
        drawSquashStretch(batch, ref, 0f, xy[0], xy[1], AnimScale.PLANT, u, false);
    }

    private float tickGraveEmerge(Grave grave, float delta) {
        float u = graveEmerge.getOrDefault(grave, 0f);
        graveEmerge.put(grave, Math.min(1f, u + delta / GraveAnim.EMERGE_DURATION));
        return u;
    }

    /** Tomb pop (and wizard plant vanish/emerge). {@code u} 0 is pancake, 1 is rest. */
    private void drawSquashStretch(Batch batch, ClipRef ref, float time,
                                   float x, float y, float baseScale, float u, boolean loop) {
        float sxN = GraveAnim.scaleX(u);
        float syN = GraveAnim.scaleY(u);
        float yPin = y + (syN - 1f) * baseScale * (GraveAnim.CANVAS * 0.5f);
        player.draw(batch, ref, time, x, yPin, baseScale * sxN, baseScale * syN, loop);
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
        Point pos = plant.getPosition();
        if (pos == null) {
            sheepFx.remove(plant);
            return false;
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        preloadWizardSheepening();
        switch (fx.phase) {
            case VANISH -> {
                if (drawPlantPop(batch, plant, xy[0], xy[1], 1f - popU(fx.time))) {
                    fx.time += delta;
                    if (fx.time >= GraveAnim.EMERGE_DURATION) {
                        fx.phase = SheepPhase.APPEAR;
                        fx.time = 0f;
                    }
                    return true;
                }
                fx.phase = SheepPhase.APPEAR;
                fx.time = 0f;
                return drawSheepening(batch, xy[0], xy[1], fx, delta);
            }
            case APPEAR, IDLE, LEAVE -> {
                return drawSheepening(batch, xy[0], xy[1], fx, delta);
            }
            case EMERGE -> {
                if (drawPlantPop(batch, plant, xy[0], xy[1], popU(fx.time))) {
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
                                 float x, float y, float u) {
        AnimPose pose = plantAdapter.poseFor(plant);
        if (pose == null) {
            return false;
        }
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return false;
        }
        drawSquashStretch(batch, ref, 0f, x, y, AnimScale.PLANT * pose.scale(), u, pose.loop());
        return true;
    }

    private boolean drawSheepening(Batch batch, float x, float y, SheepFx fx, float delta) {
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
        IdentityHashMap<Sun, StealSunBehavior.SunPull> pulled = new IdentityHashMap<>();
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
                pulled.put(sun, pull);
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
            return;
        }
        for (Sun sun : tokens) {
            if (pulled.containsKey(sun)) {
                continue;
            }
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
            drawSunToken(batch, sun, x, y, delta);
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
        return layout.centerOf(Math.round(sun.getOriginY()), sun.getOriginX());
    }

    private void drawSunToken(Batch batch, Sun sun, float x, float y, float delta) {
        if (catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.byName(SUN_PAM);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, sunClip(sun), "animation");
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        drawPose(batch, sun, pose, x, y, AnimScale.SUN, NO_PHASE, 0f, delta);
    }

    private static String sunClip(Sun sun) {
        if (sun == null || sun.getType() == null) {
            return "animation";
        }
        return switch (sun.getType()) {
            case SPECIAL -> "red";
            case RADIOACTIVE -> "blue";
            default -> "animation";
        };
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
        float time = drawPose(batch, cabinet, pose, x, xy[1], AnimScale.PLANT, NO_PHASE, 0f, delta);
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
        float time = drawPose(batch, ice, pose, x, xy[1], AnimScale.ZOMBIE, NO_PHASE, 0f, delta);
        lastCabinets.put(ice, new LiveSnap(pose, x, xy[1], false, time));
    }

    /**
     * Frostbite ice tiles: occupant {@code idle} behind {@link TroglobiteAnim#ICE_PAM}.
     */
    private void drawTerrainIce(Batch batch, GameModel model, float delta) {
        GameMap map = model.getMap();
        if (map == null || catalog == null) {
            return;
        }
        Set<Cell> live = new HashSet<>();
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
        for (Cell cell : live) {
            drawTerrainIceCell(batch, model, cell, delta);
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
        float time = drawPose(batch, cell, pose, xy[0], xy[1], AnimScale.ZOMBIE, NO_PHASE, 0f, delta);
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
        drawPose(batch, zombie, pose, x, y, AnimScale.ZOMBIE, NO_PHASE, 0f, delta);
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
        float time = drawPose(batch, piano, pose, x, y, AnimScale.ZOMBIE, NO_PHASE, 0f, delta);
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
            drawBarrelParts(batch, leftover);
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
        float time = drawPose(batch, barrel, pose, xy[0], xy[1], AnimScale.ZOMBIE, NO_PHASE, 0f, delta);
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
    private void drawBarrelParts(Batch batch, LiveSnap leftover) {
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
        deathFx.add(new DeathFx(
                AnimPose.once(pam, clip, ZombieAnimRole.DIE, snap.pose.visibility()),
                snap.x, snap.y));
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
        deathFx.add(new DeathFx(
                AnimPose.once(entry.path(), clip, ZombieAnimRole.DIE, null),
                snap.x, snap.y));
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
        deathFx.add(new DeathFx(
                AnimPose.once(entry.path(), clip, ZombieAnimRole.DIE, null),
                x, y));
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
        if (!zombieWorldCenter(zombie, xyTmp)) {
            entityOverlay.drawZombie(batch, App.getInstance().getCurrentGameModel(), zombie);
            return;
        }

        restartArcadePushClock(zombie, pose);
        restartProspectorJumpClock(zombie, pose);
        restartTombRaiseClock(zombie, pose);
        restartDodoFlyClock(zombie, pose);
        restartHunterThrowClock(zombie, pose);
        restartJugglerSpinClock(zombie, pose);
        restartOctopusTossClock(zombie, pose);
        restartFishermanClock(zombie, pose);
        restartDarkKingClock(zombie, pose);
        restartWizardSheepClock(zombie, pose);
        spawnHunterSplat(zombie);

        float x = xyTmp[0];
        float y = xyTmp[1];
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
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref != null && jump != null && jump.getPhase() == JumpBehavior.JumpPhase.JUMPING
                && ref.duration > 0f) {
            // Fuse hit → blastoff this frame. Do not let the gait walk clock finish first.
            phase = prospectorJumpPhase(jump, pose.clipName(), ref.duration);
        } else if (ref != null && gait.enabled() && ZombieAnimAdapter.isDistanceDriven(zombie, pose)) {
            // Walking is driven by travel, so a cycle always covers exactly one step and
            // ground_swatch can be held still. Every other pose stays on the wall clock.
            // Hypnotized zombies walk the other way, so distance and the hold-back both flip.
            boolean backward = zombie.isMovingBackward();
            phase = gait.phaseAt(backward ? xyTmp[2] : -xyTmp[2]);
            float holdBack = gait.footLockOffsetTiles(phase, footfallFor(gait, ref)) * layout.cellWidth();
            x += backward ? -holdBack : holdBack;
        }
        float standY = y;
        Rectangle snorkelMask = null;
        float snorkelWaterY = Float.NaN;
        SwimBehavior swim = SnorkelerAnim.isSnorkelerPam(pose.pamPath())
                ? (SwimBehavior) zombie.getBehavior(ZombieBehaviorType.SWIM)
                : null;
        if (swim != null && (swim.isSubmerged() || swim.isSurfaced()) && swim.getRise() < 1f - 1e-3f) {
            float scale = AnimScale.ZOMBIE * pose.scale();
            float measureT = phase >= 0f && ref != null ? phase * ref.duration : 0f;
            Rectangle skull = ref != null ? partAt(ref, measureT, SnorkelerAnim.SKULL_PART) : null;
            if (skull == null) {
                skull = partAt(clips.getOrLoad(pose.pamPath(), "walk"), 0f, SnorkelerAnim.SKULL_PART);
            }
            snorkelWaterY = SnorkelerAnim.waterLineY(layout, zombie.getGridY());
            y = SnorkelerAnim.drawOriginY(standY, snorkelWaterY, skull, scale, swim.getRise());
            snorkelMask = FishermanAnim.drownMaskWorld(layout, x, zombie.getGridY(), snorkelWaterY);
        }
        if (snorkelMask != null) {
            drownShader().begin(batch, snorkelMask);
        }
        maybePopLostHand(zombie, pose, x, y);
        if (lostHands.containsKey(zombie)) {
            pose = pose.withHiddenParts(lostArmBodyParts(pose.pamPath()));
        }
        float time = drawPose(batch, zombie, pose, x, y, AnimScale.ZOMBIE, phase, tickHitFlash(zombie, delta), delta);
        maybeGargantuarWalkStomp(zombie, pose, time);
        if (snorkelMask != null) {
            drownShader().end(batch);
        }
        if (!Float.isNaN(snorkelWaterY)) {
            drawSnorkelRipple(batch, pose, ref, time, x, y, snorkelWaterY);
        }
        popBrokenArmor(zombie, pose, x, y);
        lastLive.put(zombie, new LiveSnap(pose, x, y,
                zombie.isMovingBackward() || pose.flipX(), time));
        captureOctopusRelease(zombie, pose, x, y, time);
        maybeDrawCrystalSkullBeam(batch, pose, x, y, time);
        syncBarrelFront(zombie, pose, time);
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
    }

    private void drawProspectorBlasts(Batch batch, float delta) {
        float scale = AnimScale.ZOMBIE;
        for (int i = prospectorBlasts.size() - 1; i >= 0; i--) {
            BlastFx fx = prospectorBlasts.get(i);
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

    private void drawHunterSplats(Batch batch, float delta) {
        float scale = AnimScale.PLANT;
        for (int i = hunterSplats.size() - 1; i >= 0; i--) {
            BlastFx fx = hunterSplats.get(i);
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
        if (ref == null) {
            fx.time += delta;
            return !fx.dying;
        }
        float clipTime = OctopusAnim.LOOP_CLIP.equals(clip)
                ? Math.max(0f, fx.time - impactDuration(path))
                : fx.time;
        player.draw(batch, ref, clipTime, x, y, AnimScale.ZOMBIE, AnimScale.ZOMBIE, loop);
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
        float[] xy = layout.centerOf(Math.round(row), progressX);
        out[0] = xy[0];
        out[1] = xy[1];
        out[2] = progressX;
        return true;
    }

    private float drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float flash, float delta) {
        seenThisFrame.add(entity);
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return 0f;
        }
        float stateTime = phase >= 0f
                ? phase * ref.duration
                : advanceClock(entity, pose.cacheKey(), delta);
        if (phase >= 0f) {
            // Gait skips the wall clock; still stamp the clip so the next one-shot
            // (Arcade {@code push}, All-Star {@code tackle}, …) restarts from 0.
            stampClockClip(entity, pose.cacheKey());
        }
        if (pose.reverse() && ref.duration > 0f) {
            stateTime = Math.max(0f, ref.duration - Math.min(stateTime, ref.duration));
        }
        float scale = baseScale * pose.scale();
        drawClip(batch, ref, pose, stateTime, x, y, scale);
        if (flash > 0f) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 1f, 1f, flash);
            drawClip(batch, ref, pose, stateTime, x, y, scale);
            batch.setColor(Color.WHITE);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
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

    /** White additive flash while body or armor HP dropped since last frame. */
    private float tickHitFlash(ZombieInstance zombie, float delta) {
        int vitality = vitality(zombie);
        HitFlash flash = hitFlashes.get(zombie);
        if (flash == null) {
            flash = new HitFlash();
            flash.vitality = vitality;
            hitFlashes.put(zombie, flash);
        } else {
            if (vitality < flash.vitality) {
                flash.remaining = HIT_FLASH_SEC;
            }
            flash.vitality = vitality;
        }
        if (flash.remaining <= 0f) {
            return 0f;
        }
        float strength = flash.remaining / HIT_FLASH_SEC;
        flash.remaining -= delta;
        return strength;
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
        if (zombie == null || pose == null || lostHands.containsKey(zombie)) {
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

    private void drawArmorPops(Batch batch, float delta) {
        for (int i = armorPops.size() - 1; i >= 0; i--) {
            ArmorPop pop = armorPops.get(i);
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

        DeathFx(AnimPose pose, float x, float y) {
            this(pose, x, y, false);
        }

        DeathFx(AnimPose pose, float x, float y, boolean drown) {
            this.pose = pose;
            this.x = x;
            this.y = y;
            this.drown = drown;
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
        if (trySpawnJaneAsh(zombie, snap)) {
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

    /** Jane's incineration PAM lives under EFFECTS and has clip {@code animation}, not {@code die}. */
    private boolean trySpawnJaneAsh(ZombieInstance zombie, LiveSnap snap) {
        if (zombie == null || !zombie.isBlownUp() || catalog == null) {
            return false;
        }
        if (zombie.getDefinition() == null
                || !"ZombieLostCityJane".equals(zombie.getDefinition().getName())) {
            return false;
        }
        PamCatalog.PamEntry ash = catalog.byName(JANE_ASH_PAM);
        if (ash == null) {
            return false;
        }
        String clip = catalog.resolveClip(ash, "animation");
        deathFx.add(new DeathFx(
                AnimPose.once(ash.path(), clip, ZombieAnimRole.DIE, null),
                snap.x, snap.y));
        return true;
    }

    private void maybeGargantuarWalkStomp(ZombieInstance zombie, AnimPose pose, float time) {
        if (screenShake == null || pose == null || !"walk".equals(pose.clipName())) {
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

    private void drawDeathFx(Batch batch, float delta) {
        for (int i = deathFx.size() - 1; i >= 0; i--) {
            DeathFx fx = deathFx.get(i);
            ClipRef ref = clips.getOrLoad(fx.pose.pamPath(), fx.pose.clipName());
            if (ref == null) {
                continue;
            }
            if (fx.time >= ref.duration + ARMOR_POP_FADE) {
                deathFx.remove(i);
                continue;
            }
            // Hold the collapsed last frame and fade it out instead of popping it off-screen.
            float scale = AnimScale.ZOMBIE * fx.pose.scale();
            float alpha = 1f - Math.max(0f, fx.time - ref.duration) / ARMOR_POP_FADE;
            float time = Math.min(fx.time, ref.duration);
            batch.setColor(1f, 1f, 1f, alpha);
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
            batch.setColor(Color.WHITE);
            fx.time += delta;
        }
    }

    private FishermanDrownShader drownShader() {
        if (drownShader == null) {
            drownShader = new FishermanDrownShader();
        }
        return drownShader;
    }

    private void drawSnorkelRipple(Batch batch, AnimPose pose, ClipRef body, float time,
                                   float x, float y, float waterY) {
        PamCatalog.PamEntry entry = catalog == null ? null : catalog.byName(SnorkelerAnim.RIPPLE_NAME);
        String path = entry != null ? entry.path() : SnorkelerAnim.RIPPLE_PATH;
        String clip = entry != null
                ? catalog.resolveClip(entry, SnorkelerAnim.RIPPLE_CLIP, "ripple_exit")
                : SnorkelerAnim.RIPPLE_CLIP;
        ClipRef ripple = clips.getOrLoad(path, clip);
        if (ripple == null && !snorkelRippleLoaded) {
            snorkelRippleLoaded = true;
            clips.preloadSync(path, clip);
            ripple = clips.getOrLoad(path, clip);
        }
        if (ripple == null) {
            return;
        }
        float scale = AnimScale.ZOMBIE * pose.scale();
        Rectangle skull = body != null ? partAt(body, time, SnorkelerAnim.SKULL_PART) : null;
        float rx = SnorkelerAnim.skullCenterWorldX(x, skull, scale, pose.flipX());
        Rectangle clipBox = player.bounds(path, clip);
        float ry = SnorkelerAnim.rippleDrawY(waterY, clipBox, scale);
        player.draw(batch, ripple, snorkelRippleTime, rx, ry, scale, scale, true);
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
}
