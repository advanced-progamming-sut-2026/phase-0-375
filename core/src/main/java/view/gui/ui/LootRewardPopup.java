package view.gui.ui;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import model.enums.LootPickupKind;
import model.item.LootPickup;

/**
 * Brief "+50 Coins" style banner using the bundle-reward tenpatch from PvzSkin.
 */
public final class LootRewardPopup extends Table {
    private static final float SHOW_SEC = 1.6f;

    private final Label label;

    public LootRewardPopup(Skin skin) {
        setTouchable(Touchable.disabled);
        label = new Label("", skin, "bundle_reward_multiplier");
        label.setAlignment(Align.center);
        pad(10f, 28f, 10f, 28f);
        add(label).minWidth(220f);
        getColor().a = 0f;
        setScale(0.65f);
    }

    public void show(LootPickup loot) {
        if (loot == null) {
            return;
        }
        label.setText(format(loot));
        clearActions();
        getColor().a = 0f;
        setScale(0.65f);
        addAction(Actions.sequence(
            Actions.parallel(
                Actions.fadeIn(0.18f),
                Actions.scaleTo(1f, 1f, 0.22f)),
            Actions.delay(SHOW_SEC),
            Actions.parallel(
                Actions.fadeOut(0.28f),
                Actions.scaleTo(0.85f, 0.85f, 0.28f))));
    }

    private static String format(LootPickup loot) {
        int n = loot.getAmount();
        return switch (loot.getKind()) {
            case COIN_GOLD, COIN_SILVER -> "+" + n + (n == 1 ? " Coin" : " Coins");
            case DIAMOND -> "+" + n + (n == 1 ? " Gem" : " Gems");
            case FLOWER_POT -> "+" + n + (n == 1 ? " Flower Pot" : " Flower Pots");
        };
    }
}
