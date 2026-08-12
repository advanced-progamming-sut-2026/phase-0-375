package view.gui.anim;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable draw request for one entity frame: which PAM clip to play and how.
 *
 * <p>Produced by {@link view.gui.anim.plant.PlantAnimAdapter} /
 * {@link view.gui.anim.zombie.ZombieAnimAdapter}; consumed by
 * {@link view.gui.lawn.LawnEntityRenderer}.
 */
public final class AnimPose {
    private final String pamPath;
    private final String clipName;
    private final Enum<?> role;
    private final boolean loop;
    private final Map<String, Boolean> visibility;

    public AnimPose(String pamPath, String clipName, Enum<?> role, boolean loop,
                    Map<String, Boolean> visibility) {
        this.pamPath = pamPath;
        this.clipName = clipName;
        this.role = role;
        this.loop = loop;
        this.visibility = visibility == null || visibility.isEmpty()
                ? null
                : Collections.unmodifiableMap(visibility);
    }

    public static AnimPose looping(String pamPath, String clipName, Enum<?> role) {
        return new AnimPose(pamPath, clipName, role, true, null);
    }

    public String pamPath() {
        return pamPath;
    }

    public String clipName() {
        return clipName;
    }

    /** Plant or zombie role enum; useful for debugging / exclusive profiles. */
    public Enum<?> role() {
        return role;
    }

    public boolean loop() {
        return loop;
    }

    /** {@code null} means default libPVZ visibility (armor / butter / ink hidden). */
    public Map<String, Boolean> visibility() {
        return visibility;
    }

    public String cacheKey() {
        return pamPath + "#" + clipName;
    }
}
