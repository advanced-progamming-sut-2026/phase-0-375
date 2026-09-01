package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import controller.GameMenuController.LevelSummary;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import view.gui.anim.PamClipCache;
import view.gui.assets.EgyptSeasonMapLayout;
import view.gui.assets.SeasonMapLayout;
import view.gui.assets.WorldMapArt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Scrollable season path: platforms + neon connectors + level-node markers.
 * Node/platform positions and sizes come from a per-season {@link SeasonMapLayout}
 * so Egypt and Frostbite can be tuned independently.
 * <ul>
 *   <li>Next (green) — optional one-shot unlock, then idle {@code unlocked}</li>
 *   <li>Completed (blue) — stacked glow + dome pieces</li>
 *   <li>Locked — {@code locked_idle} PAM (static white fallback)</li>
 * </ul>
 */
public final class SeasonWorldMap extends Group {
    /** Hit / number label size around the marker. */
    public static float ORB_SIZE = 120f;
    /** Drawn size for the green PAM. */
    public static float GREEN_PAM_SIZE = 160f;
    public static float GREEN_PAM_SCALE = 0.55f;
    /**
     * Uniform scale for the whole completed stack (sizes + base/top offsets).
     * Keep your DX/DY as tuned at scale 1; this shrinks/grows the trio together.
     */
    public static float BLUE_STACK_SCALE = 0.55f;
    /** Completed orb stack sizes at scale 1 (native aspect). */
    public static float BLUE_BASE_W = 118f;
    public static float BLUE_BASE_H = 49f;
    public static float BLUE_DOME_W = 97f;
    public static float BLUE_DOME_H = 71f;
    public static float BLUE_TOP_W = 118f;
    public static float BLUE_TOP_H = 40f;
    /**
     * Offsets for bottom ({@code 118X49_2}) and top ({@code 118X40_2}) relative to the
     * middle dome center, at scale 1. +X = right, +Y = up.
     */
    public static float BLUE_BASE_DX = 0f;
    public static float BLUE_BASE_DY = 0f;
    public static float BLUE_TOP_DX = 0f;
    public static float BLUE_TOP_DY = -20f;
    public static float WHITE_DOME_W = 100f;
    public static float WHITE_DOME_H = 74f;
    public static float WHITE_HIGHLIGHT_W = 80f;
    public static float WHITE_HIGHLIGHT_H = 58f;
    public static float PATH_THICKNESS = 18f;
    /**
     * Node tap pulse. Tune {@link #CLICK_FX_SCALE} yourself (1 = base size).
     * Drawn as a soft circle, not a square.
     */
    public static float CLICK_FX_SIZE = 56f;
    public static float CLICK_FX_SCALE = 0.55f;
    public static float CLICK_FX_GROW = 1.45f;
    public static float CLICK_FX_DURATION = 0.20f;

    /**
     * Designed for 5 season slots even when only 4 levels exist in data yet.
     * Missing level-5 entries are padded as placeholders until added to {@code levels.json}.
     */
    public static final int SLOT_COUNT = 5;
    /** Level id used for padded stubs when a chapter has no real level 5 yet. */
    public static final int PLACEHOLDER_LEVEL_ID = 5;

    private static Texture pixel;
    private static Texture circlePixel;

    private final List<Vector2> nodeCenters = new ArrayList<>();
    private final List<Integer> nodeLevelIds = new ArrayList<>();

    public SeasonWorldMap(
            TextureBank textures,
            WorldMapArt art,
            SeasonMapLayout layout,
            Skin skin,
            PamPlayer pamPlayer,
            PamClipCache pamClips,
            List<LevelSummary> levels,
            int unlockIntroLevelId,
            IntConsumer onSelectLevel) {
        this(textures, art, layout, skin, pamPlayer, pamClips, levels, unlockIntroLevelId,
                Collections.emptySet(), onSelectLevel);
    }

