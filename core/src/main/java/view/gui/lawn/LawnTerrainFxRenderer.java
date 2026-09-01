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
final class LawnTerrainFxRenderer {
    private final LawnEntityRenderer r;

    LawnTerrainFxRenderer(LawnEntityRenderer r) {
        this.r = r;
    }

    /**
     * Phase machine for every active Egypt sandstorm: intro plays as the storm
     * starts moving, loop repeats until touchdown, then outro fades it away
     * once over the landed zombie. Also collects the freshly landed zombies
     * that must stay hidden behind their storm's outro.
     */
    void updateSandstorms(GameModel model, float delta) {
        r.sandstormConcealed.clear();
        if (model.getSandstorms().isEmpty()) {
            r.sandstorms.clear();
            return;
        }
        for (SandstormSpawn storm : model.getSandstorms()) {
            tickSandstorm(storm, delta);
        }
        Set<SandstormSpawn> live = Collections.newSetFromMap(new IdentityHashMap<>());
        live.addAll(model.getSandstorms());
        r.sandstorms.keySet().removeIf(storm -> !live.contains(storm));
    }

    private void tickSandstorm(SandstormSpawn storm, float delta) {
        SandstormFx fx = r.sandstorms.get(storm);
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
            landSandstorm(storm, fx, delta);
        }
        cacheStormScale(fx);
    }

    private void landSandstorm(SandstormSpawn storm, SandstormFx fx, float delta) {
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
            r.sandstormConcealed.add(storm.getSpawned());
        }
    }

    /** First sighting of a model sandstorm: resolve placement + clip timings. */
    SandstormFx beginSandstorm(SandstormSpawn storm) {
        if (!r.sandstormPamReady) {
            r.sandstormPamReady = true;
            r.clips.preloadSync(SandstormAnim.PAM_PATH, SandstormAnim.LOOP_CLIP);
        }
        SandstormFx fx = new SandstormFx();
        // Materialises past the zombie entry edge, off-screen right.
        fx.startX = r.layout.centerOf(storm.getLane(), r.layout.cols())[0]
                + SandstormAnim.START_MARGIN_PX;
        float[] target = r.layout.centerOf(storm.getLane(), storm.getColumn());
        fx.targetX = target[0];
        // Raised half a tile so the dust cloud doesn't sink into the lane below.
        fx.y = target[1] + r.layout.cellHeight() * 0.5f;
        fx.introDuration = r.player.clipDurationSeconds(
                SandstormAnim.PAM_PATH, SandstormAnim.INTRO_CLIP);
        fx.outroDuration = r.player.clipDurationSeconds(
                SandstormAnim.PAM_PATH, SandstormAnim.OUTRO_CLIP);
        if (fx.outroDuration <= 0f) {
            fx.outroDuration = 0.8f;
        }
        fx.outroDuration = Math.min(fx.outroDuration, SandstormSpawn.OUTRO_SECONDS);
        r.sandstorms.put(storm, fx);
        return fx;
    }

    /** Storm art is scaled to cover a fixed number of lawn cells in height. */
    void cacheStormScale(SandstormFx fx) {
        if (fx.scale > 0f || fx.clip == null) {
            return;
        }
        Rectangle bounds = r.player.bounds(SandstormAnim.PAM_PATH, fx.clip);
        fx.scale = bounds != null && bounds.height > 0f
                ? r.layout.cellHeight() * SandstormAnim.HEIGHT_CELLS / bounds.height
                : AnimScale.LAWN;
    }

    /** Draws the storms above lawn content; their zombie hides underneath. */
    void drawSandstorms(Batch batch) {
        for (SandstormFx fx : r.sandstorms.values()) {
            if (!fx.visible || fx.clip == null || fx.scale <= 0f) {
                continue;
            }
            ClipRef ref = r.clips.getOrLoad(SandstormAnim.PAM_PATH, fx.clip);
            if (ref == null) {
                continue;
            }
            r.player.draw(batch, ref, fx.clipTime, fx.x, fx.y, fx.scale, fx.scale, fx.loop);
        }
    }

    /**
     * Syncs the view gusts with the model's ice winds; each gust sweeps
     * right-to-left across its lane for {@link IceWindGust#SWEEP_SECONDS}.
     */
    void updateIceWinds(GameModel model, float delta) {
        if (model.getIceWinds().isEmpty()) {
            r.iceWinds.clear();
            return;
        }
        for (IceWindGust gust : model.getIceWinds()) {
            IceWindFx fx = r.iceWinds.get(gust);
            if (fx == null) {
                fx = beginIceWind(gust);
            }
            fx.clock += delta;
            float progress = gust.progress();
            fx.x = fx.startX + (fx.endX - fx.startX) * progress;
        }
        Set<IceWindGust> live = Collections.newSetFromMap(new IdentityHashMap<>());
        live.addAll(model.getIceWinds());
        r.iceWinds.keySet().removeIf(gust -> !live.contains(gust));
    }

    /** First sighting of a model ice wind: resolve placement + art scale. */
    IceWindFx beginIceWind(IceWindGust gust) {
        if (!r.iceWindPamReady) {
            r.iceWindPamReady = true;
            r.clips.preloadSync(IceWindAnim.PAM_PATH, IceWindAnim.CLIP);
        }
        IceWindFx fx = new IceWindFx();
        // Enters past the zombie entry edge and exits past the house edge.
        fx.startX = r.layout.centerOf(gust.getLane(), r.layout.cols())[0]
                + IceWindAnim.START_MARGIN_PX;
        fx.endX = r.layout.centerOf(gust.getLane(), 0)[0]
                - IceWindAnim.START_MARGIN_PX;
        fx.y = r.layout.centerY(gust.getLane());
        Rectangle bounds = r.player.bounds(IceWindAnim.PAM_PATH, IceWindAnim.CLIP);
        fx.scale = bounds != null && bounds.height > 0f
                ? r.layout.cellHeight() * IceWindAnim.HEIGHT_CELLS / bounds.height
                : AnimScale.LAWN;
        r.iceWinds.put(gust, fx);
        return fx;
    }

    /** Draws the ice winds above lawn content so frost puffs ride over plants. */
    void drawIceWinds(Batch batch) {
        for (IceWindFx fx : r.iceWinds.values()) {
            if (fx.scale <= 0f) {
                continue;
            }
            ClipRef ref = r.clips.getOrLoad(IceWindAnim.PAM_PATH, IceWindAnim.CLIP);
            if (ref == null) {
                continue;
            }
            r.player.draw(batch, ref, fx.clock, fx.x, fx.y, fx.scale, fx.scale, true);
        }
    }

    void preloadCraters() {
        r.textures.loadSync(BeghouledArt.ATLAS_GROUP);
        r.textures.loadSync(BeghouledArt.ATLAS_PAGE);
        r.craterRegion = r.textures.region(BeghouledArt.CRATER_TILE);
        if (r.craterRegion == null) {
            r.craterRegion = r.textures.region(BeghouledArt.CRATER_LARGE);
        }
    }

    void drawCraters(Batch batch, GameModel model, int row) {
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
        int cols = Math.min(r.layout.cols(), map.getCols());
        for (int c = 0; c < cols; c++) {
            Cell cell = map.getCell(c, row);
            if (cell == null || !isCraterCell(cell)) {
                continue;
            }
            float[] xy = r.layout.centerOf(row, c);
            batch.draw(crater, xy[0] - w * 0.5f, xy[1] - h * 0.4f, w, h);
        }
    }

    static boolean isCraterCell(Cell cell) {
        return cell.getGroundType() == GroundType.CRATER
                || cell.getTerrainStrategy() instanceof CraterTerrainStrategy;
    }

    void drawFireTiles(Batch batch, GameModel model, float delta, int row) {
        GameMap map = model != null ? model.getMap() : null;
        if (map == null || batch == null || r.player == null) {
            return;
        }
        preloadFireImpactPam();
        float scale = AnimScale.PLANT * 0.8f;

        int cols = Math.min(r.layout.cols(), map.getCols());
        for (int c = 0; c < cols; c++) {
            Cell cell = map.getCell(c, row);
            if (cell == null || cell.getGroundType() != GroundType.FIRE) {
                continue;
            }
            FireTileFx fx = r.fireTileFx.get(cell);
            if (fx == null) {
                fx = new FireTileFx();
                r.fireTileFx.put(cell, fx);
            }
            if (fx.phase == FireTileFxPhase.OUTRO) {
                fx.phase = FireTileFxPhase.INTRO;
                fx.time = 0f;
            }
            drawFireTileFx(batch, fx, row, c, scale, delta);
        }
        removeFinishedFireTiles(batch, row, scale, delta);
    }

    private void removeFinishedFireTiles(Batch batch, int row, float scale, float delta) {
        List<Cell> finished = null;
        for (Map.Entry<Cell, FireTileFx> e : r.fireTileFx.entrySet()) {
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
                r.fireTileFx.remove(cell);
            }
        }
    }

    boolean drawFireTileFx(Batch batch, FireTileFx fx, int row, int col,
                                   float scale, float delta) {
        String clip = fireTileFxClip(fx.phase);
        ClipRef ref = r.clips.getOrLoad(EffectPamPaths.POWER_UP_FIRE_IMPACT, clip);
        if (ref == null) {
            return fx.phase == FireTileFxPhase.OUTRO;
        }
        float duration = r.plantFx.effectClipDurationSeconds(ref, EffectPamPaths.POWER_UP_FIRE_IMPACT, clip);
        boolean loop = fx.phase == FireTileFxPhase.IDLE;
        float[] xy = r.layout.centerOf(row, col);
        r.player.draw(batch, ref, fx.time, xy[0], xy[1], scale, scale, loop);
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

    void preloadFireImpactPam() {
        if (r.fireImpactPamReady) {
            return;
        }
        r.clips.getOrLoad(EffectPamPaths.POWER_UP_FIRE_IMPACT, EffectPamPaths.POWER_UP_FIRE_IMPACT_INTRO);
        r.clips.getOrLoad(EffectPamPaths.POWER_UP_FIRE_IMPACT, EffectPamPaths.POWER_UP_FIRE_IMPACT_IDLE);
        r.clips.getOrLoad(EffectPamPaths.POWER_UP_FIRE_IMPACT, EffectPamPaths.POWER_UP_FIRE_IMPACT_OUTRO);
        r.fireImpactPamReady = true;
    }

    static String fireTileFxClip(FireTileFxPhase phase) {
        return switch (phase) {
            case INTRO -> EffectPamPaths.POWER_UP_FIRE_IMPACT_INTRO;
            case IDLE -> EffectPamPaths.POWER_UP_FIRE_IMPACT_IDLE;
            case OUTRO -> EffectPamPaths.POWER_UP_FIRE_IMPACT_OUTRO;
        };
    }

    TextureRegion ensureCraterRegion() {
        if (r.craterRegion != null) {
            return r.craterRegion;
        }
        r.craterRegion = r.textures.region(BeghouledArt.CRATER_TILE);
        if (r.craterRegion == null) {
            r.craterRegion = r.textures.region(BeghouledArt.CRATER_LARGE);
        }
        return r.craterRegion;
    }

    /**
     * Frostbite ice tiles: occupant {@code idle} behind {@link TroglobiteAnim#ICE_PAM}.
     */
    Set<Cell> syncTerrainIce(GameModel model) {
        GameMap map = model.getMap();
        Set<Cell> live = new HashSet<>();
        if (map == null || r.catalog == null) {
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
        for (Cell cell : r.lastTerrainIce.keySet()) {
            if (!live.contains(cell)) {
                r.plantStatus.spawnIceShatter(r.lastTerrainIce.get(cell));
            }
        }
        r.lastTerrainIce.entrySet().removeIf(e -> !live.contains(e.getKey()));
        return live;
    }

    void drawTerrainIce(Batch batch, GameModel model, Set<Cell> live, float delta, int row) {
        for (Cell cell : live) {
            if (cell.getRow() == row) {
                drawTerrainIceCell(batch, model, cell, delta);
            }
        }
    }

    static boolean isLiveTerrainIce(Cell cell) {
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
    Set<Cell> syncSlideTiles(GameModel model) {
        GameMap map = model.getMap();
        Set<Cell> live = new HashSet<>();
        if (map == null) {
            r.slideTiles.clear();
            return live;
        }
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                Cell cell = model.getCellAt(row, col);
                if (cell != null
                        && cell.getTerrainStrategy() instanceof SlideTerrainStrategy) {
                    live.add(cell);
                    r.slideTiles.computeIfAbsent(cell, key -> beginSlideTile(key));
                }
            }
        }
        r.slideTiles.keySet().removeIf(cell -> !live.contains(cell));
        return live;
    }

    /** First sighting of a model slide tile: preload art + resolve scale. */
    SlideTileFx beginSlideTile(Cell cell) {
        if (!r.slideTilePamReady) {
            r.slideTilePamReady = true;
            r.clips.preloadSync(SlideTileAnim.DOWN_PAM_PATH,
                    SlideTileAnim.IDLE_CLIP, SlideTileAnim.START_CLIP, SlideTileAnim.END_CLIP);
            r.clips.preloadSync(SlideTileAnim.UP_PAM_PATH,
                    SlideTileAnim.IDLE_CLIP, SlideTileAnim.START_CLIP, SlideTileAnim.END_CLIP);
        }
        SlideTileFx fx = new SlideTileFx();
        String pam = pamFor(cell);
        // The slider PAM's origin is its middle: fit the art over one tile so
        // that origin lands exactly on the tile centre when drawn there.
        Rectangle bounds = r.player.bounds(pam, SlideTileAnim.IDLE_CLIP);
        fx.scale = bounds != null && bounds.width > 0f && bounds.height > 0f
                ? Math.max(r.layout.cellWidth() / bounds.width,
                        r.layout.cellHeight() / bounds.height)
                : AnimScale.LAWN;
        return fx;
    }

    /** Consumes model slide cues and kicks each hit tile's active burst. */
    void harvestSlideStarts(GameModel model) {
        for (Point cue : model.drainSlideStarts()) {
            Cell cell = model.getCellAt(cue.getY(), cue.getX());
            SlideTileFx fx = cell != null ? r.slideTiles.get(cell) : null;
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
    void updateLaneGlides(GameModel model) {
        if (model.getLaneSlides().isEmpty()) {
            r.laneGlides.clear();
            return;
        }
        r.laneGlides.clear();
        for (LaneSlide glide : model.getLaneSlides()) {
            r.laneGlides.put(glide.getZombie(), glide);
        }
        Set<ZombieInstance> live = new HashSet<>(model.getZombies());
        r.laneGlides.keySet().removeIf(zombie -> !live.contains(zombie));
    }

    /** Mirrors the model's surfacing ambush zombies for the water mask. */
    void updateWaterEmerges(GameModel model) {
        if (model.getWaterEmerges().isEmpty()) {
            r.waterEmerges.clear();
            return;
        }
        r.waterEmerges.clear();
        for (WaterEmerge emerge : model.getWaterEmerges()) {
            r.waterEmerges.put(emerge.getZombie(), emerge);
        }
        Set<ZombieInstance> live = new HashSet<>(model.getZombies());
        r.waterEmerges.keySet().removeIf(zombie -> !live.contains(zombie));
    }

    void drawSlideTiles(Batch batch, Set<Cell> live, float delta, int row) {
        for (Cell cell : live) {
            if (cell.getRow() != row) {
                continue;
            }
            SlideTileFx fx = r.slideTiles.get(cell);
            if (fx == null || fx.scale <= 0f) {
                continue;
            }
            String pam = pamFor(cell);
            String clip = switch (fx.phase) {
                case IDLE -> SlideTileAnim.IDLE_CLIP;
                case ACTIVE_START -> SlideTileAnim.START_CLIP;
                case ACTIVE_END -> SlideTileAnim.END_CLIP;
            };
            ClipRef ref = r.clips.getOrLoad(pam, clip);
            if (ref == null) {
                continue;
            }
            // The PAM's (0,0) axis sits on its middle, so drawing at the tile
            // centre puts exactly that point on the middle of the tile.
            float[] xy = r.layout.centerOf(cell.getRow(), cell.getColumn());
            r.player.draw(batch, ref, fx.clock, xy[0], xy[1], fx.scale, fx.scale,
                    fx.phase == SlideTileFx.Phase.IDLE);
            fx.clock += delta;
            if (fx.phase != SlideTileFx.Phase.IDLE) {
                float duration = r.player.clipDurationSeconds(pam, clip);
                if (duration > 0f && fx.clock >= duration) {
                    fx.phase = fx.phase == SlideTileFx.Phase.ACTIVE_START
                            ? SlideTileFx.Phase.ACTIVE_END
                            : SlideTileFx.Phase.IDLE;
                    fx.clock = 0f;
                }
            }
        }
    }

    static String pamFor(Cell cell) {
        SlideTerrainStrategy slide = (SlideTerrainStrategy) cell.getTerrainStrategy();
        return slide.getSlideDirection() == SlideDirection.UP
                ? SlideTileAnim.UP_PAM_PATH
                : SlideTileAnim.DOWN_PAM_PATH;
    }

    void drawTerrainIceCell(Batch batch, GameModel model, Cell cell, float delta) {
        IceTerrainStrategy ice = (IceTerrainStrategy) cell.getTerrainStrategy();
        float[] xy = r.layout.centerOf(cell.getRow(), cell.getColumn());
        Placeable occupant = ice.getContainedEntity();
        if (occupant instanceof ZombieInstance zombie) {
            drawIcedZombieIdle(batch, zombie, model, xy[0], xy[1], delta);
        }
        PamCatalog.PamEntry entry = r.catalog.byName(TroglobiteAnim.ICE_PAM);
        if (entry == null) {
            return;
        }
        preloadIceBreak();
        String clip = r.catalog.resolveClip(entry, "idle");
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        float time = r.drawPose(batch, cell, pose, xy[0], xy[1], AnimScale.ZOMBIE, LawnEntityDrawConstants.NO_PHASE,
            r.tickHitFlash(cell, ice.getHp(), delta), delta);
        r.lastTerrainIce.put(cell, new LiveSnap(pose, xy[0], xy[1], false, time));
    }

    void drawIcedZombieIdle(Batch batch, ZombieInstance zombie, GameModel model,
                                    float x, float y, float delta) {
        if (zombie == null || zombie.getDefinition() == null) {
            return;
        }
        Chapter skin = r.artChapterFor(zombie, model.getChapter());
        PamCatalog.PamEntry entry = r.catalog.forZombie(zombie.getDefinition().getName(), skin);
        if (entry == null) {
            return;
        }
        String clip = r.catalog.resolveClip(entry, "idle", "walk");
        if (clip == null) {
            return;
        }
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE,
            ZombieAnimAdapter.armorVisibility(zombie, entry));
        if (ZombotanyAnim.isPlantHead(zombie)) {
            pose = ZombotanyAnim.withHeadHidden(pose);
        }
        float time = r.drawPose(batch, zombie, pose, x, y, AnimScale.ZOMBIE, LawnEntityDrawConstants.NO_PHASE, 0f, 0f,
                pose.cacheKey(), 0f, 1.0f);
        if (ZombotanyAnim.isPlantHead(zombie)) {
            r.zombotany.drawZombotanyPlantHead(batch, zombie, pose, x, y, time, 0f, 0f, 0f, 0f);
        }
    }

    static void collectIcedOccupants(GameModel model, Set<ZombieInstance> into) {
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

    static void addIceOccupant(Pushable item, Set<ZombieInstance> into) {
        if (item instanceof IceBlock block
            && block.getContainedEntity() instanceof ZombieInstance zombie) {
            into.add(zombie);
        }
    }

    void preloadIceBreak() {
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(TroglobiteAnim.ICE_BREAK_PAM);
        if (entry != null) {
            r.clips.getOrLoad(entry.path(), r.catalog.resolveClip(entry, "animation"));
        }
    }
}
