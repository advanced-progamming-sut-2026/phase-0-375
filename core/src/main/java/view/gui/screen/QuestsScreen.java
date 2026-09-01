package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import controller.TravelLogMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.data.minigame.MiniGameDataEntry;
import model.enums.MiniGameType;
import model.game.save.GameSaveService;
import model.enums.MenuType;
import model.enums.QuestCategory;
import model.quest.Quest;
import model.quest.QuestProgress;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.audio.GameAudio;
import view.gui.anim.PamClipCache;
import view.gui.assets.AdventureHudRegions;
import view.gui.assets.PvzAssets;
import view.gui.assets.QuestArt;
import view.gui.assets.ShopArt;
import view.gui.ui.AtlasImageButton;
import view.gui.ui.EdgeFadeOverlay;
import view.gui.ui.IZombieMatchmakingOverlay;
import view.gui.ui.ResourceBar;
import view.gui.ui.RoundedRegionImage;

import java.util.List;
import java.util.Locale;

/**
 * Travel-log quests UI: Daily / Main / Epic / Mini-Games tabs with claimable cards.
 * Panel chrome matches {@link CollectionScreen} (top rim only + brown gradient).
 */
public final class QuestsScreen extends AbstractMenuScreen {
    public enum Tab {
        DAILY, MAIN, EPIC, MINI_GAMES
    }

    private static final float MAX_DELTA = 1f / 30f;
    private static final float EDGE_FADE_H = 600f;
    private static final float EDGE = 24f;
    private static final float TOP_RESERVE = 180f;
    private static final float FRAME_BLEED = 48f;
    private static final float FRAME = 17f;
    private static final float CORNER = 26f;
    private static final float CLOSE_SIZE = 76f;
    private static final float CLOSE_SHIFT_X = -90f;
    private static final float CLOSE_SHIFT_Y = -83f;

    private static final float TAB_SLOT_W = QuestTabMarker.SLOT_W;
    private static final float TAB_SLOT_H = QuestTabMarker.SLOT_H;
    private static final float TAB_GAP = 8f;
    private static final float TAB_SHIFT_X = 45f;
    private static final float TAB_SHIFT_Y = 43f;
    private static final Color STATUS_INK = new Color(0.88f, 0.82f, 0.68f, 1f);
    private static final Color GRAD_TOP = new Color(0.30f, 0.16f, 0.07f, 1f);
    private static final Color GRAD_BOTTOM = new Color(0.12f, 0.05f, 0.02f, 1f);

    private final TravelLogMenuController controller = TravelLogMenuController.getInstance();
    private final MainMenuArt menuArt = new MainMenuArt();
    private final QuestArt questArt = new QuestArt();
    private PamClipCache pamClips;
    private EdgeFadeOverlay edgeFade;
    private Texture boardGradient;
    private Texture whitePixel;
    /** Owned bake of {@link QuestArt#ICON_LAWN_ROW} rotated 90° (One Column Less). */
    private Texture lawnColumnTex;
    private TextureRegion lawnColumnRegion;
    private TextureBank textures;

    private Tab tab = Tab.DAILY;
    private Table list;
    private Label statusLabel;
    private ResourceBar resources;
    private final Array<QuestTabMarker> tabMarkers = new Array<>();
    private QuestCardFactory cards;

    public QuestsScreen(PvzGdxGame game) {
        super(game);
    }

    public QuestsScreen(PvzGdxGame game, Tab startTab) {
        super(game);
        this.tab = startTab == null ? Tab.DAILY : startTab;
    }

