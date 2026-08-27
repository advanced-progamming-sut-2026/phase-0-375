package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import pvz.skin.BorderedTable;
import view.gui.ui.SeedPacketActor;
import view.gui.ui.SeedPacketComposite;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
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
import view.gui.assets.AdventureHudRegions;
import view.gui.assets.PvzAssets;
import view.gui.assets.ShopArt;
import view.gui.assets.UiRegions;
import view.gui.ui.AtlasImageButton;
import view.gui.ui.EdgeFadeOverlay;
import view.gui.ui.ResourceBar;
import view.gui.ui.RoundedRegionImage;
import view.gui.ui.PamEffectActor;
import view.gui.ui.ModalCard;
import view.gui.ui.ShopChosenPlantPicker;
import view.gui.anim.PamClipCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** PvZ2-inspired graphical shop backed by the existing Shop controller/model. */
public final class ShopScreen extends AbstractMenuScreen {
    private static final float MAX_DELTA = 1f / 30f;
    /** Same soft black letterbox as Adventure / game menu. */
    private static final float EDGE_FADE_H = 600f;
    private static final float PANEL_W = 1500f;
    private static final float PANEL_H = 600f;
    /** Distance from the bottom of the screen to the bottom of the panel. */
    private static final float PANEL_Y = 135f;
    /**
     * Lifts the whole shop box as one unit: brown frame, blue interior, the card
     * scroller inside it and the exit button all follow this single value.
     */
    private static final float PANEL_LIFT = 60f;
    /** Purchase success/error toast under the shop panel (+Y = higher). */
    private static final float STATUS_Y = 120f;
    /** Exit button: sits above the panel, offsets are from the panel's top-right. */
    private static final float CLOSE_SIZE = 76f;
    private static final float CLOSE_SHIFT_X = -1280f;
    private static final float CLOSE_SHIFT_Y = -2f;
    /** Thickness of the brown border drawn by BorderedTable, in UI units. */
    private static final float FRAME = 17f;
    /** Corner radius of the blue interior, matched to the frame's rounding. */
    private static final float CORNER = 26f;

    /** Card footprint inside the scrolling row. */
    private static final float CARD_W = 300f;
    private static final float CARD_H = 365f;
    /**
     * Artwork box per product: {width, height, shiftX, shiftY}.
     * shiftX is positive to the right, shiftY is positive downwards.
     * Every product has its own row, so they can be tuned independently.
     */
    private static final float[] ART_POT          = {115f, 115f, 0f, 10f};
    private static final float[] ART_PLANT_FOOD   = {90f, 90f, 0f, 10f};
    private static final float[] ART_SEED_RANDOM  = {154f, 103f, 0f, 10f};
    private static final float[] ART_SEED_CHOSEN  = {154f, 103f, 0f, 10f};
    private static final float[] ART_CONVERSION   = {130f, 125f, 0f, 10f};
    private static final float[] ART_DAILY        = {
        SeedPacketActor.PACKET_WIDTH * 1.23f,
        SeedPacketActor.PACKET_HEIGHT * 1.23f,
        0f, 10f
    };
    /** Coin-conversion artwork: size of the gem, arrow and coin icons. */
    private static final float CONV_GEM_W = 60f, CONV_GEM_H = 47f;
    private static final float CONV_ARROW_W = 60f, CONV_ARROW_H = 38f;
    private static final float CONV_COIN_W = 60f, CONV_COIN_H = 60f;
    /**
     * Fixed row slots. The artwork is centred inside ART_SLOT_* and the text rows
     * have locked heights, so changing any ART_* size never shifts the name,
     * description or price button of any card.
     */
    private static final float ART_SLOT_W = 240f;
    private static final float ART_SLOT_H = 148f;
    private static final float NAME_H = 46f;
    private static final float DESC_H = 58f;
    /** Daily-offer seed packet: display scale only; plant/frame ratio matches SeedPacketActor. */
    private static final float PACKET_SCALE = 1.23f;
    /** Purple price button (replaces the old BUY label). */
    private static final float BUY_W = 130f;
    private static final float BUY_H = 60f;
    /** Gap between the currency icon and the amount, in UI units. */
    private static final float PRICE_ICON_GAP = 8f;
    /** Font scale of the amount printed on the button. */
    private static final float PRICE_TEXT_SCALE = 1.23f;
    /** Moves icon + amount together inside the button (+X right, +Y up). */
    private static final float PRICE_CONTENT_SHIFT_X = 0f;
    private static final float PRICE_CONTENT_SHIFT_Y = 0f;
    /** Raises ONLY the amount text inside the button; the icon stays put. */
    private static final float PRICE_TEXT_LIFT = 3.5f;
    /** Raises the whole price button inside the card without resizing any row. */
    private static final float BUY_LIFT = 10f;
    /** Daily-offer sparkle: SCALE is its size, SHIFT moves its centre. */
    private static final float SPARKLE_SCALE = .37f;
    private static final float SPARKLE_SHIFT_X = 0f;
    private static final float SPARKLE_SHIFT_Y = 42f;
    /** How long the sparkle fades after the daily offer is bought. */
    private static final float SPARKLE_FADE_OUT = 0.55f;
    /**
     * Daily-offer sale banner, pinned to a corner of the daily card and rotated.
     * X/Y are the bottom-left of the banner inside the card, before rotation.
     * The asset is 122x42, so keep W/H near a 2.9 ratio to avoid stretching.
     */
    private static final String BANNER_TEXT = "20%";
    private static final float BANNER_W = 125f;
    private static final float BANNER_H = 44f;
    private static final float BANNER_X = -10f;
    private static final float BANNER_Y = 280f;
    private static final float BANNER_ROTATION = 45f;
    private static final float BANNER_TEXT_SCALE = 0.9f;
    private static final float BANNER_TEXT_SHIFT_X = 6f;
    private static final float BANNER_TEXT_SHIFT_Y = 0f;

