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

final class LawnDeathFxRenderer {
    private final LawnEntityRenderer r;

    LawnDeathFxRenderer(LawnEntityRenderer r) {
        this.r = r;
    }

    void preloadProspectorBlast() {
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry blast = r.catalog.byName(LawnEntityDrawConstants.PROSPECTOR_BLAST_PAM);
        if (blast == null) {
            return;
        }
        for (String clip : LawnEntityDrawConstants.PROSPECTOR_BLAST_CLIPS) {
            r.clips.getOrLoad(blast.path(), r.catalog.resolveClip(blast, clip));
        }
    }

    /** Ground burst at the fuse tile; stays put while the body flies. */
    void spawnProspectorBlast(ZombieInstance zombie, JumpBehavior jump) {
        if (zombie == null || jump == null || r.prospectorBlastSpawned.containsKey(zombie)) {
            return;
        }
        preloadProspectorBlast();
        PamCatalog.PamEntry blast = r.catalog == null ? null :
                r.catalog.byName(LawnEntityDrawConstants.PROSPECTOR_BLAST_PAM);
        if (blast == null) {
            return;
        }
        float[] xy = r.layout.centerOf(zombie.getGridY(), jump.getLaunchX());
        r.prospectorBlasts.add(new BlastFx(blast.path(), xy[0], xy[1]));
        r.prospectorBlastSpawned.put(zombie, Boolean.TRUE);
        if (r.screenShake != null) {
            r.screenShake.pulse();
        }
    }

    void drawProspectorBlasts(Batch batch, float delta, int row) {
        float scale = AnimScale.ZOMBIE;
        for (int i = r.prospectorBlasts.size() - 1; i >= 0; i--) {
            BlastFx fx = r.prospectorBlasts.get(i);
            if (r.layout.rowAt(fx.y) != row) {
                continue;
            }
            float maxDuration = 0f;
            boolean drew = false;
            for (String clip : LawnEntityDrawConstants.PROSPECTOR_BLAST_CLIPS) {
                ClipRef ref = r.clips.getOrLoad(fx.pamPath, clip);
                if (ref == null) {
                    continue;
                }
                maxDuration = Math.max(maxDuration, ref.duration);
                if (fx.time < ref.duration) {
                    r.player.draw(batch, ref, fx.time, fx.x, fx.y, scale, scale, false);
                    drew = true;
                }
            }
            if (!drew && (maxDuration <= 0f || fx.time >= maxDuration)) {
                r.prospectorBlasts.remove(i);
                continue;
            }
            fx.time += delta;
        }
    }

    void spawnHunterSplat(ZombieInstance zombie) {
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.isIceAgeHunter(zombie)) {
            return;
        }
        if (shoot.isThrowing()) {
            preloadHunterSplat();
        }
        int seq = shoot.getSnowballSplatSeq();
        int seen = r.hunterSplatSeq.getOrDefault(zombie, 0);
        if (seq <= seen) {
            return;
        }
        r.hunterSplatSeq.put(zombie, seq);
        Point at = shoot.getLastSnowballSplatAt();
        if (at == null || r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry splat = r.catalog.byName(HunterAnim.SPLAT_PAM);
        if (splat == null) {
            return;
        }
        String clip = r.catalog.resolveClip(splat, "animation");
        float[] xy = r.layout.centerOf(at.getY(), at.getX());
        for (int n = seen; n < seq; n++) {
            r.hunterSplats.add(new BlastFx(splat.path(), clip, xy[0], xy[1]));
        }
    }

    void preloadHunterSplat() {
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry splat = r.catalog.byName(HunterAnim.SPLAT_PAM);
        if (splat != null) {
            r.clips.getOrLoad(splat.path(), r.catalog.resolveClip(splat, "animation"));
        }
    }

    void drawHunterSplats(Batch batch, float delta, int row) {
        float scale = AnimScale.PLANT;
        for (int i = r.hunterSplats.size() - 1; i >= 0; i--) {
            BlastFx fx = r.hunterSplats.get(i);
            if (r.layout.rowAt(fx.y) != row) {
                continue;
            }
            ClipRef ref = r.clips.getOrLoad(fx.pamPath, fx.clip != null ? fx.clip : "animation");
            if (ref == null || fx.time >= ref.duration) {
                r.hunterSplats.remove(i);
                continue;
            }
            r.player.draw(batch, ref, fx.time, fx.x, fx.y, scale, scale, false);
            fx.time += delta;
        }
    }

