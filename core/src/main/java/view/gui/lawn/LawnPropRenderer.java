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
final class LawnPropRenderer {
    private final LawnEntityRenderer r;

    LawnPropRenderer(LawnEntityRenderer r) {
        this.r = r;
    }

    void prepareBowlingWalnuts(GameModel model) {
        if (!(model.getCurrentLevel() instanceof WallnutBowlingLevel bowling)) {
            r.bowlingWalnuts = List.of();
            return;
        }
        harvestBowlingExplosions(bowling);
        List<BowlingWalnut> active = bowling.getActiveWalnuts();
        r.bowlingWalnuts = active;
        for (BowlingWalnut walnut : active) {
            if (walnut != null) {
                r.seenThisFrame.add(walnut);
            }
        }
    }

    void preloadVases() {
        for (String pam : VaseBreakerAnim.allVasePams()) {
            r.clips.preloadSync(pam,
                    VaseBreakerAnim.CLIP_DROP,
                    VaseBreakerAnim.CLIP_IDLE,
                    VaseBreakerAnim.CLIP_BREAK);
        }
        r.clips.preloadSync(VaseBreakerAnim.GARGANTUAR_ZOMBIE,
                "idle", "walk", "eat", "smash_left", "fire", "cannon_fire", "die");
    }

    void playVaseBreak(String pamPath, int col, int row) {
        if (pamPath == null) {
            return;
        }
        float[] xy = r.layout.centerOf(row, col);
        r.frontEffects.add(new OneShotFx(
                pamPath, VaseBreakerAnim.CLIP_BREAK, xy[0], xy[1], AnimScale.PLANT, false));
        r.vaseAge.remove(vaseKey(col, row));
    }

    void drawVases(Batch batch, GameModel model, float delta, int row, int rows) {
        if (!(model.getCurrentLevel() instanceof VaseBreakerLevel level)) {
            return;
        }
        for (Vase vase : level.getVases()) {
            if (vase.isBroken() || vase.getPosition() == null) {
                continue;
            }
            int col = vase.getPosition().getX();
            int vaseRow = vase.getPosition().getY();
            if (LawnEntityRenderer.clampRow(vaseRow, rows) != row) {
                continue;
            }
            String key = vaseKey(col, vaseRow);
            float age = r.vaseAge.getOrDefault(key, 0f) + Math.max(0f, delta);
            r.vaseAge.put(key, age);
            String pam = VaseBreakerAnim.pamPath(vase);
            boolean dropping = age < VaseBreakerAnim.DROP_SECONDS;
            String clip = dropping ? VaseBreakerAnim.CLIP_DROP : VaseBreakerAnim.CLIP_IDLE;
            float time = dropping ? age : age - VaseBreakerAnim.DROP_SECONDS;
            ClipRef ref = r.clips.getOrLoad(pam, clip);
            if (ref == null) {
                continue;
            }
            float[] xy = r.layout.centerOf(vaseRow, col);
            r.player.draw(batch, ref, time, xy[0], xy[1],
                    AnimScale.PLANT, AnimScale.PLANT, !dropping);
        }
    }

    void pruneVaseAge(GameModel model) {
        if (!(model.getCurrentLevel() instanceof VaseBreakerLevel level)) {
            r.vaseAge.clear();
            return;
        }
        HashSet<String> live = new HashSet<>();
        for (Vase vase : level.getVases()) {
            if (!vase.isBroken() && vase.getPosition() != null) {
                live.add(vaseKey(vase.getPosition().getX(), vase.getPosition().getY()));
            }
        }
        r.vaseAge.keySet().removeIf(key -> !live.contains(key));
    }

    static String vaseKey(int col, int row) {
        return col + "," + row;
    }

