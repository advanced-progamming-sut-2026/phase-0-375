package view.gui.lawn;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import model.enums.ZombieState;
import model.game.core.GameModel;
import model.game.level.minigame.izombie.IZombieLevel;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Pulsing red danger shader that activates when enemy zombies reach the first 2 columns of the lawn (columns 0 and 1).
 * Blends a pulsing red alarm vignette across the viewport and highlights the critical danger columns.
 */
public final class DangerRedShader implements Disposable {
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
            uniform float u_time;
            uniform float u_intensity;
            uniform vec2 u_resolution;
            uniform vec2 u_dangerBounds;
            void main() {
                vec4 texColor = texture2D(u_texture, v_texCoords);

                // Pulsing alarm / heartbeat rhythm
                float pulse = 0.65 + 0.35 * sin(u_time * 5.5);

                // Normalized coordinate across the world
                vec2 norm = clamp(v_world / u_resolution, 0.0, 1.0);

                // 1. Danger Column Overlay (first 2 columns)
                float inDangerCol = 0.0;
                if (v_world.x >= u_dangerBounds.x - 20.0 && v_world.x <= u_dangerBounds.y) {
                    float colT = (u_dangerBounds.y - v_world.x) / max(1.0, (u_dangerBounds.y - u_dangerBounds.x));
                    inDangerCol = clamp(colT, 0.0, 1.0) * 0.35;
                    float stripe = sin((v_world.x + v_world.y) * 0.04 - u_time * 3.0);
                    inDangerCol += stripe * 0.05 * clamp(colT, 0.0, 1.0);
                }

                // 2. Full-Screen / Viewport Danger Vignette
                vec2 borderDist = min(norm, 1.0 - norm);
                float edgeFactor = 1.0 - clamp(min(borderDist.x, borderDist.y * 1.4) / 0.28, 0.0, 1.0);
                float vignette = edgeFactor * edgeFactor;

                // 3. Left-edge breach glow (warning glow where lawn connects to house)
                float leftGlow = (1.0 - smoothstep(0.0, 0.45, norm.x)) * 0.4;

                // Combine danger alpha
                float totalDanger = (vignette * 0.45 + leftGlow + inDangerCol) * pulse * u_intensity;
                totalDanger = clamp(totalDanger, 0.0, 0.75);

                // Deep warning red color
                vec3 redColor = vec3(0.95, 0.05, 0.05);

                gl_FragColor = vec4(redColor, totalDanger * texColor.a * v_color.a);
            }
            """;

    private ShaderProgram program;
    private Texture pixel;
    private float time;
    private float intensity;

    public DangerRedShader() {
    }

    private void ensureLoaded() {
        if (program == null) {
            ShaderProgram.pedantic = false;
            program = new ShaderProgram(VERT, FRAG);
            if (!program.isCompiled()) {
                throw new IllegalStateException("Danger red shader: " + program.getLog());
            }
        }
    }

    /**
     * Determines whether any active enemy zombie has entered the first 2 columns (columns 0 and 1).
     */
    public static boolean isDangerActive(GameModel model) {
        if (model == null) {
            return false;
        }
        // In IZombie, the player commands the zombies, so approaching column 0 is player progress rather than danger.
        if (model.getCurrentLevel() instanceof IZombieLevel) {
            return false;
        }
        List<ZombieInstance> zombies = model.getZombies();
        if (zombies == null || zombies.isEmpty()) {
            return false;
        }
        for (ZombieInstance zombie : zombies) {
            if (zombie == null || !zombie.isAlive() || zombie.getState() == ZombieState.DYING || zombie.isHypnotized()) {
                continue;
            }
            if (zombie.isMovingBackward()) {
                continue;
            }
            // First 2 columns are column 0 and column 1 (i.e. continuousX < 2.0f or gridX in [0, 1])
            if (zombie.getContinuousPosition() != null && zombie.getContinuousX() < 2.0f) {
                return true;
            }
            if (zombie.getGridX() >= 0 && zombie.getGridX() < 2) {
                return true;
            }
            if (zombie.plantColumnAtFacingBorder() >= 0 && zombie.plantColumnAtFacingBorder() < 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Smoothly updates shader intensity based on danger status and elapsed delta time.
     */
    public static float updateIntensity(float currentIntensity, boolean danger, float delta) {
        if (danger) {
            return Math.min(1.0f, currentIntensity + delta * 3.5f);
        } else {
            return Math.max(0.0f, currentIntensity - delta * 2.5f);
        }
    }

    public void update(float delta, GameModel model) {
        this.time += delta;
        boolean danger = isDangerActive(model);
        this.intensity = updateIntensity(this.intensity, danger, delta);
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public float getTime() {
        return time;
    }

    private Texture pixel() {
        if (pixel == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            pixel = new Texture(pm);
            pm.dispose();
        }
        return pixel;
    }

    public void draw(Batch batch, LawnLayout layout, float worldWidth, float worldHeight) {
        if (batch == null || intensity <= 0.001f) {
            return;
        }
        ensureLoaded();
        if (program == null || !program.isCompiled()) {
            return;
        }

        float dangerOriginX = (layout != null) ? layout.cellLeft(0) : 0f;
        float dangerBoundX = (layout != null) ? layout.cellLeft(2) : worldWidth * 0.25f;

        batch.setShader(program);
        program.bind();
        program.setUniformMatrix("u_projTrans", batch.getProjectionMatrix());
        program.setUniformi("u_texture", 0);
        program.setUniformf("u_time", time);
        program.setUniformf("u_intensity", intensity);
        program.setUniformf("u_resolution", worldWidth, worldHeight);
        program.setUniformf("u_dangerBounds", dangerOriginX, dangerBoundX);

        batch.draw(pixel(), 0f, 0f, worldWidth, worldHeight);

        batch.flush();
        batch.setShader(null);
    }

    @Override
    public void dispose() {
        if (program != null) {
            program.dispose();
            program = null;
        }
        if (pixel != null) {
            pixel.dispose();
            pixel = null;
        }
    }
}
