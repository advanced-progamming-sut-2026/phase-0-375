package view.gui.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pvz.libpvz.textures.TextureBank;

/**
 * Joust avatar portraits from {@code UI_Avatars_768}.
 */
public final class AvatarArt {
    public static final String ATLAS = "ATLASIMAGE_ATLAS_UI_AVATARS_768_00";

    public static final int MIN_ID = 1;
    public static final int MAX_ID = 30;
    public static final int COUNT = MAX_ID - MIN_ID + 1;
    public static final int DEFAULT_ID = 1;

    public static final String BORDER = "IMAGE_UI_JOUST_AVATARS_AVATAR_BORDER";
    public static final String SELECTED = "IMAGE_UI_JOUST_AVATARS_AVATAR_SELECTED";

    private AvatarArt() {}

    public static boolean isValid(int id) {
        return id >= MIN_ID && id <= MAX_ID;
    }

    public static int normalize(int id) {
        return isValid(id) ? id : DEFAULT_ID;
    }

    public static String regionId(int id) {
        return "IMAGE_UI_JOUST_AVATARS_AVATAR_" + normalize(id);
    }

    public static void ensureLoaded(TextureBank textures) {
        if (textures != null) {
            textures.loadSync(ATLAS);
        }
    }

    public static TextureRegion region(TextureBank textures, String regionId) {
        if (textures == null || regionId == null || regionId.isBlank()) {
            return null;
        }
        ensureLoaded(textures);
        return textures.region(regionId);
    }

    public static TextureRegion faceRegion(TextureBank textures, int avatarId) {
        return region(textures, regionId(avatarId));
    }
}
