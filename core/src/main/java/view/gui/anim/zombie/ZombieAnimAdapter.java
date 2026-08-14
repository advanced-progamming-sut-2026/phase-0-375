package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.ZombieState;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.PamVisibility;
import view.gui.assets.PamCatalog;
import view.gui.assets.ZombiePamAliases;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return poseFor(zombie, null);
    }

    public AnimPose poseFor(ZombieInstance zombie, Chapter chapter) {
        if (zombie == null || zombie.getDefinition() == null) {
            return null;
        }
        // Dead zombies are removed from the model; the renderer plays a lingering die clip.
        if (zombie.getState() == ZombieState.DEAD) {
            return null;
        }
        PamCatalog.PamEntry entry = catalog.forZombie(zombie.getDefinition().getName(), chapter);
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
        Map<String, Boolean> vis = armorVisibility(zombie, entry);
        if (role == ZombieAnimRole.DIE) {
            return AnimPose.once(entry.path(), clip, role, vis);
        }
        return AnimPose.looping(entry.path(), clip, role, vis);
    }

    public static boolean isDistanceDriven(ZombieInstance zombie, AnimPose pose) {
        if (zombie == null || pose == null || pose.role() != ZombieAnimRole.WALK) {
            return false;
        }
        return switch (zombie.getState()) {
            case WALKING, HYPNOTIZED, CHILLED -> !zombie.isFrozen() && !zombie.isEating();
            default -> false;
        };
    }

    /**
     * Global zombie role mapping: eat → walk → idle.
     *
     * <p>TODO: SPECIAL for {@link ZombieState#SPECIAL_ACTION} /
     * {@link ZombieState#PUSHING} / {@link ZombieState#USING_ITEM}.
     */
    private ZombieAnimRole roleFor(ZombieInstance zombie) {
        if (zombie.getState() == ZombieState.DYING || zombie.getState() == ZombieState.DEAD) {
            return ZombieAnimRole.DIE;
        }
        if (zombie.isEating()) {
            return ZombieAnimRole.EATING;
        }
        return switch (zombie.getState()) {
            case SPAWNING, WALKING, HYPNOTIZED, CHILLED, PUSHING -> ZombieAnimRole.WALK;
            case STUNNED, USING_ITEM, SPECIAL_ACTION -> ZombieAnimRole.IDLE;
            case EATING -> ZombieAnimRole.EATING;
            case DYING, DEAD -> ZombieAnimRole.DIE;
        };
    }

    private static String[] preferredClips(ZombieAnimRole role) {
        return switch (role) {
            case IDLE -> new String[]{"idle", "idle2", "idle1", "loop"};
            case EATING -> new String[]{"eat", "eating", "attack"};
            case WALK -> new String[]{"walk", "idle"};
            case DIE -> new String[]{"die", "death"};
        };
    }

    /** Current armor PAM parts, plus the biome BASIC states group when that clip needs it. */
    static Map<String, Boolean> armorVisibility(ZombieInstance zombie, PamCatalog.PamEntry entry) {
        List<Armor> armors = zombie.getArmors();
        if (armors == null || armors.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>(armors.size() * 2 + 1);
        for (Armor armor : armors) {
            String layer = currentArmorLayer(armor);
            if (layer != null) {
                parts.add(layer);
            }
            String group = ZombiePamAliases.armorGroupPart(armor.getType());
            if (group != null && !armor.isDestroyed()) {
                parts.add(group);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        String states = entry == null
                ? null
                : ZombiePamAliases.armorStatesPart(entry.name(), zombie.getDefinition().getName());
        if (states != null) {
            parts.add(states);
        }
        return PamVisibility.show(parts);
    }

    static String currentArmorLayer(Armor armor) {
        if (armor == null || armor.isDestroyed()) {
            return null;
        }
        List<String> layers = armor.getDamageLayers();
        if (layers == null || layers.isEmpty()) {
            return null;
        }
        int i = armor.getCurrentDamageLayer();
        if (i < 0) {
            i = 0;
        } else if (i >= layers.size()) {
            i = layers.size() - 1;
        }
        return layers.get(i);
    }
}