    /**
     * Blue SKU banner on the shop frame's top-right.
     * SHIFT is relative to the panel's top-right corner (+X right, +Y up).
     * Natural asset is 140×34 — keep W/H near that ratio to avoid stretch.
     */
    private static final String SKU_BANNER_TEXT = "Best deals here!";
    private static final float SKU_BANNER_W = 240f;
    private static final float SKU_BANNER_H = 60f;
    private static final float SKU_BANNER_SHIFT_X = 10f;
    private static final float SKU_BANNER_SHIFT_Y = -100f;
    private static final float SKU_BANNER_TEXT_SCALE = 1.2f;
    private static final float SKU_BANNER_TEXT_SHIFT_X = 10f;
    private static final float SKU_BANNER_TEXT_SHIFT_Y = 3f;

    /**
     * Calendar corner ornaments: only the shop-facing bottom corner is rounded
     * so they don't cover the brown frame's curve. Radius in UI units.
     */
    private static final float CALENDAR_CORNER_RADIUS = 26f;

    /** "SHOP" title: position is bottom-left of the label; color is black by default. */
    private static final float TITLE_X = UI_WIDTH / 2f - 32f;
    private static final float TITLE_Y = UI_HEIGHT - 203f;
    private static final float TITLE_FONT_SCALE = 1.1f;

    /**
     * Free-floating decoration art. Each row is {@code {asset, x, y, scale}} with
     * x/y as the bottom-left corner on the 1920x1080 stage and scale 1 meaning
     * the asset's natural 768 size. Tweak the numbers to find the right spot;
     * an id missing from the loaded atlases is skipped instead of crashing.
     */
    private static final Deco[] DECORATIONS = {
        new Deco(ShopArt.DECO_TOP_FRAME,  490.0f,  690.0f, 1.00f), // x=490.0 y=690.0 scale=1.00
        new Deco(ShopArt.DECO_LOD_PINATA,   62.2f,  309.0f, 1.00f), // x=62.2 y=309.0 scale=1.00
        new Deco(ShopArt.DECO_COIN_STACK, 1771.1f,  239.1f, 1.00f), // x=1771.1 y=239.1 scale=1.00
        new Deco(ShopArt.DECO_LOD_BDAY_GIFT, 1827.8f,  229.9f, 0.81f), // x=1827.8 y=229.9 scale=0.81
        new Deco(ShopArt.DECO_CALENDAR_LEFT_TOMBTANGLER,  225.7f,  208.8f, 0.81f), // x=225.7 y=208.8 scale=0.81
        new Deco(ShopArt.DECO_CALENDAR_RIGHT_TOMBTANGLER, 1530.9f,  210.5f, 0.81f), // x=1530.9 y=210.5 scale=0.81
        new Deco(ShopArt.DECO_SUNFLOWER,  348.0f,  205.3f, 0.81f), // x=348.0 y=205.3 scale=0.81
    };
    /**
     * true draws the decorations over everything on this screen, false tucks them
     * behind the shop box. They are added as the last actors of buildUi(), so on
     * top they cover the frame, the cards and the exit button; only the toast
     * layer stays above them, because the base screen re-adds it after buildUi().
     */
    private static final boolean DECO_ON_TOP = true;
    /** Coin/gem icon drawn next to the price. */
    private static final float PRICE_ICON = 38f;

