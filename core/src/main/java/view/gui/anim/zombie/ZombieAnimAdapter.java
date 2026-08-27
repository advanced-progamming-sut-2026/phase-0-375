package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.ZombieState;
import model.app.App;
import model.game.core.GameModel;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.zombie.ZombieFactory;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.PamVisibility;
import view.gui.anim.vase.VaseBreakerAnim;
import view.gui.assets.PamCatalog;
import view.gui.assets.PlantSpritesheetCatalog;
import view.gui.assets.ZombiePamAliases;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Global zombie defaults: model → {@link ZombieAnimRole} → PAM clip (or spritesheet fallback).
 *
 * <p><b>Ownership:</b> zombie team. Exclusive zombies go through {@link ZombieAnimOverrides}.
 * Do not mutate the model here.
 */
public final class ZombieAnimAdapter {
    public static final String BUTTER_PART = "butter";

    private final PamCatalog catalog;
    private final PlantSpritesheetCatalog sheets;
    private final ZombieAnimOverrides overrides;

    public ZombieAnimAdapter(PamCatalog catalog) {
        this(catalog, null, ZombieAnimOverrides.createDefault());
    }

    public ZombieAnimAdapter(PamCatalog catalog, PlantSpritesheetCatalog sheets) {
        this(catalog, sheets, ZombieAnimOverrides.createDefault());
    }

    public ZombieAnimAdapter(PamCatalog catalog, ZombieAnimOverrides overrides) {
        this(catalog, null, overrides);
    }

    public ZombieAnimAdapter(PamCatalog catalog, PlantSpritesheetCatalog sheets,
                             ZombieAnimOverrides overrides) {
        this.catalog = catalog;
        this.sheets = sheets;
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
        ZombieAnimRole role = roleFor(zombie);
        PamCatalog.PamEntry entry = entryFor(zombie, chapter);
        if (entry == null) {
            return withButterVisibility(zombie, sheetPose(zombie, role));
        }
        AnimPose custom = overrides.tryResolve(zombie, entry, role);
        if (custom != null) {
            return withButterVisibility(zombie, custom);
        }
        String clip = catalog.resolveClip(entry, preferredClips(role));
        if (clip == null) {
            return withButterVisibility(zombie, sheetPose(zombie, role));
        }
        Map<String, Boolean> vis = armorVisibility(zombie, entry);
        AnimPose pose;
        if (role == ZombieAnimRole.DIE) {
            pose = AnimPose.once(entry.path(), clip, role, vis);
        } else {
            pose = AnimPose.looping(entry.path(), clip, role, vis);
        }
        return withButterVisibility(zombie, pose);
    }

    private AnimPose sheetPose(ZombieInstance zombie, ZombieAnimRole role) {
        if (sheets == null || zombie.getDefinition() == null) {
            return null;
        }
        String name = zombie.getDefinition().getName();
        PlantSpritesheetCatalog.ClipSpec spec = resolveSheet(name, role);
        if (spec == null) {
            return null;
        }
        if (role == ZombieAnimRole.DIE) {
            return AnimPose.sheetOnce(spec.relativePath(), spec.cacheKey(), role);
        }
        return AnimPose.sheetLooping(spec.relativePath(), spec.cacheKey(), role);
    }

    private PlantSpritesheetCatalog.ClipSpec resolveSheet(String definitionName, ZombieAnimRole role) {
        PlantSpritesheetCatalog.ClipSpec spec = sheets.resolveClip(definitionName, preferredClips(role));
        if (spec != null) {
            return spec;
        }
        if (role == ZombieAnimRole.IDLE || role == ZombieAnimRole.WALK || role == ZombieAnimRole.DIE) {
            return sheets.idleFallback(definitionName);
        }
        return sheets.anyClip(definitionName);
    }

    private PamCatalog.PamEntry entryFor(ZombieInstance zombie, Chapter chapter) {
        String name = zombie.getDefinition().getName();
        if ("ZombieGargantuar".equals(name) && isVaseBreakerLevel()) {
            PamCatalog.PamEntry vaseGarg = catalog.byName("VASE_GARGANTUAR");
            if (vaseGarg != null && vaseGarg.path() != null
                    && vaseGarg.path().toUpperCase(java.util.Locale.ROOT).contains("/ZOMBIE/")) {
                return vaseGarg;
            }
            return new PamCatalog.PamEntry(
                    "VASE_GARGANTUAR", VaseBreakerAnim.GARGANTUAR_ZOMBIE, Map.of());
        }
        return catalog.forZombie(name, chapter);
    }

    private static boolean isVaseBreakerLevel() {
        GameModel model = App.getInstance().getCurrentGameModel();
        return model != null && model.getCurrentLevel() instanceof VaseBreakerLevel;
    }

    /** Shows the PAM {@code butter} part while {@link ZombieInstance#isButtered()}. */
    static AnimPose withButterVisibility(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || zombie == null || !zombie.isButtered()) {
            return pose;
        }
        return pose.withVisibleParts(BUTTER_PART);
    }

    public static boolean isDistanceDriven(ZombieInstance zombie, AnimPose pose) {
        if (zombie == null || pose == null || pose.role() != ZombieAnimRole.WALK) {
            return false;
        }
        if (pose.isSpritesheet()) {
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
    public static Map<String, Boolean> armorVisibility(ZombieInstance zombie, PamCatalog.PamEntry entry) {
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

    /**
     * Undamaged armor visibility for Almanac / idle previews (Conehead, Buckethead, …).
     * Builds a fresh instance so cone/bucket layers are shown without a lawn zombie.
     */
    public static Map<String, Boolean> almanacArmorVisibility(String definitionName,
                                                             PamCatalog.PamEntry entry) {
        if (definitionName == null) {
            return null;
        }
        try {
            ZombieInstance zombie = ZombieFactory.createInstance(definitionName);
            if (zombie != null) {
                return armorVisibility(zombie, entry);
            }
        } catch (RuntimeException ignored) {
            // ZombieFactory not initialized yet — fall through.
        }
        return fallbackArmorStates(definitionName, entry);
    }

    private static Map<String, Boolean> fallbackArmorStates(String definitionName,
                                                            PamCatalog.PamEntry entry) {
        List<String> parts = new ArrayList<>(3);
        if (definitionName.endsWith("Armor1")) {
            parts.add("zombie_armor_cone_norm");
        } else if (definitionName.endsWith("Armor2")) {
            parts.add("zombie_armor_bucket_norm");
        } else if (definitionName.endsWith("Armor4")) {
            parts.add("zombie_armor_brick_norm");
        }
        if (entry != null) {
            String states = ZombiePamAliases.armorStatesPart(entry.name(), definitionName);
            if (states != null) {
                parts.add(states);
            }
        }
        return parts.isEmpty() ? null : PamVisibility.show(parts);
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
