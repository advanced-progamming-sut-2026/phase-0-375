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
final class LawnPickupRenderer {
    private final LawnEntityRenderer r;

    LawnPickupRenderer(LawnEntityRenderer r) {
        this.r = r;
    }

    void drawSuns(Batch batch, GameModel model, float delta) {
        IdentityHashMap<Sun, StealSunBehavior.SunPull> pulled = pulledSuns(model);
        for (ZombieInstance zombie : model.getZombies()) {
            StealSunBehavior steal = (StealSunBehavior) zombie.getBehavior(ZombieBehaviorType.STEAL_SUN);
            if (steal == null || steal.getPulls().isEmpty()) {
                continue;
            }
            if (!r.zombieWorldCenter(zombie, r.xyTmp)) {
                continue;
            }
            float destX = r.xyTmp[0];
            float destY = r.xyTmp[1];
            for (StealSunBehavior.SunPull pull : steal.getPulls()) {
                Sun sun = pull.sun();
                if (sun == null) {
                    continue;
                }
                float[] start = pullWorld(pull);
                float u = Math.max(0f, Math.min(1f, pull.t()));
                u = u * u * (3f - 2f * u);
                drawSunToken(batch, sun,
                    start[0] + (destX - start[0]) * u,
                    start[1] + (destY - start[1]) * u,
                    delta);
            }
        }
        List<Sun> tokens = model.getActiveSuns();
        if (tokens == null) {
            drawSunFlights(batch, delta);
            return;
        }
        for (Sun sun : tokens) {
            if (pulled.containsKey(sun)) {
                continue;
            }
            writeSunDrawPos(sun, r.xyTmp);
            drawSunToken(batch, sun, r.xyTmp[0], r.xyTmp[1], delta);
        }
        drawSunFlights(batch, delta);
    }

    Sun pickSun(GameModel model, float worldX, float worldY) {
        if (model == null || model.getActiveSuns() == null) {
            return null;
        }
        IdentityHashMap<Sun, StealSunBehavior.SunPull> pulled = pulledSuns(model);
        Sun best = null;
        float bestD = 0f;
        for (Sun sun : model.getActiveSuns()) {
            if (pulled.containsKey(sun)) {
                continue;
            }
            writeSunDrawPos(sun, r.xyTmp);
            if (!SunCollect.hits(r.xyTmp[0], r.xyTmp[1], worldX, worldY)) {
                continue;
            }
            float dx = worldX - r.xyTmp[0];
            float dy = worldY - r.xyTmp[1];
            float d = dx * dx + dy * dy;
            if (best == null || d < bestD) {
                best = sun;
                bestD = d;
            }
        }
        return best;
    }

    void writeSunDrawPos(Sun sun, float[] out) {
        float[] dest = sunWorld(sun);
        float x = dest[0];
        float y = dest[1];
        if (sun.isFalling()) {
            float t = Math.max(0f, Math.min(1f, sun.fallProgress()));
            t = t * t * (3f - 2f * t);
            float[] start = sun.hasOrigin()
                ? originWorld(sun)
                : new float[]{dest[0], LawnLayout.WORLD_HEIGHT};
            x = start[0] + (dest[0] - start[0]) * t;
            y = start[1] + (dest[1] - start[1]) * t;
        }
        out[0] = x;
        out[1] = y;
    }

    void startSunCollect(Sun sun, float x0, float y0, float x1, float y1) {
        if (sun == null) {
            return;
        }
        r.sunFlights.add(new SunFlight(sun, x0, y0, x1, y1));
    }

    IdentityHashMap<Sun, StealSunBehavior.SunPull> pulledSuns(GameModel model) {
        IdentityHashMap<Sun, StealSunBehavior.SunPull> pulled = new IdentityHashMap<>();
        if (model.getZombies() == null) {
            return pulled;
        }
        for (ZombieInstance zombie : model.getZombies()) {
            StealSunBehavior steal = (StealSunBehavior) zombie.getBehavior(ZombieBehaviorType.STEAL_SUN);
            if (steal == null || steal.getPulls().isEmpty()) {
                continue;
            }
            for (StealSunBehavior.SunPull pull : steal.getPulls()) {
                if (pull.sun() != null) {
                    pulled.put(pull.sun(), pull);
                }
            }
        }
        return pulled;
    }

