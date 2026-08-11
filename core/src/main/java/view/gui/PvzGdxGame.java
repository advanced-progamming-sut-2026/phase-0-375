package view.gui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import model.app.App;
import model.enums.MenuType;
import pvz.skin.PvzSkin;
import view.gui.audio.GameAudio;
import view.gui.screen.LoginScreen;
import view.gui.screen.MainHubScreen;
import view.gui.screen.RegisterScreen;
import view.gui.ui.SkinSmoothing;

/**
 * Thin Game shell for menu screens. Uses {@link PvzSkin} only — PAM / TextureBank
 * base assets are not required for auth + stub hub.
 */
public class PvzGdxGame extends Game {
    public Skin skin;
    public SpriteBatch batch;

    @Override
    public void create() {
        skin = PvzSkin.get();
        SkinSmoothing.applyLinearFiltering(skin);
        batch = new SpriteBatch();

        App app = App.getInstance();
        GameAudio.get().syncFromUser();
        if (app.getCurrentUser() != null) {
            app.setCurrentMenu(MenuType.MAIN);
            setScreen(new MainHubScreen(this));
        } else {
            app.setCurrentMenu(MenuType.REGISTER);
            setScreen(new RegisterScreen(this));
        }
    }

    @Override
    public void dispose() {
        if (screen != null) {
            screen.dispose();
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        // PvzSkin holds a process-lifetime singleton; disposing here ends the app.
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
        super.dispose();
    }
}
