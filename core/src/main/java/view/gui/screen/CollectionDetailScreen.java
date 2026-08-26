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
import view.gui.anim.zombie.ZombieAnimAdapter;
import view.gui.assets.AlmanacArt;
import view.gui.assets.PamCatalog;
import view.gui.assets.PvzAssets;
import view.gui.assets.ShopArt;
import view.gui.ui.AtlasImageButton;
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

        Table top = new Table();
        top.setFillParent(true);
        top.top().right();
        resources = new ResourceBar(skin, t);
        top.add(resources).pad(EDGE);
        stage.addActor(top);

        Table card = new Table();
        TextureRegion stat = t.region(AlmanacArt.STAT_BG);
        if (stat != null) {
            card.setBackground(new TextureRegionDrawable(stat));
        } else {
            TextureRegion fallback = t.region(AlmanacArt.PLANT_CARD);
            if (fallback != null) {
                card.setBackground(new TextureRegionDrawable(fallback));
            }
        }
        card.pad(28f);

        title = new Label(entryName, skin, "big");
        title.setColor(Color.WHITE);
        title.setAlignment(Align.center);
        card.add(title).growX().padBottom(12f).row();

        preview = new IdlePreview(game.assets, clips);
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

        Table actions = new Table();
        upgradeBtn = new TextButton("Upgrade", skin, "purple");
        upgradeBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.upgradePlant(entryName);
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    resources.refresh();
                    fillContent();
                }
            }
        });
        buyBtn = new TextButton("Buy (" + controller.purchaseCostCoins() + ")", skin, "brown");
        buyBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                CommandResult<Void> r = controller.purchasePlant(entryName);
                showToast(r.getMessage(), !r.isSuccess());
                if (r.isSuccess()) {
                    resources.refresh();
                    fillContent();
                }
            }
        });
        actions.add(upgradeBtn).width(200f).height(56f).padRight(12f);
        actions.add(buyBtn).width(220f).height(56f);
        card.add(actions).center();

        Table root = new Table();
        root.setFillParent(true);
        root.add(card).width(CARD_W).height(CARD_H);
        stage.addActor(root);

        AtlasImageButton close = new AtlasImageButton(
            t.region(ShopArt.CLOSE), t.region(ShopArt.CLOSE_DOWN), CLOSE_SIZE, this::goBack);
        close.setPosition(UI_WIDTH - EDGE - CLOSE_SIZE, UI_HEIGHT - EDGE - CLOSE_SIZE - 8f);
        stage.addActor(close);
        top.toFront();
        close.toFront();

        fillContent();
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

    private static final class IdlePreview extends Actor {
        private final PamPlayer player;
        private final PamCatalog catalog;
        private final PamClipCache clips;
        private String pamPath;
        private String clipName;
        private Map<String, Boolean> visibility;
        private float time;

        private IdlePreview(PvzAssets assets, PamClipCache clips) {
            this.player = assets.player;
            this.catalog = assets.pamCatalog;
            this.clips = clips;
        }

        void setPlant(String name) {
            pamPath = null;
            clipName = null;
            visibility = null;
            time = 0f;
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
            visibility = null;
            time = 0f;
            PamCatalog.PamEntry entry = catalog.forZombie(name);
            if (entry == null) {
                return;
            }
            pamPath = entry.path();
            clipName = catalog.resolveClip(entry, "idle", "walk", "idle2", "loop");
            visibility = ZombieAnimAdapter.almanacArmorVisibility(name, entry);
        }

        void clearEntity() {
            pamPath = null;
            clipName = null;
            visibility = null;
        }

        @Override public void act(float delta) {
            super.act(delta);
            time += Math.max(0f, delta);
        }

        @Override public void draw(Batch batch, float parentAlpha) {
            if (pamPath == null || clipName == null) {
                return;
            }
            ClipRef ref = clips.getOrLoad(pamPath, clipName);
            if (ref == null) {
                return;
            }
            float a = batch.getColor().a * parentAlpha * getColor().a;
            batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, a);
            if (visibility != null) {
                player.draw(batch, ref, time, getX() + getWidth() * 0.5f,
                    getY() + getHeight() * 0.15f, 0.7f, 0.7f, true, visibility);
            } else {
                player.draw(batch, ref, time, getX() + getWidth() * 0.5f,
                    getY() + getHeight() * 0.15f, 0.7f, 0.7f, true);
            }
        }
    }
}
