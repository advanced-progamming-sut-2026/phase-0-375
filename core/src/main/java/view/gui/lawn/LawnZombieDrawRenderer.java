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

final class LawnZombieDrawRenderer {
    private final LawnEntityRenderer r;

    LawnZombieDrawRenderer(LawnEntityRenderer r) {
        this.r = r;
    }

    void drawZombieIdle(Batch batch, String zombieName, float x, float y, float time,
                               Chapter chapter) {
        if (zombieName == null || r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.forZombie(zombieName, chapter);
        if (entry == null) {
            return;
        }
        String clip = r.catalog.resolveClip(entry, "idle", "walk", "idle2", "idle1");
        if (clip == null) {
            return;
        }
        ClipRef ref = r.clips.getOrLoad(entry.path(), clip);
        if (ref != null) {
            Map<String, Boolean> visibility = ZombieAnimAdapter.almanacArmorVisibility(zombieName, entry);
            if (ZombotanyAnim.isPlantHeadName(zombieName)) {
                visibility = ZombotanyAnim.headHiddenVisibility(visibility);
            }
            if (visibility != null) {
                r.player.draw(batch, ref, time, x, y, AnimScale.ZOMBIE, AnimScale.ZOMBIE, true, visibility);
            } else {
                r.player.draw(batch, ref, time, x, y, AnimScale.ZOMBIE, AnimScale.ZOMBIE, true);
            }
            if (ZombotanyAnim.isPlantHeadName(zombieName)) {
                r.zombotany.drawZombotanyPlantHeadIdle(batch, zombieName, chapter, entry, ref, time, x, y);
            }
        }
    }

    void preloadZombieIdle(String zombieName, Chapter chapter) {
        if (zombieName == null || r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.forZombie(zombieName, chapter);
        if (entry == null) {
            return;
        }
        String clip = r.catalog.resolveClip(entry, "idle", "walk", "idle2", "idle1");
        if (clip != null) {
            r.clips.getOrLoad(entry.path(), clip);
        }
        if (ZombotanyAnim.isPlantHeadName(zombieName)) {
            String plantName = ZombotanyAnim.plantDefinitionName(zombieName);
            PamCatalog.PamEntry plant = plantName == null ? null : r.catalog.forPlant(plantName);
            if (plant != null) {
                String plantClip = r.catalog.resolveClip(plant, "idle", "attack", "jump_up_left", "jump_up_right");
                if (plantClip != null) {
                    r.clips.getOrLoad(plant.path(), plantClip);
                }
            }
        }
    }

    void drawZombie(Batch batch, ZombieInstance zombie, Chapter chapter, float delta) {
        AnimPose pose = r.zombieAdapter.poseFor(zombie, chapter);
        if (pose == null) {
            r.entityOverlay.drawZombie(batch, App.getInstance().getCurrentGameModel(), zombie);
            return;
        }
        boolean backward = zombie.isMovingBackward();
        if (backward) {
            pose = pose.withFlipX(true);
        }
        if (!r.zombieWorldCenter(zombie, r.xyTmp)) {
            r.entityOverlay.drawZombie(batch, App.getInstance().getCurrentGameModel(), zombie);
            return;
        }
        r.zombotany.applyZombotanySquashLeap(zombie, chapter, r.xyTmp);
        if (!zombie.isFrozen()) {
            restartLiveZombieClocks(zombie, pose);
        }
        drawZombieAtWorld(batch, zombie, pose, backward, delta);
    }

    private void restartLiveZombieClocks(ZombieInstance zombie, AnimPose pose) {
        r.zombieSpecial.restartArcadePushClock(zombie, pose);
        r.zombieSpecial.restartProspectorJumpClock(zombie, pose);
        r.zombieSpecial.restartTombRaiseClock(zombie, pose);
        r.zombieSpecial.restartDodoFlyClock(zombie, pose);
        r.zombieSpecial.restartHunterThrowClock(zombie, pose);
        r.zombieSpecial.restartJugglerSpinClock(zombie, pose);
        r.octopus.restartOctopusTossClock(zombie, pose);
        r.zombieSpecial.restartFishermanClock(zombie, pose);
        r.zombieSpecial.restartDarkKingClock(zombie, pose);
        r.zombossClock.restartDarkZombossClock(zombie, pose);
        r.zombossClock.restartEgyptZombossClock(zombie, pose);
        r.zombossClock.restartIceZombossClock(zombie, pose);
        r.zombossClock.restartBeachZombossClock(zombie, pose);
        r.zombieSpecial.restartWizardSheepClock(zombie, pose);
        r.deaths.spawnHunterSplat(zombie);
    }

    private void drawZombieAtWorld(Batch batch, ZombieInstance zombie, AnimPose pose,
                                   boolean backward, float delta) {
        float x = r.xyTmp[0];
        float y = r.xyTmp[1];
        if (SunshineAnim.isSunshine(zombie)) {
            y += SunshineAnim.drawOffsetY(r.layout.cellHeight());
        }
        if (DarkZombossAnim.isDarkZomboss(zombie) || EgyptZombossAnim.isEgyptZomboss(zombie)
                || BeachZombossAnim.isBeachZomboss(zombie)) {
            y -= r.layout.cellHeight();
        }
        float modelX = x;
        ThrowImpBehavior.Flight flight = ThrowImpBehavior.flightOf(zombie);
        if (flight != null) {
            r.zombieSpecial.alignToss(zombie, flight, pose, x, y);
            float t = flight.progress();
            float[] align = r.tossAlign.get(zombie);
            if (align != null) {
                x += align[0] * (1f - t);
                y += align[1] * (1f - t);
            }
            y += flight.heightTiles() * r.layout.cellHeight();
        }
        JumpBehavior jump = (JumpBehavior) zombie.getBehavior(ZombieBehaviorType.JUMP);
        if (jump != null) {
            y += jump.heightPx();
            if (jump.getPhase() == JumpBehavior.JumpPhase.COUNTDOWN) {
                r.clips.getOrLoad(pose.pamPath(), "blastoff");
                r.clips.getOrLoad(pose.pamPath(), "fly");
                r.clips.getOrLoad(pose.pamPath(), "land");
                r.deaths.preloadProspectorBlast();
            } else if (jump.getPhase() == JumpBehavior.JumpPhase.JUMPING) {
                r.deaths.spawnProspectorBlast(zombie, jump);
            }
        }
        ClipRef ref = pose.isSpritesheet() ? null : r.clips.getOrLoad(pose.pamPath(), pose.clipName());
        float phase = resolveZombiePhase(zombie, pose, ref, jump, backward);
        if (ref != null && gaitFor(zombie).enabled()
                && ZombieAnimAdapter.isDistanceDriven(zombie, pose)
                && !(jump != null && jump.getPhase() == JumpBehavior.JumpPhase.JUMPING)) {
            phase = gaitFor(zombie).phaseAt(backward ? r.xyTmp[2] : -r.xyTmp[2]);
            x += gaitFor(zombie).footLockOffsetTiles(phase, footfallFor(gaitFor(zombie), ref))
                    * r.layout.cellWidth();
        }
        finishZombieDraw(batch, zombie, pose, ref, jump, x, y, modelX, phase, delta);
    }

    private float resolveZombiePhase(ZombieInstance zombie, AnimPose pose, ClipRef ref,
                                     JumpBehavior jump, boolean backward) {
        if (ref != null && jump != null && jump.getPhase() == JumpBehavior.JumpPhase.JUMPING
                && ref.duration > 0f) {
            return r.zombieSpecial.prospectorJumpPhase(jump, pose.clipName(), ref.duration);
        }
        if (ref != null && gaitFor(zombie).enabled()
                && ZombieAnimAdapter.isDistanceDriven(zombie, pose)) {
            return gaitFor(zombie).phaseAt(backward ? r.xyTmp[2] : -r.xyTmp[2]);
        }
        float zombossPhase = r.zombossClock.darkZombossClipPhase(zombie, pose, ref);
        if (zombossPhase < 0f) {
            zombossPhase = r.zombossClock.egyptZombossClipPhase(zombie, pose, ref);
        }
        if (zombossPhase < 0f) {
            zombossPhase = r.zombossClock.iceZombossClipPhase(zombie, pose, ref);
        }
        if (zombossPhase < 0f) {
            zombossPhase = r.zombossClock.beachZombossClipPhase(zombie, pose, ref);
        }
        return zombossPhase >= 0f ? zombossPhase : LawnEntityDrawConstants.NO_PHASE;
    }

    private void finishZombieDraw(Batch batch, ZombieInstance zombie, AnimPose pose, ClipRef ref,
                                  JumpBehavior jump, float x, float y, float modelX, float phase,
                                  float delta) {
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
            Rectangle skull = ref != null ? r.zombieSpecial.partAt(ref, measureT, SnorkelerAnim.SKULL_PART) : null;
            if (skull == null) {
                skull = r.zombieSpecial.partAt(r.clips.getOrLoad(pose.pamPath(), "walk"), 0f, SnorkelerAnim.SKULL_PART);
            }
            snorkelWaterY = SnorkelerAnim.waterLineY(r.layout, zombie.getGridY());
            y = SnorkelerAnim.drawOriginY(standY, snorkelWaterY, skull, scale, swim.getRise());
            snorkelMask = FishermanAnim.drownMaskWorld(r.layout, x, zombie.getGridY(), snorkelWaterY);
            rippleX = SnorkelerAnim.skullCenterWorldX(x, skull, scale, pose.flipX());
        } else {
            WaterEmerge emerge = r.waterEmerges.get(zombie);
            if (emerge != null && emerge.progress() < 1f - 1e-3f) {
                snorkelWaterY = SnorkelerAnim.waterLineY(r.layout, zombie.getGridY());
                Rectangle box = pose.isSpritesheet() ? null : r.player.bounds(pose.pamPath(), pose.clipName());
                float artTop = box != null ? standY - box.y * scale : standY + r.layout.cellHeight();
                float extraSink = FishermanAnim.emergeExtraSink(r.layout.cellHeight(), zombie);
                y = WaterEmerge.drawOriginY(standY, snorkelWaterY, artTop, emerge.progress(), extraSink);
                snorkelMask = FishermanAnim.drownMaskWorld(
                        r.layout, modelX, zombie.getGridY(), snorkelWaterY,
                        FishermanAnim.emergeMaskWidthTiles(zombie),
                        FishermanAnim.EMERGE_MASK_BELOW_TILES);
                rippleX = modelX;
            } else if (r.zombieSpecial.shouldRippleOnWater(zombie, model, swim, jump)) {
                snorkelWaterY = SnorkelerAnim.waterLineY(r.layout, zombie.getGridY());
                rippleX = modelX;
            }
        }
        paintZombieSprite(batch, zombie, pose, x, y, baseScale, phase, delta,
                snorkelMask, snorkelWaterY, rippleX);
    }

    private void paintZombieSprite(Batch batch, ZombieInstance zombie, AnimPose pose,
                                   float x, float y, float baseScale, float phase, float delta,
                                   Rectangle snorkelMask, float snorkelWaterY, float rippleX) {
        if (snorkelMask != null) {
            r.drownShader().begin(batch, snorkelMask);
        }
        r.deathSpawn.maybePopLostHand(zombie, pose, x, y);
        if (r.lostHands.containsKey(zombie)) {
            pose = pose.withHiddenParts(r.deathSpawn.lostArmBodyParts(pose.pamPath()));
        }
        float glow = zombie.isGlowing() && snorkelMask == null ? LawnEntityRenderer.glowStrength() : 0f;
        float chill = (zombie.isChilled() || zombie.isFrozen()) && snorkelMask == null ? 1.0f : 0f;
        float danger;
        if (DangerRedShader.isZombieInDangerZone(zombie) && snorkelMask == null) {
            float elapsed = r.zombieDangerElapsed.getOrDefault(zombie, 0f) + delta;
            r.zombieDangerElapsed.put(zombie, elapsed);
            danger = DangerRedShader.dangerStrength(elapsed);
        } else {
            r.zombieDangerElapsed.remove(zombie);
            danger = 0f;
        }
        float animDelta = zombie.isFrozen() ? 0f : delta;
        float flash = r.tickHitFlash(zombie, delta);
        float time = r.drawPose(batch, zombie, pose, x, y, baseScale, phase,
                flash, animDelta, pose.cacheKey(), glow, chill, danger);
        if (ZombotanyAnim.isPlantHead(zombie)) {
            r.zombotany.drawZombotanyPlantHead(batch, zombie, pose, x, y, time, animDelta, flash, glow, chill);
        }
        if (zombie.isFrozen()) {
            r.plantStatus.drawZombieFreezeChill(batch, zombie, x, y, r.tickHitFlash(zombie, delta), delta);
        }
        r.zombotany.maybeSpawnZombotanyJalapenoFire(zombie);
        r.zombieSpecial.maybeGargantuarWalkStomp(zombie, pose, time);
        finishZombieOverlays(batch, zombie, pose, x, y, time, delta, snorkelMask, snorkelWaterY, rippleX);
    }

    private void finishZombieOverlays(Batch batch, ZombieInstance zombie, AnimPose pose,
                                      float x, float y, float time, float delta,
                                      Rectangle snorkelMask, float snorkelWaterY, float rippleX) {
        if (snorkelMask != null) {
            r.drownShader().end(batch);
        }
        if (!Float.isNaN(snorkelWaterY)) {
            r.zombieSpecial.drawSnorkelRipple(batch, pose, zombie, rippleX, snorkelWaterY);
        }
        if (zombie.isGlowing()) {
            r.pickup.drawGlowingPlantFoodOverlay(batch, zombie, x, y, delta);
        }
        r.deaths.popBrokenArmor(zombie, pose, x, y);
        r.lastLive.put(zombie, new LiveSnap(pose, x, y,
                zombie.isMovingBackward() || pose.flipX(), time));
        r.octopus.captureOctopusRelease(zombie, pose, x, y, time);
        r.zombieSpecial.maybeDrawCrystalSkullBeam(batch, pose, x, y, time);
        r.prop.syncBarrelFront(zombie, pose, time);
        r.prop.flashPushedBarrel(batch, zombie, delta);
    }

    static ZombieGait gaitFor(ZombieInstance zombie) {
        return ZombieGaitProfiles.forZombie(
            zombie.getDefinition() == null ? null : zombie.getDefinition().getName());
    }

    /** Measuring walks every frame of the clip, so each walk cycle is read from the art once. */
    ZombieFootfallCurve footfallFor(ZombieGait gait, ClipRef walkClip) {
        ZombieFootfallCurve footfall = r.footfalls.get(walkClip);
        if (footfall == null) {
            footfall = gait.measureFootfall(r.player, walkClip);
            r.footfalls.put(walkClip, footfall);
        }
        return footfall;
    }
}