    /** Helm/bucket/brick/crown/shoulder: last damage sprite hops off when the piece leaves. */
    void popBrokenArmor(ZombieInstance zombie, AnimPose pose, float x, float y) {
        HitFlash flash = r.hitFlashes.get(zombie);
        if (flash == null) {
            flash = new HitFlash();
            flash.vitality = r.vitality(zombie);
            r.hitFlashes.put(zombie, flash);
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
                float hopTime = 2f * LawnEntityDrawConstants.ARMOR_POP_HOP / -LawnEntityDrawConstants.ARMOR_POP_GRAVITY;
                r.armorPops.add(new ArmorPop(
                    pose.pamPath(), pose.clipName(), part,
                    x, y, y - r.layout.cellHeight() * 0.5f,
                    dir * LawnEntityDrawConstants.ARMOR_POP_BACK_TILES * r.layout.cellWidth() / hopTime,
                    LawnEntityDrawConstants.ARMOR_POP_HOP * r.layout.cellHeight(),
                    LawnEntityDrawConstants.ARMOR_POP_GRAVITY * r.layout.cellHeight()));
            }
        }
        flash.prevDroppables = living;
    }

    void drawArmorPops(Batch batch, float delta, int row) {
        for (int i = r.armorPops.size() - 1; i >= 0; i--) {
            ArmorPop pop = r.armorPops.get(i);
            if (r.layout.rowAt(pop.groundY + r.layout.cellHeight() * 0.5f) != row) {
                continue;
            }
            if (stepArmorPop(pop, delta)) {
                r.armorPops.remove(i);
                continue;
            }
            paintArmorPop(batch, pop);
        }
    }

    private boolean stepArmorPop(ArmorPop pop, float delta) {
        pop.life += delta;
        if (!pop.grounded) {
            pop.vy += pop.gravity * delta;
            pop.x += pop.vx * delta;
            pop.y += pop.vy * delta;
            if (pop.y <= pop.groundY) {
                pop.y = pop.groundY;
                if (pop.bounces < pop.maxBounces && -pop.vy > r.layout.cellHeight() * 0.25f) {
                    pop.vy = -pop.vy * LawnEntityDrawConstants.POP_BOUNCE;
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
            return pop.fade >= LawnEntityDrawConstants.ARMOR_POP_FADE;
        }
        return false;
    }

    private void paintArmorPop(Batch batch, ArmorPop pop) {
        float alpha = pop.grounded ? 1f - pop.fade / LawnEntityDrawConstants.ARMOR_POP_FADE : 1f;
        batch.setColor(1f, 1f, 1f, alpha);
        float s = AnimScale.ZOMBIE;
        r.batchTransform.set(batch.getTransformMatrix());
        r.popTransform.set(r.batchTransform)
                .translate(pop.x, pop.y, 0f)
                .scale(s, s, 1f)
                .translate(-pop.x, -pop.y, 0f);
        batch.setTransformMatrix(r.popTransform);
        r.clips.getOrLoad(pop.pamPath, pop.clipName);
        if (pop.part == null || LawnDeathPam.isHeadPopPart(pop.part)) {
            ClipRef ref = r.clips.getOrLoad(pop.pamPath, pop.clipName);
            if (ref != null) {
                r.player.draw(batch, ref, pop.clipTime, pop.x, pop.y, 1f, 1f, false,
                        r.deathSpawn.headPopVis(pop.part));
            }
        } else {
            r.player.drawPart(batch, pop.pamPath, pop.clipName, pop.clipTime, pop.x, pop.y, pop.part);
        }
        batch.setTransformMatrix(r.batchTransform);
        batch.setColor(Color.WHITE);
    }

    void drawDeathFx(Batch batch, float delta, int row) {
        for (int i = r.deathFx.size() - 1; i >= 0; i--) {
            DeathFx fx = r.deathFx.get(i);
            if (r.layout.rowAt(fx.y) != row) {
                continue;
            }
            if (paintDeathFx(batch, fx, delta)) {
                r.deathFx.remove(i);
            }
        }
    }

    private boolean paintDeathFx(Batch batch, DeathFx fx, float delta) {
        ClipRef ref = fx.pose.isSpritesheet()
                ? null
                : r.clips.getOrLoad(fx.pose.pamPath(), fx.pose.clipName());
        float hold = fx.pose.isSpritesheet()
                ? (fx.holdSeconds > 0f ? fx.holdSeconds : 0.4f)
                : fx.hold(ref);
        if (fx.time >= hold + LawnEntityDrawConstants.ARMOR_POP_FADE) {
            return true;
        }
        if (!fx.pose.isSpritesheet() && ref == null) {
            fx.time += delta;
            return false;
        }
        float scale = AnimScale.forZombie(fx.pose) * fx.pose.scale();
        float alpha = 1f - Math.max(0f, fx.time - hold) / LawnEntityDrawConstants.ARMOR_POP_FADE;
        float time = Math.min(fx.time, hold);
        batch.setColor(1f, 1f, 1f, alpha);
        if (fx.pose.isSpritesheet()) {
            r.drawPose(batch, fx, fx.pose, fx.x, fx.y, AnimScale.forZombie(fx.pose),
                    LawnEntityDrawConstants.NO_PHASE, 0f, 0f);
            batch.setColor(1f, 1f, 1f, 1f);
            fx.time += delta;
            return false;
        }
        paintDeathClip(batch, fx, ref, scale, alpha, time, delta);
        return false;
    }

    private void paintDeathClip(Batch batch, DeathFx fx, ClipRef ref, float scale, float alpha,
                                float time, float delta) {
        if (fx.drown) {
            freezeDrownWaterY(fx, ref, scale);
        }
        Rectangle mask = deathDrownMask(fx);
        Rectangle sprite = fx.drown
                ? FishermanAnim.spriteWorld(fx.x, fx.y,
                r.player.bounds(fx.pose.pamPath(), fx.pose.clipName()), scale, fx.pose.flipX())
                : null;
        boolean clipBody = mask != null && (sprite == null || FishermanAnim.overlaps(mask, sprite));
        if (clipBody) {
            r.drownShader().begin(batch, mask);
        }
        r.drawClip(batch, ref, fx.pose, time, fx.x, fx.y, scale);
        if (clipBody) {
            r.drownShader().end(batch);
        }
        float hit = fx.hitFlash / LawnEntityDrawConstants.HIT_FLASH_SEC;
        r.overlayHitFlash(batch, hit * alpha,
                () -> r.drawClip(batch, ref, fx.pose, time, fx.x, fx.y, scale));
        batch.setColor(Color.WHITE);
        fx.hitFlash = Math.max(0f, fx.hitFlash - delta);
        fx.time += delta;
    }

    private Rectangle deathDrownMask(DeathFx fx) {
        if (!fx.drown || Float.isNaN(fx.drownWaterY)) {
            return null;
        }
        int row = SnorkelerAnim.isSnorkelerPam(fx.pose.pamPath())
                ? FishermanAnim.rowAt(r.layout, fx.drownWaterY)
                : FishermanAnim.rowAt(r.layout, fx.y);
        return FishermanAnim.drownMaskWorld(r.layout, fx.x, row, fx.drownWaterY);
    }

    void freezeDrownWaterY(DeathFx fx, ClipRef die, float scale) {
        if (!Float.isNaN(fx.drownWaterY)) {
            return;
        }
        if (SnorkelerAnim.isSnorkelerPam(fx.pose.pamPath())) {
            return;
        }
        Rectangle tube = r.zombieSpecial.partAt(die, 0f, FishermanAnim.INNERTUBE_PART);
        if (tube == null) {
            tube = r.zombieSpecial.partAt(die, 0f, "zombie_innertube_layer");
        }
        if (tube == null) {
            tube = r.zombieSpecial.partAt(r.clips.getOrLoad(fx.pose.pamPath(), "idle"), 0f,
                    FishermanAnim.INNERTUBE_PART);
        }
        if (tube != null) {
            fx.drownWaterY = FishermanAnim.waterY(fx.y, tube, scale);
        }
    }
}
