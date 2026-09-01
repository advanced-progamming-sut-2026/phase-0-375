package view.gui.lawn;

import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.zombie.WizardAnim;
import model.item.LootPickup;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.zombie.armor.Armor;

import java.util.List;
import java.util.Map;

enum FireTileFxPhase {
        INTRO, IDLE, OUTRO
    }


final class FireTileFx {
        FireTileFxPhase phase = FireTileFxPhase.INTRO;
        float time;
    }


    final class BeghouledMotion {
        float fromX;
        float fromY;
        float toX;
        float toY;
        float t;
    }


    final class AnimClock {
        String clipKey;
        float time;
    }


    final class HitFlash {
        int vitality;
        float remaining;
        /** Seconds before a chew-sized drop may pulse again (plants only). */
        float quiet;
        List<Armor> prevDroppables;
    }


    final class ArmorPop {
        final String pamPath;
        final String clipName;
        /** PAM part to draw; {@code null} draws the whole clip (Gargantuar {@code particles}). */
        final String part;
        final float groundY;
        final float gravity;
        float clipTime;
        float x;
        float y;
        float vx;
        float vy;
        float fade;
        /** Seconds since the pop spawned. */
        float life;
        /** Don't start fading before this many seconds have passed, even once grounded. */
        float hold;
        boolean grounded;
        int bounces;
        int maxBounces = 2;

        ArmorPop(String pamPath, String clipName, String part,
                 float x, float y, float groundY, float vx, float vy, float gravity) {
            this.pamPath = pamPath;
            this.clipName = clipName;
            this.part = part;
            this.x = x;
            this.y = y;
            this.groundY = groundY;
            this.vx = vx;
            this.vy = vy;
            this.gravity = gravity;
        }
    }


    final class SunFlight {
        final Sun sun;
        final float x0;
        final float y0;
        final float x1;
        final float y1;
        float elapsed;

        SunFlight(Sun sun, float x0, float y0, float x1, float y1) {
            this.sun = sun;
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }
    }


    /** Cosmetic in-flight plant-food collect animation (mirror of {@link SunFlight}). */
    final class PlantFoodFlight {
        final PlantFoodPickup food;
        final float x0;
        final float y0;
        final float x1;
        final float y1;
        float elapsed;

        PlantFoodFlight(PlantFoodPickup food, float x0, float y0, float x1, float y1) {
            this.food = food;
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }
    }


    final class LootFlight {
        final LootPickup loot;
        final float x0;
        final float y0;
        final float x1;
        final float y1;
        final Runnable onComplete;
        float elapsed;
        boolean done;

        LootFlight(LootPickup loot, float x0, float y0, float x1, float y1, Runnable onComplete) {
            this.loot = loot;
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.onComplete = onComplete;
        }
    }


    final class LiveSnap {
        final AnimPose pose;
        final float x;
        final float y;
        final boolean backward;
        final float time;

        LiveSnap(AnimPose pose, float x, float y, boolean backward, float time) {
            this.pose = pose;
            this.x = x;
            this.y = y;
            this.backward = backward;
            this.time = time;
        }
    }


    final class DeathFx {
        final AnimPose pose;
        final float x;
        final float y;
        final boolean drown;
        float time;
        float drownWaterY = Float.NaN;
        float snorkelRise;
        float hitFlash;
        float holdSeconds = Float.NaN;

        DeathFx(AnimPose pose, float x, float y) {
            this(pose, x, y, false);
        }

        DeathFx(AnimPose pose, float x, float y, boolean drown) {
            this.pose = pose;
            this.x = x;
            this.y = y;
            this.drown = drown;
        }

        float hold(ClipRef ref) {
            if (!Float.isNaN(holdSeconds)) {
                return Math.max(0f, holdSeconds);
            }
            return ref != null ? ref.duration : 0f;
        }
    }


    final class BlastFx {
        final String pamPath;
        final String clip;
        final float x;
        final float y;
        float time;

        BlastFx(String pamPath, float x, float y) {
            this(pamPath, null, x, y);
        }

        BlastFx(String pamPath, String clip, float x, float y) {
            this.pamPath = pamPath;
            this.clip = clip;
            this.x = x;
            this.y = y;
        }
    }


    final class OctopusCoatFx {
        float time;
        float x;
        float y;
        boolean dying;
    }


    enum SheepPhase {
        VANISH, APPEAR, IDLE, LEAVE, EMERGE
    }


    final class SheepFx {
        SheepPhase phase = SheepPhase.VANISH;
        float time;
        String idleClip = WizardAnim.IDLE2_CLIP;
    }


    final class OneShotFx {
        final String pamPath;
        final String clipName;
        final float x;
        final float y;
        final float scale;
        final boolean loop;
        float time;
        float duration;
        boolean started;

        OneShotFx(String pamPath, String clipName, float x, float y, float scale, boolean loop) {
            this.pamPath = pamPath;
            this.clipName = clipName;
            this.x = x;
            this.y = y;
            this.scale = scale > 0f ? scale : 1f;
            this.loop = loop;
        }
    }


    enum PlantFoodFxPhase {
        ON, LOOP, OFF
    }


    final class PlantFoodFx {
        PlantFoodFxPhase phase = PlantFoodFxPhase.ON;
        float time;
    }


    /** Per-sandstorm visual state: placement, clip choice and clocks. */
    final class SandstormFx {
        float startX;
        float targetX;
        float y;
        float x;
        float scale = -1f;
        float introDuration;
        float outroDuration;
        float clock;
        float outroClock;
        boolean landedSeen;
        boolean visible;
        boolean loop;
        float clipTime;
        String clip;
    }


    /** Per-ice-wind visual state: sweep endpoints and playback clock. */
    final class IceWindFx {
        float startX;
        float endX;
        float y;
        float x;
        float scale = -1f;
        float clock;
    }


    /** Per-slide-tile visual state: idle loop or active burst playback. */
    final class SlideTileFx {
        enum Phase { IDLE, ACTIVE_START, ACTIVE_END }

        Phase phase = Phase.IDLE;
        float clock;
        float scale = -1f;
    }
