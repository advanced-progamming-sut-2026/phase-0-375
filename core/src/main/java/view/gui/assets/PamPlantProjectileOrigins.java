package view.gui.assets;

import com.badlogic.gdx.math.Rectangle;
import model.enums.PlantState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.ability.PlantProjectileOrigins;
import model.plant.ability.TimedPlantAction;
import model.plant.instance.PlantInstance;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.AnimPose;
import view.gui.anim.AnimScale;
import view.gui.anim.plant.PlantAnimAdapter;
import view.gui.lawn.LawnLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves a projectile spawn point from the plant's attack-clip PAM part bounds,
 * converted into grid units.
 */
public final class PamPlantProjectileOrigins implements PlantProjectileOrigins {
    private final PamPlayer player;
    private final PlantAnimAdapter adapter;
    private final LawnLayout layout;
    private final float sampleFraction;
    private final Map<String, String> partByClip = new HashMap<>();
    private final Map<String, float[]> offsetByClip = new HashMap<>();

    /**
     * Extra grid nudge {@code [dCol, dRow]} after PAM resolve. Positive row is
     * downward on the lawn (toward the bottom of the screen).
     */
    private static final Map<String, float[]> GRID_NUDGE = Map.of(
            "Puff-shroom", new float[]{0.05f, -0.08f},
            "Peashooter", new float[]{0.08f, 0f},
            "Repeater", new float[]{0.08f, 0f},
            "Snow Pea", new float[]{0.08f, 0f},
            "Fire Peashooter", new float[]{0.08f, 0f},
            "Goo Peashooter", new float[]{0.08f, 0f},
            "Sea-shroom", new float[]{0.2f, 0.1f},
            "Cabbage-pult", new float[]{-0.8f, -0.2f}
    );

    /** Omnidirectional shooters should leave from the plant body, not a side muzzle. */
    private static final java.util.Set<String> BODY_ORIGIN_PLANTS = java.util.Set.of(
            "Rotobaga", "Starfruit"
    );

    public PamPlantProjectileOrigins(PvzAssets assets, LawnLayout layout) {
        this(assets == null ? null : assets.player,
                assets == null ? null : new PlantAnimAdapter(assets.pamCatalog),
                layout,
                TimedPlantAction.DEFAULT_ATTACK_FIRE_FRACTION);
    }

    public PamPlantProjectileOrigins(PamPlayer player, PlantAnimAdapter adapter, LawnLayout layout) {
        this(player, adapter, layout, TimedPlantAction.DEFAULT_ATTACK_FIRE_FRACTION);
    }

    public PamPlantProjectileOrigins(PamPlayer player, PlantAnimAdapter adapter, LawnLayout layout,
                                     float sampleFraction) {
        this.player = player;
        this.adapter = adapter;
        this.layout = layout != null ? layout : LawnLayout.frontLawnDefault();
        this.sampleFraction = clamp01(sampleFraction);
    }

    @Override
    public FloatPoint origin(PlantInstance plant) {
        if (plant == null || plant.getPosition() == null || player == null || adapter == null) {
            return null;
        }
        AnimPose pose = adapter.poseFor(plant, PlantState.ATTACKING);
        if (pose == null || pose.pamPath() == null || pose.clipName() == null) {
            return null;
        }
        float[] offset = offsetFor(plant, pose);
        if (offset == null) {
            return null;
        }
        Point cell = plant.getPosition();
        float[] plantWorld = layout.centerOf(cell.getY(), cell.getX());
        float scale = AnimScale.PLANT * pose.scale();
        float signX = pose.flipX() ? -1f : 1f;
        float worldX = plantWorld[0] + offset[0] * scale * signX;
        float worldY = plantWorld[1] - offset[1] * scale;
        float[] grid = layout.gridOf(worldX, worldY);
        float[] nudge = GRID_NUDGE.get(plant.getDefinition() != null ? plant.getDefinition().getName() : null);
        if (nudge != null) {
            grid[0] += nudge[0];
            grid[1] += nudge[1];
        }
        return new FloatPoint(grid[0], grid[1]);
    }

