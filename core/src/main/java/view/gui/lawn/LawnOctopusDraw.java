package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import model.enums.ZombieBehaviorType;
import model.game.core.GameModel;
import model.game.map.Point;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.ShootBehavior;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.AnimScale;
import view.gui.anim.zombie.OctopusAnim;
import view.gui.assets.PamCatalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class LawnOctopusDraw {
    private final LawnEntityRenderer r;

    LawnOctopusDraw(LawnEntityRenderer r) {
        this.r = r;
    }

    void restartOctopusTossClock(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !OctopusAnim.TOSS_CLIP.equals(pose.clipName())) {
            return;
        }
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.isOctopusThrowing() || shoot.getOctopusTossTimer() != 0f) {
            return;
        }
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    void captureOctopusRelease(ZombieInstance zombie, AnimPose pose,
                               float x, float y, float time) {
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.hasReleasedOctopus() || pose == null) {
            return;
        }
        float s = AnimScale.ZOMBIE * pose.scale();
        ClipRef toss = r.clips.getOrLoad(pose.pamPath(), pose.clipName());
        Rectangle from = r.zombieSpecial.partAt(toss, time, OctopusAnim.HELD_PART);
        if (from == null) {
            from = r.zombieSpecial.partAt(toss, ShootBehavior.OCTOPUS_RELEASE_AT,
                    OctopusAnim.HELD_PART);
        }
        float heldX = x;
        float heldY = y;
        if (from != null) {
            heldX = x + (from.x + from.width * 0.5f) * s;
            heldY = y - (from.y + from.height * 0.5f) * s;
        }
        PamCatalog.PamEntry proj = r.catalog == null ? null : r.catalog.byName(OctopusAnim.PROJECTILE_PAM);
        ClipRef fly = proj == null ? null : r.clips.getOrLoad(proj.path(), OctopusAnim.FLY_CLIP);
        Rectangle to = r.zombieSpecial.partAt(fly, 0f, OctopusAnim.HELD_PART);
        float originX = heldX;
        float originY = heldY;
        if (to != null) {
            originX = heldX - (to.x + to.width * 0.5f) * s;
            originY = heldY + (to.y + to.height * 0.5f) * s;
        }
        for (ShootBehavior.OctopusShot shot : shoot.getOctopusShots()) {
            if (shot.isFlying() && !r.octopusAlign.containsKey(shot)) {
                r.octopusAlign.put(shot, new float[]{originX, originY});
            }
        }
        preloadOctopusProjectile();
    }

    void preloadOctopusProjectile() {
        if (r.catalog == null) {
            return;
        }
        PamCatalog.PamEntry proj = r.catalog.byName(OctopusAnim.PROJECTILE_PAM);
        if (proj == null) {
            return;
        }
        r.clips.getOrLoad(proj.path(), OctopusAnim.FLY_CLIP);
        r.clips.getOrLoad(proj.path(), OctopusAnim.IMPACT_CLIP);
        r.clips.getOrLoad(proj.path(), OctopusAnim.LOOP_CLIP);
        r.clips.getOrLoad(proj.path(), OctopusAnim.DIE_CLIP);
    }

    void drawOctopi(Batch batch, GameModel model, float delta) {
        preloadOctopusProjectile();
        PamCatalog.PamEntry proj = r.catalog == null ? null : r.catalog.byName(OctopusAnim.PROJECTILE_PAM);
        if (proj == null) {
            return;
        }
        String path = proj.path();
        drawFlyingShots(batch, model, path);
        tickOctopusCoats(batch, model, path, delta);
    }

    private void drawFlyingShots(Batch batch, GameModel model, String path) {
        for (ZombieInstance zombie : model.getZombies()) {
            ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
            if (shoot == null || !shoot.isBeachOctopus(zombie)) {
                continue;
            }
            if (shoot.isOctopusThrowing()) {
                r.clips.getOrLoad(path, OctopusAnim.FLY_CLIP);
            }
            for (ShootBehavior.OctopusShot shot : shoot.getOctopusShots()) {
                if (shot.isFlying()) {
                    drawFlyingOctopus(batch, shot, path);
                }
            }
        }
    }

    private void tickOctopusCoats(Batch batch, GameModel model, String path, float delta) {
        Set<PlantInstance> plants = new HashSet<>();
        for (PlantInstance plant : model.getAllPlants()) {
            plants.add(plant);
            if (plant.hasOctopusCoating() && !r.octopusCoats.containsKey(plant)) {
                r.octopusCoats.put(plant, new OctopusCoatFx());
            }
        }
        for (PlantInstance plant : new ArrayList<>(r.octopusCoats.keySet())) {
            OctopusCoatFx fx = r.octopusCoats.get(plant);
            boolean gone = !plants.contains(plant) || plant.getCurrentHP() <= 0
                    || (!plant.hasOctopusCoating() && !fx.dying);
            if (gone && !fx.dying) {
                fx.dying = true;
                fx.time = 0f;
            }
            if (!drawOctopusCoat(batch, plant, path, fx, delta)) {
                r.octopusCoats.remove(plant);
            }
        }
    }

    void drawFlyingOctopus(Batch batch, ShootBehavior.OctopusShot shot, String path) {
        Point cell = shot.targetCell();
        if (cell == null) {
            return;
        }
        float[] dest = r.layout.centerOf(cell.getY(), cell.getX());
        float[] start = r.octopusAlign.get(shot);
        float x0 = dest[0];
        float y0 = dest[1];
        if (start != null) {
            x0 = start[0];
            y0 = start[1];
        } else if (shot.thrower() != null && r.zombieWorldCenter(shot.thrower(), r.xyTmp)) {
            x0 = r.xyTmp[0];
            y0 = r.xyTmp[1];
        }
        float t = shot.progress();
        float x = x0 + (dest[0] - x0) * t;
        float y = y0 + (dest[1] - y0) * t + shot.heightTiles() * r.layout.cellHeight();
        ClipRef ref = r.clips.getOrLoad(path, OctopusAnim.FLY_CLIP);
        if (ref == null) {
            return;
        }
        float scale = AnimScale.ZOMBIE;
        r.player.draw(batch, ref, shot.timer(), x, y, scale, scale, true);
    }

    boolean drawOctopusCoat(Batch batch, PlantInstance plant, String path,
                            OctopusCoatFx fx, float delta) {
        Point pos = plant.getPosition();
        if (pos == null && !fx.dying) {
            return false;
        }
        float x;
        float y;
        if (pos != null) {
            float[] xy = r.layout.centerOf(pos.getY(), pos.getX());
            x = xy[0];
            y = xy[1];
            fx.x = x;
            fx.y = y;
        } else {
            x = fx.x;
            y = fx.y;
        }
        String clip = fx.dying ? OctopusAnim.DIE_CLIP
                : fx.time < impactDuration(path) ? OctopusAnim.IMPACT_CLIP
                : OctopusAnim.LOOP_CLIP;
        boolean loop = !fx.dying && OctopusAnim.LOOP_CLIP.equals(clip);
        ClipRef ref = r.clips.getOrLoad(path, clip);
        r.seenThisFrame.add(fx);
        int coatHp = plant.hasOctopusCoating() ? plant.getIceHp() : 0;
        float flash = r.tickHitFlash(fx, coatHp, delta);
        if (ref == null) {
            fx.time += delta;
            return !fx.dying;
        }
        float clipTime = OctopusAnim.LOOP_CLIP.equals(clip)
                ? Math.max(0f, fx.time - impactDuration(path))
                : fx.time;
        r.player.draw(batch, ref, clipTime, x, y, AnimScale.ZOMBIE, AnimScale.ZOMBIE, loop);
        r.overlayHitFlash(batch, flash,
                () -> r.player.draw(batch, ref, clipTime, x, y, AnimScale.ZOMBIE, AnimScale.ZOMBIE, loop));
        fx.time += delta;
        if (fx.dying && fx.time >= ref.duration) {
            return false;
        }
        return true;
    }

    float impactDuration(String path) {
        ClipRef impact = r.clips.getOrLoad(path, OctopusAnim.IMPACT_CLIP);
        return impact == null ? 0.9667f : impact.duration;
    }
}
