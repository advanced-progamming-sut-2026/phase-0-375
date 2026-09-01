package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import controller.CollectionMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.collection.Collection;
import model.enums.MenuType;
import model.user.User;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import view.gui.PvzGdxGame;
import view.gui.anim.PamClipCache;
import view.gui.anim.SpritesheetClipCache;
import view.gui.anim.zombie.SunshineAnim;
import view.gui.anim.zombie.ZombieAnimAdapter;
import view.gui.anim.zombie.ZombotanyAnim;
import view.gui.assets.AlmanacArt;
import view.gui.assets.PamCatalog;
import view.gui.assets.PlantSpritesheetCatalog;
import view.gui.assets.PvzAssets;
import view.gui.assets.ShopArt;
import view.gui.ui.AtlasImageButton;
import view.gui.ui.CollectionEntryOverlay;
import view.gui.ui.ResourceBar;

import java.util.Map;

/**
 * Full-page Almanac entry details. Uses {@link AlmanacArt#STAT_BG} as the card chrome.
 * Close returns to {@link CollectionScreen} on the same tab.
 */
public final class CollectionDetailScreen extends AbstractMenuScreen {
    private static final float CLOSE_SIZE = 76f;
    private static final float EDGE = 24f;
    private static final float CARD_W = 1100f;
    private static final float CARD_H = 820f;

    private final CollectionMenuController controller = CollectionMenuController.getInstance();
    private final PamClipCache clips;
    private final SpritesheetClipCache sheetClips;
    private final CollectionScreen.Tab returnTab;
    private final boolean plant;
    private final String entryName;

    private ResourceBar resources;
    private Label title;
    private Label body;
    private Label seedLabel;
    private TextButton upgradeBtn;
    private TextButton buyBtn;
    private IdlePreview preview;

