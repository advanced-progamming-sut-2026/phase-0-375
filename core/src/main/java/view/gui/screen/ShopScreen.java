package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import controller.ShopMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.CurrencyType;
import model.enums.MenuType;
import model.enums.ShopItemType;
import model.shop.DailyOffer;
import model.shop.Shop;
import model.shop.ShopItem;
import pvz.libpvz.textures.TextureBank;
import view.gui.PvzGdxGame;
import view.gui.assets.ShopArt;
import view.gui.ui.AtlasImageButton;
import view.gui.ui.ResourceBar;

import java.util.List;

/** PvZ2-inspired graphical shop backed by the existing Shop controller/model. */
public final class ShopScreen extends AbstractMenuScreen {
    private final ShopMenuController controller = ShopMenuController.getInstance();
    private final Table cards = new Table();
    private ResourceBar resources;
    private Label status;

    public ShopScreen(PvzGdxGame game) { super(game); }

    @Override
    public void show() {
        game.ensureAssets();
        TextureBank t = game.assets.textures;
        t.loadSync(ShopArt.ATLAS_STORE);
        t.loadSync(ShopArt.ATLAS_CARDS);
        t.loadSync(ShopArt.ATLAS_SEEDS);
        super.show();
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.SHOP);
        TextureBank t = game.assets.textures;

        Table top = new Table();
        top.setFillParent(true);
        top.setTouchable(Touchable.childrenOnly);
        top.top().right();
        resources = new ResourceBar(skin, t);
        top.add(resources).pad(34f);
        stage.addActor(top);

        AtlasImageButton close = button(t, ShopArt.CLOSE, ShopArt.CLOSE_DOWN, 76f,
            38f, UI_HEIGHT - 114f, this::goBack);
        stage.addActor(close);

        Label title = new Label("SHOP", skin, "big");
        title.setColor(Color.WHITE);
        title.setPosition(UI_WIDTH / 2f - 80f, UI_HEIGHT - 110f);
        stage.addActor(title);

        status = new Label("", skin, "medium");
        status.setAlignment(com.badlogic.gdx.utils.Align.center);
        status.setPosition(UI_WIDTH / 2f - 360f, 62f);
        status.setWidth(720f);
        stage.addActor(status);

        cards.defaults().pad(12f);
        Shop shop = Shop.getInstance(App.getInstance().getCurrentUser());
        for (ShopItem item : shop.getPermanentItems()) {
            cards.add(card(item, t)).width(250f).height(365f);
        }
        shop.refreshDailyOffer();
        DailyOffer offer = shop.getDailyOffer();
        if (offer != null) {
            cards.add(dailyCard(offer, t)).width(250f).height(365f);
        }
        ScrollPane scroll = new ScrollPane(cards, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(false, true);
        Table root = new Table();
        root.setFillParent(true);
        root.setTouchable(Touchable.childrenOnly);
        root.add(scroll).width(1520f).height(650f).padTop(120f);
        stage.addActor(root);
    }

    private Table card(ShopItem item, TextureBank t) {
        String bg = switch (item.getItemType()) {
            case POT, PLANT_FOOD -> ShopArt.CARD_GREEN;
            case SEED_PACKET_RANDOM, SEED_PACKET_CHOSEN -> ShopArt.CARD_PURPLE;
            case CURRENCY_CONVERSION -> ShopArt.CARD_YELLOW;
        };
        return itemCard(item, t, bg, item.getPrice());
    }

    private Table dailyCard(DailyOffer offer, TextureBank t) {
        Table result = itemCard(offer.getItem(), t, ShopArt.CARD_YELLOW, offer.getDiscountedPrice());
        Label daily = new Label("DAILY OFFER", skin, "medium");
        daily.setColor(Color.WHITE);
        result.add(daily).center().padTop(8f).row();
        return result;
    }

    private Table itemCard(ShopItem item, TextureBank t, String bgId, int price) {
        Table box = new Table();
        TextureRegion bg = t.region(bgId);
        if (bg != null) box.setBackground(new TextureRegionDrawable(bg));
        box.pad(18f);
        Image art = artwork(item, t);
        if (art != null) box.add(art).size(150f, 145f).center().row();
        Label name = new Label(labelFor(item), skin, "medium");
        name.setColor(Color.WHITE);
        name.setWrap(true);
        box.add(name).width(210f).center().padTop(8f).row();
        Label desc = new Label(item.getDescription(), skin, "secondary");
        desc.setColor(Color.WHITE);
        desc.setWrap(true);
        box.add(desc).width(210f).height(62f).center().row();
        Label priceLabel = new Label(price + (item.getCurrency() == CurrencyType.COIN ? " coins" : " gems"), skin, "medium");
        priceLabel.setColor(Color.WHITE);
        box.add(priceLabel).center().pad(5f).row();
        TextButton buy = new TextButton("BUY", skin, "purple");
        buy.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { purchase(item.getId(), item.getTargetPlantType(), 1); }
        });
        box.add(buy).width(150f).height(44f).center();
        return box;
    }

    private Image artwork(ShopItem item, TextureBank t) {
        String id = switch (item.getItemType()) {
            case POT -> ShopArt.POT;
            case PLANT_FOOD -> ShopArt.GEM;
            case SEED_PACKET_RANDOM, SEED_PACKET_CHOSEN -> ShopArt.SEED_ICON;
            case CURRENCY_CONVERSION -> ShopArt.COIN;
        };
        TextureRegion region = t.region(id);
        return region == null ? null : new Image(new TextureRegionDrawable(region));
    }

    private static String labelFor(ShopItem item) {
        return switch (item.getItemType()) {
            case POT -> "GREENHOUSE POT";
            case PLANT_FOOD -> "PLANT FOOD";
            case SEED_PACKET_RANDOM -> "RANDOM SEED PACKETS";
            case SEED_PACKET_CHOSEN -> "CHOSEN SEED PACKETS";
            case CURRENCY_CONVERSION -> "COIN CONVERSION";
        };
    }

    private void purchase(int id, String targetPlant, int count) {
        CommandResult<Void> result = controller.shopBuy(id, count, targetPlant);
        status.setText(result.getMessage());
        status.setColor(result.isSuccess() ? Color.WHITE : Color.SCARLET);
        if (result.isSuccess()) resources.refresh();
    }

    private AtlasImageButton button(TextureBank t, String up, String down, float size, float x, float y, Runnable action) {
        AtlasImageButton b = new AtlasImageButton(t.region(up), t.region(down), size, action);
        b.setPosition(x, y);
        return b;
    }

    private void goBack() { game.setScreen(new GreenhouseScreen(game)); }
}
