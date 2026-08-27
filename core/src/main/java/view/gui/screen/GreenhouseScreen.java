package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controller.GreenhouseMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.PotState;
import model.greenhouse.Greenhouse;
import model.greenhouse.Pot;
import pvz.libpvz.textures.TextureBank;
import view.gui.PvzGdxGame;
import view.gui.assets.AdventureHudRegions;
import view.gui.assets.PvzAssets;
import view.gui.assets.ShopArt;
import view.gui.assets.ZenGardenArt;
import view.gui.assets.ZenGardenLayout;
import view.gui.anim.PamClipCache;
import view.gui.ui.AtlasImageButton;
import view.gui.ui.PotSlotActor;
import view.gui.ui.ResourceBar;

/**
 * Greenhouse / Zen Garden: terracotta pots on painted platforms.
 */
public final class GreenhouseScreen extends AbstractMenuScreen {
    private static final float MAX_DELTA = 1f / 30f;
    private static final float CORNER_PAD = 40f;
    private static final float HUD_ICON = 100f;
    private static final float SHOP_ICON = 140f;
    /** Red sale ribbon under the shop button (same art as shop daily banner). */
    private static final float SALE_BANNER_W = 125f;
    private static final float SALE_BANNER_H = 44f;
    /** Offset from shop icon centre; +Y moves the banner up. */
    private static final float SALE_BANNER_SHIFT_X = -5f;
    private static final float SALE_BANNER_SHIFT_Y = 35f;
    private static final float SALE_BANNER_TEXT_SCALE = 1f;
    private static final float SALE_BANNER_TEXT_SHIFT_X = 0f;
    private static final float SALE_BANNER_TEXT_SHIFT_Y = 0f;

    private final GreenhouseMenuController controller = GreenhouseMenuController.getInstance();
    private final ZenGardenArt art = ZenGardenArt.create();

    private ResourceBar resourceBar;
    private Label summaryLabel;
    private final PotSlotActor[][] slots = new PotSlotActor[Greenhouse.ROWS][Greenhouse.COLS];
    private ZenGardenLayout.CoverTransform cover;
    private PamClipCache pamClips;
    private float timerAccum;

    public GreenhouseScreen(PvzGdxGame game) {
        super(game);
    }

    @Override
    public void show() {
        game.ensureAssets();
        art.ensureLoaded(game.assets.textures);
        App.getInstance().setCurrentMenu(MenuType.GREENHOUSE);
        Greenhouse.getInstance(App.getInstance().getCurrentUser());
        cover = new ZenGardenLayout.CoverTransform(UI_WIDTH, UI_HEIGHT);
        super.show();
    }

    @Override
    protected void buildUi() {
        TextureBank textures = game.assets.textures;

        Table top = new Table();
        top.setFillParent(true);
        top.setTouchable(Touchable.childrenOnly);
        top.top().right();
        resourceBar = new ResourceBar(skin, textures);
        top.add(resourceBar).pad(55f);
        stage.addActor(top);

        textures.loadSync(AdventureHudRegions.ATLAS_WORLD_MAP);
        textures.loadSync(AdventureHudRegions.ATLAS_ALWAYS_LOADED);

        AtlasImageButton back = hudButton(textures,
            AdventureHudRegions.BACK_NORMAL, AdventureHudRegions.BACK_DOWN,
            HUD_ICON, CORNER_PAD, UI_HEIGHT - CORNER_PAD - HUD_ICON, this::goBack);
        stage.addActor(back);

        summaryLabel = new Label("", skin, "medium");
        summaryLabel.setPosition(CORNER_PAD + HUD_ICON + 16f, UI_HEIGHT - CORNER_PAD - 48f);
        stage.addActor(summaryLabel);

        PotSlotActor.Listener listener = this::onPotAction;
        PvzAssets assets = game.assets;
        pamClips = new PamClipCache(assets.player);
        for (int row = 0; row < Greenhouse.ROWS; row++) {
            for (int col = 0; col < Greenhouse.COLS; col++) {
                int x = col + 1;
                int y = row + 1;
                PotSlotActor slot = new PotSlotActor(x, y, skin, assets, pamClips, listener);
                slots[row][col] = slot;
                slot.placeOnPlatform(cover, col, row);
                stage.addActor(slot);
            }
        }

        float shopX = UI_WIDTH - CORNER_PAD - SHOP_ICON;
        float shopY = CORNER_PAD;
        AtlasImageButton shop = hudButton(textures,
            AdventureHudRegions.STORE_NORMAL, AdventureHudRegions.STORE_DOWN,
            SHOP_ICON, shopX, shopY, this::openShop);
        stage.addActor(shop);
        stage.addActor(shopSaleBanner(textures, shopX, shopY));

        refreshAll();
    }

