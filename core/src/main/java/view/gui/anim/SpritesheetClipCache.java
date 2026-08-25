package view.gui.anim;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import view.gui.assets.PlantSpritesheetCatalog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads plant spritesheet PNGs into libGDX {@link Animation}s for the PAM fallback path.
 */
public final class SpritesheetClipCache implements Disposable {
    public record SheetAnim(Animation<TextureRegion> animation, float duration, Texture texture) {}

    private final FileHandle assetsRoot;
    private final Map<String, SheetAnim> clips = new HashMap<>();
    private final Set<Texture> textures = new HashSet<>();

    public SpritesheetClipCache(FileHandle assetsRoot) {
        this.assetsRoot = assetsRoot;
    }

    public SheetAnim getOrLoad(PlantSpritesheetCatalog.ClipSpec spec) {
        if (spec == null || assetsRoot == null) {
            return null;
        }
        String key = spec.cacheKey();
        SheetAnim cached = clips.get(key);
        if (cached != null) {
            return cached;
        }
        FileHandle file = assetsRoot.child(spec.relativePath());
        if (!file.exists()) {
            // Case-sensitive FS: try lowercase images/
            String alt = spec.relativePath().replace("IMAGES/", "images/");
            file = assetsRoot.child(alt);
            if (!file.exists()) {
                return null;
            }
        }
        Texture texture = new Texture(file);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.add(texture);

        int cols = Math.max(1, spec.columns());
        int rows = Math.max(1, spec.rows());
        int frameW = texture.getWidth() / cols;
        int frameH = texture.getHeight() / rows;
        if (frameW <= 0 || frameH <= 0) {
            return null;
        }

        TextureRegion[][] grid = TextureRegion.split(texture, frameW, frameH);
        Array<TextureRegion> frames = new Array<>(spec.frameCount());
        if (spec.frameIndices() != null && spec.frameIndices().length > 0) {
            for (int index : spec.frameIndices()) {
                TextureRegion region = regionAt(grid, cols, rows, index);
                if (region != null) {
                    frames.add(region);
                }
            }
        } else {
            for (int r = 0; r < rows && r < grid.length; r++) {
                for (int c = 0; c < cols && c < grid[r].length; c++) {
                    frames.add(grid[r][c]);
                }
            }
        }
        if (frames.size == 0) {
            return null;
        }

        float frameDuration = Math.max(0.001f, spec.frameDuration());
        Animation<TextureRegion> animation =
                new Animation<>(frameDuration, frames, Animation.PlayMode.NORMAL);
        SheetAnim anim = new SheetAnim(animation, frames.size * frameDuration, texture);
        clips.put(key, anim);
        return anim;
    }

    private static TextureRegion regionAt(TextureRegion[][] grid, int cols, int rows, int index) {
        if (index < 0 || cols <= 0) {
            return null;
        }
        int r = index / cols;
        int c = index % cols;
        if (r < 0 || r >= rows || r >= grid.length) {
            return null;
        }
        if (c < 0 || c >= grid[r].length) {
            return null;
        }
        return grid[r][c];
    }

    @Override
    public void dispose() {
        clips.clear();
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
