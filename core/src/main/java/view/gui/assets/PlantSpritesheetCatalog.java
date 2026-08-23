package view.gui.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fallback plant / effect art when no PAM exists: PNG spritesheets under
 * {@code IMAGES/{res}/...} with optional {@code sprites.json}.
 */
public final class PlantSpritesheetCatalog {
    private static final float DEFAULT_FPS = 12f;
    private static final String META_FILE = "sprites.json";

    /**
     * One playable clip cut from a spritesheet PNG.
     *
     * @param relativePath path from the assets root to the PNG
     * @param columns grid columns
     * @param rows grid rows
     * @param frameDuration seconds per frame
     * @param frameIndices frame indices in row-major order; {@code null} = all cells
     */
    public record ClipSpec(
            String relativePath,
            int columns,
            int rows,
            float frameDuration,
            int[] frameIndices
    ) {
        public int frameCount() {
            if (frameIndices != null && frameIndices.length > 0) {
                return frameIndices.length;
            }
            return Math.max(0, columns * rows);
        }

        public float durationSeconds() {
            return frameCount() * Math.max(0.001f, frameDuration);
        }

        public String cacheKey() {
            StringBuilder sb = new StringBuilder(relativePath.length() + 32);
            sb.append(relativePath).append('#').append(columns).append('x').append(rows)
                    .append('@').append(frameDuration);
            if (frameIndices != null) {
                sb.append(':');
                for (int i = 0; i < frameIndices.length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(frameIndices[i]);
                }
            }
            return sb.toString();
        }
    }

    private final FileHandle root;
    private final String resolution;
    private final Map<String, String> plantFolderAliases;
    private final Map<String, FolderMeta> folderCache = new HashMap<>();
    private final Map<String, ClipSpec> byCacheKey = new HashMap<>();

    public PlantSpritesheetCatalog(FileHandle assetsRoot, String resolution) {
        this.root = assetsRoot;
        this.resolution = resolution == null || resolution.isBlank() ? "768" : resolution.trim();
        this.plantFolderAliases = PlantPamAliases.all();
    }

    /** Spec previously returned by {@link #resolveClip} / idle fallbacks, or {@code null}. */
    public ClipSpec byCacheKey(String cacheKey) {
        return cacheKey == null ? null : byCacheKey.get(cacheKey);
    }

    private ClipSpec remember(ClipSpec spec) {
        if (spec != null) {
            byCacheKey.put(spec.cacheKey(), spec);
        }
        return spec;
    }

    /**
     * First preferred clip that has a spritesheet, or {@code null}.
     */
    public ClipSpec resolveClip(String definitionName, String... preferred) {
        FolderMeta folder = folderFor(definitionName);
        if (folder == null || preferred == null) {
            return null;
        }
        for (String want : preferred) {
            if (want == null || want.isBlank()) {
                continue;
            }
            ClipSpec spec = folder.clips.get(want.toLowerCase(Locale.ROOT));
            if (spec != null) {
                return remember(spec);
            }
        }
        return null;
    }

    /**
     * Any available clip for this plant (prefer {@code idle}, then {@code attack}, then first).
     */
    public ClipSpec anyClip(String definitionName) {
        FolderMeta folder = folderFor(definitionName);
        if (folder == null || folder.clips.isEmpty()) {
            return null;
        }
        ClipSpec idle = folder.clips.get("idle");
        if (idle != null) {
            return remember(idle);
        }
        ClipSpec attack = folder.clips.get("attack");
        if (attack != null) {
            return remember(attack);
        }
        return remember(folder.clips.values().iterator().next());
    }

    /** Idle-friendly fallback: dedicated idle, else first frame of any sheet. */
    public ClipSpec idleFallback(String definitionName) {
        FolderMeta folder = folderFor(definitionName);
        if (folder == null) {
            return null;
        }
        ClipSpec idle = folder.clips.get("idle");
        if (idle != null) {
            return remember(idle);
        }
        ClipSpec any = anyClip(definitionName);
        if (any == null) {
            return null;
        }
        return remember(new ClipSpec(any.relativePath(), any.columns(), any.rows(), any.frameDuration(),
                new int[]{0}));
    }

    public boolean hasSheets(String definitionName) {
        FolderMeta folder = folderFor(definitionName);
        return folder != null && !folder.clips.isEmpty();
    }