    /**
     * @return canvas-space muzzle point {@code [x, y]} (Y-down, origin at canvas
     *         center), or {@code null} if the clip is not ready
     */
    private float[] offsetFor(PlantInstance plant, AnimPose pose) {
        String key = pose.pamPath() + "#" + pose.clipName() + "@" + sampleFraction;
        float[] cached = offsetByClip.get(key);
        if (cached != null) {
            return cached;
        }
        ClipRef clip = player.getClip(pose.pamPath(), pose.clipName());
        if (clip == null) {
            return null;
        }
        String plantName = plant.getDefinition() != null ? plant.getDefinition().getName() : null;
        boolean bodyOrigin = plantName != null && BODY_ORIGIN_PLANTS.contains(plantName);
        String part = bodyOrigin ? null
                : partByClip.computeIfAbsent(pose.pamPath() + "#" + pose.clipName(),
                ignored -> pickMuzzlePart(pose.pamPath(), clip));
        float[] offset;
        if (part != null) {
            Rectangle bounds = sampleBounds(clip, part);
            if (bounds == null) {
                return null;
            }
            offset = anchorPoint(part, bounds);
        } else {
            offset = bodyOffset(pose, clip);
            if (offset == null) {
                return null;
            }
        }
        offsetByClip.put(key, offset);
        return offset;
    }

    /** Visual center of the plant body (idle clip when present, else attack AABB). */
    private float[] bodyOffset(AnimPose pose, ClipRef attackClip) {
        Rectangle bounds = null;
        String idleClip = resolveClipName(pose.pamPath(), "idle");
        if (idleClip != null) {
            bounds = player.bounds(pose.pamPath(), idleClip);
        }
        if (bounds == null) {
            bounds = player.bounds(pose.pamPath(), pose.clipName());
        }
        if (bounds == null && attackClip != null) {
            return new float[]{0f, 0f};
        }
        if (bounds == null) {
            return null;
        }
        return new float[]{
                bounds.x + bounds.width * 0.5f,
                bounds.y + bounds.height * 0.5f
        };
    }

    private Rectangle sampleBounds(ClipRef clip, String part) {
        Rectangle[] frames = player.partBoundsByFrame(clip, part);
        if (frames == null || frames.length == 0) {
            return null;
        }
        int target = Math.min(frames.length - 1, Math.max(0, Math.round(sampleFraction * (frames.length - 1))));
        Rectangle atFraction = nearestNonNull(frames, target);
        if (atFraction == null) {
            return null;
        }
        // Prefer the most forward (rightmost) pose near the fire fraction - usually the spit.
        Rectangle best = atFraction;
        float bestRight = atFraction.x + atFraction.width;
        int window = Math.max(1, frames.length / 5);
        int from = Math.max(0, target - window);
        int to = Math.min(frames.length - 1, target + window);
        for (int i = from; i <= to; i++) {
            Rectangle r = frames[i];
            if (r == null) {
                continue;
            }
            float right = r.x + r.width;
            if (right > bestRight) {
                bestRight = right;
                best = r;
            }
        }
        return best;
    }

    private static Rectangle nearestNonNull(Rectangle[] frames, int target) {
        if (frames[target] != null) {
            return frames[target];
        }
        for (int d = 1; d < frames.length; d++) {
            int low = target - d;
            int high = target + d;
            if (low >= 0 && frames[low] != null) {
                return frames[low];
            }
            if (high < frames.length && frames[high] != null) {
                return frames[high];
            }
        }
        return null;
    }

    /**
     * Spit / muzzle: front (right) of the part. Mouth: upper-front of the opening.
     * Everything else: center.
     */
    private static float[] anchorPoint(String part, Rectangle bounds) {
        String n = part.toLowerCase(Locale.ROOT);
        boolean spit = n.contains("spit") || n.contains("muzzle") || n.contains("nozzle")
                || n.contains("stem_pea") || n.contains("projectile");
        boolean mouth = n.contains("mouth") || n.contains("lips");
        float x;
        float y;
        if (spit) {
            x = bounds.x + bounds.width * 0.92f;
            y = bounds.y + bounds.height * 0.45f;
        } else if (mouth) {
            x = bounds.x + bounds.width * 0.88f;
            y = bounds.y + bounds.height * 0.22f;
        } else {
            x = bounds.x + bounds.width * 0.5f;
            y = bounds.y + bounds.height * 0.5f;
        }
        return new float[]{x, y};
    }

