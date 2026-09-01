package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import model.enums.Chapter;
import model.game.core.GameModel;
import model.game.map.Cell;
import model.game.map.GameMap;
import model.item.pushable.Pushable;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Per-frame draw / prune pipeline extracted from {@link LawnEntityRenderer}. */
final class LawnEntityFramePass {

    private final LawnEntityRenderer r;

    LawnEntityFramePass(LawnEntityRenderer renderer) {
        this.r = renderer;
    }

    void draw(Batch batch, GameModel model, float delta) {
        if (model == null) {
            return;
        }
        beginDrawFrame(delta);
        syncDestroyedEntities(model);
        Set<Pushable> liveCabinets = liveCabinets(model);
        Set<Cell> liveIce = r.terrain.syncTerrainIce(model);
        Set<Cell> liveSlides = r.terrain.syncSlideTiles(model);
        r.terrain.harvestSlideStarts(model);
        float plantDelta = r.endMode == LawnEntityRenderer.EndMode.WIN ? 0f : delta;
        harvestAndDrawBackFx(batch, model, delta, plantDelta);
        drawLawnRows(batch, model, delta, plantDelta, liveCabinets, liveIce, liveSlides);
        drawAboveRows(batch, model, delta);
        pruneDrawState(model);
        r.drawEndLevel(batch, model);
    }

    private void beginDrawFrame(float delta) {
        r.snorkelRippleTime += Math.max(0f, delta);
        r.seenThisFrame.clear();
    }

    private void syncDestroyedEntities(GameModel model) {
        Set<ZombieInstance> alive = new HashSet<>(model.getZombies());
        for (ZombieInstance zombie : r.lastLive.keySet()) {
            if (!alive.contains(zombie)) {
                r.deathSpawn.spawnDeath(model, zombie, r.lastLive.get(zombie));
            }
        }
        r.lastLive.entrySet().removeIf(e -> !alive.contains(e.getKey()));
        Set<Pushable> liveCabinets = liveCabinets(model);
        for (Pushable cabinet : r.lastCabinets.keySet()) {
            if (!liveCabinets.contains(cabinet)) {
                r.prop.spawnCabinetDeath(r.lastCabinets.get(cabinet));
            }
        }
        r.lastCabinets.entrySet().removeIf(e -> !liveCabinets.contains(e.getKey()));
    }

    private Set<Pushable> liveCabinets(GameModel model) {
        Set<Pushable> liveCabinets = new HashSet<>();
        for (ZombieInstance zombie : model.getZombies()) {
            LawnPropRenderer.collectLiveCabinet(zombie.getPushableItem(), liveCabinets);
        }
        if (model.getOrphanedPushables() != null) {
            for (Pushable orphan : model.getOrphanedPushables()) {
                LawnPropRenderer.collectLiveCabinet(orphan, liveCabinets);
            }
        }
        return liveCabinets;
    }

    private void harvestAndDrawBackFx(Batch batch, GameModel model, float delta, float plantDelta) {
        List<PlantInstance> plants = model.getAllPlants();
        IdentityHashMap<PlantInstance, float[]> deathBlastNow = new IdentityHashMap<>();
        for (PlantInstance plant : plants) {
            r.plantFx.maybeSpawnPlantExplosion(plant, deathBlastNow);
            r.plantFx.maybeSpawnMeleeFx(plant);
            r.plantFx.updateMeleeIdlePulse(plant);
        }
        r.plantFx.spawnMissingDeathBlasts(deathBlastNow);
        r.deathBlastSeen.clear();
        r.deathBlastSeen.putAll(deathBlastNow);
        r.plantFx.harvestBeghouledClears(model);
        r.plantFx.drawEffects(batch, r.backEffects, delta);
        r.prop.prepareBowlingWalnuts(model);
        r.terrain.updateSandstorms(model, delta);
        r.terrain.updateIceWinds(model, delta);
        r.terrain.updateLaneGlides(model);
        r.terrain.updateWaterEmerges(model);
        for (PlantInstance plant : plants) {
            if (LawnEntityRenderer.plantRow(plant) < 0) {
                r.plantFx.drawPlant(batch, plant, plantDelta);
            }
        }
    }

