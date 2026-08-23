package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import model.enums.Chapter;
import model.game.core.GameModel;
import model.game.map.Lane;
import model.game.map.LawnMower;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.AnimScale;
import view.gui.anim.PamClipCache;
import view.gui.assets.MowerPam;
import view.gui.assets.PamCatalog;
import view.gui.assets.PvzAssets;

import java.util.IdentityHashMap;

/**
 * Lawn mower spawn intro, idle, transition, and sweep animation.
 */
public final class LawnMowerRenderer {
    public static final float SPAWN_DURATION = 1.5f;
    public static final float REVEAL_AT = 0.1f;
    /** Rest position: slightly left of column 0. */
    private static final float REST_COL = LawnMower.REST_COL;

    private final PamPlayer player;
    private final PamClipCache clips;
    private final PamCatalog catalog;
    private final LawnLayout layout;

    private final IdentityHashMap<Integer, RowVis> rowVis = new IdentityHashMap<>();
    private final IdentityHashMap<String, Float> clocks = new IdentityHashMap<>();

    private String mowerPam = MowerPam.forChapter(null);
    private float introTime;
    private boolean introDone;
    private boolean enabled = true;

    public LawnMowerRenderer(PvzAssets assets, LawnLayout layout) {
        this.player = assets.player;
        this.clips = new PamClipCache(assets.player);
        this.catalog = assets.pamCatalog;
        this.layout = layout;
    }

    public void reset(Chapter chapter, boolean playIntro) {
        mowerPam = MowerPam.forChapter(chapter);
        enabled = playIntro;
        introTime = 0f;
        introDone = !playIntro;
        rowVis.clear();
        clocks.clear();
    }

    public boolean isIntroPlaying() {
        return enabled && !introDone;
    }

    public void tickIntro(float delta) {
        if (!enabled || introDone) {
            return;
        }
        introTime += delta;
        if (introTime >= SPAWN_DURATION) {
            introDone = true;
        }
    }

    /** Per-row clip phase after intro; starts sweep when transition ends. */
    public void tick(GameModel model, float delta) {
        if (!enabled || model == null || model.getMap() == null) {
            return;
        }
        int rows = model.getMap().getRows();
        for (int row = 0; row < rows; row++) {
            Lane lane = model.getMap().getLane(row);
            LawnMower mower = lane == null ? null : lane.getLawnMower();
            RowVis vis = rowVis.computeIfAbsent(row, r -> new RowVis());
            if (mower == null) {
                vis.phase = VisPhase.DONE;
                continue;
            }
            if (!introDone) {
                vis.phase = VisPhase.INTRO;
                continue;
            }
            if (mower.isActive()) {
                vis.phase = VisPhase.IDLE;
                vis.transitionElapsed = 0f;
                continue;
            }
            if (mower.isTriggered() && !mower.isSweeping()) {
                if (vis.phase != VisPhase.TRANSITION) {
                    vis.phase = VisPhase.TRANSITION;
                    vis.transitionElapsed = 0f;
                    clocks.remove(clockKey(row, "transition"));
                }
                vis.transitionElapsed += delta;
                if (vis.transitionElapsed >= transitionDuration()) {
                    mower.beginSweep();
                    vis.phase = VisPhase.ATTACK;
                }
                continue;
            }
            if (mower.isSweeping()) {
                vis.phase = VisPhase.ATTACK;
            }
        }
    }

    public void draw(Batch batch, GameModel model, float delta) {
        if (!enabled || model == null || model.getMap() == null || catalog == null) {
            return;
        }
        int rows = model.getMap().getRows();
        for (int row = 0; row < rows; row++) {
            drawRow(batch, model, delta, row);
        }
    }

    public void drawRow(Batch batch, GameModel model, float delta, int row) {
        if (!enabled || model == null || model.getMap() == null || catalog == null) {
            return;
        }
        Lane lane = model.getMap().getLane(row);
        LawnMower mower = lane == null ? null : lane.getLawnMower();
        RowVis vis = rowVis.computeIfAbsent(row, r -> new RowVis());
        if (vis.phase == VisPhase.DONE && mower == null) {
            return;
        }
        float[] rest = layout.centerOf(row, REST_COL);
        if (!introDone) {
            drawSpawn(batch, rest[0], rest[1]);
            if (introTime >= REVEAL_AT && mower != null) {
                drawMower(batch, row, rest[0], rest[1], "idle", true, delta);
            }
            return;
        }
        if (mower == null) {
            return;
        }
        if (mower.isActive()) {
            drawMower(batch, row, rest[0], rest[1], "idle", true, delta);
        } else if (mower.isTriggered() && !mower.isSweeping()) {
            drawMower(batch, row, rest[0], rest[1], "transition", false, delta);
        } else if (mower.isSweeping()) {
            float[] sweep = layout.centerOf(row, mower.getXPosition());
            drawMower(batch, row, sweep[0], sweep[1], "attack", true, delta);
        }
    }

    private void drawSpawn(Batch batch, float x, float y) {
        PamCatalog.PamEntry entry = catalog.byName(MowerPam.SPAWN);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, "animation");
        drawClip(batch, entry.path(), clip, x, y, true, introTime);
    }

    private void drawMower(Batch batch, int row, float x, float y,
                           String clipPref, boolean loop, float delta) {
        PamCatalog.PamEntry entry = catalog.byName(mowerPam);
        if (entry == null) {
            return;
        }
        String clip = catalog.resolveClip(entry, clipPref);
        float stateTime = advanceClock(clockKey(row, clipPref), delta, loop, mowerPam, clip);
        drawClip(batch, entry.path(), clip, x, y, loop, stateTime);
    }

    private void drawClip(Batch batch, String pamPath, String clip, float x, float y,
                          boolean loop, float stateTime) {
        ClipRef ref = clips.getOrLoad(pamPath, clip);
        if (ref == null) {
            return;
        }
        player.draw(batch, ref, stateTime, x, y, AnimScale.LAWN, AnimScale.LAWN, loop);
    }

    private float transitionDuration() {
        PamCatalog.PamEntry entry = catalog == null ? null : catalog.byName(mowerPam);
        if (entry == null) {
            return 0.35f;
        }
        float dur = catalog.clipDurationSeconds(entry, "transition");
        return dur > 0f ? dur : 0.35f;
    }

    private float advanceClock(String key, float delta, boolean loop,
                               String pamName, String clip) {
        float t = clocks.getOrDefault(key, 0f) + delta;
        if (loop) {
            PamCatalog.PamEntry entry = catalog.byName(pamName);
            if (entry != null) {
                float dur = catalog.clipDurationSeconds(entry, clip);
                if (dur > 0f && t >= dur) {
                    t %= dur;
                }
            }
        }
        clocks.put(key, t);
        return t;
    }

    private static String clockKey(int row, String clip) {
        return row + "|" + clip;
    }

    private enum VisPhase {
        INTRO, IDLE, TRANSITION, ATTACK, DONE
    }

    private static final class RowVis {
        VisPhase phase = VisPhase.INTRO;
        float transitionElapsed;
    }
}
