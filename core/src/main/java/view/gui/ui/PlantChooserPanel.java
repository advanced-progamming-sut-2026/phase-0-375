package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.ray3k.tenpatch.TenPatchDrawable;
import model.collection.Collection;
import model.enums.PlantAbilityType;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import view.gui.anim.AnimScale;
import view.gui.anim.PamClipCache;
import view.gui.anim.SpritesheetClipCache;
import view.gui.assets.PamCatalog;
import view.gui.assets.PlantSpritesheetCatalog;
import view.gui.assets.PvzAssets;

/**
 * Plant picker: chooser plant card then a packet grid.
 * Panel chrome is {@code IMAGE_UI_ALMANAC_SELECTOR_BKGD}.
 */
public final class PlantChooserPanel extends Table implements Disposable {
    public static final int GRID_COLS = 5;
    static final String PIRATE_BG = "IMAGE_UI_CARDS_BACKGROUNDS_CARD_PLANT_BG_PIRATE";
    static final String CHOOSER_CARD = "IMAGE_UI_CARDS_CHOOSER_CHOOSER_PLANT_CARD";
    static final String SELECTOR_BG = "IMAGE_UI_ALMANAC_SELECTOR_BKGD";
    private static final float CARD_GREEN_HEIGHT = 40f;
    private static final float AVATAR = 160f;
    private static final Color TITLE = new Color(1f, 1f, 1f, 1f);
    private static final Color BODY = new Color(67f / 255f, 62f / 255f, 0f, 1f);

    private final Skin skin;
    private final PamClipCache clips;
    private final Listener listener;

    private final Label title;
    private final Label description;
    private final TextButton upgrade;
    private final TextButton boost;
    private final PamPreview preview;
    private final Table grid;
    private String inspected;

    public interface Listener {
        void onToggle(String plantName, boolean locked);

        void onUpgrade(String plantName);

        void onBoost(String plantName);
    }

