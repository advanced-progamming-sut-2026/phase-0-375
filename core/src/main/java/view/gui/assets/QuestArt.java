package view.gui.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import model.enums.QuestCategory;
import model.quest.Quest;
import view.gui.PvzGdxGame;

import java.util.HashMap;
import java.util.Map;

/**
 * Quest travel-log art: tab faces from Exports PNGs, plus atlas region IDs from
 * {@code UI_Quests_768}.
 */
public final class QuestArt {
    public static final String ATLAS = "UI_Quests_768";
    public static final String ATLAS_IMAGE = "ATLASIMAGE_ATLAS_UI_QUESTS_768_00";

    public static final String ICON_ZOMBIE = "IMAGE_UI_QUESTS_QUESTICONS_ZOMBIE";
    public static final String ICON_GARGANTUAR = "IMAGE_UI_QUESTS_QUESTICONS_GARGANTUAR";
    public static final String ICON_EPIC = "IMAGE_UI_QUESTS_ICON_EPIC";
    public static final String ICON_EXPANSION_LEVEL = "IMAGE_UI_QUESTS_QUESTICONS_EXPANSIONLEVEL";
    public static final String ICON_PREMIUM_SEEDS2 = "IMAGE_UI_QUESTS_QUESTICONS_PREMIUMSEEDS2";
    public static final String ICON_KNOCKBACK = "IMAGE_UI_QUESTS_QUESTICONS_KNOCKBACK";
    public static final String ICON_BIG_WAVE_BEACH = "IMAGE_UI_QUESTS_QUESTICONS_BIGWAVEBEACH";
    public static final String ICON_PREMIUM_PLANTS = "IMAGE_UI_QUESTS_QUESTICONS_PREMIUMPLANTS";
    public static final String ICON_PLANT = "IMAGE_UI_QUESTS_QUESTICONS_PLANT";
    public static final String ICON_EGYPT = "IMAGE_UI_QUESTS_QUESTICONS_EGYPT";
    public static final String ICON_DARK_AGES = "IMAGE_UI_QUESTS_QUESTICONS_DARKAGES";
    public static final String ICON_FROSTBITE_CAVES = "IMAGE_UI_QUESTS_QUESTICONS_FROSTBITECAVES";
    public static final String ICON_ARENA = "IMAGE_UI_QUESTS_QUESTICONS_ARENA";
    public static final String ICON_MINTS = "IMAGE_UI_QUESTS_QUESTICONS_MINTS";
    public static final String ICON_ELECTROCUTE = "IMAGE_UI_QUESTS_QUESTICONS_ELECTROCUTE";
    public static final String ICON_DAILY_SUN = "IMAGE_UI_HUD_INGAME_SUN";
    public static final String ICON_WIN_STREAK = "IMAGE_UI_QUESTS_REPLACE_QUEST_BUTTON";
    /** Lawn row strip — Defenseless Row. */
    public static final String ICON_LAWN_ROW = "IMAGE_BACKGROUNDS_FRONTLAWN_ROW_01";
    public static final String ATLAS_FRONT_LAWN = "DelayLoad_Background_FrontLawn";
    /** Synthetic: 90° bake of {@link #ICON_LAWN_ROW} for One Column Less. */
    public static final String ICON_LAWN_COLUMN = "QUEST_SYNTH_LAWN_COLUMN";
    /** Synthetic: row + column stacked for Defenseless Cross. */
    public static final String ICON_LAWN_CROSS = "QUEST_SYNTH_LAWN_CROSS";

    /** Looping badge on daily quest cards (top corner). */
    public static final String PAM_DAILY_CLOCK =
        "768/INITIAL/UI/QUESTS/DAILY_QUEST_CLOCK_ICON/DAILY_QUEST_CLOCK_ICON.PAM";
    public static final String PAM_CLIP = "default";

    public static final String ICON_MINIGAMES_HUD = "IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_NORMAL";
    public static final String ICON_VASE_BREAKER =
        "IMAGE_UI_VASEBREAKER_ENDLESS_NODE_VASEBREAKER_ENDLESS_NODE_115X150_2";
    public static final String ICON_BEGHOULED = "IMAGE_UI_POWERUPS_POWER_BEGHOULEDSHUFFLE";
    public static final String ICON_BOWLING = "IMAGE_UI_PACKETS_TOOLS_PROJECTILE_BOWLINGBULB_MEGA";

    public static final String COIN_ICON = "IMAGE_UI_QUESTS_COIN_ICON";
    public static final String GEM_ICON = "IMAGE_UI_QUESTS_GEM_ICON";
    /** Unlock / “new plant” reward art. */
    public static final String REWARD_NEW_PLANT = "IMAGE_UI_PACKETS_SUNFLOWER";
    /** Seed-packet inventory reward art. */
    public static final String REWARD_SEED_PACKET = "IMAGE_UI_STOREMULTI_SEEDPACKETICON";

    public static final String PANEL_DEFAULT = "IMAGE_UI_QUESTS_TRAVEL_LOG_PANEL_DEFAULT";
    public static final String PANEL_COMPLETE = "IMAGE_UI_QUESTS_TRAVEL_LOG_PANEL_COMPLETE";
    public static final String PANEL_EPIC = "IMAGE_UI_QUESTS_TRAVEL_LOG_PANEL_EPIC";
    public static final String PANEL_EPIC_COMPLETE = "IMAGE_UI_QUESTS_TRAVEL_LOG_PANEL_EPIC_COMPLETE";
    public static final String PANEL_EXPIRED = "IMAGE_UI_QUESTS_TRAVEL_LOG_PANEL_EXPIRED";
    public static final String PANEL_CLAIM_ALL = "IMAGE_UI_QUESTS_TRAVEL_LOG_PANEL_CLAIM_ALL";