    @Override
    public void show() {
        game.ensureAssets();
        menuArt.ensureLoaded(game.assets.textures);
        questArt.ensureLoaded(game);
        if (edgeFade == null) {
            edgeFade = new EdgeFadeOverlay(EDGE_FADE_H);
        }
        super.show();
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);
        controller.syncForCurrentUser();
        textures = game.assets.textures;
        loadQuestAtlases(textures);
        ensureLawnColumnIcon();
        pamClips = new PamClipCache(game.assets.player);
        pamClips.preloadSync(QuestArt.PAM_DAILY_CLOCK, QuestArt.PAM_CLIP);
        if (whitePixel == null) {
            whitePixel = solidPixel(Color.WHITE);
        }
        cards = new QuestCardFactory(skin, textures, whitePixel, lawnColumnRegion, game, pamClips);
        float visibleH = UI_HEIGHT - TOP_RESERVE;
        Table top = addResourceBar();
        stage.addActor(buildBoardPanel(visibleH));
        addTabs(visibleH);
        AtlasImageButton close = addCloseButton();
        top.toFront();
        close.toFront();
        for (QuestTabMarker marker : tabMarkers) {
            marker.toFront();
        }
        refresh();
    }

    private void loadQuestAtlases(TextureBank t) {
        t.loadSync(ShopArt.ATLAS_STORE);
        t.loadSync(ShopArt.ATLAS_SEEDS);
        t.loadSync("ATLASIMAGE_ATLAS_UI_SEEDPACKETS_768_00");
        t.loadSync(AdventureHudRegions.ATLAS_ALWAYS_LOADED);
        t.loadSync("ATLASIMAGE_ATLAS_UI_ALWAYSLOADED_768_01");
        t.loadSync("UI_VaseBreakerMenu_768");
        t.loadSync("ATLASIMAGE_ATLAS_UI_VASEBREAKERMENU_768_00");
        t.loadSync(QuestArt.ATLAS);
        t.loadSync(QuestArt.ATLAS_IMAGE);
        t.loadSync(QuestArt.ATLAS_FRONT_LAWN);
    }

    private Table addResourceBar() {
        Table top = new Table();
        top.setFillParent(true);
        top.setTouchable(Touchable.childrenOnly);
        top.top().right();
        resources = new ResourceBar(skin, textures);
        top.add(resources).pad(EDGE);
        stage.addActor(top);
        return top;
    }

    private Group buildBoardPanel(float visibleH) {
        float panelW = UI_WIDTH + FRAME_BLEED * 2f;
        float panelH = visibleH + FRAME_BLEED;
        Group panel = new Group();
        panel.setSize(panelW, panelH);
        panel.setPosition(-FRAME_BLEED, -FRAME_BLEED);
        BorderedTable frame = new BorderedTable();
        frame.setBounds(0f, 0f, panelW, panelH);
        panel.addActor(frame);
        boardGradient = verticalGradient(GRAD_TOP, GRAD_BOTTOM, 8);
        RoundedRegionImage inside = new RoundedRegionImage(
                new TextureRegion(boardGradient), CORNER, true, true, false, false);
        inside.setBounds(FRAME, FRAME, panelW - FRAME * 2f, panelH - FRAME * 2f);
        inside.setTouchable(Touchable.disabled);
        panel.addActor(inside);
        panel.addActor(boardContent(visibleH));
        return panel;
    }

    private Table boardContent(float visibleH) {
        Table content = new Table();
        content.pad(20f, 28f, 20f, 28f);
        statusLabel = new Label("", skin, "medium");
        statusLabel.setColor(STATUS_INK);
        statusLabel.setAlignment(Align.left);
        list = new Table();
        list.top().left();
        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        content.add(statusLabel).growX().padBottom(10f).row();
        content.add(scroll).grow().row();
        content.setBounds(FRAME_BLEED, FRAME_BLEED, UI_WIDTH, visibleH - FRAME);
        return content;
    }

    private void addTabs(float visibleH) {
        tabMarkers.clear();
        Tab[] order = {Tab.DAILY, Tab.MAIN, Tab.EPIC, Tab.MINI_GAMES};
        float tabX = EDGE + TAB_SHIFT_X;
        float tabY = visibleH - TAB_SLOT_H + 18f + TAB_SHIFT_Y;
        for (int i = 0; i < order.length; i++) {
            Tab target = order[i];
            QuestTabMarker marker = new QuestTabMarker(skin, tabLabel(target), () -> setTab(target));
            marker.setPosition(tabX + i * (TAB_SLOT_W + TAB_GAP), tabY);
            tabMarkers.add(marker);
            stage.addActor(marker);
        }
        refreshTabs();
    }

    private AtlasImageButton addCloseButton() {
        AtlasImageButton close = new AtlasImageButton(
                textures.region(ShopArt.CLOSE), textures.region(ShopArt.CLOSE_DOWN),
                CLOSE_SIZE, this::goBack);
        close.setPosition(
                UI_WIDTH - EDGE - CLOSE_SIZE + CLOSE_SHIFT_X,
                UI_HEIGHT - EDGE - CLOSE_SIZE + CLOSE_SHIFT_Y);
        stage.addActor(close);
        return close;
    }

    private void setTab(Tab next) {
        if (tab == next) {
            return;
        }
        GameAudio.get().playNavClick();
        tab = next;
        refresh();
    }

    private void refreshTabs() {
        for (int i = 0; i < tabMarkers.size; i++) {
            Tab target = switch (i) {
                case 0 -> Tab.DAILY;
                case 1 -> Tab.MAIN;
                case 2 -> Tab.EPIC;
                default -> Tab.MINI_GAMES;
            };
            boolean on = tab == target;
            tabMarkers.get(i).setFace(questArt.region(activeFace(target, on)));
        }
    }

    private static String activeFace(Tab target, boolean on) {
        return switch (target) {
            case DAILY -> on ? QuestArt.DAILY_ACTIVE : QuestArt.DAILY_INACTIVE;
            case MAIN -> on ? QuestArt.MAIN_ACTIVE : QuestArt.MAIN_INACTIVE;
            case EPIC -> on ? QuestArt.EPIC_ACTIVE : QuestArt.EPIC_INACTIVE;
            case MINI_GAMES -> on ? QuestArt.MINI_ACTIVE : QuestArt.MINI_INACTIVE;
        };
    }

    private void refresh() {
        controller.syncForCurrentUser();
        if (resources != null) {
            resources.refresh();
        }
        refreshTabs();
        list.clear();
        list.top().left();

        if (tab == Tab.MINI_GAMES) {
            fillMiniGames();
            return;
        }

        CommandResult<List<Quest>> result = switch (tab) {
            case DAILY -> controller.showDailyQuests();
            case MAIN -> controller.showMainQuests();
            case EPIC -> controller.showEpicQuests();
            default -> controller.showAllQuests();
        };
        if (!result.isSuccess() || result.getData() == null) {
            statusLabel.setText(result.getMessage());
            Label empty = new Label(result.getMessage(), skin, "medium");
            empty.setColor(STATUS_INK);
            list.add(empty).pad(24f);
            return;
        }

        List<Quest> quests = result.getData();
        statusLabel.setText(tabLabel(tab) + "  ·  " + quests.size() + " active");
        if (quests.isEmpty()) {
            Label empty = new Label("No active quests in this tab.", skin, "medium");
            empty.setColor(STATUS_INK);
            list.add(empty).pad(24f);
            return;
        }
        for (Quest quest : quests) {
            list.add(questCard(quest)).growX()
                .padLeft(QuestCardFactory.CARD_SIDE).padRight(QuestCardFactory.CARD_SIDE)
                .padBottom(QuestCardFactory.CARD_GAP).row();
        }
    }

    private void fillMiniGames() {
        CommandResult<List<MiniGameDataEntry>> result = controller.showMiniGames();
        if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
            statusLabel.setText(result.getMessage());
            Label empty = new Label(result.getMessage(), skin, "medium");
            empty.setColor(STATUS_INK);
            list.add(empty).pad(24f);
            return;
        }
        List<MiniGameDataEntry> games = result.getData();
        statusLabel.setText("Mini-Games  ·  " + (games.size() + 2) + " stages");
        for (MiniGameDataEntry entry : games) {
            list.add(miniGameCard(entry)).growX()
                .padLeft(QuestCardFactory.CARD_SIDE).padRight(QuestCardFactory.CARD_SIDE)
                .padBottom(QuestCardFactory.CARD_GAP).row();
        }
        list.add(iZombieMultiplayerCard()).growX()
            .padLeft(QuestCardFactory.CARD_SIDE).padRight(QuestCardFactory.CARD_SIDE)
            .padBottom(QuestCardFactory.CARD_GAP).row();
        list.add(iZombieCouchPlayCard()).growX()
            .padLeft(QuestCardFactory.CARD_SIDE).padRight(QuestCardFactory.CARD_SIDE)
            .padBottom(QuestCardFactory.CARD_GAP).row();
    }

    private Table questCard(Quest quest) {
        QuestProgress progress = quest.getProgress();
        int current = progress == null ? 0 : progress.getCurrentValue();
        int target = progress == null ? 1 : Math.max(1, progress.getTargetValue());
        boolean ready = quest.checkCompletion();

        TextButton action = new TextButton(ready ? "CLAIM" : "PLAY", skin,
            ready ? "green" : "purple");
        action.setDisabled(!ready);
        if (!ready) {
            action.setColor(0.85f, 0.85f, 0.85f, 1f);
        }
        action.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!ready) {
                    return;
                }
                CommandResult<Void> result = controller.completeQuest(quest.getName());
                showToast(result.getMessage(), !result.isSuccess());
                refresh();
            }
        });

        return cards.questRow(
            quest.getName(),
            quest.getConditionDescription(),
            QuestArt.iconFor(quest),
            current,
            target,
            ready,
            quest.getCategory() == QuestCategory.DAILY,
            quest.getReward(),
            0,
            action);
    }

    private Table miniGameCard(MiniGameDataEntry entry) {
        TextButton play = new TextButton("PLAY", skin, "purple");
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playMiniGame(entry);
            }
        });
        String type = entry.getMiniGameType();
        String detail = "Stage " + entry.getStage()
            + "  ·  Difficulty " + entry.getDifficultyTier()
            + "  ·  Reward " + entry.getCoinReward() + " coins";
        return cards.questRow(
            prettyType(type),
            detail,
            QuestCardFactory.miniGameIconId(type),
            0,
            1,
            false,
            false,
            null,
            entry.getCoinReward(),
            play);
    }

    private void playMiniGame(MiniGameDataEntry entry) {
        try {
            MiniGameType type = MiniGameType.valueOf(
                    entry.getMiniGameType().toUpperCase(Locale.ROOT)
                            .replace(' ', '_').replace('-', '_'));
            if (tryResumeMiniGame(type, entry.getStage())) {
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // Unknown mini-game type — fall through to controller error.
        }
        CommandResult<Void> result = controller.enterMiniGame(
                entry.getMiniGameType(), entry.getStage());
        showToast(result.getMessage(), !result.isSuccess());
        if (result.isSuccess()) {
            game.setScreen(new LevelObjectivesScreen(game, null));
        }
    }

    private boolean tryResumeMiniGame(MiniGameType type, int stage) {
        if (!GameSaveService.getInstance().hasSaveForMiniGame(type, stage)) {
            return false;
        }
        try {
            GameSaveService.getInstance().resumeSavedGame();
            game.setScreen(new GameplayScreen(game));
            return true;
        } catch (Exception resumeError) {
            GameSaveService.getInstance().clearCurrentUserSave();
            showToast("Could not resume save; starting fresh.", true);
            return false;
        }
    }

    /** Same I, Zombie branding as the SP stages; opens multiplayer matchmaking only. */
    private Table iZombieMultiplayerCard() {
        TextButton play = new TextButton("PLAY", skin, "purple");
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stage.addActor(new IZombieMatchmakingOverlay(game, skin, () -> refresh()));
            }
        });
        return cards.questRow(
            prettyType("I_ZOMBIE"),
            "Multiplayer 1v1 — invite a friend or match with a random opponent.",
            QuestCardFactory.miniGameIconId("I_ZOMBIE"),
            0,
            1,
            false,
            false,
            null,
            0,
            play);
    }

    /** Offline two-player I, Zombie on one machine (couch play). */
    private Table iZombieCouchPlayCard() {
        TextButton play = new TextButton("PLAY", skin, "purple");
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> result = MultiplayerMatchBootstrap.openCouchPlay(game);
                showToast(result.getMessage(), !result.isSuccess());
            }
        });
        return cards.questRow(
            prettyType("I_ZOMBIE") + " · Couch Play",
            "Offline 1v1 on this device — plants use the mouse, zombies use the keyboard.",
            QuestCardFactory.miniGameIconId("I_ZOMBIE"),
            0,
            1,
            false,
            false,
            null,
            0,
            play);
    }

    /** Bakes {@link QuestArt#ICON_LAWN_ROW} rotated 90° for column / cross icons. */
    private void ensureLawnColumnIcon() {
        if (lawnColumnRegion != null || textures == null) {
            return;
        }
        TextureRegion row = textures.region(QuestArt.ICON_LAWN_ROW);
        if (row == null) {
            return;
        }
        lawnColumnTex = bakeRotated90(row);
        if (lawnColumnTex != null) {
            lawnColumnRegion = new TextureRegion(lawnColumnTex);
            lawnColumnRegion.flip(false, true);
        }
    }

    private Texture bakeRotated90(TextureRegion src) {
        int sw = Math.max(1, src.getRegionWidth());
        int sh = Math.max(1, src.getRegionHeight());
        int dw = sh;
        int dh = sw;
        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, dw, dh, false);
        fbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Matrix4 prev = game.batch.getProjectionMatrix().cpy();
        game.batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0f, 0f, dw, dh));
        game.batch.begin();
        game.batch.draw(src,
            dw * 0.5f - sw * 0.5f, dh * 0.5f - sh * 0.5f,
            sw * 0.5f, sh * 0.5f,
            sw, sh, 1f, 1f, 90f);
        game.batch.end();
        game.batch.setProjectionMatrix(prev);
        Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, dw, dh);
        fbo.end();
        fbo.dispose();
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }


    private static String tabLabel(Tab tab) {
        return switch (tab) {
            case DAILY -> "Daily";
            case MAIN -> "Main";
            case EPIC -> "Epic";
            case MINI_GAMES -> "Mini-Games";
        };
    }

    private void goBack() {
        game.setScreen(new AdventureScreen(game));
    }

    @Override
    protected void onBack() {
        goBack();
    }

    private static String prettyType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Mini-Game";
        }
        String[] words = raw.replace('_', ' ').split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)))
                .append(word.substring(1).toLowerCase());
        }
        return out.toString();
    }

    private static Texture verticalGradient(Color top, Color bottom, int height) {
        Pixmap pm = new Pixmap(1, Math.max(2, height), Pixmap.Format.RGBA8888);
        for (int y = 0; y < pm.getHeight(); y++) {
            float a = y / (float) (pm.getHeight() - 1);
            pm.setColor(
                top.r + (bottom.r - top.r) * a,
                top.g + (bottom.g - top.g) * a,
                top.b + (bottom.b - top.b) * a,
                1f);
            pm.drawPixel(0, y);
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }

    private static Texture solidPixel(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
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
        if (boardGradient != null) {
            boardGradient.dispose();
            boardGradient = null;
        }
        if (whitePixel != null) {
            whitePixel.dispose();
            whitePixel = null;
        }
        if (lawnColumnTex != null) {
            lawnColumnTex.dispose();
            lawnColumnTex = null;
            lawnColumnRegion = null;
        }
        questArt.dispose();
        super.dispose();
    }
}