    public PlantChooserPanel(Skin skin, PvzAssets assets, Listener listener) {
        this.skin = skin;
        this.clips = new PamClipCache(assets.player);
        this.listener = listener;
        assets.textures.loadSync("UI_AlwaysLoadedTiles_768");
        assets.textures.loadSync("ATLASIMAGE_ATLAS_UI_ALWAYSLOADEDTILES_768_00");
        assets.textures.loadSync("UI_AlwaysLoaded_Uncompressed_768");
        assets.textures.loadSync("ATLASIMAGE_ATLAS_UI_ALWAYSLOADED_UNCOMPRESSED_768_00");
        assets.textures.loadSync("UI_Almanac_768");
        assets.textures.loadSync("ATLASIMAGE_ATLAS_UI_ALMANAC_768_00");
        pad(12f, 16f, 16f, 16f);
        Drawable panelBg = stretchStrip(assets.textures, SELECTOR_BG);
        if (panelBg == null) {
            panelBg = UiDrawables.tryNamed(skin, "image_ui_almanac_selector_bkgd");
        }
        if (panelBg != null) {
            setBackground(panelBg);
        }

        Table card = new Table();
        Drawable cardBg = chooserCard(assets.textures);
        if (cardBg == null) {
            cardBg = UiDrawables.tryNamed(skin, "image_ui_cards_chooser_chooser_plant_card");
        }
        if (cardBg != null) {
            card.setBackground(cardBg);
        }
        title = new Label("Select a plant", skin, "big_outline");
        title.setAlignment(Align.center);
        title.setColor(TITLE);
        card.add(title).growX().height(CARD_GREEN_HEIGHT).pad(16f, 16f, 4f, 16f).row();

        preview = new PamPreview(assets, clips);
        Table right = new Table();
        description = new Label("", skin, "secondary");
        description.setWrap(true);
        description.setAlignment(Align.topLeft);
        description.setColor(BODY);
        right.add(description).grow().top().left().padBottom(8f).padTop(32f).row();

        Table actions = new Table();
        upgrade = actionButton("purple", "image_ui_generic_coin_icon_small");
        upgrade.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (inspected != null) {
                    listener.onUpgrade(inspected);
                }
            }
        });
        boost = actionButton("green", "image_ui_generic_gem_icon_small");
        boost.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (inspected != null) {
                    listener.onBoost(inspected);
                }
            }
        });
        upgrade.setDisabled(true);
        boost.setDisabled(true);
        actions.add(upgrade).width(176f).height(44f).padRight(8f);
        actions.add(boost).width(176f).height(44f);
        right.add(actions).growX().bottom().left();

        Table body = new Table();
        body.add(preview).size(AVATAR, AVATAR).top().pad(8f, 12f, 12f, 8f);
        body.add(right).grow().top().pad(8f, 8f, 12f, 16f);
        card.add(body).growX();
        add(card).growX().padBottom(10f).row();

        Table well = new Table();
        grid = new Table();
        grid.top().left();
        ScrollPane scroll = new ScrollPane(grid, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setFlickScroll(true);
        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1 && scroll.getStage() != null) {
                    scroll.getStage().setScrollFocus(scroll);
                }
            }
        });
        well.add(scroll).grow().pad(8f);
        add(well).grow().minHeight(240f);
    }

    public void inspect(String plantName, boolean unlocked, boolean boosted, int level) {
        inspected = plantName;
        if (plantName == null) {
            title.setText("Select a plant");
            description.setText("");
            upgrade.setDisabled(true);
            boost.setDisabled(true);
            preview.setPlant(null);
            return;
        }
        title.setText(plantName);
        description.setText(blurb(plantName));
        preview.setPlant(plantName);
        boolean maxed = level >= Collection.MAX_PLANT_LEVEL;
        upgrade.setDisabled(!unlocked || maxed);
        upgrade.setText(maxed ? "Max level" : "Upgrade " + upgradeCoins(level));
        boost.setDisabled(!unlocked || boosted);
        boost.setText(boosted ? "Boosted" : "Boost 2");
    }

    public void setGrid(Iterable<SeedPacketActor> packets) {
        grid.clearChildren();
        int col = 0;
        for (SeedPacketActor packet : packets) {
            grid.add(packet).size(SeedPacketActor.PACKET_WIDTH, SeedPacketActor.PACKET_HEIGHT).pad(4f);
            col++;
            if (col >= GRID_COLS) {
                grid.row();
                col = 0;
            }
        }
    }

    public String inspected() {
        return inspected;
    }

    @Override
    public void dispose() {
        preview.dispose();
    }

    private TextButton actionButton(String styleName, String iconName) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(
                skin.get(styleName, TextButton.TextButtonStyle.class));
        Drawable disabled = UiDrawables.tenPatch(skin, "image_ui_generic_disabledbutton");
        if (disabled == null) {
            disabled = UiDrawables.tryNamed(skin, "image_ui_generic_disabledbutton");
        }
        if (disabled != null) {
            style.disabled = disabled;
        }
        TextButton button = new TextButton("", style);
        button.clearChildren();
        Drawable icon = UiDrawables.tryNamed(skin, iconName);
        if (icon != null) {
            Image image = new Image(icon);
            image.setTouchable(Touchable.disabled);
            button.add(image).size(22f, 22f).padLeft(10f).padRight(6f);
        }
        button.add(button.getLabel()).expandX().left().padRight(10f);
        return button;
    }

    private static int upgradeCoins(int level) {
        int next = Math.max(level, Collection.MIN_PLANT_LEVEL) + 1;
        return 500 * (next - 1);
    }

    /** Stretch a vertical 9-slice strip; do not scale the raw region. */
    static Drawable chooserCard(TextureBank textures) {
        return stretchStrip(textures, CHOOSER_CARD);
    }

    static Drawable stretchStrip(TextureBank textures, String id) {
        TextureRegion region = textures == null || id == null ? null : textures.region(id);
        if (region == null) {
            return null;
        }
        int w = region.getRegionWidth();
        int h = region.getRegionHeight();
        int x0 = Math.min(10, Math.max(0, w / 4));
        int x1 = Math.max(x0, w - x0 - 1);
        int y0 = Math.min(10, Math.max(0, h / 8));
        int y1 = Math.max(y0, h - 24 - 1);
        TenPatchDrawable drawable = new TenPatchDrawable(new int[] { x0, x1 }, new int[] { y0, y1 }, false, region);
        drawable.setMinWidth(0f);
        drawable.setMinHeight(0f);
        return drawable;
    }

    private static String blurb(String plantName) {
        if (!PlantFactory.hasDefinition(plantName)) {
            return "";
        }
        Plant plant = PlantFactory.getDefinition(plantName);
        PlantAbilityType ability = plant.getAbilityType();
        if (ability == null) {
            return plant.getCategory() == null ? "" : plant.getCategory().name();
        }
        return switch (ability) {
            case PRODUCE_SUN -> "Produces sun over time.";
            case INSTANT_SUN_BURST -> "Gives a burst of sun when planted.";
            case SHOOT_PROJECTILE -> "Shoots projectiles down its lane.";
            case INSTANT_EXPLOSIVE -> "Explodes as soon as it is planted.";
            case DELAYED_EXPLOSIVE -> "Arms, then explodes when zombies get close.";
            case MELEE_ATTACK -> "Hits nearby zombies in melee range.";
            case PASSIVE_SHIELD -> "Absorbs bites and blocks the lane.";
            case MODIFIER_UTILITY -> "Changes the lane instead of dealing direct damage.";
            case MINT_FAMILY_BOOST -> "Boosts every plant of the same family for a short time.";
        };
    }

    /** Cover-fit dest inside a box, local to the box origin. */
    static void coverDest(float boxW, float boxH, float imgW, float imgH, Rectangle out) {
        float scale = Math.max(boxW / imgW, boxH / imgH);
        float dw = imgW * scale;
        float dh = imgH * scale;
        out.set((boxW - dw) * 0.5f, (boxH - dh) * 0.5f, dw, dh);
    }

    private static final class PamPreview extends Actor implements Disposable {
        private final TextureBank textures;
        private final PamPlayer player;
        private final PamCatalog catalog;
        private final PlantSpritesheetCatalog sheets;
        private final PamClipCache clips;
        private final SpritesheetClipCache sheetClips;
        private final RectClipShader clip = new RectClipShader();
        private final Rectangle mask = new Rectangle();
        private final Rectangle cover = new Rectangle();
        private String pamPath;
        private String clipName;
        private PlantSpritesheetCatalog.ClipSpec sheetSpec;
        private float sheetOffsetY;
        private float sheetScaleMul = 1f;
        private float time;

        PamPreview(PvzAssets assets, PamClipCache clips) {
            this.textures = assets.textures;
            this.player = assets.player;
            this.catalog = assets.pamCatalog;
            this.sheets = assets.plantSheets;
            this.clips = clips;
            this.sheetClips = assets.root != null ? new SpritesheetClipCache(assets.root) : null;
        }

        void setPlant(String plantName) {
            pamPath = null;
            clipName = null;
            sheetSpec = null;
            sheetOffsetY = 0f;
            sheetScaleMul = 1f;
            time = 0f;
            if (plantName == null) {
                return;
            }
            if (sheets != null && sheets.hasSheets(plantName)) {
                sheetSpec = sheets.resolveClip(plantName, "idle", "attack");
                if (sheetSpec == null) {
                    sheetSpec = sheets.anyClip(plantName);
                }
                if (sheetSpec != null) {
                    if ("Cat-tail".equalsIgnoreCase(plantName)) {
                        sheetScaleMul = AnimScale.PLANT;
                    }
                    return;
                }
            }
            PamCatalog.PamEntry entry = catalog.forPlant(plantName);
            if (entry == null) {
                return;
            }
            pamPath = entry.path();
            clipName = catalog.resolveClip(entry, "idle", "idle2", "idle1", "loop");
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            time += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            // SpriteBatch leaves a_position in draw space; Table parents use transform=false
            // so getX/Y during draw already match batch.draw coordinates.
            mask.set(getX(), getY(), getWidth(), getHeight());
            clip.begin(batch, mask);
            Color old = batch.getColor();
            batch.setColor(old.r, old.g, old.b, old.a * parentAlpha);
            TextureRegion bg = textures.region(PIRATE_BG);
            if (bg != null) {
                coverDest(getWidth(), getHeight(), bg.getRegionWidth(), bg.getRegionHeight(), cover);
                batch.draw(bg, getX() + cover.x, getY() + cover.y, cover.width, cover.height);
            }
            batch.setColor(old);
            if (sheetSpec != null && sheetClips != null) {
                SpritesheetClipCache.SheetAnim sheet = sheetClips.getOrLoad(sheetSpec);
                if (sheet != null && sheet.animation() != null) {
                    TextureRegion frame = sheet.animation().getKeyFrame(time, true);
                    if (frame != null) {
                        float scale = 0.55f * sheetScaleMul;
                        float w = frame.getRegionWidth() * scale;
                        float h = frame.getRegionHeight() * scale;
                        float cx = getX() + getWidth() * 0.5f;
                        float cy = getY() + getHeight() * 0.15f + sheetOffsetY;
                        batch.draw(frame, cx - w * 0.5f, cy, w, h);
                    }
                }
            } else if (pamPath != null && clipName != null) {
                ClipRef ref = clips.getOrLoad(pamPath, clipName);
                if (ref != null) {
                    float scale = AnimScale.PLANT * 0.7f;
                    player.draw(batch, ref, time, getX() + getWidth() * 0.5f, getY() + getHeight() * 0.45f,
                            scale, scale, true);
                }
            }
            clip.end(batch);
        }

        @Override
        public void dispose() {
            clip.dispose();
            if (sheetClips != null) {
                sheetClips.dispose();
            }
        }
    }

    /**
     * Keeps fragments inside a stage-space AABB; discards the rest.
     * Bind after {@link Batch#setShader} — setShader does not bind.
     */
    private static final class RectClipShader implements Disposable {
        private static final String VERT = """
                attribute vec4 a_position;
                attribute vec4 a_color;
                attribute vec2 a_texCoord0;
                uniform mat4 u_projTrans;
                varying vec4 v_color;
                varying vec2 v_texCoords;
                varying vec2 v_world;
                void main() {
                    v_world = a_position.xy;
                    v_color = a_color;
                    v_color.a = v_color.a * (255.0 / 254.0);
                    v_texCoords = a_texCoord0;
                    gl_Position = u_projTrans * a_position;
                }
                """;

        private static final String FRAG = """
                #ifdef GL_ES
                precision mediump float;
                #endif
                varying vec4 v_color;
                varying vec2 v_texCoords;
                varying vec2 v_world;
                uniform sampler2D u_texture;
                uniform vec2 u_maskMin;
                uniform vec2 u_maskMax;
                void main() {
                    if (v_world.x < u_maskMin.x || v_world.x >= u_maskMax.x
                            || v_world.y < u_maskMin.y || v_world.y >= u_maskMax.y) {
                        discard;
                    }
                    gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
                }
                """;

        private final ShaderProgram program;

        RectClipShader() {
            ShaderProgram.pedantic = false;
            program = new ShaderProgram(VERT, FRAG);
            if (!program.isCompiled()) {
                throw new IllegalStateException("Plant preview clip shader: " + program.getLog());
            }
        }

        void begin(Batch batch, Rectangle mask) {
            batch.setShader(program);
            program.bind();
            program.setUniformf("u_maskMin", mask.x, mask.y);
            program.setUniformf("u_maskMax", mask.x + mask.width, mask.y + mask.height);
        }

        void end(Batch batch) {
            batch.flush();
            batch.setShader(null);
        }

        @Override
        public void dispose() {
            program.dispose();
        }
    }
}
