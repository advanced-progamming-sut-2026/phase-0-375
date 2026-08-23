package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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

        AtlasImageButton shop = hudButton(textures,
                AdventureHudRegions.STORE_NORMAL, AdventureHudRegions.STORE_DOWN,
                SHOP_ICON, UI_WIDTH - CORNER_PAD - SHOP_ICON, CORNER_PAD, this::openShop);
        stage.addActor(shop);

        refreshAll();
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
                openShop();
                return;
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
        showToast("Buy a 'Pot' from the shop to unlock the next slot.", false);
    }

    private void goBack() {
        CommandResult<Void> r = controller.menuExit();
        showToast(r.getMessage(), !r.isSuccess());
        if (r.isSuccess()) {
            game.setScreen(new AdventureScreen(game));
        }
    }

    private void refreshAll() {
        Greenhouse gh = Greenhouse.getInstance(App.getInstance().getCurrentUser());
        for (int row = 0; row < Greenhouse.ROWS; row++) {
            for (int col = 0; col < Greenhouse.COLS; col++) {
                Pot pot = gh.getPot(col + 1, row + 1);
                PotSlotActor slot = slots[row][col];
                slot.refresh(pot);
                slot.placeOnPlatform(cover, col, row);
            }
        }
        int[] next = gh.nextPotToUnlock();
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
