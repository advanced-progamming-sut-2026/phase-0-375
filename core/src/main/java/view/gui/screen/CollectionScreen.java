package view.gui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import controller.CollectionMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.collection.Collection;
import model.enums.MenuType;
import model.enums.PlantCategory;
import model.plant.definition.Plant;
import model.user.User;
import model.zombie.definition.Zombie;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.BorderedTable;
import view.gui.PvzGdxGame;
import view.gui.anim.PamClipCache;
import view.gui.anim.SpritesheetClipCache;
import view.gui.assets.AlmanacArt;
import view.gui.assets.AlmanacZombiePacketIds;
import view.gui.assets.PlantSpritesheetCatalog;
import view.gui.assets.PvzAssets;
import view.gui.assets.SeedPacketIds;
import view.gui.assets.SheetPacketPortraits;
import view.gui.assets.ShopArt;
import view.gui.assets.UiRegions;
import view.gui.anim.zombie.SunshineAnim;
import view.gui.ui.AtlasImageButton;
import view.gui.ui.CollectionEntryOverlay;
import view.gui.ui.EdgeFadeOverlay;
import view.gui.ui.PauseMenuOverlay;
import view.gui.ui.ResourceBar;
import view.gui.ui.RoundedRegionImage;
import view.gui.ui.SeedPacketActor;
import view.gui.ui.SkinFonts;
import view.gui.ui.ZombieAlmanacPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Almanac list (plants / zombies). Brown shop-style frame over the main-menu
 * cosmic backdrop (same edge fade as Adventure). Entry details open as a modal
 * overlay ({@link view.gui.ui.CollectionEntryOverlay}), not a full screen.
 *
 * <p>Layout knobs (edit these):
 * <ul>
 *   <li>{@link #TOP_RESERVE} — empty space above the brown frame</li>
 *   <li>{@link #FRAME_BLEED} — how far L/R/bottom hang off-screen (hides those borders)</li>
 *   <li>{@link #TAB_SHIFT_X}/{@link #TAB_SHIFT_Y} — move plant/zombie markers (+X right, +Y up)</li>
 *   <li>{@link #CLOSE_SHIFT_X}/{@link #CLOSE_SHIFT_Y} — move the X button</li>
 *   <li>{@link #FILTER_ROW_H} — filter strip height (padding around controls)</li>
 *   <li>{@link #FILTER_BTN_W}/{@link #FILTER_BTN_H} — status filter button size</li>
 *   <li>{@link #FILTER_FAMILY_W}/{@link #FILTER_FAMILY_H} — family SelectBox size</li>
 *   <li>{@link #PLANT_META_INK} — plant name / level / seed-count color</li>
 *   <li>{@link #SEED_BAR_SCALE} / {@link #SEED_BAR_H} / {@link #SEED_BAR_PAD_TOP} —
 *       seed ProgressBar ({@code xp_yellow} / {@code xp_green}); scale shrinks skin art</li>
 *   <li>{@link #ZOMBIE_CELL_PAD} / row pad — spacing between zombie cards</li>
 *   <li>{@link #PACKET_SCALE} — plant/zombie card scale vs native art</li>
 *   <li>{@link #GRAD_TOP} / {@link #GRAD_BOTTOM} — inner brown gradient</li>
 * </ul>
 */
public final class CollectionScreen extends AbstractMenuScreen {
    enum Tab { PLANTS, ZOMBIES }

    private enum PlantFilter { ALL, UNLOCKED, LOCKED, UPGRADABLE }

    private static final float PACKET_SCALE = 1.2f;
    private static final float PACKET_W = SeedPacketActor.PACKET_WIDTH * PACKET_SCALE;
    private static final float PACKET_H = SeedPacketActor.PACKET_HEIGHT * PACKET_SCALE;
    private static final float ZOMBIE_PACKET_SCALE = 1.45f;
    private static final float ZOMBIE_PACKET_W = 119f * ZOMBIE_PACKET_SCALE;
    private static final float ZOMBIE_PACKET_H = 95f * ZOMBIE_PACKET_SCALE;
    private static final int GRID_COLS = 8;
    private static final int ZOMBIE_GRID_COLS = 7;
    private static final float CLOSE_SIZE = 76f;
    private static final float EDGE = 24f;
    /**
     * Overall height of the plant-filter strip (space around the controls).
     * Does not resize the buttons / SelectBox — those use FILTER_BTN_* / FILTER_FAMILY_*.
     */
    private static final float FILTER_ROW_H = 90f;
    /** All / Unlocked / Locked / Upgradable button size. */
    private static final float FILTER_BTN_W = 130f;
    private static final float FILTER_BTN_H = 44f;
    /** Family SelectBox size. */
    private static final float FILTER_FAMILY_W = 240f;
    private static final float FILTER_FAMILY_H = 40f;
    /**
     * Seed-upgrade ProgressBar under each plant card.
     * Skin nine-patches ignore a smaller cell height — shrink with {@link #SEED_BAR_SCALE}.
     */
    public static float SEED_BAR_SCALE = 0.9f;
    /** Unscaled bar height before {@link #SEED_BAR_SCALE}. */
    public static float SEED_BAR_H = 16f;
    public static float SEED_BAR_PAD_TOP = 2f;
    /** Incomplete seed bar (PvzSkin). */
    private static final String SEED_BAR_GATHERING = "xp_yellow";
    /** Enough seeds for next upgrade (PvzSkin). */
    private static final String SEED_BAR_READY = "xp_green";
    /** Horizontal gap between zombie cards. */
    private static final float ZOMBIE_CELL_PAD = 1f;
    /** Extra vertical gap between zombie grid rows. */
    private static final float ZOMBIE_ROW_PAD = 14f;
    /** Fixed footer under each plant packet so row cards stay aligned. */
    private static final float PLANT_META_H = 58f;
    /**
     * Name / Lv / seed-count text under plant cards.
     * Soft parchment — readable on brown, less glare than pure white.
     */
    private static final Color PLANT_META_INK = new Color(0.88f, 0.82f, 0.68f, 1f);
    /** Empty strip above the brown collection frame. */
    private static final float TOP_RESERVE = 180f;
    /**
     * Push the brown nine-patch past the left/right/bottom screen edges so only
     * the top rim stays visible. Prefer this over shrinking the panel — content
     * still fills the viewport while side/bottom borders sit off-screen.
     */
    private static final float FRAME_BLEED = 48f;
    private static final float FRAME = 17f;
    private static final float CORNER = 26f;
    private static final float MAX_DELTA = 1f / 30f;
    private static final float TAB_GAP = 10f;

    /** Native 768 almanac tab slot (ACTIVE is 80×106; DOWN is 80×80). */
    private static final float TAB_SLOT_W = 80f;
    private static final float TAB_SLOT_H = 106f;
    /** Added to default tab anchor (+X right, +Y up). */
    private static final float TAB_SHIFT_X = 40f;
    private static final float TAB_SHIFT_Y = 58f;
    /** Added to default close-button anchor (+X right, +Y up). */
    private static final float CLOSE_SHIFT_X = -90f;
    private static final float CLOSE_SHIFT_Y = -83f;

    /** Sunflower on the green plants tab (relative to tab slot). */
    private static final float PLANT_ICON_W = 56f;
    private static final float PLANT_ICON_H = 46f;
    private static final float PLANT_ICON_X = 12f;
    private static final float PLANT_ICON_Y = 42f;

    /** Store zombie tab icon on the purple zombies tab (native 66×66). */
    private static final float ZOMBIE_ICON_W = 52f;
    private static final float ZOMBIE_ICON_H = 52f;
    private static final float ZOMBIE_ICON_X = 14f;
    private static final float ZOMBIE_ICON_Y = 38f;

    private static final Color INK = new Color(0.15f, 0.12f, 0.15f, 1f);
    /** Inner board gradient — darken here. */
    private static final Color GRAD_TOP = new Color(0.30f, 0.16f, 0.07f, 1f);
    private static final Color GRAD_BOTTOM = new Color(0.12f, 0.05f, 0.02f, 1f);

    private final CollectionMenuController controller = CollectionMenuController.getInstance();

    private Tab tab;
    private PlantFilter filter = PlantFilter.ALL;
    private PlantCategory categoryFilter;

    private TextureBank textures;
    private Texture boardGradient;
    private static final float EDGE_FADE_H = 600f;

    private final MainMenuArt menuArt = new MainMenuArt();
    private EdgeFadeOverlay edgeFade;
    private PamClipCache pamClips;
    private SpritesheetClipCache sheetClips;
    private ResourceBar resources;
    private Table grid;
    private Label statusLabel;
    private Table filterRow;
    private SelectBox<String> familyBox;
    private final Array<TextButton> statusButtons = new Array<>();
    private TabMarker plantsTab;
    private TabMarker zombiesTab;
    private final ArrayList<String> visiblePlantNames = new ArrayList<>();
    private final ArrayList<String> visibleZombieNames = new ArrayList<>();

    public CollectionScreen(PvzGdxGame game) {
        this(game, Tab.PLANTS);
    }

    public CollectionScreen(PvzGdxGame game, Tab tab) {
        super(game);
        this.tab = tab == null ? Tab.PLANTS : tab;
    }

    @Override
    public void show() {
        game.ensureAssets();
        if (pamClips == null) {
            pamClips = new PamClipCache(game.assets.player);
        }
        if (sheetClips == null && game.assets != null && game.assets.root != null) {
            sheetClips = new SpritesheetClipCache(game.assets.root);
        }
        menuArt.ensureLoaded(game.assets.textures);
        if (edgeFade == null) {
            edgeFade = new EdgeFadeOverlay(EDGE_FADE_H);
        }
        super.show();
    }

    @Override
    protected void buildUi() {
        App.getInstance().setCurrentMenu(MenuType.COLLECTION);
        textures = game.assets.textures;
        TextureBank t = textures;
        t.loadSync(AlmanacArt.ATLAS);
        t.loadSync(AlmanacArt.ATLAS_IMAGE);
        t.loadSync(AlmanacArt.ATLAS_SEED_PACKETS);
        t.loadSync("ATLASIMAGE_ATLAS_UI_SEEDPACKETS_768_00");
        t.loadSync(UiRegions.ATLAS_UI_ALWAYS_LOADED);
        t.loadSync("UI_AlwaysLoaded_Uncompressed_768");
        t.loadSync(PauseMenuOverlay.ATLAS_GROUP);
        t.loadSync(PauseMenuOverlay.ATLAS_PAGE);
        t.loadSync(AlmanacArt.ATLAS_COMMON_TABS);
        t.loadSync(ShopArt.ATLAS_STORE);
        t.loadSync(AlmanacArt.ATLAS_STAT_ICONS);
        t.loadSync(AlmanacArt.ATLAS_GRADIENTS);

        // Visible board height; panel itself is larger so L/R/bottom borders hang off-screen.
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
        // Only round the top corners (the ones that stay on-screen).
        RoundedRegionImage inside = new RoundedRegionImage(
            new TextureRegion(boardGradient), CORNER, true, true, false, false);
        inside.setBounds(FRAME, FRAME, innerW, innerH);
        inside.setTouchable(Touchable.disabled);
        panel.addActor(inside);

        Table content = new Table();
        content.pad(12f, 16f, 12f, 16f);

        // Grid + status must exist before filterBar(): setChecked/SelectBox fire
        // ChangeEvents that call refreshGrid during construction.
        grid = new Table();
        statusLabel = new Label("", skin, "medium");
        statusLabel.setColor(PLANT_META_INK);
        statusLabel.setAlignment(Align.right);

        filterRow = filterBar();
        content.add(filterRow).growX().height(FILTER_ROW_H).padBottom(8f).row();

        ScrollPane scroll = new ScrollPane(grid, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        content.add(scroll).grow().padBottom(8f).row();
        content.add(statusLabel).growX().right();

        // Content fills the on-screen rect; top FRAME strip stays free for the brown rim.
        content.setBounds(FRAME_BLEED, FRAME_BLEED, UI_WIDTH, visibleH - FRAME);
        panel.addActor(content);
        stage.addActor(panel);

        plantsTab = new TabMarker(
            t.region(AlmanacArt.TAB_PLANT_ICON),
            PLANT_ICON_W, PLANT_ICON_H, PLANT_ICON_X, PLANT_ICON_Y,
            () -> setTab(Tab.PLANTS));
        zombiesTab = new TabMarker(
            t.region(AlmanacArt.TAB_ZOMBIE_ICON),
            ZOMBIE_ICON_W, ZOMBIE_ICON_H, ZOMBIE_ICON_X, ZOMBIE_ICON_Y,
            () -> setTab(Tab.ZOMBIES));
        // Default: sit on the visible top rim; nudge with TAB_SHIFT_*.
        float tabX = EDGE + TAB_SHIFT_X;
        float tabY = visibleH - TAB_SLOT_H + 18f + TAB_SHIFT_Y;
        plantsTab.setPosition(tabX, tabY);
        zombiesTab.setPosition(tabX + TAB_SLOT_W + TAB_GAP, tabY);
        stage.addActor(plantsTab);
        stage.addActor(zombiesTab);
        refreshTabs();

        AtlasImageButton close = new AtlasImageButton(
            t.region(ShopArt.CLOSE), t.region(ShopArt.CLOSE_DOWN), CLOSE_SIZE, this::goBack);
        close.setPosition(
            UI_WIDTH - EDGE - CLOSE_SIZE + CLOSE_SHIFT_X,
            UI_HEIGHT - EDGE - CLOSE_SIZE + CLOSE_SHIFT_Y);
        stage.addActor(close);

        top.toFront();
        close.toFront();
        plantsTab.toFront();
        zombiesTab.toFront();

        refreshGrid();
    }

    private Table filterBar() {
        Table bar = new Table();
        statusButtons.clear();
        bar.add(statusButton("All", PlantFilter.ALL))
            .width(FILTER_BTN_W).height(FILTER_BTN_H).padRight(6f);
        bar.add(statusButton("Unlocked", PlantFilter.UNLOCKED))
            .width(FILTER_BTN_W).height(FILTER_BTN_H).padRight(6f);
        bar.add(statusButton("Locked", PlantFilter.LOCKED))
            .width(FILTER_BTN_W).height(FILTER_BTN_H).padRight(6f);
        bar.add(statusButton("Upgradable", PlantFilter.UPGRADABLE))
            .width(FILTER_BTN_W).height(FILTER_BTN_H).padRight(20f);
        restyleStatusButtons();

        Label familyLbl = new Label("Family:", skin, "secondary");
        familyLbl.setColor(PLANT_META_INK);
        bar.add(familyLbl).padRight(8f);

        familyBox = new SelectBox<>(skin);
        Array<String> families = new Array<>();
        families.add("Any");
        for (PlantCategory cat : PlantCategory.values()) {
            families.add(familyLabel(cat));
        }
        familyBox.setItems(families);
        familyBox.setSelectedIndex(categoryFilter == null ? 0 : categoryFilter.ordinal() + 1);
        familyBox.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                int index = familyBox.getSelectedIndex();
                categoryFilter = index <= 0 ? null : PlantCategory.values()[index - 1];
                refreshGrid();
            }
        });
        bar.add(familyBox).width(FILTER_FAMILY_W).height(FILTER_FAMILY_H);
        return bar;
    }

    private TextButton statusButton(String label, PlantFilter f) {
        TextButton b = new TextButton(label, skin, "purple");
        SkinFonts.scaleButton(b, skin, "purple", 0.85f);
        b.setUserObject(f);
        // ClickListener (not ButtonGroup/checked): purple TextButtons don't toggle reliably.
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                filter = f;
                restyleStatusButtons();
                refreshGrid();
            }
        });
        statusButtons.add(b);
        return b;
    }

    private void restyleStatusButtons() {
        for (TextButton button : statusButtons) {
            PlantFilter buttonFilter = (PlantFilter) button.getUserObject();
            String style = buttonFilter == filter ? "green" : "purple";
            button.setStyle(skin.get(style, TextButton.TextButtonStyle.class));
        }
    }

    private Label metaLabel(String text, String styleName) {
        Label.LabelStyle base = skin.get(styleName, Label.LabelStyle.class);
        Label.LabelStyle style = new Label.LabelStyle(base);
        style.fontColor = PLANT_META_INK.cpy();
        return new Label(text, style);
    }

    /**
     * Scaled xp ProgressBar. Skin drawables keep a min height, so Table
     * {@code .height(...)} alone cannot shrink them — parent Group scale does.
     */
    private Actor seedProgressBar(float have, float need) {
        String styleName = have >= need ? SEED_BAR_READY : SEED_BAR_GATHERING;
        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle(
            skin.get(styleName, ProgressBar.ProgressBarStyle.class));
        ProgressBar bar = new ProgressBar(0f, need, 1f, false, style);
        bar.setAnimateDuration(0f);
        bar.setValue(Math.min(have, need));
        bar.setTouchable(Touchable.disabled);
        bar.setSize(PACKET_W, SEED_BAR_H);

        // ProgressBar is a Widget — scale only applies via a transforming Group parent.
        Group scaled = new Group();
        scaled.setTransform(true);
        scaled.setSize(PACKET_W, SEED_BAR_H);
        scaled.setOrigin(PACKET_W * 0.5f, SEED_BAR_H * 0.5f);
        scaled.setScale(SEED_BAR_SCALE);
        scaled.addActor(bar);

        float scaledH = SEED_BAR_H * SEED_BAR_SCALE;
        Group slot = new Group();
        slot.setSize(PACKET_W, scaledH);
        scaled.setPosition(0f, (scaledH - SEED_BAR_H) * 0.5f);
        slot.addActor(scaled);
        return slot;
    }

    private void setTab(Tab next) {
        tab = next;
        refreshTabs();
        refreshGrid();
    }

    /** Active = ACTIVE art; inactive = DOWN. Slot position never moves. */
    private void refreshTabs() {
        boolean plants = tab == Tab.PLANTS;
        plantsTab.setFace(textures.region(
            plants ? AlmanacArt.TAB_PLANTS : AlmanacArt.TAB_PLANTS_DOWN));
        zombiesTab.setFace(textures.region(
            plants ? AlmanacArt.TAB_ZOMBIES_DOWN : AlmanacArt.TAB_ZOMBIES));
    }

    private void refreshGrid() {
        if (grid == null || statusLabel == null) {
            return;
        }
        grid.clear();
        Collection col = controller.currentCollection();
        if (filterRow != null) {
            filterRow.setVisible(tab == Tab.PLANTS);
        }
        if (tab == Tab.PLANTS) {
            refreshPlantGrid(col);
        } else {
            refreshZombieGrid(col);
        }
    }

    private void refreshPlantGrid(Collection col) {
        TextureBank t = textures;
        List<Plant> plants = new ArrayList<>(col.getAllPlants());
        plants.sort(Comparator.comparing(Plant::getName, String.CASE_INSENSITIVE_ORDER));
        User user = App.getInstance().getCurrentUser();
        visiblePlantNames.clear();
        int ownedCount = 0;
        int shown = 0;
        int colIndex = 0;
        for (Plant plant : plants) {
            if (col.ownsPlant(plant.getName())) {
                ownedCount++;
            }
            if (!passesFilter(col, user, plant)) {
                continue;
            }
            String name = plant.getName();
            visiblePlantNames.add(name);
            boolean owned = col.ownsPlant(name);
            int level = col.getPlantLevel(name);
            int have = seedCount(user, name);
            int need = owned ? col.getUpgradeSeedCost(name) : 0;

            Table cell = new Table();
            cell.top();
            SeedPacketActor packet = new SeedPacketActor(
                t, skin, name, plant.getCost(), Math.max(1, level), false, !owned, false);
            packet.onClick(() -> openPlantDetail(name));
            applySheetPortraitIfNeeded(packet, name);
            // Scale via wrapper so Table layout does not fight setScale.
            Group packetSlot = new Group();
            packetSlot.setSize(PACKET_W, PACKET_H);
            packet.setTransform(true);
            packet.setScale(PACKET_SCALE);
            packetSlot.addActor(packet);
            cell.add(packetSlot).size(PACKET_W, PACKET_H).row();

            // secondary style is DarkBrown — override fontColor via PLANT_META_INK.
            Label nameLabel = metaLabel(name, "secondary");
            nameLabel.setAlignment(Align.center);
            nameLabel.setWrap(true);
            cell.add(nameLabel).width(PACKET_W + 8f).height(28f).padTop(2f).row();

            Table meta = new Table();
            meta.top();
            if (!owned) {
                Label locked = metaLabel("LOCKED", "secondary");
                locked.setAlignment(Align.center);
                meta.add(locked).width(PACKET_W + 4f).expandY().center();
            } else {
                boolean maxed = level >= Collection.MAX_PLANT_LEVEL;
                Label levelLabel = metaLabel(
                    maxed ? ("Lv " + level + "  MAX") : ("Lv " + level), "secondary");
                levelLabel.setAlignment(Align.center);
                meta.add(levelLabel).width(PACKET_W + 4f).row();

                if (!maxed && need > 0) {
                    meta.add(seedProgressBar(have, need))
                        .width(PACKET_W)
                        .height(SEED_BAR_H * SEED_BAR_SCALE)
                        .padTop(SEED_BAR_PAD_TOP)
                        .row();

                    Label seedCountLabel = metaLabel(have + " / " + need, "secondary");
                    seedCountLabel.setAlignment(Align.center);
                    meta.add(seedCountLabel).width(PACKET_W + 4f).padTop(1f);
                }
            }
            cell.add(meta).width(PACKET_W + 8f).height(PLANT_META_H).top();
            // top() keeps packet art on one horizontal line across the row.
            grid.add(cell).pad(5f).top();
            shown++;
            colIndex++;
            if (colIndex >= GRID_COLS) {
                grid.row();
                colIndex = 0;
            }
        }
        if (shown == 0) {
            Label empty = new Label("No plants match this filter.", skin, "medium");
            empty.setColor(INK);
            grid.add(empty).pad(24f);
        }
        statusLabel.setText("Plants Collected: " + ownedCount + " of " + plants.size());
    }

    private void refreshZombieGrid(Collection col) {
        TextureBank t = textures;
        visibleZombieNames.clear();
        List<Zombie> zombies = new ArrayList<>(col.getAllZombies());
        zombies.sort(Comparator.comparing(Zombie::getName, String.CASE_INSENSITIVE_ORDER));
        int discovered = 0;
        int colIndex = 0;
        for (Zombie zombie : zombies) {
            String name = zombie.getName();
            visibleZombieNames.add(name);
            boolean seen = col.ownsZombie(name);
            if (seen) {
                discovered++;
            }
            Table cell = new Table();
            cell.setTouchable(Touchable.enabled);
            ZombieAlmanacPacket packet = new ZombieAlmanacPacket(t, ZOMBIE_PACKET_W, ZOMBIE_PACKET_H)
                .show(name, seen);
            if (seen) {
                applyZombieSheetPortraitIfNeeded(packet, name);
            }
            cell.add(packet).size(ZOMBIE_PACKET_W, ZOMBIE_PACKET_H).row();
            Label meta = metaLabel(seen ? shortName(name) : "???", "secondary");
            meta.setAlignment(Align.center);
            cell.add(meta).width(ZOMBIE_PACKET_W + 4f).padTop(2f);
            cell.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    packet.setPressed(true);
                    return true;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    packet.setPressed(false);
                    if (x >= 0f && x <= cell.getWidth() && y >= 0f && y <= cell.getHeight()) {
                        openZombieDetail(name);
                    }
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    packet.setPressed(false);
                }
            });
            grid.add(cell).padLeft(ZOMBIE_CELL_PAD).padRight(ZOMBIE_CELL_PAD)
                .padTop(ZOMBIE_ROW_PAD).padBottom(ZOMBIE_ROW_PAD);
            colIndex++;
            if (colIndex >= ZOMBIE_GRID_COLS) {
                grid.row();
                colIndex = 0;
            }
        }
        statusLabel.setText("Zombies Discovered: " + discovered + " of " + zombies.size());
    }

    private void openPlantDetail(String name) {
        CollectionEntryOverlay.openPlant(
            stage, skin, game.assets, pamClips, name,
            new ArrayList<>(visiblePlantNames), this::onEntryChanged, this::toastResult);
    }

    private void openZombieDetail(String name) {
        CollectionEntryOverlay.openZombie(
            stage, skin, game.assets, pamClips, name,
            new ArrayList<>(visibleZombieNames), this::onEntryChanged, this::toastResult);
    }

    private void onEntryChanged() {
        if (resources != null) {
            resources.refresh();
        }
        refreshGrid();
    }

    private void toastResult(CommandResult<?> r) {
        if (r == null || r.getMessage() == null || r.getMessage().isBlank()) {
            return;
        }
        showToast(r.getMessage(), !r.isSuccess());
        toast.toFront();
    }

    private boolean passesFilter(Collection col, User user, Plant plant) {
        String name = plant.getName();
        boolean owned = col.ownsPlant(name);
        if (categoryFilter != null && plant.getCategory() != categoryFilter) {
            return false;
        }
        return switch (filter) {
            case ALL -> true;
            case UNLOCKED -> owned;
            case LOCKED -> !owned;
            case UPGRADABLE -> owned
                && col.getPlantLevel(name) < Collection.MAX_PLANT_LEVEL
                && seedCount(user, name) >= col.getUpgradeSeedCost(name)
                && user.getCoins() >= col.getUpgradeCoinCost(name);
        };
    }

    private void goBack() {
        CommandResult<Void> r = controller.menuExit();
        showToast(r.getMessage(), !r.isSuccess());
        game.setScreen(new AdventureScreen(game));
    }

    @Override
    protected void onBack() {
        goBack();
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

    private void applySheetPortraitIfNeeded(SeedPacketActor packet, String plantName) {
        SheetPacketPortraits.applyIfNeeded(packet, plantName, game.assets, sheetClips);
    }

    private void applyZombieSheetPortraitIfNeeded(ZombieAlmanacPacket packet, String zombieName) {
        if (packet == null || zombieName == null || textures == null) {
            return;
        }
        String atlasId = AlmanacZombiePacketIds.portraitId(zombieName);
        if (atlasId != null && textures.region(atlasId) != null) {
            return;
        }
        PvzAssets assets = game.assets;
        if (assets == null || assets.plantSheets == null || sheetClips == null) {
            return;
        }
        PlantSpritesheetCatalog.ClipSpec spec = assets.plantSheets.idleFallback(zombieName);
        if (spec == null) {
            return;
        }
        SpritesheetClipCache.SheetAnim sheet = sheetClips.getOrLoad(spec);
        if (sheet == null || sheet.animation() == null) {
            return;
        }
        TextureRegion frame = sheet.animation().getKeyFrame(0f);
        if (SunshineAnim.isSunshineName(zombieName)) {
            TextureRegion upright = SunshineAnim.packetPortraitFrame(sheet.animation());
            if (upright != null) {
                frame = upright;
            }
        }
        if (frame == null) {
            return;
        }
        if (SunshineAnim.isSunshineName(zombieName)) {
            packet.setPortraitOverride(frame,
                    CollectionEntryOverlay.SUNSHINE_PACKET_PORTRAIT_SCALE,
                    CollectionEntryOverlay.SUNSHINE_PACKET_PORTRAIT_OFFSET_X,
                    CollectionEntryOverlay.SUNSHINE_PACKET_PORTRAIT_OFFSET_Y);
        } else {
            packet.setPortraitOverride(frame);
        }
    }

    private static int seedCount(User user, String plant) {
        if (user == null || user.getSeedPackets() == null) {
            return 0;
        }
        return user.getSeedPackets().getOrDefault(plant, 0);
    }

    private static String familyLabel(PlantCategory cat) {
        return switch (cat) {
            case SUN_PRODUCER -> "Sun Producer";
            case SHOOTER -> "Shooter";
            case LOBBER -> "Lobber";
            case EXPLOSIVE -> "Explosive";
            case MELEE -> "Melee";
            case WALL_NUT -> "Wall-Nut";
            case MODIFIER -> "Modifier";
            case STRIKE_THROUGH -> "Strike Through";
            case HOMING -> "Homing";
            case MINT -> "Mint";
        };
    }

    private static String shortName(String name) {
        if (name == null) {
            return "";
        }
        String s = name.startsWith("Zombie") ? name.substring("Zombie".length()) : name;
        return s.isEmpty() ? name : s;
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
        if (sheetClips != null) {
            sheetClips.dispose();
            sheetClips = null;
        }
        super.dispose();
    }

    /**
     * Fixed-size tab slot. Face is top-aligned so ACTIVE (pointed) vs DOWN (flat)
     * never shifts the marker; icon sits on the colored fill.
     */
    private static final class TabMarker extends Group {
        private final Image face = new Image();
        private final Image icon = new Image();

        TabMarker(TextureRegion iconRegion, float iconW, float iconH,
                  float iconX, float iconY, Runnable action) {
            setSize(TAB_SLOT_W, TAB_SLOT_H);
            face.setTouchable(Touchable.disabled);
            icon.setTouchable(Touchable.disabled);
            addActor(face);
            if (iconRegion != null) {
                icon.setDrawable(new TextureRegionDrawable(iconRegion));
                icon.setSize(iconW, iconH);
                icon.setPosition(iconX, iconY);
                addActor(icon);
            }
            addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
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
            // Top-align inside the fixed slot so height changes only grow downward.
            face.setPosition(0f, TAB_SLOT_H - face.getHeight());
        }
    }
}
