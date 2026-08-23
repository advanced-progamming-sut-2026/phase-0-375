package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import controller.GameMenuController;
import model.app.App;
import model.user.User;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.UiRegions;

/**
 * Coin / gem readout for logged-in menus. In debug mode, free-currency cheat
 * buttons sit under each wallet row.
 *
 * <p>Tune cheat button size / amounts via {@link #CHEAT_BTN_W}, {@link #CHEAT_BTN_H},
 * {@link #CHEAT_COINS}, {@link #CHEAT_GEMS}.
 */
public final class ResourceBar extends Table {
    /** Overall scale for icons, text, cheat buttons, and spacing. */
    private static final float SCALE = 1.2f;
    private static final float ICON = 48f * SCALE;
    private static final float ICON_GAP = 6f * SCALE;
    private static final float COL_GAP = 22f * SCALE;
    /** Space between wallet row and cheat button (smaller = button higher). */
    private static final float CHEAT_PAD_TOP = -10f;
    /**
     * Horizontal shift for cheat chips. Positive = toward the right edge of the screen.
     * (padLeft + right-align does nothing useful here — the bar is already right-anchored.)
     */
    private static final float CHEAT_SHIFT_X_COIN = 10f;
    private static final float CHEAT_SHIFT_X_GEM = 24f;
    /** Nudge "+N" text down inside the cheat button. Bigger = lower. */
    private static final float CHEAT_LABEL_PAD_TOP = 12f;
    /** Outer top inset (smaller = whole bar higher on screen). */
    private static final float BAR_PAD_TOP = 6f * SCALE;
    private static final float BAR_PAD_RIGHT = 16f * SCALE;
    /** Cheat button width (UI px). */
    private static final float CHEAT_BTN_W = 120f * SCALE;
    /** Cheat button height (UI px). */
    private static final float CHEAT_BTN_H = 52f * SCALE;
    private static final int CHEAT_COINS = 1000;
    private static final int CHEAT_GEMS = 100;

    private final Label coins;
    private final Label gems;
    private final Stack addCoinsStack;
    private final Stack addGemsStack;
    private final AtlasImageButton addCoinsButton;
    private final AtlasImageButton addGemsButton;
    private final Cell<?> addCoinsCell;
    private final Cell<?> addGemsCell;

    public ResourceBar(Skin skin) {
        this(skin, null);
    }

    public ResourceBar(Skin skin, TextureBank textures) {
        top().right().padTop(BAR_PAD_TOP).padRight(BAR_PAD_RIGHT);

        if (textures != null) {
            textures.loadSync(UiRegions.ATLAS_WORLD_MAP);
            textures.loadSync(UiRegions.ATLAS_UI_ALWAYS_LOADED);
        }

        TextureRegion coinRegion = textures != null ? textures.region(UiRegions.COIN_ICON) : null;
        TextureRegion gemRegion = textures != null ? textures.region(UiRegions.GEM_ICON) : null;

        Image coinIcon = coinRegion != null ? new Image(new TextureRegionDrawable(coinRegion)) : null;
        Image gemIcon = gemRegion != null ? new Image(new TextureRegionDrawable(gemRegion)) : null;

        coins = new Label("0", skin, "medium");
        coins.setFontScale(SCALE);
        gems = new Label("0", skin, "medium");
        gems.setFontScale(SCALE);

        addCoinsButton = cheatButton(textures,
                UiRegions.FREE_COINS_GOLDEN_UP, UiRegions.FREE_COINS_GOLDEN_DOWN,
                () -> cheat("coin", CHEAT_COINS));
        addGemsButton = cheatButton(textures,
                UiRegions.FREE_COINS_UP, UiRegions.FREE_COINS_DOWN,
                () -> cheat("diamond", CHEAT_GEMS));

        addCoinsStack = labeledCheat(skin, addCoinsButton, "+" + CHEAT_COINS);
        addGemsStack = labeledCheat(skin, addGemsButton, "+" + CHEAT_GEMS);

        Table coinCol = new Table();
        Table coinRow = new Table();
        if (coinIcon != null) {
            coinRow.add(coinIcon).size(ICON).padRight(ICON_GAP);
        }
        coinRow.add(coins);
        coinCol.add(coinRow).right().row();
        if (addCoinsStack != null) {
            addCoinsCell = coinCol.add(addCoinsStack).size(CHEAT_BTN_W, CHEAT_BTN_H)
                    .padTop(CHEAT_PAD_TOP).padRight(-CHEAT_SHIFT_X_COIN).right();
        } else {
            addCoinsCell = null;
        }

        Table gemCol = new Table();
        Table gemRow = new Table();
        if (gemIcon != null) {
            gemRow.add(gemIcon).size(ICON).padRight(ICON_GAP);
        }
        gemRow.add(gems);
        gemCol.add(gemRow).right().row();
        if (addGemsStack != null) {
            addGemsCell = gemCol.add(addGemsStack).size(CHEAT_BTN_W, CHEAT_BTN_H)
                    .padTop(CHEAT_PAD_TOP).padRight(-CHEAT_SHIFT_X_GEM).right();
        } else {
            addGemsCell = null;
        }

        add(coinCol).padRight(COL_GAP);
        add(gemCol);
        refresh();
    }

    private static Stack labeledCheat(Skin skin, AtlasImageButton button, String text) {
        if (button == null) {
            return null;
        }
        Label label = new Label(text, skin, "medium");
        label.setAlignment(Align.center);
        label.setColor(Color.WHITE);
        label.setFontScale(SCALE);
        label.setTouchable(Touchable.disabled);

        Stack stack = new Stack();
        stack.add(button);
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.disabled);
        overlay.add(label).expand().center().padTop(CHEAT_LABEL_PAD_TOP);
        stack.add(overlay);
        return stack;
    }

    private static AtlasImageButton cheatButton(TextureBank textures, String upId, String downId,
                                                Runnable action) {
        if (textures == null) {
            return null;
        }
        TextureRegion up = textures.region(upId);
        TextureRegion down = textures.region(downId);
        if (up == null) {
            return null;
        }
        return new AtlasImageButton(up, down, CHEAT_BTN_W, CHEAT_BTN_H, action);
    }

    private void cheat(String type, int amount) {
        User user = App.getInstance().getCurrentUser();
        if (user == null || !user.isDebugMode()) {
            return;
        }
        GameMenuController.getInstance().cheatAdd(amount, type);
        refresh();
    }

    public void refresh() {
        User user = App.getInstance().getCurrentUser();
        boolean debug = user != null && user.isDebugMode();
        layoutCheat(addCoinsStack, addCoinsButton, addCoinsCell, debug, CHEAT_SHIFT_X_COIN);
        layoutCheat(addGemsStack, addGemsButton, addGemsCell, debug, CHEAT_SHIFT_X_GEM);

        if (user == null) {
            coins.setText("—");
            gems.setText("—");
            return;
        }
        coins.setText(String.valueOf(user.getCoins()));
        gems.setText(String.valueOf(user.getGems()));
    }

    private static void layoutCheat(Stack stack, AtlasImageButton button, Cell<?> cell,
                                    boolean visible, float shiftX) {
        if (stack == null || cell == null) {
            return;
        }
        stack.setVisible(visible);
        if (button != null) {
            button.setDisabled(!visible);
        }
        if (visible) {
            cell.size(CHEAT_BTN_W, CHEAT_BTN_H).padTop(CHEAT_PAD_TOP).padRight(-shiftX);
        } else {
            cell.size(0f).pad(0f);
        }
    }
}
