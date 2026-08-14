package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import model.app.App;
import model.game.core.GameModel;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.AnimScale;
import view.gui.anim.PamClipCache;
import view.gui.anim.plant.PlantAnimAdapter;
import view.gui.anim.zombie.ZombieAnimAdapter;
import view.gui.anim.zombie.ZombieFootfallCurve;
import view.gui.anim.zombie.ZombieGait;
import view.gui.anim.zombie.ZombieGaitProfiles;
import view.gui.assets.PamCatalog;
import view.gui.assets.PvzAssets;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Draws plants and zombies on the lawn via libPVZ PAM clips.
 *
 * <p>Pipeline: model entity → {@link PlantAnimAdapter} / {@link ZombieAnimAdapter}
 * → {@link AnimPose} → {@link PamClipCache} → {@code PamPlayer.draw}.
 *
 * <p>TODO: projectiles, plant-food FX, mowers, and grid props.
 * TODO: sort draw order by row (back → front) then Y within a lane.
 * TODO: tint / freeze overlays from model status flags.
 */
public final class LawnEntityRenderer {
    private static final float NO_PHASE = -1f;

    private final LawnLayout layout;
    private final PlantAnimAdapter plantAdapter;
    private final ZombieAnimAdapter zombieAdapter;
    private final PamClipCache clips;
    private final pvz.libpvz.pam.PamPlayer player;

    private final IdentityHashMap<Object, AnimClock> clocks = new IdentityHashMap<>();
    private final IdentityHashMap<ClipRef, ZombieFootfallCurve> footfalls = new IdentityHashMap<>();
    private final Set<Object> seenThisFrame = new HashSet<>();
    private final float[] xyTmp = new float[3];

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
        drawPose(batch, plant, pose, xy[0], xy[1], AnimScale.PLANT, NO_PHASE, delta);
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

        float x = xyTmp[0];
        float phase = NO_PHASE;
        ZombieGait gait = gaitFor(zombie);
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref != null && gait.enabled() && ZombieAnimAdapter.isDistanceDriven(zombie, pose)) {
            // Walking is driven by travel, so a cycle always covers exactly one step and the
            // planted foot can be held still. Every other pose stays on the wall clock.
            // Hypnotized zombies walk the other way, so distance and the hold-back both flip.
            boolean backward = zombie.isMovingBackward();
            phase = gait.phaseAt(backward ? xyTmp[2] : -xyTmp[2]);
            float holdBack = gait.footLockOffsetTiles(phase, footfallFor(gait, ref)) * layout.cellWidth();
            x += backward ? -holdBack : holdBack;
        }
        drawPose(batch, zombie, pose, x, xyTmp[1], AnimScale.ZOMBIE, phase, delta);
    }

    private static ZombieGait gaitFor(ZombieInstance zombie) {
        return ZombieGaitProfiles.forZombie(
                zombie.getDefinition() == null ? null : zombie.getDefinition().getName());
    }

    /** Measuring walks every frame of the clip, so each walk cycle is read from the art once. */
    private ZombieFootfallCurve footfallFor(ZombieGait gait, ClipRef walkClip) {
        ZombieFootfallCurve footfall = footfalls.get(walkClip);
        if (footfall == null) {
            footfall = gait.measureFootfall(player, walkClip);
            footfalls.put(walkClip, footfall);
        }
        return footfall;
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
        out[2] = progressX;
        return true;
    }

    private void drawPose(Batch batch, Object entity, AnimPose pose,
                          float x, float y, float baseScale, float phase, float delta) {
        seenThisFrame.add(entity);
        ClipRef ref = clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return;
        }
        float stateTime = phase >= 0f
                ? phase * ref.duration
                : advanceClock(entity, pose.cacheKey(), delta);
        float scale = baseScale * pose.scale();
        if (pose.visibility() == null) {
            player.draw(batch, ref, stateTime, x, y, scale, scale, pose.loop());
        } else {
            player.draw(batch, ref, stateTime, x, y, scale, scale, pose.loop(), pose.visibility());
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