    private void drawLawnRows(Batch batch, GameModel model, float delta, float plantDelta,
                              Set<Pushable> liveCabinets, Set<Cell> liveIce, Set<Cell> liveSlides) {
        List<PlantInstance> plants = model.getAllPlants();
        Set<PlantInstance> livePlants = Collections.newSetFromMap(new IdentityHashMap<>());
        livePlants.addAll(plants);
        GameMap map = model.getMap();
        int rows = map != null ? map.getRows() : r.layout.rows();
        for (int row = 0; row < rows; row++) {
            drawOneLawnRow(batch, model, delta, plantDelta, row, rows, plants, livePlants,
                    liveCabinets, liveIce, liveSlides);
        }
    }

    private void drawOneLawnRow(Batch batch, GameModel model, float delta, float plantDelta,
                                int row, int rows,
                                List<PlantInstance> plants, Set<PlantInstance> livePlants,
                                Set<Pushable> liveCabinets, Set<Cell> liveIce, Set<Cell> liveSlides) {
        r.terrain.drawCraters(batch, model, row);
        r.terrain.drawFireTiles(batch, model, delta, row);
        r.plantStatus.drawGraves(batch, model, delta, row);
        r.plantStatus.drawGraveGhosts(batch, delta, row);
        r.prop.drawVases(batch, model, delta, row, rows);
        for (PlantInstance plant : plants) {
            int lane = LawnEntityRenderer.plantRow(plant);
            if (lane >= 0 && LawnEntityRenderer.clampRow(lane, rows) == row) {
                r.plantFx.drawPlant(batch, plant, plantDelta);
            }
        }
        r.plantStatus.drawPlantGhosts(batch, livePlants, plantDelta, row);
        for (Pushable cabinet : liveCabinets) {
            int lane = cabinet.getRow();
            if (lane >= 0 && LawnEntityRenderer.clampRow(lane, rows) == row) {
                r.prop.drawPushable(batch, model, cabinet, delta);
            }
        }
        r.terrain.drawSlideTiles(batch, liveSlides, delta, row);
        r.terrain.drawTerrainIce(batch, model, liveIce, delta, row);
        for (ZombieInstance zombie : model.getZombies()) {
            if (LawnEntityRenderer.clampRow(LawnEntityRenderer.zombieRow(zombie), rows) == row
                    && !r.sandstormConcealed.contains(zombie)
                    && !LawnEntityRenderer.drawsAboveLawn(zombie)) {
                Chapter skin = r.artChapterFor(zombie, model.getChapter());
                r.zombieDraw.drawZombie(batch, zombie, skin, delta);
            }
        }
        r.prop.drawBowlingWalnuts(batch, delta, row, rows);
        r.deaths.drawDeathFx(batch, delta, row);
        r.deaths.drawArmorPops(batch, delta, row);
        r.deaths.drawHunterSplats(batch, delta, row);
        r.deaths.drawProspectorBlasts(batch, delta, row);
        r.drawMowers(batch, model, delta, row);
    }

