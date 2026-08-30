package view.gui.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Disposable;
import model.app.App;
import model.enums.GameState;
import model.game.core.PvZGameLoop;
import pvz.libpvz.textures.TextureBank;
import view.gui.anim.SpritesheetClipCache;
import view.gui.assets.PvzAssets;
import view.gui.assets.SheetPacketPortraits;
import view.gui.assets.UiRegions;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual conveyor belt HUD component on the left side of the screen.
 *
 * <p>Renders:
 * <ul>
 *   <li>Continuous scrolling {@code IMAGE_UI_CONVEYOR_CONVEYOR_BELT} segments moving upward.</li>
 *   <li>{@code IMAGE_UI_CONVEYOR_CONVEYOR_SIDE} rail along the right edge.</li>
 *   <li>{@code IMAGE_UI_CONVEYOR_CONVEYOR_TOP} cap machinery across the top edge.</li>
 *   <li>Smooth upward translation of arriving seed packets from bottom off-screen,
 *       stacking vertically with non-tight spacing.</li>
 *   <li>Automatic upward slot advancement when a packet is planted.</li>
 * </ul>
 */
public final class ConveyorBeltHud extends WidgetGroup implements Disposable {

    public static final float TRACK_WIDTH = 127f;
    public static final float SIDE_WIDTH = 13f;
    public static final float TOTAL_WIDTH = TRACK_WIDTH + SIDE_WIDTH; // 140f
    public static final float TOTAL_HEIGHT = 768f;

    public static final float TRACK_SEGMENT_H = 18f;
    public static final float TOP_CAP_H = 10f;
    public static final float TOP_MARGIN = 8f;
    public static final float PACKET_GAP = 8f;
    public static final float PACKET_X = (TRACK_WIDTH - SeedPacketActor.PACKET_WIDTH) * 0.5f; // 4f
    public static final float TOP_REST_Y =
            TOTAL_HEIGHT - TOP_CAP_H - TOP_MARGIN - SeedPacketActor.PACKET_HEIGHT; // 675f

    /** Upward translation speed of packets and belt track (pixels per second). */
    public static final float BELT_SPEED = 140f;

    public interface DragCallback {
        void onDragStart(SeedPacketActor packet, String plantName);
        void onDrag(SeedPacketActor packet, String plantName, float stageX, float stageY);
        void onDragEnd(SeedPacketActor packet, String plantName, float stageX, float stageY);
    }

    public static final class ConveyorItem {
        public final String plantName;
        public final SeedPacketActor actor;
        public float currentY;
        public float targetY;
        public boolean dragging;

        public ConveyorItem(String plantName, SeedPacketActor actor, float startY, float targetY) {
            this.plantName = plantName;
            this.actor = actor;
            this.currentY = startY;
            this.targetY = targetY;
        }
    }

    private final TextureBank textures;
    private final PvzAssets assets;
    private final Skin skin;
    private final DragCallback dragCallback;
    private final SpritesheetClipCache sheetClips;

    private TextureRegion beltRegion;
    private TextureRegion sideRegion;
    private TextureRegion topRegion;

    private Texture fallbackBeltTex;
    private Texture fallbackSideTex;
    private Texture fallbackTopTex;

    private final List<ConveyorItem> items = new ArrayList<>();
    private float beltTrackOffsetY = 0f;
    private ConveyorItem lastDraggedItem;

    public ConveyorBeltHud(PvzAssets assets, Skin skin, DragCallback dragCallback) {
        this.assets = assets;
        this.textures = assets != null ? assets.textures : null;
        this.skin = skin;
        this.dragCallback = dragCallback;
        this.sheetClips = assets != null && assets.root != null
                ? new SpritesheetClipCache(assets.root)
                : null;

        setSize(TOTAL_WIDTH, TOTAL_HEIGHT);
        setPosition(0f, 0f);
        setTouchable(Touchable.childrenOnly);

        loadTextures(assets);
    }

