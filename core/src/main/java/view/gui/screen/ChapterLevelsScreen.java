package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import controller.GameMenuController;
import controller.GameMenuController.LevelSummary;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.enums.MenuType;
import model.game.save.GameSaveService;
import pvz.libpvz.textures.TextureBank;
import view.gui.PvzGdxGame;
import view.gui.anim.PamClipCache;
import view.gui.assets.AdventureHudRegions;
import view.gui.assets.PvzAssets;
import view.gui.assets.SeasonMapLayout;
import view.gui.assets.WorldMapArt;
import view.gui.audio.GameAudio;
import view.gui.audio.MusicTracks;
import view.gui.ui.AtlasImageButton;
import view.gui.ui.EdgeFadeOverlay;
import view.gui.ui.LevelEnterOverlay;
import view.gui.ui.ResourceBar;
import view.gui.ui.SeasonWorldMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Season Map: drag-pan islands + neon path + orb markers for one chapter.
 * Tap an unlocked node to enter that level. Decor stays fixed; map pans.
 */
public final class ChapterLevelsScreen extends AbstractMenuScreen {
    private static final float MAX_DELTA = 1f / 30f;
    private static final float CORNER_PAD = 40f;
    private static final float HUD_ICON = 100f;
    private static final float EDGE_FADE_H = 600f;
    /** Instant pan when tapping Left/Right once. */
    public static float KEYBOARD_PAN_STEP = 120f;
    /** Continuous pan speed while holding Left/Right (px/sec). */
    public static float KEYBOARD_PAN_SPEED = 700f;

    /**
     * Ancient Egypt decoration.
     */
    private static final Deco[] EGYPT_DECORATIONS = {
        new Deco(WorldMapArt.DECOR_20,  626.2f,  286.1f, 0.55f), // x=626.2 y=286.1 scale=0.55
        new Deco(WorldMapArt.DECOR_21,  869.5f,  355.1f, 0.55f), // x=869.5 y=355.1 scale=0.55
        new Deco(WorldMapArt.DECOR_22, 1166.0f,  525.2f, 0.55f), // x=1166.0 y=525.2 scale=0.55
        new Deco(WorldMapArt.DECOR_23,  117.2f,  507.6f, 0.55f), // x=117.2 y=507.6 scale=0.55
        new Deco(WorldMapArt.DECOR_24,  578.4f,  587.3f, 0.55f), // x=578.4 y=587.3 scale=0.55
        new Deco(WorldMapArt.DECOR_25, 1406.1f,  514.5f, 0.31f), // x=1406.1 y=514.5 scale=0.31
        new Deco(WorldMapArt.DECOR_34,  192.9f,  256.6f, 0.42f), // x=192.9 y=256.6 scale=0.42
        new Deco(WorldMapArt.DECOR_35, 1780.5f,  301.8f, 0.37f), // x=1780.5 y=301.8 scale=0.37
        new Deco(WorldMapArt.DECOR_37, 1445.0f,  634.9f, 0.55f), // x=1445.0 y=634.9 scale=0.55
        new Deco(WorldMapArt.DECOR_17,  603.5f,  703.2f, 0.55f), // x=603.5 y=703.2 scale=0.55
    };

    /**
     * Frostbite Caves decoration.
     */
    private static final Deco[] FROSTBITE_DECORATIONS = {
        new Deco(WorldMapArt.DECOR_ICE_22, 1265.8f,  628.2f, 1.07f), // x=1265.8 y=628.2 scale=1.07
        new Deco(WorldMapArt.DECOR_ICE_23,  420.0f,  680.0f, 0.55f), // x=420.0 y=680.0 scale=0.55
        new Deco(WorldMapArt.DECOR_ICE_24,  590.2f,  215.4f, 1.43f), // x=590.2 y=215.4 scale=1.43
        new Deco(WorldMapArt.DECOR_ICE_41,  938.6f,  587.9f, 0.55f), // x=938.6 y=587.9 scale=0.55
        new Deco(WorldMapArt.DECOR_ICE_42, 1180.0f,  240.0f, 0.55f), // x=1180.0 y=240.0 scale=0.55
        new Deco(WorldMapArt.DECOR_ICE_47, 1732.6f,  296.9f, 0.80f), // x=1732.6 y=296.9 scale=0.80
    };

