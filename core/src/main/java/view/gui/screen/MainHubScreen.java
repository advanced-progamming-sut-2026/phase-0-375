package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import controller.MainMenuController;
import controller.NewsMenuController;
import controller.ProfileMenuController;
import controller.SettingsMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import pvz.libpvz.textures.TextureBank;
import view.gui.PvzGdxGame;
import view.gui.assets.PvzAssets;
import view.gui.assets.UiRegions;
import view.gui.ui.ModalCard;
import view.gui.ui.NewsOverlay;
import view.gui.ui.ProfileOverlay;
import view.gui.ui.ResourceBar;
import view.gui.ui.SettingsOverlay;
import view.gui.ui.SkinIconButton;
import view.gui.ui.ZarrabiIntro;

/**
 * PvZ2-style main hub: cosmic background, brown icon buttons, PLAY → adventure.
 */
public final class MainHubScreen extends AbstractMenuScreen {
    private static final float MAX_DELTA = 1f / 30f;
    private static final float CORNER_PAD = 55f;
    private static final float ICON_SIZE = 90f;
    private static final float PLAY_WIDTH = 250f;
    private static final float PLAY_HEIGHT = 85f;
    private static final float LOGO_WIDTH = 720f;
    private static final float PLAY_FONT_SCALE = 1.7f;
    /** Play Zarrabi fly-in once per app session when hub opens while logged in. */
    private static boolean zarrabiPlayedThisSession;
    /** Pulsing "gone" message when Zarrabi already visited this session. */
    public static float ZARRABI_GONE_FADE_SEC = 0.85f;
    public static float ZARRABI_GONE_HOLD_SEC = 0.35f;
    public static float ZARRABI_GONE_Y = 420f;

    private final MainMenuController controller = MainMenuController.getInstance();
    private final MainMenuArt art = new MainMenuArt();
    private ZarrabiIntro zarrabiIntro;

    private SkinIconButton newsButton;
    private ResourceBar resourceBar;

    public MainHubScreen(PvzGdxGame game) {
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
        App.getInstance().setCurrentMenu(MenuType.MAIN);
        TextureBank textures = game.assets.textures;

        Table top = new Table();
        top.setFillParent(true);
        top.top().right();
        resourceBar = new ResourceBar(skin, textures);
        top.add(resourceBar).pad(CORNER_PAD);
        stage.addActor(top);

        addLogo(textures);
        addPlayButton();
        addCornerIcons(textures);
        addLogoutButton();
        playZarrabiIntroIfNeeded();
    }

    private void playZarrabiIntroIfNeeded() {
        if (zarrabiPlayedThisSession) {
            showZarrabiGoneMessage();
            return;
        }
        zarrabiPlayedThisSession = true;
        if (zarrabiIntro != null) {
            zarrabiIntro.dispose();
        }
        zarrabiIntro = new ZarrabiIntro();
        FileHandle root = game.assets != null ? game.assets.root : null;
        Group portrait = zarrabiIntro.play(UI_WIDTH, UI_HEIGHT, root);
        if (portrait != null) {
            stage.addActor(portrait);
            portrait.toFront();
            toast.toFront();
        }
    }

    private void showZarrabiGoneMessage() {
        Label label = new Label("Zarrabi's just gone :((", skin, "big");
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        label.pack();
        label.setPosition((UI_WIDTH - label.getWidth()) * 0.5f, ZARRABI_GONE_Y);
        label.getColor().a = 0f;
        label.addAction(Actions.forever(Actions.sequence(
                Actions.fadeIn(ZARRABI_GONE_FADE_SEC, Interpolation.sine),
                Actions.delay(ZARRABI_GONE_HOLD_SEC),
                Actions.fadeOut(ZARRABI_GONE_FADE_SEC, Interpolation.sine),
                Actions.delay(ZARRABI_GONE_HOLD_SEC)
        )));
        stage.addActor(label);
        label.toFront();
        toast.toFront();
    }

    private void addLogo(TextureBank textures) {
        TextureRegion logoRegion = art.region(textures, UiRegions.LOGO);
        if (logoRegion == null) {
            return;
        }
        Image logo = new Image(new TextureRegionDrawable(logoRegion));
        float aspect = logoRegion.getRegionHeight() / (float) logoRegion.getRegionWidth();
        float logoHeight = LOGO_WIDTH * aspect;
        logo.setSize(LOGO_WIDTH, logoHeight);
        logo.setPosition((UI_WIDTH - LOGO_WIDTH) * 0.5f, UI_HEIGHT - logoHeight - 80f);
        stage.addActor(logo);
    }

