package view.gui.screen;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pvz.libpvz.textures.TextureBank;
import view.gui.anim.SpritesheetClipCache;
import view.gui.anim.zombie.SunshineAnim;
import view.gui.assets.AlmanacZombiePacketIds;
import view.gui.assets.PlantSpritesheetCatalog;
import view.gui.assets.PvzAssets;
import view.gui.ui.CollectionEntryOverlay;
import view.gui.ui.ZombieAlmanacPacket;

final class CollectionZombiePortraits {
    private CollectionZombiePortraits() {}

    static void applyIfNeeded(ZombieAlmanacPacket packet, String zombieName,
                              TextureBank textures, PvzAssets assets,
                              SpritesheetClipCache sheetClips) {
        if (packet == null || zombieName == null || textures == null) {
            return;
        }
        String atlasId = AlmanacZombiePacketIds.portraitId(zombieName);
        if (atlasId != null && textures.region(atlasId) != null) {
            return;
        }
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
}
