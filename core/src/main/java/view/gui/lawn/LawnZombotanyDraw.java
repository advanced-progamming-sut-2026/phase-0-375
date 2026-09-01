package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.game.map.Point;
import model.zombie.behavior.zombotany.ZombotanyJalapenoBehavior;
import model.zombie.behavior.zombotany.ZombotanySquashBehavior;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.AnimScale;
import view.gui.anim.plant.ExplosivePlantFx;
import view.gui.anim.zombie.ZombotanyAnim;
import view.gui.assets.PamCatalog;

final class LawnZombotanyDraw {
    private final LawnEntityRenderer r;

    LawnZombotanyDraw(LawnEntityRenderer r) {
        this.r = r;
    }

    void applyZombotanySquashLeap(ZombieInstance zombie, Chapter chapter, float[] xy) {
        if (zombie == null || xy == null) {
            return;
        }
        ZombotanySquashBehavior squash =
                (ZombotanySquashBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_SQUASH);
        if (squash == null || !squash.isSquashing()
                || squash.getSmashTargetGridX() < 0 || squash.getSmashTargetGridY() < 0) {
            return;
        }
        PamCatalog.PamEntry entry = ZombotanyAnim.squashPlantEntry(r.catalog, zombie);
        float[] to = r.layout.centerOf(squash.getSmashTargetGridY(), squash.getSmashTargetGridX());
        float dx = to[0] - xy[0];
        float dy = to[1] - xy[1];
        float travel = ZombotanyAnim.squashLeapTravel(zombie, entry);
        if (travel > 0f) {
            xy[0] += dx * travel;
            xy[1] += dy * travel;
        }
        float travelTiles = r.layout.cellWidth() > 0f
                ? (float) Math.sqrt(dx * dx + dy * dy) / r.layout.cellWidth()
                : 1f;
        xy[1] += ZombotanyAnim.squashLeapHeight(zombie, entry, travelTiles) * r.layout.cellHeight();
    }

    void maybeSpawnZombotanyJalapenoFire(ZombieInstance zombie) {
        if (zombie == null) {
            return;
        }
        ZombotanyJalapenoBehavior jala =
                (ZombotanyJalapenoBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_JALAPENO);
        if (jala == null || !jala.isAttacking()) {
            return;
        }
        if (r.jalapenoFireSpawned.put(zombie, Boolean.TRUE) != null) {
            return;
        }
        Point pos = new Point(0, zombie.getGridY());
        float[] xy = r.layout.centerOf(zombie.getGridY(), Math.max(0, zombie.getGridX()));
        r.plantFx.spawnExplosionSpecs(ExplosivePlantFx.specsForName("Jalapeno"), pos, xy[0], xy[1]);
    }

    void drawZombotanyPlantHead(Batch batch, ZombieInstance zombie, AnimPose bodyPose,
                                float bodyX, float bodyY, float bodyTime, float delta,
                                float flash, float glow, float chill) {
        if (r.catalog == null || bodyPose == null || bodyPose.isSpritesheet()) {
            return;
        }
        String plantName = ZombotanyAnim.plantDefinitionName(zombie);
        PamCatalog.PamEntry plant = plantName == null ? null : r.catalog.forPlant(plantName);
        AnimPose headPose = ZombotanyAnim.plantHeadPose(zombie, plant);
        if (headPose == null) {
            return;
        }
        if (bodyPose.flipX()) {
            headPose = headPose.withFlipX(false);
        }
        float bodyScale = AnimScale.ZOMBIE * bodyPose.scale();
        float[] headXy = zombotanyHeadWorld(bodyPose, bodyX, bodyY, bodyTime, bodyScale);
        Object clockKey = r.zombotanyHeadClocks.computeIfAbsent(zombie, z -> new Object());
        r.seenThisFrame.add(zombie);
        r.drawPose(batch, clockKey, headPose, headXy[0], headXy[1], AnimScale.PLANT,
                LawnEntityDrawConstants.NO_PHASE, flash, delta, headPose.cacheKey(), glow, chill, 0f);
    }

    void drawZombotanyPlantHeadIdle(Batch batch, String zombieName, Chapter chapter,
                                    PamCatalog.PamEntry bodyEntry, ClipRef bodyRef,
                                    float time, float x, float y) {
        if (r.catalog == null || bodyEntry == null || bodyRef == null) {
            return;
        }
        String plantName = ZombotanyAnim.plantDefinitionName(zombieName);
        PamCatalog.PamEntry plant = plantName == null ? null : r.catalog.forPlant(plantName);
        if (plant == null) {
            return;
        }
        String clip = r.catalog.resolveClip(plant, "idle", "idle2", "idle1", "loop");
        if (clip == null) {
            return;
        }
        ClipRef headRef = r.clips.getOrLoad(plant.path(), clip);
        if (headRef == null) {
            return;
        }
        float bodyScale = AnimScale.ZOMBIE;
        float[] headXy = zombotanyHeadWorld(bodyEntry.path(), bodyRef, false, x, y, time, bodyScale);
        float headScale = AnimScale.PLANT * ZombotanyAnim.HEAD_SCALE;
        r.player.draw(batch, headRef, time, headXy[0], headXy[1], -headScale, headScale, true);
    }

    float[] zombotanyHeadWorld(AnimPose bodyPose, float bodyX, float bodyY,
                               float bodyTime, float bodyScale) {
        ClipRef bodyRef = r.clips.getOrLoad(bodyPose.pamPath(), bodyPose.clipName());
        return zombotanyHeadWorld(bodyPose.pamPath(), bodyRef, bodyPose.flipX(),
                bodyX, bodyY, bodyTime, bodyScale);
    }

    float[] zombotanyHeadWorld(String pam, ClipRef bodyRef, boolean flipX,
                               float bodyX, float bodyY, float bodyTime, float bodyScale) {
        Rectangle skull = null;
        if (bodyRef != null) {
            for (String part : ZombotanyAnim.SKULL_PARTS) {
                skull = r.zombieSpecial.partAt(bodyRef, bodyTime, part);
                if (skull != null) {
                    break;
                }
            }
        }
        float fallbackY = r.layout.cellHeight() * 0.28f;
        return ZombotanyAnim.headWorldCenter(skull, flipX, bodyX, bodyY, bodyScale, fallbackY);
    }
}
