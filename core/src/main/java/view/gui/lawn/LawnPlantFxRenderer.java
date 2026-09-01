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
final class LawnPlantFxRenderer {
    private final LawnEntityRenderer r;

    LawnPlantFxRenderer(LawnEntityRenderer r) {
        this.r = r;
    }

    void drawPlant(Batch batch, PlantInstance plant, float delta) {
        if (r.plantStatus.drawWizardSheep(batch, plant, delta)) {
            return;
        }
        Point pos = plant.getPosition();
        if (pos == null) {
            r.seenThisFrame.add(plant);
            r.tickHitFlash(plant, LawnEntityRenderer.plantVitality(plant), delta);
            r.entityOverlay.drawPlant(batch, App.getInstance().getCurrentGameModel(), plant);
            return;
        }
        AnimPose pose = r.plantAdapter.poseFor(plant);
        if (pose == null) {
            r.seenThisFrame.add(plant);
            r.tickHitFlash(plant, LawnEntityRenderer.plantVitality(plant), delta);
            r.entityOverlay.drawPlant(batch, App.getInstance().getCurrentGameModel(), plant);
            return;
        }
        float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
        applyBeghouledMotion(plant, xy, delta);
        applySquashLeap(plant, xy);
        float[] pfXy = r.layout.centerOf(pos.getY() - 0.5f, pos.getX() + 0.1f);
        String clockKey = pose.cacheKey() + "#" + plant.getActionEpoch();
        float flash = r.tickHitFlash(plant, LawnEntityRenderer.plantVitality(plant), delta);
        float animDelta = plant.isFrozen() ? 0f : delta;
        float time = r.drawPose(batch, plant, pose, xy[0], xy[1], AnimScale.forPlant(pose),
                LawnEntityDrawConstants.NO_PHASE, flash, animDelta, clockKey);
        r.plantStatus.drawPlantChill(batch, plant, xy[0], xy[1], flash, delta);
        r.plantStatus.drawPlantFreezeIce(batch, plant, xy[0], xy[1], flash, delta);
        updateAndDrawPlantFoodFx(batch, plant, pfXy[0], pfXy[1], delta);
        r.lastPlants.put(plant, new LiveSnap(pose, xy[0], xy[1], false, time));
    }

    /** Squash leaps from its tile onto the captured smash target during ATTACKING. */
    void applySquashLeap(PlantInstance plant, float[] xy) {
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
        PamCatalog.PamEntry entry = r.catalog == null ? null
                : r.catalog.forPlant(plant.getDefinition().getName());
        float[] to = r.layout.centerOf(explosive.getSmashTargetGridY(), explosive.getSmashTargetGridX());
        float dx = to[0] - xy[0];
        float dy = to[1] - xy[1];
        float travel = SquashAnim.leapTravelFraction(plant, entry);
        if (travel > 0f) {
            xy[0] += dx * travel;
            xy[1] += dy * travel;
        }
        float travelTiles = r.layout.cellWidth() > 0f
                ? (float) Math.sqrt(dx * dx + dy * dy) / r.layout.cellWidth()
                : 1f;
        xy[1] += SquashAnim.leapVisualHeightCells(plant, entry, travelTiles) * r.layout.cellHeight();
    }

    void maybeSpawnPlantExplosion(PlantInstance plant, IdentityHashMap<PlantInstance, float[]> deathBlastNow) {
        Point pos = plant.getPosition();
        if (pos == null) {
            return;
        }
        float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
        maybeSpawnArmorBreakExplosion(plant, xy[0], xy[1]);
        if (ExplosivePlantFx.isDeathDetonator(plant)) {
            deathBlastNow.put(plant, new float[]{xy[0], xy[1]});
            return;
        }
        AnimPose pose = r.plantAdapter.poseFor(plant);
        if (!ExplosivePlantFx.shouldSpawn(plant, pose)) {
            return;
        }
        if (r.explosionSpawned.put(plant, Boolean.TRUE) != null) {
            return;
        }
        spawnExplosionSpecs(ExplosivePlantFx.specsFor(plant), pos, xy[0], xy[1]);
    }

    void maybeSpawnArmorBreakExplosion(PlantInstance plant, float x, float y) {
        if (plant == null || plant.getArmorBreakEpoch() <= 0) {
            return;
        }
        if (!ExplosivePlantFx.isDeathDetonator(plant) && !plant.armorExplodesOnBreak()) {
            return;
        }
        int epoch = plant.getArmorBreakEpoch();
        Integer last = r.armorBreakFxEpoch.get(plant);
        if (last != null && last == epoch) {
            return;
        }
        r.armorBreakFxEpoch.put(plant, epoch);
        spawnExplosionSpecs(ExplosivePlantFx.specsFor(plant), plant.getPosition(), x, y);
    }