    /**
     * Big Wave Beach decoration.
     */
    private static final Deco[] BEACH_DECORATIONS = {
        new Deco(WorldMapArt.DECOR_BEACH_22, 1090.6f,  708.2f, 0.55f), // x=1090.6 y=708.2 scale=0.55
        new Deco(WorldMapArt.DECOR_BEACH_23,  420.0f,  480.0f, 0.55f), // x=420.0 y=480.0 scale=0.55
        new Deco(WorldMapArt.DECOR_BEACH_24,  680.0f,  300.0f, 0.55f), // x=680.0 y=300.0 scale=0.55
        new Deco(WorldMapArt.DECOR_BEACH_27, 1540.5f,  629.1f, 0.55f), // x=1540.5 y=629.1 scale=0.55
        new Deco(WorldMapArt.DECOR_BEACH_28, 1180.0f,  240.0f, 0.55f), // x=1180.0 y=240.0 scale=0.55
        new Deco(WorldMapArt.DECOR_BEACH_37, 1420.0f,  500.0f, 0.55f), // x=1420.0 y=500.0 scale=0.55
        new Deco(WorldMapArt.DECOR_BEACH_41, 1660.0f,  320.0f, 0.55f), // x=1660.0 y=320.0 scale=0.55
        new Deco(WorldMapArt.DECOR_BEACH_42,  520.0f,  640.0f, 0.55f), // x=520.0 y=640.0 scale=0.55
    };

    /**
     * Dark Ages decoration.
     */
    private static final Deco[] DARK_AGES_DECORATIONS = {
        new Deco(WorldMapArt.DECOR_DARK_8,  976.4f,  783.6f, 0.54f), // x=976.4 y=783.6 scale=0.54
        new Deco(WorldMapArt.DECOR_DARK_9,  420.0f,  480.0f, 0.55f), // x=420.0 y=480.0 scale=0.55
        new Deco(WorldMapArt.DECOR_DARK_18, 1794.7f,  298.6f, 0.61f), // x=1794.7 y=298.6 scale=0.61
        new Deco(WorldMapArt.DECOR_DARK_49, 1271.5f,  688.2f, 0.42f), // x=1271.5 y=688.2 scale=0.42
        new Deco(WorldMapArt.DECOR_DARK_56,  593.1f,  677.2f, 0.55f), // x=593.1 y=677.2 scale=0.55
        new Deco(WorldMapArt.DECOR_DARK_DANGER, 1084.6f,  269.4f, 0.34f), // x=1084.6 y=269.4 scale=0.34
    };

    private Deco[] activeDecorations() {
        return switch (chapter) {
            case FROSTBITE_CAVES -> FROSTBITE_DECORATIONS;
            case BIG_WAVE_BEACH -> BEACH_DECORATIONS;
            case DARK_AGES -> DARK_AGES_DECORATIONS;
            default -> EGYPT_DECORATIONS;
        };
    }

    private final GameMenuController controller = GameMenuController.getInstance();
    private final WorldMapArt mapArt = new WorldMapArt();
    private final MainMenuArt menuArt = new MainMenuArt();
    private final Chapter chapter;

    private PamClipCache pamClips;
    private SeasonWorldMap map;
    private EdgeFadeOverlay edgeFade;

    public ChapterLevelsScreen(PvzGdxGame game, Chapter chapter) {
        super(game);
        this.chapter = chapter;
    }

    @Override
    public void show() {
        game.ensureAssets();
        if (game.assets != null) {
            mapArt.configure(chapter);
            mapArt.ensureLoaded(game.assets.textures);
            menuArt.ensureLoaded(game.assets.textures);
            pamClips = new PamClipCache(game.assets.player);
            pamClips.preloadSync(
                    WorldMapArt.ORB_NODE_PAM,
                    WorldMapArt.ORB_GREEN_CLIP,
                    WorldMapArt.ORB_UNLOCK_CLIP,
                    WorldMapArt.ORB_LOCKED_CLIP);
            mapArt.preloadPlatformPams(pamClips);
        }
        if (edgeFade == null) {
            edgeFade = new EdgeFadeOverlay(EDGE_FADE_H);
        }
        GameAudio.get().play(MusicTracks.WORLD_MAP);
        super.show();
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.GAME);

