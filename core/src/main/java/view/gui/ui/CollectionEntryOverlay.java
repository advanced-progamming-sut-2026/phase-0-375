package view.gui.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controller.CollectionMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.collection.Collection;
import model.enums.PlantCategory;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.user.User;
import model.zombie.definition.Zombie;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.BorderedTable;
import view.gui.anim.PamClipCache;
import view.gui.anim.PamVisibility;
import view.gui.anim.SpritesheetClipCache;
import view.gui.anim.zombie.SunshineAnim;
import view.gui.anim.zombie.ZombieAnimAdapter;
import view.gui.anim.zombie.ZombotanyAnim;
import view.gui.assets.AlmanacArt;
import view.gui.assets.AlmanacZombieLabels;
import view.gui.assets.PamCatalog;
import view.gui.assets.PlantSpritesheetCatalog;
import view.gui.assets.PvzAssets;
import view.gui.audio.GameAudio;
import view.gui.assets.SeedPacketIds;
import view.gui.assets.ShopArt;
import view.gui.assets.UiRegions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Almanac entry overlay (shop-picker style): dimmed backdrop + centered card.
 *
 * <p>Toggle card fill with {@link #PLANT_BLUE_BG} / {@link #ZOMBIE_BLUE_BG}:
 * {@code true} = shop {@code MINISTORE_BG} blue (rounded), {@code false} = cream frame.
 */
public final class CollectionEntryOverlay {
    // ── Shared fade ────────────────────────────────────────────────────────
    public static float FADE_IN = 0.2f;
    public static float FADE_OUT = 0.15f;

    // ── Card fill mode (cream vs shop blue) ─────────────────────────────────
    /** Plant overlay: {@code true} = blue MINISTORE fill, {@code false} = cream. */
    public static boolean PLANT_BLUE_BG = true;
    /** Zombie overlay: {@code true} = blue MINISTORE fill, {@code false} = cream. */
    public static boolean ZOMBIE_BLUE_BG = true;
    /** Brown frame thickness when blue fill is on (same as shop). */
    public static float BLUE_FRAME = 17f;
    /** Corner radius of the blue interior. */
    public static float BLUE_CORNER = 26f;
    /** Fallback radial blue if MINISTORE_BG is missing. */
    public static final Color BLUE_CENTER = new Color(0.18f, 0.42f, 0.82f, 1f);
    public static final Color BLUE_EDGE = new Color(0.02f, 0.06f, 0.18f, 1f);
    /** Text colors on blue fill (cream uses {@link #INK} / {@link #MUTED} / {@link #FLAVOR_YELLOW}). */
    public static final Color BLUE_INK = Color.WHITE;
    /** Stat captions (SUN COST, TOUGHNESS, …). */
    public static final Color BLUE_MUTED = new Color(0.75f, 0.8f, 0.95f, 1f);
    public static final Color BLUE_FLAVOR = new Color(0.88f, 0.78f, 0.28f, 1f);
    /** Plant Food line on blue fill. */
    public static final Color BLUE_PF_GREEN = new Color(0.48f, 0.76f, 0.38f, 1f);

    // ── Zombie modal ───────────────────────────────────────────────────────
    /** Card width on the 1920-wide stage. */
    public static float ZOMBIE_MODAL_W = 980f;
    public static float ZOMBIE_OVERLAY_PAD = 24f;
    /** Inner pad of the card. */
    public static float ZOMBIE_CARD_PAD = 40f;
    /** Extra bottom pad (especially useful with blue fill). */
    public static float ZOMBIE_CARD_PAD_BOTTOM = 72f;
    /** Almanac packet frame size in the overlay preview (not the grid packet art). */
    public static float ZOMBIE_PREVIEW_W = 260f;
    public static float ZOMBIE_PREVIEW_H = 260f;
    /** Pushes the preview block down (+) or up (−) in the left column. */
    public static float ZOMBIE_PREVIEW_PAD_TOP = 0f;
    /** Pushes the whole left column (preview) down (+) or up (−). */
    public static float ZOMBIE_LEFT_COL_PAD_TOP = 0f;
    /** Pushes the right column (stats / description) down (+) or up (−). */
    public static float ZOMBIE_RIGHT_COL_PAD_TOP = 40f;
    /** Idle PAM draw scale inside the preview frame. */
    public static float ZOMBIE_PAM_SCALE = 0.8f;
    /** PAM vertical anchor: 0 = bottom of frame, 0.5 = center, 1 = top. Higher = zombie moves up. */
    public static float ZOMBIE_PAM_ANCHOR_Y = 0.28f;
    /**
     * Extra Y for I, Zombie sunshine spritesheet preview (negative = lower).
     * Sheet frames are taller than PAM canvases and overflow the almanac box.
     */
    public static float SUNSHINE_PREVIEW_OFFSET_Y = -70f;
    /** Spritesheet preview scale multiplier for sunshine zombie only (1 = default). */
    public static float SUNSHINE_PREVIEW_SCALE = 0.8f;
    /** Portrait scale on collection grid packet for sunshine (1 = default fit). */
    public static float SUNSHINE_PACKET_PORTRAIT_SCALE = 1.05f;
    public static float SUNSHINE_PACKET_PORTRAIT_OFFSET_X = 0f;
    public static float SUNSHINE_PACKET_PORTRAIT_OFFSET_Y = -6f;
    public static float ZOMBIE_LEFT_COL_W = 280f;
    public static float ZOMBIE_TITLE_PAD_BOTTOM = 16f;
    public static float ZOMBIE_STATS_PAD_BOTTOM = 12f;
    public static float ZOMBIE_DESC_PAD_BOTTOM = 10f;
    public static float ZOMBIE_STAT_ROW_PAD_BOTTOM = 10f;

    // ── Plant modal (overall) ──────────────────────────────────────────────
    /** Card width on the 1920-wide stage. */
    public static float PLANT_MODAL_W = 950f;
    /** Padding around the card inside the dimmed overlay. */
    public static float PLANT_OVERLAY_PAD = 28f;
    /** Inner pad of the card. */
    public static float PLANT_CARD_PAD = 40f;
    /** Extra bottom pad when using blue fill (cream mode still uses {@link #PLANT_CARD_PAD}). */
    public static float PLANT_CARD_PAD_BOTTOM = 56f;
    /**
     * Inset from the brown frame to the ◀ ▶ nav arrows.
     * Smaller = arrows closer to the card edges.
     */
    public static float PLANT_NAV_EDGE_PAD = 30f;
    /** Gap between each nav arrow and the center body. */
    public static float PLANT_NAV_GAP = 4f;

    // ── Plant preview / PAM ────────────────────────────────────────────────
    public static float PREVIEW_W = 160f;
    public static float PREVIEW_H = 160f;
    /** Idle PAM draw scale (was ~0.62 before the 4× bump). */
    public static float PLANT_PAM_SCALE = 0.62f;
    /** 0 = bottom of preview frame, 0.5 = vertical center, 1 = top. */
    public static float PLANT_PAM_ANCHOR_Y = 0.5f;
    /**
     * Extra Y for Cat-tail spritesheet preview (negative = lower).
     * Custom sheet origin differs from PAM plants.
     */
    public static float CATTAIL_PREVIEW_OFFSET_Y = -45f;
    /** Spritesheet preview scale multiplier for Cat-tail only (1 = default). */
    public static float CATTAIL_PREVIEW_SCALE = 0.75f;
    /** Portrait scale on collection grid seed packet for Cat-tail (1 = default fit). */
    public static float CATTAIL_PACKET_PORTRAIT_SCALE = 1.5f;
    /** Portrait nudge on collection grid seed packet (Cat-tail only). */
    public static float CATTAIL_PACKET_PORTRAIT_OFFSET_X = 0f;
    public static float CATTAIL_PACKET_PORTRAIT_OFFSET_Y = -10f;
    public static float LEVEL_LABEL_PAD_BOTTOM = 12f;
    public static float PREVIEW_PAD_BOTTOM = 14f;
    /** Pushes preview / seed bar / buttons down in the plant left column. */
    public static float LEFT_COL_PAD_TOP = 0f;

    // ── Plant columns ──────────────────────────────────────────────────────
    /** Left column width — keep near {@link #PREVIEW_W} so the stats column gets room. */
    public static float LEFT_COL_W = 260f;
    /** Gap between left (preview) and right (stats) columns. */
    public static float BODY_COL_GAP = 20f;
    public static float BODY_SIDE_PAD = 6f;

    // ── Typography / colors ────────────────────────────────────────────────
    public static float UI_FONT_SCALE = 1.22f;
    public static float SEED_BAR_FONT_SCALE = 0.88f;
    public static final Color INK = new Color(0.12f, 0.10f, 0.12f, 1f);
    public static final Color MUTED = new Color(0.35f, 0.32f, 0.30f, 1f);
    public static final Color FLAVOR_YELLOW = new Color(0.55f, 0.42f, 0.05f, 1f);
    public static final Color PF_GREEN = new Color(0.12f, 0.45f, 0.18f, 1f);
    public static final Color DIM = new Color(0f, 0f, 0f, 0.55f);

    // ── Seed bar ───────────────────────────────────────────────────────────
    /** Bar width (usually same as {@link #PREVIEW_W}). */
    public static float SEED_BAR_W = PREVIEW_W;
    /** Unscaled height before {@link #SEED_BAR_SCALE}. */
    public static float SEED_BAR_H = 16f;
    /** Skin xp bar scale — height alone cannot shrink the nine-patch. */
    public static float SEED_BAR_SCALE = 0.72f;
    public static float SEED_BAR_PAD_BOTTOM = 8f;
    public static float SEED_XP_ICON = 16f;
    public static float SEED_XP_ICON_PAD_LEFT = 4f;

    // ── Action buttons ─────────────────────────────────────────────────────
    public static float BTN_W = 240f;
    public static float BTN_H = 56f;
    public static float BTN_GAP = 8f;
    public static float ACTIONS_PAD_TOP = 20f;

    // ── Stats / family / text ──────────────────────────────────────────────
    public static float STAT_ICON = 44f;
    public static float STAT_ICON_PAD_RIGHT = 10f;
    public static float STAT_ROW_PAD_BOTTOM = 10f;
    public static float STAT_VALUE_PAD_TOP = 2f;
    public static float FAMILY_ICON = 48f;
    public static float FAMILY_ICON_PAD_RIGHT = 10f;
    public static float PLANT_FOOD_ICON = 36f;
    public static float PLANT_FOOD_ICON_PAD_RIGHT = 10f;
    public static float TITLE_PAD_BOTTOM = 40f;
    /** Top inset above the plant name — lower than {@link #PLANT_CARD_PAD} lifts the title. */
    public static float PLANT_TITLE_PAD_TOP = 30f;
    public static float STATS_PAD_BOTTOM = 12f;
    public static float FAMILY_PAD_BOTTOM = 12f;
    public static float PLANT_FOOD_PAD_BOTTOM = 12f;
    public static float ABILITY_PAD_BOTTOM = 10f;

    // ── Nav arrows ─────────────────────────────────────────────────────────
    public static float NAV_W = 64f;
    public static float NAV_H = 108f;
    public static float NAV_PAD = 10f;

    /** Corner X on the plant card ({@code IMAGE_UI_GENERIC_CLOSE_CIRCLE}). */
    public static float CLOSE_SIZE = 56f;
    public static float CLOSE_PAD = 8f;

    private static final String MAGNET_ITEM_PART = "Magnet_Item";
    private static final String CLOSE_UP = "IMAGE_UI_GENERIC_CLOSE_CIRCLE";
    private static final String CLOSE_DOWN = "IMAGE_UI_GENERIC_CLOSE_CIRCLE_DOWN";

    private static Texture pixel;
    private static Texture zombieBlueGradient;
    private static InputProcessor savedInputProcessor;

    private CollectionEntryOverlay() {}

    public static Table openPlant(Stage stage, Skin skin, PvzAssets assets, PamClipCache clips,
                                  String plantName, List<String> siblings,
                                  Runnable onChanged, Consumer<CommandResult<?>> onResult) {
        TextureBank textures = assets.textures;
        ensureAlmanacAtlases(textures);

        List<String> names = siblings == null || siblings.isEmpty()
            ? List.of(plantName) : new ArrayList<>(siblings);
        int start = Math.max(0, names.indexOf(plantName));
        if (names.indexOf(plantName) < 0) {
            names = new ArrayList<>(names);
            names.add(0, plantName);
            start = 0;
        }

        Table overlay = dimOverlay();
        GameAudio.get().playOverlayOpen();
        PlantPage page = new PlantPage(skin, assets, clips, textures, names, start,
            onChanged, onResult, () -> dismiss(overlay, null));
        addOverlayRoot(overlay, page.root, PLANT_MODAL_W, PLANT_OVERLAY_PAD, PLANT_BLUE_BG);
        fadeIn(stage, overlay);
        bindOverlayNavKeys(stage, page::handleNavKey);
        return overlay;
    }

    public static Table openZombie(Stage stage, Skin skin, PvzAssets assets, PamClipCache clips,
                                   String zombieName, List<String> siblings,
                                   Runnable onChanged, Consumer<CommandResult<?>> onResult) {
        TextureBank textures = assets.textures;
        ensureAlmanacAtlases(textures);

        List<String> names = siblings == null || siblings.isEmpty()
            ? List.of(zombieName) : new ArrayList<>(siblings);
        int start = Math.max(0, names.indexOf(zombieName));
        if (names.indexOf(zombieName) < 0) {
            names = new ArrayList<>(names);
            names.add(0, zombieName);
            start = 0;
        }

        Table overlay = dimOverlay();
        GameAudio.get().playOverlayOpen();
        ZombiePage page = new ZombiePage(skin, assets, clips, textures, names, start,
            () -> dismiss(overlay, null));
        addOverlayRoot(overlay, page.root, ZOMBIE_MODAL_W, ZOMBIE_OVERLAY_PAD, ZOMBIE_BLUE_BG);
        fadeIn(stage, overlay);
        bindOverlayNavKeys(stage, page::handleNavKey);
        return overlay;
    }

    private static void addOverlayRoot(Table overlay, Actor root, float modalW, float pad, boolean blueBg) {
        if (blueBg) {
            overlay.add(root).size(root.getWidth(), root.getHeight()).pad(pad);
        } else {
            overlay.add(root).width(modalW).pad(pad);
        }
    }

    private static void ensureAlmanacAtlases(TextureBank textures) {
        textures.loadSync(AlmanacArt.ATLAS);
        textures.loadSync(AlmanacArt.ATLAS_IMAGE);
        textures.loadSync(AlmanacArt.ATLAS_STAT_ICONS);
        textures.loadSync(AlmanacArt.ATLAS_GRADIENTS);
        textures.loadSync(UiRegions.ATLAS_UI_ALWAYS_LOADED);
        textures.loadSync("ATLASIMAGE_ATLAS_UI_ALWAYSLOADED_768_01");
        textures.loadSync(SeedPacketIds.ATLAS);
        textures.loadSync("ATLASIMAGE_ATLAS_UI_SEEDPACKETS_768_00");
        textures.loadSync(ShopArt.ATLAS_STORE);
    }

    private static Table dimOverlay() {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(DIM));
        overlay.setTouchable(Touchable.enabled);
        return overlay;
    }

    private static void fadeIn(Stage stage, Table overlay) {
        overlay.getColor().a = 0f;
        overlay.addAction(Actions.fadeIn(FADE_IN));
        stage.addActor(overlay);
    }

    private static void bindOverlayNavKeys(Stage stage, NavKeyHandler handler) {
        unbindOverlayKeys();
        savedInputProcessor = Gdx.input.getInputProcessor();
        InputAdapter nav = new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                return handler.handleNavKey(keycode);
            }
        };
        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(nav);
        if (savedInputProcessor != null) {
            mux.addProcessor(savedInputProcessor);
        } else {
            mux.addProcessor(stage);
        }
        Gdx.input.setInputProcessor(mux);
    }

    @FunctionalInterface
    private interface NavKeyHandler {
        boolean handleNavKey(int keycode);
    }

    private static void dismiss(Table overlay, Runnable after) {
        unbindOverlayKeys();
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

    private static void unbindOverlayKeys() {
        if (savedInputProcessor != null) {
            Gdx.input.setInputProcessor(savedInputProcessor);
            savedInputProcessor = null;
        }
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

    /** PvZ2 almanac-style radial blue (bright center → dark navy edge). */
    private static Texture zombieBlueGradient() {
        if (zombieBlueGradient == null) {
            int size = 128;
            Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            float cx = (size - 1) * 0.5f;
            float cy = (size - 1) * 0.5f;
            float maxR = (float) Math.sqrt(cx * cx + cy * cy);
            Color c = BLUE_CENTER;
            Color e = BLUE_EDGE;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float dx = x - cx;
                    float dy = y - cy;
                    float t = Math.min(1f, (float) Math.sqrt(dx * dx + dy * dy) / maxR);
                    // Ease toward the edge so the center stays a larger bright pool.
                    t = t * t;
                    pm.setColor(
                        c.r + (e.r - c.r) * t,
                        c.g + (e.g - c.g) * t,
                        c.b + (e.b - c.b) * t,
                        1f);
                    pm.drawPixel(x, y);
                }
            }
            zombieBlueGradient = new Texture(pm);
            zombieBlueGradient.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            pm.dispose();
        }
        return zombieBlueGradient;
    }

    /** Same crop as ShopScreen — strips transparent padding from MINISTORE_BG. */
    private static TextureRegion paintedMinistoreCore(TextureRegion src) {
        int cropX = Math.round(src.getRegionWidth() * 0.025f);
        int cropY = Math.round(src.getRegionHeight() * 0.175f);
        TextureRegion core = new TextureRegion(src);
        core.setRegion(src.getRegionX() + cropX, src.getRegionY() + cropY,
            src.getRegionWidth() - cropX * 2, src.getRegionHeight() - cropY * 2);
        return core;
    }

    private static TextureRegion blueFillRegion(TextureBank textures) {
        TextureRegion mini = textures.region(ShopArt.MINISTORE_BG);
        return mini != null ? paintedMinistoreCore(mini) : new TextureRegion(zombieBlueGradient());
    }

    private static Color panelInk(boolean blue) {
        return blue ? BLUE_INK : INK;
    }

    private static Color panelMuted(boolean blue) {
        return blue ? BLUE_MUTED : MUTED;
    }

    private static Color panelFlavor(boolean blue) {
        return blue ? BLUE_FLAVOR : FLAVOR_YELLOW;
    }

    private static Color panelPlantFoodGreen(boolean blue) {
        return blue ? BLUE_PF_GREEN : PF_GREEN;
    }

    /** Cream BorderedTable shell (classic overlay). Content should already include pad. */
    private static Stack creamModalRoot(Table content, TextureBank textures, Runnable onClose) {
        BorderedTable card = new BorderedTable();
        card.add(content).grow();

        AtlasImageButton close = new AtlasImageButton(
            textures.region(CLOSE_UP),
            textures.region(CLOSE_DOWN),
            CLOSE_SIZE, onClose);
        Table closeLayer = new Table();
        closeLayer.setFillParent(true);
        closeLayer.top().right();
        closeLayer.add(close).size(CLOSE_SIZE).pad(CLOSE_PAD);

        Stack root = new Stack();
        root.add(card);
        root.add(closeLayer);
        return root;
    }

    /**
     * Shop-style panel: brown frame + rounded blue interior with explicit bounds.
     * Call {@link #layoutPanel()} after content text changes.
     */
    private static final class BlueModalPanel extends WidgetGroup {
        private final BorderedTable frame = new BorderedTable();
        private final RoundedRegionImage blueFill;
        private final Table content;
        private final AtlasImageButton closeBtn;
        private final float modalW;
        private final float minInnerH;

        BlueModalPanel(TextureBank textures, Table content, float modalW, float minInnerH,
                       Runnable onClose) {
            this.content = content;
            this.modalW = modalW;
            this.minInnerH = minInnerH;
            blueFill = new RoundedRegionImage(blueFillRegion(textures), BLUE_CORNER);
            blueFill.setTouchable(Touchable.disabled);
            frame.setTouchable(Touchable.disabled);
            closeBtn = new AtlasImageButton(
                textures.region(CLOSE_UP),
                textures.region(CLOSE_DOWN),
                CLOSE_SIZE, onClose);
            addActor(frame);
            addActor(blueFill);
            addActor(content);
            addActor(closeBtn);
            layoutPanel();
        }

        @Override
        public float getPrefWidth() {
            return getWidth() > 0f ? getWidth() : modalW;
        }

        @Override
        public float getPrefHeight() {
            return getHeight() > 0f ? getHeight() : 400f;
        }

        void layoutPanel() {
            float frameT = BLUE_FRAME;
            float innerW = modalW - frameT * 2f;
            content.setSize(innerW, 0f);
            content.invalidate();
            content.validate();
            float innerH = Math.max(content.getPrefHeight(), minInnerH);
            float panelH = innerH + frameT * 2f;

            setSize(modalW, panelH);
            frame.setBounds(0f, 0f, modalW, panelH);
            blueFill.setBounds(frameT, frameT, innerW, innerH);
            content.setBounds(frameT, frameT, innerW, innerH);
            closeBtn.setSize(CLOSE_SIZE, CLOSE_SIZE);
            closeBtn.setPosition(
                modalW - CLOSE_SIZE - CLOSE_PAD,
                panelH - CLOSE_SIZE - CLOSE_PAD);
            if (hasParent()) {
                invalidateHierarchy();
            }
        }
    }

    private static Image regionImage(TextureBank textures, String id) {
        TextureRegion region = textures.region(id);
        Image image = new Image(region != null ? new TextureRegionDrawable(region) : null);
        image.setScaling(Scaling.fit);
        return image;
    }

    private static Label whiteLabel(Skin skin, String style, String text, Color color) {
        Label.LabelStyle base = skin.get(style, Label.LabelStyle.class);
        Label.LabelStyle copy = new Label.LabelStyle(base);
        copy.fontColor = color.cpy();
        Label label = new Label(text, copy);
        label.setAlignment(Align.left);
        return label;
    }

    // ── Plant modal card ───────────────────────────────────────────────────

    private static final class PlantPage {
        private final Skin skin;
        private final TextureBank textures;
        private final CollectionMenuController controller = CollectionMenuController.getInstance();
        private final List<String> names;
        private final Runnable onChanged;
        private final Consumer<CommandResult<?>> onResult;
        private final boolean blueBg = PLANT_BLUE_BG;

        final Actor root;
        private final BlueModalPanel bluePanel;
        private final IdlePreview preview;
        private final Label levelLabel;
        private final ProgressBar seedBar;
        private final Label seedCountLabel;
        private final Image xpIcon;
        private final TextButton upgradeBtn;
        private final TextButton buyBtn;
        private final Label title;
        private final Label sunStat;
        private final Label rechargeStat;
        private final Label toughStat;
        private final Label damageStat;
        private final Label rangeStat;
        private final Label specialStat;
        private final FamilyBadge familyBadge;
        private final Label familyLabel;
        private final Label plantFoodLabel;
        private final Label abilityLabel;
        private final Label flavorLabel;
        private int index;

        private PlantPage(Skin skin, PvzAssets assets, PamClipCache clips, TextureBank textures,
                          List<String> names, int start,
                          Runnable onChanged, Consumer<CommandResult<?>> onResult, Runnable onClose) {
            this.skin = skin;
            this.textures = textures;
            this.names = names;
            this.index = start;
            this.onChanged = onChanged;
            this.onResult = onResult;

            Color ink = panelInk(blueBg);
            Color muted = panelMuted(blueBg);
            Color flavor = panelFlavor(blueBg);
            Color pfGreen = panelPlantFoodGreen(blueBg);

            preview = new IdlePreview(assets, clips, PLANT_PAM_SCALE, PLANT_PAM_ANCHOR_Y);
            // Level/Locked sits on the cream STAT_BG preview — always dark, even on blue cards.
            levelLabel = inkLabel(skin, "medium", "", INK);
            levelLabel.setAlignment(Align.center);

            seedBar = new ProgressBar(0f, 1f, 1f, false,
                skin.get("xp_yellow", ProgressBar.ProgressBarStyle.class));
            seedBar.setAnimateDuration(0f);
            seedBar.setTouchable(Touchable.disabled);
            seedCountLabel = inkLabel(skin, "secondary", "", ink);
            SkinFonts.scaleLabel(seedCountLabel, skin, "secondary", SEED_BAR_FONT_SCALE);
            seedCountLabel.setAlignment(Align.center);
            xpIcon = new Image();
            xpIcon.setScaling(Scaling.fit);

            Stack seedStack = new Stack();
            seedStack.add(scaledSeedBarSlot(seedBar, SEED_BAR_W));
            Table seedOverlay = new Table();
            seedOverlay.add(xpIcon).size(SEED_XP_ICON).padLeft(SEED_XP_ICON_PAD_LEFT).left();
            seedOverlay.add(seedCountLabel).expandX().center();
            seedStack.add(seedOverlay);

            upgradeBtn = new TextButton("UPGRADE", skin, "purple");
            buyBtn = new TextButton("BUY", skin, "brown");
            SkinFonts.scaleButton(upgradeBtn, skin, "purple", UI_FONT_SCALE);
            SkinFonts.scaleButton(buyBtn, skin, "brown", UI_FONT_SCALE);
            upgradeBtn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    CommandResult<Void> r = controller.upgradePlant(currentName());
                    GameAudio.get().feedbackPurchase(r);
                    if (onResult != null) {
                        onResult.accept(r);
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                    refresh();
                }
            });
            buyBtn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    CommandResult<Void> r = controller.purchasePlant(currentName());
                    GameAudio.get().feedbackPurchase(r);
                    if (onResult != null) {
                        onResult.accept(r);
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                    refresh();
                }
            });
            Table left = new Table();
            Stack previewStack = new Stack();
            Image previewBg = regionImage(textures, AlmanacArt.STAT_BG);
            previewBg.setFillParent(true);
            previewStack.add(previewBg);
            previewStack.add(preview);
            Table levelPad = new Table();
            levelPad.bottom();
            levelPad.add(levelLabel).padBottom(LEVEL_LABEL_PAD_BOTTOM);
            previewStack.add(levelPad);
            left.add(previewStack).size(PREVIEW_W, PREVIEW_H).padBottom(PREVIEW_PAD_BOTTOM).row();
            left.add(seedStack)
                .width(SEED_BAR_W)
                .height(SEED_BAR_H * SEED_BAR_SCALE)
                .padBottom(SEED_BAR_PAD_BOTTOM)
                .row();
            Table actions = new Table();
            actions.add(upgradeBtn).width(BTN_W).height(BTN_H).padBottom(BTN_GAP).row();
            actions.add(buyBtn).width(BTN_W).height(BTN_H);
            left.add(actions).padTop(ACTIONS_PAD_TOP);

            title = inkLabel(skin, "big", "", ink);
            title.setAlignment(Align.center);
            sunStat = inkLabel(skin, "medium", "", ink);
            rechargeStat = inkLabel(skin, "medium", "", ink);
            toughStat = inkLabel(skin, "medium", "", ink);
            damageStat = inkLabel(skin, "medium", "", ink);
            rangeStat = inkLabel(skin, "medium", "", ink);
            specialStat = inkLabel(skin, "medium", "", ink);
            familyLabel = inkLabel(skin, "medium", "", ink);
            plantFoodLabel = inkLabel(skin, "secondary", "", pfGreen);
            plantFoodLabel.setWrap(true);
            abilityLabel = inkLabel(skin, "secondary", "", muted);
            abilityLabel.setWrap(true);
            flavorLabel = inkLabel(skin, "secondary", "", flavor);
            flavorLabel.setWrap(true);

            Table stats = new Table();
            stats.add(statRow(AlmanacArt.ICON_SUN, "SUN COST", sunStat))
                .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM);
            stats.add(statRow(AlmanacArt.ICON_RECHARGE, "RECHARGE", rechargeStat))
                .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM).row();
            stats.add(statRow(AlmanacArt.ICON_TOUGHNESS, "TOUGHNESS", toughStat))
                .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM);
            stats.add(statRow(AlmanacArt.ICON_DAMAGE, "DAMAGE", damageStat))
                .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM).row();
            stats.add(statRow(AlmanacArt.ICON_RANGE, "RANGE", rangeStat))
                .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM);
            stats.add(statRow(AlmanacArt.ICON_SPECIAL, "SPECIAL", specialStat))
                .growX().uniformX().padBottom(STAT_ROW_PAD_BOTTOM).row();

            Table familyRow = new Table();
            familyBadge = new FamilyBadge(textures, STAT_ICON);
            familyRow.add(familyBadge).size(STAT_ICON).padRight(STAT_ICON_PAD_RIGHT);
            familyRow.add(familyLabel).left().growX();

            Table pfRow = new Table();
            pfRow.add(regionImage(textures, AlmanacArt.ICON_PLANT_FOOD))
                .size(STAT_ICON).padRight(STAT_ICON_PAD_RIGHT).top();
            pfRow.add(plantFoodLabel).growX().left();

            Table right = new Table();
            right.top();
            right.add(stats).growX().padBottom(STATS_PAD_BOTTOM).row();
            right.add(familyRow).left().padBottom(FAMILY_PAD_BOTTOM).row();
            right.add(pfRow).growX().padBottom(PLANT_FOOD_PAD_BOTTOM).row();
            right.add(abilityLabel).growX().padBottom(ABILITY_PAD_BOTTOM).row();
            right.add(flavorLabel).growX().row();

            Table body = new Table();
            left.padTop(LEFT_COL_PAD_TOP);
            body.add(left).width(LEFT_COL_W).top().padRight(BODY_COL_GAP);
            body.add(right).grow().top();

            AtlasImageButton prev = new AtlasImageButton(
                textures.region(AlmanacArt.NAV_PREV),
                textures.region(AlmanacArt.NAV_PREV_DOWN),
                NAV_W, NAV_H, this::prev);
            AtlasImageButton next = new AtlasImageButton(
                textures.region(AlmanacArt.NAV_NEXT),
                textures.region(AlmanacArt.NAV_NEXT_DOWN),
                NAV_W, NAV_H, this::next);

            Table middle = new Table();
            middle.add(prev).size(NAV_W, NAV_H).padRight(PLANT_NAV_GAP);
            middle.add(body).grow();
            middle.add(next).size(NAV_W, NAV_H).padLeft(PLANT_NAV_GAP);

            Table content = new Table();
            float padBottom = blueBg ? PLANT_CARD_PAD_BOTTOM : PLANT_CARD_PAD;
            content.padLeft(PLANT_NAV_EDGE_PAD).padRight(PLANT_NAV_EDGE_PAD)
                .padBottom(padBottom).padTop(PLANT_TITLE_PAD_TOP);
            content.add(title).growX().center().padBottom(TITLE_PAD_BOTTOM).row();
            content.add(middle).growX();

            if (blueBg) {
                bluePanel = new BlueModalPanel(
                    textures, content, PLANT_MODAL_W,
                    PREVIEW_H + PLANT_CARD_PAD + padBottom + ACTIONS_PAD_TOP + BTN_H * 2f + 80f,
                    onClose);
                root = bluePanel;
            } else {
                bluePanel = null;
                root = creamModalRoot(content, textures, onClose);
            }

            refresh();
        }

        boolean handleNavKey(int keycode) {
            if (names.size() <= 1) {
                return false;
            }
            if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
                prev();
                return true;
            }
            if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
                next();
                return true;
            }
            return false;
        }

        private Table statRow(String iconId, String caption, Label value) {
            Table row = new Table();
            row.add(regionImage(textures, iconId))
                .size(STAT_ICON)
                .padRight(STAT_ICON_PAD_RIGHT)
                .top();
            Table text = new Table();
            Label cap = inkLabel(skin, "secondary", caption, panelMuted(blueBg));
            text.add(cap).left().row();
            value.setWrap(true);
            value.setAlignment(Align.left);
            text.add(value).left().growX().padTop(STAT_VALUE_PAD_TOP);
            row.add(text).left().growX().top();
            return row;
        }

        private void setFamilyIcon(Plant plant) {
            familyBadge.setPlant(textures, plant);
        }

        private String currentName() {
            return names.get(index);
        }

        private void prev() {
            if (names.size() <= 1) {
                return;
            }
            GameAudio.get().playNavClick();
            index = (index - 1 + names.size()) % names.size();
            refresh();
        }

        private void next() {
            if (names.size() <= 1) {
                return;
            }
            GameAudio.get().playNavClick();
            index = (index + 1) % names.size();
            refresh();
        }

        private void refresh() {
            String name = currentName();
            Collection col = controller.currentCollection();
            Plant plant = col.getPlant(name);
            title.setText(name);
            preview.setPlant(name);

            boolean owned = col.ownsPlant(name);
            int level = owned ? col.getPlantLevel(name) : 1;
            levelLabel.setText(owned ? ("Level " + level) : "Locked");

            User user = App.getInstance().getCurrentUser();
            int have = user != null && user.getSeedPackets() != null
                ? user.getSeedPackets().getOrDefault(name, 0) : 0;
            boolean maxed = owned && level >= Collection.MAX_PLANT_LEVEL;
            int need = owned && !maxed ? Math.max(1, col.getUpgradeSeedCost(name)) : 1;
            boolean ready = owned && !maxed && have >= need;

            ProgressBar.ProgressBarStyle style = skin.get(
                ready ? "xp_green" : "xp_yellow", ProgressBar.ProgressBarStyle.class);
            seedBar.setStyle(style);
            seedBar.setRange(0f, need);
            seedBar.setValue(owned && !maxed ? Math.min(have, need) : 0f);
            if (!owned) {
                seedCountLabel.setText("");
            } else if (maxed) {
                seedCountLabel.setText("MAX");
            } else {
                seedCountLabel.setText(have + "/" + need);
            }

            Drawable icon = skin.optional(
                ready ? "image_ui_generic_xp_progress_icon_green"
                    : "image_ui_generic_xp_progress_icon_yellow",
                Drawable.class);
            if (icon == null) {
                icon = skin.optional(
                    ready ? "image_ui_generic_xp_progress_icon_green_large"
                        : "image_ui_generic_xp_progress_icon_yellow_large",
                    Drawable.class);
            }
            xpIcon.setDrawable(icon);
            xpIcon.setVisible(owned && !maxed);

            int purchaseCost = controller.purchaseCostCoins();
            int upgradeCoins = owned && !maxed ? col.getUpgradeCoinCost(name) : 0;
            int coins = user != null ? user.getCoins() : 0;
            boolean canBuy = !owned && coins >= purchaseCost;
            boolean canUpgrade = owned && !maxed && have >= need && coins >= upgradeCoins;

            upgradeBtn.setVisible(true);
            buyBtn.setVisible(true);
            upgradeBtn.setDisabled(!canUpgrade);
            buyBtn.setDisabled(!canBuy);
            buyBtn.setText("BUY (" + purchaseCost + ")");
            // Disabled skin already greys; reinforce for locked-looking chrome.
            upgradeBtn.setColor(canUpgrade ? Color.WHITE : Color.GRAY);
            buyBtn.setColor(canBuy ? Color.WHITE : Color.GRAY);

            if (plant == null) {
                if (bluePanel != null) {
                    bluePanel.layoutPanel();
                }
                return;
            }
            PlantStats now = statsAt(plant, owned ? level : 1);
            PlantStats next = owned && !maxed ? statsAt(plant, level + 1) : now;
            sunStat.setText(diff(now.cost, next.cost, owned && !maxed));
            rechargeStat.setText(formatFloat(now.recharge));
            toughStat.setText(diff(now.hp, next.hp, owned && !maxed));
            damageStat.setText(diff(now.damage, next.damage, owned && !maxed));
            rangeStat.setText(rangeLabel(plant.getCategory()));
            specialStat.setText(specialLabel(plant));
            setFamilyIcon(plant);
            familyLabel.setText(prettyEnum(plant.getCategory().name()));
            plantFoodLabel.setText("Plant Food: " + plantFoodBlurb(plant));
            abilityLabel.setText(abilityBlurb(plant));
            flavorLabel.setText(flavorBlurb(plant, owned, level, maxed, col));
            if (bluePanel != null) {
                bluePanel.layoutPanel();
            }
        }

        private static String diff(int now, int next, boolean showNext) {
            if (!showNext || now == next) {
                return String.valueOf(now);
            }
            return now + " > " + next;
        }

        private static String formatFloat(float v) {
            if (Math.abs(v - Math.rint(v)) < 0.05f) {
                return String.valueOf((int) Math.rint(v));
            }
            return String.format(Locale.US, "%.1f", v);
        }

        private static String rangeLabel(PlantCategory category) {
            if (category == null) {
                return "—";
            }
            return switch (category) {
                case LOBBER -> "Lobbed";
                case SHOOTER, STRIKE_THROUGH -> "Straight";
                case HOMING -> "Homing";
                case MELEE -> "Touch";
                case EXPLOSIVE -> "Tile / Area";
                case SUN_PRODUCER -> "—";
                case WALL_NUT -> "—";
                case MODIFIER -> "Aura";
                case MINT -> "Family";
            };
        }

        private static String specialLabel(Plant plant) {
            if (plant.getTags() != null && !plant.getTags().isEmpty()) {
                return prettyEnum(plant.getTags().get(0).name());
            }
            return prettyEnum(plant.getAbilityType().name());
        }

        private static String plantFoodBlurb(Plant plant) {
            return switch (plant.getPlantFoodType()) {
                case NONE -> "None.";
                case SPAWN_SUN_ITEMS -> "Drops a burst of sun.";
                case PROJECTILE_BURST -> "Fires a rapid projectile volley.";
                case RANDOM_HYPNOTIZE -> "Hypnotizes random zombies.";
                case KNOCKBACK_BLAST -> "Knocks back and damages nearby zombies.";
                case MAP_WIDE_FREEZE -> "Stuns / freezes zombies across the lawn.";
                case SPAWN_CLONES -> "Spawns temporary nearby clones.";
                case LOCAL_AOE_ATTACK -> "Hits all zombies around the plant.";
                case PULL_UNDERWATER -> "Pulls zombies in and finishes them.";
                case GRANT_PERMANENT_ARMOR -> "Grants lasting armor.";
                case ATTRACT_AND_HEAL -> "Pulls zombies in and fully heals.";
            };
        }

        private static String abilityBlurb(Plant plant) {
            String ability = prettyEnum(plant.getAbilityType().name());
            return plant.getName() + " — " + ability
                + " (value " + formatFloat(plant.getAbilityValue()) + ").";
        }

        private static String flavorBlurb(Plant plant, boolean owned, int level,
                                          boolean maxed, Collection col) {
            if (!owned) {
                return "Locked. Purchase to add this plant to your collection.";
            }
            if (maxed) {
                return "Already at max level.";
            }
            return "Next upgrade needs " + col.getUpgradeSeedCost(plant.getName())
                + " seed packets and " + col.getUpgradeCoinCost(plant.getName()) + " coins.";
        }

        private static String prettyEnum(String raw) {
            if (raw == null || raw.isEmpty()) {
                return "—";
            }
            String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            return sb.toString();
        }

        private static PlantStats statsAt(Plant plant, int level) {
            int hp = plant.getBaseHP();
            int damage = plant.getDamage();
            int cost = plant.getCost();
            float recharge = plant.getRechargeTime();
            if (level > 1 && plant.getLevels() != null) {
                for (LevelUpgrade up : plant.getLevels().cumulativeUpgrades(level).values()) {
                    if (up == null) {
                        continue;
                    }
                    switch (up.getType()) {
                        case BUFF_HP -> hp += (int) up.getValue();
                        case BUFF_DAMAGE -> damage += (int) up.getValue();
                        case BUFF_COST -> cost = Math.max(0, cost + (int) up.getValue());
                        case BUFF_RECHARGE -> recharge = Math.max(0f, recharge + up.getValue());
                        default -> {
                        }
                    }
                }
            }
            return new PlantStats(cost, hp, damage, recharge);
        }
    }

    // ── Zombie modal card ──────────────────────────────────────────────────

    private static final class ZombiePage {
        private final CollectionMenuController controller = CollectionMenuController.getInstance();
        private final TextureBank textures;
        private final List<String> names;
        private final boolean blueBg = ZOMBIE_BLUE_BG;
        private int index;

        final Actor root;
        private final BlueModalPanel bluePanel;
        private final IdlePreview preview;
        private final Label title;
        private final Label toughStat;
        private final Label speedStat;
        private final Label descLabel;
        private final Label flavorLabel;

        ZombiePage(Skin skin, PvzAssets assets, PamClipCache clips, TextureBank textures,
                   List<String> names, int start, Runnable onClose) {
            this.textures = textures;
            this.names = names;
            this.index = start;

            Color ink = panelInk(blueBg);
            Color flavor = panelFlavor(blueBg);

            preview = new IdlePreview(assets, clips, ZOMBIE_PAM_SCALE, ZOMBIE_PAM_ANCHOR_Y);
            title = inkLabel(skin, "big", "", ink);
            title.setAlignment(Align.center);
            toughStat = inkLabel(skin, "medium", "", ink);
            speedStat = inkLabel(skin, "medium", "", ink);
            descLabel = inkLabel(skin, "secondary", "", ink);
            descLabel.setWrap(true);
            descLabel.setAlignment(Align.topLeft);
            flavorLabel = inkLabel(skin, "secondary", "", flavor);
            flavorLabel.setWrap(true);
            flavorLabel.setAlignment(Align.topLeft);

            Stack previewStack = new Stack();
            Image previewBg = regionImage(textures, AlmanacArt.STAT_BG);
            previewBg.setFillParent(true);
            previewStack.add(previewBg);
            previewStack.add(preview);

            Table left = new Table();
            left.top();
            left.padTop(ZOMBIE_LEFT_COL_PAD_TOP);
            left.add(previewStack)
                .size(ZOMBIE_PREVIEW_W, ZOMBIE_PREVIEW_H)
                .padTop(ZOMBIE_PREVIEW_PAD_TOP);

            Table stats = new Table();
            stats.add(zombieStatRow(skin, textures, AlmanacArt.ICON_ZOMBIE_TOUGHNESS,
                "TOUGHNESS", toughStat))
                .growX().uniformX().padBottom(ZOMBIE_STAT_ROW_PAD_BOTTOM);
            stats.add(zombieStatRow(skin, textures, AlmanacArt.ICON_ZOMBIE_SPEED,
                "SPEED", speedStat))
                .growX().uniformX().padBottom(ZOMBIE_STAT_ROW_PAD_BOTTOM).row();

            Table right = new Table();
            right.top().left();
            right.padTop(ZOMBIE_RIGHT_COL_PAD_TOP);
            right.add(stats).growX().padBottom(ZOMBIE_STATS_PAD_BOTTOM).row();
            right.add(descLabel).growX().padBottom(ZOMBIE_DESC_PAD_BOTTOM).row();
            right.add(flavorLabel).growX().row();

            Table body = new Table();
            body.add(left).width(ZOMBIE_LEFT_COL_W).top().padRight(BODY_COL_GAP);
            body.add(right).grow().top().left();

            AtlasImageButton prev = new AtlasImageButton(
                textures.region(AlmanacArt.NAV_PREV),
                textures.region(AlmanacArt.NAV_PREV_DOWN),
                NAV_W, NAV_H, this::prev);
            AtlasImageButton next = new AtlasImageButton(
                textures.region(AlmanacArt.NAV_NEXT),
                textures.region(AlmanacArt.NAV_NEXT_DOWN),
                NAV_W, NAV_H, this::next);

            Table middle = new Table();
            middle.add(prev).size(NAV_W, NAV_H).padRight(NAV_PAD);
            middle.add(body).grow().padLeft(BODY_SIDE_PAD).padRight(BODY_SIDE_PAD);
            middle.add(next).size(NAV_W, NAV_H).padLeft(NAV_PAD);

            Table content = new Table();
            float padBottom = blueBg ? ZOMBIE_CARD_PAD_BOTTOM : ZOMBIE_CARD_PAD;
            content.pad(ZOMBIE_CARD_PAD).padBottom(padBottom);
            content.add(title).growX().center().padBottom(ZOMBIE_TITLE_PAD_BOTTOM).row();
            content.add(middle).growX();

            if (blueBg) {
                bluePanel = new BlueModalPanel(
                    textures, content, ZOMBIE_MODAL_W,
                    ZOMBIE_PREVIEW_H + ZOMBIE_CARD_PAD + padBottom + ZOMBIE_RIGHT_COL_PAD_TOP + 40f,
                    onClose);
                root = bluePanel;
            } else {
                bluePanel = null;
                root = creamModalRoot(content, textures, onClose);
            }

            refresh();
        }

        boolean handleNavKey(int keycode) {
            if (names.size() <= 1) {
                return false;
            }
            if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
                prev();
                return true;
            }
            if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
                next();
                return true;
            }
            return false;
        }

        private void prev() {
            if (names.size() <= 1) {
                return;
            }
            GameAudio.get().playNavClick();
            index = (index - 1 + names.size()) % names.size();
            refresh();
        }

        private void next() {
            if (names.size() <= 1) {
                return;
            }
            GameAudio.get().playNavClick();
            index = (index + 1) % names.size();
            refresh();
        }

        private void refresh() {
            String name = names.get(index);
            Collection col = controller.currentCollection();
            boolean discovered = col.ownsZombie(name);
            Zombie zombie = col.getZombie(name);

            title.setText(discovered ? prettyZombieTitle(name) : "???");
            if (!discovered || zombie == null) {
                preview.clearEntity();
                toughStat.setText("—");
                speedStat.setText("—");
                descLabel.setText("You have not seen this zombie in battle yet.");
                flavorLabel.setText("");
                flavorLabel.setVisible(false);
                if (bluePanel != null) {
                    bluePanel.layoutPanel();
                }
                return;
            }

            preview.setZombie(name);
            toughStat.setText(AlmanacZombieLabels.toughnessLabel(zombie));
            speedStat.setText(AlmanacZombieLabels.speedLabel(zombie));
            descLabel.setText(AlmanacZombieLabels.description(zombie));
            String flavor = AlmanacZombieLabels.flavor(zombie);
            flavorLabel.setText(flavor);
            flavorLabel.setVisible(flavor != null && !flavor.isBlank());
            if (bluePanel != null) {
                bluePanel.layoutPanel();
            }
        }

        private Table zombieStatRow(Skin skin, TextureBank textures, String iconId,
                                    String caption, Label value) {
            Table row = new Table();
            row.add(regionImage(textures, iconId))
                .size(STAT_ICON)
                .padRight(STAT_ICON_PAD_RIGHT)
                .top();
            Table text = new Table();
            Label cap = inkLabel(skin, "secondary", caption, panelMuted(blueBg));
            text.add(cap).left().row();
            value.setWrap(true);
            value.setAlignment(Align.left);
            text.add(value).left().growX().padTop(STAT_VALUE_PAD_TOP);
            row.add(text).left().growX().top();
            return row;
        }

        private static String prettyZombieTitle(String id) {
            if (id == null) {
                return "";
            }
            return switch (id) {
                case "ZombieDefault" -> "Basic Zombie";
                case "ZombieArmor1" -> "Conehead Zombie";
                case "ZombieArmor2" -> "Buckethead Zombie";
                case "ZombieArmor4" -> "Brickhead Zombie";
                default -> id.startsWith("Zombie") ? id.substring("Zombie".length()) + " Zombie" : id;
            };
        }
    }

    private static Label inkLabel(Skin skin, String style, String text, Color color) {
        Label label = whiteLabel(skin, style, text, color);
        SkinFonts.scaleLabel(label, skin, style, UI_FONT_SCALE);
        // scaleLabel replaces the style from skin defaults — restore our ink color.
        label.getStyle().fontColor = color.cpy();
        return label;
    }

    /** Wraps skin xp ProgressBar — Table height alone cannot shrink the nine-patch. */
    private static Actor scaledSeedBarSlot(ProgressBar bar, float width) {
        bar.setSize(width, SEED_BAR_H);
        Group scaled = new Group();
        scaled.setTransform(true);
        scaled.setSize(width, SEED_BAR_H);
        scaled.setOrigin(width * 0.5f, SEED_BAR_H * 0.5f);
        scaled.setScale(SEED_BAR_SCALE);
        scaled.addActor(bar);

        float scaledH = SEED_BAR_H * SEED_BAR_SCALE;
        Group slot = new Group();
        slot.setSize(width, scaledH);
        scaled.setPosition(0f, (scaledH - SEED_BAR_H) * 0.5f);
        slot.addActor(scaled);
        return slot;
    }

    private record PlantStats(int cost, int hp, int damage, float recharge) {}

    private static final class IdlePreview extends Actor {
        private final PamPlayer player;
        private final PamCatalog catalog;
        private final PlantSpritesheetCatalog sheets;
        private final PamClipCache clips;
        private final SpritesheetClipCache sheetClips;
        private final float drawScale;
        private final float anchorY;
        private String pamPath;
        private String clipName;
        private PlantSpritesheetCatalog.ClipSpec sheetSpec;
        private Map<String, Boolean> visibility;
        private String plantPamPath;
        private String plantClipName;
        private float sheetOffsetY;
        private float sheetScaleMul = 1f;
        private float time;

        private IdlePreview(PvzAssets assets, PamClipCache clips) {
            this(assets, clips, 0.55f, 0.15f);
        }

        private IdlePreview(PvzAssets assets, PamClipCache clips, float drawScale, float anchorY) {
            this.player = assets.player;
            this.catalog = assets.pamCatalog;
            this.sheets = assets.plantSheets;
            this.clips = clips;
            this.sheetClips = new SpritesheetClipCache(assets.root);
            this.drawScale = drawScale;
            this.anchorY = anchorY;
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
            if (sheets != null && sheets.hasSheets(name)) {
                // Full sheet loop for almanac preview (e.g. Cat-tail attack = all frames).
                sheetSpec = sheets.resolveClip(name, "attack");
                if (sheetSpec == null) {
                    sheetSpec = sheets.anyClip(name);
                }
                if (sheetSpec != null) {
                    if ("Cat-tail".equalsIgnoreCase(name)) {
                        sheetOffsetY = CATTAIL_PREVIEW_OFFSET_Y;
                        sheetScaleMul = CATTAIL_PREVIEW_SCALE;
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
            if ("Magnet-shroom".equalsIgnoreCase(name)) {
                visibility = PamVisibility.hide(MAGNET_ITEM_PART);
            }
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
                        sheetOffsetY = SUNSHINE_PREVIEW_OFFSET_Y;
                        sheetScaleMul = SUNSHINE_PREVIEW_SCALE;
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
            float cy = getY() + getHeight() * anchorY + sheetOffsetY;
            if (sheetSpec != null) {
                SpritesheetClipCache.SheetAnim sheet = sheetClips.getOrLoad(sheetSpec);
                if (sheet != null && sheet.animation() != null) {
                    TextureRegion frame = sheet.animation().getKeyFrame(time, true);
                    if (frame != null) {
                        float scale = drawScale * 0.85f * sheetScaleMul;
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
            if (visibility != null) {
                player.draw(batch, ref, time, cx, cy, drawScale, drawScale, true, visibility);
            } else {
                player.draw(batch, ref, time, cx, cy, drawScale, drawScale, true);
            }
            drawZombotanyPlantHead(batch, ref, cx, cy, drawScale);
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
