package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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
import view.gui.assets.PvzAssets;
import view.gui.ui.EdgeFadeOverlay;
import view.gui.ui.ResourceBar;

import java.util.List;

/**
 * Travel-log quests UI: Daily / Main / Epic / Mini-Games tabs with claimable cards.
 * In-level objectives stay on {@link LevelObjectivesOverlay} — not this screen.
 */
public final class QuestsScreen extends AbstractMenuScreen {
    private enum Tab {
        DAILY, MAIN, EPIC, MINI_GAMES
    }

    private static final float MAX_DELTA = 1f / 30f;
    private static final float EDGE_FADE_H = 600f;
    private static final float PANEL_W = 1280f;
    private static final float PANEL_H = 820f;
    /** Clears the ornate BorderedTable rim so title / Back stay inside the cream. */
    private static final float FRAME_PAD = 48f;
    private static final float CARD_H = 118f;
    private static final Color INK = new Color(0.12f, 0.10f, 0.12f, 1f);
    private static final Color MUTED = new Color(0.35f, 0.32f, 0.30f, 1f);
    private static final Color REWARD = new Color(0.45f, 0.28f, 0.05f, 1f);

    private final TravelLogMenuController controller = TravelLogMenuController.getInstance();
    private final MainMenuArt menuArt = new MainMenuArt();
    private EdgeFadeOverlay edgeFade;

