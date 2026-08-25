package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.CollectionMenuController;
import controller.GameMenuController;
import controller.GameMenuController.ChapterSummary;
import controller.MainMenuController;
import controller.TravelLogMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.MenuType;
import model.quest.Quest;
import pvz.libpvz.textures.TextureBank;
import view.gui.PvzGdxGame;
import view.gui.assets.AdventureHudRegions;
import view.gui.assets.ChapterIslandArt;
import view.gui.assets.PvzAssets;
import view.gui.ui.AtlasImageButton;
import view.gui.ui.ChapterCarousel;
import view.gui.ui.EdgeFadeOverlay;
import view.gui.ui.ResourceBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Adventure menu: multi-chapter carousel with lerp transitions.
 * Art: {@code IMAGE_UI_UNIVERSE_WORLDS_*}. Enter opens that chapter's levels.
 */
public final class AdventureScreen extends AbstractMenuScreen {
    private static final float MAX_DELTA = 1f / 30f;
    private static final float CORNER_PAD = 40f;
    private static final float HUD_ICON = 100f;
    private static final float HUD_GAP = 14f;
    private static final float EDGE_FADE_H = 600f;

    private final GameMenuController controller = GameMenuController.getInstance();
    private final ChapterIslandArt islandArt = new ChapterIslandArt();
    private final MainMenuArt menuArt = new MainMenuArt();
    private EdgeFadeOverlay edgeFade;

    private final List<ChapterSummary> chapters = new ArrayList<>();

    private ChapterCarousel carousel;
    private Label titleLabel;
    private Label progressLabel;
    private TextButton enterButton;
    private ResourceBar resourceBar;

    public AdventureScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    public void show() {
        game.ensureAssets();
        islandArt.ensureLoaded(game.assets.textures);
        menuArt.ensureLoaded(game.assets.textures);
        loadHudAtlases(game.assets.textures);
        if (edgeFade == null) {
            edgeFade = new EdgeFadeOverlay(EDGE_FADE_H);
        }
        super.show();
    }

    private static void loadHudAtlases(TextureBank textures) {
        textures.loadSync(AdventureHudRegions.ATLAS_WORLD_MAP);
        textures.loadSync(AdventureHudRegions.ATLAS_ALWAYS_LOADED);
        textures.loadSync(AdventureHudRegions.ATLAS_GAME_CENTER);
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.GAME);
        chapters.clear();
        int startIndex = 0;

        CommandResult<List<ChapterSummary>> result = controller.listChapters();
        if (!result.isSuccess() || result.getData() == null) {
            showToast(result.getMessage(), true);
        } else {
            chapters.addAll(result.getData());
            for (int i = 0; i < chapters.size(); i++) {
                if (chapters.get(i).unlocked()) {
                    startIndex = i;
                    break;
                }
            }
        }

        Table top = new Table();
        top.setFillParent(true);
        top.setTouchable(Touchable.childrenOnly);
        top.top().right();
        resourceBar = new ResourceBar(skin, game.assets.textures);
        // Match MainHubScreen resource-bar placement (pad 55, top-right).
        top.add(resourceBar).pad(55f);
        stage.addActor(top);