    void drawSunFlights(Batch batch, float delta) {
        for (int i = r.sunFlights.size() - 1; i >= 0; i--) {
            SunFlight flight = r.sunFlights.get(i);
            flight.elapsed += delta;
            if (SunCollect.done(flight.elapsed)) {
                r.sunFlights.remove(i);
                continue;
            }
            float x = flight.x1;
            float y = flight.y1;
            float sx = 1f;
            float sy = 1f;
            if (SunCollect.flying(flight.elapsed)) {
                float u = SunCollect.flyU(flight.elapsed);
                x = flight.x0 + (flight.x1 - flight.x0) * u;
                y = flight.y0 + (flight.y1 - flight.y0) * u;
            } else {
                float u = SunCollect.vanishU(flight.elapsed);
                sx = GraveAnim.scaleX(u);
                sy = GraveAnim.scaleY(u);
            }
            drawSunToken(batch, flight.sun, x, y, delta, sx, sy);
        }
    }

    float[] pullWorld(StealSunBehavior.SunPull pull) {
        float[] xy = r.layout.centerOf(pull.startRow(), pull.startCol());
        xy[0] += pull.startOffsetX() * r.layout.cellWidth();
        xy[1] += pull.startOffsetY() * r.layout.cellHeight();
        if (pull.startFallDuration() > 0f && pull.startFallRemaining() > 0f) {
            float t = 1f - pull.startFallRemaining() / pull.startFallDuration();
            t = Math.max(0f, Math.min(1f, t));
            xy[1] = LawnLayout.WORLD_HEIGHT + (xy[1] - LawnLayout.WORLD_HEIGHT) * t;
        }
        return xy;
    }

    float[] sunWorld(Sun sun) {
        float[] xy = r.layout.centerOf(sun.getY(), sun.getX());
        xy[0] += sun.getOffsetX() * r.layout.cellWidth();
        xy[1] += sun.getOffsetY() * r.layout.cellHeight();
        return xy;
    }

    float[] originWorld(Sun sun) {
        return r.layout.centerOf(sun.getOriginY(), sun.getOriginX());
    }

    void drawSunToken(Batch batch, Sun sun, float x, float y, float delta) {
        drawSunToken(batch, sun, x, y, delta, 1f, 1f);
    }

