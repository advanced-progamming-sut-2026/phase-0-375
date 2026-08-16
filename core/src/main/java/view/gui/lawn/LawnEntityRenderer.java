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
import model.item.pushable.ArcadeMachine;
import model.item.pushable.Barrel;
import model.item.pushable.Piano;
import model.item.pushable.Pushable;
import model.zombie.armor.Armor;
import model.zombie.behavior.BarrelRollerBehavior;
import model.zombie.behavior.JumpBehavior;
import model.zombie.behavior.PushBehavior;
import model.zombie.behavior.StealSunBehavior;
import model.zombie.behavior.SummonBehavior;
import model.zombie.behavior.ThrowImpBehavior;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.AnimScale;
import view.gui.anim.GraveAnim;
import view.gui.anim.PamClipCache;
import view.gui.anim.plant.PlantAnimAdapter;
import view.gui.anim.zombie.BarrelRollerAnim;
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
import java.util.Set;

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
            "zombie_arm_outer_lower", "zombie_arms_outer_upper"};

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
    /** Outstretched pushing hand on {@code ZOMBIE_80S_ARCADE}. */
    private static final String ARCADE_HAND_PART = "zombie_troglobite_hand_oute_push";

    private final LawnLayout layout;
    private final PlantAnimAdapter plantAdapter;
    private final ZombieAnimAdapter zombieAdapter;
    private final PamClipCache clips;
    private final pvz.libpvz.pam.PamPlayer player;
    private final PamCatalog catalog;

    private final IdentityHashMap<Object, AnimClock> clocks = new IdentityHashMap<>();
    private final IdentityHashMap<ClipRef, ZombieFootfallCurve> footfalls = new IdentityHashMap<>();
    /** Left-edge canvas X of the pushing hand, one sample per push-clip frame. */
    private final IdentityHashMap<ClipRef, float[]> arcadePushHandX = new IdentityHashMap<>();
    /** Crystal Skull / beam part names that actually exist on the loaded PAM. */
    private String crystalSkullPart;
    private String crystalBeamPart;
    private final IdentityHashMap<ZombieInstance, HitFlash> hitFlashes = new IdentityHashMap<>();
    private final IdentityHashMap<ZombieInstance, Chapter> artChapters = new IdentityHashMap<>();
    private final List<ArmorPop> armorPops = new ArrayList<>();
    private final List<DeathFx> deathFx = new ArrayList<>();
    private final IdentityHashMap<ZombieInstance, LiveSnap> lastLive = new IdentityHashMap<>();
    private final IdentityHashMap<Pushable, LiveSnap> lastCabinets = new IdentityHashMap<>();
    /** World-pixel skull alignment for a thrown Imp; lerped to 0 as it lands. */
    private final IdentityHashMap<ZombieInstance, float[]> tossAlign = new IdentityHashMap<>();
    private final List<BlastFx> prospectorBlasts = new ArrayList<>();
    private final IdentityHashMap<ZombieInstance, Boolean> prospectorBlastSpawned = new IdentityHashMap<>();
    private final IdentityHashMap<Grave, Float> graveEmerge = new IdentityHashMap<>();
    private final Set<Object> seenThisFrame = new HashSet<>();
    private final float[] xyTmp = new float[3];
    private final Matrix4 batchTransform = new Matrix4();
    private final Matrix4 popTransform = new Matrix4();

    private final DebugEntityOverlay entityOverlay;

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
            } else {
                drawCabinet(batch, cabinet, delta);
            }
        }
        for (ZombieInstance zombie : model.getZombies()) {
            Chapter skin = artChapterFor(zombie, model.getChapter());
            drawZombie(batch, zombie, skin, delta);
        }
        drawProspectorBlasts(batch, delta);
        drawDeathFx(batch, delta);
        drawArmorPops(batch, delta);
        drawSuns(batch, model, delta);

        clocks.keySet().removeIf(key -> !seenThisFrame.contains(key));
        graveEmerge.keySet().removeIf(grave -> !seenThisFrame.contains(grave));
        hitFlashes.keySet().removeIf(key -> !seenThisFrame.contains(key));
        tossAlign.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        prospectorBlastSpawned.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        artChapters.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
    }

    private void drawPlant(Batch batch, PlantInstance plant, float delta) {
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
        float sxN = GraveAnim.scaleX(u);
        float syN = GraveAnim.scaleY(u);
        float s = AnimScale.PLANT;
        float[] xy = layout.centerOf(row, col);
        // Scale is about the PAM centre; pin the base so the pancake sits on the tile.
        float y = xy[1] + (syN - 1f) * s * (GraveAnim.CANVAS * 0.5f);
        player.draw(batch, ref, 0f, xy[0], y, s * sxN, s * syN, false);
    }

    private float tickGraveEmerge(Grave grave, float delta) {
        float u = graveEmerge.getOrDefault(grave, 0f);
        graveEmerge.put(grave, Math.min(1f, u + delta / GraveAnim.EMERGE_DURATION));
        return u;
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
        String clip = firstLoadedClip(pam, "death", snap.pose.clipName());
        deathFx.add(new DeathFx(
                AnimPose.once(pam, clip, ZombieAnimRole.DIE, snap.pose.visibility()),
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
        float time = drawPose(batch, zombie, pose, x, y, AnimScale.ZOMBIE, phase, tickHitFlash(zombie, delta), delta);
        popBrokenArmor(zombie, pose, x, y);
        lastLive.put(zombie, new LiveSnap(pose, x, y,
                zombie.isMovingBackward() || pose.flipX(), time));
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
                    if (pop.bounces < 2 && -pop.vy > layout.cellHeight() * 0.25f) {
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
            if (pop.part == null) {
                // Whole clip: PAM default-hidden flags stay on (butter, etc.).
                player.draw(batch, pop.pamPath, pop.clipName, 0f, pop.x, pop.y, 1f, 1f, false);
            } else {
                player.drawPart(batch, pop.pamPath, pop.clipName, 0f, pop.x, pop.y, pop.part);
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
        float time;

        DeathFx(AnimPose pose, float x, float y) {
            this.pose = pose;
            this.x = x;
            this.y = y;
        }
    }

    private static final class BlastFx {
        final String pamPath;
        final float x;
        final float y;
        float time;

        BlastFx(String pamPath, float x, float y) {
            this.pamPath = pamPath;
            this.x = x;
            this.y = y;
        }
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
        deathFx.add(new DeathFx(diePose, snap.x, snap.y));

        // The head lies on the ground until the body has finished collapsing, then both fade together.
        ClipRef dieRef = clips.getOrLoad(pam, dieClip);
        String headGroup = deathHeadGroup(pam);
        float hold = dieRef != null && (headGroup != null || popsHeadAndArm(pam))
                ? dieRef.duration : 0f;
        float dir = snap.backward ? -1f : 1f;
        if (headGroup != null && firstLoadedClip(pam, "particles", null) != null) {
            // Gargantuar/Imp: the clip is already just the head; drawPart would whitelist butter.
            // All-Star: {_particles} is default-hidden, so the clip must be drawn via drawPart.
            addLimbPop(pam, "particles", headGroup, snap.x, snap.y, 0f, dir, 0.2f, 0.85f, hold,
                    !isAllStar(pam));
            return;
        }
        for (int i = 0; i < bits.size(); i++) {
            float back = 0.1f + i * 0.1f;
            float hop = 0.85f + (i % 2) * 0.3f;
            addLimbPop(pam, "particles", bits.get(i), snap.x, snap.y, 0f, dir, back, hop, hold, false);
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

    private static boolean popsHeadAndArm(String pam) {
        return isArcadeZombie(pam) || isProspector(pam);
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
        if (popsHeadAndArm(pam)) {
            return ARCADE_HEAD_PARTS;
        }
        return null;
    }

    private String firstLoadedClip(String pam, String preferred, String fallback) {
        if (preferred == null) {
            return fallback;
        }
        if (clips.getOrLoad(pam, preferred) != null) {
            return preferred;
        }
        player.getParts(pam);
        return player.getClip(pam, preferred) != null ? preferred : fallback;
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
        for (String part : names) {
            if (player.partBounds(pam, "particles", 0f, part) != null) {
                bits.add(part);
            }
        }
        return bits;
    }

    private void addLimbPop(String pam, String clip, String part,
                            float originX, float originY, float time,
                            float dir, float backTiles, float hopTiles, float hold,
                            boolean wholeClip) {
        float s = AnimScale.ZOMBIE;
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
            batch.setColor(1f, 1f, 1f, alpha);
            drawClip(batch, ref, fx.pose, Math.min(fx.time, ref.duration), fx.x, fx.y, scale);
            batch.setColor(Color.WHITE);
            fx.time += delta;
        }
    }
}
