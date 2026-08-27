package view.gui.anim.zombie;

import model.zombie.instance.ZombieInstance;
import view.gui.assets.ZombiePamAliases;

/**
 * I, Zombie sun producer - PNG spritesheet under {@link ZombiePamAliases#SUNSHINE}.
 */
public final class SunshineAnim {
    public static final float DRAW_OFFSET_Y_CELLS = 0.25f;

    private SunshineAnim() {}

    public static boolean isSunshine(ZombieInstance zombie) {
        return zombie != null
                && zombie.getDefinition() != null
                && isSunshineName(zombie.getDefinition().getName());
    }

    public static boolean isSunshineName(String definitionName) {
        return "ZombieIZombieSun".equals(definitionName);
    }

    public static float drawOffsetY(float cellHeight) {
        return Math.max(0f, cellHeight) * DRAW_OFFSET_Y_CELLS;
    }
}