    void drawBowlingWalnuts(Batch batch, float delta, int row, int rows) {
        if (r.bowlingWalnuts.isEmpty()) {
            return;
        }
        for (BowlingWalnut walnut : r.bowlingWalnuts) {
            if (walnut == null) {
                continue;
            }
            if (LawnEntityRenderer.clampRow(Math.round(walnut.getY()), rows) != row) {
                continue;
            }
            drawBowlingWalnut(batch, walnut, delta);
        }
    }

    void harvestBowlingExplosions(WallnutBowlingLevel bowling) {
        for (FloatPoint point : bowling.drainExplosions()) {
            if (point == null) {
                continue;
            }
            float[] xy = r.layout.centerOf(point.getY(), point.getX());
            r.plantFx.spawnExplosionSpecs(ExplosivePlantFx.specsForName("Explode-o-nut"), null, xy[0], xy[1]);
        }
    }

    void drawBowlingWalnut(Batch batch, BowlingWalnut walnut, float delta) {
        String plantName = BowlingWalnutAnim.artPlantName(walnut);
        ClipRef ref = r.plantFx.plantIdleClip(plantName);
        if (ref == null) {
            if (r.entityOverlay != null) {
                r.entityOverlay.drawProjectile(batch, walnut);
            }
            return;
        }
        AnimClock clock = r.clocks.computeIfAbsent(walnut, k -> new AnimClock());
        clock.time += Math.max(0f, delta);
        float[] xy = r.layout.centerOf(walnut.getY(), walnut.getX());
        float scale = BowlingWalnutAnim.scale(walnut);
        float degrees = BowlingWalnutAnim.rollDegrees(walnut, clock.time);
        batch.flush();
        r.batchTransform.set(batch.getTransformMatrix());
        r.popTransform.set(r.batchTransform)
            .translate(xy[0], xy[1], 0f)
            .rotate(0f, 0f, 1f, degrees)
            .translate(-xy[0], -xy[1], 0f);
        batch.setTransformMatrix(r.popTransform);
        r.player.draw(batch, ref, 0f, xy[0], xy[1], scale, scale, true);
        batch.flush();
        batch.setTransformMatrix(r.batchTransform);
    }

