package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import model.game.level.special.ScoreLevel;
import model.game.score.MyopointTracker;

import java.util.Map;

/**
 * End-of-run Myopoint summary for the daily Score Game (win or lose).
 */
public final class MyopointResultsOverlay extends Table {
    private static final float BTN_W = 240f;
    private static final float BTN_H = 64f;
    private static final float BTN_GAP = 20f;
    private static final float ROW_W = BTN_W * 2f + BTN_GAP;
    private static final Color TITLE_WIN = new Color(0.55f, 1f, 0.4f, 1f);
    private static final Color TITLE_LOSE = new Color(1f, 0.45f, 0.4f, 1f);
    private static final Color BEST = new Color(1f, 0.9f, 0.35f, 1f);
    private static final Color BODY = new Color(0.96f, 0.93f, 0.82f, 1f);

    private final Table card;
    private final Table buttons;
    private boolean started;

    public MyopointResultsOverlay(
            Skin skin,
            ScoreLevel level,
            boolean won,
            Runnable onRetry,
            Runnable onExit) {
        setFillParent(true);
        setTouchable(Touchable.childrenOnly);
        center();

        MyopointTracker tracker = level.getTracker();
        BitmapFont big = SkinFonts.outlined(skin, "big");
        BitmapFont medium = SkinFonts.outlined(skin, "medium");

        Label title = new Label(won ? "YOU SURVIVED!" : "RUN OVER",
            new Label.LabelStyle(big, won ? TITLE_WIN : TITLE_LOSE));
        title.setAlignment(Align.center);

        BitmapFont scoreFont = SkinFonts.outlined(SkinFonts.getScaled(skin, "big", 1.35f));
        Label score = new Label(String.valueOf(tracker.getTotalPoints()),
            new Label.LabelStyle(scoreFont, Color.WHITE));
        score.setAlignment(Align.center);

        Label scoreCaption = new Label("Myopoints",
            new Label.LabelStyle(medium, BODY));
        scoreCaption.setAlignment(Align.center);

        card = new Table();
        Drawable cardBg = UiDrawables.tenPatch(skin, "image_ui_cards_store_store_bundle_card");
        if (cardBg != null) {
            card.setBackground(cardBg);
        }
        card.pad(28f, 36f, 24f, 36f);
        card.add(title).growX().padBottom(10f).row();
        card.add(score).growX().padBottom(2f).row();
        card.add(scoreCaption).growX().padBottom(16f).row();

        Table breakdown = new Table();
        breakdown.defaults().left().padBottom(4f);
        for (Map.Entry<String, Integer> entry : tracker.getBreakdown().entrySet()) {
            Label key = new Label(entry.getKey(), new Label.LabelStyle(medium, BODY));
            Label val = new Label(String.valueOf(entry.getValue()),
                new Label.LabelStyle(medium, Color.WHITE));
            val.setAlignment(Align.right);
            breakdown.add(key).growX();
            breakdown.add(val).width(80f).right().row();
        }
        card.add(breakdown).growX().padBottom(12f).row();

        if (level.isNewPersonalBest()) {
            Label best = new Label("New personal best!",
                new Label.LabelStyle(medium, BEST));
            best.setAlignment(Align.center);
            card.add(best).growX().padBottom(8f).row();
        } else if (level.getPreviousPersonalBest() > 0) {
            Label best = new Label("Best: " + level.getPreviousPersonalBest(),
                new Label.LabelStyle(medium, BODY));
            best.setAlignment(Align.center);
            card.add(best).growX().padBottom(8f).row();
        }

        TextButton retry = new TextButton("RETRY", skin, "purple");
        TextButton exit = new TextButton("EXIT", skin, "brown");
        retry.addListener(change(onRetry));
        exit.addListener(change(onExit));
        buttons = new Table();
        buttons.add(retry).width(BTN_W).height(BTN_H).padRight(BTN_GAP);
        buttons.add(exit).width(BTN_W).height(BTN_H);
        buttons.getColor().a = 0f;
        buttons.setTouchable(Touchable.disabled);

        Table stack = new Table();
        stack.add(card).width(ROW_W).padBottom(28f).row();
        stack.add(buttons).width(ROW_W);
        add(stack);

        card.getColor().a = 0f;
        card.setTransform(true);
        card.setScale(0.85f);
    }

    /** Call once the world black fade has finished. */
    public void play() {
        if (started) {
            return;
        }
        started = true;
        card.pack();
        card.setSize(ROW_W, card.getPrefHeight());
        card.setOrigin(ROW_W * 0.5f, card.getHeight() * 0.5f);
        card.clearActions();
        card.addAction(Actions.sequence(
            Actions.parallel(
                Actions.fadeIn(0.25f),
                Actions.scaleTo(1f, 1f, 0.35f, Interpolation.fade)),
            Actions.run(this::showButtons)));
    }

    private void showButtons() {
        buttons.setTouchable(Touchable.enabled);
        buttons.clearActions();
        buttons.addAction(Actions.fadeIn(0.22f));
    }

    private static ChangeListener change(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (action != null) {
                    action.run();
                }
            }
        };
    }
}
