package view.gui.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import model.app.App;
import model.network.client.NetworkClient;
import model.network.enums.InviteStatus;
import model.network.enums.MatchmakingMode;
import model.network.enums.MatchmakingStatus;
import model.network.enums.PlayerRole;
import model.network.packet.CancelInvitePacket;
import model.network.packet.InviteRequestPacket;
import model.network.packet.InviteStatusPacket;
import model.network.packet.matchmaking.CancelMatchmakingPacket;
import model.network.packet.matchmaking.MatchFoundPacket;
import model.network.packet.matchmaking.MatchmakingRequestPacket;
import model.network.packet.matchmaking.MatchmakingResponsePacket;
import model.user.User;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.screen.AbstractMenuScreen;
import view.gui.screen.MultiplayerMatchBootstrap;

import java.util.function.Consumer;

/**
 * Travel-Log I, Zombie multiplayer lobby overlay.
 * Supports Play with Known Player (Direct 10s Invite) and Play with Random Player.
 */
public class IZombieMatchmakingOverlay extends Table {

    private static final float FADE_IN = 0.20f;
    private static final float FADE_OUT = 0.17f;
    private static Texture whitePixelTex;

    private final PvzGdxGame game;
    private final Skin skin;
    private final Runnable onDismissCallback;

    // Known Player UI Elements
    private TextField usernameField;
    private SelectBox<String> knownRoleSelect;
    private TextButton sendInviteBtn;
    private TextButton cancelInviteBtn;
    private Label inviteStatusLabel;
    private String currentInviteId = null;
    private String currentTargetUsername = null;
    private float inviteTimer = 10f;
    private boolean waitingForInvite = false;

    // Random Match UI Elements
    private SelectBox<String> randomRoleSelect;
    private TextButton findRandomBtn;
    private Label randomStatusLabel;
    private boolean waitingForRandom = false;
    private Table searchingOverlay;

    private Consumer<InviteStatusPacket> inviteStatusHandler;
    private Consumer<MatchmakingResponsePacket> matchmakingResponseHandler;
    private Consumer<MatchFoundPacket> matchFoundHandler;

    public IZombieMatchmakingOverlay(PvzGdxGame game, Skin skin, Runnable onDismissCallback) {
        this.game = game;
        this.skin = skin;
        this.onDismissCallback = onDismissCallback;

        setFillParent(true);
        setName(AbstractMenuScreen.OVERLAY_NAME);
        setBackground(new TextureRegionDrawable(getWhitePixel()).tint(new Color(0f, 0f, 0f, 0.60f)));

        Runnable closer = this::dismiss;
        setUserObject(closer);

        buildCard();
        setupNetworkListeners();

        getColor().a = 0f;
        addAction(Actions.fadeIn(FADE_IN));
    }