    /** Resolve a spritesheet under {@code IMAGES/} from a PAM-style asset path */
    public ClipSpec resolveAssetPath(String assetPath, String... preferred) {
        if (assetPath == null || assetPath.isBlank() || root == null) {
            return null;
        }
        String normalized = assetPath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.startsWith("IMAGES/")) {
            normalized = normalized.substring("IMAGES/".length());
        } else if (upper.startsWith("IMAGES\\")) {
            normalized = normalized.substring("IMAGES\\".length());
        }
        String dirRel;
        String fileStem = null;
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            int slash = normalized.lastIndexOf('/');
            dirRel = slash < 0 ? "" : normalized.substring(0, slash);
            String file = slash < 0 ? normalized : normalized.substring(slash + 1);
            fileStem = file.substring(0, file.length() - 4);
        } else {
            dirRel = normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        }
        if (dirRel.isBlank()) {
            return null;
        }
        FolderMeta folder = folderForRelativeDir(dirRel);
        if (folder == null) {
            return null;
        }
        if (preferred != null) {
            for (String want : preferred) {
                if (want == null || want.isBlank()) {
                    continue;
                }
                ClipSpec spec = folder.clips.get(want.toLowerCase(Locale.ROOT));
                if (spec != null) {
                    return remember(spec);
                }
            }
        }
        if (fileStem != null) {
            ClipSpec byStem = folder.clips.get(fileStem.toLowerCase(Locale.ROOT));
            if (byStem != null) {
                return remember(byStem);
            }
        }
        ClipSpec animation = folder.clips.get("animation");
        if (animation != null) {
            return remember(animation);
        }
        if (!folder.clips.isEmpty()) {
            return remember(folder.clips.values().iterator().next());
        }
        return null;
    }

    private FolderMeta folderForRelativeDir(String relativeDir) {
        String key = "rel:" + relativeDir.replace('\\', '/');
        FolderMeta cached = folderCache.get(key);
        if (cached != null) {
            return cached.missing ? null : cached;
        }
        FileHandle dir = imagesChild(relativeDir);
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            folderCache.put(key, FolderMeta.missing());
            return null;
        }
        FolderMeta meta = loadFolder(dir, relativeDir);
        folderCache.put(key, meta);
        return meta.missing ? null : meta;
    }

    private FileHandle imagesChild(String relativeDir) {
        FileHandle images = root.child("IMAGES").child(relativeDir);
        if (images.exists()) {
            return images;
        }
        return root.child("images").child(relativeDir);
    }

    private FolderMeta folderFor(String definitionName) {
        if (definitionName == null || root == null) {
            return null;
        }
        String folderName = folderNameFor(definitionName);
        String relativeDir = resolution + "/FULL/PLANT/" + folderName;
        String key = "plant:" + folderName;
        FolderMeta cached = folderCache.get(key);
        if (cached != null) {
            return cached.missing ? null : cached;
        }
        FileHandle dir = imagesChild(relativeDir);
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            FolderMeta missing = FolderMeta.missing();
            folderCache.put(key, missing);
            return null;
        }
        FolderMeta meta = loadFolder(dir, relativeDir);
        folderCache.put(key, meta);
        return meta.missing ? null : meta;
    }

    private String folderNameFor(String definitionName) {
        String alias = plantFolderAliases.get(definitionName);
        if (alias != null && !alias.isBlank()) {
            return alias.trim().toUpperCase(Locale.ROOT);
        }
        return PamCatalog.normalize(definitionName);
    }

    private FolderMeta loadFolder(FileHandle dir, String relativeDir) {
        FileHandle metaFile = dir.child(META_FILE);
        Map<String, ClipSpec> clips = new HashMap<>();
        float defaultFps = DEFAULT_FPS;
        if (metaFile.exists()) {
            JsonValue rootJson = new JsonReader().parse(metaFile);
            defaultFps = rootJson.getFloat("fps", DEFAULT_FPS);
            JsonValue clipNode = rootJson.get("clips");
            if (clipNode != null) {
                for (JsonValue c = clipNode.child; c != null; c = c.next) {
                    ClipSpec spec = parseClip(dir, relativeDir, c.name, c, defaultFps);
                    if (spec != null) {
                        clips.put(c.name.toLowerCase(Locale.ROOT), spec);
                    }
                }
            }
        }
        // Discover bare PNGs not listed in sprites.json.
        for (FileHandle file : dir.list()) {
            if (file == null || file.isDirectory()) {
                continue;
            }
            String name = file.name();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) {
                continue;
            }
            String clipName = name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT);
            if (clips.containsKey(clipName)) {
                continue;
            }
            ClipSpec spec = autoSpec(dir, relativeDir, clipName, file.name(), defaultFps, null);
            if (spec != null) {
                clips.put(clipName, spec);
            }
        }
        if (clips.isEmpty()) {
            return FolderMeta.missing();
        }
        return new FolderMeta(false, clips);
    }

    private ClipSpec parseClip(FileHandle dir, String relativeDir, String clipName, JsonValue node,
                               float defaultFps) {
        String file = node.getString("file", clipName + ".png");
        float fps = node.getFloat("fps", defaultFps);
        int columns = node.getInt("columns", 0);
        int rows = node.getInt("rows", 0);
        int[] frames = readFrameIndices(node.get("frames"));
        if (columns <= 0 || rows <= 0) {
            return autoSpec(dir, relativeDir, clipName, file, fps, frames);
        }
        FileHandle png = dir.child(file);
        if (!png.exists()) {
            return null;
        }
        String relative = relativeAssetPath(relativeDir, file);
        return new ClipSpec(relative, columns, rows, 1f / Math.max(1f, fps), frames);
    }

    private ClipSpec autoSpec(FileHandle dir, String relativeDir, String clipName, String fileName,
                              float fps, int[] frames) {
        FileHandle png = dir.child(fileName);
        if (!png.exists()) {
            return null;
        }
        int[] grid = detectGrid(png);
        if (grid == null) {
            return null;
        }
        String relative = relativeAssetPath(relativeDir, fileName);
        return new ClipSpec(relative, grid[0], grid[1], 1f / Math.max(1f, fps), frames);
    }

    private static String relativeAssetPath(String relativeDir, String fileName) {
        return "IMAGES/" + relativeDir + "/" + fileName;
    }

    private static int[] readFrameIndices(JsonValue framesNode) {
        if (framesNode == null || !framesNode.isArray() || framesNode.size == 0) {
            return null;
        }
        int[] frames = new int[framesNode.size];
        int i = 0;
        for (JsonValue v = framesNode.child; v != null; v = v.next) {
            frames[i++] = v.asInt();
        }
        return frames;
    }

    /**
     * Counts opaque content islands to infer a regular {@code columns x rows} grid.
     *
     * @return {@code int[]{columns, rows}} or {@code null}
     */
    static int[] detectGrid(FileHandle png) {
        Pixmap pixmap = null;
        try {
            pixmap = new Pixmap(png);
            int w = pixmap.getWidth();
            int h = pixmap.getHeight();
            if (w <= 0 || h <= 0) {
                return null;
            }
            List<int[]> colIslands = alphaIslands(pixmap, true);
            List<int[]> rowIslands = alphaIslands(pixmap, false);
            int cols = colIslands.size();
            int rows = rowIslands.size();
            if (cols <= 0 || rows <= 0) {
                return new int[]{1, 1};
            }
            return new int[]{cols, rows};
        } catch (Exception ignored) {
            return null;
        } finally {
            if (pixmap != null) {
                pixmap.dispose();
            }
        }
    }

    /** Opaque-alpha runs along columns ({@code alongX}) or rows. */
    private static List<int[]> alphaIslands(Pixmap pixmap, boolean alongX) {
        int w = pixmap.getWidth();
        int h = pixmap.getHeight();
        int n = alongX ? w : h;
        boolean[] solid = new boolean[n];
        for (int i = 0; i < n; i++) {
            int count = 0;
            if (alongX) {
                for (int y = 0; y < h; y += 2) {
                    if ((pixmap.getPixel(i, y) & 0xff) > 10) {
                        count++;
                        if (count > 3) {
                            break;
                        }
                    }
                }
            } else {
                for (int x = 0; x < w; x += 2) {
                    if ((pixmap.getPixel(x, i) & 0xff) > 10) {
                        count++;
                        if (count > 3) {
                            break;
                        }
                    }
                }
            }
            solid[i] = count > 3;
        }
        List<int[]> islands = new ArrayList<>();
        int i = 0;
        while (i < n) {
            if (!solid[i]) {
                i++;
                continue;
            }
            int start = i;
            while (i < n && solid[i]) {
                i++;
            }
            islands.add(new int[]{start, i - 1});
        }
        return islands;
    }

    private static final class FolderMeta {
        final boolean missing;
        final Map<String, ClipSpec> clips;

        FolderMeta(boolean missing, Map<String, ClipSpec> clips) {
            this.missing = missing;
            this.clips = clips;
        }

        static FolderMeta missing() {
            return new FolderMeta(true, Map.of());
        }
    }
}