    public static final String EXPORT_DIR = "Exports/ATLASIMAGE_ATLAS_UI_QUESTS_768_00";

    public static final String DAILY_ACTIVE = "daily_active.png";
    public static final String DAILY_INACTIVE = "daily_inactive.png";
    public static final String MAIN_ACTIVE = "main_active_cyan.png";
    public static final String MAIN_INACTIVE = "main_inactive_cyan.png";
    public static final String EPIC_ACTIVE = "epic_active.png";
    public static final String EPIC_INACTIVE = "epic_inactive.png";
    public static final String MINI_ACTIVE = "minigame_active_orange.png";
    public static final String MINI_INACTIVE = "minigame_inactive_orange.png";

    private final Map<String, Texture> textures = new HashMap<>();

    /** Atlas region id for a quest row icon. */
    public static String iconFor(Quest quest) {
        if (quest == null) {
            return ICON_ZOMBIE;
        }
        if (quest.getCategory() == QuestCategory.EPIC) {
            return ICON_EPIC;
        }
        String base = baseName(quest.getName());
        if ("Chapter Hunter".equalsIgnoreCase(base)) {
            return chapterIcon(firstNonBlank(quest.getVariable(), parenValue(quest.getName())));
        }
        return switch (base) {
            case "Professional Destroyer" -> ICON_EXPANSION_LEVEL;
            case "Plant Specialist" -> ICON_PREMIUM_SEEDS2;
            case "Only Cactus" -> ICON_KNOCKBACK;
            case "Symmetry" -> ICON_BIG_WAVE_BEACH;
            case "Bloom in Constraints" -> ICON_PREMIUM_PLANTS;
            case "Cloudy Day" -> ICON_PLANT;
            case "Frugal Planter" -> ICON_ARENA;
            case "Quick Strike" -> ICON_MINTS;
            case "Almost Victorious" -> ICON_ELECTROCUTE;
            case "Daily Sun Collector" -> ICON_DAILY_SUN;
            case "Win Streak" -> ICON_WIN_STREAK;
            case "Defenseless Row" -> ICON_LAWN_ROW;
            case "One Column Less" -> ICON_LAWN_COLUMN;
            case "Defenseless Cross" -> ICON_LAWN_CROSS;
            default -> ICON_ZOMBIE;
        };
    }

    private static String chapterIcon(String chapter) {
        if (chapter == null || chapter.isBlank()) {
            return ICON_ZOMBIE;
        }
        String key = chapter.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (key) {
            case "ANCIENT_EGYPT", "EGYPT" -> ICON_EGYPT;
            case "BIG_WAVE_BEACH", "BIGWAVEBEACH" -> ICON_BIG_WAVE_BEACH;
            case "DARK_AGES", "DARKAGES" -> ICON_DARK_AGES;
            case "FROSTBITE_CAVES", "FROSTBITECAVES" -> ICON_FROSTBITE_CAVES;
            default -> ICON_ZOMBIE;
        };
    }

    /** Strips {@code " (variant)"} suffix used by {@code QuestLoader}. */
    private static String baseName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        int cut = name.lastIndexOf(" (");
        if (cut > 0 && name.endsWith(")")) {
            return name.substring(0, cut).trim();
        }
        return name.trim();
    }

    private static String parenValue(String name) {
        if (name == null) {
            return null;
        }
        int cut = name.lastIndexOf(" (");
        if (cut > 0 && name.endsWith(")")) {
            return name.substring(cut + 2, name.length() - 1).trim();
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    public void ensureLoaded(PvzGdxGame game) {
        load(game, DAILY_ACTIVE);
        load(game, DAILY_INACTIVE);
        load(game, MAIN_ACTIVE);
        load(game, MAIN_INACTIVE);
        load(game, EPIC_ACTIVE);
        load(game, EPIC_INACTIVE);
        load(game, MINI_ACTIVE);
        load(game, MINI_INACTIVE);
    }

    public TextureRegion region(String fileName) {
        Texture tex = textures.get(fileName);
        return tex == null ? null : new TextureRegion(tex);
    }

    public void dispose() {
        for (Texture tex : textures.values()) {
            tex.dispose();
        }
        textures.clear();
    }

    private void load(PvzGdxGame game, String fileName) {
        if (textures.containsKey(fileName)) {
            return;
        }
        FileHandle file = resolve(game, fileName);
        if (file == null || !file.exists()) {
            System.err.println("[QuestArt] Missing tab art: " + fileName);
            return;
        }
        Texture tex = new Texture(file);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textures.put(fileName, tex);
    }

    private static FileHandle resolve(PvzGdxGame game, String fileName) {
        String relative = EXPORT_DIR + "/" + fileName;
        if (game.assets != null && game.assets.root != null) {
            FileHandle fromRoot = game.assets.root.child(relative);
            if (fromRoot.exists()) {
                return fromRoot;
            }
        }
        return PvzAssets.resolveAsset(relative);
    }
}
