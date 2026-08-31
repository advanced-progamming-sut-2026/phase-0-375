package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import controller.RegisterMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.SecurityQuestion;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.assets.UiRegions;
import view.gui.audio.GameAudio;
import view.gui.audio.MusicTracks;
import view.gui.ui.CollectionEntryOverlay;
import view.gui.ui.SkinFonts;
import view.gui.ui.UiMotion;

/**
 * Registration form → {@link RegisterMenuController} (fields + security question).
 */
public final class RegisterScreen extends AbstractMenuScreen {
    private static final float MAX_DELTA = 1f / 30f;
    private static final float FIELD_WIDTH = 360f;
    private static final float FIELD_HEIGHT = 40f;
    private static final float CARD_W = 560f;
    private static final float SECURITY_CARD_W = 780f;
    private static final float SECURITY_FIELD_W = 680f;
    private static final float SECURITY_FIELD_H = 48f;
    private static final float SECURITY_SELECT_H = 64f;
    /** Font scale for security-step labels / fields (1 = skin default). */
    public static float SECURITY_FONT_SCALE = 1.22f;
    private static final float CARD_MAX_HEIGHT = UI_HEIGHT - 240f;
    private static final float LOGO_WIDTH = 520f;
    /** Distance from top of screen to top of logo (larger = lower logo). */
    public static float LOGO_TOP_PAD = 120f;

    private final RegisterMenuController controller = RegisterMenuController.getInstance();
    private final MainMenuArt art = new MainMenuArt();
    private Texture backgroundTex;

    private Table stepSecurity;
    private Cell<Table> contentCell;
    private Cell<ScrollPane> scrollCell;
    private Label registerTitle;
    private BorderedTable card;

    private TextField username;
    private TextField password;
    private TextField passwordConfirm;
    private TextField nickname;
    private TextField email;
    private SelectBox<String> gender;

    private SelectBox<String> question;
    private TextField answer;
    private TextField answerConfirm;

    public RegisterScreen(PvzGdxGame game) {
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
        App.getInstance().setCurrentMenu(MenuType.REGISTER);

        addBackground();
        addLogo();

        stepSecurity = buildSecurityStep();

        card = new BorderedTable();
        card.pad(36f, 40f, 36f, 40f);
        registerTitle = new Label("Register", skin, "big");
        registerTitle.setColor(CollectionEntryOverlay.INK);
        card.add(registerTitle).padBottom(14f).row();
        contentCell = card.add(buildFieldsStep()).growX();
        contentCell.row();

        ScrollPane scroll = new ScrollPane(card, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        Table root = new Table();
        root.setFillParent(true);
        scrollCell = root.add(scroll).width(CARD_W).maxHeight(CARD_MAX_HEIGHT).padTop(40f);
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

    private Table buildFieldsStep() {
        Table t = new Table();
        username = field("Username");
        password = field("Password");
        password.setPasswordMode(true);
        password.setPasswordCharacter('*');
        passwordConfirm = field("Confirm password");
        passwordConfirm.setPasswordMode(true);
        passwordConfirm.setPasswordCharacter('*');
        nickname = field("Nickname");
        email = field("Email");
        gender = new SelectBox<>(skin);
        gender.setItems("male", "female");

        addField(t, username);
        addField(t, password);
        addField(t, passwordConfirm);
        addField(t, nickname);
        addField(t, email);
        t.add(gender).width(FIELD_WIDTH).height(FIELD_HEIGHT).padBottom(10f).row();

        TextButton next = new TextButton("Continue", skin, "purple");
        UiMotion.bindPressScale(next);
        next.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitRegisterFields();
            }
        });
        t.add(next).width(260f).height(52f).padBottom(8f).row();