        // Decor first so platforms / nodes always paint above it.
        addStaticDecor();
        addMap();
        addHud();
    }

    private void addStaticDecor() {
        TextureBank textures = game.assets != null ? game.assets.textures : null;
        if (textures == null) {
            return;
        }
        Deco[] list = activeDecorations();
        if (list.length == 0) {
            return;
        }
        for (Deco deco : list) {
            TextureRegion region = textures.region(deco.id);
            if (region == null) {
                continue;
            }
            Image image = new Image(new TextureRegionDrawable(region));
            image.setSize(region.getRegionWidth() * deco.scale, region.getRegionHeight() * deco.scale);
            image.setPosition(deco.x, deco.y);
            image.setTouchable(Touchable.disabled);
            stage.addActor(image);
        }
    }

    private static final class Deco {
        private final String id;
        private final float x;
        private final float y;
        private final float scale;

        private Deco(String id, float x, float y, float scale) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.scale = scale;
        }
    }

    private void addMap() {
        TextureBank textures = game.assets != null ? game.assets.textures : null;
        CommandResult<List<LevelSummary>> result = controller.listLevels(chapter);
        List<LevelSummary> levels = result.isSuccess() ? result.getData() : null;
        if (!result.isSuccess() || levels == null || levels.isEmpty()) {
            showToast(result.getMessage() != null ? result.getMessage() : "No levels found.", true);
            Label empty = new Label("No levels to show", skin, "big");
            empty.setColor(1f, 1f, 1f, 1f);
            empty.setAlignment(Align.center);
            empty.setSize(UI_WIDTH, UI_HEIGHT);
            stage.addActor(empty);
            return;
        }

        SeasonSlots slots = ensureSeasonSlots(levels);
        levels = slots.levels();
        Set<Integer> placeholderLevelIds = slots.placeholderIds();
        int unlockIntroLevelId = consumeUnlockIntroLevelId(levels, placeholderLevelIds);
        map = new SeasonWorldMap(
                textures,
                mapArt,
                SeasonMapLayout.forChapter(chapter),
                skin,
                game.assets != null ? game.assets.player : null,
                pamClips,
                levels,
                unlockIntroLevelId,
                placeholderLevelIds,
                this::onNodeTapped);

        Group viewport = new Group();
        viewport.setSize(UI_WIDTH, UI_HEIGHT);
        viewport.setPosition(0f, 0f);
        viewport.setTouchable(Touchable.childrenOnly);
        viewport.addActor(map);
        map.setTouchable(Touchable.enabled);
        map.addListener(new DragListener() {
            {
                setTapSquareSize(8f);
            }

            @Override
            public void drag(InputEvent event, float x, float y, int pointer) {
                float minX = UI_WIDTH - map.getWidth();
                if (minX > 0f) {
                    minX = 0f;
                }
                map.setX(MathUtils.clamp(map.getX() + getDeltaX(), minX, 0f));
            }
        });
        stage.addActor(viewport);

        int focusId = levels.get(0).levelId();
        for (LevelSummary s : levels) {
            if (placeholderLevelIds.contains(s.levelId())) {
                continue;
            }
            if (s.unlocked() && !s.completed()) {
                focusId = s.levelId();
                break;
            }
        }
        float cx = map.nodeCenterX(focusId);
        if (cx >= 0f) {
            float minX = UI_WIDTH - map.getWidth();
            if (minX > 0f) {
                minX = 0f;
            }
            map.setX(MathUtils.clamp(UI_WIDTH * 0.5f - cx, minX, 0f));
        }
    }

    private void addHud() {
        Table top = new Table();
        top.setFillParent(true);
        top.setTouchable(Touchable.childrenOnly);
        top.top().right();
        top.add(new ResourceBar(skin, game.assets != null ? game.assets.textures : null)).pad(55f);
        stage.addActor(top);

        TextureBank textures = game.assets != null ? game.assets.textures : null;
        if (textures == null) {
            return;
        }
        TextureRegion up = textures.region(AdventureHudRegions.BACK_NORMAL);
        TextureRegion down = textures.region(AdventureHudRegions.BACK_DOWN);
        AtlasImageButton back = new AtlasImageButton(up, down, HUD_ICON, this::goBack);
        back.setPosition(CORNER_PAD, UI_HEIGHT - CORNER_PAD - HUD_ICON);
        stage.addActor(back);

        Label title = new Label(chapter.name().replace('_', ' '), skin, "big");
        title.setAlignment(Align.center);
        title.setColor(1f, 1f, 1f, 1f);
        title.setPosition(0f, UI_HEIGHT - 100f);
        title.setSize(UI_WIDTH, 48f);
        title.setTouchable(Touchable.disabled);
        stage.addActor(title);
    }

    private void onNodeTapped(int levelId) {
        CommandResult<List<LevelSummary>> listed = controller.listLevels(chapter);
        if (!listed.isSuccess() || listed.getData() == null) {
            showToast(listed.getMessage(), true);
            return;
        }
        LevelSummary match = null;
        for (LevelSummary s : listed.getData()) {
            if (s.levelId() == levelId) {
                match = s;
                break;
            }
        }
        if (match == null) {
            // Padded map stub (chapter has no real level 5 in levels.json yet).
            if (levelId == SeasonWorldMap.PLACEHOLDER_LEVEL_ID) {
                showToast("This level is not implemented yet.", true);
            } else {
                showToast("Level not found.", true);
            }
            return;
        }
        if (!match.unlocked()) {
            showToast("Level locked — finish the previous one first.", true);
            return;
        }
        String seasonName = chapter.name().replace('_', ' ');
        LevelEnterOverlay.show(
                stage,
                skin,
                game.assets != null ? game.assets.textures : null,
                seasonName,
                levelId,
                () -> startLevel(levelId));
    }

    /**
     * Pad the chapter list to {@link SeasonWorldMap#SLOT_COUNT} nodes so the map
     * always shows the future level-5 platform. Only chapters missing a real level 5
     * get a placeholder stub (locked look + “not implemented” on tap).
     */
    private static SeasonSlots ensureSeasonSlots(List<LevelSummary> fromController) {
        List<LevelSummary> out = new ArrayList<>(fromController);
        Set<Integer> placeholders = new HashSet<>();
        boolean hasFive = false;
        boolean level4Done = false;
        for (LevelSummary s : out) {
            if (s.levelId() == SeasonWorldMap.PLACEHOLDER_LEVEL_ID) {
                hasFive = true;
            }
            if (s.levelId() == 4 && s.completed()) {
                level4Done = true;
            }
        }
        if (!hasFive && out.size() < SeasonWorldMap.SLOT_COUNT) {
            // Normal unlock rule: available after level 4 is cleared — still not enterable.
            out.add(new LevelSummary(
                    SeasonWorldMap.PLACEHOLDER_LEVEL_ID,
                    LevelType.NORMAL,
                    level4Done,
                    false));
            placeholders.add(SeasonWorldMap.PLACEHOLDER_LEVEL_ID);
        }
        out.sort((a, b) -> Integer.compare(a.levelId(), b.levelId()));
        return new SeasonSlots(out, placeholders);
    }

    /**
     * First visit to a newly unlocked next-playable node plays {@code unlocked_animation}
     * once; later visits stay on idle {@code unlocked}. Skips padded placeholders.
     */
    private int consumeUnlockIntroLevelId(List<LevelSummary> levels, Set<Integer> placeholderLevelIds) {
        Set<Integer> placeholders = placeholderLevelIds != null
                ? placeholderLevelIds
                : Collections.emptySet();
        for (LevelSummary s : levels) {
            if (placeholders.contains(s.levelId())) {
                continue;
            }
            if (!s.unlocked() || s.completed()) {
                continue;
            }
            Preferences prefs = Gdx.app.getPreferences("pvz-worldmap-unlock-fx");
            String key = chapter.name() + "_" + s.levelId();
            if (!prefs.getBoolean(key, false)) {
                prefs.putBoolean(key, true);
                prefs.flush();
                return s.levelId();
            }
            return -1;
        }
        return -1;
    }

    private record SeasonSlots(List<LevelSummary> levels, Set<Integer> placeholderIds) {}

    private void startLevel(int levelId) {
        if (GameSaveService.getInstance().hasSaveForAdventure(chapter, levelId)) {
            try {
                GameSaveService.getInstance().resumeSavedGame();
                game.setScreen(new GameplayScreen(game));
                return;
            } catch (Exception e) {
                GameSaveService.getInstance().clearCurrentUserSave();
                showToast("Could not resume save; starting fresh.", true);
            }
        }
        String chapterArg = chapter.name().toLowerCase(Locale.ROOT);
        CommandResult<Void> r = controller.enterChapter(chapterArg, levelId);
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new LevelObjectivesScreen(game, chapter));
        }
    }

    private void goBack() {
        game.setScreen(new AdventureScreen(game));
    }

    @Override
    protected void onBack() {
        goBack();
    }

    @Override
    protected void onLeft() {
        panMap(KEYBOARD_PAN_STEP);
    }

    @Override
    protected void onRight() {
        panMap(-KEYBOARD_PAN_STEP);
    }

    private void panMap(float dx) {
        if (map == null) {
            return;
        }
        float minX = UI_WIDTH - map.getWidth();
        if (minX > 0f) {
            minX = 0f;
        }
        map.setX(MathUtils.clamp(map.getX() + dx, minX, 0f));
    }

    @Override
    public void render(float delta) {
        if (delta > MAX_DELTA) {
            delta = MAX_DELTA;
        }
        if (map != null) {
            float hold = KEYBOARD_PAN_SPEED * delta;
            if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT)
                    || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) {
                panMap(hold);
            }
            if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT)
                    || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
                panMap(-hold);
            }
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
