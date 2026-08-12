package view.gui.anim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helpers for libPVZ part visibility maps.
 */
public final class PamVisibility {
    private PamVisibility() {}

    /**
     * Visibility map with each non-blank part name set to {@code true}.
     * Empty / all-blank input yields {@code null}.
     */
    public static Map<String, Boolean> show(String... partNames) {
        if (partNames == null || partNames.length == 0) {
            return null;
        }
        Map<String, Boolean> map = new HashMap<>();
        for (String part : partNames) {
            putVisible(map, part);
        }
        return freeze(map);
    }

    /**
     * Visibility map with each non-blank part name set to {@code true}.
     * Empty input yields {@code null}.
     */
    public static Map<String, Boolean> show(Iterable<String> partNames) {
        if (partNames == null) {
            return null;
        }
        Map<String, Boolean> map = new HashMap<>();
        for (String part : partNames) {
            putVisible(map, part);
        }
        return freeze(map);
    }

    /**
     * Copy of {@code base} plus each named part forced visible.
     * {@code base} may be {@code null}.
     */
    public static Map<String, Boolean> showAlso(Map<String, Boolean> base, String... partNames) {
        Map<String, Boolean> map = base == null || base.isEmpty()
                ? new HashMap<>()
                : new HashMap<>(base);
        if (partNames != null) {
            for (String part : partNames) {
                putVisible(map, part);
            }
        }
        return freeze(map);
    }

    /**
     * Copy of {@code base} plus each named part forced visible.
     * {@code base} may be {@code null}.
     */
    public static Map<String, Boolean> showAlso(Map<String, Boolean> base, Iterable<String> partNames) {
        Map<String, Boolean> map = base == null || base.isEmpty()
                ? new HashMap<>()
                : new HashMap<>(base);
        if (partNames != null) {
            for (String part : partNames) {
                putVisible(map, part);
            }
        }
        return freeze(map);
    }

    private static void putVisible(Map<String, Boolean> map, String part) {
        if (part != null && !part.isBlank()) {
            map.put(part, Boolean.TRUE);
        }
    }

    private static Map<String, Boolean> freeze(Map<String, Boolean> map) {
        if (map.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableMap(map);
    }
}
