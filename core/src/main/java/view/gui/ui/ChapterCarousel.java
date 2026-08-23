package view.gui.ui;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import controller.GameMenuController.ChapterSummary;
import view.gui.assets.ChapterIslandArt;
import view.gui.assets.PvzAssets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Horizontal adventure carousel: several chapters visible at once, focus lerps smoothly.
 * Neighbors sit smaller/dimmer beside the selected world.
 */
public final class ChapterCarousel extends Group {
    private static final float SLOT_W = 600f;
    private static final float SLOT_H = 700f;
    private static final float SPACING = 500f;
    private static final float LERP_SPEED = 10f;
    private static final float SIDE_SCALE = 0.68f;
    private static final float FAR_SCALE = 0.5f;
    private static final float SIDE_ALPHA = 0.55f;
    private static final float FAR_ALPHA = 0.28f;

    private final PvzAssets assets;
    private final ChapterIslandArt art;
    private final List<ChapterIslandView> slots = new ArrayList<>();
    private final List<ChapterSummary> chapters = new ArrayList<>();

    /** Continuous focus; lerps toward {@link #targetFocus}. */
    private float focus;
    private float targetFocus;
    private int selectedIndex;
    private IntConsumer onSelectionChanged;
    private IntConsumer onActivate;

    public ChapterCarousel(PvzAssets assets, ChapterIslandArt art) {
        this.assets = assets;
        this.art = art;
        setTransform(false);
        // Empty areas must not swallow clicks meant for arrow / HUD buttons underneath.
        setTouchable(Touchable.childrenOnly);
    }

    public void setOnSelectionChanged(IntConsumer onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public void setOnActivate(IntConsumer onActivate) {
        this.onActivate = onActivate;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void setChapters(List<ChapterSummary> summaries, int initialIndex) {
        clearChildren();
        slots.clear();
        chapters.clear();
        if (summaries != null) {
            chapters.addAll(summaries);
        }

        selectedIndex = chapters.isEmpty()
                ? 0
                : MathUtils.clamp(initialIndex, 0, chapters.size() - 1);
        focus = selectedIndex;
        targetFocus = selectedIndex;

        for (int i = 0; i < chapters.size(); i++) {
            ChapterSummary summary = chapters.get(i);
            ChapterIslandView view = new ChapterIslandView(assets);
            view.setSize(SLOT_W, SLOT_H);
            view.setOrigin(SLOT_W * 0.5f, SLOT_H * 0.5f);
            view.setImageId(art.imageId(summary.chapter()));
            view.setUnlocked(summary.unlocked());
            view.setFill(0.92f);
            final int chapterIndex = i;
            view.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (chapterIndex == selectedIndex) {
                        if (onActivate != null) {
                            onActivate.accept(chapterIndex);
                        }
                    } else {
                        selectIndex(chapterIndex);
                    }
                }
            });
            slots.add(view);
            addActor(view);
        }
        layoutSlots();
        notifySelection();
    }

    /** Step −1 / +1 with wrap; {@code targetFocus} accumulates so lerp takes the short path. */
    public void move(int delta) {
        if (chapters.isEmpty() || delta == 0) {
            return;
        }
        targetFocus += delta;
        selectedIndex = Math.floorMod(Math.round(targetFocus), chapters.size());
        notifySelection();
    }

    public void selectIndex(int index) {
        if (chapters.isEmpty()) {
            return;
        }
        int n = chapters.size();
        index = Math.floorMod(index, n);
        float best = index;
        // Choose unwrapped target closest to current focus for short-path lerp.
        float a = index;
        float b = index + n;
        float c = index - n;
        best = a;
        if (Math.abs(b - focus) < Math.abs(best - focus)) {
            best = b;
        }
        if (Math.abs(c - focus) < Math.abs(best - focus)) {
            best = c;
        }
        targetFocus = best;
        selectedIndex = index;
        notifySelection();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (chapters.isEmpty()) {
            return;
        }
        float t = Math.min(1f, LERP_SPEED * delta);
        focus += (targetFocus - focus) * t;
        if (Math.abs(targetFocus - focus) < 0.001f) {
            focus = targetFocus;
        }
        layoutSlots();
    }

    private void layoutSlots() {
        int n = chapters.size();
        if (n == 0) {
            return;
        }
        float centerX = getWidth() * 0.5f;
        float centerY = getHeight() * 0.5f + 36f;

        // Draw far slots first so the focused island is on top.
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        // Sort by distance from focus descending.
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                if (Math.abs(circularDelta(order[a], focus, n))
                        < Math.abs(circularDelta(order[b], focus, n))) {
                    int tmp = order[a];
                    order[a] = order[b];
                    order[b] = tmp;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            slots.get(order[i]).setZIndex(i);
        }

        for (int i = 0; i < n; i++) {
            ChapterIslandView view = slots.get(i);
            float d = circularDelta(i, focus, n);
            float abs = Math.abs(d);

            float scale;
            float alpha;
            if (abs <= 1f) {
                scale = MathUtils.lerp(1f, SIDE_SCALE, abs);
                alpha = MathUtils.lerp(1f, SIDE_ALPHA, abs);
            } else {
                float t = Math.min(1f, abs - 1f);
                scale = MathUtils.lerp(SIDE_SCALE, FAR_SCALE, t);
                alpha = MathUtils.lerp(SIDE_ALPHA, FAR_ALPHA, t);
            }

            float x = centerX + d * SPACING - SLOT_W * 0.5f;
            float y = centerY - SLOT_H * 0.5f;
            // Slight vertical dip for side cards.
            y -= Math.min(abs, 1.5f) * 18f;

            view.setPosition(x, y);
            view.setScale(scale);
            view.getColor().a = alpha;
            view.setVisible(abs < 2.2f);
        }
    }

    /** Signed circular distance from focus to slot index, in (−n/2, n/2]. */
    private static float circularDelta(int index, float focus, int n) {
        float d = index - focus;
        d -= MathUtils.round(d / n) * n;
        return d;
    }

    private void notifySelection() {
        if (onSelectionChanged != null && !chapters.isEmpty()) {
            onSelectionChanged.accept(selectedIndex);
        }
    }
}