    void drawSunToken(Batch batch, Sun sun, float x, float y, float delta,
                              float sxN, float syN) {
        if (r.catalog == null) {
            return;
        }
        String pamName = sunPam(sun);
        PamCatalog.PamEntry entry = r.catalog.byName(pamName);
        if (entry == null && !LawnEntityDrawConstants.SUN_PAM.equals(pamName)) {
            entry = r.catalog.byName(LawnEntityDrawConstants.SUN_PAM);
        }
        if (entry == null) {
            return;
        }
        String[] preferred = preferredClips(sun);
        String clip = r.catalog.resolveClip(entry, preferred);
        boolean loop = sun == null || !sun.isTransitioning();
        AnimPose pose = loop ? AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE)
                             : AnimPose.once(entry.path(), clip, ZombieAnimRole.IDLE);
        float baseScale = AnimScale.SUN * sunScale(sun);
        float stateTime;
        if (sxN == 1f && syN == 1f) {
            stateTime = r.drawPose(batch, sun, pose, x, y, baseScale, LawnEntityDrawConstants.NO_PHASE, 0f, delta);
        } else {
            r.seenThisFrame.add(sun);
            ClipRef ref = r.clips.getOrLoad(pose.pamPath(), pose.clipName());
            if (ref == null) {
                return;
            }
            stateTime = r.advanceClock(sun, pose.cacheKey(), delta);
            r.player.draw(batch, ref, stateTime, x, y,
                baseScale * sxN, baseScale * syN, pose.loop());
        }
        if (sun != null && sun.isTransitioning()) {
            float dur = PamCatalog.clipDurationSeconds(entry, clip);
            if (dur <= 0f) {
                dur = 0.5f;
            }
            if (stateTime >= dur) {
                sun.completeTransition();
            }
        }
    }

    void harvestRadioactiveSunExplosions(GameModel model) {
        if (model == null || r.catalog == null) {
            return;
        }
        List<Point> explosions = model.drainRadioactiveSunExplosions();
        if (explosions == null || explosions.isEmpty()) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(LawnEntityDrawConstants.SUN_BOMB_PAM);
        if (entry == null) {
            entry = r.catalog.byName(LawnEntityDrawConstants.SUN_PAM);
        }
        if (entry == null) {
            return;
        }
        String clip = r.catalog.resolveClip(entry, "attack", "animation");
        for (Point pt : explosions) {
            float[] xy = r.layout.centerOf(pt.getY(), pt.getX());
            r.frontEffects.add(new OneShotFx(entry.path(), clip, xy[0], xy[1], AnimScale.SUN * 1.25f, false));
        }
    }

    static String sunPam(Sun sun) {
        if (sun == null || sun.getType() == null) {
            return LawnEntityDrawConstants.SUN_PAM;
        }
        if (sun.getType() == SunType.RADIOACTIVE || sun.isTransitioning() || sun.isTransitioned()) {
            return LawnEntityDrawConstants.SUN_BOMB_PAM;
        }
        return LawnEntityDrawConstants.SUN_PAM;
    }

    static float sunScale(Sun sun) {
        if (sun == null || sun.getType() == null) {
            return 1f;
        }
        if (sun.isTransitioned()) {
            return 1f;
        }
        return switch (sun.getType()) {
            case SPECIAL -> 1.10f;
            case RADIOACTIVE -> 1.25f;
            default -> 1f;
        };
    }

    static String[] preferredClips(Sun sun) {
        if (sun == null || sun.getType() == null) {
            return new String[]{"animation"};
        }
        if (sun.isTransitioning()) {
            return new String[]{"transition", "animation"};
        }
        if (sun.isTransitioned()) {
            return new String[]{"normalSunIdle", "animation"};
        }
        if (sun.getType() == SunType.RADIOACTIVE) {
            if (sun.isFalling()) {
                float p = sun.fallProgress();
                if (p < 0.333f) {
                    return new String[]{"animation", "animation2", "animation3", "blue"};
                } else if (p < 0.666f) {
                    return new String[]{"animation2", "animation3", "animation", "blue"};
                } else {
                    return new String[]{"animation3", "animation2", "animation", "blue"};
                }
            }
            return new String[]{"animation3", "animation2", "animation", "blue"};
        }
        if (sun.getType() == SunType.SPECIAL) {
            return new String[]{"red", "animation"};
        }
        return new String[]{"animation"};
    }

    static String sunClip(Sun sun) {
        String[] preferred = preferredClips(sun);
        return preferred[0];
    }

    void drawPlantFood(Batch batch, GameModel model, float delta) {
        List<PlantFoodPickup> tokens = model.getActivePlantFood();
        if (tokens != null && !tokens.isEmpty()) {
            for (PlantFoodPickup food : tokens) {
                writePlantFoodDrawPos(food, r.xyTmp);
                drawPlantFoodToken(batch, food, r.xyTmp[0], r.xyTmp[1], delta, "idle");
            }
        }
        drawPlantFoodFlights(batch, delta);
    }

    PlantFoodPickup pickPlantFood(GameModel model, float worldX, float worldY) {
        if (model == null || model.getActivePlantFood() == null) {
            return null;
        }
        PlantFoodPickup best = null;
        float bestD = 0f;
        for (PlantFoodPickup food : model.getActivePlantFood()) {
            writePlantFoodDrawPos(food, r.xyTmp);
            if (!SunCollect.hits(r.xyTmp[0], r.xyTmp[1], worldX, worldY)) {
                continue;
            }
            float dx = worldX - r.xyTmp[0];
            float dy = worldY - r.xyTmp[1];
            float d = dx * dx + dy * dy;
            if (best == null || d < bestD) {
                best = food;
                bestD = d;
            }
        }
        return best;
    }

    void writePlantFoodDrawPos(PlantFoodPickup food, float[] out) {
        float[] xy = r.layout.centerOf(food.getY(), food.getX());
        out[0] = xy[0] + food.getOffsetX() * r.layout.cellWidth();
        out[1] = xy[1] + food.getOffsetY() * r.layout.cellHeight();
    }

    /**
     * Kicks off the collect-flight animation: the orb lerps from
     * {@code (x0, y0)} to {@code (x1, y1)} (the HUD logo) and then plays a
     * grave-style squash/stretch vanish at the destination. The pickup has
     * already been removed from the model by the caller, so the flight is
     * purely cosmetic.
     */
    void startPlantFoodCollect(PlantFoodPickup food, float x0, float y0, float x1, float y1) {
        if (food == null) {
            return;
        }
        r.plantFoodFlights.add(new PlantFoodFlight(food, x0, y0, x1, y1));
    }

    void drawPlantFoodFlights(Batch batch, float delta) {
        for (int i = r.plantFoodFlights.size() - 1; i >= 0; i--) {
            PlantFoodFlight flight = r.plantFoodFlights.get(i);
            flight.elapsed += delta;
            if (PlantFoodCollect.done(flight.elapsed)) {
                r.plantFoodFlights.remove(i);
                continue;
            }
            float x = flight.x1;
            float y = flight.y1;
            float sx = 1f;
            float sy = 1f;
            if (PlantFoodCollect.flying(flight.elapsed)) {
                float u = PlantFoodCollect.flyU(flight.elapsed);
                x = flight.x0 + (flight.x1 - flight.x0) * u;
                y = flight.y0 + (flight.y1 - flight.y0) * u;
            } else {
                float u = PlantFoodCollect.vanishU(flight.elapsed);
                sx = GraveAnim.scaleX(u);
                sy = GraveAnim.scaleY(u);
            }
            drawPlantFoodToken(batch, flight.food, x, y, delta, sx, sy, "idle");
        }
    }

    void drawGlowingPlantFoodOverlay(Batch batch, ZombieInstance zombie,
                                             float x, float y, float delta) {
        drawPlantFoodToken(batch, zombie, x, y, delta, "animation2", "animation");
    }

    void drawPlantFoodToken(Batch batch, Object clockKey, float x, float y, float delta,
                                    String... clipPrefs) {
        drawPlantFoodToken(batch, clockKey, x, y, delta, 1f, 1f, clipPrefs);
    }

    /**
     * Scale-aware variant used by {@link #drawPlantFoodFlights} so the orb can
     * squash/stretch into the bank logo the same way a collected sun does.
     */
    void drawPlantFoodToken(Batch batch, Object clockKey, float x, float y, float delta,
                                    float sxN, float syN, String... clipPrefs) {
        if (r.catalog == null || clipPrefs == null || clipPrefs.length == 0) {
            return;
        }
        PamCatalog.PamEntry entry = r.catalog.byName(LawnEntityDrawConstants.PLANTFOOD_PICKUP_PAM);
        if (entry == null) {
            return;
        }
        String clip = r.catalog.resolveClip(entry, clipPrefs);
        AnimPose pose = AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
        if (sxN == 1f && syN == 1f) {
            r.drawPose(batch, clockKey, pose, x, y, AnimScale.SUN, LawnEntityDrawConstants.NO_PHASE, 0f, delta);
            return;
        }
        r.seenThisFrame.add(clockKey);
        ClipRef ref = r.clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return;
        }
        float stateTime = r.advanceClock(clockKey, pose.cacheKey(), delta);
        r.player.draw(batch, ref, stateTime, x, y,
            AnimScale.SUN * sxN, AnimScale.SUN * syN, pose.loop());
    }

    void drawLoot(Batch batch, GameModel model, float delta) {
        List<LootPickup> tokens = model == null ? null : model.getActiveLootPickups();
        if (tokens != null) {
            for (LootPickup loot : tokens) {
                writeLootDrawPos(loot, r.xyTmp);
                drawLootToken(batch, loot, r.xyTmp[0], r.xyTmp[1], delta, 1f, 1f);
            }
        }
        drawLootFlights(batch, delta);
    }

    void writeLootDrawPos(LootPickup loot, float[] out) {
        float[] xy = r.layout.centerOf(loot.getY(), loot.getX());
        out[0] = xy[0] + loot.getOffsetX() * r.layout.cellWidth();
        out[1] = xy[1] + loot.getOffsetY() * r.layout.cellHeight();
    }

    void startLootCollect(LootPickup loot, float x0, float y0, float x1, float y1,
                                 Runnable onComplete) {
        if (loot == null) {
            return;
        }
        r.lootFlights.add(new LootFlight(loot, x0, y0, x1, y1, onComplete));
    }

    void drainPendingLootFlights() {
        for (int i = r.lootFlights.size() - 1; i >= 0; i--) {
            LootFlight flight = r.lootFlights.get(i);
            if (!flight.done && flight.onComplete != null) {
                flight.onComplete.run();
                flight.done = true;
            }
        }
        r.lootFlights.clear();
    }

    void drawLootFlights(Batch batch, float delta) {
        for (int i = r.lootFlights.size() - 1; i >= 0; i--) {
            LootFlight flight = r.lootFlights.get(i);
            flight.elapsed += delta;
            if (LootCollect.done(flight.elapsed)) {
                if (!flight.done && flight.onComplete != null) {
                    flight.onComplete.run();
                    flight.done = true;
                }
                r.lootFlights.remove(i);
                continue;
            }
            float x = flight.x1;
            float y = flight.y1;
            float sx = 1f;
            float sy = 1f;
            if (LootCollect.flying(flight.elapsed)) {
                float u = LootCollect.flyU(flight.elapsed);
                x = flight.x0 + (flight.x1 - flight.x0) * u;
                y = flight.y0 + (flight.y1 - flight.y0) * u;
            } else {
                float u = LootCollect.vanishU(flight.elapsed);
                sx = GraveAnim.scaleX(u);
                sy = GraveAnim.scaleY(u);
            }
            drawLootToken(batch, flight.loot, x, y, delta, sx, sy);
        }
    }

    void drawLootToken(Batch batch, LootPickup loot, float x, float y, float delta,
                               float sxN, float syN) {
        if (loot == null) {
            return;
        }
        if (loot.getKind() == LootPickupKind.FLOWER_POT) {
            drawFlowerPotToken(batch, loot, x, y, sxN, syN);
            return;
        }
        if (r.catalog == null) {
            return;
        }
        String pamName = switch (loot.getKind()) {
            case COIN_GOLD -> LawnEntityDrawConstants.COIN_GOLD_PAM;
            case COIN_SILVER -> LawnEntityDrawConstants.COIN_SILVER_PAM;
            case DIAMOND -> LawnEntityDrawConstants.COIN_DIAMOND_PAM;
            case FLOWER_POT -> null;
        };
        String clip = loot.getKind() == LootPickupKind.DIAMOND ? "idle" : "animation";
        PamCatalog.PamEntry entry = pamName == null ? null : r.catalog.byName(pamName);
        if (entry == null) {
            return;
        }
        String resolved = r.catalog.resolveClip(entry, clip);
        AnimPose pose = AnimPose.looping(entry.path(), resolved, ZombieAnimRole.IDLE);
        float scale = loot.getKind() == LootPickupKind.DIAMOND
            ? AnimScale.LOOT_GEM
            : AnimScale.LOOT_COIN;
        if (sxN == 1f && syN == 1f) {
            r.drawPose(batch, loot, pose, x, y, scale, LawnEntityDrawConstants.NO_PHASE, 0f, delta);
            return;
        }
        r.seenThisFrame.add(loot);
        ClipRef ref = r.clips.getOrLoad(pose.pamPath(), pose.clipName());
        if (ref == null) {
            return;
        }
        float stateTime = r.advanceClock(loot, pose.cacheKey(), delta);
        r.player.draw(batch, ref, stateTime, x, y,
            scale * sxN, scale * syN, pose.loop());
    }

    void drawFlowerPotToken(Batch batch, LootPickup loot, float x, float y,
                                    float sxN, float syN) {
        if (r.textures == null) {
            return;
        }
        if (r.flowerPotRegion == null) {
            r.flowerPotRegion = r.textures.region(LawnEntityDrawConstants.FLOWER_POT_REGION);
        }
        if (r.flowerPotRegion == null) {
            return;
        }
        r.seenThisFrame.add(loot);
        float h = LawnEntityDrawConstants.FLOWER_POT_DRAW_H * syN;
        float w = r.flowerPotRegion.getRegionHeight() <= 0
            ? h
            : h * (r.flowerPotRegion.getRegionWidth() / (float) r.flowerPotRegion.getRegionHeight());
        w *= sxN;
        batch.draw(r.flowerPotRegion, x - w * 0.5f, y - h * 0.5f, w, h);
    }
}