    private void buildCard() {
        BorderedTable card = new BorderedTable();
        card.pad(24f);

        Label title = new Label("I, ZOMBIE — MULTIPLAYER", skin, "big");
        title.setColor(Color.BLACK);
        title.setAlignment(Align.center);
        card.add(title).colspan(3).padBottom(18f).row();

        // Play with Known Player
        Table knownSection = createSectionBox();
        Label knownHeading = new Label("👥 Play with Known Player", skin, "medium");
        knownHeading.setColor(Color.BLACK);
        knownSection.add(knownHeading).left().row();

        Label knownDesc = new Label("Invite an online player to an authoritative 1v1 match (10s invite window).", skin, "secondary");
        knownDesc.setColor(new Color(0.3f, 0.25f, 0.2f, 1f));
        knownDesc.setWrap(true);
        knownSection.add(knownDesc).width(500f).left().padTop(4f).padBottom(8f).row();

        Table knownControls = new Table();
        usernameField = new TextField("", skin);
        usernameField.setMessageText("Target username");
        knownControls.add(usernameField).width(170f).height(44f).padRight(8f);

        knownRoleSelect = new SelectBox<>(skin);
        knownRoleSelect.setItems("ANY", "PLANT", "ZOMBIE");
        knownControls.add(knownRoleSelect).width(110f).height(44f).padRight(8f);

        sendInviteBtn = new TextButton("SEND INVITE", skin, "green");
        sendInviteBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendDirectInvite();
            }
        });
        knownControls.add(sendInviteBtn).width(140f).height(44f).padRight(8f);

        cancelInviteBtn = new TextButton("CANCEL", skin, "brown");
        cancelInviteBtn.setVisible(false);
        cancelInviteBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cancelDirectInvite();
            }
        });
        knownControls.add(cancelInviteBtn).width(100f).height(44f);
        knownSection.add(knownControls).left().padBottom(4f).row();

        inviteStatusLabel = new Label("", skin, "secondary");
        inviteStatusLabel.setColor(new Color(0.2f, 0.5f, 0.2f, 1f));
        knownSection.add(inviteStatusLabel).left().padTop(2f).row();

        card.add(knownSection).growX().padBottom(14f).row();

        // Play with Random Player
        Table randomSection = createSectionBox();
        Label randomHeading = new Label("🎲 Play with Random Player", skin, "medium");
        randomHeading.setColor(Color.BLACK);
        randomSection.add(randomHeading).left().row();

        Label randomDesc = new Label("Join the matchmaking queue to pair with any available opponent.", skin, "secondary");
        randomDesc.setColor(new Color(0.3f, 0.25f, 0.2f, 1f));
        randomDesc.setWrap(true);
        randomSection.add(randomDesc).width(500f).left().padTop(4f).padBottom(8f).row();

        Table randomControls = new Table();
        randomControls.add(new Label("Role: ", skin, "secondary")).padRight(6f);
        randomRoleSelect = new SelectBox<>(skin);
        randomRoleSelect.setItems("ANY", "PLANT", "ZOMBIE");
        randomControls.add(randomRoleSelect).width(120f).height(44f).padRight(12f);

        findRandomBtn = new TextButton("FIND MATCH", skin, "green");
        findRandomBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startRandomMatchmaking();
            }
        });
        randomControls.add(findRandomBtn).width(160f).height(44f);
        randomSection.add(randomControls).left().padBottom(4f).row();

        randomStatusLabel = new Label("", skin, "secondary");
        randomStatusLabel.setColor(new Color(0.2f, 0.5f, 0.2f, 1f));
        randomSection.add(randomStatusLabel).left().padTop(2f).row();

        card.add(randomSection).growX().padBottom(18f).row();

        TextButton closeBtn = new TextButton("CLOSE", skin, "brown");
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dismiss();
            }
        });
        card.add(closeBtn).width(180f).height(50f).center();

        add(card).width(620f).pad(20f);
    }

    private Table createSectionBox() {
        Table box = new Table();
        box.pad(10f, 14f, 10f, 14f);
        return box;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (waitingForInvite) {
            inviteTimer -= delta;
            int sec = (int) Math.ceil(Math.max(0f, inviteTimer));
            inviteStatusLabel.setText("Waiting for " + currentTargetUsername + "... (" + sec + "s)");
            inviteStatusLabel.setColor(new Color(0.8f, 0.5f, 0.1f, 1f));
            if (inviteTimer <= 0f) {
                waitingForInvite = false;
                resetInvitePanel("Invite timed out after 10 seconds.");
            }
        }
    }

    private NetworkClient getOrConnectClient() {
        App app = App.getInstance();
        NetworkClient client = app.getNetworkClient();
        if (client != null && client.isConnected()) {
            return client;
        }
        try {
            return app.ensureConnected();
        } catch (Exception e) {
            return null;
        }
    }

    private void setupNetworkListeners() {
        NetworkClient client = getOrConnectClient();
        if (client == null) return;

        // Re-bind global invite listener in case connect happened just now.
        game.bindNetworkInviteListener();

        inviteStatusHandler = status -> Gdx.app.postRunnable(() -> {
            if (status.getStatus() == InviteStatus.PENDING) {
                currentInviteId = status.getInviteId();
                waitingForInvite = true;
                inviteTimer = 10f;
                inviteStatusLabel.setText(status.getMessage() != null ? status.getMessage() : "Invite sent!");
                inviteStatusLabel.setColor(new Color(0.2f, 0.6f, 0.2f, 1f));
            } else if (status.getStatus() == InviteStatus.ACCEPTED) {
                waitingForInvite = false;
                inviteStatusLabel.setText("Invite accepted! Joining game room...");
                inviteStatusLabel.setColor(Color.GREEN);
            } else if (status.getStatus() == InviteStatus.DECLINED) {
                waitingForInvite = false;
                resetInvitePanel(status.getMessage() != null ? status.getMessage() : "Invite declined.");
            } else if (status.getStatus() == InviteStatus.TIMED_OUT) {
                waitingForInvite = false;
                resetInvitePanel(status.getMessage() != null ? status.getMessage() : "Invite timed out.");
            } else if (status.getStatus() == InviteStatus.BUSY) {
                waitingForInvite = false;
                resetInvitePanel(status.getMessage() != null ? status.getMessage() : "Player is currently busy.");
            } else if (status.getStatus() == InviteStatus.OFFLINE || status.getStatus() == InviteStatus.NOT_FOUND) {
                waitingForInvite = false;
                resetInvitePanel(status.getMessage() != null ? status.getMessage() : "User is offline or not found.");
            } else if (status.getStatus() == InviteStatus.CANCELLED) {
                waitingForInvite = false;
                resetInvitePanel("Invite cancelled.");
            }
        });
        client.registerHandler(InviteStatusPacket.class, inviteStatusHandler);

        matchmakingResponseHandler = resp -> Gdx.app.postRunnable(() -> {
            if (resp.getStatus() == MatchmakingStatus.QUEUED) {
                waitingForRandom = true;
                showSearchingOverlay();
            } else if (resp.getStatus() == MatchmakingStatus.CANCELLED) {
                waitingForRandom = false;
                hideSearchingOverlay();
                randomStatusLabel.setText("Queue cancelled.");
                randomStatusLabel.setColor(Color.GRAY);
            } else if (resp.getStatus() == MatchmakingStatus.ERROR) {
                waitingForRandom = false;
                hideSearchingOverlay();
                randomStatusLabel.setText("Error: " + resp.getMessage());
                randomStatusLabel.setColor(Color.RED);
            }
        });
        client.registerHandler(MatchmakingResponsePacket.class, matchmakingResponseHandler);

        matchFoundHandler = match -> Gdx.app.postRunnable(() -> {
            if (getStage() == null) {
                return;
            }
            // Only defer when an accepted invite is waiting for its room MatchFound.
            for (Actor actor : getStage().getActors()) {
                if (actor instanceof InviteReceivedOverlay invite && invite.isJoiningAcceptedInvite()) {
                    return;
                }
            }
            hideSearchingOverlay();
            dismissCleanly(() -> MultiplayerMatchBootstrap.open(game, client, match));
        });
        client.registerHandler(MatchFoundPacket.class, matchFoundHandler);
    }

    private void unregisterNetworkListeners() {
        NetworkClient client = App.getInstance().getNetworkClient();
        if (client == null) return;
        if (inviteStatusHandler != null) {
            client.unregisterHandler(InviteStatusPacket.class, inviteStatusHandler);
            inviteStatusHandler = null;
        }
        if (matchmakingResponseHandler != null) {
            client.unregisterHandler(MatchmakingResponsePacket.class, matchmakingResponseHandler);
            matchmakingResponseHandler = null;
        }
        if (matchFoundHandler != null) {
            client.unregisterHandler(MatchFoundPacket.class, matchFoundHandler);
            matchFoundHandler = null;
        }
    }

    private void sendDirectInvite() {
        String target = usernameField.getText().trim();
        if (target.isEmpty()) {
            inviteStatusLabel.setText("Please enter a username.");
            inviteStatusLabel.setColor(Color.RED);
            return;
        }

        NetworkClient client = getOrConnectClient();
        if (client == null || !client.isConnected()) {
            inviteStatusLabel.setText("Server offline / not connected.");
            inviteStatusLabel.setColor(Color.RED);
            return;
        }

        PlayerRole role = parseRole(knownRoleSelect.getSelected());
        User currentUser = App.getInstance().getCurrentUser();
        String myName = currentUser != null ? currentUser.getUsername() : "Player";

        currentTargetUsername = target;
        waitingForInvite = true;
        inviteTimer = 10f;
        sendInviteBtn.setVisible(false);
        cancelInviteBtn.setVisible(true);
        usernameField.setDisabled(true);
        inviteStatusLabel.setText("Sending invite to " + target + "...");
        inviteStatusLabel.setColor(new Color(0.8f, 0.6f, 0.1f, 1f));

        client.sendPacket(new InviteRequestPacket(target, role, myName));
    }

    private void cancelDirectInvite() {
        NetworkClient client = getOrConnectClient();
        if (client != null && client.isConnected()) {
            client.sendPacket(new CancelInvitePacket(currentInviteId, currentTargetUsername));
        }
        waitingForInvite = false;
        resetInvitePanel("Invite cancelled.");
    }

    private void resetInvitePanel(String msg) {
        waitingForInvite = false;
        currentInviteId = null;
        sendInviteBtn.setVisible(true);
        cancelInviteBtn.setVisible(false);
        usernameField.setDisabled(false);
        inviteStatusLabel.setText(msg);
        inviteStatusLabel.setColor(Color.RED);
    }

    private void startRandomMatchmaking() {
        if (waitingForInvite) {
            cancelDirectInvite();
        }
        NetworkClient client = getOrConnectClient();
        if (client == null || !client.isConnected()) {
            randomStatusLabel.setText("Server offline / not connected.");
            randomStatusLabel.setColor(Color.RED);
            return;
        }

        PlayerRole role = parseRole(randomRoleSelect.getSelected());
        User currentUser = App.getInstance().getCurrentUser();
        String myName = currentUser != null ? currentUser.getUsername() : "Player";

        waitingForRandom = true;
        randomStatusLabel.setText("");
        showSearchingOverlay();
        client.sendPacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, role, myName));
    }

    private void cancelRandomMatchmaking() {
        NetworkClient client = getOrConnectClient();
        if (client != null && client.isConnected()) {
            client.sendPacket(new CancelMatchmakingPacket());
        }
        waitingForRandom = false;
        hideSearchingOverlay();
        randomStatusLabel.setText("Matchmaking cancelled.");
        randomStatusLabel.setColor(Color.GRAY);
    }

    private void showSearchingOverlay() {
        if (searchingOverlay != null && searchingOverlay.getStage() != null) {
            return;
        }
        if (getStage() == null) {
            return;
        }

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setName(AbstractMenuScreen.OVERLAY_NAME);
        overlay.setBackground(new TextureRegionDrawable(getWhitePixel()).tint(new Color(0f, 0f, 0f, 0.70f)));
        overlay.setUserObject((Runnable) this::cancelRandomMatchmaking);

        BorderedTable card = new BorderedTable();
        card.pad(28f);

        Label title = new Label("SEARCHING FOR RANDOM OPPONENT", skin, "big");
        title.setColor(Color.BLACK);
        title.setAlignment(Align.center);
        card.add(title).padBottom(12f).row();

        Label hint = new Label("Waiting in the matchmaking queue…", skin, "secondary");
        hint.setColor(new Color(0.3f, 0.25f, 0.2f, 1f));
        hint.setAlignment(Align.center);
        card.add(hint).padBottom(22f).row();

        TextButton cancelBtn = new TextButton("CANCEL QUEUE", skin, "brown");
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cancelRandomMatchmaking();
            }
        });
        card.add(cancelBtn).width(200f).height(50f).center();

        overlay.add(card).width(520f).pad(20f);
        overlay.getColor().a = 0f;
        overlay.addAction(Actions.fadeIn(FADE_IN));

        searchingOverlay = overlay;
        getStage().addActor(overlay);
    }

    private void hideSearchingOverlay() {
        if (searchingOverlay == null) {
            return;
        }
        Table overlay = searchingOverlay;
        searchingOverlay = null;
        overlay.setTouchable(Touchable.disabled);
        overlay.clearActions();
        overlay.addAction(Actions.sequence(
                Actions.fadeOut(FADE_OUT),
                Actions.removeActor()
        ));
    }

    private PlayerRole parseRole(String selected) {
        if ("PLANT".equalsIgnoreCase(selected)) return PlayerRole.PLANT;
        if ("ZOMBIE".equalsIgnoreCase(selected)) return PlayerRole.ZOMBIE;
        return PlayerRole.ANY;
    }

    public void dismiss() {
        if (waitingForInvite) {
            cancelDirectInvite();
        }
        if (waitingForRandom) {
            cancelRandomMatchmaking();
        }
        dismissCleanly(null);
    }

    private void dismissCleanly(Runnable after) {
        unregisterNetworkListeners();
        hideSearchingOverlay();
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
