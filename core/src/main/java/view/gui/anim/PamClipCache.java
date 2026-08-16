package view.gui.anim;

import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Caches {@link ClipRef} handles and kicks async PAM loads when a clip is not ready yet.
 *
 * <p>One cache per shared {@link PamPlayer}. Safe to call from the render thread only.
 */
public final class PamClipCache {
    private final PamPlayer player;
    private final Map<String, ClipRef> clips = new HashMap<>();
    private final Set<String> loading = new HashSet<>();

    public PamClipCache(PamPlayer player) {
        this.player = player;
    }

    /**
     * @return ready clip, or {@code null} if still loading / missing
     */
    public ClipRef getOrLoad(String pamPath, String clipName) {
        if (pamPath == null || clipName == null) {
            return null;
        }
        String key = pamPath + "#" + clipName;
        ClipRef cached = clips.get(key);
        if (cached != null) {
            return cached;
        }
        ClipRef ref;
        try {
            ref = player.getClip(pamPath, clipName);
        } catch (IllegalArgumentException missing) {
            return null;
        }
        if (ref != null) {
            clips.put(key, ref);
            loading.remove(pamPath);
            return ref;
        }
        if (loading.add(pamPath)) {
            player.loadAsync(pamPath, () -> loading.remove(pamPath));
        }
        return null;
    }

    /** Optional sync preload for a known PAM + clips (loading screens / level start). */
    public void preloadSync(String pamPath, String... clipNames) {
        if (pamPath == null) {
            return;
        }
        player.loadSync(pamPath);
        if (clipNames == null) {
            return;
        }
        for (String clip : clipNames) {
            ClipRef ref = player.getClip(pamPath, clip);
            if (ref != null) {
                clips.put(pamPath + "#" + clip, ref);
            }
        }
    }

    // TODO: preloadAsync(Iterable<String> pamPaths) for level-enter loading gates
    // TODO: evict unused ClipRefs when leaving a chapter if memory is tight
}
