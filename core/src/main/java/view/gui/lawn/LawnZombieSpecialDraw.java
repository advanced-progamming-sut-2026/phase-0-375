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

final class LawnZombieSpecialDraw {
    private final LawnEntityRenderer r;

    LawnZombieSpecialDraw(LawnEntityRenderer r) {
        this.r = r;
    }

    /** Kick the EFFECTS PAM load during {@code power_up} so {@code laser_beam} is ready at 0.63s. */
    void preloadCrystalSkullBeam() {
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry beam = r.catalog.byName(LawnEntityDrawConstants.CRYSTALSKULL_BEAM_PAM);
        if (beam != null) {
            r.clips.getOrLoad(beam.path(), r.catalog.resolveClip(beam, "laser_beam"));
        }
    }

    /**
     * {@code CRYSTALSKULL_BEAM} starts when {@code zombie_egypt_ra_staff_whiteglow} fires
     * at {@link StealSunBehavior#ATTACK_BEAM_AT} of {@code attack}. The beam's right edge
     * sits on the skull's left and follows that part each frame.
     */
    void maybeDrawCrystalSkullBeam(Batch batch, AnimPose pose, float x, float y, float time) {
        if (pose == null || r.catalog == null) {
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
        PamCatalog.PamEntry beam = r.catalog.byName(LawnEntityDrawConstants.CRYSTALSKULL_BEAM_PAM);
        if (beam == null) {
            return;
        }
        String clip = r.catalog.resolveClip(beam, "laser_beam");
        ClipRef beamRef = r.clips.getOrLoad(beam.path(), clip);
        if (beamRef == null) {
            return;
        }
        float beamTime = time - StealSunBehavior.ATTACK_BEAM_AT;
        if (beamTime > beamRef.duration) {
            return;
        }
        float[] xy = crystalBeamWorld(pose, beam, clip, beamRef, x, y, time, beamTime);
        r.player.draw(batch, beamRef, beamTime, xy[0], xy[1], AnimScale.ZOMBIE, AnimScale.ZOMBIE, false);
    }

    private float[] crystalBeamWorld(AnimPose pose, PamCatalog.PamEntry beam, String clip,
                                     ClipRef beamRef, float x, float y, float time, float beamTime) {
        ClipRef attack = r.clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (r.crystalSkullPart == null) {
            r.crystalSkullPart = firstDrawnPart(attack, LawnEntityDrawConstants.CRYSTALSKULL_SKULL_PARTS);
        }
        if (r.crystalBeamPart == null) {
            r.crystalBeamPart = firstDrawnPart(beamRef, LawnEntityDrawConstants.CRYSTALSKULL_BEAM_PARTS);
        }
        Rectangle skull = partAt(attack, time, r.crystalSkullPart);
        Rectangle beamBox = partAt(beamRef, beamTime, r.crystalBeamPart);
        if (beamBox == null) {
            beamBox = r.player.bounds(beam.path(), clip);
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
        return new float[]{bx, by};
    }

    String firstDrawnPart(ClipRef clip, String[] names) {
        if (clip == null || names == null) {
            return null;
        }
        for (String name : names) {
            Rectangle[] frames = r.player.partBoundsByFrame(clip, name);
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
    Rectangle partAt(ClipRef clip, float time, String name) {
        if (clip == null || name == null) {
            return null;
        }
        Rectangle now = r.player.partBounds(clip, time, name);
        if (now != null) {
            return now;
        }
        Rectangle[] frames = r.player.partBoundsByFrame(clip, name);
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
    void restartArcadePushClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !"push".equals(pose.clipName())) {
            return;
        }
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        if (push == null || !push.isPushing() || push.getPushTimer() != 0f) {
            return;
        }
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code fly_start} / {@code fly_end}: rewind so the next hop replays. */
    void restartDodoFlyClock(ZombieInstance zombie, AnimPose pose) {
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
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code throw}: rewind so the next barrage replays. */
    void restartHunterThrowClock(ZombieInstance zombie, AnimPose pose) {
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
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code spinup} / {@code spindown}: rewind so the next cycle replays. */
    void restartJugglerSpinClock(ZombieInstance zombie, AnimPose pose) {
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
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code power}: rewind so the next raise doesn't keep the old time. */
    void restartTombRaiseClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !"power".equals(pose.clipName())) {
            return;
        }
        SummonBehavior summon = (SummonBehavior) zombie.getBehavior(ZombieBehaviorType.SUMMON);
        if (summon == null || !summon.isRaising() || summon.getRaiseTimer() != 0f) {
            return;
        }
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code sheep}: rewind so the next cast replays. */
    void restartWizardSheepClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !WizardAnim.SHEEP_CLIP.equals(pose.clipName())) {
            return;
        }
        TransformBehavior transform = (TransformBehavior) zombie.getBehavior(ZombieBehaviorType.TRANSFORM);
        if (transform == null || !transform.isCasting() || transform.getSheepTimer() != 0f) {
            return;
        }
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code intro}/{@code special}: rewind so the next cycle replays. */
    void restartDarkKingClock(ZombieInstance zombie, AnimPose pose) {
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
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** First frame of a new {@code intro}/{@code cast}/{@code reel}: rewind so the next cycle replays. */
    void restartFishermanClock(ZombieInstance zombie, AnimPose pose) {
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
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** Fuse done: cut walk immediately and play {@code blastoff} from t=0. */
    void restartProspectorJumpClock(ZombieInstance zombie, AnimPose pose) {
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
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    /** 0 at the start of this jump clip; not the leftover walk gait phase. */
    static float prospectorJumpPhase(JumpBehavior jump, String clip, float duration) {
        float local = jump.getTravelTimer();
        if ("fly".equals(clip)) {
            local -= JumpBehavior.BLASTOFF_DURATION;
        } else if ("land".equals(clip)) {
            local -= JumpBehavior.BLASTOFF_DURATION + JumpBehavior.FLY_DURATION;
        }
        return Math.max(0f, local) / duration;
    }

    /**
     * Shift the Imp so {@code zombie_imp_skull} sits where it left the Gargantuar.
     * Stored in world pixels and faded out along the flight so it lands on the tile centre.
     *
     * <p>libPVZ {@code partBounds} is PAM-local (Y-down from the canvas centre). Draw flips Y,
     * so world Y is {@code originY - localY * scale}.
     */
    void alignToss(ZombieInstance imp, ThrowImpBehavior.Flight flight,
                           AnimPose pose, float impX, float impY) {
        if (!flight.isFlying() || r.tossAlign.containsKey(imp) || pose == null) {
            return;
        }
        LiveSnap garg = r.lastLive.get(flight.thrower());
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
        r.tossAlign.put(imp, new float[]{gargSkullX - impSkullX, gargSkullY - impSkullY});
    }

    Rectangle skullBounds(String pam, String clip, float time) {
        Rectangle bounds = r.player.partBounds(pam, clip, time, "zombie_imp_skull");
        if (bounds == null) {
            bounds = r.player.partBounds(pam, clip, time, "_zombie_imp_head_top");
        }
        return bounds;
    }

    void maybeGargantuarWalkStomp(ZombieInstance zombie, AnimPose pose, float time) {
        if (r.screenShake == null || pose == null || pose.isSpritesheet()
                || !"walk".equals(pose.clipName())) {
            return;
        }
        if (!LawnDeathPam.isGargantuar(pose.pamPath())) {
            return;
        }
        LiveSnap prev = r.lastLive.get(zombie);
        float prevTime = prev != null && "walk".equals(prev.pose.clipName()) ? prev.time : -1f;
        ClipRef walk = r.clips.getOrLoad(pose.pamPath(), pose.clipName());
        float duration = walk != null ? walk.duration : 0f;
        if (GargantuarAnim.crossedWalkStomp(prevTime, time, duration)) {
            r.screenShake.pulse();
        }
    }

    void drawSnorkelRipple(Batch batch, AnimPose pose, ZombieInstance zombie,
                                   float rippleX, float tileWaterY) {
        String rippleName = SnorkelerAnim.rippleName(zombie);
        PamCatalog.PamEntry entry = r.catalog == null ? null : r.catalog.byName(rippleName);
        String path = entry != null ? entry.path() : SnorkelerAnim.ripplePath(zombie);
        String clip = entry != null
            ? r.catalog.resolveClip(entry, SnorkelerAnim.RIPPLE_CLIP, "ripple_exit")
            : SnorkelerAnim.RIPPLE_CLIP;
        ClipRef ripple = r.clips.getOrLoad(path, clip);
        if (ripple == null && !r.snorkelRippleLoaded.contains(path)) {
            r.snorkelRippleLoaded.add(path);
            r.clips.preloadSync(path, clip);
            ripple = r.clips.getOrLoad(path, clip);
        }
        if (ripple == null) {
            return;
        }
        boolean gargantuar = zombie.getDefinition() != null
                && zombie.getDefinition().getSize() == ZombieSize.LARGE;
        float rippleScale = AnimScale.ZOMBIE * (gargantuar ? 1f : pose.scale());
        float anchorY = tileWaterY;
        if (gargantuar) {
            // Tile waterline is one cell low for the large unit; same anchor as normal zombies.
            anchorY += r.layout.cellHeight();
        }
        Rectangle clipBox = r.player.bounds(path, clip);
        float ry = SnorkelerAnim.rippleDrawY(anchorY, clipBox, rippleScale);
        r.player.draw(batch, ripple, r.snorkelRippleTime, rippleX, ry, rippleScale, rippleScale, true);
    }

    /** Ripple-only on shallow tiles after emerge; no foot mask (that hid the whole body). */
    boolean shouldRippleOnWater(ZombieInstance zombie, GameModel model,
                                        SwimBehavior swim, JumpBehavior jump) {
        if (model == null || zombie == null || zombie.isDead()) {
            return false;
        }
        if (r.waterEmerges.containsKey(zombie)) {
            return false;
        }
        if (jump != null && jump.getPhase() == JumpBehavior.JumpPhase.JUMPING) {
            return false;
        }
        if (swim != null && (swim.isSubmerged() || swim.isSurfaced())) {
            return false;
        }
        return model.isWaterTile(zombie.getGridY(), zombie.getGridX());
    }
}
