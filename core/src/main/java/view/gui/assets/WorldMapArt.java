package view.gui.assets;

import com.badlogic.gdx.Gdx;
import model.enums.Chapter;
import pvz.libpvz.textures.TextureBank;

/**
 * Season world-map atlases / platform / decoration IDs.
 * Call {@link #configure(Chapter)} before {@link #ensureLoaded}.
 */
public final class WorldMapArt {
    public static final String ATLAS_EGYPT = "WorldMap_Egypt_768";
    public static final String ATLAS_EGYPT_PAGE_0 = "ATLASIMAGE_ATLAS_WORLDMAP_EGYPT_768_00";
    public static final String ATLAS_EGYPT_PAGE_1 = "ATLASIMAGE_ATLAS_WORLDMAP_EGYPT_768_01";
    public static final String ATLAS_ICEAGE = "WorldMap_Iceage_768";
    public static final String ATLAS_ICEAGE_PAGE = "ATLASIMAGE_ATLAS_WORLDMAP_ICEAGE_768_00";
    public static final String ATLAS_ICEAGE_NONPVR = "WorldMap_Iceage_NonPVR_768";
    public static final String ATLAS_ICEAGE_NONPVR_PAGE = "ATLASIMAGE_ATLAS_WORLDMAP_ICEAGE_NONPVR_768_00";
    public static final String ATLAS_BEACH = "WorldMap_Beach_768";
    public static final String ATLAS_BEACH_PAGE = "ATLASIMAGE_ATLAS_WORLDMAP_BEACH_768_00";
    public static final String ATLAS_BEACH_NONPVR = "WorldMap_Beach_NonPVR_768";
    public static final String ATLAS_BEACH_NONPVR_PAGE = "ATLASIMAGE_ATLAS_WORLDMAP_BEACH_NONPVR_768_00";
    public static final String ATLAS_DARK = "WorldMap_Dark_768";
    public static final String ATLAS_DARK_PAGE = "ATLASIMAGE_ATLAS_WORLDMAP_DARK_768_00";
    public static final String ATLAS_DARK_NONPVR = "WorldMap_Dark_NonPVR_768";
    public static final String ATLAS_DARK_NONPVR_PAGE = "ATLASIMAGE_ATLAS_WORLDMAP_DARK_NONPVR_768_00";
    public static final String ATLAS_SPINE = "WorldMapSpine_768";
    public static final String ATLAS_SPINE_PAGE = "ATLASIMAGE_ATLAS_WORLDMAPSPINE_768_00";
    public static final String ATLAS_COMMON = "WorldMap_768";

    /** Neon path (shared). */
    public static String PATH_SEGMENT = "IMAGE_WORLDMAP_MAP_PATH_MAP_PATH_135X16";

    /** Shared level-node PAM (all seasons). */
    public static String ORB_NODE_PAM = "768/INITIAL/WORLDMAP/LEVEL_NODE/LEVEL_NODE.PAM";
    public static String ORB_GREEN_PAM = ORB_NODE_PAM;
    public static String ORB_GREEN_CLIP = "unlocked";
    public static String ORB_UNLOCK_CLIP = "unlocked_animation";
    public static String ORB_LOCKED_CLIP = "locked_idle";
    public static String ORB_BLUE_BASE = "IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_118X49_2";
    public static String ORB_BLUE_DOME = "IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71_2";
    public static String ORB_BLUE_TOP = "IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_118X40_2";
    public static String ORB_WHITE_DOME = "IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71_3";
    public static String ORB_WHITE_HIGHLIGHT = "IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_79X58_3";

    // Egypt decoration constants (kept for ChapterLevelsScreen dump names).
    public static String DECOR_17 = "IMAGE_WORLDMAP_EGYPT_ISLAND17";
    public static String DECOR_20 = "IMAGE_WORLDMAP_EGYPT_ISLAND20";
    public static String DECOR_21 = "IMAGE_WORLDMAP_EGYPT_ISLAND21";
    public static String DECOR_22 = "IMAGE_WORLDMAP_EGYPT_ISLAND22";
    public static String DECOR_23 = "IMAGE_WORLDMAP_EGYPT_ISLAND23";
    public static String DECOR_24 = "IMAGE_WORLDMAP_EGYPT_ISLAND24";
    public static String DECOR_25 = "IMAGE_WORLDMAP_EGYPT_ISLAND25";
    public static String DECOR_26 = "IMAGE_WORLDMAP_EGYPT_ISLAND26";
    public static String DECOR_27 = "IMAGE_WORLDMAP_EGYPT_ISLAND27";
    public static String DECOR_34 = "IMAGE_WORLDMAP_EGYPT_ISLAND34";
    public static String DECOR_35 = "IMAGE_WORLDMAP_EGYPT_ISLAND35";
    public static String DECOR_37 = "IMAGE_WORLDMAP_EGYPT_ISLAND37";