    public SeasonWorldMap(
            TextureBank textures,
            WorldMapArt art,
            SeasonMapLayout layout,
            Skin skin,
            PamPlayer pamPlayer,
            PamClipCache pamClips,
            List<LevelSummary> levels,
            int unlockIntroLevelId,
            Set<Integer> placeholderLevelIds,
            IntConsumer onSelectLevel) {
        setTransform(false);
        setTouchable(Touchable.enabled);
        SeasonMapLayout mapLayout = layout != null ? layout : new EgyptSeasonMapLayout();
        setSize(mapLayout.mapWidth, mapLayout.mapHeight);
        if (levels == null || levels.isEmpty() || mapLayout.nodeXy == null) {
            return;
        }
        Set<Integer> placeholders = placeholderLevelIds != null
                ? placeholderLevelIds
                : Collections.emptySet();
        Group pathLayer = new Group();
        Group islandLayer = new Group();
        Group markerLayer = new Group();
        pathLayer.setTouchable(Touchable.disabled);
        islandLayer.setTouchable(Touchable.disabled);
        addActor(islandLayer);
        addActor(pathLayer);
        addActor(markerLayer);
        int nextPlayableId = nextPlayableId(levels, placeholders);
        int count = Math.min(levels.size(), mapLayout.nodeXy.length);
        for (int i = 0; i < count; i++) {
            addNode(i, textures, art, mapLayout, skin, pamPlayer, pamClips, levels.get(i),
                    placeholders, nextPlayableId, unlockIntroLevelId, islandLayer, markerLayer,
                    onSelectLevel);
        }
        addPathSegments(pathLayer, textures, mapLayout.orbLift);
    }

    private static int nextPlayableId(List<LevelSummary> levels, Set<Integer> placeholders) {
        for (LevelSummary s : levels) {
            if (placeholders.contains(s.levelId())) {
                continue;
            }
            if (s.unlocked() && !s.completed()) {
                return s.levelId();
            }
        }
        return -1;
    }

    private void addNode(
            int i,
            TextureBank textures,
            WorldMapArt art,
            SeasonMapLayout mapLayout,
            Skin skin,
            PamPlayer pamPlayer,
            PamClipCache pamClips,
            LevelSummary summary,
            Set<Integer> placeholders,
            int nextPlayableId,
            int unlockIntroLevelId,
            Group islandLayer,
            Group markerLayer,
            IntConsumer onSelectLevel) {
        float cx = mapLayout.nodeXy[i][0];
        float cy = mapLayout.nodeXy[i][1];
        nodeCenters.add(new Vector2(cx, cy));
        nodeLevelIds.add(summary.levelId());
        float pw = mapLayout.platformW(i);
        float ph = mapLayout.platformH(i);
        float platformX = cx + mapLayout.platformOffsetX(i) - pw * 0.5f;
        float platformY = cy + mapLayout.platformOffsetY(i) - ph * 0.55f;
        addPlatform(islandLayer, textures, art, pamPlayer, pamClips, summary, pw, ph,
                platformX, platformY);
        float orbX = cx - ORB_SIZE * 0.5f;
        float orbY = cy + mapLayout.orbLift;
        float orbCx = cx;
        float orbCy = orbY + ORB_SIZE * 0.45f;
        addOrbMarker(markerLayer, textures, pamPlayer, pamClips, summary, placeholders,
                nextPlayableId, unlockIntroLevelId, orbCx, orbCy);
        addLevelNumber(markerLayer, skin, summary.levelId(), orbX, orbY);
        addNodeHit(markerLayer, onSelectLevel, summary.levelId(), platformX, platformY, pw, ph,
                orbX, orbY, orbCx, orbCy);
    }

    private static void addPlatform(
            Group islandLayer,
            TextureBank textures,
            WorldMapArt art,
            PamPlayer pamPlayer,
            PamClipCache pamClips,
            LevelSummary summary,
            float pw,
            float ph,
            float platformX,
            float platformY) {
        WorldMapArt.PlatformArt platformArt = art.platformArt(summary.levelId());
        Actor platform = null;
        if (platformArt.hasPam() && pamPlayer != null && pamClips != null) {
            platform = animatedPlatform(pamPlayer, pamClips, platformArt, pw, ph);
        }
        if (platform == null) {
            Image image = regionImage(textures, platformArt.staticId(), pw, ph);
            if (image == null) {
                image = solid(pw, ph, new Color(0.72f, 0.58f, 0.38f, 1f));
            }
            platform = image;
        }
        platform.setPosition(platformX, platformY);
        platform.setTouchable(Touchable.disabled);
        islandLayer.addActor(platform);
    }

