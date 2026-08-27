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
import view.gui.assets.UiRegions;
import view.gui.audio.GameAudio;
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

    // Forgot-password modal state
    private int resetStep; // 0 idle, 1 answer, 2 new password
    private Label resetPrompt;
    private TextField resetField;
    private TextButton resetAction;

    public LoginScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    public void show() {
        game.ensureAssets();
        art.ensureLoaded(game.assets.textures);
        super.show();
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.LOGIN);

        addBackground();
        addLogo();

        BorderedTable card = new BorderedTable();
        card.pad(44f, 48f, 44f, 48f);
        Label title = new Label("Login", skin, "big");
        title.setColor(CollectionEntryOverlay.INK);
        card.add(title).padBottom(18f).row();

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

        TextButton login = new TextButton("Login", skin, "purple");
        UiMotion.bindPressScale(login);
        login.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitLogin();
            }
        });
        card.add(login).width(260f).height(52f).padBottom(8f).row();

        TextButton forgot = new TextButton("Forgot password", skin, "brown");
        UiMotion.bindPressScale(forgot);
        forgot.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openForgotModal();
            }
        });
        card.add(forgot).width(260f).height(48f).padBottom(8f).row();

        TextButton toRegister = new TextButton("Create account", skin, "brown");
        UiMotion.bindPressScale(toRegister);
        toRegister.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.menuExit();
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new RegisterScreen(game));
                }
            }
        });
        card.add(toRegister).width(260f).height(48f);

        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(520f).padTop(40f);
        stage.addActor(root);
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
        FileHandle local = Gdx.files.local("assets/" + MainMenuArt.AUTH_BG_RELATIVE);
        return local.exists() ? local : Gdx.files.local(MainMenuArt.AUTH_BG_RELATIVE);
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

    private void openForgotModal() {
        resetStep = 0;
        Table body = new Table();
        TextField userField = new TextField(username.getText(), skin);
        userField.setMessageText("Username");
        TextField emailField = new TextField("", skin);
        emailField.setMessageText("Email");
        Label questionHeading = new Label("Security question:", skin, "medium");
        questionHeading.setColor(CollectionEntryOverlay.MUTED);
        resetPrompt = new Label("Enter username and email to begin reset.", skin);
        resetPrompt.setWrap(true);
        resetPrompt.setColor(CollectionEntryOverlay.INK);
        resetField = new TextField("", skin);
        resetAction = new TextButton("Continue", skin, "purple");

        Runnable showIdentityStep = () -> {
            body.clearChildren();
            resetPrompt.setFontScale(1f);
            body.add(resetPrompt).width(400f).left().padBottom(12f).row();
            body.add(userField).width(400f).height(48f).padBottom(8f).row();
            body.add(emailField).width(400f).height(48f).padBottom(8f).row();
            body.add(resetAction).width(220f).height(56f);
            body.invalidateHierarchy();
        };
        Runnable showAnswerStep = () -> {
            body.clearChildren();
            body.add(questionHeading).width(400f).left().padBottom(6f).row();
            body.add(resetPrompt).width(400f).left().padBottom(14f).row();
            body.add(resetField).width(400f).height(48f).padBottom(12f).row();
            body.add(resetAction).width(220f).height(56f);
            body.invalidateHierarchy();
        };
        Runnable showPasswordStep = () -> {
            body.clearChildren();
            body.add(resetPrompt).width(400f).left().padBottom(14f).row();
            body.add(resetField).width(400f).height(48f).padBottom(12f).row();
            body.add(resetAction).width(220f).height(56f);
            body.invalidateHierarchy();
        };

        showIdentityStep.run();

        Table overlay = ModalCard.create(skin, "Forgot password", body, () -> resetStep = 0);

        resetAction.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (resetStep == 0) {
                    CommandResult<Void> r = controller.forgetPassword(
                            userField.getText(), emailField.getText());
                    showToast(r.getMessage(), !r.isSuccess());
                    if (!r.isSuccess()) {
                        return;
                    }
                    resetStep = 1;
                    resetPrompt.setText(r.getMessage());
                    resetPrompt.setFontScale(1.28f);
                    resetField.setPasswordMode(false);
                    resetField.setText("");
                    resetField.setMessageText("Your answer");
                    resetAction.setText("Submit answer");
                    showAnswerStep.run();
                } else if (resetStep == 1) {
                    CommandResult<Void> r = controller.answer(resetField.getText());
                    showToast(r.getMessage(), !r.isSuccess());
                    if (!r.isSuccess()) {
                        overlay.remove();
                        resetStep = 0;
                        return;
                    }
                    resetStep = 2;
                    resetPrompt.setText(r.getMessage());
                    resetPrompt.setFontScale(1f);
                    resetField.setText("");
                    resetField.setPasswordMode(true);
                    resetField.setPasswordCharacter('*');
                    resetField.setMessageText("New password");
                    resetAction.setText("Update password");
                    showPasswordStep.run();
                } else {
                    CommandResult<Void> r = controller.resetPassword(resetField.getText());
                    showToast(r.getMessage(), !r.isSuccess());
                    if (r.isSuccess()) {
                        overlay.remove();
                        resetStep = 0;
                    }
                }
            }
        });

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
