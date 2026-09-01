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

final class LawnDeathSpawn {
    private final LawnEntityRenderer r;

    LawnDeathSpawn(LawnEntityRenderer r) {
        this.r = r;
    }

    /** {@code particle_arm} hops off {@code particles} at half HP. */
    void maybePopLostHand(ZombieInstance zombie, AnimPose pose, float x, float y) {
        if (zombie == null || pose == null || pose.isSpritesheet() || r.lostHands.containsKey(zombie)) {
            return;
        }
        if (!LawnDeathPam.atOrBelowHalfHp(zombie)) {
            return;
        }
        String pam = pose.pamPath();
        if (firstLoadedClip(pam, "particles", null) == null) {
            return;
        }
        List<String> arms = particleArmParts(pam);
        if (arms.isEmpty()) {
            for (String part : particleParts(pam)) {
                if (LawnDeathPam.isArmPopPart(part)) {
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
        r.lostHands.put(zombie, Boolean.TRUE);
    }

    void spawnDeath(GameModel model, ZombieInstance zombie, LiveSnap snap) {
        if (snap == null || snap.pose == null) {
            return;
        }
        if (trySpawnAsh(zombie, snap) || spawnSheetDeath(snap)) {
            return;
        }
        if (attachBarrelLeftover(model, zombie, snap)) {
            return;
        }
        String pam = snap.pose.pamPath();
        String dieClip = BarrelRollerAnim.isUnarmedClip(snap.pose.clipName())
                ? firstLoadedClip(pam, "die2", snap.pose.clipName())
                : firstLoadedClip(pam, "die", snap.pose.clipName());
        List<String> bits = particleParts(pam);
        AnimPose diePose = diePoseFor(snap, pam, dieClip, bits);
        if (spawnDrownDeath(zombie, snap, pam, diePose)) {
            return;
        }
        r.deathFx.add(new DeathFx(diePose, snap.x, snap.y, false));
        spawnDeathPops(zombie, snap, pam, dieClip, bits);
    }

    private boolean spawnSheetDeath(LiveSnap snap) {
        if (!snap.pose.isSpritesheet()) {
            return false;
        }
        AnimPose fade = AnimPose.sheetOnce(
                snap.pose.pamPath(), snap.pose.clipName(), ZombieAnimRole.DIE);
        if (snap.pose.flipX()) {
            fade = fade.flipped();
        }
        DeathFx fx = new DeathFx(fade, snap.x, snap.y, false);
        fx.holdSeconds = 0.4f;
        r.deathFx.add(fx);
        return true;
    }

    private AnimPose diePoseFor(LiveSnap snap, String pam, String dieClip, List<String> bits) {
        Map<String, Boolean> vis = new HashMap<>();
        if (snap.pose.visibility() != null) {
            vis.putAll(snap.pose.visibility());
        }
        for (String part : bits) {
            vis.put(part, Boolean.FALSE);
        }
        LawnDeathPam.hideInkButter(vis);
        String[] bodyHead = LawnDeathPam.deathHeadParts(pam);
        if (bodyHead != null) {
            for (String part : bodyHead) {
                vis.put(part, Boolean.FALSE);
            }
        }
        AnimPose diePose = AnimPose.once(pam, dieClip, ZombieAnimRole.DIE, vis.isEmpty() ? null : vis);
        return snap.pose.flipX() ? diePose.flipped() : diePose;
    }

    private boolean spawnDrownDeath(ZombieInstance zombie, LiveSnap snap, String pam, AnimPose diePose) {
        boolean drown = FishermanAnim.isFishermanPam(pam);
        if (SnorkelerAnim.isSnorkelerPam(pam)) {
            SwimBehavior swim = (SwimBehavior) zombie.getBehavior(ZombieBehaviorType.SWIM);
            if (swim != null && (swim.isSubmerged() || swim.isSurfaced()) && swim.getRise() < 1f) {
                DeathFx fx = new DeathFx(diePose, snap.x, snap.y, true);
                fx.snorkelRise = swim.getRise();
                fx.drownWaterY = SnorkelerAnim.waterLineY(r.layout, zombie.getGridY());
                r.deathFx.add(fx);
                return true;
            }
        }
        if (drown) {
            r.deathFx.add(new DeathFx(diePose, snap.x, snap.y, true));
            return true;
        }
        return false;
    }

    private void spawnDeathPops(ZombieInstance zombie, LiveSnap snap, String pam, String dieClip,
                                List<String> bits) {
        ClipRef dieRef = r.clips.getOrLoad(pam, dieClip);
        String headGroup = LawnDeathPam.deathHeadGroup(pam);
        float hold = dieRef != null && (headGroup != null || LawnDeathPam.popsHeadAndArm(pam))
                ? dieRef.duration : 0f;
        float dir = snap.backward ? -1f : 1f;
        if (headGroup != null && firstLoadedClip(pam, "particles", null) != null) {
            addLimbPop(pam, "particles", headGroup, snap.x, snap.y, 0f,
                    LawnDeathPam.randomHeadThrowDir(), LawnEntityDrawConstants.HEAD_THROW_BACK_TILES,
                    LawnEntityDrawConstants.HEAD_THROW_HOP_TILES, hold, !LawnDeathPam.isAllStar(pam));
            return;
        }
        for (int i = 0; i < bits.size(); i++) {
            String part = bits.get(i);
            if (r.lostHands.containsKey(zombie) && LawnDeathPam.isArmPopPart(part)) {
                continue;
            }
            boolean head = LawnDeathPam.isHeadParticlePart(part);
            float throwDir = head ? LawnDeathPam.randomHeadThrowDir() : dir;
            float back = head ? LawnEntityDrawConstants.HEAD_THROW_BACK_TILES : 0.1f + i * 0.1f;
            float hop = head ? LawnEntityDrawConstants.HEAD_THROW_HOP_TILES : 0.85f + (i % 2) * 0.3f;
            addLimbPop(pam, "particles", part, snap.x, snap.y, 0f, throwDir, back, hop, hold, false);
        }
    }

    /**
     * Freeze the last live barrel parts on the orphan. Body still plays {@code die}.
     */
    boolean attachBarrelLeftover(GameModel model, ZombieInstance zombie, LiveSnap snap) {
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
        r.clips.getOrLoad(snap.pose.pamPath(), snap.pose.clipName());
        AnimPose held = AnimPose.once(snap.pose.pamPath(), snap.pose.clipName(),
            ZombieAnimRole.IDLE, null);
        if (snap.pose.flipX()) {
            held = held.flipped();
        }
        r.lastCabinets.put(barrel, new LiveSnap(held, snap.x, snap.y, snap.backward, snap.time));
        return true;
    }

    Barrel findOrphanBarrel(GameModel model, ZombieInstance zombie) {
        Barrel fromOrphans = matchBarrel(
            model == null ? null : model.getOrphanedPushables(), zombie);
        if (fromOrphans != null) {
            return fromOrphans;
        }
        for (Pushable item : r.lastCabinets.keySet()) {
            if (item instanceof Barrel barrel
                && !barrel.isDestroyed()
                && barrel.getPosition() != null) {
                return barrel;
            }
        }
        return null;
    }

    static Barrel matchBarrel(Iterable<Pushable> items, ZombieInstance zombie) {
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

    /** Incineration PAM lives under EFFECTS and has clip {@code animation}, not {@code die}. */
    boolean trySpawnAsh(ZombieInstance zombie, LiveSnap snap) {
        if (zombie == null || !zombie.isBlownUp() || r.catalog == null) {
            return false;
        }
        PamCatalog.PamEntry ash = r.catalog.byName(LawnDeathPam.ashPamFor(zombie));
        if (ash == null) {
            return false;
        }
        String clip = r.catalog.resolveClip(ash, "animation");
        r.deathFx.add(new DeathFx(
            AnimPose.once(ash.path(), clip, ZombieAnimRole.DIE, null),
            snap.x, snap.y));
        return true;
    }

    String firstLoadedClip(String pam, String preferred, String fallback) {
        if (preferred == null) {
            return fallback;
        }
        return r.clips.getOrLoad(pam, preferred) != null ? preferred : fallback;
    }

    /** Skull / jaw / outer arm on {@code particles}. Egypt uses biome-prefixed names. */
    List<String> particleParts(String pam) {
        List<String> bits = new ArrayList<>();
        if (firstLoadedClip(pam, "particles", null) == null) {
            return bits;
        }
        if (LawnDeathPam.popsHeadAndArm(pam)) {
            bits.addAll(List.of(LawnEntityDrawConstants.ARCADE_PARTICLE_PARTS));
            return bits;
        }
        String[] names = LawnDeathPam.deathHeadGroup(pam) != null
            ? new String[]{LawnDeathPam.deathHeadGroup(pam)}
            : LawnDeathPam.egyptDeathParts(pam)
            ? LawnEntityDrawConstants.DEATH_PARTS_EGYPT : LawnEntityDrawConstants.DEATH_PARTS;
        boolean particleHead = partDrawn(r.clips.getOrLoad(pam, "particles"), "particle_head");
        if (particleHead && LawnDeathPam.deathHeadGroup(pam) == null) {
            bits.add("particle_head");
        }
        List<String> armParticles = particleArmParts(pam);
        bits.addAll(armParticles);
        boolean particleLimb = !armParticles.isEmpty();
        for (String part : names) {
            if (particleHead && (part.contains("skull") || part.contains("jaw"))) {
                continue;
            }
            if (particleLimb && LawnDeathPam.isArmPopPart(part) && !LawnDeathPam.isParticleLimb(part)) {
                continue;
            }
            if (partDrawn(r.clips.getOrLoad(pam, "particles"), part)) {
                bits.add(part);
            }
        }
        return bits;
    }

    /** Detached {@code particle_arm} (or {@code particle_hand} if that group is missing). */
    List<String> particleArmParts(String pam) {
        List<String> arms = new ArrayList<>();
        List<String> hands = new ArrayList<>();
        ClipRef particles = r.clips.getOrLoad(pam, "particles");
        if (particles == null) {
            return arms;
        }
        for (String name : LawnEntityDrawConstants.ARM_PARTICLE_NAMES) {
            if (!partDrawn(particles, name)) {
                continue;
            }
            if (LawnDeathPam.isParticleHandPart(name)) {
                hands.add(name);
            } else {
                arms.add(name);
            }
        }
        return arms.isEmpty() ? hands : arms;
    }

    boolean partDrawn(ClipRef clip, String name) {
        if (clip == null || name == null) {
            return false;
        }
        Rectangle[] frames = r.player.partBoundsByFrame(clip, name);
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

    String[] lostArmBodyParts(String pam) {
        if (pam == null) {
            return LawnEntityDrawConstants.LOST_HAND_BODY_PARTS;
        }
        String[] cached = r.lostArmBodyByPam.get(pam);
        if (cached != null) {
            return cached.length == 0 ? LawnEntityDrawConstants.LOST_HAND_BODY_PARTS : cached;
        }
        List<String> names = new ArrayList<>();
        LawnDeathPam.collectLostArmBodyParts(r.player.getParts(pam), names);
        cached = names.toArray(String[]::new);
        r.lostArmBodyByPam.put(pam, cached);
        return cached.length == 0 ? LawnEntityDrawConstants.LOST_HAND_BODY_PARTS : cached;
    }

    Map<String, Boolean> headPopVis(String part) {
        r.popVis.clear();
        LawnDeathPam.hideInkButter(r.popVis);
        for (String hide : LawnEntityDrawConstants.HEAD_POP_HIDE) {
            r.popVis.put(hide, Boolean.FALSE);
        }
        if (part != null) {
            r.popVis.put(part, Boolean.TRUE);
        }
        return r.popVis;
    }

    void addLimbPop(String pam, String clip, String part,
                            float originX, float originY, float time,
                            float dir, float backTiles, float hopTiles, float hold,
                            boolean wholeClip) {
        float s = AnimScale.ZOMBIE;
        r.clips.getOrLoad(pam, clip);
        Rectangle bounds = r.player.partBounds(pam, clip, time, part);
        float groundY = originY - r.layout.cellHeight() * 0.5f;
        if (bounds != null) {
            groundY = originY - r.layout.cellHeight() * 0.5f + (bounds.y + bounds.height) * s;
        }
        float hopTime = 2f * hopTiles / -LawnEntityDrawConstants.ARMOR_POP_GRAVITY;
        ArmorPop pop = new ArmorPop(
            pam, clip, wholeClip ? null : part,
            originX, originY, groundY,
            dir * backTiles * r.layout.cellWidth() / hopTime,
            hopTiles * r.layout.cellHeight(),
            LawnEntityDrawConstants.ARMOR_POP_GRAVITY * r.layout.cellHeight());
        pop.hold = hold;
        pop.clipTime = time;
        if (LawnDeathPam.isHeadParticlePart(part) || LawnDeathPam.isArmPopPart(part)) {
            pop.maxBounces = 0;
        }
        r.armorPops.add(pop);
    }
}