    // Frostbite Caves decoration (WorldMap_Iceage atlas).
    public static String DECOR_ICE_22 = "IMAGE_WORLDMAP_ICEAGE_ISLAND22";
    public static String DECOR_ICE_23 = "IMAGE_WORLDMAP_ICEAGE_ISLAND23";
    public static String DECOR_ICE_24 = "IMAGE_WORLDMAP_ICEAGE_ISLAND24";
    public static String DECOR_ICE_41 = "IMAGE_WORLDMAP_ICEAGE_ISLAND41";
    public static String DECOR_ICE_42 = "IMAGE_WORLDMAP_ICEAGE_ISLAND42";
    public static String DECOR_ICE_43 = "IMAGE_WORLDMAP_ICEAGE_ISLAND43";
    public static String DECOR_ICE_44 = "IMAGE_WORLDMAP_ICEAGE_ISLAND44";
    public static String DECOR_ICE_47 = "IMAGE_WORLDMAP_ICEAGE_ISLAND47";
    public static String DECOR_ICE_ANIM3_270 = "IMAGE_WORLDMAP_ICEAGE_ANIM3_ANIM3_270X210";

    // Big Wave Beach decoration (WorldMap_Beach atlas).
    public static String DECOR_BEACH_15 = "IMAGE_WORLDMAP_BEACH_ISLAND15";
    public static String DECOR_BEACH_22 = "IMAGE_WORLDMAP_BEACH_ISLAND22";
    public static String DECOR_BEACH_23 = "IMAGE_WORLDMAP_BEACH_ISLAND23";
    public static String DECOR_BEACH_24 = "IMAGE_WORLDMAP_BEACH_ISLAND24";
    public static String DECOR_BEACH_27 = "IMAGE_WORLDMAP_BEACH_ISLAND27";
    public static String DECOR_BEACH_28 = "IMAGE_WORLDMAP_BEACH_ISLAND28";
    public static String DECOR_BEACH_37 = "IMAGE_WORLDMAP_BEACH_ISLAND37";
    public static String DECOR_BEACH_41 = "IMAGE_WORLDMAP_BEACH_ISLAND41";
    public static String DECOR_BEACH_42 = "IMAGE_WORLDMAP_BEACH_ISLAND42";

    // Dark Ages decoration (WorldMap_Dark atlas).
    public static String DECOR_DARK_8 = "IMAGE_WORLDMAP_DARK_ISLAND8";
    public static String DECOR_DARK_9 = "IMAGE_WORLDMAP_DARK_ISLAND9";
    public static String DECOR_DARK_18 = "IMAGE_WORLDMAP_DARK_ISLAND18";
    public static String DECOR_DARK_20 = "IMAGE_WORLDMAP_DARK_ISLAND20";
    public static String DECOR_DARK_35 = "IMAGE_WORLDMAP_DARK_ISLAND35";
    public static String DECOR_DARK_36 = "IMAGE_WORLDMAP_DARK_ISLAND36";
    public static String DECOR_DARK_49 = "IMAGE_WORLDMAP_DARK_ISLAND49";
    public static String DECOR_DARK_56 = "IMAGE_WORLDMAP_DARK_ISLAND56";
    public static String DECOR_DARK_DANGER =
            "IMAGE_WORLDMAP_DANGER_NODE_DARK_DANGER_NODE_DARK_389X448";

    /** One platform slot: optional looping PAM + static fallback region. */
    public record PlatformArt(
            String pamPath,
            String pamClip,
            String staticId,
            float nativeW,
            float nativeH) {
        public boolean hasPam() {
            return pamPath != null && !pamPath.isBlank();
        }
    }

    private Chapter chapter = Chapter.ANCIENT_EGYPT;
    private PlatformArt[] platforms = egyptPlatforms();
    private String[] decorIds = egyptDecorIds();
    private boolean loaded;

    public void configure(Chapter chapter) {
        this.chapter = chapter != null ? chapter : Chapter.ANCIENT_EGYPT;
        this.platforms = switch (this.chapter) {
            case FROSTBITE_CAVES -> frostbitePlatforms();
            case BIG_WAVE_BEACH -> beachPlatforms();
            case DARK_AGES -> darkAgesPlatforms();
            default -> egyptPlatforms();
        };
        this.decorIds = switch (this.chapter) {
            case FROSTBITE_CAVES -> frostbiteDecorIds();
            case BIG_WAVE_BEACH -> beachDecorIds();
            case DARK_AGES -> darkAgesDecorIds();
            default -> egyptDecorIds();
        };
        loaded = false;
    }

