package view.gui.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import view.gui.anim.SpritesheetClipCache;
import view.gui.ui.CollectionEntryOverlay;
import view.gui.ui.SeedPacketActor;

/**
 * Spritesheet fallbacks for seed-packet portraits when no {@code IMAGE_UI_PACKETS_*} art exists
 * (e.g. Cat-tail). Shared by collection, shop, plant selection, and in-game HUD.
 */
public final class SheetPacketPortraits {
    private SheetPacketPortraits() {}

    /** Applies a sheet frame to {@code packet} when atlas portrait is missing. */
    public static void applyIfNeeded(SeedPacketActor packet, String plantName,
                                     PvzAssets assets, SpritesheetClipCache sheetClips) {
        if (packet == null || plantName == null || SeedPacketIds.portraitId(plantName) != null) {
            return;
        }
        TextureRegion frame = idleFrame(plantName, assets, sheetClips);
        if (frame == null) {
            return;
        }
        if ("Cat-tail".equalsIgnoreCase(plantName)) {
            packet.setPortraitOverride(
                    frame,
                    CollectionEntryOverlay.CATTAIL_PACKET_PORTRAIT_SCALE,
                    CollectionEntryOverlay.CATTAIL_PACKET_PORTRAIT_OFFSET_X,
                    CollectionEntryOverlay.CATTAIL_PACKET_PORTRAIT_OFFSET_Y);
        } else {
            packet.setPortraitOverride(frame);
        }
    }

    /** Idle / first sheet frame for a plant, or {@code null}. */
    public static TextureRegion idleFrame(String plantName, PvzAssets assets,
                                          SpritesheetClipCache sheetClips) {
        if (plantName == null || assets == null || assets.plantSheets == null || sheetClips == null) {
            return null;
        }
        PlantSpritesheetCatalog.ClipSpec spec = assets.plantSheets.idleFallback(plantName);
        if (spec == null) {
            return null;
        }
        SpritesheetClipCache.SheetAnim sheet = sheetClips.getOrLoad(spec);
        if (sheet == null || sheet.animation() == null) {
            return null;
        }
        return sheet.animation().getKeyFrame(0f);
    }
}