    private Tab tab = Tab.DAILY;
    private Table list;
    private Label statusLabel;
    private ResourceBar resources;
    private final Array<TextButton> tabButtons = new Array<>();

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
        if (edgeFade == null) {
            edgeFade = new EdgeFadeOverlay(EDGE_FADE_H);
        }
        super.show();
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);
        controller.syncForCurrentUser();

        TextureBank textures = game.assets.textures;
        Table top = new Table();
        top.setFillParent(true);
        top.setTouchable(Touchable.childrenOnly);
        top.top().right();
        resources = new ResourceBar(skin, textures);
        top.add(resources).pad(24f);
        stage.addActor(top);

        BorderedTable card = new BorderedTable();
        card.pad(FRAME_PAD, 40f, FRAME_PAD, 40f);

        Label title = new Label("Quests", skin, "big");
        title.setColor(Color.BLACK);
        title.setAlignment(Align.center);
        card.add(title).growX().padBottom(12f).row();

        Table tabs = new Table();
        tabButtons.clear();
        tabs.add(tabButton("Daily", Tab.DAILY)).width(180f).height(52f).padRight(8f);
        tabs.add(tabButton("Main", Tab.MAIN)).width(180f).height(52f).padRight(8f);
        tabs.add(tabButton("Epic", Tab.EPIC)).width(180f).height(52f).padRight(8f);
        tabs.add(tabButton("Mini-Games", Tab.MINI_GAMES)).width(220f).height(52f);
        card.add(tabs).padBottom(12f).row();

        statusLabel = new Label("", skin, "secondary");
        statusLabel.setColor(MUTED);
        statusLabel.setAlignment(Align.center);
        card.add(statusLabel).growX().padBottom(8f).row();

        list = new Table();
        list.top().left();
        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        // Expand into leftover height so title/Back are never crushed into the rim.
        card.add(scroll).grow().padBottom(12f).row();

        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new AdventureScreen(game));
            }
        });
        card.add(back).width(200f).height(56f);

        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(PANEL_W).height(PANEL_H);
        stage.addActor(root);

        refresh();
    }

    private TextButton tabButton(String label, Tab target) {
        TextButton button = new TextButton(label, skin, "brown");
        button.setUserObject(target);
        // ClickListener (not ChangeListener/checked): brown TextButtons don't toggle reliably.
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (tab == target) {
                    return;
                }
                tab = target;
                refresh();
            }
        });
        tabButtons.add(button);
        return button;
    }

    private void refresh() {
        controller.syncForCurrentUser();
        if (resources != null) {
            resources.refresh();
        }
        highlightTabs();
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
            empty.setColor(INK);
            list.add(empty).pad(24f);
            return;
        }

        List<Quest> quests = result.getData();
        statusLabel.setText(tabLabel() + "  ·  " + quests.size() + " active");
        if (quests.isEmpty()) {
            Label empty = new Label("No active quests in this tab.", skin, "medium");
            empty.setColor(MUTED);
            list.add(empty).pad(24f);
            return;
        }
        for (Quest quest : quests) {
            list.add(questCard(quest)).growX().height(CARD_H).padBottom(10f).row();
        }
    }

    private void fillMiniGames() {
        CommandResult<List<MiniGameDataEntry>> result = controller.showMiniGames();
        if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
            statusLabel.setText(result.getMessage());
            Label empty = new Label(result.getMessage(), skin, "medium");
            empty.setColor(INK);
            list.add(empty).pad(24f);
            return;
        }
        List<MiniGameDataEntry> games = result.getData();
        statusLabel.setText("Mini-Games  ·  " + games.size() + " stages");
        for (MiniGameDataEntry entry : games) {
            list.add(miniGameCard(entry)).growX().height(CARD_H).padBottom(10f).row();
        }
    }

    private Table questCard(Quest quest) {
        BorderedTable card = new BorderedTable();
        card.pad(14f, 18f, 14f, 18f);

        Label name = new Label(quest.getName(), skin, "medium");
        name.setColor(INK);
        name.setWrap(true);

        Label condition = new Label(
            quest.getConditionDescription() == null ? "" : quest.getConditionDescription(),
            skin, "secondary");
        condition.setColor(MUTED);
        condition.setWrap(true);

        QuestProgress progress = quest.getProgress();
        int current = progress == null ? 0 : progress.getCurrentValue();
        int target = progress == null ? 1 : Math.max(1, progress.getTargetValue());
        boolean ready = quest.checkCompletion();

        ProgressBar bar = new ProgressBar(0f, target, 1f, false,
            skin.get(ready ? "xp_green" : "xp_yellow", ProgressBar.ProgressBarStyle.class));
        bar.setAnimateDuration(0f);
        bar.setValue(Math.min(current, target));
        bar.setTouchable(Touchable.disabled);

        Label progressLabel = new Label(current + " / " + target, skin, "secondary");
        progressLabel.setColor(INK);
        progressLabel.setAlignment(Align.center);

        Table barWrap = new Table();
        barWrap.stack(bar, progressLabel).growX().height(24f);

        Label reward = new Label(formatReward(quest.getReward()), skin, "secondary");
        reward.setColor(REWARD);
        reward.setWrap(true);
        reward.setAlignment(Align.right);

        TextButton claim = new TextButton(ready ? "CLAIM" : "IN PROGRESS", skin,
            ready ? "purple" : "brown");
        claim.setDisabled(!ready);
        if (!ready) {
            claim.setColor(Color.GRAY);
        }
        claim.addListener(new ChangeListener() {
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

        Table left = new Table();
        left.add(name).growX().left().row();
        left.add(condition).growX().left().padTop(4f).row();
        left.add(barWrap).growX().padTop(8f);

        card.add(left).grow().left().padRight(16f);
        card.add(reward).width(220f).right().padRight(12f);
        card.add(claim).width(160f).height(52f);
        return card;
    }

    private Table miniGameCard(MiniGameDataEntry entry) {
        BorderedTable card = new BorderedTable();
        card.pad(14f, 18f, 14f, 18f);

        Label name = new Label(prettyType(entry.getMiniGameType()), skin, "medium");
        name.setColor(INK);

        Label meta = new Label(
            "Stage " + entry.getStage()
                + "  ·  Difficulty " + entry.getDifficultyTier()
                + "  ·  Reward " + entry.getCoinReward() + " coins",
            skin, "secondary");
        meta.setColor(MUTED);
        meta.setWrap(true);

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

        Table left = new Table();
        left.add(name).growX().left().row();
        left.add(meta).growX().left().padTop(6f);

        card.add(left).grow().left().padRight(16f);
        card.add(play).width(160f).height(52f);
        return card;
    }

    private void highlightTabs() {
        for (TextButton button : tabButtons) {
            boolean on = tab == button.getUserObject();
            button.setStyle(skin.get(on ? "green" : "brown", TextButton.TextButtonStyle.class));
        }
    }

    private String tabLabel() {
        return switch (tab) {
            case DAILY -> "Daily";
            case MAIN -> "Main";
            case EPIC -> "Epic";
            case MINI_GAMES -> "Mini-Games";
        };
    }

    private static String formatReward(QuestReward reward) {
        if (reward == null) {
            return "No reward";
        }
        StringBuilder sb = new StringBuilder();
        if (reward.getCoinAmount() > 0) {
            sb.append(reward.getCoinAmount()).append(" coins");
        }
        if (reward.getGemAmount() > 0) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(reward.getGemAmount()).append(" gems");
        }
        if (reward.getInventoryItem() != null && !reward.getInventoryItem().isBlank()
            && reward.getInventoryItemAmount() > 0) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(reward.getInventoryItemAmount()).append("× ")
                .append(reward.getInventoryItem().replace('_', ' '));
        }
        if (reward.getUnlockableName() != null && !reward.getUnlockableName().isBlank()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            String unlock = reward.getUnlockableName();
            if (unlock.toLowerCase().startsWith("random")) {
                sb.append("Unlock a new plant");
            } else {
                sb.append("Unlock ").append(unlock);
            }
        }
        return sb.length() == 0 ? "No reward" : sb.toString();
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
}
