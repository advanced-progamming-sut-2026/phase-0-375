package model.game.core;

import model.game.map.Cell;
import model.game.map.Point;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Presentation-only lawn FX: storms, slides, cues, announcements. */
final class LawnPresentationFx {

    private final List<model.projectile.Projectile> projectileHitCues = new ArrayList<>();
    private final List<Point> radioactiveSunExplosionCues = new ArrayList<>();
    private final List<Point> slideStartCues = new ArrayList<>();
    private final Map<ZombieInstance, GameModel.ArmedSlide> armedSlides = new HashMap<>();
    private final List<LaneSlide> laneSlides = new ArrayList<>();
    private final List<LaneSlide> laneSlidesView = Collections.unmodifiableList(laneSlides);
    private final List<WaterEmerge> waterEmerges = new ArrayList<>();
    private final List<WaterEmerge> waterEmergesView = Collections.unmodifiableList(waterEmerges);
    private final ArrayDeque<String> pendingAnnouncements = new ArrayDeque<>();
    private final List<SandstormSpawn> pendingSandstorms = new ArrayList<>();
    private final List<SandstormSpawn> sandstormsView =
            Collections.unmodifiableList(pendingSandstorms);
    private final List<IceWindGust> iceWinds = new ArrayList<>();
    private final List<IceWindGust> iceWindsView = Collections.unmodifiableList(iceWinds);

    void clear() {
        projectileHitCues.clear();
        pendingSandstorms.clear();
        iceWinds.clear();
        pendingAnnouncements.clear();
        slideStartCues.clear();
        radioactiveSunExplosionCues.clear();
        armedSlides.clear();
        laneSlides.clear();
        waterEmerges.clear();
    }

    void recordProjectileHit(model.projectile.Projectile projectile) {
        if (projectile != null) {
            projectileHitCues.add(projectile);
        }
    }

    List<model.projectile.Projectile> drainProjectileHits() {
        if (projectileHitCues.isEmpty()) {
            return List.of();
        }
        List<model.projectile.Projectile> drained = new ArrayList<>(projectileHitCues);
        projectileHitCues.clear();
        return drained;
    }

    void discardUnreadProjectileHits() {
        projectileHitCues.clear();
    }

    void recordRadioactiveSunExplosion(int col, int row) {
        radioactiveSunExplosionCues.add(new Point(col, row));
    }

    List<Point> drainRadioactiveSunExplosions() {
        if (radioactiveSunExplosionCues.isEmpty()) {
            return List.of();
        }
        List<Point> drained = new ArrayList<>(radioactiveSunExplosionCues);
        radioactiveSunExplosionCues.clear();
        return drained;
    }

    void beginWaterEmerge(ZombieInstance zombie) {
        if (zombie != null) {
            waterEmerges.add(new WaterEmerge(zombie));
        }
    }

    List<WaterEmerge> waterEmerges() {
        return waterEmergesView;
    }

    boolean isWaterEmerging(ZombieInstance zombie) {
        if (zombie == null || waterEmerges.isEmpty()) {
            return false;
        }
        for (WaterEmerge emerge : waterEmerges) {
            if (emerge.getZombie() == zombie) {
                return true;
            }
        }
        return false;
    }

    void armLaneSlide(ZombieInstance zombie, Cell slideTile, int toRow) {
        if (zombie == null || slideTile == null) {
            return;
        }
        armedSlides.put(zombie,
                new GameModel.ArmedSlide(slideTile.getColumn(), slideTile.getRow(), toRow));
    }

    boolean tickArmedSlide(GameModel model, ZombieInstance zombie, float continuousX) {
        if (zombie == null || zombie.isMovingBackward()) {
            return false;
        }
        GameModel.ArmedSlide armed = armedSlides.get(zombie);
        if (armed == null || continuousX > armed.getTileColumn()) {
            return false;
        }
        armedSlides.remove(zombie);
        model.moveZombieToLane(zombie, armed.getToRow());
        if (armed.getFromRow() != armed.getToRow()) {
            laneSlides.add(new LaneSlide(zombie, armed.getFromRow(), armed.getToRow()));
        }
        slideStartCues.add(new Point(armed.getTileColumn(), armed.getFromRow()));
        return true;
    }

    List<Point> drainSlideStarts() {
        if (slideStartCues.isEmpty()) {
            return List.of();
        }
        List<Point> drained = new ArrayList<>(slideStartCues);
        slideStartCues.clear();
        return drained;
    }

    void discardUnreadSlideStarts() {
        slideStartCues.clear();
    }

    List<LaneSlide> laneSlides() {
        return laneSlidesView;
    }

    void queueSandstormSpawn(GameModel model, Zombie zombie, int lane, int columnsAhead) {
        pendingSandstorms.add(new SandstormSpawn(model, zombie, lane, columnsAhead));
    }

    List<SandstormSpawn> sandstorms() {
        return sandstormsView;
    }

    void queueIceWindGust(int lane) {
        if (lane >= 0) {
            iceWinds.add(new IceWindGust(lane));
        }
    }

    List<IceWindGust> iceWinds() {
        return iceWindsView;
    }

    void enqueueAnnouncement(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        pendingAnnouncements.addLast(text);
    }

    String consumeAnnouncement() {
        return pendingAnnouncements.pollFirst();
    }

    void tick(float deltaTime) {
        tickSandstorms(deltaTime);
        tickIceWinds(deltaTime);
        tickLaneSlides(deltaTime);
        tickWaterEmerges(deltaTime);
    }

    private void tickSandstorms(float deltaTime) {
        if (pendingSandstorms.isEmpty()) {
            return;
        }
        Iterator<SandstormSpawn> iterator = pendingSandstorms.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(deltaTime)) {
                iterator.remove();
            }
        }
    }

    private void tickIceWinds(float deltaTime) {
        if (iceWinds.isEmpty()) {
            return;
        }
        Iterator<IceWindGust> iterator = iceWinds.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(deltaTime)) {
                iterator.remove();
            }
        }
    }

    private void tickLaneSlides(float deltaTime) {
        armedSlides.keySet().removeIf(ZombieInstance::isDead);
        if (laneSlides.isEmpty()) {
            return;
        }
        Iterator<LaneSlide> iterator = laneSlides.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(deltaTime)) {
                iterator.remove();
            }
        }
    }

    private void tickWaterEmerges(float deltaTime) {
        if (waterEmerges.isEmpty()) {
            return;
        }
        Iterator<WaterEmerge> iterator = waterEmerges.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(deltaTime)) {
                iterator.remove();
            }
        }
    }
}
