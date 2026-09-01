package view.gui.screen.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;

/** Shovel / plant-food arming and OS cursor hide/show. */
public final class GameplayCursors {
    private final GameplayContext ctx;

    public GameplayCursors(GameplayContext ctx) {
        this.ctx = ctx;
    }

    public void setPlantfoodMode(boolean armed) {
        if (armed == ctx.plantfoodMode) {
            return;
        }
        if (armed) {
            setShovelMode(false);
        }
        ctx.plantfoodMode = armed;
        if (ctx.plantFoodBank != null) {
            ctx.plantFoodBank.setButtonChecked(ctx.plantfoodMode);
        }
        applyArmedCursor();
    }

    public void setShovelMode(boolean armed) {
        if (armed == ctx.shovelMode) {
            return;
        }
        if (armed) {
            setPlantfoodMode(false);
        }
        ctx.shovelMode = armed;
        if (ctx.shovelButton != null) {
            ctx.shovelButton.setChecked(ctx.shovelMode);
        }
        applyArmedCursor();
    }

    public void clearArmedModes() {
        if (ctx.plantfoodMode) {
            setPlantfoodMode(false);
        }
        if (ctx.shovelMode) {
            setShovelMode(false);
        }
        ctx.swapDragging = false;
        ctx.swapFromCol = -1;
        ctx.swapFromRow = -1;
        ctx.previewPlant = null;
        ctx.hoverCol = -1;
        ctx.hoverRow = -1;
        ctx.placement.cancelZombieDrop();
    }

    public void restoreOsCursor() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    }

    private void applyArmedCursor() {
        if (ctx.plantfoodMode || ctx.shovelMode) {
            hideOsCursor();
            ctx.previewPlant = null;
            ctx.previewTime = 0f;
        } else {
            restoreOsCursor();
            ctx.hoverCol = -1;
            ctx.hoverRow = -1;
        }
    }

    private void hideOsCursor() {
        if (ctx.hiddenCursor == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            ctx.hiddenCursor = Gdx.graphics.newCursor(pixmap, 0, 0);
            pixmap.dispose();
        }
        if (ctx.hiddenCursor != null) {
            Gdx.graphics.setCursor(ctx.hiddenCursor);
        }
    }
}
