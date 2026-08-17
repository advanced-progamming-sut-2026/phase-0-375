package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import model.app.App;
import model.enums.PlacableLayer;
import model.game.core.GameModel;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.projectile.Splash;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.AnimScale;
import view.gui.anim.PamClipCache;
import view.gui.anim.plant.ExplosivePlantFx;
import view.gui.anim.plant.PlantAnimAdapter;
import view.gui.anim.projectile.ProjectileAnimAdapter;
import view.gui.anim.zombie.ZombieAnimAdapter;
import view.gui.assets.PamCatalog;
import view.gui.assets.ProjectilePamPaths;
import view.gui.assets.PvzAssets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Draws plants, zombies, and projectiles on the lawn via libPVZ PAM clips.
 *
 * <p>Pipeline: model entity → {@link PlantAnimAdapter} / {@link ZombieAnimAdapter} /
 * {@link ProjectileAnimAdapter} → {@link AnimPose} → {@link PamClipCache} → {@code PamPlayer.draw}.
 *
 * <p>TODO: plant-food FX, mowers, and grid props.
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
    private final IdentityHashMap<PlantInstance, Boolean> explosionSpawned = new IdentityHashMap<>();
    private final IdentityHashMap<PlantInstance, float[]> deathBlastSeen = new IdentityHashMap<>();
    private final List<OneShotFx> backEffects = new ArrayList<>();
    private final List<OneShotFx> frontEffects = new ArrayList<>();
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

        // GROUND (Lily Pad) behind MAIN, OVERLAY (Pumpkin) in front;
        // back rows before front rows so nearer plants occlude.
        List<PlantInstance> plants = new ArrayList<>(model.getAllPlants());
        plants.sort(LawnEntityRenderer::compareDrawOrder);
        IdentityHashMap<PlantInstance, float[]> deathBlastNow = new IdentityHashMap<>();
        for (PlantInstance plant : plants) {
            maybeSpawnPlantExplosion(plant, deathBlastNow);
        }
        spawnMissingDeathBlasts(deathBlastNow);
        deathBlastSeen.clear();
        deathBlastSeen.putAll(deathBlastNow);

        drawEffects(batch, backEffects, delta);
        for (PlantInstance plant : plants) {
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
        harvestProjectileHits(model);
        drawEffects(batch, frontEffects, delta);

        clocks.keySet().removeIf(key -> !seenThisFrame.contains(key));
        explosionSpawned.keySet().removeIf(plant -> !seenThisFrame.contains(plant));
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

    private void maybeSpawnPlantExplosion(PlantInstance plant, IdentityHashMap<PlantInstance, float[]> deathBlastNow) {
        Point pos = plant.getPosition();
        if (pos == null) {
            return;
        }
        float[] xy = layout.centerOf(pos.getY(), pos.getX());
        if (ExplosivePlantFx.isDeathDetonator(plant)) {
            deathBlastNow.put(plant, new float[]{xy[0], xy[1]});
            return;
        }
        AnimPose pose = plantAdapter.poseFor(plant);
        if (!ExplosivePlantFx.shouldSpawn(plant, pose)) {
            return;
        }
        if (explosionSpawned.put(plant, Boolean.TRUE) != null) {
            return;
        }
        spawnExplosionSpecs(ExplosivePlantFx.specsFor(plant), pos, xy[0], xy[1]);
    }

    private void spawnMissingDeathBlasts(IdentityHashMap<PlantInstance, float[]> deathBlastNow) {
        for (var entry : deathBlastSeen.entrySet()) {
            if (deathBlastNow.containsKey(entry.getKey())) {
                continue;
            }
            float[] xy = entry.getValue();
            spawnExplosionSpecs(ExplosivePlantFx.specsFor(entry.getKey()), null, xy[0], xy[1]);
        }
    }

    private void spawnExplosionSpecs(List<ExplosivePlantFx.Spec> specs, Point pos, float x, float y) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        for (ExplosivePlantFx.Spec spec : specs) {
            if (spec.placement() == ExplosivePlantFx.Placement.ALONG_LANE && pos != null) {
                int row = pos.getY();
                for (int col = 0; col < layout.cols(); col++) {
                    float[] tile = layout.centerOf(row, col);
                    addEffect(spec.layer(), spec.pamPath(), ExplosivePlantFx.jalapenoClip(col),
                            tile[0], tile[1], AnimScale.PLANT);
                }
            } else {
                addEffect(spec.layer(), spec.pamPath(), spec.clipName(), x, y, AnimScale.PLANT);
            }
        }
    }

    private void addEffect(ExplosivePlantFx.Layer layer, String pamPath, String clipName,
                           float x, float y, float scale) {
        OneShotFx fx = new OneShotFx(pamPath, clipName, x, y, scale);
        if (layer == ExplosivePlantFx.Layer.BACK) {
            backEffects.add(fx);
        } else {
            frontEffects.add(fx);
        }
    }

    private void harvestProjectileHits(GameModel model) {
        List<Projectile> hits = model.drainProjectileHits();
        for (Projectile projectile : hits) {
            if (projectile == null) {
                continue;
            }
            projectileWorldCenter(projectile, xyTmp);
            spawnProjectileHit(projectile, xyTmp[0], xyTmp[1]);
        }
    }

    private void spawnProjectileHit(Projectile projectile, float x, float y) {
        ProjectilePamPaths.HitPam hit = ProjectilePamPaths.hitFor(projectile);
        if (hit == null || hit.path() == null) {
            return;
        }
        String clip = hit.clip() != null ? hit.clip() : ProjectilePamPaths.CLIP_PREFERENCES[0];
        frontEffects.add(new OneShotFx(hit.path(), clip, x, y, AnimScale.PROJECTILE));
    }

    private void drawEffects(Batch batch, List<OneShotFx> effects, float delta) {
        Iterator<OneShotFx> it = effects.iterator();
        while (it.hasNext()) {
            OneShotFx fx = it.next();
            ClipRef ref = clips.getOrLoad(fx.pamPath, fx.clipName);
            if (ref == null) {
                continue;
            }
            if (!fx.started) {
                fx.started = true;
                fx.time = 0f;
                fx.duration = clipDurationSeconds(ref, fx.pamPath, fx.clipName);
            } else {
                fx.time += delta;
            }
            player.draw(batch, ref, fx.time, fx.x, fx.y, fx.scale, fx.scale, false);
            if (fx.duration > 0f && fx.time >= fx.duration) {
                it.remove();
            }
        }
    }

    private float clipDurationSeconds(ClipRef ref, String pamPath, String clipName) {
        float seconds = player.clipDurationSeconds(pamPath, clipName);
        if (seconds > 0f) {
            return seconds;
        }
        if (ref != null && ref.duration > 0f) {
            return ref.duration;
        }
        return 1.5f;
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
        projectileWorldCenter(projectile, xyTmp);
        drawPose(batch, projectile, pose, xyTmp[0], xyTmp[1], AnimScale.PROJECTILE, delta, pose.cacheKey());
    }

    private void projectileWorldCenter(Projectile projectile, float[] out) {
        float[] xy = layout.centerOf(projectile.getY(), projectile.getX());
        out[0] = xy[0];
        out[1] = xy[1];
        if (projectile instanceof Splash splash) {
            out[1] += splash.getVisualHeight() * layout.cellHeight();
        }
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

    /**
     * Back rows first, then layer: GROUND → MAIN → OVERLAY.
     */
    private static int compareDrawOrder(PlantInstance a, PlantInstance b) {
        int rowA = rowOf(a);
        int rowB = rowOf(b);
        if (rowA != rowB) {
            return Integer.compare(rowA, rowB);
        }
        return Integer.compare(layerOrdinal(a), layerOrdinal(b));
    }

    private static int rowOf(PlantInstance plant) {
        Point pos = plant == null ? null : plant.getPosition();
        return pos == null ? 0 : pos.getY();
    }

    private static int layerOrdinal(PlantInstance plant) {
        PlacableLayer layer = plant == null ? null : plant.getLayer();
        return layer == null ? PlacableLayer.MAIN.ordinal() : layer.ordinal();
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

    private static final class OneShotFx {
        final String pamPath;
        final String clipName;
        final float x;
        final float y;
        final float scale;
        float time;
        float duration;
        boolean started;

        OneShotFx(String pamPath, String clipName, float x, float y, float scale) {
            this.pamPath = pamPath;
            this.clipName = clipName;
            this.x = x;
            this.y = y;
            this.scale = scale > 0f ? scale : 1f;
        }
    }
}
