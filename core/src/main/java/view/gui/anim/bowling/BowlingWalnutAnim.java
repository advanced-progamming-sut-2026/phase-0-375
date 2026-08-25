package view.gui.anim.bowling;

import model.enums.BowlingWalnutType;
import model.game.level.minigame.bowling.BowlingWalnut;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import view.gui.anim.AnimScale;

public final class BowlingWalnutAnim {
    /** Giant walnuts are drawn this many times larger than a normal Wall-nut. */
    public static final float GIANT_SCALE_MUL = 1.45f;
    /** Clockwise degrees per tile of travel for a normal walnut. */
    public static final float DEGREES_PER_TILE = -360f;

    private BowlingWalnutAnim() {}

    /** Definition / catalog name used for art. */
    public static String artPlantName(BowlingWalnutType type) {
        if (type == BowlingWalnutType.GIANT) {
            return "Wall-nut";
        }
        String name = WallnutBowlingLevel.plantNameFor(type);
        return name != null ? name : "Wall-nut";
    }

    public static String artPlantName(BowlingWalnut walnut) {
        return artPlantName(walnut == null ? null : walnut.getType());
    }

    public static float scale(BowlingWalnutType type) {
        if (type == BowlingWalnutType.GIANT) {
            return AnimScale.PLANT * GIANT_SCALE_MUL;
        }
        return AnimScale.PLANT;
    }

    public static float scale(BowlingWalnut walnut) {
        return scale(walnut == null ? null : walnut.getType());
    }

    /**
     * Frozen idle pose spun about the cell center so the walnut looks like it
     * is rolling rather than walking in place.
     */
    public static float rollDegrees(BowlingWalnutType type, float elapsedSeconds) {
        float dist = Math.max(0f, elapsedSeconds) * WallnutBowlingLevel.WALNUT_SPEED;
        float perTile = DEGREES_PER_TILE;
        if (type == BowlingWalnutType.GIANT) {
            perTile /= GIANT_SCALE_MUL;
        }
        return dist * perTile;
    }

    public static float rollDegrees(BowlingWalnut walnut, float elapsedSeconds) {
        return rollDegrees(walnut == null ? null : walnut.getType(), elapsedSeconds);
    }
}