    private final ShopMenuController controller = ShopMenuController.getInstance();
    private final MainMenuArt menuArt = new MainMenuArt();
    private final Table cards = new Table();
    private EdgeFadeOverlay edgeFade;
    private TextureBank textures;
    private ResourceBar resources;
    private Label status;
    /** Set when rebuild should keep a fading sparkle on the sold-out daily card. */
    private boolean fadeDailySparkle;

    public ShopScreen(PvzGdxGame game) { super(game); }

    @Override
    public void show() {
        game.ensureAssets();
        TextureBank t = game.assets.textures;
        menuArt.ensureLoaded(t);
        t.loadSync(ShopArt.ATLAS_STORE);
        t.loadSync(ShopArt.ATLAS_CARDS);
        t.loadSync(ShopArt.ATLAS_SEEDS);
        t.loadSync(AdventureHudRegions.ATLAS_WORLD_MAP);
        t.loadSync(UiRegions.ATLAS_UI_ALWAYS_LOADED);   // sunflower, coin stack, top frame, LOD pinata
        t.loadSync(ShopArt.ATLAS_CALENDAR);             // calendar corner ornaments
        t.loadSync(ShopArt.ATLAS_LOD);                  // birthday gift
        if (edgeFade == null) {
            edgeFade = new EdgeFadeOverlay(EDGE_FADE_H);
        }
        super.show();
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.SHOP);
        TextureBank t = game.assets.textures;

        // Cosmic main-menu backdrop + edge fade are drawn in render(), not as stage actors.
        if (!DECO_ON_TOP) addDecorations(t);

        Table top = new Table();
        top.setFillParent(true);
        top.setTouchable(Touchable.childrenOnly);
        top.top().right();
        resources = new ResourceBar(skin, t);
        top.add(resources).pad(34f);
        stage.addActor(top);

        float panelX = (UI_WIDTH - PANEL_W) * 0.5f;
        float panelY = PANEL_Y + PANEL_LIFT;
        AtlasImageButton close = button(t, ShopArt.CLOSE, ShopArt.CLOSE_DOWN, CLOSE_SIZE,
            panelX + PANEL_W - CLOSE_SIZE + CLOSE_SHIFT_X,
            panelY + PANEL_H + CLOSE_SHIFT_Y, this::goBack);
        stage.addActor(close);

        status = new Label("", skin, "medium");
        status.setAlignment(com.badlogic.gdx.utils.Align.center);
        status.setPosition(UI_WIDTH / 2f - 360f, STATUS_Y);
        status.setWidth(720f);
        stage.addActor(status);