    /** Red "sale" ribbon parked under the greenhouse shop icon. */
    private Group shopSaleBanner(TextureBank t, float shopX, float shopY) {
        Group banner = new Group();
        banner.setSize(SALE_BANNER_W, SALE_BANNER_H);
        banner.setTouchable(Touchable.disabled);
        banner.setPosition(
            shopX + (SHOP_ICON - SALE_BANNER_W) * 0.5f + SALE_BANNER_SHIFT_X,
            shopY - SALE_BANNER_H * 0.55f + SALE_BANNER_SHIFT_Y);

        TextureRegion region = t.region(ShopArt.SALE_BANNER);
        if (region != null) {
            Image art = new Image(new TextureRegionDrawable(region));
            art.setScaling(Scaling.stretch);
            art.setBounds(0f, 0f, SALE_BANNER_W, SALE_BANNER_H);
            banner.addActor(art);
        }
        Label text = new Label("sale", skin, "medium");
        text.setColor(Color.WHITE);
        text.setAlignment(Align.center);
        text.setFontScale(SALE_BANNER_TEXT_SCALE);
        text.pack();
        text.setPosition(
            (SALE_BANNER_W - text.getWidth()) * 0.5f + SALE_BANNER_TEXT_SHIFT_X,
            (SALE_BANNER_H - text.getHeight()) * 0.5f + SALE_BANNER_TEXT_SHIFT_Y);
        banner.addActor(text);
        return banner;
    }

    private AtlasImageButton hudButton(TextureBank textures, String upId, String downId,
                                       float size, float x, float y, Runnable action) {
        TextureRegion up = textures.region(upId);
        TextureRegion down = textures.region(downId);
        AtlasImageButton button = new AtlasImageButton(up, down, size, action);
        button.setPosition(x, y);
        return button;
    }

    private void onPotAction(int x, int y, PotState state) {
        CommandResult<Void> result;
        switch (state) {
            case LOCKED -> {
                Greenhouse gh = Greenhouse.getInstance(App.getInstance().getCurrentUser());
                int[] next = gh.nextPotToUnlock();
                if (next == null) {
                    showToast("All pots are already unlocked.", false);
                    return;
                }
                if (next[0] != x || next[1] != y) {
                    showToast("Unlock pots in order. Next slot: (" + next[0] + "," + next[1] + ").", true);
                    return;
                }
                result = controller.buyPot();
            }
            case EMPTY -> result = controller.plantPot(x, y);
            case GROWING -> result = controller.grow(x, y);
            case READY -> result = controller.collect(x, y);
            default -> {
                return;
            }
        }
        showToast(result.getMessage(), !result.isSuccess());
        refreshAll();
    }

    private void openShop() {
        game.setScreen(new ShopScreen(game));
    }

    private void goBack() {
        CommandResult<Void> r = controller.menuExit();
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new AdventureScreen(game));
        }
    }

    @Override
    protected void onBack() {
        goBack();
    }

    private void refreshAll() {
        Greenhouse gh = Greenhouse.getInstance(App.getInstance().getCurrentUser());
        int[] next = gh.nextPotToUnlock();
        for (int row = 0; row < Greenhouse.ROWS; row++) {
            for (int col = 0; col < Greenhouse.COLS; col++) {
                int x = col + 1;
                int y = row + 1;
                Pot pot = gh.getPot(x, y);
                PotSlotActor slot = slots[row][col];
                boolean purchasable = next != null && next[0] == x && next[1] == y;
                slot.refresh(pot, purchasable);
                slot.placeOnPlatform(cover, col, row);
            }
        }
        String nextTxt = next == null
            ? "All pots unlocked"
            : "Next unlock: (" + next[0] + "," + next[1] + ")";
        summaryLabel.setText("Unlocked " + gh.getUnlockedPotCount() + "/" + Greenhouse.TOTAL_POTS
            + "  ·  Growing " + gh.getProducingCount()
            + "  ·  Ready " + gh.getReadyCount()
            + "  ·  " + nextTxt);
        if (resourceBar != null) {
            resourceBar.refresh();
        }
    }

    @Override
    public void render(float delta) {
        if (delta > MAX_DELTA) {
            delta = MAX_DELTA;
        }

        Gdx.gl.glClearColor(0.02f, 0.05f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        PvzAssets assets = game.assets;
        if (assets != null) {
            assets.textures.update();
            TextureRegion bg = assets.textures.region(ZenGardenArt.BACKGROUND);
            if (bg != null && cover != null) {
                game.batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
                game.batch.begin();
                game.batch.draw(bg, cover.originX, cover.originY, cover.drawW, cover.drawH);
                game.batch.end();
            }
        }

        timerAccum += delta;
        if (timerAccum >= 1f) {
            timerAccum = 0f;
            refreshAll();
        }

        stage.act(delta);
        stage.draw();
    }
}