        addCarousel(startIndex);
        addHudIcons();
        addNavButtons();
        addFooter();
        addKeyboardNav();
    }

    private void addHudIcons() {
        TextureBank textures = game.assets.textures;
        float yTop = UI_HEIGHT - CORNER_PAD - HUD_ICON;
        float yBottom = CORNER_PAD;

        // Top-left: Back
        stage.addActor(hudButton(textures,
            AdventureHudRegions.BACK_NORMAL, AdventureHudRegions.BACK_DOWN,
            CORNER_PAD, yTop, this::goBack));

        // Bottom-left stack: Quests, Collection
        stage.addActor(hudButton(textures,
            AdventureHudRegions.QUESTS_NORMAL, AdventureHudRegions.QUESTS_DOWN,
            CORNER_PAD, yBottom + HUD_ICON + HUD_GAP, this::openQuests));
        stage.addActor(hudButton(textures,
            AdventureHudRegions.COLLECTION_NORMAL, AdventureHudRegions.COLLECTION_DOWN,
            CORNER_PAD, yBottom, this::openCollection));

        // Bottom-right stack: Greenhouse, Leaderboard
        float xRight = UI_WIDTH - CORNER_PAD - HUD_ICON;
        stage.addActor(hudButton(textures,
            AdventureHudRegions.GREENHOUSE_NORMAL, AdventureHudRegions.GREENHOUSE_DOWN,
            xRight, yBottom + HUD_ICON + HUD_GAP, this::openGreenhouse));
        stage.addActor(hudButton(textures,
            AdventureHudRegions.LEADERBOARD_NORMAL, AdventureHudRegions.LEADERBOARD_DOWN,
            xRight, yBottom, this::openLeaderboard));

        TextButton scoreGame = new TextButton("Score Game", skin, "purple");
        scoreGame.setSize(200f, 56f);
        scoreGame.setPosition(xRight - 200f - 16f, yBottom);
        scoreGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openScoreGame();
            }
        });
        stage.addActor(scoreGame);
    }

    private AtlasImageButton hudButton(TextureBank textures, String upId, String downId,
                                       float x, float y, Runnable action) {
        TextureRegion up = textures.region(upId);
        TextureRegion down = textures.region(downId);
        AtlasImageButton button = new AtlasImageButton(up, down, HUD_ICON, action);
        button.setPosition(x, y);
        return button;
    }

    private void goBack() {
        CommandResult<Void> r = controller.menuExit();
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new MainHubScreen(game));
        }
    }

    private void openQuests() {
        TravelLogMenuController travel = TravelLogMenuController.getInstance();
        travel.syncForCurrentUser();
        App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);
        CommandResult<List<Quest>> quests = travel.showAllQuests();
        Table list = new Table();
        if (!quests.isSuccess() || quests.getData() == null) {
            list.add(new Label(quests.getMessage(), skin, "medium"));
        } else {
            for (Quest q : quests.getData()) {
                list.add(new Label(q.getName(), skin, "medium")).left().padBottom(6f).row();
            }
        }
        TextButton miniGames = new TextButton("MINI-GAMES", skin, "purple");
        miniGames.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MiniGameScreen(game));
            }
        });
        list.add(miniGames).width(300f).height(58f).padTop(18f).row();
        game.setScreen(new PlaceholderMenuScreen(game, "Quests", MenuType.TRAVEL_LOG, list));
    }

    private void openCollection() {
        App.getInstance().setCurrentMenu(MenuType.COLLECTION);
        CommandResult<List<String>> plants = CollectionMenuController.getInstance().showPlants();
        Table list = new Table();
        if (!plants.isSuccess() || plants.getData() == null) {
            list.add(new Label(plants.getMessage(), skin, "medium"));
        } else {
            for (String name : plants.getData()) {
                list.add(new Label(name, skin, "medium")).left().padBottom(4f).row();
            }
        }
        game.setScreen(new PlaceholderMenuScreen(game, "Collection", MenuType.COLLECTION, list));
    }

    private void openGreenhouse() {
        App.getInstance().setCurrentMenu(MenuType.GREENHOUSE);
        game.setScreen(new GreenhouseScreen(game));
    }

    private void openLeaderboard() {
        game.setScreen(new LeaderboardScreen(game, () -> game.setScreen(new AdventureScreen(game))));
    }

    private void openScoreGame() {
        CommandResult<Void> r = MainMenuController.getInstance().enterScoreGame();
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new LevelObjectivesScreen(game, Chapter.ANCIENT_EGYPT));
        }
    }

    private void addNavButtons() {
        TextButton prev = new TextButton("<", skin, "brown");
        prev.setSize(72f, 72f);
        prev.setPosition(CORNER_PAD, (UI_HEIGHT - 72f) * 0.5f);
        prev.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                move(-1);
            }
        });
        stage.addActor(prev);

        TextButton next = new TextButton(">", skin, "brown");
        next.setSize(72f, 72f);
        next.setPosition(UI_WIDTH - CORNER_PAD - 72f, (UI_HEIGHT - 72f) * 0.5f);
        next.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                move(1);
            }
        });
        stage.addActor(next);
    }

    private void addCarousel(int startIndex) {
        carousel = new ChapterCarousel(game.assets, islandArt);
        carousel.setSize(UI_WIDTH, UI_HEIGHT);
        carousel.setPosition(0f, 0f);
        carousel.setOnSelectionChanged(this::refreshCaptions);
        carousel.setOnActivate(i -> tryEnterSelected());
        carousel.setChapters(chapters, startIndex);
        stage.addActor(carousel);

        titleLabel = new Label("", skin, "big");
        titleLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        progressLabel = new Label("", skin, "medium");
        progressLabel.setAlignment(com.badlogic.gdx.utils.Align.center);

        Table captions = new Table();
        captions.setFillParent(true);
        captions.setTouchable(Touchable.disabled);
        captions.bottom().padBottom(200f);
        captions.add(titleLabel).padBottom(8f).row();
        captions.add(progressLabel);
        stage.addActor(captions);

        refreshCaptions(carousel.selectedIndex());
    }

    private void refreshCaptions(int index) {
        if (chapters.isEmpty() || titleLabel == null) {
            return;
        }
        ChapterSummary summary = chapters.get(index);
        titleLabel.setText(summary.displayName());
        if (summary.unlocked()) {
            progressLabel.setText("Progress " + summary.completedLevels() + " / " + summary.totalLevels());
        } else {
            progressLabel.setText("Locked  ·  Progress "
                + summary.completedLevels() + " / " + summary.totalLevels());
        }
        if (enterButton != null) {
            enterButton.setText(summary.unlocked() ? "Enter" : "Locked");
            enterButton.setDisabled(!summary.unlocked());
        }
    }

    private void addFooter() {
        enterButton = new TextButton("Enter", skin, "purple");
        enterButton.setSize(280f, 72f);
        enterButton.getLabel().setFontScale(1.5f);
        enterButton.setPosition((UI_WIDTH - 280f) * 0.5f, CORNER_PAD + 70f);
        enterButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tryEnterSelected();
            }
        });
        stage.addActor(enterButton);

        TextButton debug = new TextButton("Debug playground", skin, "brown");
        debug.setSize(240f, 52f);
        debug.setPosition(UI_WIDTH - CORNER_PAD - 240f, CORNER_PAD + 2f * (HUD_ICON + HUD_GAP) + 24f);
        debug.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.enterDebugLevel();
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    game.setScreen(new DebugPlaygroundScreen(game));
                }
            }
        });
        stage.addActor(debug);
    }

    private void addKeyboardNav() {
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
                    move(-1);
                    return true;
                }
                if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
                    move(1);
                    return true;
                }
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    tryEnterSelected();
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setInputProcessor(stage);
    }

    private void move(int delta) {
        if (carousel != null) {
            carousel.move(delta);
        }
    }

    private void tryEnterSelected() {
        if (chapters.isEmpty() || carousel == null) {
            return;
        }
        ChapterSummary summary = chapters.get(carousel.selectedIndex());
        if (!summary.unlocked()) {
            showToast("Chapter locked — finish the previous world first.", true);
            return;
        }
        game.setScreen(new ChapterLevelsScreen(game, summary.chapter()));
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
            assets.textures.update();
            game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
            game.batch.begin();
            menuArt.drawBackground(game.batch, assets.textures, UI_WIDTH, UI_HEIGHT);
            if (edgeFade != null) {
                edgeFade.draw(game.batch, UI_WIDTH, UI_HEIGHT);
            }
            game.batch.end();
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void hide() {
        if (edgeFade != null) {
            edgeFade.dispose();
            edgeFade = null;
        }
        super.hide();
    }

    @Override
    public void dispose() {
        if (edgeFade != null) {
            edgeFade.dispose();
            edgeFade = null;
        }
        super.dispose();
    }
}
