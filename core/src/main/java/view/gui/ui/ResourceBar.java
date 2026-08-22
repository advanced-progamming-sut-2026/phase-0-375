package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import model.app.App;
import model.user.User;
import pvz.libpvz.textures.TextureBank;
import view.gui.assets.UiRegions;

/**
 * Coin / gem readout for logged-in menus (phase 2).
 */
public final class ResourceBar extends Table {
    private final Label coins;
    private final Label gems;
    private final Image coinIcon;
    private final Image gemIcon;

    public ResourceBar(Skin skin) {
        this(skin, null);
    }

    public ResourceBar(Skin skin, TextureBank textures) {
        top().right().pad(16f);

        TextureRegion coinRegion = textures != null ? textures.region(UiRegions.COIN_ICON) : null;
        TextureRegion gemRegion = textures != null ? textures.region(UiRegions.GEM_ICON) : null;

        coinIcon = coinRegion != null ? new Image(new TextureRegionDrawable(coinRegion)) : null;
        gemIcon = gemRegion != null ? new Image(new TextureRegionDrawable(gemRegion)) : null;

        coins = new Label("0", skin, "medium");
        gems = new Label("0", skin, "medium");

        if (coinIcon != null) {
            add(coinIcon).size(48f).padRight(6f);
        }
        add(coins).padRight(22f);
        if (gemIcon != null) {
            add(gemIcon).size(48f).padRight(6f);
        }
        add(gems);
        refresh();
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
