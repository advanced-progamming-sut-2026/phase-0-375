package view.gui.anim;

import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Caches {@link ClipRef} handles and kicks async PAM loads when a clip is not ready yet.
 *
 * <p>One cache per shared {@link PamPlayer}. Safe to call from the render thread only.
 * After a PAM is baked, missing clip names fall back to common effect clips / the first
 * available clip instead of throwing.
 */
public final class PamClipCache {
    private static final String[] CLIP_FALLBACKS = {"animation", "idle", "loop", "projectile"};

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

        ClipRef baked;
        try {
            baked = player.getClip(pamPath, "");
        } catch (IllegalArgumentException missing) {
            baked = null;
        }
        if (baked == null) {
            if (loading.add(pamPath)) {
                player.loadAsync(pamPath, () -> loading.remove(pamPath));
            }
            return null;
        }
        loading.remove(pamPath);

        ClipRef resolved = resolveLoadedClip(pamPath, clipName, baked);
        if (resolved != null) {
            clips.put(key, resolved);
        }
        return resolved;
    }

    private ClipRef resolveLoadedClip(String pamPath, String clipName, ClipRef whole) {
        List<String> available = player.clips(pamPath);
        if (available == null || available.isEmpty()) {
            return whole;
        }
        String actual = findClip(available, clipName);
        if (actual == null) {
            for (String pref : CLIP_FALLBACKS) {
                actual = findClip(available, pref);
                if (actual != null) {
                    break;
                }
            }
        }
        if (actual == null) {
            actual = available.getFirst();
        }
        return player.getClip(pamPath, actual);
    }

    private static String findClip(List<String> available, String want) {
        if (want == null || want.isBlank()) {
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
            ClipRef ref = getOrLoad(pamPath, clip);
            if (ref != null) {
                clips.put(pamPath + "#" + clip, ref);
            }
        }
    }

    // TODO: preloadAsync(Iterable<String> pamPaths) for level-enter loading gates
    // TODO: evict unused ClipRefs when leaving a chapter if memory is tight
}