    private String pickMuzzlePart(String pamPath, ClipRef clip) {
        List<String> names = new ArrayList<>();
        collectPartNames(player.getParts(pamPath), names);
        String best = null;
        int bestScore = 0;
        int bestLastFrame = -1;
        for (String name : names) {
            int score = muzzleScore(name);
            if (score <= 0) {
                continue;
            }
            int last = lastVisibleFrame(clip, name);
            if (last < 0) {
                continue;
            }
            if (score > bestScore || (score == bestScore && last > bestLastFrame)) {
                bestScore = score;
                bestLastFrame = last;
                best = name;
            }
        }
        return best;
    }

    private static void collectPartNames(PamPlayer.AnimationPart part, List<String> out) {
        if (part == null) {
            return;
        }
        if (part.name != null && !part.name.isBlank()) {
            out.add(part.name);
        }
        if (part.children != null) {
            for (PamPlayer.AnimationPart child : part.children) {
                collectPartNames(child, out);
            }
        }
    }

    private int lastVisibleFrame(ClipRef clip, String part) {
        Rectangle[] frames = player.partBoundsByFrame(clip, part);
        if (frames == null) {
            return -1;
        }
        for (int i = frames.length - 1; i >= 0; i--) {
            if (frames[i] != null) {
                return i;
            }
        }
        return -1;
    }

    private static float clamp01(float value) {
        if (value <= 0f) {
            return 0f;
        }
        return Math.min(value, 1f);
    }

    /**
     * Exact clip match, then prefix match (e.g. {@code idle} → {@code idle1_1}).
     * Avoids {@link PamPlayer#getClip} throwing on plants with staged idle clips.
     */
    private String resolveClipName(String pamPath, String want) {
        if (pamPath == null || want == null || want.isBlank()) {
            return null;
        }
        List<String> available = player.clips(pamPath);
        if (available == null || available.isEmpty()) {
            return null;
        }
        String needle = want.toLowerCase(Locale.ROOT);
        String prefixHit = null;
        for (String name : available) {
            if (name == null) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals(needle)) {
                return name;
            }
            if (prefixHit == null && lower.startsWith(needle)) {
                prefixHit = name;
            }
        }
        return prefixHit;
    }

    static int muzzleScore(String name) {
        if (name == null || name.isBlank()) {
            return 0;
        }
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("|") || n.startsWith("image_")) {
            return 0;
        }
        if (n.contains("plantfood") || n.contains("unloaded") || n.contains("custom")) {
            return 0;
        }
        // Spit / muzzle flash is the release point; stem_pea sits behind the lips.
        if (n.contains("_spit") || n.endsWith("spit")) {
            return 100;
        }
        if (n.contains("muzzle") && !n.contains("helmet") && !n.contains("blast")) {
            return 95;
        }
        if (n.contains("projectile")) {
            return 90;
        }
        if (n.matches("bulb[1-3]_body")) {
            return 88;
        }
        if (n.contains("kernelpult_kernel") || n.endsWith("_kernel")) {
            return 86;
        }
        if (n.contains("stem_pea")) {
            return 70;
        }
        if (n.contains("nozzle") && !n.contains("hole") && !n.contains("closed") && !n.contains("neck")) {
            return 65;
        }
        if (n.contains("spike") && n.contains("attack") && n.contains("base")) {
            return 60;
        }
        if (n.contains("citrus") && n.contains("orb")) {
            return 55;
        }
        if (n.contains("frontspike1")) {
            return 50;
        }
        if (n.contains("lips") && !n.contains("closed") && !n.contains("dimple")) {
            return 30;
        }
        if (n.contains("mouth") && !n.contains("closed") && !n.contains("open") && !n.contains("charge")) {
            return 25;
        }
        return 0;
    }
}