    private void addPlayButton() {
        TextButton play = new TextButton("PLAY", skin, "purple");
        play.setSize(PLAY_WIDTH, PLAY_HEIGHT);
        play.setPosition((UI_WIDTH - PLAY_WIDTH) * 0.5f, CORNER_PAD);
        play.getLabel().setFontScale(PLAY_FONT_SCALE);
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                enterAdventure();
            }
        });
        stage.addActor(play);
    }

    private void addCornerIcons(TextureBank textures) {
        float y = CORNER_PAD;
        float gap = 20f;

        SkinIconButton profile = iconButton(textures, UiRegions.PROFILE_ICON, 0.7f, this::enterProfile);
        profile.setPosition(CORNER_PAD, y);
        stage.addActor(profile);

        // iconScale > 1 → news icon larger than brown background (pad alone cannot do this).
        newsButton = iconButton(textures, UiRegions.NEWS_ICON, 1.35f, this::openNewsOverlay);
        newsButton.setBadge(NewsMenuController.getInstance().countUnread());
        newsButton.setPosition(CORNER_PAD + ICON_SIZE + gap, y);
        stage.addActor(newsButton);

        SkinIconButton settings = iconButton(textures, UiRegions.SETTINGS_ICON, this::enterSettings);
        settings.setPosition(UI_WIDTH - CORNER_PAD - ICON_SIZE, y);
        stage.addActor(settings);
    }

    private SkinIconButton iconButton(TextureBank textures, String regionId, Runnable action) {
        return iconButton(textures, regionId, SkinIconButton.DEFAULT_ICON_SCALE, action);
    }

    private SkinIconButton iconButton(TextureBank textures, String regionId, float iconScale,
                                      Runnable action) {
        TextureRegion icon = art.region(textures, regionId);
        return new SkinIconButton(skin, icon, ICON_SIZE, iconScale, action);
    }

    private void addLogoutButton() {
        TextButton logout = new TextButton("Logout", skin, "brown");
        logout.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Table overlay = ModalCard.confirm(
                        skin,
                        "Logout",
                        "Are you sure you want to log out?",
                        "Logout",
                        () -> {
                            CommandResult<Void> r = controller.logout();
                            showToast(r.getMessage(), !r.isSuccess());
                            if (r.isSuccess()) {
                                game.setScreen(new RegisterScreen(game));
                            }
                        });
                stage.addActor(overlay);
                toast.toFront();
            }
        });
        logout.setSize(160f, 52f);
        logout.setPosition(CORNER_PAD, UI_HEIGHT - 95f);
        stage.addActor(logout);
    }

    private void enterAdventure() {
        CommandResult<Void> r = controller.menuEnter("game");
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new AdventureScreen(game));
        }
    }

    private void enterProfile() {
        CommandResult<Void> r = controller.menuEnter("profile");
        showToast(r.getMessage(), !r.isSuccess());
        if (!r.isSuccess()) {
            return;
        }
        Table overlay = ProfileOverlay.create(
                skin,
                this::showToast,
                () -> {
                    CommandResult<Void> exit = ProfileMenuController.getInstance().menuExit();
                    if (!exit.isSuccess()) {
                        showToast(exit.getMessage(), true);
                    }
                },
                () -> resourceBar.refresh());
        stage.addActor(overlay);
        toast.toFront();
    }

    private void enterSettings() {
        CommandResult<Void> r = controller.menuEnter("settings");
        showToast(r.getMessage(), !r.isSuccess());
        if (!r.isSuccess()) {
            return;
        }
        Table overlay = SettingsOverlay.create(
                skin,
                this::showToast,
                () -> {
                    CommandResult<Void> exit = SettingsMenuController.getInstance().menuExit();
                    if (!exit.isSuccess()) {
                        showToast(exit.getMessage(), true);
                    }
                },
                () -> resourceBar.refresh());
        stage.addActor(overlay);
        toast.toFront();
    }

    private void openNewsOverlay() {
        CommandResult<Void> r = controller.menuEnter("news");
        if (!r.isSuccess()) {
            showToast(r.getMessage(), true);
            return;
        }

        Table overlay = NewsOverlay.create(skin, () -> {
            CommandResult<Void> exit = NewsMenuController.getInstance().menuExit();
            if (!exit.isSuccess()) {
                showToast(exit.getMessage(), true);
            }
            refreshNewsBadge();
            resourceBar.refresh();
        });
        stage.addActor(overlay);
        toast.toFront();
    }

    private void refreshNewsBadge() {
        if (newsButton != null) {
            newsButton.setBadge(NewsMenuController.getInstance().countUnread());
        }
    }

    @Override
    public void render(float delta) {
        if (delta > MAX_DELTA) {
            delta = MAX_DELTA;
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        PvzAssets assets = game.assets;
        if (assets != null) {
            game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
            game.batch.begin();
            art.drawBackground(game.batch, assets.textures, UI_WIDTH, UI_HEIGHT);
            game.batch.end();
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        if (zarrabiIntro != null) {
            zarrabiIntro.dispose();
            zarrabiIntro = null;
        }
        super.dispose();
    }
}