        textures = t;
        cards.defaults().pad(12f);
        populateCards();
        ScrollPane scroll = new ScrollPane(cards, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(false, true);
        scroll.setOverscroll(false, false);
        scroll.setClamp(true);

        // Brown border + cream fill come from one BorderedTable nine-patch.
        BorderedTable frame = new BorderedTable();
        frame.setBounds(0f, 0f, PANEL_W, PANEL_H);

        // Plain Group, NOT Stack: Stack.layout() rewrites every child to the full
        // panel rect on each validate, which is what silently discarded the inner
        // bounds below and let the cards scroll out over the frame.
        Group panel = new Group();
        panel.setSize(PANEL_W, PANEL_H);
        panel.setPosition(panelX, panelY);
        panel.addActor(frame);

        float innerW = PANEL_W - FRAME * 2f;
        float innerH = PANEL_H - FRAME * 2f;

        TextureRegion miniBg = t.region(ShopArt.MINISTORE_BG);
        if (miniBg != null) {
            // Rounded so it cannot cover the frame's curved corners.
            RoundedRegionImage inside = new RoundedRegionImage(paintedCore(miniBg), CORNER);
            inside.setBounds(FRAME, FRAME, innerW, innerH);
            inside.setTouchable(Touchable.disabled);
            panel.addActor(inside);
        }

        // ScrollPane scissor-clips its widget to its own bounds, so with the
        // bounds honoured the cards can never be painted over the frame.
        scroll.setBounds(FRAME, FRAME, innerW, innerH);
        panel.addActor(scroll);
        stage.addActor(panel);
        addSkuBanner(t, panelX, panelY);
        if (DECO_ON_TOP) addDecorations(t);

        // Title last so it paints above panel, banner and decorations.
        Label title = new Label("SHOP", skin, "big");
        title.setColor(Color.BLACK);
        title.setFontScale(TITLE_FONT_SCALE);
        title.setPosition(TITLE_X, TITLE_Y);
        stage.addActor(title);
    }

    /**
     * Blue SKU ribbon at the shop panel's top-right. Tune {@code SKU_BANNER_*}
     * knobs at the top of this class.
     */
    private void addSkuBanner(TextureBank t, float panelX, float panelY) {
        TextureRegion region = t.region(ShopArt.SKU_BANNER_BLUE);
        if (region == null) {
            return;
        }
        Group banner = new Group();
        banner.setSize(SKU_BANNER_W, SKU_BANNER_H);
        banner.setTouchable(Touchable.disabled);
        // Anchor: panel top-right, then apply SHIFT (+X right, +Y up).
        banner.setPosition(
            panelX + PANEL_W - SKU_BANNER_W + SKU_BANNER_SHIFT_X,
            panelY + PANEL_H + SKU_BANNER_SHIFT_Y);

        Image art = new Image(new TextureRegionDrawable(region));
        art.setScaling(Scaling.stretch);
        art.setBounds(0f, 0f, SKU_BANNER_W, SKU_BANNER_H);
        banner.addActor(art);

        Label text = new Label(SKU_BANNER_TEXT, skin, "medium");
        text.setColor(Color.BLACK);
        text.setAlignment(Align.center);
        text.setFontScale(SKU_BANNER_TEXT_SCALE);
        text.pack();
        // SHIFT is offset from the banner centre (+X right, +Y up).
        text.setPosition(
            (SKU_BANNER_W - text.getWidth()) * 0.5f + SKU_BANNER_TEXT_SHIFT_X,
            (SKU_BANNER_H - text.getHeight()) * 0.5f + SKU_BANNER_TEXT_SHIFT_Y);
        banner.addActor(text);
        stage.addActor(banner);
    }

    /**
     * IMAGE_UI_STORE_MINISTORE_BG ships with transparent padding baked into the
     * asset (~16% top/bottom, ~1% left/right). Stretching the raw region keeps
     * that padding on screen, so the cream frame fill shows through above and
     * below the artwork. Crop the padding off and stretch only the painted core.
     */
    private static TextureRegion paintedCore(TextureRegion src) {
        int cropX = Math.round(src.getRegionWidth() * 0.025f);
        int cropY = Math.round(src.getRegionHeight() * 0.175f);
        TextureRegion core = new TextureRegion(src);
        core.setRegion(src.getRegionX() + cropX, src.getRegionY() + cropY,
            src.getRegionWidth() - cropX * 2, src.getRegionHeight() - cropY * 2);
        return core;
    }

    /** (Re)builds the card row; called again after a purchase so state stays fresh. */
    private void populateCards() {
        cards.clear();
        Shop shop = controller.ensureDailyOffer();
        for (ShopItem item : shop.getPermanentItems()) {
            cards.add(card(item, textures)).width(CARD_W).height(CARD_H);
        }
        DailyOffer offer = shop.getDailyOffer();
        if (offer != null) {
            cards.add(dailyCard(offer, textures)).width(CARD_W).height(CARD_H);
        }
    }

    private Table card(ShopItem item, TextureBank t) {
        String bg = item.getItemType() == ShopItemType.CURRENCY_CONVERSION
            ? ShopArt.CARD_YELLOW
            : ShopArt.CARD_GREEN;
        return itemCard(item, t, bg, item.getPrice());
    }