    private void loadTextures(PvzAssets assets) {
        if (textures != null) {
            beltRegion = textures.region(UiRegions.CONVEYOR_BELT);
            sideRegion = textures.region(UiRegions.CONVEYOR_SIDE);
            topRegion = textures.region(UiRegions.CONVEYOR_TOP);
        }
        if (beltRegion == null) {
            fallbackBeltTex = loadFallbackTexture(assets, "conveyor_belt.png", 127, 18, new Color(0.25f, 0.25f, 0.25f, 1f));
            if (fallbackBeltTex != null) beltRegion = new TextureRegion(fallbackBeltTex);
        }
        if (sideRegion == null) {
            fallbackSideTex = loadFallbackTexture(assets, "conveyor_side.png", 13, 768, new Color(0.35f, 0.35f, 0.35f, 1f));
            if (fallbackSideTex != null) sideRegion = new TextureRegion(fallbackSideTex);
        }
        if (topRegion == null) {
            fallbackTopTex = loadFallbackTexture(assets, "conveyor_top.png", 127, 10, new Color(0.15f, 0.15f, 0.15f, 1f));
            if (fallbackTopTex != null) topRegion = new TextureRegion(fallbackTopTex);
        }
    }

    private static Texture loadFallbackTexture(PvzAssets assets, String fileName, int fallbackW, int fallbackH, Color fallbackColor) {
        FileHandle handle = resolveFile(assets, fileName);
        if (handle != null && handle.exists()) {
            try {
                Texture tex = new Texture(handle);
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                return tex;
            } catch (Exception ignored) {}
        }
        // Safe fallback placeholder
        Pixmap pix = new Pixmap(fallbackW, fallbackH, Pixmap.Format.RGBA8888);
        pix.setColor(fallbackColor);
        pix.fill();
        Texture tex = new Texture(pix);
        pix.dispose();
        return tex;
    }

    private static FileHandle resolveFile(PvzAssets assets, String fileName) {
        if (assets != null && assets.root != null) {
            FileHandle f = assets.root.child("Exports/" + fileName);
            if (f.exists()) return f;
            f = assets.root.child(fileName);
            if (f.exists()) return f;
        }
        FileHandle local = Gdx.files.local("assets/Exports/" + fileName);
        if (local.exists()) return local;
        local = Gdx.files.local("assets/" + fileName);
        if (local.exists()) return local;
        FileHandle abs = Gdx.files.absolute("C:/Users/ahgha/Desktop/pvz-assets/Exports/" + fileName);
        if (abs.exists()) return abs;
        return null;
    }

    public static float computeSlotY(int slotIndex) {
        return TOP_REST_Y - slotIndex * (SeedPacketActor.PACKET_HEIGHT + PACKET_GAP);
    }

    /**
     * Synchronizes conveyor items with the model's plant card list.
     */
    public void sync(List<String> modelPlants) {
        if (modelPlants == null) {
            modelPlants = List.of();
        }

        // 1. Check if cards were removed (e.g. planted)
        while (items.size() > modelPlants.size()) {
            ConveyorItem toRemove = null;
            if (lastDraggedItem != null && items.contains(lastDraggedItem)) {
                // If the last dragged item matches one of the removed items
                toRemove = lastDraggedItem;
                lastDraggedItem = null;
            } else {
                // Find first item that differs or remove the first item
                for (int i = 0; i < items.size(); i++) {
                    if (i >= modelPlants.size() || !items.get(i).plantName.equals(modelPlants.get(i))) {
                        toRemove = items.get(i);
                        break;
                    }
                }
                if (toRemove == null && !items.isEmpty()) {
                    toRemove = items.get(0);
                }
            }
            if (toRemove != null) {
                toRemove.actor.remove();
                items.remove(toRemove);
            }
        }

        // 2. Check if new cards were added (delivered from bottom)
        if (items.size() < modelPlants.size()) {
            for (int i = items.size(); i < modelPlants.size(); i++) {
                String plantName = modelPlants.get(i);
                SeedPacketActor packet = new SeedPacketActor(
                        textures, skin, plantName, 0, 1, false, false, false);
                SheetPacketPortraits.applyIfNeeded(packet, plantName, assets, sheetClips);

                ConveyorItem item = new ConveyorItem(
                        plantName, packet, calculateSpawnY(), computeSlotY(i));

                packet.onDragPlant(new SeedPacketActor.DragPlant() {
                    @Override
                    public void dragStart(SeedPacketActor actor) {
                        item.dragging = true;
                        lastDraggedItem = item;
                        if (dragCallback != null) {
                            dragCallback.onDragStart(actor, item.plantName);
                        }
                    }

                    @Override
                    public void drag(SeedPacketActor actor, float stageX, float stageY) {
                        if (dragCallback != null) {
                            dragCallback.onDrag(actor, item.plantName, stageX, stageY);
                        }
                    }

                    @Override
                    public void dragEnd(SeedPacketActor actor, float stageX, float stageY) {
                        item.dragging = false;
                        if (dragCallback != null) {
                            dragCallback.onDragEnd(actor, item.plantName, stageX, stageY);
                        }
                        // Reset visual local position if still attached
                        actor.setPosition(PACKET_X, item.currentY);
                    }
                });

                packet.setPosition(PACKET_X, item.currentY);
                items.add(item);
                addActor(packet);
            }
        }

        // 3. Recalculate target slots for all current items
        for (int i = 0; i < items.size(); i++) {
            ConveyorItem item = items.get(i);
            item.targetY = computeSlotY(i);
        }
    }

