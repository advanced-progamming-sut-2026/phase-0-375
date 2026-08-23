package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Soft green tint for glowing zombies. Mixes RGB toward a green luminance tint;
 * PAM vertex colors stay in the mix so the body does not flatten like a solid wash.
 *
 * <p>Uniforms after {@link ShaderProgram#bind()} — same rule as {@link HitFlashShader}.
 */
final class GlowGreenShader implements Disposable {
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
            uniform float u_glow;
            void main() {
                vec4 tex = texture2D(u_texture, v_texCoords);
                vec3 base = tex.rgb * v_color.rgb;
                float lum = dot(base, vec3(0.299, 0.587, 0.114));
                vec3 green = vec3(0.35, 1.0, 0.45) * lum;
                vec3 rgb = mix(base, green, clamp(u_glow, 0.0, 1.0));
                gl_FragColor = vec4(rgb, tex.a * v_color.a);
            }
            """;

    private final ShaderProgram program;

    GlowGreenShader() {
        ShaderProgram.pedantic = false;
        program = new ShaderProgram(VERT, FRAG);
        if (!program.isCompiled()) {
            throw new IllegalStateException("Glow green shader: " + program.getLog());
        }
    }

    void begin(Batch batch, float glow) {
        batch.setShader(program);
        program.bind();
        program.setUniformMatrix("u_projTrans", batch.getProjectionMatrix());
        program.setUniformi("u_texture", 0);
        program.setUniformf("u_glow", glow);
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
