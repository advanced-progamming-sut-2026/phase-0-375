package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import controller.NewsMenuController;
import model.news.NewsItem;
import pvz.skin.BorderedTable;
import view.gui.audio.GameAudio;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Dimmed modal news panel shown over the main hub (fade in/out).
 */
public final class NewsOverlay {
    private static final float FADE_IN = 0.11f;
    private static final float FADE_OUT = 0.07f;
    /** Inner vertical padding of the news card (top / bottom). */
    public static float CARD_PAD_Y = 28f;
    /** Inner horizontal padding of the news card (left / right). */
    public static float CARD_PAD_X = 0f;
    private static final Color TITLE_COLOR = rgb(250, 228, 101);
    private static final Color DATE_COLOR = rgb(10, 64, 68);
    private static final Color NEWS_COLOR = rgb(67, 62, 0);
    private static final Color DIM = new Color(0f, 0f, 0f, 0.55f);

    private static Texture pixel;
    private static final Vector2 TEMP = new Vector2();
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("EEE MMM d yyyy", Locale.ENGLISH);

    private NewsOverlay() {}

    public static Table create(Skin skin, Runnable onClose) {
        GameAudio.get().playOverlayOpen();
        NewsMenuController controller = NewsMenuController.getInstance();

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setName(view.gui.screen.AbstractMenuScreen.OVERLAY_NAME);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(DIM));
        overlay.setTouchable(Touchable.enabled);
        Runnable closer = () -> dismiss(overlay, onClose);
        overlay.setUserObject(closer);
        overlay.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getTarget() == overlay) {
                    closer.run();
                }
            }
        });

        BorderedTable card = new BorderedTable();
        card.pad(CARD_PAD_Y, CARD_PAD_X, CARD_PAD_Y, CARD_PAD_X);

        Table header = new Table();
        Label title = new Label("News", skin, "big_outline");
        title.setColor(TITLE_COLOR);
        header.add(title).expandX().center().pad(18f, 28f, 16f, 28f);
        card.add(header).growX().row();

        Table body = new Table();
        body.pad(18f, 28f, 24f, 28f);
        body.add(buildContent(skin, controller)).grow().padBottom(18f).row();

        TextButton back = new TextButton("Back", skin, "brown");
        UiMotion.bindPressScale(back);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closer.run();
            }
        });
        body.add(back).width(200f).height(56f);
        card.add(body).grow();

        overlay.add(card).width(1000f).height(800f).pad(40f);
        fadeIn(overlay);
        UiMotion.fadeSlideIn(card, 0.35f);
        return overlay;
    }

    private static void fadeIn(Table overlay) {
        overlay.getColor().a = 0f;
        overlay.addAction(Actions.fadeIn(FADE_IN));
    }

    private static void dismiss(Table overlay, Runnable after) {
        overlay.setTouchable(Touchable.disabled);
        overlay.clearActions();
        overlay.addAction(Actions.sequence(
            Actions.fadeOut(FADE_OUT),
            Actions.run(() -> {
                overlay.remove();
                if (after != null) {
                    after.run();
                }
            })
        ));
    }

    private static ScrollPane buildContent(Skin skin, NewsMenuController controller) {
        Table content = new Table(skin);
        applyInnerBackground(content, skin);
        content.defaults().left().growX();
        content.pad(16f, 18f, 16f, 18f);

        ScrollPane scroll = new ScrollPane(content, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        List<NewsItem> raw = controller.showAll().getData();
        List<NewsItem> allNews = raw == null ? new ArrayList<>() : new ArrayList<>(raw);
        if (allNews.isEmpty()) {
            Label empty = new Label(
                    "No news yet. Unlock plants, encounter zombies, or unlock new levels "
                            + "to start generating news.",
                    skin, "medium_outline");
            empty.setColor(NEWS_COLOR);
            empty.setWrap(true);
            content.add(empty).growX().pad(12f);
            return scroll;
        }

        allNews.sort(Comparator
                .comparing(NewsItem::getPublishDate)
                .reversed()
                .thenComparing((one, two) -> Boolean.compare(
                        controller.isRead(one.getId()), controller.isRead(two.getId()))));

        LocalDate currentDate = null;
        for (NewsItem newsItem : allNews) {
            if (!newsItem.getPublishDate().equals(currentDate)) {
                currentDate = newsItem.getPublishDate();
                String formattedDate = newsItem.getPublishDate().format(DATE_FORMATTER);
                Label dateLabel = new Label(formattedDate, skin, "big");
                dateLabel.setColor(DATE_COLOR);
                dateLabel.setWrap(true);
                content.add(dateLabel).padTop(8f).padBottom(12f).row();
            }

            boolean isRead = controller.isRead(newsItem.getId());
            String tag = isRead ? "" : "[NEW] ";

            Table itemContainer = new Table();
            itemContainer.defaults().left().growX();

            Label itemTitle = new Label(tag + newsItem.getTitle(), skin, "big");
            itemTitle.setColor(NEWS_COLOR);
            itemTitle.setWrap(true);
            itemContainer.add(itemTitle).padBottom(8f).row();

            Label itemBody = new Label(newsItem.getBody(), skin, "big");
            itemBody.setColor(NEWS_COLOR);
            itemBody.setWrap(true);
            itemContainer.add(itemBody).row();

            content.add(itemContainer).padBottom(22f).row();

            if (!isRead) {
                itemContainer.addAction(new Action() {
                    @Override
                    public boolean act(float delta) {
                        if (isVisibleInScrollPane(itemContainer, scroll)) {
                            controller.markAsRead(newsItem.getId());
                            return true;
                        }
                        return false;
                    }
                });
            }
        }

        return scroll;
    }

    private static void applyInnerBackground(Table content, Skin skin) {
        try {
            content.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        } catch (Exception e) {
            content.setBackground(new TextureRegionDrawable(whitePixel()).tint(rgb(230, 220, 190)));
        }
    }

    private static boolean isVisibleInScrollPane(Actor item, ScrollPane scrollPane) {
        if (item.getStage() == null || scrollPane.getStage() == null) {
            return false;
        }
        if (item.getHeight() <= 0 || scrollPane.getHeight() <= 0) {
            return false;
        }

        TEMP.set(0, 0);
        item.localToStageCoordinates(TEMP);
        scrollPane.stageToLocalCoordinates(TEMP);

        float itemBottom = TEMP.y;
        float itemTop = itemBottom + item.getHeight();
        return itemTop > 0 && itemBottom < scrollPane.getHeight();
    }

    private static Color rgb(int r, int g, int b) {
        return new Color(r / 255f, g / 255f, b / 255f, 1f);
    }

    private static Texture whitePixel() {
        if (pixel == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
            pixmap.dispose();
        }
        return pixel;
    }
}
