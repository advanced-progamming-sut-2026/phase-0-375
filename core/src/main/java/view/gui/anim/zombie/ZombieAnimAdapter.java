package view.gui.anim.zombie;

import model.enums.ZombieState;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Global zombie defaults: model → {@link ZombieAnimRole} → PAM clip.
 *
 * <p><b>Ownership:</b> zombie team. Exclusive zombies go through {@link ZombieAnimOverrides}.
 * Do not mutate the model here.
 */
public final class ZombieAnimAdapter {
    private final PamCatalog catalog;
    private final ZombieAnimOverrides overrides;

    public ZombieAnimAdapter(PamCatalog catalog) {
        this(catalog, ZombieAnimOverrides.createDefault());
    }

    public ZombieAnimAdapter(PamCatalog catalog, ZombieAnimOverrides overrides) {
        this.catalog = catalog;
        this.overrides = overrides != null ? overrides : ZombieAnimOverrides.createDefault();
    }

    public AnimPose poseFor(ZombieInstance zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            return null;
        }
        // Dead zombies leave the field; skip draw. Dying gets a die clip later.
        if (zombie.getState() == ZombieState.DEAD) {
            return null;
        }
        PamCatalog.PamEntry entry = catalog.forZombie(zombie.getDefinition().getName());
        if (entry == null) {
            return null;
        }
        ZombieAnimRole role = roleFor(zombie);
        AnimPose custom = overrides.tryResolve(zombie, entry, role);
        if (custom != null) {
            return custom;
        }
        String clip = catalog.resolveClip(entry, preferredClips(role));
        if (clip == null) {
            return null;
        }
        return AnimPose.looping(entry.path(), clip, role);
    }

    /**
     * Global zombie role mapping: eat → walk → idle.
     *
     * <p>TODO: DIE while {@link ZombieState#DYING}; SPECIAL for
     * {@link ZombieState#SPECIAL_ACTION} / {@link ZombieState#PUSHING} /
     * {@link ZombieState#USING_ITEM}.
     */
    private ZombieAnimRole roleFor(ZombieInstance zombie) {
        if (zombie.isEating()) {
            return ZombieAnimRole.EATING;
        }
        return switch (zombie.getState()) {
            case SPAWNING, WALKING, HYPNOTIZED, CHILLED, PUSHING -> ZombieAnimRole.WALK;
            case STUNNED, USING_ITEM, SPECIAL_ACTION, DYING -> ZombieAnimRole.IDLE;
            case EATING -> ZombieAnimRole.EATING;
            case DEAD -> ZombieAnimRole.IDLE;
        };
    }

    private static String[] preferredClips(ZombieAnimRole role) {
        return switch (role) {
            case IDLE -> new String[]{"idle", "idle2", "idle1", "loop"};
            case EATING -> new String[]{"eat", "eating", "attack"};
            case WALK -> new String[]{"walk", "idle"};
            // TODO: case DIE -> new String[]{"die", "death"};
            // TODO: case SPECIAL -> ...
        };
    }
}
