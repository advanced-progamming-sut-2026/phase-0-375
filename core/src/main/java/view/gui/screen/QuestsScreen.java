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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import controller.TravelLogMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.data.minigame.MiniGameDataEntry;
import model.enums.MenuType;
import model.enums.QuestCategory;
import model.quest.Quest;
import model.quest.QuestProgress;
import model.quest.QuestReward;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.anim.PamClipCache;
import view.gui.assets.AdventureHudRegions;
import view.gui.assets.PvzAssets;
import view.gui.assets.QuestArt;
import view.gui.assets.ShopArt;
import view.gui.ui.AtlasImageButton;
import view.gui.ui.EdgeFadeOverlay;
import view.gui.ui.IZombieMatchmakingOverlay;
import view.gui.ui.PamEffectActor;
import view.gui.ui.ResourceBar;
import view.gui.ui.RoundedRegionImage;
import view.gui.ui.SkinFonts;

import java.util.List;

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

    /** Native 768 quest tab (ACTIVE 139×86; INACTIVE 139×65). */
    private static final float TAB_SLOT_W = 139f;
    private static final float TAB_SLOT_H = 86f;
    private static final float TAB_GAP = 8f;
    /**
     * Move all quest tab markers. +X right, +Y up.
     * Change {@link #TAB_SHIFT_Y} to nudge the whole row up/down.
     */
    private static final float TAB_SHIFT_X = 45f;
    private static final float TAB_SHIFT_Y = 43f;
    /** Tab caption color — keep bright so it reads on colored faces. */
    private static final Color TAB_LABEL_COLOR = new Color(1f, 0.96f, 0.88f, 1f);
    /** Caption box size inside the tab slot. */
    private static final float TAB_LABEL_W = 130f;
    private static final float TAB_LABEL_H = 28f;
    /** Caption position inside the tab slot (+X right, +Y up from bottom-left). */
    private static final float TAB_LABEL_X = 4.5f;
    private static final float TAB_LABEL_Y = 37f;
    private static final float TAB_LABEL_SCALE = 1.15f;
    private static final float TAB_LABEL_SCALE_MINI = 1.15f;

    /** Reference card height (cards size to content; kept for tuning notes). */
    private static final float CARD_H = 128f;
    /** Vertical gap between quest rows — stays fixed when text scales change. */
    private static final float CARD_GAP = 20f;
    /**
     * Left/right inset for each quest row inside the scroll list.
     * Raise to pull cards inward; lower to widen them.
     */
    private static final float CARD_SIDE = 300f;
    private static final float CARD_RADIUS = 8f;
    private static final float CARD_BORDER = 3f;
    /** Inner cream content pad: top, left, bottom, right. */
    private static final float BODY_PAD_TOP = -8f;
    private static final float BODY_PAD_LEFT = 24f;
    private static final float BODY_PAD_BOTTOM = -10f;
    private static final float BODY_PAD_RIGHT = 26f;
    private static final float ICON_SIZE = 72f;
    private static final float ICON_FRAME = 4f;
    private static final float BTN_W = 150f;
    private static final float BTN_H = 52f;
    /** Vertical nudge for CLAIM/PLAY — positive moves down, negative up. */
    private static final float BTN_SHIFT_Y = 0f;
    /** Daily clock badge size / inset in the top-left of a daily card. */
    private static final float DAILY_CLOCK_SIZE = 56f;
    private static final float DAILY_CLOCK_PAD = 4f;
    private static final float DAILY_CLOCK_SCALE = 0.3f;
    /** Reward column width (icons + xN text). */
    private static final float REWARD_COL_W = 100f;
    /** Per-reward-type icon sizes — tweak each independently. */
    private static final float REWARD_ICON_COIN = 58f;
    private static final float REWARD_ICON_GEM = 50f;
    private static final float REWARD_ICON_SEED = 90f;
    private static final float REWARD_ICON_NEW_PLANT = 70f;
    /** Quest-card text scales — change these to resize each text type. */
    private static final float TEXT_TITLE = 1.2f;
    private static final float TEXT_DESC = 1.2f;
    private static final float TEXT_PROGRESS = 1f;
    private static final float TEXT_REWARD = 1.5f;
    private static final float TEXT_BUTTON = 1f;

    private static final Color INK = new Color(0.12f, 0.10f, 0.12f, 1f);
    private static final Color STATUS_INK = new Color(0.88f, 0.82f, 0.68f, 1f);
    private static final Color GRAD_TOP = new Color(0.30f, 0.16f, 0.07f, 1f);
    private static final Color GRAD_BOTTOM = new Color(0.12f, 0.05f, 0.02f, 1f);
    private static final Color CREAM = new Color(0.93f, 0.88f, 0.76f, 1f);
    private static final Color ICON_WELL = new Color(0.82f, 0.76f, 0.62f, 1f);
    private static final Color READY_BORDER = new Color(0.28f, 0.72f, 0.22f, 1f);
    private static final Color BAR_TEXT = new Color(1f, 1f, 1f, 1f);
    private static final Color REWARD_AMT = new Color(0.15f, 0.10f, 0.08f, 1f);

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
    private final Array<TabMarker> tabMarkers = new Array<>();

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
        TextureBank t = textures;
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
        ensureLawnColumnIcon();
        pamClips = new PamClipCache(game.assets.player);
        pamClips.preloadSync(QuestArt.PAM_DAILY_CLOCK, QuestArt.PAM_CLIP);
        if (whitePixel == null) {
            whitePixel = solidPixel(Color.WHITE);
        }

        float visibleH = UI_HEIGHT - TOP_RESERVE;
        float panelW = UI_WIDTH + FRAME_BLEED * 2f;
        float panelH = visibleH + FRAME_BLEED;
        float panelX = -FRAME_BLEED;
        float panelY = -FRAME_BLEED;

        Table top = new Table();
        top.setFillParent(true);
        top.setTouchable(Touchable.childrenOnly);
        top.top().right();
        resources = new ResourceBar(skin, t);
        top.add(resources).pad(EDGE);
        stage.addActor(top);

        Group panel = new Group();
        panel.setSize(panelW, panelH);
        panel.setPosition(panelX, panelY);

        BorderedTable frame = new BorderedTable();
        frame.setBounds(0f, 0f, panelW, panelH);
        panel.addActor(frame);

        float innerW = panelW - FRAME * 2f;
        float innerH = panelH - FRAME * 2f;
        boardGradient = verticalGradient(GRAD_TOP, GRAD_BOTTOM, 8);
        RoundedRegionImage inside = new RoundedRegionImage(
            new TextureRegion(boardGradient), CORNER, true, true, false, false);
        inside.setBounds(FRAME, FRAME, innerW, innerH);
        inside.setTouchable(Touchable.disabled);
        panel.addActor(inside);

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
        panel.addActor(content);
        stage.addActor(panel);

        tabMarkers.clear();
        Tab[] order = {Tab.DAILY, Tab.MAIN, Tab.EPIC, Tab.MINI_GAMES};
        float tabX = EDGE + TAB_SHIFT_X;
        float tabY = visibleH - TAB_SLOT_H + 18f + TAB_SHIFT_Y;
        for (int i = 0; i < order.length; i++) {
            Tab target = order[i];
            TabMarker marker = new TabMarker(tabLabel(target), () -> setTab(target));
            marker.setPosition(tabX + i * (TAB_SLOT_W + TAB_GAP), tabY);
            tabMarkers.add(marker);
            stage.addActor(marker);
        }
        refreshTabs();

        AtlasImageButton close = new AtlasImageButton(
            t.region(ShopArt.CLOSE), t.region(ShopArt.CLOSE_DOWN), CLOSE_SIZE, this::goBack);
        close.setPosition(
            UI_WIDTH - EDGE - CLOSE_SIZE + CLOSE_SHIFT_X,
            UI_HEIGHT - EDGE - CLOSE_SIZE + CLOSE_SHIFT_Y);
        stage.addActor(close);

        top.toFront();
        close.toFront();
        for (TabMarker marker : tabMarkers) {
            marker.toFront();
        }

        refresh();
    }

    private void setTab(Tab next) {
        if (tab == next) {
            return;
        }
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
                .padLeft(CARD_SIDE).padRight(CARD_SIDE).padBottom(CARD_GAP).row();
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
        statusLabel.setText("Mini-Games  ·  " + (games.size() + 1) + " stages");
        for (MiniGameDataEntry entry : games) {
            list.add(miniGameCard(entry)).growX()
                .padLeft(CARD_SIDE).padRight(CARD_SIDE).padBottom(CARD_GAP).row();
        }
        list.add(iZombieMultiplayerCard()).growX()
            .padLeft(CARD_SIDE).padRight(CARD_SIDE).padBottom(CARD_GAP).row();
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

        return questRow(
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
                CommandResult<Void> result = controller.enterMiniGame(
                    entry.getMiniGameType(), entry.getStage());
                showToast(result.getMessage(), !result.isSuccess());
                if (result.isSuccess()) {
                    game.setScreen(new LevelObjectivesScreen(game, null));
                }
            }
        });
        String type = entry.getMiniGameType();
        String detail = "Stage " + entry.getStage()
            + "  ·  Difficulty " + entry.getDifficultyTier()
            + "  ·  Reward " + entry.getCoinReward() + " coins";
        return questRow(
            prettyType(type),
            detail,
            miniGameIconId(type),
            0,
            1,
            false,
            false,
            null,
            entry.getCoinReward(),
            play);
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
        return questRow(
            prettyType("I_ZOMBIE"),
            "Multiplayer 1v1 — invite a friend or match with a random opponent.",
            miniGameIconId("I_ZOMBIE"),
            0,
            1,
            false,
            false,
            null,
            0,
            play);
    }

    /**
     * Cream rounded plate with icon, title + description + bar, rewards, and action.
     * Height follows content (and text scales); list spacing stays {@link #CARD_GAP}.
     */
    private Table questRow(String title, String description, String iconId,
                           int current, int target, boolean ready, boolean dailyClock,
                           QuestReward reward, int fallbackCoins, TextButton action) {
        float midW = midColumnWidth();

        // Outer ring always present so claimable (green) and normal cards share the same
        // content inset — otherwise ready cards look like they ignore BODY_PAD_*.
        Stack stack = new Stack();
        stack.add(roundedFill(ready ? READY_BORDER : CREAM, CARD_RADIUS));

        Table plate = new Table();
        plate.pad(CARD_BORDER);
        Stack plateStack = new Stack();
        plateStack.add(roundedFill(CREAM, Math.max(2f, CARD_RADIUS - 2f)));

        Table body = new Table();
        body.pad(BODY_PAD_TOP, BODY_PAD_LEFT, BODY_PAD_BOTTOM, BODY_PAD_RIGHT);
        // Center columns vertically so tall reward icons (e.g. Main seed packets)
        // don't leave empty cream under title/progress like a missing top pad.
        body.add(questIcon(iconId)).size(ICON_SIZE, ICON_SIZE).padRight(14f);

        Label name = new Label(title == null ? "" : title, skin, "medium");
        name.setColor(INK);
        name.setWrap(true);
        SkinFonts.scaleLabel(name, skin, "medium", TEXT_TITLE);

        String descText = description == null ? "" : description.trim();
        Label desc = new Label(descText, skin, "secondary");
        desc.setColor(new Color(0.35f, 0.32f, 0.30f, 1f));
        desc.setWrap(true);
        SkinFonts.scaleLabel(desc, skin, "secondary", TEXT_DESC);

        ProgressBar bar = new ProgressBar(0f, Math.max(1, target), 1f, false,
            skin.get(ready || current >= target ? "xp_green" : "xp_yellow",
                ProgressBar.ProgressBarStyle.class));
        bar.setAnimateDuration(0f);
        bar.setValue(Math.min(current, Math.max(1, target)));
        bar.setTouchable(Touchable.disabled);

        Label progressLabel = new Label(current + "/" + target, skin, "secondary");
        progressLabel.setColor(BAR_TEXT);
        progressLabel.setAlignment(Align.center);
        SkinFonts.scaleLabel(progressLabel, skin, "secondary", TEXT_PROGRESS);

        Table barWrap = new Table();
        barWrap.stack(bar, progressLabel).growX().height(22f * Math.max(1f, TEXT_PROGRESS));

        Table mid = new Table();
        // Fixed wrap width so preferred height tracks TEXT_* scales correctly.
        mid.add(name).width(midW).left().row();
        if (!descText.isEmpty()) {
            mid.add(desc).width(midW).left().padTop(2f).row();
        }
        mid.add(barWrap).width(midW).padTop(6f);
        body.add(mid).growX().left().padRight(12f);

        body.add(rewardColumn(reward, fallbackCoins)).width(REWARD_COL_W).center().padRight(12f);
        SkinFonts.scaleButton(action, skin, "purple", TEXT_BUTTON);
        body.add(action).width(BTN_W).height(BTN_H).padTop(BTN_SHIFT_Y);

        // Keep body at preferred height inside the stack so list min-sizing cannot
        // stretch the cream and pin content to the top.
        Table bodySlot = new Table();
        bodySlot.add(body).growX().expandY().center();
        plateStack.add(bodySlot);
        plate.add(plateStack).grow();
        stack.add(plate);

        if (dailyClock && pamClips != null && game.assets != null) {
            Table corner = new Table();
            corner.setFillParent(true);
            corner.top().left();
            PamEffectActor clock = new PamEffectActor(
                game.assets.player, pamClips, QuestArt.PAM_DAILY_CLOCK, QuestArt.PAM_CLIP);
            clock.setEffectScale(DAILY_CLOCK_SCALE);
            corner.add(clock).size(DAILY_CLOCK_SIZE, DAILY_CLOCK_SIZE)
                .padTop(DAILY_CLOCK_PAD).padLeft(DAILY_CLOCK_PAD);
            stack.add(corner);
        }

        Table root = new Table();
        root.add(stack).growX();
        return root;
    }

    /**
     * Mid-column wrap width for one card at the current layout constants.
     */
    private static float midColumnWidth() {
        float w = UI_WIDTH - 28f * 2f - CARD_SIDE * 2f;
        w -= CARD_BORDER * 2f;
        w -= BODY_PAD_LEFT + BODY_PAD_RIGHT;
        w -= ICON_SIZE + 14f;
        w -= REWARD_COL_W + 12f;
        w -= BTN_W;
        return Math.max(80f, w);
    }

    private static String miniGameIconId(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return QuestArt.ICON_ZOMBIE;
        }
        String key = rawType.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (key) {
            case "VASE_BREAKER", "VASEBREAKER" -> QuestArt.ICON_VASE_BREAKER;
            case "BEGHOULED" -> QuestArt.ICON_BEGHOULED;
            case "WALLNUT_BOWLING", "WALL_NUT_BOWLING", "BOWLING" -> QuestArt.ICON_BOWLING;
            case "I_ZOMBIE", "IZOMBIE" -> QuestArt.ICON_GARGANTUAR;
            case "ZOMBOTANY" -> QuestArt.ICON_ZOMBIE;
            default -> QuestArt.ICON_ZOMBIE;
        };
    }

    private Actor rewardColumn(QuestReward reward, int fallbackCoins) {
        Table col = new Table();
        boolean any = false;
        if (reward != null && reward.getCoinAmount() > 0) {
            col.add(rewardChip(QuestArt.COIN_ICON, reward.getCoinAmount(), REWARD_ICON_COIN, null))
                .padBottom(4f).row();
            any = true;
        }
        if (reward != null && reward.getGemAmount() > 0) {
            col.add(rewardChip(QuestArt.GEM_ICON, reward.getGemAmount(), REWARD_ICON_GEM, null))
                .padBottom(4f).row();
            any = true;
        }
        if (reward != null && reward.getInventoryItem() != null && !reward.getInventoryItem().isBlank()
            && reward.getInventoryItemAmount() > 0) {
            col.add(rewardChip(QuestArt.REWARD_SEED_PACKET, reward.getInventoryItemAmount(),
                REWARD_ICON_SEED, null)).padBottom(4f).row();
            any = true;
        }
        if (reward != null && reward.getUnlockableName() != null && !reward.getUnlockableName().isBlank()) {
            col.add(rewardChip(QuestArt.REWARD_NEW_PLANT, 0, REWARD_ICON_NEW_PLANT, "New plant"))
                .padBottom(4f).row();
            any = true;
        }
        if (!any && fallbackCoins > 0) {
            col.add(rewardChip(QuestArt.COIN_ICON, fallbackCoins, REWARD_ICON_COIN, null)).row();
            any = true;
        }
        if (!any) {
            Label none = new Label("—", skin, "secondary");
            none.setColor(REWARD_AMT);
            none.setAlignment(Align.center);
            col.add(none);
        }
        return col;
    }

    private Table rewardChip(String iconId, int amount, float iconSize, String caption) {
        Table chip = new Table();
        TextureRegion region = textures == null ? null : textures.region(iconId);
        if (region != null) {
            Image icon = new Image(new TextureRegionDrawable(region));
            icon.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            chip.add(icon).size(iconSize, iconSize).row();
        }
        if (amount > 0) {
            Label amt = new Label("x" + amount, skin, "secondary");
            amt.setColor(REWARD_AMT);
            amt.setAlignment(Align.center);
            SkinFonts.scaleLabel(amt, skin, "secondary", TEXT_REWARD);
            chip.add(amt);
        } else if (caption != null && !caption.isBlank()) {
            Label label = new Label(caption, skin, "secondary");
            label.setColor(REWARD_AMT);
            label.setAlignment(Align.center);
            SkinFonts.scaleLabel(label, skin, "secondary", TEXT_REWARD * 0.85f);
            chip.add(label);
        }
        return chip;
    }

    private Actor questIcon(String iconId) {
        Stack well = new Stack();
        well.add(roundedFill(ICON_WELL, 6f));

        Table pad = new Table();
        pad.pad(ICON_FRAME);
        String id = iconId == null || iconId.isBlank() ? QuestArt.ICON_ZOMBIE : iconId;

        if (QuestArt.ICON_LAWN_CROSS.equals(id)) {
            Stack cross = new Stack();
            TextureRegion row = textures == null ? null : textures.region(QuestArt.ICON_LAWN_ROW);
            if (row != null) {
                Image rowImg = new Image(new TextureRegionDrawable(row));
                rowImg.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                cross.add(rowImg);
            }
            if (lawnColumnRegion != null) {
                Image colImg = new Image(new TextureRegionDrawable(lawnColumnRegion));
                colImg.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                cross.add(colImg);
            }
            pad.add(cross).grow();
        } else {
            TextureRegion iconRegion = resolveIconRegion(id);
            if (iconRegion != null) {
                Image icon = new Image(new TextureRegionDrawable(iconRegion));
                icon.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                pad.add(icon).grow();
            } else {
                Label fallback = new Label("?", skin, "medium");
                fallback.setColor(INK);
                fallback.setAlignment(Align.center);
                pad.add(fallback).grow();
            }
        }
        well.add(pad);
        return well;
    }

    private TextureRegion resolveIconRegion(String id) {
        if (QuestArt.ICON_LAWN_COLUMN.equals(id)) {
            return lawnColumnRegion;
        }
        return textures == null ? null : textures.region(id);
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

    private RoundedRegionImage roundedFill(Color color, float radius) {
        RoundedRegionImage image = new RoundedRegionImage(new TextureRegion(whitePixel), radius);
        image.setColor(color);
        image.setTouchable(Touchable.disabled);
        return image;
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

    /**
     * Fixed-size tab slot. Face is top-aligned so ACTIVE vs INACTIVE height
     * changes never shift the marker. Caption uses {@code TAB_LABEL_*} knobs.
     */
    private final class TabMarker extends Group {
        private final Image face = new Image();
        private final Label caption;

        TabMarker(String title, Runnable action) {
            setSize(TAB_SLOT_W, TAB_SLOT_H);
            face.setTouchable(Touchable.disabled);
            addActor(face);

            caption = new Label(title, skin, "medium");
            caption.setColor(TAB_LABEL_COLOR);
            caption.setAlignment(Align.center);
            caption.setTouchable(Touchable.disabled);
            float scale = "Mini-Games".equals(title) ? TAB_LABEL_SCALE_MINI : TAB_LABEL_SCALE;
            SkinFonts.scaleLabel(caption, skin, "medium", scale);
            caption.setSize(TAB_LABEL_W, TAB_LABEL_H);
            caption.setPosition(TAB_LABEL_X, TAB_LABEL_Y);
            addActor(caption);

            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (action != null) {
                        action.run();
                    }
                }
            });
        }

        void setFace(TextureRegion region) {
            if (region == null) {
                return;
            }
            face.setDrawable(new TextureRegionDrawable(region));
            face.setSize(region.getRegionWidth(), region.getRegionHeight());
            face.setPosition(0f, TAB_SLOT_H - face.getHeight());
        }
    }
}
