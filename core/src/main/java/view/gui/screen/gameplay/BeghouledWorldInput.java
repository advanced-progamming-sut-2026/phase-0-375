package view.gui.screen.gameplay;

import com.badlogic.gdx.InputAdapter;
import model.game.level.minigame.beghouled.BeghouledLevel;

/** Beghouled tile swap plus world collect/feed clicks. */
public final class BeghouledWorldInput extends InputAdapter {
    private final GameplayContext ctx;
    private final int[] cell = new int[2];

    public BeghouledWorldInput(GameplayContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (ctx.swapDragging) {
            updateHover(screenX, screenY);
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (ctx.swapDragging) {
            updateHover(screenX, screenY);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        ctx.view.worldViewport.unproject(ctx.worldTmp.set(screenX, screenY, 0f));
        if (ctx.placement.onWorldClick(ctx.worldTmp.x, ctx.worldTmp.y)) {
            return true;
        }
        if (ctx.isPregame() || ctx.endSequenceActive || ctx.pauseMenuOpen) {
            return false;
        }
        if (!ctx.lawnLayout.worldToCell(ctx.worldTmp.x, ctx.worldTmp.y, cell)) {
            return false;
        }
        return beginSwap();
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!ctx.swapDragging) {
            return false;
        }
        ctx.swapDragging = false;
        ctx.view.worldViewport.unproject(ctx.worldTmp.set(screenX, screenY, 0f));
        int fromCol = ctx.swapFromCol;
        int fromRow = ctx.swapFromRow;
        ctx.swapFromCol = -1;
        ctx.swapFromRow = -1;
        ctx.hoverCol = -1;
        ctx.hoverRow = -1;
        if (!ctx.lawnLayout.worldToCell(ctx.worldTmp.x, ctx.worldTmp.y, cell)) {
            return true;
        }
        ctx.placement.tryBeghouledSwap(fromCol, fromRow, cell[0], cell[1]);
        return true;
    }

    private void updateHover(int screenX, int screenY) {
        ctx.view.worldViewport.unproject(ctx.worldTmp.set(screenX, screenY, 0f));
        if (!ctx.lawnLayout.worldToCell(ctx.worldTmp.x, ctx.worldTmp.y, cell)) {
            ctx.hoverCol = -1;
            ctx.hoverRow = -1;
            return;
        }
        ctx.hoverCol = cell[0];
        ctx.hoverRow = cell[1];
    }

    private boolean beginSwap() {
        var level = GameplayLevelQueries.currentLevel();
        if (!(level instanceof BeghouledLevel beghouled)) {
            return false;
        }
        if (beghouled.plantAt(cell[1], cell[0]) == null || beghouled.isCrater(cell[1], cell[0])) {
            return false;
        }
        ctx.swapDragging = true;
        ctx.swapFromCol = cell[0];
        ctx.swapFromRow = cell[1];
        ctx.hoverCol = cell[0];
        ctx.hoverRow = cell[1];
        return true;
    }
}