    void drawPushable(Batch batch, GameModel model, Pushable item, float delta) {
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

    static void collectLiveCabinet(Pushable item, Set<Pushable> live) {
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

    void drawCabinet(Batch batch, Pushable cabinet, float delta) {
        Point pos = cabinet.getPosition();
        if (pos == null || r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(LawnEntityDrawConstants.ARCADE_CABINET_PAM);
        if (entry == null) {
            return;
        }
        String clip = r.catalog.resolveClip(entry, "idle");
        String damage = cabinetDamagePart(cabinet);
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE, damage);
        float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
        float x = xy[0] + arcadeArmPushDeltaX(cabinet);
        float time = r.drawPose(batch, cabinet, pose, x, xy[1], AnimScale.PLANT, LawnEntityDrawConstants.NO_PHASE,
            r.tickHitFlash(cabinet, LawnEntityRenderer.itemHp(cabinet), delta), delta);
        r.lastCabinets.put(cabinet, new LiveSnap(pose, x, xy[1], false, time));
    }

    /** Pushed ice cube: same arm-follow as the arcade cabinet, ice PAM on the grid cell. */
    void drawIceBlock(Batch batch, GameModel model, Pushable ice, float delta) {
        Point pos = ice.getPosition();
        if (pos == null || r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(TroglobiteAnim.ICE_PAM);
        if (entry == null) {
            return;
        }
        r.clips.getOrLoad(entry.path(), "idle");
        r.terrain.preloadIceBreak();
        float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
        float x = xy[0] + arcadeArmPushDeltaX(ice);
        if (ice instanceof IceBlock block
            && block.getContainedEntity() instanceof ZombieInstance occupant) {
            r.terrain.drawIcedZombieIdle(batch, occupant, model, x, xy[1], delta);
        }
        String clip = r.catalog.resolveClip(entry, "idle");
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        float time = r.drawPose(batch, ice, pose, x, xy[1], AnimScale.ZOMBIE, LawnEntityDrawConstants.NO_PHASE,
            r.tickHitFlash(ice, LawnEntityRenderer.itemHp(ice), delta), delta);
        r.lastCabinets.put(ice, new LiveSnap(pose, x, xy[1], false, time));
    }

    /** Piano rides the pianist’s cell centre — no extra offset. */
    void drawPiano(Batch batch, Pushable piano, float delta) {
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(LawnEntityDrawConstants.PIANO_PAM);
        if (entry == null) {
            return;
        }
        r.clips.getOrLoad(entry.path(), "die");
        r.clips.getOrLoad(entry.path(), "particles");
        String clip = r.catalog.resolveClip(entry, "play");
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        float x;
        float y;
        ZombieInstance pusher = piano.getPusher();
        if (pusher != null && r.zombieWorldCenter(pusher, r.xyTmp)) {
            x = r.xyTmp[0];
            y = r.xyTmp[1];
        } else {
            Point pos = piano.getPosition();
            if (pos == null) {
                return;
            }
            float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
            x = xy[0];
            y = xy[1];
        }
        float time = r.drawPose(batch, piano, pose, x, y, AnimScale.ZOMBIE, LawnEntityDrawConstants.NO_PHASE,
            r.tickHitFlash(piano, LawnEntityRenderer.itemHp(piano), delta), delta);
        r.lastCabinets.put(piano, new LiveSnap(pose, x, y, false, time));
    }

    /**
     * Barrel art lives in the pusher's walk/eat. After he dies, freeze those
     * barrel parts at the last {@code partBounds} pose. Separate barrel PAM is
     * only the break clip.
     */
    void drawBarrel(Batch batch, Pushable barrel, float delta) {
        Point pos = barrel.getPosition();
        if (pos == null || r.catalog == null) {
            return;
        }
        ZombieInstance pusher = barrel.getPusher();
        if (pusher != null && !pusher.isDead()) {
            rememberLiveBarrel(barrel, pusher);
            return;
        }
        LiveSnap leftover = r.lastCabinets.get(barrel);
        if (leftover != null && leftover.pose != null
            && BarrelRollerAnim.isPusherPam(leftover.pose.pamPath())) {
            r.seenThisFrame.add(barrel);
            drawBarrelParts(batch, leftover, r.tickHitFlash(barrel, LawnEntityRenderer.itemHp(barrel), delta));
            r.lastCabinets.put(barrel, leftover);
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(BarrelRollerAnim.BARREL_PAM);
        if (entry == null) {
            return;
        }
        r.clips.getOrLoad(entry.path(), "die");
        String clip = r.catalog.resolveClip(entry, "roll");
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
        float time = r.drawPose(batch, barrel, pose, xy[0], xy[1], AnimScale.ZOMBIE, LawnEntityDrawConstants.NO_PHASE,
            r.tickHitFlash(barrel, LawnEntityRenderer.itemHp(barrel), delta), delta);
        r.lastCabinets.put(barrel, new LiveSnap(pose, xy[0], xy[1], false, time));
    }

    /** Cache break-FX origin at the barrel parts, not the grid cell. */
    void rememberLiveBarrel(Pushable barrel, ZombieInstance pusher) {
        PamCatalog.PamEntry entry = r.catalog.byName(BarrelRollerAnim.BARREL_PAM);
        if (entry != null) {
            r.clips.getOrLoad(entry.path(), "die");
        }
        LiveSnap body = r.lastLive.get(pusher);
        if (body != null && body.pose != null) {
            r.lastCabinets.put(barrel, new LiveSnap(body.pose, body.x, body.y, body.backward, body.time));
            return;
        }
        Point pos = barrel.getPosition();
        if (pos == null) {
            return;
        }
        float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
        AnimPose pose = entry == null
            ? null
            : AnimPose.looping(entry.path(), "roll", ZombieAnimRole.IDLE);
        if (pose != null) {
            r.lastCabinets.put(barrel, new LiveSnap(pose, xy[0], xy[1], false, 0f));
        }
    }

    static float leftoverHoldPhase(LiveSnap leftover, ClipRef ref) {
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
    void drawBarrelParts(Batch batch, LiveSnap leftover, float flash) {
        if (leftover == null || leftover.pose == null) {
            return;
        }
        paintBarrelParts(batch, leftover);
        r.overlayHitFlash(batch, flash, () -> paintBarrelParts(batch, leftover));
    }

    void paintBarrelParts(Batch batch, LiveSnap leftover) {
        if (leftover == null || leftover.pose == null) {
            return;
        }
        ClipRef ref = r.clips.getOrLoad(leftover.pose.pamPath(), leftover.pose.clipName());
        float t = leftover.time;
        if (ref != null && ref.duration > 0f) {
            t = leftoverHoldPhase(leftover, ref) * ref.duration;
        }
        float s = AnimScale.ZOMBIE * leftover.pose.scale();
        float sx = leftover.pose.flipX() ? -s : s;
        r.batchTransform.set(batch.getTransformMatrix());
        r.popTransform.set(r.batchTransform)
            .translate(leftover.x, leftover.y, 0f)
            .scale(sx, s, 1f)
            .translate(-leftover.x, -leftover.y, 0f);
        batch.setTransformMatrix(r.popTransform);
        for (String part : BarrelRollerAnim.BARREL_PARTS) {
            r.player.drawPart(batch, leftover.pose.pamPath(), leftover.pose.clipName(),
                t, leftover.x, leftover.y, part);
        }
        batch.setTransformMatrix(r.batchTransform);
    }

    /** HP → {@code arcade_cabinet_damage0} (pristine) … {@code damage5} (almost gone). */
    static String cabinetDamagePart(Pushable cabinet) {
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
    float arcadeArmPushDeltaX(Pushable cabinet) {
        ZombieInstance pusher = cabinet.getPusher();
        if (pusher == null || pusher.getDefinition() == null) {
            return 0f;
        }
        PushBehavior push = (PushBehavior) pusher.getBehavior(ZombieBehaviorType.PUSH);
        if (push == null || !push.isPushing()) {
            return 0f;
        }
        PamCatalog.PamEntry entry = r.catalog.forZombie(pusher.getDefinition().getName(), null);
        if (entry == null) {
            return 0f;
        }
        ClipRef clip = r.clips.getOrLoad(entry.path(), "push");
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
    float[] arcadePushHandCurve(ClipRef clip) {
        float[] cached = r.arcadePushHandX.get(clip);
        if (cached != null) {
            return cached.length == 0 ? null : cached;
        }
        float[] xs = leftEdgeCurve(clip, LawnEntityDrawConstants.ARCADE_HAND_PART);
        r.arcadePushHandX.put(clip, xs == null ? new float[0] : xs);
        return xs;
    }

    float[] leftEdgeCurve(ClipRef clip, String part) {
        Rectangle[] frames = r.player.partBoundsByFrame(clip, part);
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

    static float sampleCurve(float[] xs, float phase) {
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

    void spawnCabinetDeath(LiveSnap snap) {
        if (snap == null || snap.pose == null) {
            return;
        }
        String pam = snap.pose.pamPath();
        if (LawnDeathPam.isPianoProp(pam)) {
            spawnPianoDeath(snap, pam);
            return;
        }
        if (BarrelRollerAnim.isBarrelPropPam(pam) || BarrelRollerAnim.isPusherPam(pam)) {
            spawnBarrelBreak(snap);
            return;
        }
        if (TroglobiteAnim.isIcePropPam(pam)) {
            r.plantStatus.spawnIceShatter(snap);
            return;
        }
        String clip = r.deathSpawn.firstLoadedClip(pam, "death", snap.pose.clipName());
        addFlashingDeath(AnimPose.once(pam, clip, ZombieAnimRole.DIE, snap.pose.visibility()),
            snap.x, snap.y);
    }

    void spawnBarrelBreak(LiveSnap snap) {
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(BarrelRollerAnim.BARREL_PAM);
        if (entry == null) {
            return;
        }
        String clip = r.catalog.resolveClip(entry, "die");
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

    void addFlashingDeath(AnimPose pose, float x, float y) {
        DeathFx fx = new DeathFx(pose, x, y);
        fx.hitFlash = LawnEntityDrawConstants.HIT_FLASH_SEC;
        r.deathFx.add(fx);
    }

    float[] barrelWorldCenter(LiveSnap snap) {
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

    Rectangle barrelPartBounds(String pam, String clip, float time) {
        if (pam == null || clip == null) {
            return null;
        }
        r.clips.getOrLoad(pam, clip);
        Rectangle union = null;
        for (String part : BarrelRollerAnim.BARREL_PARTS) {
            Rectangle partBox = r.player.partBounds(pam, clip, time, part);
            if (partBox == null) {
                continue;
            }
            if (union == null) {
                union = new Rectangle(partBox);
            } else {
                union.merge(partBox);
            }
        }
        if (union != null) {
            return union;
        }
        ClipRef ref = r.clips.getOrLoad(pam, clip);
        if (ref == null) {
            return null;
        }
        for (String part : BarrelRollerAnim.BARREL_PARTS) {
            Rectangle[] frames = r.player.partBoundsByFrame(ref, part);
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

    void spawnPianoDeath(LiveSnap snap, String pam) {
        String dieClip = r.deathSpawn.firstLoadedClip(pam, "die", snap.pose.clipName());
        Map<String, Boolean> vis = new HashMap<>();
        if (snap.pose.visibility() != null) {
            vis.putAll(snap.pose.visibility());
        }
        vis.put("_particles", Boolean.FALSE);
        for (String part : LawnEntityDrawConstants.PIANO_PARTICLE_PARTS) {
            vis.put(part, Boolean.FALSE);
        }
        r.deathFx.add(new DeathFx(
            AnimPose.once(pam, dieClip, ZombieAnimRole.DIE, vis),
            snap.x, snap.y));
        ClipRef dieRef = r.clips.getOrLoad(pam, dieClip);
        float hold = dieRef != null ? dieRef.duration : 0f;
        String particleClip = r.deathSpawn.firstLoadedClip(pam, "particles", null);
        if (particleClip == null) {
            return;
        }
        float dir = snap.backward ? -1f : 1f;
        for (int i = 0; i < LawnEntityDrawConstants.PIANO_PARTICLE_PARTS.length; i++) {
            float back = 0.1f + i * 0.1f;
            float hop = 0.85f + (i % 2) * 0.3f;
            r.deathSpawn.addLimbPop(pam, particleClip, LawnEntityDrawConstants.PIANO_PARTICLE_PARTS[i],
                snap.x, snap.y, 0f, dir, back, hop, hold, false);
        }
    }

    /** Art-measured tiles from zombie origin to the barrel centre. */
    void syncBarrelFront(ZombieInstance zombie, AnimPose pose, float time) {
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
        float tiles = -localCenterX * AnimScale.ZOMBIE * pose.scale() / r.layout.cellWidth();
        push.setBarrelFrontOffsetTiles(tiles);
    }

    /** Barrel art rides the pusher PAM; additive-flash just those parts when the barrel is hit. */
    void flashPushedBarrel(Batch batch, ZombieInstance zombie, float delta) {
        if (zombie.isDead()
            || !(zombie.getPushableItem() instanceof Barrel barrel)
            || barrel.isDestroyed()) {
            return;
        }
        r.seenThisFrame.add(barrel);
        float flash = r.tickHitFlash(barrel, LawnEntityRenderer.itemHp(barrel), delta);
        LiveSnap snap = r.lastLive.get(zombie);
        if (snap == null) {
            return;
        }
        r.overlayHitFlash(batch, flash, () -> paintBarrelParts(batch, snap));
    }
}
