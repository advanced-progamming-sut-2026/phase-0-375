package view.gui.anim.zombie;

import com.badlogic.gdx.math.Rectangle;
import model.enums.ZombieSize;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;
import view.gui.lawn.LawnLayout;

/**
 * Beach Snorkeler: default walk/eat/die. In water the body is clipped at the
 * lower tile waterline; the head sits on that line with {@code WATER_ZOMBIE_RIPPLE}.
 */
public final class SnorkelerAnim {
    public static final String DEFINITION_NAME = "ZombieBeachSnorkel";
    public static final String PAM_NAME = "ZOMBIE_BEACH_SNORKELER";
    public static final String SKULL_PART = "zombie_snorkeler_skull";
    public static final String RIPPLE_NAME = "WATER_ZOMBIE_RIPPLE";
    public static final String RIPPLE_PATH =
            "768/FULL/BACKGROUNDS/WATER_ZOMBIE_RIPPLE/WATER_ZOMBIE_RIPPLE.PAM";
    public static final String IMP_RIPPLE_NAME = "WATER_IMP_RIPPLE";
    public static final String IMP_RIPPLE_PATH =
            "768/FULL/BACKGROUNDS/WATER_IMP_RIPPLE/WATER_IMP_RIPPLE.PAM";
    public static final String GARGANTUAR_RIPPLE_NAME = "WATER_GARGANTUAR_RIPPLE";
    public static final String GARGANTUAR_RIPPLE_PATH =
            "768/FULL/BACKGROUNDS/WATER_GARGANTUAR_RIPPLE/WATER_GARGANTUAR_RIPPLE.PAM";
    public static final String RIPPLE_CLIP = "ripple";
    /** Snorkeler swim surface — low in the tile. */
    public static final float WATERLINE_FROM_BOTTOM = 0.14f;

    private SnorkelerAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, SnorkelerAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        return null;
    }

    public static boolean isSnorkelerPam(String pam) {
        return pam != null && pam.toUpperCase().contains(PAM_NAME);
    }

    public static float waterLineY(LawnLayout layout, int row) {
        return layout.cellBottom(row) + WATERLINE_FROM_BOTTOM * layout.cellHeight();
    }

    /** Catalog {@code name} for the foot ripple PAM matching zombie size. */
    public static String rippleName(ZombieInstance zombie) {
        ZombieSize size = zombie == null || zombie.getDefinition() == null
                ? null
                : zombie.getDefinition().getSize();
        if (size == ZombieSize.IMP) {
            return IMP_RIPPLE_NAME;
        }
        if (size == ZombieSize.LARGE) {
            return GARGANTUAR_RIPPLE_NAME;
        }
        return RIPPLE_NAME;
    }

    /** Fallback PAM path when the catalog entry is not indexed yet. */
    public static String ripplePath(ZombieInstance zombie) {
        ZombieSize size = zombie == null || zombie.getDefinition() == null
                ? null
                : zombie.getDefinition().getSize();
        if (size == ZombieSize.IMP) {
            return IMP_RIPPLE_PATH;
        }
        if (size == ZombieSize.LARGE) {
            return GARGANTUAR_RIPPLE_PATH;
        }
        return RIPPLE_PATH;
    }

    /**
     * Draw origin so skull-bottom sits on {@code waterLineY} at rise 0 and on
     * {@code standY} at rise 1.
     */
    public static float drawOriginY(float standY, float waterLineY, Rectangle skull,
                                    float scale, float rise) {
        float sunk = sunkOriginY(waterLineY, skull, scale);
        float t = Math.max(0f, Math.min(1f, rise));
        return sunk + (standY - sunk) * t;
    }

    public static float sunkOriginY(float waterLineY, Rectangle skull, float scale) {
        if (skull == null) {
            return waterLineY;
        }
        return waterLineY + (skull.y + skull.height) * scale;
    }

    public static float skullCenterWorldX(float originX, Rectangle skull, float scale, boolean flipX) {
        if (skull == null) {
            return originX;
        }
        float localCx = (skull.x + skull.width * 0.5f) * scale;
        return originX + (flipX ? -localCx : localCx);
    }

    /**
     * PamPlayer origin so the top of the ripple clip sits on the waterline
     * (dark rings hang below). Canvas-centre draw puts the art too high.
     */
    public static float rippleDrawY(float waterY, Rectangle clipBox, float scale) {
        if (clipBox == null) {
            return waterY;
        }
        return waterY + clipBox.y * scale;
    }
}
