package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

/**
 * SpriteBatch shader that discards fragments inside an invisible world-space
 * AABB (Fisherman drown). Does not draw the mask.
 */
public final class FishermanDrownShader implements Disposable {
    private static final String VERT = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying vec2 v_world;
            void main() {
                v_world = a_position.xy;
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
            varying vec2 v_world;
            uniform sampler2D u_texture;
            uniform vec2 u_maskMin;
            uniform vec2 u_maskMax;
            void main() {
                if (v_world.x >= u_maskMin.x && v_world.x < u_maskMax.x
                        && v_world.y >= u_maskMin.y && v_world.y < u_maskMax.y) {
                    discard;
                }
                gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
            }
            """;

    private final ShaderProgram program;

    public FishermanDrownShader() {
        ShaderProgram.pedantic = false;
        program = new ShaderProgram(VERT, FRAG);
        if (!program.isCompiled()) {
            throw new IllegalStateException("Fisherman drown shader: " + program.getLog());
        }
    }

    /** World-space AABB; fragments inside are not drawn. */
    public void begin(Batch batch, Rectangle mask) {
        batch.setShader(program);
        program.setUniformf("u_maskMin", mask.x, mask.y);
        program.setUniformf("u_maskMax", mask.x + mask.width, mask.y + mask.height);
    }

    public void end(Batch batch) {
        batch.flush();
        batch.setShader(null);
    }

    @Override
    public void dispose() {
        program.dispose();
    }
}
