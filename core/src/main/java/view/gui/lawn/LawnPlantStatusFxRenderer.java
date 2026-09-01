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
final class LawnPlantStatusFxRenderer {
    private final LawnEntityRenderer r;

    LawnPlantStatusFxRenderer(LawnEntityRenderer r) {
        this.r = r;
    }

    void drawGraves(Batch batch, GameModel model, float delta, int row) {
        GameMap map = model.getMap();
        if (map == null || r.catalog == null) {
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
                PamCatalog.PamEntry entry = r.catalog.byName(GraveAnim.pamFor(chapter, grave));
                if (entry == null) {
                    continue;
                }
                drawGrave(batch, grave, row, col, entry.path(), delta);
            }
        }
    }

    void drawGrave(Batch batch, Grave grave, int row, int col,
                           String path, float delta) {
        ClipRef ref = r.clips.getOrLoad(path, GraveAnim.clipFor(grave));
        if (ref == null) {
            return;
        }
        r.seenThisFrame.add(grave);
        float u = tickGraveEmerge(grave, delta);
        float[] xy = r.layout.centerOf(row, col);
        float flash = r.tickHitFlash(grave, grave.getHp(), delta);
        drawSquashStretch(batch, ref, 0f, xy[0], xy[1], AnimScale.PLANT, u, false, flash);
        r.lastGraves.put(grave, new LiveSnap(
            AnimPose.looping(path, GraveAnim.clipFor(grave), ZombieAnimRole.IDLE),
            xy[0], xy[1], false, u));
    }

    float tickGraveEmerge(Grave grave, float delta) {
        float u = r.graveEmerge.getOrDefault(grave, 0f);
        r.graveEmerge.put(grave, Math.min(1f, u + delta / GraveAnim.EMERGE_DURATION));
        return u;
    }

    /** Killing blow removes the plant/grave before draw; hold last pose through the flash. */
    void drawPlantGhosts(Batch batch, Set<PlantInstance> live, float delta, int row) {
        for (PlantInstance plant : new ArrayList<>(r.lastPlants.keySet())) {
            if (live.contains(plant)) {
                continue;
            }
            LiveSnap snap = r.lastPlants.get(plant);
            if (snap == null || r.layout.rowAt(snap.y) != row) {
                continue;
            }
            float flash = r.tickHitFlash(plant, 0, delta);
            if (flash <= 0f || snap.pose == null) {
                r.lastPlants.remove(plant);
                continue;
            }
            r.drawPose(batch, plant, snap.pose, snap.x, snap.y, AnimScale.forPlant(snap.pose),
                    LawnEntityDrawConstants.NO_PHASE, flash, 0f);
        }
    }

    void drawGraveGhosts(Batch batch, float delta, int row) {
        for (Grave grave : new ArrayList<>(r.lastGraves.keySet())) {
            if (r.seenThisFrame.contains(grave)) {
                continue;
            }
            LiveSnap snap = r.lastGraves.get(grave);
            if (snap == null || r.layout.rowAt(snap.y) != row) {
                continue;
            }
            float flash = r.tickHitFlash(grave, 0, delta);
            if (flash <= 0f || snap.pose == null) {
                r.lastGraves.remove(grave);
                continue;
            }
            ClipRef ref = r.clips.getOrLoad(snap.pose.pamPath(), snap.pose.clipName());
            if (ref == null) {
                r.lastGraves.remove(grave);
                continue;
            }
            r.seenThisFrame.add(grave);
            drawSquashStretch(batch, ref, 0f, snap.x, snap.y, AnimScale.PLANT, snap.time, false,
                flash);
        }
    }

    void drawSquashStretch(Batch batch, ClipRef ref, float time,
                                   float x, float y, float baseScale, float u, boolean loop,
                                   float flash) {
        float sxN = GraveAnim.scaleX(u);
        float syN = GraveAnim.scaleY(u);
        float yPin = y + (syN - 1f) * baseScale * (GraveAnim.CANVAS * 0.5f);
        float sx = baseScale * sxN;
        float sy = baseScale * syN;
        r.player.draw(batch, ref, time, x, yPin, sx, sy, loop);
        r.overlayHitFlash(batch, flash, () -> r.player.draw(batch, ref, time, x, yPin, sx, sy, loop));
    }

    /**
     * @return true if this plant was drawn as a vanishing plant, sheep, or emerging plant
     */
    boolean drawWizardSheep(Batch batch, PlantInstance plant, float delta) {
        SheepFx fx = ensureWizardSheepFx(plant);
        if (fx == null) {
            return false;
        }
        r.seenThisFrame.add(plant);
        float flash = r.tickHitFlash(plant, LawnEntityRenderer.plantVitality(plant), delta);
        Point pos = plant.getPosition();
        if (pos == null) {
            r.sheepFx.remove(plant);
            return false;
        }
        float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
        preloadWizardSheepening();
        return paintWizardSheepPhase(batch, plant, fx, xy, flash, delta);
    }

    private SheepFx ensureWizardSheepFx(PlantInstance plant) {
        SheepFx fx = r.sheepFx.get(plant);
        if (plant.isTransformed()) {
            if (fx == null) {
                fx = new SheepFx();
                fx.idleClip = ThreadLocalRandom.current().nextBoolean()
                    ? WizardAnim.IDLE2_CLIP : WizardAnim.IDLE3_CLIP;
                r.sheepFx.put(plant, fx);
            }
            return fx;
        }
        if (fx != null && fx.phase != SheepPhase.LEAVE && fx.phase != SheepPhase.EMERGE) {
            if (fx.phase == SheepPhase.VANISH) {
                fx.phase = SheepPhase.EMERGE;
                fx.time = Math.max(0f, GraveAnim.EMERGE_DURATION - fx.time);
            } else {
                fx.phase = SheepPhase.LEAVE;
                fx.time = 0f;
            }
        }
        return fx;
    }

    private boolean paintWizardSheepPhase(Batch batch, PlantInstance plant, SheepFx fx,
                                          float[] xy, float flash, float delta) {
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
                        r.sheepFx.remove(plant);
                    }
                    return true;
                }
                r.sheepFx.remove(plant);
                return false;
            }
        }
        return false;
    }

    static float popU(float time) {
        return Math.max(0f, Math.min(1f, time / GraveAnim.EMERGE_DURATION));
    }

    boolean drawPlantPop(Batch batch, PlantInstance plant,
                                 float x, float y, float u, float flash) {
        AnimPose pose = r.plantAdapter.poseFor(plant);
        if (pose == null || pose.isSpritesheet()) {
            return false;
        }
        ClipRef ref = r.clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return false;
        }
        drawSquashStretch(batch, ref, 0f, x, y, AnimScale.PLANT * pose.scale(), u, pose.loop(),
            flash);
        return true;
    }

    boolean drawSheepening(Batch batch, float x, float y, SheepFx fx,
                                   float flash, float delta) {
        PamCatalog.PamEntry entry = r.catalog == null ? null : r.catalog.byName(WizardAnim.SHEEPENING_PAM);
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
        ClipRef ref = r.clips.getOrLoad(entry.path(), r.catalog.resolveClip(entry, clip));
        if (ref == null) {
            fx.time += delta;
            return true;
        }
        r.player.draw(batch, ref, fx.time, x, y, AnimScale.PLANT, AnimScale.PLANT, loop);
        r.overlayHitFlash(batch, flash, () ->
            r.player.draw(batch, ref, fx.time, x, y, AnimScale.PLANT, AnimScale.PLANT, loop));
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

    void preloadWizardSheepening() {
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(WizardAnim.SHEEPENING_PAM);
        if (entry == null) {
            return;
        }
        r.clips.getOrLoad(entry.path(), r.catalog.resolveClip(entry, WizardAnim.APPEAR_CLIP));
        r.clips.getOrLoad(entry.path(), r.catalog.resolveClip(entry, WizardAnim.LEAVE_CLIP));
        r.clips.getOrLoad(entry.path(), r.catalog.resolveClip(entry, WizardAnim.IDLE2_CLIP));
        r.clips.getOrLoad(entry.path(), r.catalog.resolveClip(entry, WizardAnim.IDLE3_CLIP));
    }

    void drawPlantChill(Batch batch, PlantInstance plant,
                                float x, float y, float flash, float delta) {
        int hits = plant.getFreezeHitCount();
        boolean show = !plant.isFrozen() && hits > 0 && !plant.hasOctopusCoating();
        if (!show) {
            r.plantChillClocks.remove(plant);
            return;
        }
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(PlantFreezeAnim.CHILL_PAM);
        if (entry == null) {
            return;
        }
        String preferredClip = hits == 1 ? PlantFreezeAnim.CHILL_STAGE1_CLIP : PlantFreezeAnim.CHILL_STAGE2_CLIP;
        String clip = r.catalog.resolveClip(entry, preferredClip);
        AnimPose pose = AnimPose.looping(entry.path(), clip, PlantAnimRole.IDLE);
        Object chillClock = r.plantChillClocks.computeIfAbsent(plant, p -> new Object());
        String clockKey = pose.cacheKey() + "#plant-chill-stage" + hits;
        r.drawPose(batch, chillClock, pose, x, y, AnimScale.PLANT, LawnEntityDrawConstants.NO_PHASE,
                flash, delta, clockKey);
    }

    void drawZombieFreezeChill(Batch batch, ZombieInstance zombie,
                                       float x, float y, float flash, float delta) {
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(PlantFreezeAnim.CHILL_PAM);
        if (entry == null) {
            return;
        }
        String clip = r.catalog.resolveClip(entry, PlantFreezeAnim.CHILL_STAGE2_CLIP);
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        Object chillClock = r.zombieChillClocks.computeIfAbsent(zombie, z -> new Object());
        String clockKey = pose.cacheKey() + "#zombie-frozen-chill-stage2";
        r.drawPose(batch, chillClock, pose, x, y, AnimScale.ZOMBIE, LawnEntityDrawConstants.NO_PHASE,
                flash, delta, clockKey);
    }

    void drawPlantFreezeIce(Batch batch, PlantInstance plant,
                                    float x, float y, float flash, float delta) {
        boolean show = plant.isFrozen() && !plant.hasOctopusCoating();
        if (!show) {
            r.plantIceIntro.remove(plant);
            r.plantIceClocks.remove(plant);
            LiveSnap prev = r.lastPlantIce.remove(plant);
            if (prev != null) {
                spawnIceShatter(prev);
            }
            return;
        }
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(PlantFreezeAnim.ICE_PAM);
        if (entry == null) {
            return;
        }
        r.terrain.preloadIceBreak();
        float startDur = plantIceStartDuration(entry.path());
        Float intro = r.plantIceIntro.get(plant);
        boolean playStart = intro == null || intro < startDur;
        AnimPose pose;
        String clockKey;
        if (playStart) {
            String clip = r.catalog.resolveClip(entry, PlantFreezeAnim.START_CLIP);
            pose = AnimPose.once(entry.path(), clip, PlantAnimRole.SPECIAL, null);
            clockKey = pose.cacheKey() + "#plant-ice-start";
        } else {
            String clip = r.catalog.resolveClip(entry, PlantFreezeAnim.IDLE_CLIP);
            pose = AnimPose.looping(entry.path(), clip, PlantAnimRole.IDLE);
            clockKey = pose.cacheKey() + "#plant-ice-idle";
        }
        Object iceClock = r.plantIceClocks.computeIfAbsent(plant, p -> new Object());
        float time = r.drawPose(batch, iceClock, pose, x, y, AnimScale.PLANT, LawnEntityDrawConstants.NO_PHASE,
                flash, delta, clockKey);
        if (playStart) {
            r.plantIceIntro.put(plant, Math.min(time, startDur));
        }
        r.lastPlantIce.put(plant, new LiveSnap(pose, x, y, false, time));
    }

    float plantIceStartDuration(String path) {
        ClipRef start = r.clips.getOrLoad(path, PlantFreezeAnim.START_CLIP);
        return start == null ? 0.9f : start.duration;
    }

    void spawnIceShatter(LiveSnap snap) {
        if (snap == null) {
            return;
        }
        r.terrain.preloadIceBreak();
        PamCatalog.PamEntry entry = r.catalog == null ? null : r.catalog.byName(TroglobiteAnim.ICE_BREAK_PAM);
        if (entry == null) {
            return;
        }
        String clip = r.catalog.resolveClip(entry, "animation");
        r.prop.addFlashingDeath(AnimPose.once(entry.path(), clip, ZombieAnimRole.DIE, null),
            snap.x, snap.y);
    }
}
