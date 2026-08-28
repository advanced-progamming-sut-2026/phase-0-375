package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import controller.GameMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.user.User;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.UiRegions;

/**
 * Coin / gem wallet chips. In debug mode, tap a chip to add a custom amount.
 */
public final class ResourceBar extends Table {
    private static final float SCALE = 1.15f;
    private static final float CHIP_H = 56f * SCALE;
    private static final float COIN_CHIP_W = 180f * SCALE;
    private static final float GEM_CHIP_W = 158f * SCALE;
    private static final float COL_GAP = 14f * SCALE;
    private static final float BAR_PAD_TOP = 6f * SCALE;
    private static final float BAR_PAD_RIGHT = 16f * SCALE;
    /** Nudge amount text so it sits on the dark plate beside the icon. */
    private static final float AMOUNT_PAD_LEFT = 52f * SCALE;

    private final Skin skin;
    private final Label coins;
    private final Label gems;

    public ResourceBar(Skin skin) {
        this(skin, null);
    }

    public ResourceBar(Skin skin, TextureBank textures) {
        this.skin = skin;
        top().right().padTop(BAR_PAD_TOP).padRight(BAR_PAD_RIGHT);

        if (textures != null) {
            textures.loadSync(UiRegions.ATLAS_UI_ALWAYS_LOADED);
        }

        coins = amountLabel(skin);
        gems = amountLabel(skin);

        AtlasImageButton coinButton = walletButton(textures,
                UiRegions.COIN_BUY_NORMAL, UiRegions.COIN_BUY_SELECTED,
                COIN_CHIP_W, CHIP_H, () -> promptCheat("coin"));
        AtlasImageButton gemButton = walletButton(textures,
                UiRegions.PREMIUM_NORMAL, UiRegions.PREMIUM_SELECTED,
                GEM_CHIP_W, CHIP_H, () -> promptCheat("diamond"));

        add(chip(coinButton, coins, COIN_CHIP_W)).padRight(COL_GAP);
        add(chip(gemButton, gems, GEM_CHIP_W));
        refresh();
    }

    private static Label amountLabel(Skin skin) {
        Label label = new Label("0", skin, "medium");
        label.setAlignment(Align.left);
        label.setColor(Color.WHITE);
        SkinFonts.scaleLabel(label, skin, "medium", SCALE);
        label.setTouchable(Touchable.disabled);
        return label;
    }

    private static Stack chip(AtlasImageButton button, Label amount, float width) {
        Stack stack = new Stack();
        if (button != null) {
            stack.add(button);
        }
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.disabled);
        overlay.add(amount).expand().left().padLeft(AMOUNT_PAD_LEFT);
        stack.add(overlay);
        stack.setSize(width, CHIP_H);
        return stack;
    }

    private static AtlasImageButton walletButton(TextureBank textures, String upId, String downId,
                                                 float w, float h, Runnable action) {
        if (textures == null) {
            return null;
        }
        TextureRegion up = textures.region(upId);
        TextureRegion down = textures.region(downId);
        if (up == null) {
            return null;
        }
        return new AtlasImageButton(up, down, w, h, action);
    }

    private void promptCheat(String type) {
        User user = App.getInstance().getCurrentUser();
        if (user == null || !user.isDebugMode()) {
            return;
        }
        Stage stage = getStage();
        if (stage == null) {
            return;
        }

        Table body = new Table();
        Label hint = new Label("How many " + (type.equals("coin") ? "coins" : "gems") + " to add?",
                skin, "secondary");
        TextField field = new TextField("1000", skin);
        field.setMessageText("amount");
        body.add(hint).left().padBottom(10f).row();
        body.add(field).width(280f).height(48f).padBottom(12f).row();

        TextButton add = new TextButton("Add", skin, "purple");
        Table actions = new Table();
        actions.add(add).width(140f).height(48f);

        Table overlay = ModalCard.create(skin, "Cheat add", body, null);
        // Replace default Close-only footer: ModalCard already adds Close.
        // Hook Add beside by listening on our button placed in body.
        body.add(actions).padTop(4f);

        add.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int amount = parseAmount(field.getText());
                if (amount <= 0) {
                    return;
                }
                CommandResult<Void> result = GameMenuController.getInstance().cheatAdd(amount, type);
                if (result.isSuccess()) {
                    refresh();
                    overlay.remove();
                }
            }
        });

        stage.addActor(overlay);
        stage.setKeyboardFocus(field);
    }

    private static int parseAmount(String text) {
        if (text == null) {
            return 0;
        }
        String trimmed = text.trim().replace(",", "");
        if (trimmed.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void refresh() {
        User user = App.getInstance().getCurrentUser();
        if (user == null) {
            coins.setText("—");
            gems.setText("—");
            return;
        }
        coins.setText(String.valueOf(user.getCoins()));
        gems.setText(String.valueOf(user.getGems()));
    }
}