    public CollectionDetailScreen(PvzGdxGame game, CollectionScreen.Tab returnTab,
                                  boolean plant, String entryName) {
        super(game);
        this.clips = new PamClipCache(game.assets.player);
        this.sheetClips = new SpritesheetClipCache(game.assets.root);
        this.returnTab = returnTab == null ? CollectionScreen.Tab.PLANTS : returnTab;
        this.plant = plant;
        this.entryName = entryName;
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.COLLECTION);
        TextureBank t = game.assets.textures;
        t.loadSync(AlmanacArt.ATLAS);
        t.loadSync(AlmanacArt.ATLAS_IMAGE);
        t.loadSync(AlmanacArt.ATLAS_SEED_PACKETS);
        t.loadSync("ATLASIMAGE_ATLAS_UI_SEEDPACKETS_768_00");
        t.loadSync(ShopArt.ATLAS_STORE);
        addResourceBar(t);
        stage.addActor(detailRoot(buildCard(t)));
        AtlasImageButton close = new AtlasImageButton(
                t.region(ShopArt.CLOSE), t.region(ShopArt.CLOSE_DOWN), CLOSE_SIZE, this::goBack);
        close.setPosition(UI_WIDTH - EDGE - CLOSE_SIZE, UI_HEIGHT - EDGE - CLOSE_SIZE - 8f);
        stage.addActor(close);
        close.toFront();
        fillContent();
    }

    private void addResourceBar(TextureBank t) {
        Table top = new Table();
        top.setFillParent(true);
        top.top().right();
        resources = new ResourceBar(skin, t);
        top.add(resources).pad(EDGE);
        stage.addActor(top);
        top.toFront();
    }

    private Table buildCard(TextureBank t) {
        Table card = new Table();
        applyCardBackground(card, t);
        card.pad(28f);
        title = new Label(entryName, skin, "big");
        title.setColor(Color.WHITE);
        title.setAlignment(Align.center);
        card.add(title).growX().padBottom(12f).row();
        preview = new IdlePreview(game.assets, clips, sheetClips);
        card.add(preview).size(260f, 260f).padBottom(10f).row();
        seedLabel = new Label("", skin, "medium");
        seedLabel.setColor(Color.WHITE);
        seedLabel.setAlignment(Align.center);
        card.add(seedLabel).growX().padBottom(8f).row();
        body = new Label("", skin, "secondary");
        body.setColor(Color.WHITE);
        body.setWrap(true);
        body.setAlignment(Align.topLeft);
        ScrollPane scroll = new ScrollPane(body, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        card.add(scroll).grow().padBottom(14f).row();
        card.add(actionButtons()).center();
        return card;
    }

    private void applyCardBackground(Table card, TextureBank t) {
        TextureRegion stat = t.region(AlmanacArt.STAT_BG);
        if (stat != null) {
            card.setBackground(new TextureRegionDrawable(stat));
            return;
        }
        TextureRegion fallback = t.region(AlmanacArt.PLANT_CARD);
        if (fallback != null) {
            card.setBackground(new TextureRegionDrawable(fallback));
        }
    }

    private Table actionButtons() {
        Table actions = new Table();
        upgradeBtn = new TextButton("Upgrade", skin, "purple");
        upgradeBtn.addListener(purchaseListener(() -> controller.upgradePlant(entryName)));
        buyBtn = new TextButton("Buy (" + controller.purchaseCostCoins() + ")", skin, "brown");
        buyBtn.addListener(purchaseListener(() -> controller.purchasePlant(entryName)));
        actions.add(upgradeBtn).width(200f).height(56f).padRight(12f);
        actions.add(buyBtn).width(220f).height(56f);
        return actions;
    }

    private ChangeListener purchaseListener(java.util.function.Supplier<CommandResult<Void>> action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = action.get();
                showPurchaseResult(r);
                if (r.isSuccess()) {
                    resources.refresh();
                    fillContent();
                }
            }
        };
    }

    private Table detailRoot(Table card) {
        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(CARD_W).height(CARD_H);
        return root;
    }

    private void fillContent() {
        if (plant) {
            fillPlant();
        } else {
            fillZombie();
        }
    }

    private void fillPlant() {
        CommandResult<String> r = controller.showPlant(entryName);
        title.setText(entryName);
        body.setText(r.isSuccess() ? r.getData() : r.getMessage());
        preview.setPlant(entryName);
        Collection col = controller.currentCollection();
        boolean owned = col.ownsPlant(entryName);
        upgradeBtn.setVisible(owned);
        upgradeBtn.setDisabled(!owned || col.getPlantLevel(entryName) >= Collection.MAX_PLANT_LEVEL);
        buyBtn.setVisible(!owned);
        buyBtn.setDisabled(owned);
        User user = App.getInstance().getCurrentUser();
        int have = user != null && user.getSeedPackets() != null
            ? user.getSeedPackets().getOrDefault(entryName, 0) : 0;
        if (owned) {
            seedLabel.setText("Seed packets: " + have
                + " / next upgrade needs " + col.getUpgradeSeedCost(entryName));
        } else {
            seedLabel.setText("Locked · purchase costs " + controller.purchaseCostCoins() + " coins");
        }
    }

    private void fillZombie() {
        Collection col = controller.currentCollection();
        boolean discovered = col.ownsZombie(entryName);
        upgradeBtn.setVisible(false);
        buyBtn.setVisible(false);
        if (!discovered) {
            title.setText("Undiscovered");
            body.setText("You have not seen this zombie in battle yet.");
            seedLabel.setText("");
            preview.clearEntity();
            return;
        }
        title.setText(entryName);
        CommandResult<String> r = controller.showZombie(entryName);
        body.setText(r.isSuccess() ? r.getData() : r.getMessage());
        seedLabel.setText("Discovered");
        preview.setZombie(entryName);
    }

    private void goBack() {
        game.setScreen(new CollectionScreen(game, returnTab));
    }

    @Override
    protected void onBack() {
        goBack();
    }

    @Override
    public void dispose() {
        if (sheetClips != null) {
            sheetClips.dispose();
        }
        super.dispose();
    }

    private static final class IdlePreview extends Actor {
        private final PamPlayer player;
        private final PamCatalog catalog;
        private final PlantSpritesheetCatalog sheets;
        private final PamClipCache clips;
        private final SpritesheetClipCache sheetClips;
        private String pamPath;
        private String clipName;
        private PlantSpritesheetCatalog.ClipSpec sheetSpec;
        private Map<String, Boolean> visibility;
        private String plantPamPath;
        private String plantClipName;
        private float sheetOffsetY;
        private float sheetScaleMul = 1f;
        private float time;

        private IdlePreview(PvzAssets assets, PamClipCache clips, SpritesheetClipCache sheetClips) {
            this.player = assets.player;
            this.catalog = assets.pamCatalog;
            this.sheets = assets.plantSheets;
            this.clips = clips;
            this.sheetClips = sheetClips;
        }

        void setPlant(String name) {
            pamPath = null;
            clipName = null;
            sheetSpec = null;
            visibility = null;
            plantPamPath = null;
            plantClipName = null;
            sheetOffsetY = 0f;
            sheetScaleMul = 1f;
            time = 0f;
            // Prefer installed spritesheets (e.g. Cat-tail) over missing PAM clips.
            // Full sheet loop for almanac preview (attack = all frames when available).
            if (sheets != null && sheets.hasSheets(name)) {
                sheetSpec = sheets.resolveClip(name, "attack");
                if (sheetSpec == null) {
                    sheetSpec = sheets.anyClip(name);
                }
                if (sheetSpec != null) {
                    if ("Cat-tail".equalsIgnoreCase(name)) {
                        sheetOffsetY = CollectionEntryOverlay.CATTAIL_PREVIEW_OFFSET_Y;
                        sheetScaleMul = CollectionEntryOverlay.CATTAIL_PREVIEW_SCALE;
                    }
                    return;
                }
            }
            PamCatalog.PamEntry entry = catalog.forPlant(name);
            if (entry == null) {
                return;
            }
            pamPath = entry.path();
            clipName = catalog.resolveClip(entry, "idle", "idle2", "idle1", "loop");
        }

        void setZombie(String name) {
            pamPath = null;
            clipName = null;
            sheetSpec = null;
            visibility = null;
            plantPamPath = null;
            plantClipName = null;
            sheetOffsetY = 0f;
            sheetScaleMul = 1f;
            time = 0f;
            if (sheets != null && sheets.hasSheets(name)) {
                sheetSpec = sheets.resolveClip(name, "idle", "walk");
                if (sheetSpec == null) {
                    sheetSpec = sheets.anyClip(name);
                }
                if (sheetSpec != null) {
                    if (SunshineAnim.isSunshineName(name)) {
                        sheetOffsetY = CollectionEntryOverlay.SUNSHINE_PREVIEW_OFFSET_Y;
                        sheetScaleMul = CollectionEntryOverlay.SUNSHINE_PREVIEW_SCALE;
                    }
                    return;
                }
            }
            PamCatalog.PamEntry entry = catalog.forZombie(name);
            if (entry == null) {
                return;
            }
            pamPath = entry.path();
            clipName = catalog.resolveClip(entry, "idle", "walk", "idle2", "loop");
            visibility = ZombieAnimAdapter.almanacArmorVisibility(name, entry);
            if (ZombotanyAnim.isPlantHeadName(name)) {
                visibility = ZombotanyAnim.headHiddenVisibility(visibility);
                String plantName = ZombotanyAnim.plantDefinitionName(name);
                PamCatalog.PamEntry plant = plantName == null ? null : catalog.forPlant(plantName);
                if (plant != null) {
                    plantPamPath = plant.path();
                    plantClipName = catalog.resolveClip(plant, "idle", "idle2", "idle1", "loop");
                }
            }
        }

        void clearEntity() {
            pamPath = null;
            clipName = null;
            sheetSpec = null;
            visibility = null;
            plantPamPath = null;
            plantClipName = null;
            sheetOffsetY = 0f;
            sheetScaleMul = 1f;
        }

        @Override public void act(float delta) {
            super.act(delta);
            time += Math.max(0f, delta);
        }

        @Override public void draw(Batch batch, float parentAlpha) {
            float a = batch.getColor().a * parentAlpha * getColor().a;
            batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, a);
            float cx = getX() + getWidth() * 0.5f;
            float cy = getY() + getHeight() * 0.15f + sheetOffsetY;
            if (sheetSpec != null && sheetClips != null) {
                SpritesheetClipCache.SheetAnim sheet = sheetClips.getOrLoad(sheetSpec);
                if (sheet != null && sheet.animation() != null) {
                    TextureRegion frame = sheet.animation().getKeyFrame(time, true);
                    if (frame != null) {
                        float scale = 0.55f * sheetScaleMul;
                        float w = frame.getRegionWidth() * scale;
                        float h = frame.getRegionHeight() * scale;
                        batch.draw(frame, cx - w * 0.5f, cy, w, h);
                    }
                    return;
                }
            }
            if (pamPath == null || clipName == null) {
                return;
            }
            ClipRef ref = clips.getOrLoad(pamPath, clipName);
            if (ref == null) {
                return;
            }
            float bodyScale = 0.7f;
            if (visibility != null) {
                player.draw(batch, ref, time, cx, cy, bodyScale, bodyScale, true, visibility);
            } else {
                player.draw(batch, ref, time, cx, cy, bodyScale, bodyScale, true);
            }
            drawZombotanyPlantHead(batch, ref, cx, cy, bodyScale);
        }

        private void drawZombotanyPlantHead(Batch batch, ClipRef bodyRef, float bodyX, float bodyY,
                                            float bodyScale) {
            if (plantPamPath == null || plantClipName == null) {
                return;
            }
            ClipRef plantRef = clips.getOrLoad(plantPamPath, plantClipName);
            if (plantRef == null) {
                return;
            }
            com.badlogic.gdx.math.Rectangle skull = null;
            for (String part : ZombotanyAnim.SKULL_PARTS) {
                skull = player.partBounds(bodyRef, time, part);
                if (skull != null) {
                    break;
                }
            }
            float[] xy = ZombotanyAnim.headWorldCenter(
                    skull, false, bodyX, bodyY, bodyScale, getHeight() * 0.2f);
            float headScale = bodyScale * ZombotanyAnim.HEAD_SCALE;
            player.draw(batch, plantRef, time, xy[0], xy[1], -headScale, headScale, true);
        }
    }
}
