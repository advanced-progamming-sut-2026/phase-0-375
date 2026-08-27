package view.gui.anim.zombie;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import model.zombie.instance.ZombieInstance;
import view.gui.assets.ZombiePamAliases;

/**
 * I, Zombie sun producer - PNG spritesheet under {@link ZombiePamAliases#SUNSHINE}.
 */
public final class SunshineAnim {
    public static final float DRAW_OFFSET_Y_CELLS = 0.25f;
    /**
     * Idle sheet is 4×2. Frame 0 leans forward; frame 6 stands more upright for
     * collection-grid portraits.
     */
    public static final int PACKET_PORTRAIT_FRAME = 4;

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

    /** Prefer {@link #PACKET_PORTRAIT_FRAME} when the animation has enough frames. */
    public static TextureRegion packetPortraitFrame(Animation<TextureRegion> animation) {
        if (animation == null) {
            return null;
        }
        float frameDuration = animation.getFrameDuration();
        if (frameDuration <= 0f) {
            return animation.getKeyFrame(0f, false);
        }
        // Avoid getKeyFrames() — libGDX may return Object[] and the TextureRegion[] cast fails.
        float time = PACKET_PORTRAIT_FRAME * frameDuration;
        float duration = animation.getAnimationDuration();
        if (duration > 0f && time >= duration) {
            time = Math.max(0f, duration - frameDuration * 0.5f);
        }
        return animation.getKeyFrame(time, false);
    }
}