        TextButton toLogin = new TextButton("I have an account", skin, "brown");
        UiMotion.bindPressScale(toLogin);
        toLogin.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                goToLogin();
            }
        });
        t.add(toLogin).width(260f).height(48f);
        return t;
    }

    private void submitRegisterFields() {
        CommandResult<Void> r = controller.register(
                username.getText(),
                password.getText(),
                passwordConfirm.getText(),
                nickname.getText(),
                email.getText(),
                gender.getSelected());
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            showSecurityStep();
        }
    }

    private void goToLogin() {
        CommandResult<Void> r = controller.menuEnter("login");
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new LoginScreen(game));
        }
    }

    private void showSecurityStep() {
        registerTitle.setText("Security question");
        SkinFonts.scaleLabel(registerTitle, skin, "big", SECURITY_FONT_SCALE);
        contentCell.setActor(stepSecurity);
        if (scrollCell != null) {
            scrollCell.width(SECURITY_CARD_W);
        }
        card.pad(40f, 48f, 40f, 48f);
        card.invalidateHierarchy();
    }

    private Table buildSecurityStep() {
        Table t = new Table();
        t.defaults().left();

        Label hint = scaledLabel(
                "Pick a question you will remember. You will need the answer to reset your password.",
                "secondary", CollectionEntryOverlay.MUTED);
        hint.setWrap(true);
        t.add(hint).width(SECURITY_FIELD_W).padBottom(18f).row();

        Label qLabel = scaledLabel("Question", "medium", CollectionEntryOverlay.INK);
        t.add(qLabel).padBottom(8f).row();

        question = new SelectBox<>(skin);
        Array<String> items = new Array<>();
        SecurityQuestion[] qs = SecurityQuestion.values();
        for (int i = 0; i < qs.length; i++) {
            items.add((i + 1) + ". " + qs[i].getText());
        }
        question.setItems(items);
        t.add(question).width(SECURITY_FIELD_W).height(SECURITY_SELECT_H).padBottom(18f).row();

        Label aLabel = scaledLabel("Your answer", "medium", CollectionEntryOverlay.INK);
        t.add(aLabel).padBottom(8f).row();
        answer = field("Answer");
        t.add(answer).width(SECURITY_FIELD_W).height(SECURITY_FIELD_H).padBottom(14f).row();

        Label cLabel = scaledLabel("Confirm answer", "medium", CollectionEntryOverlay.INK);
        t.add(cLabel).padBottom(8f).row();
        answerConfirm = field("Confirm answer");
        t.add(answerConfirm).width(SECURITY_FIELD_W).height(SECURITY_FIELD_H).padBottom(22f).row();

        TextButton finish = new TextButton("Create account", skin, "purple");
        SkinFonts.scaleButton(finish, skin, "purple", SECURITY_FONT_SCALE);
        UiMotion.bindPressScale(finish);
        finish.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitSecurity();
            }
        });
        t.add(finish).width(300f).height(56f).padTop(4f);
        return t;
    }

    private void submitSecurity() {
        int qNumber = question.getSelectedIndex() + 1;
        CommandResult<Void> r = controller.pickQuestion(
                qNumber, answer.getText(), answerConfirm.getText());
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new LoginScreen(game));
        }
    }

    @Override
    protected void onBack() {
        goToLogin();
    }

    @Override
    protected void onConfirm() {
        if (contentCell != null && contentCell.getActor() == stepSecurity) {
            submitSecurity();
        } else {
            submitRegisterFields();
        }
    }

    private Label scaledLabel(String text, String style, com.badlogic.gdx.graphics.Color color) {
        Label label = new Label(text, skin, style);
        label.setColor(color);
        SkinFonts.scaleLabel(label, skin, style, SECURITY_FONT_SCALE);
        return label;
    }

    private TextField field(String message) {
        TextField f = new TextField("", skin);
        f.setMessageText(message);
        return f;
    }

    private void addField(Table t, TextField field) {
        t.add(field).width(FIELD_WIDTH).height(FIELD_HEIGHT).padBottom(6f).row();
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