    private Table dailyCard(DailyOffer offer, TextureBank t) {
        boolean soldOut = offer.isPurchased() || offer.isExpired();
        Table result = itemCard(offer.getItem(), t, ShopArt.CARD_PURPLE,
            offer.getDiscountedPrice(), offer.getItem().getTargetPlantType(), true, soldOut);
        result.addActor(saleBanner(t));   // kept visible once the offer is bought
        if (!soldOut) {
            result.addActor(sparkleActor());
        } else if (fadeDailySparkle) {
            fadeDailySparkle = false;
            PamEffectActor sparkle = sparkleActor();
            sparkle.addAction(Actions.sequence(
                Actions.fadeOut(SPARKLE_FADE_OUT),
                Actions.removeActor()));
            result.addActor(sparkle);
        }
        return result;
    }

    private PamEffectActor sparkleActor() {
        PamEffectActor sparkle = new PamEffectActor(game.assets.player, new PamClipCache(game.assets.player),
            "768/initial/UI/store/card_sparkle/card_sparkle.pam", "animation");
        sparkle.setBounds(SPARKLE_SHIFT_X, SPARKLE_SHIFT_Y, CARD_W, CARD_H);
        sparkle.setEffectScale(SPARKLE_SCALE);
        return sparkle;
    }

    private Table itemCard(ShopItem item, TextureBank t, String bgId, int price) {
        return itemCard(item, t, bgId, price, null, false, false);
    }

    private Table itemCard(ShopItem item, TextureBank t, String bgId, int price,
                           String plantOverride, boolean daily, boolean soldOut) {
        Table box = new Table();
        TextureRegion bg = t.region(bgId);
        if (bg != null) box.setBackground(new TextureRegionDrawable(bg));
        box.pad(18f);
        Actor art = artwork(item, t, plantOverride, daily);
        float[] artBox = artBox(item.getItemType(), daily);
        Container<Actor> artSlot = new Container<>(art);
        artSlot.setTouchable(Touchable.childrenOnly);
        artSlot.size(artBox[0], artBox[1]);
        artSlot.padLeft(artBox[2]).padRight(-artBox[2]);
        artSlot.padTop(artBox[3]).padBottom(-artBox[3]);
        box.add(artSlot).size(ART_SLOT_W, ART_SLOT_H).center().row();

        String title = daily ? "DAILY OFFER\n" + labelFor(item) : labelFor(item);
        Label name = new Label(title, skin, "medium");
        name.setColor(Color.WHITE);
        name.setWrap(true);
        name.setAlignment(com.badlogic.gdx.utils.Align.center);
        box.add(name).width(210f).height(NAME_H).padTop(6f).row();

        Label desc = new Label(item.getDescription(), skin, "secondary");
        desc.setColor(Color.WHITE);
        desc.setWrap(true);
        desc.setAlignment(com.badlogic.gdx.utils.Align.top | com.badlogic.gdx.utils.Align.center);
        box.add(desc).width(210f).height(DESC_H).row();

        if (soldOut) {
            Label sold = new Label("ALREADY BOUGHT TODAY", skin, "medium");
            sold.setColor(Color.LIGHT_GRAY);
            sold.setAlignment(com.badlogic.gdx.utils.Align.center);
            sold.setWrap(true);
            box.add(sold).width(210f).height(BUY_H).center()
                .padTop(6f - BUY_LIFT).padBottom(BUY_LIFT);
        } else {
            // Mirrored pads keep the row height identical while lifting the cell.
            box.add(priceButton(item, price, t)).width(BUY_W).height(BUY_H).center()
                .padTop(6f - BUY_LIFT).padBottom(BUY_LIFT);
        }
        return box;
    }

    /** One tunable decoration entry. */
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

    /** Adds every decoration at its natural size multiplied by its scale. */
    private void addDecorations(TextureBank t) {
        for (Deco deco : DECORATIONS) {
            if (isCalendarCorner(deco.id)) {
                addCalendarCorner(t, deco);
                continue;
            }
            TextureRegion region = t.region(deco.id);
            if (region == null) continue;   // not in the loaded atlases, skip quietly
            Image art = new Image(new TextureRegionDrawable(region));
            art.setSize(region.getRegionWidth() * deco.scale,
                region.getRegionHeight() * deco.scale);
            art.setPosition(deco.x, deco.y);
            art.setTouchable(Touchable.disabled);
            stage.addActor(art);
        }
    }

