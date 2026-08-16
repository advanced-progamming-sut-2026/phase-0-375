package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Redraws a sprite as white using its texture alpha. {@code PamPlayer} writes
 * per-vertex PAM colors, so a batch tint never reaches "flash white"; this
 * shader ignores those RGB values.
 *
 * <p>Uniforms must be set after {@link ShaderProgram#bind()} — {@code Batch.setShader}
 * does not bind, and an unbound {@code glUniform} lands on the previous program
 * leaving {@code u_flash} at 0 (invisible overlay).
 */
final class HitFlashShader implements Disposable {
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
            uniform float u_flash;
            void main() {
                float a = texture2D(u_texture, v_texCoords).a * v_color.a * u_flash;
                gl_FragColor = vec4(1.0, 1.0, 1.0, a);
            }
            """;

    private final ShaderProgram program;

    HitFlashShader() {
        ShaderProgram.pedantic = false;
        program = new ShaderProgram(VERT, FRAG);
        if (!program.isCompiled()) {
            throw new IllegalStateException("Hit flash shader: " + program.getLog());
        }
    }

    void begin(Batch batch, float flash) {
        batch.setShader(program);
        program.bind();
        program.setUniformMatrix("u_projTrans", batch.getProjectionMatrix());
        program.setUniformi("u_texture", 0);
        program.setUniformf("u_flash", flash);
    }

    void end(Batch batch) {
        batch.flush();
        batch.setShader(null);
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
