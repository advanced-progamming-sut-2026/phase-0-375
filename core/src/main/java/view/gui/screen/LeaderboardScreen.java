package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controller.GameMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.user.User;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;

import java.util.List;

/**
 * Global leaderboard (phase-1 columns): progress, mini-games, daily/non-daily
 * quests, and highest MyoPoint. Header clicks toggle asc/desc via
 * {@link GameMenuController#leaderboard(String, String)}.
 */
public final class LeaderboardScreen extends AbstractMenuScreen {
    private static final String BG_RELATIVE =
        "Exports/AI generated/Gemini_Generated_Image_5eopbf5eopbf5eop.jpg";
    private static final float PANEL_W = 1500f;
    private static final float PANEL_H = 820f;
    /** Pushes the "Leaderboard" title down inside the card. */
    private static final float TITLE_PAD_TOP = -10f;
    /** Pulls the Back button up (less gap under the table). */
    private static final float BACK_LIFT = 37f;
    private static final float HEADER_FONT_SCALE = 0.95f;
    private static final float BACK_FONT_SCALE = 1.15f;
    private static final float COL_USER = 220f;
    private static final float COL_PROGRESS = 200f;
    private static final float COL_MINI = 140f;
    private static final float COL_DAILY = 150f;
    private static final float COL_QUESTS = 130f;
    private static final float COL_SCORE = 140f;
    private static final Color HEADER = new Color(0.15f, 0.1f, 0.05f, 1f);
    private static final Color ROW = new Color(0.12f, 0.08f, 0.04f, 1f);
    private static final Color SELF = new Color(0.45f, 0.22f, 0.05f, 1f);

    private final Runnable backAction;
    private final GameMenuController controller = GameMenuController.getInstance();

    private Texture backgroundTex;
    private Table body;
    private String sortKey = "score";
    private boolean descending = true;

    public LeaderboardScreen(PvzGdxGame game, Runnable onBack) {
        super(game);
        this.backAction = onBack != null ? onBack : () -> game.setScreen(new MainHubScreen(game));
    }

    @Override
    protected void buildUi() {
        addBackground();

        BorderedTable card = new BorderedTable();
        card.pad(28f, 32f, 24f, 32f);

        Label title = new Label("Leaderboard", skin, "big");
        title.setColor(Color.BLACK);
        title.setAlignment(Align.center);
        card.add(title).padTop(TITLE_PAD_TOP).padBottom(8f).row();

        Label hint = new Label("Click a column header to sort  ·  click again to flip order", skin, "secondary");
        hint.setColor(Color.DARK_GRAY);
        hint.setAlignment(Align.center);
        card.add(hint).padBottom(16f).row();

        body = new Table();
        ScrollPane scroll = new ScrollPane(body, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        card.add(scroll).width(PANEL_W - 80f).height(PANEL_H - 200f)
            .padBottom(16f - BACK_LIFT).row();

        TextButton back = new TextButton("Back", skin, "brown");
        back.getLabel().setFontScale(BACK_FONT_SCALE);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                backAction.run();
            }
        });
        card.add(back).width(200f).height(56f).padTop(-BACK_LIFT);

        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(PANEL_W).height(PANEL_H);
        stage.addActor(root);

        refreshRows();
    }

    @Override
    protected void onBack() {
        backAction.run();
    }

    private void addBackground() {
        FileHandle file = resolveBackground();
        if (file == null || !file.exists()) {
            return;
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
            FileHandle fromRoot = game.assets.root.child(BG_RELATIVE);
            if (fromRoot.exists()) {
                return fromRoot;
            }
        }
        FileHandle local = Gdx.files.local("assets/" + BG_RELATIVE);
        return local.exists() ? local : Gdx.files.local(BG_RELATIVE);
    }

    private void refreshRows() {
        body.clear();
        body.add(headerRow()).growX().padBottom(10f).row();

        CommandResult<List<User>> result = controller.leaderboard(sortKey, descending ? "desc" : "asc");
        if (!result.isSuccess() || result.getData() == null) {
            Label err = new Label(result.getMessage(), skin, "medium");
            err.setColor(Color.SCARLET);
            body.add(err).padTop(24f);
            return;
        }
        List<User> users = result.getData();
        if (users.isEmpty()) {
            Label empty = new Label("No players yet.", skin, "medium");
            empty.setColor(ROW);
            body.add(empty).padTop(24f);
            return;
        }

        String self = App.getInstance().getCurrentUser() == null
            ? null
            : App.getInstance().getCurrentUser().getUsername();
        int rank = 1;
        for (User user : users) {
            boolean me = self != null && self.equalsIgnoreCase(user.getUsername());
            body.add(dataRow(rank, user, me)).growX().padBottom(4f).row();
            rank++;
        }
    }

    private Table headerRow() {
        Table row = new Table();
        row.add(spacerLabel("#")).width(56f);
        row.add(headerButton("Username", "username")).width(COL_USER);
        row.add(headerButton("Progress", "progress")).width(COL_PROGRESS);
        row.add(headerButton("Mini-games", "minigames")).width(COL_MINI);
        row.add(headerButton("Daily quests", "daily-quests")).width(COL_DAILY);
        row.add(headerButton("Quests", "quests")).width(COL_QUESTS);
        row.add(headerButton("MyoPoint", "score")).width(COL_SCORE);
        return row;
    }

    private TextButton headerButton(String label, String key) {
        String mark = "";
        if (key.equals(sortKey)) {
            mark = descending ? " ▼" : " ▲";
        }
        TextButton button = new TextButton(label + mark, skin, "brown");
        button.getLabel().setFontScale(HEADER_FONT_SCALE);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (key.equals(sortKey)) {
                    descending = !descending;
                } else {
                    sortKey = key;
                    descending = !"username".equals(key);
                }
                refreshRows();
            }
        });
        return button;
    }

    private Table dataRow(int rank, User user, boolean highlight) {
        Table row = new Table();
        Color color = highlight ? SELF : ROW;
        row.add(cell(String.valueOf(rank), color, skin)).width(56f);
        row.add(cell(user.getUsername(), color, skin)).width(COL_USER);
        row.add(cell(formatProgress(user), color, skin)).width(COL_PROGRESS);
        row.add(cell(String.valueOf(user.getCompletedMiniGames()), color, skin)).width(COL_MINI);
        row.add(cell(String.valueOf(user.getCompletedDailyQuests()), color, skin)).width(COL_DAILY);
        row.add(cell(String.valueOf(user.getCompletedNonDailyQuests()), color, skin)).width(COL_QUESTS);
        row.add(cell(String.valueOf(user.getHighestMyopoint()), color, skin)).width(COL_SCORE);
        return row;
    }

    private static Label cell(String text, Color color, Skin skin) {
        Label label = new Label(text, skin, "medium");
        label.setColor(color);
        label.setAlignment(Align.center);
        label.setEllipsis(true);
        return label;
    }

    /** Doc example: level 1 of chapter 3 → {@code Level 1 · Chapter 3}. */
    static String formatProgress(User user) {
        var progress = user.getChapterProgress();
        if (progress == null || progress.isEmpty()) {
            return "Level 0 · Chapter 1";
        }
        Chapter[] chapters = Chapter.values();
        for (int i = chapters.length - 1; i >= 0; i--) {
            int level = progress.getOrDefault(chapters[i], 0);
            if (level > 0) {
                return "Level " + level + " · Chapter " + (i + 1);
            }
        }
        return "Level 0 · Chapter 1";
    }

    private Label spacerLabel(String text) {
        Label label = new Label(text, skin, "medium");
        label.setColor(HEADER);
        label.setAlignment(Align.center);
        return label;
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