    private static boolean isCalendarCorner(String id) {
        return ShopArt.DECO_CALENDAR_LEFT_TOMBTANGLER.equals(id)
            || ShopArt.DECO_CALENDAR_RIGHT_TOMBTANGLER.equals(id);
    }

    /**
     * Left ornament: round bottom-left only. Right ornament: round bottom-right only.
     * Matches the shop frame curve so the brown corner stays visible.
     */
    private void addCalendarCorner(TextureBank t, Deco deco) {
        TextureRegion region = t.region(deco.id);
        if (region == null) return;
        boolean left = ShopArt.DECO_CALENDAR_LEFT_TOMBTANGLER.equals(deco.id);
        // tl, tr, bl, br
        RoundedRegionImage art = new RoundedRegionImage(region, CALENDAR_CORNER_RADIUS,
            false, false, left, !left);
        art.setSize(region.getRegionWidth() * deco.scale,
            region.getRegionHeight() * deco.scale);
        art.setPosition(deco.x, deco.y);
        art.setTouchable(Touchable.disabled);
        stage.addActor(art);
    }

    /**
     * Rotated "20%" ribbon for the daily card. A Group only honours setRotation
     * while transform is enabled, and it rotates around its origin, so the
     * origin is centred and the banner is placed by its own bottom-left corner.
     */
    private Group saleBanner(TextureBank t) {
        Group banner = new Group();
        banner.setSize(BANNER_W, BANNER_H);
        banner.setTransform(true);
        banner.setOrigin(Align.center);
        banner.setRotation(BANNER_ROTATION);
        banner.setPosition(BANNER_X, BANNER_Y);
        banner.setTouchable(Touchable.disabled);

        TextureRegion region = t.region(ShopArt.SALE_BANNER);
        if (region != null) {
            Image art = new Image(new TextureRegionDrawable(region));
            art.setScaling(Scaling.stretch);
            art.setBounds(0f, 0f, BANNER_W, BANNER_H);
            banner.addActor(art);
        }
        Label percent = new Label(BANNER_TEXT, skin, "medium");
        percent.setColor(Color.WHITE);
        percent.setAlignment(Align.center);
        percent.setFontScale(BANNER_TEXT_SCALE);
        percent.pack();
        // SHIFT is offset from the ribbon centre (+X right, +Y up in local space).
        percent.setPosition(
            (BANNER_W - percent.getWidth()) * 0.5f + BANNER_TEXT_SHIFT_X,
            (BANNER_H - percent.getHeight()) * 0.5f + BANNER_TEXT_SHIFT_Y);
        banner.addActor(percent);
        return banner;
    }

    /** Artwork box for one product: {width, height, shiftX, shiftY}. */
    private static float[] artBox(ShopItemType type, boolean daily) {
        if (daily) return ART_DAILY;
        return switch (type) {
            case POT -> ART_POT;
            case PLANT_FOOD -> ART_PLANT_FOOD;
            case SEED_PACKET_RANDOM -> ART_SEED_RANDOM;
            case SEED_PACKET_CHOSEN -> ART_SEED_CHOSEN;
            case CURRENCY_CONVERSION -> ART_CONVERSION;
        };
    }