    void maybeSpawnMeleeFx(PlantInstance plant) {
        Point pos = plant.getPosition();
        if (pos == null) {
            return;
        }
        r.seenThisFrame.add(plant);
        AnimPose pose = r.plantAdapter.poseFor(plant);
        if (!MeleePlantFx.shouldSpawn(plant, pose)) {
            r.meleePlantFoodFxSpawned.remove(plant);
            return;
        }
        boolean plantFood = plant.getState() == PlantState.PLANT_FOOD;
        if (plantFood) {
            if (r.meleePlantFoodFxSpawned.put(plant, Boolean.TRUE) != null) {
                return;
            }
        } else {
            r.meleePlantFoodFxSpawned.remove(plant);
            int epoch = plant.getActionEpoch();
            Integer last = r.meleeAttackFxEpoch.get(plant);
            if (last != null && last == epoch) {
                return;
            }
            r.meleeAttackFxEpoch.put(plant, epoch);
        }
        float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
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

    void updateMeleeIdlePulse(PlantInstance plant) {
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
        if (r.meleeIdlePulses.containsKey(plant)) {
            return;
        }
        float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
        OneShotFx fx = addEffect(toExplosiveLayer(spec.layer()), spec.pamPath(), spec.clipName(),
                xy[0], xy[1], AnimScale.PLANT, true);
        r.meleeIdlePulses.put(plant, fx);
    }

    void removeMeleeIdlePulse(PlantInstance plant) {
        OneShotFx fx = r.meleeIdlePulses.remove(plant);
        if (fx == null) {
            return;
        }
        r.backEffects.remove(fx);
        r.frontEffects.remove(fx);
    }

    void spawnMeleeTileHits(MeleePlantFx.Spec spec, Point pos, int radius) {
        int row = pos.getY();
        int col = pos.getX();
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                int tileRow = row + dr;
                int tileCol = col + dc;
                if (tileRow < 0 || tileCol < 0
                        || tileRow >= r.layout.rows() || tileCol >= r.layout.cols()) {
                    continue;
                }
                float[] tile = r.layout.centerOf(tileRow, tileCol);
                addEffect(toExplosiveLayer(spec.layer()), spec.pamPath(), spec.clipName(),
                        tile[0], tile[1], AnimScale.PLANT, false);
            }
        }
    }

    static ExplosivePlantFx.Layer toExplosiveLayer(MeleePlantFx.Layer layer) {
        return layer == MeleePlantFx.Layer.BACK ? ExplosivePlantFx.Layer.BACK : ExplosivePlantFx.Layer.FRONT;
    }

    void spawnMissingDeathBlasts(IdentityHashMap<PlantInstance, float[]> deathBlastNow) {
        for (var entry : r.deathBlastSeen.entrySet()) {
            if (deathBlastNow.containsKey(entry.getKey())) {
                continue;
            }
            float[] xy = entry.getValue();
            spawnExplosionSpecs(ExplosivePlantFx.specsFor(entry.getKey()), null, xy[0], xy[1]);
        }
    }

    void spawnExplosionSpecs(List<ExplosivePlantFx.Spec> specs, Point pos, float x, float y) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        if (r.screenShake != null) {
            r.screenShake.pulse();
        }
        for (ExplosivePlantFx.Spec spec : specs) {
            if (spec.placement() == ExplosivePlantFx.Placement.ALONG_LANE && pos != null) {
                int row = pos.getY();
                for (int col = 0; col < r.layout.cols(); col++) {
                    float[] tile = r.layout.centerOf(row, col);
                    addEffect(spec.layer(), spec.pamPath(), ExplosivePlantFx.jalapenoClip(col),
                            tile[0], tile[1], AnimScale.PLANT, false);
                }
            } else {
                addEffect(spec.layer(), spec.pamPath(), spec.clipName(), x, y, AnimScale.PLANT, false);
            }
        }
    }

    OneShotFx addEffect(ExplosivePlantFx.Layer layer, String pamPath, String clipName,
                                float x, float y, float scale, boolean loop) {
        OneShotFx fx = new OneShotFx(pamPath, clipName, x, y, scale, loop);
        if (layer == ExplosivePlantFx.Layer.BACK) {
            r.backEffects.add(fx);
        } else {
            r.frontEffects.add(fx);
        }
        return fx;
    }

    void harvestProjectileHits(GameModel model) {
        List<Projectile> hits = model.drainProjectileHits();
        for (Projectile projectile : hits) {
            if (projectile == null) {
                continue;
            }
            if (GameplayCombatSfx.zombieGotShotEnabled) {
                GameAudio.get().playSfx(GameSfx.ZOMBIE_GOT_SHOT);
            }
            projectileWorldCenter(projectile, r.xyTmp);
            spawnProjectileHit(projectile, r.xyTmp[0], r.xyTmp[1]);
        }
    }

    void spawnProjectileHit(Projectile projectile, float x, float y) {
        ProjectilePamPaths.HitPam hit = ProjectilePamPaths.hitFor(projectile);
        if (hit == null || hit.path() == null) {
            return;
        }
        String clip = hit.clip() != null ? hit.clip() : ProjectilePamPaths.CLIP_PREFERENCES[0];
        r.frontEffects.add(new OneShotFx(hit.path(), clip, x, y, AnimScale.PROJECTILE, false));
    }

    void drawEffects(Batch batch, List<OneShotFx> effects, float delta) {
        Iterator<OneShotFx> it = effects.iterator();
        while (it.hasNext()) {
            OneShotFx fx = it.next();
            ClipRef ref = r.clips.getOrLoad(fx.pamPath, fx.clipName);
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
            r.player.draw(batch, ref, fx.time, fx.x, fx.y, fx.scale, fx.scale, fx.loop);
            if (!fx.loop && fx.duration > 0f && fx.time >= fx.duration) {
                it.remove();
            }
        }
    }

    float effectClipDurationSeconds(ClipRef ref, String pamPath, String clipName) {
        float seconds = r.player.clipDurationSeconds(pamPath, clipName);
        if (seconds > 0f) {
            return seconds;
        }
        if (ref != null && ref.duration > 0f) {
            return ref.duration;
        }
        return 1.5f;
    }

    void drawProjectile(Batch batch, Projectile projectile, float delta) {
        if (projectile == null) {
            return;
        }
        AnimPose pose = r.projectileAdapter.poseFor(projectile);
        if (pose == null) {
            r.entityOverlay.drawProjectile(batch, projectile);
            return;
        }
        projectileWorldCenter(projectile, r.xyTmp);
        r.drawPose(batch, projectile, pose, r.xyTmp[0], r.xyTmp[1], AnimScale.forProjectile(pose),
                LawnEntityDrawConstants.NO_PHASE,
                0f, delta, pose.cacheKey());
    }

    void projectileWorldCenter(Projectile projectile, float[] out) {
        float[] xy = r.layout.centerOf(projectile.getY(), projectile.getX());
        out[0] = xy[0];
        out[1] = xy[1];
        if (projectile instanceof Splash splash) {
            out[1] += splash.getVisualHeight() * r.layout.cellHeight();
        }
    }

    void harvestBeghouledClears(GameModel model) {
        if (!(model.getCurrentLevel() instanceof BeghouledLevel beghouled)) {
            return;
        }
        for (int[] cell : beghouled.consumeLastClearedCells()) {
            if (cell == null || cell.length < 2) {
                continue;
            }
            float[] xy = r.layout.centerOf(cell[0], cell[1]);
            r.frontEffects.add(new OneShotFx(
                    EffectPamPaths.PLANTFOOD_FX, EffectPamPaths.PLANTFOOD_FX_ON,
                    xy[0], xy[1], AnimScale.PLANT * 0.85f, false));
        }
    }

    void applyBeghouledMotion(PlantInstance plant, float[] xy, float delta) {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (!(model != null && model.getCurrentLevel() instanceof BeghouledLevel)) {
            r.beghouledMotion.remove(plant);
            return;
        }
        BeghouledMotion motion = r.beghouledMotion.get(plant);
        if (motion == null) {
            motion = new BeghouledMotion();
            motion.toX = xy[0];
            motion.toY = xy[1];
            motion.fromX = xy[0];
            motion.fromY = xy[1] + r.layout.cellHeight() * 1.15f;
            motion.t = 0f;
            r.beghouledMotion.put(plant, motion);
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
        motion.t = Math.min(1f, motion.t + Math.max(0f, delta) / LawnEntityRenderer.BEGHOULED_MOVE_SEC);
        float u = beghouledEase(motion.t);
        xy[0] = motion.fromX + (motion.toX - motion.fromX) * u;
        xy[1] = motion.fromY + (motion.toY - motion.fromY) * u;
    }

    static float beghouledEase(float t) {
        float u = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - u) * (1f - u);
    }

    /** Idle PAM at a world point — drag-to-plant cursor ghost. */
    void drawPlantIdle(Batch batch, String plantName, float x, float y, float time) {
        drawPlantIdle(batch, plantName, x, y, time, AnimScale.PLANT);
    }

    void drawPlantIdle(Batch batch, String plantName, float x, float y, float time, float scale) {
        String name = resolveIdlePlantName(plantName);
        ClipRef ref = plantIdleClip(name);
        if (ref != null) {
            r.player.draw(batch, ref, time, x, y, scale, scale, true);
            return;
        }
        PlantSpritesheetCatalog.ClipSpec spec = plantIdleSheetSpec(name);
        if (spec == null || r.sheetClips == null) {
            return;
        }
        SpritesheetClipCache.SheetAnim sheet = r.sheetClips.getOrLoad(spec);
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

    void preloadPlantIdle(String plantName) {
        String name = resolveIdlePlantName(plantName);
        if (plantIdleClip(name) != null) {
            return;
        }
        PlantSpritesheetCatalog.ClipSpec spec = plantIdleSheetSpec(name);
        if (spec != null && r.sheetClips != null) {
            r.sheetClips.getOrLoad(spec);
        }
    }

    static String resolveIdlePlantName(String plantName) {
        if ("Giant Wall-nut".equalsIgnoreCase(plantName)) {
            return "Wall-nut";
        }
        return plantName;
    }

    ClipRef plantIdleClip(String plantName) {
        if (plantName == null || r.catalog == null) {
            return null;
        }
        PamCatalog.PamEntry entry = r.catalog.forPlant(plantName);
        if (entry == null) {
            return null;
        }
        String clip = r.catalog.resolveClip(entry, "idle", "idle2", "idle1", "loop");
        return clip == null ? null : r.clips.getOrLoad(entry.path(), clip);
    }

    PlantSpritesheetCatalog.ClipSpec plantIdleSheetSpec(String plantName) {
        if (plantName == null || r.plantSheets == null) {
            return null;
        }
        PlantSpritesheetCatalog.ClipSpec spec = r.plantSheets.resolveClip(plantName, "idle", "idle2");
        if (spec != null) {
            return spec;
        }
        return r.plantSheets.idleFallback(plantName);
    }

    void updateAndDrawPlantFoodFx(Batch batch, PlantInstance plant, float x, float y, float delta) {
        boolean active = plant.isPlantFoodActive() || plant.getState() == PlantState.PLANT_FOOD;
        PlantFoodFx fx = r.plantFoodFx.get(plant);
        if (!active && fx == null) {
            return;
        }
        if (fx == null) {
            fx = new PlantFoodFx();
            r.plantFoodFx.put(plant, fx);
            preloadPlantFoodFx();
        }
        if (!active && fx.phase != PlantFoodFxPhase.OFF) {
            fx.phase = PlantFoodFxPhase.OFF;
            fx.time = 0f;
        }

        String clip = plantFoodFxClip(fx.phase);
        ClipRef ref = r.clips.getOrLoad(EffectPamPaths.PLANTFOOD_FX, clip);
        if (ref == null) {
            if (!active) {
                r.plantFoodFx.remove(plant);
            }
            return;
        }
        float duration = effectClipDurationSeconds(ref, EffectPamPaths.PLANTFOOD_FX, clip);
        boolean loop = fx.phase == PlantFoodFxPhase.LOOP;
        fx.time += delta;
        r.player.draw(batch, ref, fx.time, x, y, AnimScale.PLANT, AnimScale.PLANT, loop);

        if (fx.phase == PlantFoodFxPhase.ON && duration > 0f && fx.time >= duration) {
            fx.phase = PlantFoodFxPhase.LOOP;
            fx.time = 0f;
        } else if (fx.phase == PlantFoodFxPhase.OFF && duration > 0f && fx.time >= duration) {
            r.plantFoodFx.remove(plant);
        } else if (fx.phase == PlantFoodFxPhase.OFF && duration <= 0f) {
            r.plantFoodFx.remove(plant);
        } else if (fx.phase == PlantFoodFxPhase.ON && duration <= 0f) {
            fx.phase = active ? PlantFoodFxPhase.LOOP : PlantFoodFxPhase.OFF;
            fx.time = 0f;
        }
    }

    void preloadPlantFoodFx() {
        r.clips.getOrLoad(EffectPamPaths.PLANTFOOD_FX, EffectPamPaths.PLANTFOOD_FX_ON);
        r.clips.getOrLoad(EffectPamPaths.PLANTFOOD_FX, EffectPamPaths.PLANTFOOD_FX_LOOP);
        r.clips.getOrLoad(EffectPamPaths.PLANTFOOD_FX, EffectPamPaths.PLANTFOOD_FX_OFF);
    }

    static String plantFoodFxClip(PlantFoodFxPhase phase) {
        return switch (phase) {
            case ON -> EffectPamPaths.PLANTFOOD_FX_ON;
            case LOOP -> EffectPamPaths.PLANTFOOD_FX_LOOP;
            case OFF -> EffectPamPaths.PLANTFOOD_FX_OFF;
        };
    }
}
