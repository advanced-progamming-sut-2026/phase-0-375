package view.gui.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import model.enums.Chapter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves plant / zombie definition names to PAM paths using {@code animations.json}
 * plus alias tables owned by each team.
 *
 * <p>Plant aliases: {@link PlantPamAliases}. Zombie aliases: {@link ZombiePamAliases}.
 * Exclusive per-entity clip logic belongs in {@code view.gui.anim.plant} /
 * {@code view.gui.anim.zombie}, not here. Use {@link #resolveClip} for shared
 * role → clip selection.
 */
public final class PamCatalog {
    public record PamEntry(String name, String path, Map<String, Float> clips) {}

    private final Map<String, PamEntry> byNormName;
    private final Map<String, String> plantOverrides;

    private PamCatalog(Map<String, PamEntry> byNormName) {
        this.byNormName = byNormName;
        this.plantOverrides = PlantPamAliases.all();
    }

    public static PamCatalog load(FileHandle assetsRoot) {
        Map<String, PamEntry> index = new HashMap<>();
        FileHandle json = assetsRoot.child("animations.json");
        if (!json.exists()) {
            return new PamCatalog(index);
        }
        JsonValue root = new JsonReader().parse(json);
        JsonValue animations = root.get("animations");
        if (animations == null) {
            return new PamCatalog(index);
        }
        for (JsonValue anim = animations.child; anim != null; anim = anim.next) {
            String name = anim.getString("name", null);
            String path = anim.getString("path", null);
            if (name == null || path == null) {
                continue;
            }
            if (!isGameplayPam(path)) {
                continue;
            }
            Map<String, Float> clips = new HashMap<>();
            JsonValue clipNode = anim.get("clips");
            if (clipNode != null) {
                for (JsonValue c = clipNode.child; c != null; c = c.next) {
                    clips.put(c.name, c.asFloat());
                }
            }
            PamEntry entry = new PamEntry(name, path, clips);
            String key = normalize(name);
            PamEntry existing = index.get(key);
            if (existing == null || prefer(entry, existing)) {
                index.put(key, entry);
            }
        }
        return new PamCatalog(index);
    }

    public PamEntry forPlant(String definitionName) {
        if (definitionName == null) {
            return null;
        }
        String override = plantOverrides.get(definitionName);
        if (override != null) {
            PamEntry e = byNormName.get(normalize(override));
            if (e != null) {
                return e;
            }
        }
        return resolve(definitionName);
    }

    public PamEntry forZombie(String definitionName) {
        return forZombie(definitionName, null);
    }

    public PamEntry forZombie(String definitionName, Chapter chapter) {
        if (definitionName == null) {
            return null;
        }
        String override = ZombiePamAliases.pamName(definitionName, chapter);
        if (override != null) {
            PamEntry e = byNormName.get(normalize(override));
            if (e != null) {
                return e;
            }
        }
        return resolve(definitionName);
    }

    /**
     * Picks the first preferred clip that exists on {@code entry}, then soft-falls
     * back to any clip whose name starts with the first preference (e.g. {@code idle}
     * → {@code idle_stage1}).
     *
     * <p>Returns the first preference even when the catalog clip map is empty so
     * {@link pvz.libpvz.pam.PamPlayer} can still try a live lookup after load.
     */
    public String resolveClip(PamEntry entry, String... preferred) {
        if (entry == null || preferred == null || preferred.length == 0) {
            return null;
        }
        Map<String, Float> clips = entry.clips();
        if (clips == null || clips.isEmpty()) {
            return preferred[0];
        }
        Map<String, String> lowerToActual = new HashMap<>();
        for (String name : clips.keySet()) {
            lowerToActual.put(name.toLowerCase(Locale.ROOT), name);
        }
        for (String want : preferred) {
            if (want == null || want.isBlank()) {
                continue;
            }
            String actual = lowerToActual.get(want.toLowerCase(Locale.ROOT));
            if (actual != null) {
                return actual;
            }
        }
        String prefix = preferred[0] == null ? null : preferred[0].toLowerCase(Locale.ROOT);
        if (prefix != null && !prefix.isEmpty()) {
            String best = null;
            for (Map.Entry<String, String> e : lowerToActual.entrySet()) {
                if (e.getKey().startsWith(prefix)) {
                    if (best == null || e.getKey().length() < best.length()) {
                        best = e.getValue();
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return preferred[0];
    }

    /**
     * Exact clip duration from {@link PamEntry#clips()}, or {@code 0} if the clip
     * is missing.
     */
    public float clipDurationSeconds(PamEntry entry, String clipName) {
        String actual = findExactClip(entry, clipName);
        if (actual == null) {
            return 0f;
        }
        Float duration = entry.clips().get(actual);
        return duration != null && duration > 0f ? duration : 0f;
    }

    private static String findExactClip(PamEntry entry, String clipName) {
        if (entry == null || clipName == null || clipName.isBlank()) {
            return null;
        }
        Map<String, Float> clips = entry.clips();
        if (clips == null || clips.isEmpty()) {
            return null;
        }
        String want = clipName.toLowerCase(Locale.ROOT);
        for (String name : clips.keySet()) {
            if (name.toLowerCase(Locale.ROOT).equals(want)) {
                return name;
            }
        }
        return null;
    }

    private PamEntry resolve(String definitionName) {
        String key = normalize(definitionName);
        PamEntry exact = byNormName.get(key);
        if (exact != null) {
            return exact;
        }
        // Soft match: longest catalog key contained in / containing the query.
        PamEntry best = null;
        int bestScore = 0;
        for (Map.Entry<String, PamEntry> e : byNormName.entrySet()) {
            String k = e.getKey();
            if (k.contains(key) || key.contains(k)) {
                int score = Math.min(k.length(), key.length());
                if (score > bestScore) {
                    bestScore = score;
                    best = e.getValue();
                }
            }
        }
        return best;
    }

    private static boolean isGameplayPam(String path) {
        String upper = path.toUpperCase(Locale.ROOT);
        if (upper.contains("/EFFECTS/80S_ARCADE_CABINET/") && !upper.contains("BREAK")) {
            return true;
        }
        if (upper.contains("/EFFECTS/ZOMBIE_") && upper.contains("_ASH/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/CRYSTALSKULL_BEAM/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/ZOMBIE_PROSPECTOR_BLAST_OFF/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/ZOMBIE_HUNTER_SNOWBALL_SPLAT/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/ZOMBIE_OCTOPUS_PROJECTILE/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/DARK_WIZARD_SHEEPENING/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/FROSTBITE_ICE_BLOCK_ZOMBIE/")
                && !upper.contains("BEHIND")) {
            return true;
        }
        if (upper.contains("/EFFECTS/FROSTBITE_ICE_BLOCK_PARTICLES/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/SUN/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/PLANTFOOD_PICKUP/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/COIN_")) {
            return true;
        }
        if (upper.contains("/EFFECTS/MOWER_SPAWN/")) {
            return true;
        }
        if (upper.contains("/MOWERS/")) {
            return true;
        }
        if (upper.contains("/GRAVESTONES/")) {
            return true;
        }
        if (upper.contains("/BACKGROUNDS/WAVE_UPPERLAYER/")) {
            return true;
        }
        if (upper.contains("/BACKGROUNDS/WATER_ZOMBIE_RIPPLE/")) {
            return true;
        }
        if (upper.contains("/EFFECTS/")) {
            return false;
        }
        if (upper.contains("/NPC/")) {
            return false;
        }
        return upper.contains("/PLANT/") || upper.contains("/ZOMBIE/");
    }

    /** Catalog entry by animations.json {@code name}, or {@code null}. */
    public PamEntry byName(String pamName) {
        if (pamName == null) {
            return null;
        }
        return byNormName.get(normalize(pamName));
    }

    /** Prefer INITIAL gameplay packs over FULL / holiday variants. */
    private static boolean prefer(PamEntry candidate, PamEntry existing) {
        int c = score(candidate.path);
        int e = score(existing.path);
        return c > e;
    }

    private static int score(String path) {
        String upper = path.toUpperCase(Locale.ROOT);
        int s = 0;
        if (upper.contains("/INITIAL/")) {
            s += 4;
        }
        if (upper.contains("/PLANT/") || upper.contains("/ZOMBIE/")) {
            s += 2;
        }
        if (upper.contains("/FULL/")) {
            s += 1;
        }
        return s;
    }

    static String normalize(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                sb.append(Character.toUpperCase(ch));
            }
        }
        return sb.toString();
    }
}
