package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.ShootBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Explorer: default walk / eat / die. Torch fire ({@code torch_fire_frame_*} and
 * {@code torch_end_lit}) follows {@link ShootBehavior#isTorchLit()}.
 */
public final class ExplorerAnim {
    public static final String DEFINITION_NAME = "ZombieExplorer";
    public static final String TORCH_END_LIT = "torch_end_lit";
    public static final String TORCH_FIRE_PREFIX = "torch_fire_frame_";
    /** PAM names the first flame {@code torch_fire_fire_frame_01}, not {@code torch_fire_frame_01}. */
    public static final String TORCH_FIRE_FIRST_FRAME = "torch_fire_fire_frame_01";
    public static final String TORCH_FIRE_FIRST_PREFIX = "torch_fire_fire_frame_";

    private static final String[] TORCH_PARTS = torchParts();

    private ExplorerAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, ExplorerAnim::resolve);
    }

    static boolean isTorchLitPart(String part) {
        return part != null && (part.equals(TORCH_END_LIT)
                || part.startsWith(TORCH_FIRE_PREFIX)
                || part.startsWith(TORCH_FIRE_FIRST_PREFIX));
    }

    static Map<String, Boolean> torchVisibility(boolean lit) {
        Map<String, Boolean> vis = new HashMap<>();
        for (String part : TORCH_PARTS) {
            vis.put(part, lit);
        }
        return vis;
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null) {
            return null;
        }
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.isExplorer(zombie)) {
            return null;
        }
        Map<String, Boolean> vis = torchVisibility(shoot.isTorchLit());
        String clip = clipFor(entry, role);
        if (role == ZombieAnimRole.DIE) {
            return AnimPose.once(entry.path(), clip, role, vis);
        }
        return AnimPose.looping(entry.path(), clip, role, vis);
    }

    private static String clipFor(PamCatalog.PamEntry entry, ZombieAnimRole role) {
        return switch (role) {
            case IDLE -> firstClip(entry, "idle", "idle2", "idle1", "loop");
            case EATING -> firstClip(entry, "eat", "eating", "attack");
            case WALK -> firstClip(entry, "walk", "idle");
            case DIE -> firstClip(entry, "die", "death");
        };
    }

    private static String firstClip(PamCatalog.PamEntry entry, String... preferred) {
        Map<String, Float> clips = entry.clips();
        if (clips == null || clips.isEmpty()) {
            return preferred[0];
        }
        for (String name : preferred) {
            if (clips.containsKey(name)) {
                return name;
            }
        }
        return preferred[0];
    }

    private static String[] torchParts() {
        String[] parts = new String[1 + 48];
        parts[0] = TORCH_END_LIT;
        int n = 1;
        for (int i = 1; i <= 12; i++) {
            String pad = String.format("%02d", i);
            parts[n++] = TORCH_FIRE_PREFIX + pad;
            parts[n++] = TORCH_FIRE_PREFIX + i;
            parts[n++] = TORCH_FIRE_FIRST_PREFIX + pad;
            parts[n++] = TORCH_FIRE_FIRST_PREFIX + i;
        }
        return parts;
    }
}