    public Chapter chapter() {
        return chapter;
    }

    public String[] decorIds() {
        return decorIds;
    }

    public PlatformArt platformArt(int levelId) {
        int idx = Math.max(0, levelId - 1);
        if (idx >= platforms.length) {
            return platforms[platforms.length - 1];
        }
        return platforms[idx];
    }

    /** Static region fallback / Egypt platforms. */
    public String platformFor(int levelId) {
        return platformArt(levelId).staticId();
    }

    @Deprecated
    public String platformFor(int levelId, int levelCount) {
        return platformFor(levelId);
    }

    public void ensureLoaded(TextureBank textures) {
        if (loaded || textures == null) {
            return;
        }
        textures.loadSync(ATLAS_SPINE);
        textures.loadSync(ATLAS_SPINE_PAGE);
        textures.loadSync(ATLAS_COMMON);
        textures.loadSync(AdventureHudRegions.ATLAS_WORLD_MAP);
        textures.loadSync(AdventureHudRegions.ATLAS_ALWAYS_LOADED);

        if (chapter == Chapter.FROSTBITE_CAVES) {
            textures.loadSync(ATLAS_ICEAGE);
            textures.loadSync(ATLAS_ICEAGE_PAGE);
            textures.loadSync(ATLAS_ICEAGE_NONPVR);
            textures.loadSync(ATLAS_ICEAGE_NONPVR_PAGE);
        } else if (chapter == Chapter.BIG_WAVE_BEACH) {
            textures.loadSync(ATLAS_BEACH);
            textures.loadSync(ATLAS_BEACH_PAGE);
            textures.loadSync(ATLAS_BEACH_NONPVR);
            textures.loadSync(ATLAS_BEACH_NONPVR_PAGE);
        } else if (chapter == Chapter.DARK_AGES) {
            textures.loadSync(ATLAS_DARK);
            textures.loadSync(ATLAS_DARK_PAGE);
            textures.loadSync(ATLAS_DARK_NONPVR);
            textures.loadSync(ATLAS_DARK_NONPVR_PAGE);
        } else {
            textures.loadSync(ATLAS_EGYPT);
            textures.loadSync(ATLAS_EGYPT_PAGE_0);
            textures.loadSync(ATLAS_EGYPT_PAGE_1);
        }

        for (PlatformArt art : platforms) {
            warm(textures, art.staticId());
        }
        for (String decorId : decorIds) {
            warm(textures, decorId);
        }
        warm(textures, ORB_BLUE_BASE);
        warm(textures, ORB_BLUE_DOME);
        warm(textures, ORB_BLUE_TOP);
        warm(textures, ORB_WHITE_DOME);
        warm(textures, ORB_WHITE_HIGHLIGHT);
        warm(textures, PATH_SEGMENT);
        loaded = true;
    }

    /** Preload platform PAM clips for the active chapter. */
    public void preloadPlatformPams(view.gui.anim.PamClipCache clips) {
        if (clips == null) {
            return;
        }
        for (PlatformArt art : platforms) {
            if (art.hasPam()) {
                clips.preloadSync(art.pamPath(), art.pamClip());
            }
        }
    }

    public boolean supportsChapter(Chapter chapter) {
        return chapter == Chapter.ANCIENT_EGYPT
                || chapter == Chapter.FROSTBITE_CAVES
                || chapter == Chapter.BIG_WAVE_BEACH
                || chapter == Chapter.DARK_AGES;
    }

    private static void warm(TextureBank textures, String id) {
        if (id == null) {
            return;
        }
        if (textures.region(id) == null) {
            Gdx.app.error("WorldMapArt", "Missing region: " + id);
        }
    }

    private static String[] egyptDecorIds() {
        return new String[] {
                DECOR_17, DECOR_20, DECOR_21, DECOR_22, DECOR_23, DECOR_24,
                DECOR_25, DECOR_26, DECOR_27, DECOR_34, DECOR_35, DECOR_37,
        };
    }

    private static String[] frostbiteDecorIds() {
        return new String[] {
                DECOR_ICE_22, DECOR_ICE_23, DECOR_ICE_24,
                DECOR_ICE_41, DECOR_ICE_42, DECOR_ICE_43, DECOR_ICE_44,
                DECOR_ICE_47, DECOR_ICE_ANIM3_270,
        };
    }

    /** Beach decoration IDs for warm + debugger dump names. */
    private static String[] beachDecorIds() {
        return new String[] {
                DECOR_BEACH_15, DECOR_BEACH_22, DECOR_BEACH_23, DECOR_BEACH_24, DECOR_BEACH_27,
                DECOR_BEACH_28, DECOR_BEACH_37, DECOR_BEACH_41, DECOR_BEACH_42,
        };
    }