    private void drawAboveRows(Batch batch, GameModel model, float delta) {
        for (ZombieInstance zombie : model.getZombies()) {
            if (LawnEntityRenderer.drawsAboveLawn(zombie) && !r.sandstormConcealed.contains(zombie)) {
                Chapter skin = r.artChapterFor(zombie, model.getChapter());
                r.zombieDraw.drawZombie(batch, zombie, skin, delta);
            }
        }
        r.terrain.drawSandstorms(batch);
        r.terrain.drawIceWinds(batch);
        r.octopus.drawOctopi(batch, model, delta);
        r.zomboss.drawZombossFireballs(batch, model, delta);
        r.zomboss.drawZombossMissiles(batch, model, delta);
        r.zomboss.drawZombossSharks(batch, model, delta);
        r.pickup.drawSuns(batch, model, delta);
        r.pickup.drawPlantFood(batch, model, delta);
        r.pickup.drawLoot(batch, model, delta);
        if (model.getProjectiles() != null) {
            for (Projectile projectile : model.getProjectiles()) {
                r.plantFx.drawProjectile(batch, projectile, delta);
            }
        }
        r.plantFx.harvestProjectileHits(model);
        r.pickup.harvestRadioactiveSunExplosions(model);
        r.plantFx.drawEffects(batch, r.frontEffects, delta);
    }

    private void pruneDrawState(GameModel model) {
        pruneFxClocks(model);
        pruneEntityClocks(model);
    }

    private void pruneFxClocks(GameModel model) {
        r.prop.pruneVaseAge(model);
        r.clocks.keySet().removeIf(key -> !r.seenThisFrame.contains(key));
        r.zombotanyHeadClocks.keySet().removeIf(zombie -> !r.seenThisFrame.contains(zombie));
        r.beghouledMotion.keySet().removeIf(plant -> !r.seenThisFrame.contains(plant));
        r.explosionSpawned.keySet().removeIf(plant -> !r.seenThisFrame.contains(plant));
        r.jalapenoFireSpawned.keySet().removeIf(zombie -> !r.seenThisFrame.contains(zombie));
        r.armorBreakFxEpoch.keySet().removeIf(plant -> !r.seenThisFrame.contains(plant));
        r.meleeAttackFxEpoch.keySet().removeIf(plant -> !r.seenThisFrame.contains(plant));
        r.meleePlantFoodFxSpawned.keySet().removeIf(plant -> !r.seenThisFrame.contains(plant));
        r.meleeIdlePulses.entrySet().removeIf(entry -> {
            if (r.seenThisFrame.contains(entry.getKey())) {
                return false;
            }
            r.backEffects.remove(entry.getValue());
            r.frontEffects.remove(entry.getValue());
            return true;
        });
        r.plantFoodFx.keySet().removeIf(plant -> !r.seenThisFrame.contains(plant));
        for (PlantInstance plant : new ArrayList<>(r.lastPlantIce.keySet())) {
            if (!r.seenThisFrame.contains(plant)) {
                r.plantIceIntro.remove(plant);
                r.plantIceClocks.remove(plant);
                r.plantStatus.spawnIceShatter(r.lastPlantIce.remove(plant));
            }
        }
    }

    private void pruneEntityClocks(GameModel model) {
        r.plantChillClocks.keySet().removeIf(plant -> !r.seenThisFrame.contains(plant));
        r.zombieChillClocks.keySet().removeIf(zombie ->
                !r.seenThisFrame.contains(zombie) || !zombie.isFrozen());
        r.zombieDangerElapsed.keySet().removeIf(zombie -> !r.seenThisFrame.contains(zombie));
        r.graveEmerge.keySet().removeIf(grave -> !r.seenThisFrame.contains(grave));
        r.sheepFx.keySet().removeIf(plant -> !r.seenThisFrame.contains(plant)
            && !plant.isTransformed());
        r.hitFlashes.entrySet().removeIf(e ->
            !r.seenThisFrame.contains(e.getKey()) && e.getValue().remaining <= 0f);
        r.lostHands.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        r.tossAlign.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        r.prospectorBlastSpawned.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        r.hunterSplatSeq.keySet().removeIf(zombie -> !model.getZombies().contains(zombie));
        r.octopusAlign.keySet().removeIf(shot -> !shot.isFlying());
        Set<ZombieInstance> keepArt = new HashSet<>(model.getZombies());
        r.terrain.collectIcedOccupants(model, keepArt);
        r.artChapters.keySet().removeIf(zombie -> !keepArt.contains(zombie));
    }
}