    private float calculateSpawnY() {
        if (items.isEmpty()) {
            return -SeedPacketActor.PACKET_HEIGHT;
        }
        float lowestY = 0f;
        for (ConveyorItem item : items) {
            lowestY = Math.min(lowestY, item.currentY);
        }
        return lowestY - (SeedPacketActor.PACKET_HEIGHT + PACKET_GAP);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (isGamePaused()) {
            return;
        }

        for (ConveyorItem item : items) {
            if (!item.dragging && item.currentY < item.targetY) {
                item.currentY = Math.min(item.targetY, item.currentY + BELT_SPEED * delta);
                item.actor.setPosition(PACKET_X, item.currentY);
            }
        }

        // Continuously scroll the belt track upwards
        beltTrackOffsetY = (beltTrackOffsetY + BELT_SPEED * delta) % TRACK_SEGMENT_H;
    }

    private static boolean isGamePaused() {
        PvZGameLoop loop = App.getInstance().getCurrentGameLoop();
        return loop != null && loop.getGameState() == GameState.PAUSED;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        float x = getX();
        float y = getY();
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

        // 1. Draw conveyor belt segments stacked from bottom to top
        if (beltRegion != null) {
            float startY = -TRACK_SEGMENT_H + beltTrackOffsetY;
            for (float cy = startY; cy < TOTAL_HEIGHT + TRACK_SEGMENT_H; cy += TRACK_SEGMENT_H) {
                batch.draw(beltRegion, x, y + cy, TRACK_WIDTH, TRACK_SEGMENT_H);
            }
        }

        // 2. Draw seed packet children
        super.draw(batch, parentAlpha);

        // 3. Draw side border rail
        if (sideRegion != null) {
            batch.draw(sideRegion, x + TRACK_WIDTH, y, SIDE_WIDTH, TOTAL_HEIGHT);
        }

        // 4. Draw top cap machinery
        if (topRegion != null) {
            batch.draw(topRegion, x, y + TOTAL_HEIGHT - TOP_CAP_H, TRACK_WIDTH, TOP_CAP_H);
        }
    }

    public List<SeedPacketActor> getPacketActors() {
        List<SeedPacketActor> list = new ArrayList<>(items.size());
        for (ConveyorItem item : items) {
            list.add(item.actor);
        }
        return list;
    }

    @Override
    public void dispose() {
        if (fallbackBeltTex != null) {
            fallbackBeltTex.dispose();
            fallbackBeltTex = null;
        }
        if (fallbackSideTex != null) {
            fallbackSideTex.dispose();
            fallbackSideTex = null;
        }
        if (fallbackTopTex != null) {
            fallbackTopTex.dispose();
            fallbackTopTex = null;
        }
        if (sheetClips != null) {
            sheetClips.dispose();
        }
    }
}
