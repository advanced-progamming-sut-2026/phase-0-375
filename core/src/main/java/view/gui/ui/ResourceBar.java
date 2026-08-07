package view.gui.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import model.app.App;
import model.user.User;

/**
 * Coin / gem readout for logged-in menus (phase 2).
 */
public final class ResourceBar extends Table {
    private final Label coins;
    private final Label gems;

    public ResourceBar(Skin skin) {
        top().right().pad(16f);
        coins = new Label("Coins: 0", skin, "secondary");
        gems = new Label("Gems: 0", skin, "secondary");
        add(coins).padRight(18f);
        add(gems);
        refresh();
    }

    public void refresh() {
        User user = App.getInstance().getCurrentUser();
        if (user == null) {
            coins.setText("Coins: —");
            gems.setText("Gems: —");
            return;
        }
        coins.setText("Coins: " + user.getCoins());
        gems.setText("Gems: " + user.getGems());
    }
}