    private static PlatformArt[] egyptPlatforms() {
        return new PlatformArt[] {
                new PlatformArt(null, null, "IMAGE_WORLDMAP_EGYPT_ISLAND1", 360f, 280f),
                new PlatformArt(null, null, "IMAGE_WORLDMAP_EGYPT_ISLAND5", 240f, 180f),
                new PlatformArt(null, null, "IMAGE_WORLDMAP_EGYPT_ISLAND9", 240f, 180f),
                new PlatformArt(null, null, "IMAGE_WORLDMAP_EGYPT_ISLAND4", 240f, 180f),
                new PlatformArt(null, null,
                        "IMAGE_WORLDMAP_ZOMBOSS_NODE_EGYPT_ZOMBOSS_NODE_EGYPT_914X994", 420f, 460f),
        };
    }

    private static PlatformArt[] frostbitePlatforms() {
        return new PlatformArt[] {
                new PlatformArt(
                        "768/FULL/WORLDMAP/ICEAGE/ANIM3/ANIM3.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_ICEAGE_ANIM3_ANIM3_1307X1318",
                        1307f, 1318f),
                new PlatformArt(
                        "768/FULL/WORLDMAP/ICEAGE/ANIM12/ANIM12.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_ICEAGE_ANIM12_ANIM12_400X500",
                        400f, 500f),
                new PlatformArt(
                        "768/FULL/WORLDMAP/ICEAGE/ANIM26/ANIM26.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_ICEAGE_ANIM26_ANIM26_375X281",
                        375f, 281f),
                new PlatformArt(
                        "768/FULL/WORLDMAP/ICEAGE/ANIM10/ANIM10.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_ICEAGE_ANIM10_ANIM10_400X500",
                        400f, 500f),
                new PlatformArt(
                        "768/FULL/WORLDMAP/ZOMBOSS_NODE_ICEAGE/ZOMBOSS_NODE_ICEAGE.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_ZOMBOSS_NODE_ICEAGE_ZOMBOSS_NODE_ICEAGE_1055X1280",
                        1055f, 1280f),
        };
    }

    private static PlatformArt[] beachPlatforms() {
        return new PlatformArt[] {
                new PlatformArt(
                        "768/FULL/WORLDMAP/BEACH/ANIM27/ANIM27.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_BEACH_ANIM27_ANIM27_1362X953",
                        1362f, 953f),
                new PlatformArt(
                        "768/FULL/WORLDMAP/BEACH/ANIM10/ANIM10.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_BEACH_ANIM10_ANIM10_295X271",
                        295f, 271f),
                new PlatformArt(
                        "768/FULL/WORLDMAP/BEACH/ANIM12/ANIM12.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_BEACH_ANIM12_ANIM12_335X420",
                        335f, 420f),
                new PlatformArt(
                        "768/FULL/WORLDMAP/BEACH/ANIM17/ANIM17.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_BEACH_ANIM17_ANIM17_321X255",
                        321f, 255f),
                new PlatformArt(
                        null, null,
                        "IMAGE_WORLDMAP_ZOMBOSS_NODE_BEACH_ZOMBOSS_NODE_BEACH_905X1096",
                        905f, 1096f),
        };
    }

    /** Dark Ages decoration IDs for warm + debugger dump names. */
    private static String[] darkAgesDecorIds() {
        return new String[] {
                DECOR_DARK_8, DECOR_DARK_9, DECOR_DARK_18, DECOR_DARK_20,
                DECOR_DARK_35, DECOR_DARK_36, DECOR_DARK_49, DECOR_DARK_56,
                DECOR_DARK_DANGER,
        };
    }

    private static PlatformArt[] darkAgesPlatforms() {
        return new PlatformArt[] {
                new PlatformArt(
                        "768/FULL/WORLDMAP/DARK/ANIM1/ANIM1.PAM",
                        "idle",
                        "IMAGE_WORLDMAP_DARK_ANIM1_ANIM1_1201X1413",
                        1201f, 1413f),
                new PlatformArt(
                        null, null,
                        "IMAGE_WORLDMAP_DARK_ISLAND7",
                        189f, 158f),
                new PlatformArt(
                        null, null,
                        "IMAGE_WORLDMAP_DARK_ISLAND7",
                        189f, 158f),
                new PlatformArt(
                        null, null,
                        "IMAGE_WORLDMAP_DARK_ISLAND6",
                        181f, 135f),
                new PlatformArt(
                        null, null,
                        "IMAGE_WORLDMAP_ZOMBOSS_NODE_DARK_ZOMBOSS_NODE_DARK_905X1096",
                        905f, 1096f),
        };
    }
}
