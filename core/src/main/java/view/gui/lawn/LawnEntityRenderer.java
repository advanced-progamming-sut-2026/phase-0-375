package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import model.app.App;
import model.game.core.GameModel;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.AnimScale;
import view.gui.anim.PamClipCache;
import view.gui.anim.plant.PlantAnimAdapter;
import view.gui.anim.projectile.ProjectileAnimAdapter;
import view.gui.anim.zombie.ZombieAnimAdapter;
import view.gui.assets.PamCatalog;
import view.gui.assets.PvzAssets;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Draws plants, zombies, and projectiles on the lawn via libPVZ PAM clips.
 *
 * <p>Pipeline: model entity → {@link PlantAnimAdapter} / {@link ZombieAnimAdapter} /
 * {@link ProjectileAnimAdapter} → {@link AnimPose} → {@link PamClipCache} → {@code PamPlayer.draw}.
 *
 * <p>TODO: plant-food FX, mowers, and grid props.
 * TODO: sort draw order by row (back → front) then Y within a lane.
 * TODO: tint / freeze overlays from model status flags.
 */
public final class LawnEntityRenderer {
    private final LawnLayout layout;
    private final PlantAnimAdapter plantAdapter;
    private final ZombieAnimAdapter zombieAdapter;
    private final ProjectileAnimAdapter projectileAdapter;
    private final PamClipCache clips;
    private final pvz.libpvz.pam.PamPlayer player;

    private final IdentityHashMap<Object, AnimClock> clocks = new IdentityHashMap<>();
    private final Set<Object> seenThisFrame = new HashSet<>();
    private final float[] xyTmp = new float[2];
    private final Matrix4 poseTransform = new Matrix4();
    private final Matrix4 batchTransform = new Matrix4();

    private final DebugEntityOverlay entityOverlay;

    public LawnEntityRenderer(PvzAssets assets, LawnLayout layout, DebugEntityOverlay entityOverlay) {
        this(assets, layout,
                new PlantAnimAdapter(assets.pamCatalog),
                new ZombieAnimAdapter(assets.pamCatalog),
                entityOverlay);
    }

    public LawnEntityRenderer(PvzAssets assets, LawnLayout layout,
                              PlantAnimAdapter plantAdapter, ZombieAnimAdapter zombieAdapter,
                              DebugEntityOverlay entityOverlay) {
        this.layout = layout;
        this.plantAdapter = plantAdapter;
        this.zombieAdapter = zombieAdapter;
        this.projectileAdapter = new ProjectileAnimAdapter();
        this.player = assets.player;
        this.clips = new PamClipCache(assets.player);
        this.entityOverlay = entityOverlay;
    }

    public LawnEntityRenderer(PamCatalog catalog, PvzAssets assets, LawnLayout layout,
                              DebugEntityOverlay entityOverlay) {
        this(assets, layout, new PlantAnimAdapter(catalog), new ZombieAnimAdapter(catalog), entityOverlay);
    }

    public void draw(Batch batch, GameModel model, float delta) {
        if (model == null) {
            return;
        }
        seenThisFrame.clear();

        for (PlantInstance plant : model.getAllPlants()) {
            drawPlant(batch, plant, delta);
        }
        for (ZombieInstance zombie : model.getZombies()) {
            drawZombie(batch, zombie, delta);
        }
        if (model.getProjectiles() != null) {
            for (Projectile projectile : model.getProjectiles()) {
                drawProjectile(batch, projectile, delta);
            }
        }

        clocks.keySet().removeIf(key -> !seenThisFrame.contains(key));
    }

    private void drawPlant(Batch batch, PlantInstance plant, float delta) {
        Point pos = plant.getPosition();
        if (pos == null) {
            entityOverlay.drawPlant(batch, App.getInstance().getCurrentGameModel(), plant);
            return;
        }
        AnimPose pose = plantAdapter.poseFor(plant);
        if (pose == null) {
            entityOverlay.drawPlant(batch, App.getInstance().getCurrentGameModel(), plant);
            return;
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        drawPose(batch, plant, pose, xy[0], xy[1], AnimScale.PLANT, delta,
                pose.cacheKey() + "#" + plant.getActionEpoch());
    }

    private void drawZombie(Batch batch, ZombieInstance zombie, float delta) {
        AnimPose pose = zombieAdapter.poseFor(zombie);
        if (pose == null) {
            entityOverlay.drawZombie(batch, App.getInstance().getCurrentGameModel(), zombie);
            return;
        }
        if (!zombieWorldCenter(zombie, xyTmp)) {
            entityOverlay.drawZombie(batch, App.getInstance().getCurrentGameModel(), zombie);
            return;
        }
        drawPose(batch, zombie, pose, xyTmp[0], xyTmp[1], AnimScale.ZOMBIE, delta, pose.cacheKey());
    }

    private void drawProjectile(Batch batch, Projectile projectile, float delta) {
        if (projectile == null) {
            return;
        }
        AnimPose pose = projectileAdapter.poseFor(projectile);
        if (pose == null) {
            entityOverlay.drawProjectile(batch, projectile);
            return;
        }
        float[] xy = layout.centerOf(projectile.getY(), projectile.getX());
        drawPose(batch, projectile, pose, xy[0], xy[1], AnimScale.PROJECTILE, delta, pose.cacheKey());
    }

    private boolean zombieWorldCenter(ZombieInstance zombie, float[] out) {
        FloatPoint cont = zombie.getContinuousPosition();
        Point grid = zombie.getGridPosition();
        float row;
        float progressX;
        if (cont != null) {
            progressX = cont.getX();
            row = cont.getY();
        } else if (grid != null) {
            progressX = grid.getX();
            row = grid.getY();
        } else {
            return false;
        }
        float[] xy = layout.centerOf(Math.round(row), progressX);
        out[0] = xy[0];
        out[1] = xy[1];
        return true;
    }

    private void drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float delta, String clockKey) {
        seenThisFrame.add(entity);
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return;
        }
        float stateTime = advanceClock(entity, clockKey, delta);
        float scale = baseScale * pose.scale();
        boolean flipX = pose.flipX();
        boolean vis = pose.visibility() != null;
        float batchSx = (flipX ? -1f : 1f) * (vis ? scale : 1f);
        float batchSy = vis ? scale : 1f;
        float pamScale = vis ? 1f : scale;
        boolean useBatch = flipX || (vis && Math.abs(scale - 1f) > 0.001f);
        if (useBatch) {
            batchTransform.set(batch.getTransformMatrix());
            poseTransform.set(batchTransform)
                    .translate(x, y, 0f)
                    .scale(batchSx, batchSy, 1f)
                    .translate(-x, -y, 0f);
            batch.setTransformMatrix(poseTransform);
        }
        if (vis) {
            player.draw(batch, ref, stateTime, x, y, pose.loop(), pose.visibility());
        } else {
            player.draw(batch, ref, stateTime, x, y, pamScale, pamScale, pose.loop());
        }
        if (useBatch) {
            batch.setTransformMatrix(batchTransform);
        }
    }

    private float advanceClock(Object entity, String clipKey, float delta) {
        AnimClock clock = clocks.get(entity);
        if (clock == null) {
            clock = new AnimClock();
            clocks.put(entity, clock);
        }
        if (!clipKey.equals(clock.clipKey)) {
            clock.clipKey = clipKey;
            clock.time = 0f;
        } else {
            clock.time += delta;
        }
        return clock.time;
    }

    private static final class AnimClock {
        String clipKey;
        float time;
    }
}
