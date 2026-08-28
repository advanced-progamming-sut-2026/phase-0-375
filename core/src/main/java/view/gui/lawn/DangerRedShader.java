package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import model.enums.ZombieState;
import model.zombie.instance.ZombieInstance;

/**
 * Red warning shader applied directly to zombies that enter the first 2 columns (columns 0 and 1)
 * to indicate impending danger to the player's house.
 *
 * <p>At the start of the warning flash, the shader starts with a lesser red tone (softer baseline)
 * and smoothly pulses up to a vibrant warning peak.
 */
public final class DangerRedShader implements Disposable {
    public static final float PULSE_HZ = 1.8f;
    /** Softer initial red tone at the start of the warning flash. */
    public static final float LESSER_RED_BASE = 0.25f;
    /** Amplitude of the warning pulse up to peak red. */
    public static final float PULSE_AMP = 0.60f;
    /** Duration in seconds to ramp from the initial lesser tone to full pulse depth. */
    public static final float ENTRY_RAMP_SEC = 0.45f;

    private static final String VERT = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            void main() {
                v_color = a_color;
                v_color.a = v_color.a * (255.0 / 254.0);
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
            """;

    private static final String FRAG = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform float u_red;
            void main() {
                vec4 tex = texture2D(u_texture, v_texCoords);
                vec3 base = tex.rgb * v_color.rgb;
                float lum = dot(base, vec3(0.299, 0.587, 0.114));
                vec3 dangerRed = vec3(1.15, 0.20, 0.20) * (lum * 1.15 + 0.05);
                vec3 tinted = base * vec3(1.3, 0.45, 0.45);
                vec3 redColor = mix(tinted, dangerRed, 0.6);
                vec3 rgb = mix(base, redColor, clamp(u_red, 0.0, 1.0));
                gl_FragColor = vec4(rgb, tex.a * v_color.a);
            }
            """;

    private final ShaderProgram program;

    public DangerRedShader() {
        ShaderProgram.pedantic = false;
        program = new ShaderProgram(VERT, FRAG);
        if (!program.isCompiled()) {
            throw new IllegalStateException("Danger red shader: " + program.getLog());
        }
    }

    public void begin(Batch batch, float red) {
        batch.setShader(program);
        program.bind();
        program.setUniformMatrix("u_projTrans", batch.getProjectionMatrix());
        program.setUniformi("u_texture", 0);
        program.setUniformf("u_red", red);
    }

    public void end(Batch batch) {
        batch.flush();
        batch.setShader(null);
    }

    /**
     * Global clock fallback.
     */
    public static float dangerStrength() {
        return dangerStrength(System.nanoTime() * 1e-9);
    }

    /**
     * Calculates the warning red intensity for a zombie given its elapsed time in the danger zone.
     * Starts at {@link #LESSER_RED_BASE} (0.25) and smoothly pulses between 0.25 and 0.85.
     *
     * @param elapsedSeconds elapsed seconds since entering the danger zone or animation timestamp
     * @return red shader intensity [0.20 .. 0.85]
     */
    public static float dangerStrength(double elapsedSeconds) {
        if (elapsedSeconds < 0.0) {
            elapsedSeconds = 0.0;
        }
        // Smooth sine wave starting at 0.0 at t=0 (cosine 1 -> 0):
        float wave = 0.5f - 0.5f * (float) Math.cos(elapsedSeconds * Math.PI * 2.0 * PULSE_HZ);
        float pulse = LESSER_RED_BASE + PULSE_AMP * wave;

        // Entry ramp for the first ENTRY_RAMP_SEC so it starts smoothly with a lesser red tone
        float ramp = Math.min(1.0f, (float) (elapsedSeconds / ENTRY_RAMP_SEC));
        float initialStart = LESSER_RED_BASE * 0.8f;
        return initialStart + (pulse - initialStart) * ramp;
    }

    /**
     * True if the given zombie is active and advancing in the first 2 columns (col 0 or 1).
     */
    public static boolean isZombieInDangerZone(ZombieInstance zombie) {
        if (zombie == null || !zombie.isAlive() || zombie.getState() == ZombieState.DYING || zombie.isHypnotized()) {
            return false;
        }
        if (zombie.isMovingBackward()) {
            return false;
        }
        if (zombie.getContinuousPosition() != null && zombie.getContinuousX() < 2.0f) {
            return true;
        }
        if (zombie.getGridX() >= 0 && zombie.getGridX() < 2) {
            return true;
        }
        if (zombie.plantColumnAtFacingBorder() >= 0 && zombie.plantColumnAtFacingBorder() < 2) {
            return true;
        }
        return false;
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
