package view.gui.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import model.network.client.NetworkClient;
import model.network.enums.InviteDecision;
import model.network.packet.CancelInvitePacket;
import model.network.packet.InviteReceivedPacket;
import model.network.packet.InviteResponsePacket;
import model.network.packet.matchmaking.MatchFoundPacket;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.screen.AbstractMenuScreen;
import view.gui.screen.MultiplayerMatchBootstrap;

import java.util.function.Consumer;

/**
 * Cross-Client Incoming Match Invite Popup Overlay with a 10-second countdown timer.
 */
public class InviteReceivedOverlay extends Table {

    private static final float FADE_IN = 0.20f;
    private static final float FADE_OUT = 0.17f;
    private static Texture whitePixelTex;

    private final PvzGdxGame game;
    private final NetworkClient networkClient;
    private final InviteReceivedPacket invitePacket;
    private final Runnable onDismissCallback;

    private Label timerLabel;
    private float remainingSeconds;
    private boolean decisionMade = false;
    private boolean accepted = false;

    private Consumer<CancelInvitePacket> cancelHandler;
    private Consumer<MatchFoundPacket> matchFoundHandler;

    public InviteReceivedOverlay(
            PvzGdxGame game,
            Skin skin,
            NetworkClient networkClient,
            InviteReceivedPacket invitePacket,
            Runnable onDismissCallback
    ) {
        this.game = game;
        this.networkClient = networkClient;
        this.invitePacket = invitePacket;
        this.onDismissCallback = onDismissCallback;
        this.remainingSeconds = invitePacket != null && invitePacket.getTimeoutSeconds() > 0
                ? invitePacket.getTimeoutSeconds() : 10.0f;
        setFillParent(true);
        setName(AbstractMenuScreen.OVERLAY_NAME);
        setBackground(new TextureRegionDrawable(getWhitePixel()).tint(new Color(0f, 0f, 0f, 0.65f)));
        setUserObject((Runnable) () -> triggerDecline(InviteDecision.DECLINE));
        add(buildCard(skin)).width(540f).pad(20f);
        setupNetworkListeners();
        getColor().a = 0f;
        addAction(Actions.fadeIn(FADE_IN));
    }

    private BorderedTable buildCard(Skin skin) {
        BorderedTable card = new BorderedTable();
        card.pad(24f);
        Label title = new Label("GAME INVITE RECEIVED!", skin, "big");
        title.setColor(Color.BLACK);
        title.setAlignment(Align.center);
        card.add(title).colspan(2).padBottom(14f).row();
        String inviter = (invitePacket != null && invitePacket.getInviterUsername() != null)
                ? invitePacket.getInviterUsername() : "A player";
        Label body = new Label(inviter + " invites you to a 1v1 I, Zombie match!", skin, "medium");
        body.setColor(Color.BLACK);
        body.setWrap(true);
        body.setAlignment(Align.center);
        card.add(body).colspan(2).width(460f).padBottom(16f).row();
        timerLabel = new Label("Auto-decline in: " + (int) Math.ceil(remainingSeconds) + "s",
                skin, "secondary");
        timerLabel.setColor(new Color(0.85f, 0.35f, 0.1f, 1f));
        timerLabel.setAlignment(Align.center);
        card.add(timerLabel).colspan(2).padBottom(20f).row();
        card.add(actionButtons(skin)).colspan(2).row();
        return card;
    }

    private Table actionButtons(Skin skin) {
        Table actions = new Table();
        TextButton declineBtn = new TextButton("DECLINE", skin, "brown");
        declineBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                triggerDecline(InviteDecision.DECLINE);
            }
        });
        actions.add(declineBtn).width(160f).height(52f).padRight(16f);
        TextButton acceptBtn = new TextButton("ACCEPT", skin, "green");
        acceptBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                triggerAccept();
            }
        });
        actions.add(acceptBtn).width(160f).height(52f);
        return actions;
    }

    private void setupNetworkListeners() {
        if (networkClient == null) return;

        cancelHandler = cancel -> Gdx.app.postRunnable(() -> {
            if (invitePacket != null && invitePacket.getInviteId() != null
                    && invitePacket.getInviteId().equals(cancel.getInviteId())
                    && !decisionMade) {
                decisionMade = true;
                dismiss(null);
            }
        });
        networkClient.registerHandler(CancelInvitePacket.class, cancelHandler);

        matchFoundHandler = match -> Gdx.app.postRunnable(() -> {
            // Only open for THIS invite's accept — never swallow later random queue matches.
            if (!accepted) {
                return;
            }
            accepted = false;
            dismiss(() -> MultiplayerMatchBootstrap.open(game, networkClient, match));
        });
        networkClient.registerHandler(MatchFoundPacket.class, matchFoundHandler);
    }

    private void unregisterNetworkListeners() {
        if (networkClient == null) return;
        if (cancelHandler != null) {
            networkClient.unregisterHandler(CancelInvitePacket.class, cancelHandler);
            cancelHandler = null;
        }
        if (matchFoundHandler != null) {
            networkClient.unregisterHandler(MatchFoundPacket.class, matchFoundHandler);
            matchFoundHandler = null;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (decisionMade) return;

        remainingSeconds -= delta;
        int sec = (int) Math.ceil(Math.max(0f, remainingSeconds));
        timerLabel.setText("Auto-decline in: " + sec + "s");

        if (remainingSeconds <= 0f) {
            triggerDecline(InviteDecision.TIMEOUT);
        }
    }

    /** True after the user accepted and we are waiting for {@code MatchFoundPacket}. */
    public boolean isJoiningAcceptedInvite() {
        return accepted && decisionMade;
    }

    private void triggerAccept() {
        if (decisionMade) return;
        decisionMade = true;
        accepted = true;

        if (networkClient != null && networkClient.isConnected() && invitePacket != null) {
            networkClient.sendPacket(new InviteResponsePacket(
                    invitePacket.getInviteId(),
                    invitePacket.getInviterUsername(),
                    InviteDecision.ACCEPT
            ));
        }

        timerLabel.setText("Accepted! Connecting to room...");
        timerLabel.setColor(Color.GREEN);
    }

    private void triggerDecline(InviteDecision decision) {
        if (decisionMade) return;
        decisionMade = true;
        accepted = false;

        if (networkClient != null && networkClient.isConnected() && invitePacket != null) {
            networkClient.sendPacket(new InviteResponsePacket(
                    invitePacket.getInviteId(),
                    invitePacket.getInviterUsername(),
                    decision != null ? decision : InviteDecision.DECLINE
            ));
        }

        dismiss(null);
    }

    public void dismiss(Runnable after) {
        unregisterNetworkListeners();
        setTouchable(Touchable.disabled);
        clearActions();
        if (getStage() == null) {
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
            if (after != null) {
                after.run();
            }
            return;
        }
        addAction(Actions.sequence(
                Actions.fadeOut(FADE_OUT),
                Actions.run(() -> {
                    remove();
                    if (onDismissCallback != null) {
                        onDismissCallback.run();
                    }
                    if (after != null) {
                        after.run();
                    }
                })
        ));
    }

    private static Texture getWhitePixel() {
        if (whitePixelTex == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            whitePixelTex = new Texture(pixmap);
            pixmap.dispose();
        }
        return whitePixelTex;
    }
}
