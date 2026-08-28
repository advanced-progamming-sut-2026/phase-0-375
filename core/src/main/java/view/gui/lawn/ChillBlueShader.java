package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Vivid ice-cyan blue tint for chilled and frozen zombies.
 * Blends base texture colors with an icy cyan-blue tint and highlighted luminance.
 */
final class ChillBlueShader implements Disposable {
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
            uniform float u_chill;
            void main() {
                vec4 tex = texture2D(u_texture, v_texCoords);
                vec3 base = tex.rgb * v_color.rgb;
                float lum = dot(base, vec3(0.299, 0.587, 0.114));
                vec3 iceBlue = vec3(0.15, 0.75, 1.15) * (lum * 1.15 + 0.05);
                vec3 tinted = base * vec3(0.35, 0.85, 1.3);
                vec3 chillColor = mix(tinted, iceBlue, 0.55);
                vec3 rgb = mix(base, chillColor, clamp(u_chill, 0.0, 1.0));
                gl_FragColor = vec4(rgb, tex.a * v_color.a);
            }
            """;

    private final ShaderProgram program;

    ChillBlueShader() {
        ShaderProgram.pedantic = false;
        program = new ShaderProgram(VERT, FRAG);
        if (!program.isCompiled()) {
            throw new IllegalStateException("Chill blue shader: " + program.getLog());
        }
    }

    void begin(Batch batch, float chill) {
        batch.setShader(program);
        program.bind();
        program.setUniformMatrix("u_projTrans", batch.getProjectionMatrix());
        program.setUniformi("u_texture", 0);
        program.setUniformf("u_chill", chill);
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
