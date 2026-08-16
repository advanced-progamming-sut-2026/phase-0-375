package view.gui.anim.zombie;

import com.badlogic.gdx.math.Rectangle;
import model.enums.ZombieBehaviorType;
import model.zombie.behavior.FishBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;
import view.gui.lawn.LawnLayout;

/**
 * Beach Fisherman: {@code intro} on spawn, looping {@code idle} (no walk),
 * {@code cast} then {@code reel} while fishing. {@code die} falls through.
 */
public final class FishermanAnim {
    public static final String DEFINITION_NAME = "ZombieBeachFisherman";
    public static final String PAM_NAME = "ZOMBIE_BEACH_FISHERMAN";
    public static final String INNERTUBE_PART = "zombie_innertube";

    private FishermanAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, FishermanAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        FishBehavior fish = (FishBehavior) zombie.getBehavior(ZombieBehaviorType.FISH);
        if (fish == null) {
            return AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        }
        return switch (fish.getPhase()) {
            case INTRO -> AnimPose.once(entry.path(), "intro", ZombieAnimRole.EATING, null);
            case CASTING -> AnimPose.once(entry.path(), "cast", ZombieAnimRole.EATING, null);
            case REELING -> AnimPose.once(entry.path(), "reel", ZombieAnimRole.EATING, null);
            case IDLE -> AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        };
    }

    public static boolean isFishermanPam(String pam) {
        return pam != null && pam.toUpperCase().contains("ZOMBIE_BEACH_FISHERMAN");
    }

    /** World Y of the innertube's bottom edge (PAM Y-down, draw Y-up). Frozen at death start. */
    public static float waterY(float originY, Rectangle innertube, float scale) {
        return originY - (innertube.y + innertube.height) * scale;
    }

    /**
     * Three-tile-wide strip (centered on his column) from below this tile up
     * to the waterline, so a sinking body that hangs under the cell is still covered.
     */
    public static Rectangle drownMaskWorld(LawnLayout layout, float originX, float originY,
                                           float waterY) {
        if (layout == null) {
            return null;
        }
        return drownMaskWorld(layout, originX, rowAt(layout, originY), waterY);
    }

    /** Same strip, pinned to a grid row so a sunk origin cannot pick the lane below. */
    public static Rectangle drownMaskWorld(LawnLayout layout, float originX, int row,
                                           float waterY) {
        if (layout == null) {
            return null;
        }
        int col = colAt(layout, originX);
        int r = Math.max(0, Math.min(layout.rows() - 1, row));
        float tile = layout.cellWidth();
        float left = layout.cellLeft(col) - tile;
        float bottom = layout.cellBottom(r) - layout.cellHeight();
        float h = waterY - bottom;
        if (h <= 1f) {
            return null;
        }
        return new Rectangle(left, bottom, tile * 3f, h);
    }

    /** True when the invisible tile mask covers any of the zombie's world box. */
    public static boolean overlaps(Rectangle mask, Rectangle zombieWorld) {
        return mask != null && zombieWorld != null && mask.overlaps(zombieWorld);
    }

    /**
     * PAM-local clip box (Y-down, canvas centre) → world AABB (Y-up).
     */
    public static Rectangle spriteWorld(float originX, float originY, Rectangle local,
                                        float scale, boolean flipX) {
        if (local == null || scale <= 0f) {
            return null;
        }
        float x0 = local.x * scale;
        float x1 = (local.x + local.width) * scale;
        if (flipX) {
            float t = x0;
            x0 = -x1;
            x1 = -t;
        }
        float top = originY - local.y * scale;
        float bottom = originY - (local.y + local.height) * scale;
        float left = originX + Math.min(x0, x1);
        return new Rectangle(left, Math.min(bottom, top), Math.abs(x1 - x0), Math.abs(top - bottom));
    }

    public static int colAt(LawnLayout layout, float originX) {
        int col = (int) Math.floor((originX - LawnLayout.LAWN_ORIGIN_X) / layout.cellWidth());
        return Math.max(0, Math.min(layout.cols() - 1, col));
    }

    public static int rowAt(LawnLayout layout, float originY) {
        float top = LawnLayout.LAWN_ORIGIN_Y + LawnLayout.GRID_HEIGHT;
        int row = (int) Math.floor((top - originY) / layout.cellHeight());
        return Math.max(0, Math.min(layout.rows() - 1, row));
    }
}
