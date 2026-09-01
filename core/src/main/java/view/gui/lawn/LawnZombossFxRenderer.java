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
final class LawnZombossFxRenderer {
    private final LawnEntityRenderer r;

    LawnZombossFxRenderer(LawnEntityRenderer r) {
        this.r = r;
    }

    void drawZombossFireballs(Batch batch, GameModel model, float delta) {
        if (batch == null || model == null || r.catalog == null || r.player == null) {
            return;
        }
        ZombieInstance boss = model.findZomboss();
        if (boss == null || !DarkZombossAnim.isDarkZomboss(boss)) {
            return;
        }
        ZombossBehavior behavior = (ZombossBehavior) boss.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (behavior == null) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName("ZOMBOSS_DARK_FIREBALL");
        if (entry == null) {
            return;
        }
        ClipRef fall = r.clips.getOrLoad(entry.path(), "fall");
        if (fall == null) {
            return;
        }
        float scale = AnimScale.PLANT * 0.9f;
        for (ZombossPendingImpact impact : behavior.getPendingImpacts()) {
            if (impact == null || impact.isResolved()) {
                continue;
            }
            float[] target = r.layout.centerOf(impact.getRow(), impact.getCol());
            float[] origin = r.layout.centerOf(boss.getGridY(), boss.getGridX());
            float t = impact.progress01();
            float x = origin[0] + (target[0] - origin[0]) * t;
            float y = origin[1] + (target[1] - origin[1]) * t
                    + (float) Math.sin(t * Math.PI) * r.layout.cellHeight() * 1.4f;
            float duration = Math.max(0.05f, fall.duration);
            float state = t * duration;
            r.player.draw(batch, fall, state, x, y, scale, scale, false);
        }
    }

    void drawZombossMissiles(Batch batch, GameModel model, float delta) {
        if (batch == null || model == null || r.player == null) {
            return;
        }
        ZombieInstance boss = model.findZomboss();
        if (boss == null) {
            return;
        }
        boolean egypt = EgyptZombossAnim.isEgyptZomboss(boss);
        boolean ice = IceZombossAnim.isIceZomboss(boss);
        if (!egypt && !ice) {
            return;
        }
        ZombossBehavior behavior = (ZombossBehavior) boss.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (behavior == null) {
            return;
        }
        harvestMissileExplosions(behavior);
        String missilePam = egypt
                ? EffectPamPaths.ZOMBOSS_MISSILE_EXPLOSION_EGYPT
                : EffectPamPaths.ZOMBOSS_MISSILE_EXPLOSION_ICEAGE;
        drawFallingMissiles(batch, behavior, missilePam);
    }

    private void harvestMissileExplosions(ZombossBehavior behavior) {
        if (behavior instanceof EgyptZombossBehavior egyptBoss) {
            addMissileCues(egyptBoss.drainExplosionCues(), EffectPamPaths.ZOMBOSS_MISSILE_EXPLOSION_EGYPT);
        } else if (behavior instanceof IceZombossBehavior iceBoss) {
            addMissileCues(iceBoss.drainExplosionCues(), EffectPamPaths.ZOMBOSS_MISSILE_EXPLOSION_ICEAGE);
        }
    }

    private void addMissileCues(Iterable<int[]> tiles, String pam) {
        for (int[] tile : tiles) {
            if (tile == null || tile.length < 2) {
                continue;
            }
            float[] xy = r.layout.centerOf(tile[0], tile[1]);
            r.frontEffects.add(new OneShotFx(
                    pam, EffectPamPaths.ZOMBOSS_MISSILE_EXPLOSION_CLIP,
                    xy[0], xy[1], AnimScale.PLANT, false));
        }
    }

    private void drawFallingMissiles(Batch batch, ZombossBehavior behavior, String missilePam) {
        ClipRef missile = r.clips.getOrLoad(missilePam, EffectPamPaths.ZOMBOSS_MISSILE_CLIP);
        if (missile == null) {
            return;
        }
        float scale = AnimScale.PLANT;
        float fallHeight = r.layout.cellHeight() * 3.2f;
        for (ZombossPendingImpact impact : behavior.getPendingImpacts()) {
            if (impact == null || impact.isResolved()) {
                continue;
            }
            float[] target = r.layout.centerOf(impact.getRow(), impact.getCol());
            float t = impact.progress01();
            float y = target[1] + fallHeight * (1f - t);
            float duration = Math.max(0.05f, missile.duration);
            float state = (t * impact.getTravelSeconds()) % duration;
            r.player.draw(batch, missile, state, target[0], y, scale, scale, true);
        }
    }

    void drawZombossSharks(Batch batch, GameModel model, float delta) {
        if (batch == null || model == null || r.player == null) {
            return;
        }
        ZombieInstance boss = model.findZomboss();
        if (boss == null || !BeachZombossAnim.isBeachZomboss(boss)) {
            return;
        }
        ZombossBehavior behavior = (ZombossBehavior) boss.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (!(behavior instanceof BeachZombossBehavior beach)) {
            return;
        }
        String pam = EffectPamPaths.ZOMBOSS_SHARK_PROJECTILE;
        ClipRef walk = r.clips.getOrLoad(pam, EffectPamPaths.ZOMBOSS_SHARK_WALK_CLIP);
        ClipRef submerge = r.clips.getOrLoad(pam, EffectPamPaths.ZOMBOSS_SHARK_SUBMERGE_CLIP);
        ClipRef attack = r.clips.getOrLoad(pam, EffectPamPaths.ZOMBOSS_SHARK_ATTACK_CLIP);
        if (walk == null && submerge == null && attack == null) {
            return;
        }
        float scale = AnimScale.PLANT * 0.85f;
        int spawnCol = Math.max(0, model.getColumnCount() - 1);
        for (BeachZombossPendingShark shark : beach.getPendingSharks()) {
            drawPendingShark(batch, shark, walk, submerge, attack, scale, spawnCol);
        }
    }

    private void drawPendingShark(Batch batch, BeachZombossPendingShark shark,
                                  ClipRef walk, ClipRef submerge, ClipRef attack,
                                  float scale, int spawnCol) {
        if (shark == null || shark.isResolved()) {
            return;
        }
        float[] target = r.layout.centerOf(shark.getRow(), shark.getCol());
        float[] origin = r.layout.centerOf(shark.getRow(), spawnCol);
        ClipRef ref;
        float x;
        float state;
        boolean loop;
        switch (shark.getPhase()) {
            case WALK -> {
                float t = shark.phaseProgress01();
                x = origin[0] + (target[0] - origin[0]) * t;
                ref = walk;
                state = t * Math.max(0.05f, shark.getWalkSeconds());
                loop = true;
            }
            case SUBMERGE -> {
                x = target[0];
                ref = submerge;
                state = shark.phaseProgress01() * Math.max(0.05f,
                        BeachZombossBehavior.SHARK_SUBMERGE_SECONDS);
                loop = false;
            }
            case ATTACK -> {
                x = target[0];
                ref = attack;
                state = shark.phaseProgress01() * Math.max(0.05f,
                        BeachZombossBehavior.SHARK_ATTACK_SECONDS);
                loop = false;
            }
            default -> {
                return;
            }
        }
        drawSharkClip(batch, ref, state, x, target[1], scale, loop);
    }

    private void drawSharkClip(Batch batch, ClipRef ref, float state, float x, float y,
                               float scale, boolean loop) {
        if (ref == null) {
            return;
        }
        float duration = Math.max(0.05f, ref.duration);
        float t = loop ? state % duration : Math.min(state, duration);
        r.player.draw(batch, ref, t, x, y, scale, scale, loop);
    }
}
