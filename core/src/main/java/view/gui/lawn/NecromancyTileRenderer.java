package view.gui.lawn;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import model.enums.GroundType;
import model.game.core.GameModel;
import model.game.map.Cell;
import model.game.map.GameMap;

/**
 * Additive purple glow on {@link GroundType#NECROMANCY} cells.
 *
 * <p>Dark Ages necromancy pads have no dedicated PAM; this shader washes the
 * tile violet with a soft edge so graves/plants on top stay readable.
 */
public final class NecromancyTileRenderer implements Disposable {
    /** Saturated violet — stronger than a plain batch tint. */
    static final Color PURPLE = new Color(0.62f, 0.18f, 1f, 0.55f);

    private static final String VERT = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec2 v_uv;
            void main() {
                v_uv = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
            """;

    private static final String FRAG = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec2 v_uv;
            uniform sampler2D u_texture;
            uniform vec4 u_purple;
            uniform float u_pulse;
            void main() {
                float edgeX = smoothstep(0.0, 0.22, v_uv.x) * smoothstep(1.0, 0.78, v_uv.x);
                float edgeY = smoothstep(0.0, 0.22, v_uv.y) * smoothstep(1.0, 0.78, v_uv.y);
                float edge = edgeX * edgeY;
                float a = u_purple.a * edge * u_pulse;
                // Push chroma harder toward violet than a flat multiply would.
                vec3 rgb = mix(u_purple.rgb, vec3(0.78, 0.28, 1.0), 0.45) * edge * u_pulse;
                gl_FragColor = vec4(rgb, a);
            }
            """;

    private final ShaderProgram program;
    private final Texture pixel;
    private float time;

    public NecromancyTileRenderer() {
        ShaderProgram.pedantic = false;
        program = new ShaderProgram(VERT, FRAG);
        if (!program.isCompiled()) {
            throw new IllegalStateException("Necromancy tile shader: " + program.getLog());
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
    }

    static boolean draws(Cell cell) {
        return cell != null && cell.getGroundType() == GroundType.NECROMANCY;
    }

    /** Soft breathing so the pad reads as active magic, not a static wash. */
    static float pulse(float timeSec) {
        return 0.82f + 0.18f * (float) Math.sin(timeSec * 2.2);
    }

    public void draw(Batch batch, LawnLayout layout, GameModel model, float delta) {
        if (batch == null || layout == null || model == null) {
            return;
        }
        GameMap map = model.getMap();
        if (map == null) {
            return;
        }
        time += Math.max(0f, delta);
        float pulse = pulse(time);
        boolean any = false;
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                if (draws(map.getCell(col, row))) {
                    any = true;
                    break;
                }
            }
            if (any) {
                break;
            }
        }
        if (!any) {
            return;
        }
        batch.flush();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setShader(program);
        program.bind();
        program.setUniformMatrix("u_projTrans", batch.getProjectionMatrix());
        program.setUniformi("u_texture", 0);
        program.setUniformf("u_purple", PURPLE.r, PURPLE.g, PURPLE.b, PURPLE.a);
        program.setUniformf("u_pulse", pulse);
        for (int row = 0; row < map.getRows(); row++) {
            for (int col = 0; col < map.getCols(); col++) {
                if (!draws(map.getCell(col, row))) {
                    continue;
                }
                batch.draw(pixel, layout.cellLeft(col), layout.cellBottom(row),
                        layout.cellWidth(), layout.cellHeight());
            }
        }
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
