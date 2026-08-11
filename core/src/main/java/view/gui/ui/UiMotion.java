package view.gui.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/**
 * Lightweight Scene2D motion helpers for menu polish.
 */
public final class UiMotion {
    private UiMotion() {}

    public static void fadeSlideIn(Actor actor, float duration) {
        actor.getColor().a = 0f;
        actor.addAction(Actions.fadeIn(duration, Interpolation.fade));
    }

    public static void bindPressScale(Button button) {
        button.setOrigin(Align.center);
        button.setTransform(true);
        button.addListener(new ClickListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                    float x, float y, int pointer, int buttonCode) {
                button.clearActions();
                button.addAction(Actions.scaleTo(0.94f, 0.94f, 0.06f, Interpolation.sine));
                return super.touchDown(event, x, y, pointer, buttonCode);
            }

            @Override
            public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                float x, float y, int pointer, int buttonCode) {
                button.clearActions();
                button.addAction(Actions.scaleTo(1f, 1f, 0.1f, Interpolation.sine));
                super.touchUp(event, x, y, pointer, buttonCode);
            }
        });
    }
}