    /** Purple button whose face is the currency icon plus the price. */
    private TextButton priceButton(ShopItem item, int price, TextureBank t) {
        TextButton button = new TextButton(String.valueOf(price), skin, "purple");
        Label amount = button.getLabel();
        amount.setFontScale(PRICE_TEXT_SCALE);
        TextureRegion icon = t.region(item.getCurrency() == CurrencyType.COIN ? ShopArt.COIN : ShopArt.GEM);
        button.clearChildren();
        if (icon != null) {
            Image coin = new Image(new TextureRegionDrawable(icon));
            coin.setTouchable(Touchable.disabled);
            button.add(coin).size(PRICE_ICON, PRICE_ICON).padRight(PRICE_ICON_GAP);
        }
        // Only the text cell is lifted, so the icon keeps its own baseline.
        button.add(amount).padBottom(PRICE_TEXT_LIFT).padTop(-PRICE_TEXT_LIFT);
        // Table content is centred, so a mirrored pad pair shifts it as a block.
        button.padLeft(PRICE_CONTENT_SHIFT_X).padRight(-PRICE_CONTENT_SHIFT_X);
        button.padBottom(PRICE_CONTENT_SHIFT_Y).padTop(-PRICE_CONTENT_SHIFT_Y);
        button.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                // Permanent chosen SKU needs a plant pick; daily already has a plant.
                if (item.getId() == Shop.ITEM_ID_SEED_CHOSEN) {
                    openChosenPlantPicker(item);
                } else {
                    confirmPurchase(item, item.getTargetPlantType(), 1);
                }
            }
        });
        return button;
    }

    /** Doc: chosen seed packets require picking an unlocked plant before buy. */
    private void openChosenPlantPicker(ShopItem item) {
        Set<String> unlocked = App.getInstance().getCurrentUser() == null
            ? null
            : App.getInstance().getCurrentUser().getUnlockedPlants();
        List<String> plants = unlocked == null ? List.of() : new ArrayList<>(unlocked);
        if (plants.isEmpty()) {
            status.setText("No unlocked plants to buy packets for.");
            status.setColor(Color.SCARLET);
            return;
        }
        ShopChosenPlantPicker.open(stage, skin, textures, plants, plant ->
            confirmPurchase(item, plant, 1));
    }

    private void confirmPurchase(ShopItem item, String targetPlant, int count) {
        String detail = confirmLabel(item, targetPlant);
        stage.addActor(ModalCard.confirm(skin, "Confirm purchase",
            "Are you sure you want to buy this?\n" + detail,
            "Buy",
            () -> purchase(item.getId(), targetPlant, count)));
    }

    private static String confirmLabel(ShopItem item, String targetPlant) {
        if (item.getId() == Shop.ITEM_ID_DAILY_OFFER) {
            return targetPlant == null ? "Daily Offer" : "Daily Offer — " + targetPlant;
        }
        String base = labelFor(item);
        return targetPlant == null || targetPlant.isEmpty() ? base : base + " — " + targetPlant;
    }

    private Actor artwork(ShopItem item, TextureBank t, String plantOverride, boolean daily) {
        if (daily && plantOverride != null) {
            return SeedPacketComposite.matchingActor(t, plantOverride)
                .setCompositeScale(PACKET_SCALE);
        }
        String id = switch (item.getItemType()) {
            case POT -> ShopArt.POT;
            case PLANT_FOOD -> "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON";
            case SEED_PACKET_RANDOM -> ShopArt.SEED_ICON;
            case SEED_PACKET_CHOSEN -> ShopArt.SELECTED_PACKET;
            case CURRENCY_CONVERSION -> ShopArt.COIN;
        };
        if (item.getItemType() == ShopItemType.CURRENCY_CONVERSION) {
            Stack conversion = new Stack();
            Table row = new Table();
            TextureRegion coin = t.region(ShopArt.COIN);
            TextureRegion arrow = t.region(ShopArt.CONVERT_ARROW);
            TextureRegion gem = t.region(ShopArt.GEM);
            if (gem != null) row.add(new Image(new TextureRegionDrawable(gem))).size(CONV_GEM_W, CONV_GEM_H);
            if (arrow != null) row.add(new Image(new TextureRegionDrawable(arrow))).size(CONV_ARROW_W, CONV_ARROW_H).pad(4f);
            if (coin != null) row.add(new Image(new TextureRegionDrawable(coin))).size(CONV_COIN_W, CONV_COIN_H);
            conversion.add(row);
            return conversion;
        }
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
        if (result.isSuccess()) {
            resources.refresh();
            if (id == Shop.ITEM_ID_DAILY_OFFER) {
                fadeDailySparkle = true;
            }
            populateCards();
        }
    }

    private AtlasImageButton button(TextureBank t, String up, String down, float size, float x, float y, Runnable action) {
        AtlasImageButton b = new AtlasImageButton(t.region(up), t.region(down), size, action);
        b.setPosition(x, y);
        return b;
    }

    private void goBack() { game.setScreen(new GreenhouseScreen(game)); }

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