    private static void addOrbMarker(
            Group markerLayer,
            TextureBank textures,
            PamPlayer pamPlayer,
            PamClipCache pamClips,
            LevelSummary summary,
            Set<Integer> placeholders,
            int nextPlayableId,
            int unlockIntroLevelId,
            float orbCx,
            float orbCy) {
        boolean unimplemented = placeholders.contains(summary.levelId());
        if (unimplemented || !summary.unlocked()) {
            Group orbSlot = new Group();
            orbSlot.setTouchable(Touchable.disabled);
            markerLayer.addActor(orbSlot);
            addLockedMarker(orbSlot, textures, pamPlayer, pamClips, orbCx, orbCy);
        } else if (summary.completed()) {
            addBlueMarker(markerLayer, textures, orbCx, orbCy);
        } else if (pamPlayer != null && pamClips != null) {
            boolean playUnlock = summary.levelId() == nextPlayableId
                    && summary.levelId() == unlockIntroLevelId;
            Group orbSlot = new Group();
            orbSlot.setTouchable(Touchable.disabled);
            markerLayer.addActor(orbSlot);
            addGreenMarker(orbSlot, pamPlayer, pamClips, orbCx, orbCy, playUnlock);
        }
    }

    private static void addLevelNumber(Group markerLayer, Skin skin, int levelId,
                                       float orbX, float orbY) {
        Label number = new Label(String.valueOf(levelId), skin, "big");
        number.setAlignment(Align.center);
        number.setColor(Color.WHITE);
        number.setSize(ORB_SIZE, ORB_SIZE);
        number.setPosition(orbX, orbY);
        number.setTouchable(Touchable.disabled);
        markerLayer.addActor(number);
    }

