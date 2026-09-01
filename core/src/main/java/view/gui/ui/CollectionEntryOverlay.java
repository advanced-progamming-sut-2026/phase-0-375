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

    static final String MAGNET_ITEM_PART = "Magnet_Item";
    static final String CLOSE_UP = "IMAGE_UI_GENERIC_CLOSE_CIRCLE";
    static final String CLOSE_DOWN = "IMAGE_UI_GENERIC_CLOSE_CIRCLE_DOWN";

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
        CollectionPlantPage page = new CollectionPlantPage(skin, assets, clips, textures, names, start,
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
        CollectionZombiePage page = new CollectionZombiePage(skin, assets, clips, textures, names, start,
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

    static TextureRegion blueFillRegion(TextureBank textures) {
        TextureRegion mini = textures.region(ShopArt.MINISTORE_BG);
        return mini != null ? paintedMinistoreCore(mini) : new TextureRegion(zombieBlueGradient());
    }

    static Color panelInk(boolean blue) {
        return blue ? BLUE_INK : INK;
    }

    static Color panelMuted(boolean blue) {
        return blue ? BLUE_MUTED : MUTED;
    }

    static Color panelFlavor(boolean blue) {
        return blue ? BLUE_FLAVOR : FLAVOR_YELLOW;
    }

    static Color panelPlantFoodGreen(boolean blue) {
        return blue ? BLUE_PF_GREEN : PF_GREEN;
    }

    /** Cream BorderedTable shell (classic overlay). Content should already include pad. */
    static Stack creamModalRoot(Table content, TextureBank textures, Runnable onClose) {
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

    static Image regionImage(TextureBank textures, String id) {
        TextureRegion region = textures.region(id);
        Image image = new Image(region != null ? new TextureRegionDrawable(region) : null);
        image.setScaling(Scaling.fit);
        return image;
    }

    static Label whiteLabel(Skin skin, String style, String text, Color color) {
        Label.LabelStyle base = skin.get(style, Label.LabelStyle.class);
        Label.LabelStyle copy = new Label.LabelStyle(base);
        copy.fontColor = color.cpy();
        Label label = new Label(text, copy);
        label.setAlignment(Align.left);
        return label;
    }

    static Label inkLabel(Skin skin, String style, String text, Color color) {
        Label label = whiteLabel(skin, style, text, color);
        SkinFonts.scaleLabel(label, skin, style, UI_FONT_SCALE);
        label.getStyle().fontColor = color.cpy();
        return label;
    }

    /** Wraps skin xp ProgressBar — Table height alone cannot shrink the nine-patch. */
    static Actor scaledSeedBarSlot(ProgressBar bar, float width) {
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
}
