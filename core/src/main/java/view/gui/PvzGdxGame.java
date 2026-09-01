package view.gui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import model.app.App;
import model.enums.MenuType;
import model.network.client.NetworkClient;
import model.network.packet.InviteReceivedPacket;
import pvz.skin.PvzSkin;
import view.gui.assets.PvzAssets;
import view.gui.audio.GameAudio;
import view.gui.screen.AbstractGameplayScreen;
import view.gui.screen.AbstractMenuScreen;
import view.gui.screen.GameplayScreen;
import view.gui.screen.MainHubScreen;
import view.gui.screen.RegisterScreen;
import view.gui.ui.InviteReceivedOverlay;
import view.gui.ui.SkinFonts;
import view.gui.ui.SkinSmoothing;

import java.util.function.Consumer;

/**
 * Thin Game shell. Menus use {@link PvzSkin}; gameplay screens may request
 * {@link PvzAssets} (TextureBank / PamPlayer) lazily.
 */
public class PvzGdxGame extends Game {
    public Skin skin;
    public SpriteBatch batch;
    public PvzAssets assets;

    private Consumer<InviteReceivedPacket> inviteHandler;
    private NetworkClient inviteBoundClient;

    @Override
    public void create() {
        skin = PvzSkin.get();
        SkinSmoothing.applyLinearFiltering(skin);
        batch = new SpriteBatch();

        App app = App.getInstance();
        app.setOnNetworkConnected(this::bindNetworkInviteListener);
        GameAudio.get().syncFromUser();
        if (app.reconnectStayLoggedInSession()) {
            bindNetworkInviteListener();
            app.setCurrentMenu(MenuType.MAIN);
            setScreen(new MainHubScreen(this));
        } else {
            app.setCurrentMenu(MenuType.REGISTER);
            setScreen(new RegisterScreen(this));
        }
    }

    /** Creates shared libPVZ assets on first gameplay need. */
    public PvzAssets ensureAssets() {
        if (assets == null) {
            assets = PvzAssets.createDefault();
        }
        return assets;
    }

    /**
     * Single app-wide invite listener so popups appear on whatever screen is
     * active (hub, travel log, mini-game menu, gameplay), not only screens that
     * happened to register a handler in {@code show()}.
     */
    public void bindNetworkInviteListener() {
        NetworkClient client = App.getInstance().getNetworkClient();
        if (client == null) {
            return;
        }
        if (inviteBoundClient != null && inviteHandler != null) {
            inviteBoundClient.unregisterHandler(InviteReceivedPacket.class, inviteHandler);
        }
        inviteHandler = this::showIncomingInvite;
        inviteBoundClient = client;
        client.registerHandler(InviteReceivedPacket.class, inviteHandler);
    }

    void showIncomingInvite(InviteReceivedPacket packet) {
        Gdx.app.postRunnable(() -> presentIncomingInvite(packet));
    }

    private void presentIncomingInvite(InviteReceivedPacket packet) {
        if (packet == null) {
            return;
        }
        Screen current = getScreen();
        if (current instanceof GameplayScreen gameplay) {
            gameplay.openInviteOverlay(packet);
            return;
        }
        Stage stage = null;
        if (current instanceof AbstractMenuScreen menu) {
            stage = menu.getStage();
        } else if (current instanceof AbstractGameplayScreen gameplay) {
            stage = gameplay.getUiStage();
        }
        if (stage == null) {
            return;
        }
        for (Actor actor : stage.getActors()) {
            if (actor instanceof InviteReceivedOverlay) {
                return;
            }
        }
        NetworkClient client = App.getInstance().getNetworkClient();
        InviteReceivedOverlay overlay = new InviteReceivedOverlay(this, skin, client, packet, null);
        stage.addActor(overlay);
        overlay.toFront();
    }

    @Override
    public void render() {
        if (assets != null) {
            assets.textures.update();
        }
        super.render();
    }

    @Override
    public void dispose() {
        if (inviteBoundClient != null && inviteHandler != null) {
            inviteBoundClient.unregisterHandler(InviteReceivedPacket.class, inviteHandler);
        }
        if (screen != null) {
            screen.dispose();
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (assets != null) {
            assets.dispose();
            assets = null;
        }
        // PvzSkin holds a process-lifetime singleton; disposing here ends the app.
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
        SkinFonts.disposeDynamicFonts();
        super.dispose();
    }
}
