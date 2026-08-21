package view.gui.lawn;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * Additive brighten of the hovered lawn row and column while dragging a plant.
 */
public final class LawnRowColHighlight implements Disposable {
    private static final float AMOUNT = 0.35f;

    private static final String VERT = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec2 v_world;
            void main() {
                v_world = a_position.xy;
                gl_Position = u_projTrans * a_position;
            }
            """;

    private static final String FRAG = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec2 v_world;
            uniform sampler2D u_texture;
            uniform float u_colMin;
            uniform float u_colMax;
            uniform float u_rowMin;
            uniform float u_rowMax;
            uniform float u_amount;
            void main() {
                float inCol = step(u_colMin, v_world.x) * (1.0 - step(u_colMax, v_world.x));
                float inRow = step(u_rowMin, v_world.y) * (1.0 - step(u_rowMax, v_world.y));
                float m = max(inCol, inRow);
                if (m < 0.5) discard;
                gl_FragColor = vec4(1.0, 1.0, 1.0, u_amount);
            }
            """;

    private final ShaderProgram program;
    private final Texture pixel;

    public LawnRowColHighlight() {
        ShaderProgram.pedantic = false;
        program = new ShaderProgram(VERT, FRAG);
        if (!program.isCompiled()) {
            throw new IllegalStateException("Lawn row/col highlight shader: " + program.getLog());
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
    }

    /** True if world {@code (x, y)} sits on the highlighted row or column. */
    static boolean covers(LawnLayout layout, int col, int row, float x, float y) {
        if (layout == null || col < 0 || row < 0) {
            return false;
        }
        float colL = layout.cellLeft(col);
        float colR = colL + layout.cellWidth();
        float rowB = layout.cellBottom(row);
        float rowT = rowB + layout.cellHeight();
        return (x >= colL && x < colR) || (y >= rowB && y < rowT);
    }

    public void draw(Batch batch, LawnLayout layout, int col, int row) {
        if (batch == null || layout == null || col < 0 || row < 0) {
            return;
        }
        float colL = layout.cellLeft(col);
        float colR = colL + layout.cellWidth();
        float rowB = layout.cellBottom(row);
        float rowT = rowB + layout.cellHeight();
        batch.flush();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setShader(program);
        program.bind();
        program.setUniformMatrix("u_projTrans", batch.getProjectionMatrix());
        program.setUniformi("u_texture", 0);
        program.setUniformf("u_colMin", colL);
        program.setUniformf("u_colMax", colR);
        program.setUniformf("u_rowMin", rowB);
        program.setUniformf("u_rowMax", rowT);
        program.setUniformf("u_amount", AMOUNT);
        batch.draw(pixel, LawnLayout.LAWN_ORIGIN_X, LawnLayout.LAWN_ORIGIN_Y,
                LawnLayout.GRID_WIDTH, LawnLayout.GRID_HEIGHT);
        batch.flush();
        batch.setShader(null);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void dispose() {
        program.dispose();
        pixel.dispose();
    }
}