    private static void addNodeHit(
            Group markerLayer,
            IntConsumer onSelectLevel,
            int levelId,
            float platformX,
            float platformY,
            float pw,
            float ph,
            float orbX,
            float orbY,
            float orbCx,
            float orbCy) {
        float pad = 24f;
        float hitLeft = Math.min(platformX, orbX) - pad;
        float hitRight = Math.max(platformX + pw, orbX + ORB_SIZE) + pad;
        float hitBottom = Math.min(platformY, orbY) - pad;
        float hitTop = Math.max(platformY + ph, orbY + ORB_SIZE) + pad;
        Group hit = new Group();
        hit.setSize(hitRight - hitLeft, hitTop - hitBottom);
        hit.setPosition(hitLeft, hitBottom);
        hit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                playNodeClickFx(markerLayer, orbCx, orbCy);
                if (onSelectLevel != null) {
                    onSelectLevel.accept(levelId);
                }
            }
        });
        markerLayer.addActor(hit);
    }

    private void addPathSegments(Group pathLayer, TextureBank textures, float orbLift) {
        for (int i = 0; i < nodeCenters.size() - 1; i++) {
            Vector2 a = nodeCenters.get(i);
            Vector2 b = nodeCenters.get(i + 1);
            float ax = a.x;
            float ay = a.y + orbLift + ORB_SIZE * 0.5f;
            float bx = b.x;
            float by = b.y + orbLift + ORB_SIZE * 0.5f;
            Image segment = pathSegment(textures, ax, ay, bx, by);
            if (segment != null) {
                pathLayer.addActor(segment);
            }
        }
    }

    /** Looping PAM platform sized into {@code w}×{@code h}; falls back to null if unloadable. */
    private static PamEffectActor animatedPlatform(
            PamPlayer player, PamClipCache clips, WorldMapArt.PlatformArt art, float w, float h) {
        PamEffectActor pam = new PamEffectActor(player, clips, art.pamPath(), art.pamClip());
        pam.setSize(w, h);
        float sx = art.nativeW() > 1f ? w / art.nativeW() : 1f;
        float sy = art.nativeH() > 1f ? h / art.nativeH() : 1f;
        pam.setEffectScale(Math.min(sx, sy));
        pam.setLooping(true);
        pam.setTouchable(Touchable.disabled);
        return pam;
    }

    /** Brief circular scale+fade pulse at the marker when a node is tapped. */
    private static void playNodeClickFx(Group layer, float cx, float cy) {
        float size = CLICK_FX_SIZE * CLICK_FX_SCALE;
        Image ring = new Image(new TextureRegionDrawable(softCircle()));
        ring.setSize(size, size);
        ring.setColor(0.85f, 1f, 1f, 0.70f);
        ring.setOrigin(Align.center);
        ring.setPosition(cx - size * 0.5f, cy - size * 0.5f);
        ring.setTouchable(Touchable.disabled);
        layer.addActor(ring);
        ring.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.scaleTo(CLICK_FX_GROW, CLICK_FX_GROW, CLICK_FX_DURATION),
                        Actions.fadeOut(CLICK_FX_DURATION)),
                Actions.removeActor()));
    }

    private static void addGreenMarker(Group layer, PamPlayer player, PamClipCache clips,
                                       float cx, float cy, boolean playUnlockIntro) {
        if (player == null || clips == null) {
            return;
        }
        if (playUnlockIntro) {
            PamEffectActor intro = nodePam(player, clips, WorldMapArt.ORB_UNLOCK_CLIP, cx, cy);
            intro.setLooping(false);
            intro.onComplete(() -> {
                intro.remove();
                layer.addActor(idleGreen(player, clips, cx, cy));
            });
            layer.addActor(intro);
            return;
        }
        layer.addActor(idleGreen(player, clips, cx, cy));
    }

    private static PamEffectActor idleGreen(PamPlayer player, PamClipCache clips,
                                            float cx, float cy) {
        PamEffectActor pam = nodePam(player, clips, WorldMapArt.ORB_GREEN_CLIP, cx, cy);
        pam.setLooping(true);
        return pam;
    }

    private static void addLockedMarker(Group layer, TextureBank textures,
                                        PamPlayer player, PamClipCache clips,
                                        float cx, float cy) {
        if (player != null && clips != null) {
            PamEffectActor pam = nodePam(player, clips, WorldMapArt.ORB_LOCKED_CLIP, cx, cy);
            pam.setLooping(true);
            layer.addActor(pam);
            return;
        }
        addWhiteMarker(layer, textures, cx, cy);
    }

    private static PamEffectActor nodePam(PamPlayer player, PamClipCache clips,
                                          String clip, float cx, float cy) {
        PamEffectActor pam = new PamEffectActor(player, clips, WorldMapArt.ORB_NODE_PAM, clip);
        pam.setSize(GREEN_PAM_SIZE, GREEN_PAM_SIZE);
        pam.setPosition(cx - GREEN_PAM_SIZE * 0.5f, cy - GREEN_PAM_SIZE * 0.5f);
        pam.setEffectScale(GREEN_PAM_SCALE);
        pam.setTouchable(Touchable.disabled);
        return pam;
    }

    private static void addBlueMarker(Group layer, TextureBank textures, float cx, float cy) {
        float s = BLUE_STACK_SCALE;
        float baseW = BLUE_BASE_W * s;
        float baseH = BLUE_BASE_H * s;
        float domeW = BLUE_DOME_W * s;
        float domeH = BLUE_DOME_H * s;
        float topW = BLUE_TOP_W * s;
        float topH = BLUE_TOP_H * s;
        float baseDx = BLUE_BASE_DX * s;
        float baseDy = BLUE_BASE_DY * s;
        float topDx = BLUE_TOP_DX * s;
        float topDy = BLUE_TOP_DY * s;

        // Middle dome is the fixed anchor; base/top offsets scale with the stack.
        float domeX = cx - domeW * 0.5f;
        float domeY = cy - domeH * 0.35f;
        float domeCx = domeX + domeW * 0.5f;
        float domeCy = domeY + domeH * 0.5f;

        Image base = regionImage(textures, WorldMapArt.ORB_BLUE_BASE, baseW, baseH);
        if (base != null) {
            base.setPosition(domeCx - baseW * 0.5f + baseDx, domeCy - baseH * 0.5f + baseDy);
            layer.addActor(base);
        }
        Image dome = regionImage(textures, WorldMapArt.ORB_BLUE_DOME, domeW, domeH);
        if (dome != null) {
            dome.setPosition(domeX, domeY);
            layer.addActor(dome);
        }
        Image top = regionImage(textures, WorldMapArt.ORB_BLUE_TOP, topW, topH);
        if (top != null) {
            top.setPosition(domeCx - topW * 0.5f + topDx, domeCy - topH * 0.5f + topDy);
            layer.addActor(top);
        }
        if (base == null && dome == null && top == null) {
            layer.addActor(placedSolid(cx, cy, ORB_SIZE, new Color(0.25f, 0.65f, 1f, 1f)));
        }
    }

    private static void addWhiteMarker(Group layer, TextureBank textures, float cx, float cy) {
        Image dome = regionImage(textures, WorldMapArt.ORB_WHITE_DOME, WHITE_DOME_W, WHITE_DOME_H);
        if (dome != null) {
            dome.setPosition(cx - WHITE_DOME_W * 0.5f, cy - WHITE_DOME_H * 0.35f);
            layer.addActor(dome);
        }
        Image hi = regionImage(textures, WorldMapArt.ORB_WHITE_HIGHLIGHT,
                WHITE_HIGHLIGHT_W, WHITE_HIGHLIGHT_H);
        if (hi != null) {
            hi.setPosition(cx - WHITE_HIGHLIGHT_W * 0.5f, cy - WHITE_HIGHLIGHT_H * 0.15f);
            layer.addActor(hi);
        }
        if (dome == null && hi == null) {
            layer.addActor(placedSolid(cx, cy, ORB_SIZE, new Color(0.85f, 0.85f, 0.88f, 1f)));
        }
    }

    private static Image placedSolid(float cx, float cy, float size, Color color) {
        Image image = solid(size, size, color);
        image.setPosition(cx - size * 0.5f, cy - size * 0.5f);
        return image;
    }

    public float nodeCenterX(int levelId) {
        for (int i = 0; i < nodeLevelIds.size(); i++) {
            if (nodeLevelIds.get(i) == levelId) {
                return nodeCenters.get(i).x;
            }
        }
        return -1f;
    }

    private static Image pathSegment(TextureBank textures, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) {
            return null;
        }
        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        Image segment = regionImage(textures, WorldMapArt.PATH_SEGMENT, len, PATH_THICKNESS);
        if (segment == null) {
            segment = solid(len, PATH_THICKNESS, new Color(0.2f, 0.95f, 1f, 0.95f));
        }
        segment.setOrigin(0f, PATH_THICKNESS * 0.5f);
        segment.setPosition(x1, y1 - PATH_THICKNESS * 0.5f);
        segment.setRotation(angle);
        return segment;
    }

    private static Image regionImage(TextureBank textures, String id, float w, float h) {
        if (textures == null || id == null) {
            return null;
        }
        TextureRegion region = textures.region(id);
        if (region == null) {
            return null;
        }
        Image image = new Image(new TextureRegionDrawable(region));
        image.setSize(w, h);
        return image;
    }

    private static Image solid(float w, float h, Color color) {
        Image image = new Image(new TextureRegionDrawable(whitePixel()));
        image.setSize(w, h);
        image.setColor(color);
        return image;
    }

    private static TextureRegion whitePixel() {
        if (pixel == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
            pixmap.dispose();
        }
        return new TextureRegion(pixel);
    }

    /** Soft white disc used for the tap pulse (circular, not square). */
    private static TextureRegion softCircle() {
        if (circlePixel == null) {
            int size = 64;
            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pixmap.setBlending(Pixmap.Blending.None);
            float cx = (size - 1) * 0.5f;
            float cy = (size - 1) * 0.5f;
            float r = size * 0.48f;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float d = (float) Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                    float a = 0f;
                    if (d <= r * 0.72f) {
                        a = 1f;
                    } else if (d < r) {
                        float t = (d - r * 0.72f) / (r * 0.28f);
                        a = 1f - t;
                    }
                    pixmap.setColor(1f, 1f, 1f, a);
                    pixmap.drawPixel(x, y);
                }
            }
            circlePixel = new Texture(pixmap);
            circlePixel.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            pixmap.dispose();
        }
        return new TextureRegion(circlePixel);
    }
}
