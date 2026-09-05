package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import controller.LoginMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.assets.PvzAssets;
import view.gui.assets.UiRegions;
import view.gui.audio.GameAudio;
import view.gui.audio.MusicTracks;
import view.gui.ui.CollectionEntryOverlay;
import view.gui.ui.ModalCard;
import view.gui.ui.UiMotion;

/**
 * Login form → {@link LoginMenuController}. Errors via toast; forgot-password as modal.
 */
public final class LoginScreen extends AbstractMenuScreen {
    private static final float MAX_DELTA = 1f / 30f;
    private static final float LOGO_WIDTH = 520f;
    /** Distance from top of screen to top of logo (larger = lower logo). */
    public static float LOGO_TOP_PAD = 120f;

    private final LoginMenuController controller = LoginMenuController.getInstance();
    private final MainMenuArt art = new MainMenuArt();
    private Texture backgroundTex;

    private TextField username;
    private TextField password;
    private CheckBox stayLoggedIn;

    public LoginScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    public void show() {
        game.ensureAssets();
        art.ensureLoaded(game.assets.textures);
        GameAudio.get().play(MusicTracks.TITLE);
        super.show();
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.LOGIN);
        addBackground();
        addLogo();
        BorderedTable card = loginCard();
        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(520f).padTop(40f);
        stage.addActor(root);
    }

    private BorderedTable loginCard() {
        BorderedTable card = new BorderedTable();
        card.pad(44f, 48f, 44f, 48f);
        Label title = new Label("Login", skin, "big");
        title.setColor(CollectionEntryOverlay.INK);
        card.add(title).padBottom(18f).row();
        addCredentialFields(card);
        card.add(actionButton("Login", "purple", this::submitLogin))
                .width(260f).height(52f).padBottom(8f).row();
        card.add(actionButton("Forgot password", "brown", this::openForgotModal))
                .width(260f).height(48f).padBottom(8f).row();
        card.add(actionButton("Create account", "brown", this::goRegister))
                .width(260f).height(48f);
        return card;
    }

    private void addCredentialFields(BorderedTable card) {
        username = new TextField("", skin);
        username.setMessageText("Username");
        password = new TextField("", skin);
        password.setMessageText("Password");
        password.setPasswordMode(true);
        password.setPasswordCharacter('*');
        stayLoggedIn = new CheckBox(" Stay logged in", skin);
        stayLoggedIn.getLabel().setColor(CollectionEntryOverlay.MUTED);
        card.add(username).width(360f).height(40f).padBottom(10f).row();
        card.add(password).width(360f).height(40f).padBottom(10f).row();
        card.add(stayLoggedIn).left().padBottom(16f).row();
    }

    private TextButton actionButton(String text, String style, Runnable action) {
        TextButton button = new TextButton(text, skin, style);
        UiMotion.bindPressScale(button);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private void goRegister() {
        CommandResult<Void> r = controller.menuExit();
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new RegisterScreen(game));
        }
    }

    private void addBackground() {
        FileHandle file = resolveBackground();
        if (file == null || !file.exists()) {
            return;
        }
        if (backgroundTex != null) {
            backgroundTex.dispose();
        }
        backgroundTex = new Texture(file);
        backgroundTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image bg = new Image(new TextureRegionDrawable(backgroundTex));
        bg.setScaling(Scaling.fill);
        bg.setFillParent(true);
        bg.setTouchable(Touchable.disabled);
        stage.addActor(bg);
    }

    private FileHandle resolveBackground() {
        if (game.assets != null && game.assets.root != null) {
            FileHandle fromRoot = game.assets.root.child(MainMenuArt.AUTH_BG_RELATIVE);
            if (fromRoot.exists()) {
                return fromRoot;
            }
        }
        return PvzAssets.resolveAsset(MainMenuArt.AUTH_BG_RELATIVE);
    }

    private void addLogo() {
        TextureBank textures = game.assets != null ? game.assets.textures : null;
        if (textures == null) {
            return;
        }
        TextureRegion logoRegion = art.region(textures, UiRegions.LOGO);
        if (logoRegion == null) {
            return;
        }
        Image logo = new Image(new TextureRegionDrawable(logoRegion));
        float aspect = logoRegion.getRegionHeight() / (float) logoRegion.getRegionWidth();
        float logoHeight = LOGO_WIDTH * aspect;
        logo.setSize(LOGO_WIDTH, logoHeight);
        logo.setPosition((UI_WIDTH - LOGO_WIDTH) * 0.5f, UI_HEIGHT - logoHeight - LOGO_TOP_PAD);
        stage.addActor(logo);
    }

    private void submitLogin() {
        CommandResult<Void> result = controller.login(
                username.getText(),
                password.getText(),
                stayLoggedIn.isChecked());
        showToast(result.getMessage(), !result.isSuccess());
        if (result.isSuccess()) {
            GameAudio.get().syncFromUser();
            game.setScreen(new MainHubScreen(game));
        }
    }

    @Override
    protected void onConfirm() {
        if (hasOverlay()) {
            return;
        }
        submitLogin();
    }

    private boolean hasOverlay() {
        var actors = stage.getActors();
        for (int i = actors.size - 1; i >= 0; i--) {
            if (OVERLAY_NAME.equals(actors.get(i).getName())) {
                return true;
            }
        }
        return false;
    }

    private void openForgotModal() {
        LoginForgotFlow flow = new LoginForgotFlow(skin, controller, username.getText());
        Table overlay = ModalCard.create(skin, "Forgot password", flow.body(), flow::reset);
        flow.actionButton().addListener(flow.listener(overlay, this::showToast));
        stage.addActor(overlay);
        toast.toFront();
    }

    @Override
    public void render(float delta) {
        if (delta > MAX_DELTA) {
            delta = MAX_DELTA;
        }
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        if (backgroundTex != null) {
            backgroundTex.dispose();
            backgroundTex = null;
        }
        super.dispose();
    }
}
