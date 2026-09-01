package view.gui.screen.gameplay;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import view.gui.ui.ZombieHotkeys;

import java.util.List;

/** Couch-play A–L zombie spawn plus arrow-key drop cursor. */
public final class CouchZombieKeyInput extends InputAdapter {
    private final GameplayContext ctx;

    public CouchZombieKeyInput(GameplayContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (!ctx.couchPlayMode || ctx.pauseMenuOpen || ctx.endSequenceActive || ctx.isPregame()) {
            return false;
        }
        if (ctx.zombieDropMode && nudgeIfArrow(keycode)) {
            return true;
        }
        String keyName = Input.Keys.toString(keycode);
        if (keyName != null && keyName.length() == 1) {
            int index = ZombieHotkeys.indexOf(keyName.charAt(0));
            List<String> roster = GameplayLevelQueries.iZombieRosterNames();
            if (index >= 0 && index < roster.size()) {
                ctx.placement.enterZombieDrop(roster.get(index));
                return true;
            }
        }
        return false;
    }

    private boolean nudgeIfArrow(int keycode) {
        int dCol = 0;
        int dRow = 0;
        if (keycode == Input.Keys.LEFT) {
            dCol = -1;
        } else if (keycode == Input.Keys.RIGHT) {
            dCol = 1;
        } else if (keycode == Input.Keys.UP) {
            dRow = -1;
        } else if (keycode == Input.Keys.DOWN) {
            dRow = 1;
        }
        if (dCol == 0 && dRow == 0) {
            return false;
        }
        ctx.placement.nudgeDropCursor(dCol, dRow);
        return true;
    }
}
