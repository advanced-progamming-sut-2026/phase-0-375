package view.gui.ui;

import model.network.enums.ReactionType;

import java.util.List;

/** Preset multiplayer reactions mapped to {@link model.network.packet.chat.ReactionPacket} content ids. */
public final class ReactionPresets {
    public record Preset(
            ReactionType type,
            String contentId,
            String label,
            String pamPath,
            String pamClip,
            String imageId
    ) {}

    public static final Preset MSG_866 = new Preset(
            ReactionType.TEXT, "MSG_866", "8-6-6", null, null, null);
    public static final Preset MSG_TEREKOONDI = new Preset(
            ReactionType.TEXT, "MSG_TEREKOONDI", "Terekoondi Shir", null, null, null);
    public static final Preset MSG_ZARAB = new Preset(
            ReactionType.TEXT, "MSG_ZARAB", "Zarrab's gonna touch you", null, null, null);

    public static final Preset EMOJI_MGP = new Preset(
            ReactionType.EMOJI, "EMOJI_MGP", "",
            null, null, "image_ui_hud_eventbutton_event_icon_mgpwinterevent_up");
    public static final Preset EMOJI_FEAST = new Preset(
            ReactionType.EMOJI, "EMOJI_FEAST", "",
            null, null, "image_ui_hud_eventbutton_event_icon_feastivus_up");
    public static final Preset EMOJI_TRIGGER = new Preset(
            ReactionType.EMOJI, "EMOJI_TRIGGER", "",
            null, null, "image_ui_hud_eventbutton_event_icon_trigger_up");

    public static final Preset STICKER_CHICKEN = new Preset(
            ReactionType.TAUNT, "STICKER_CHICKEN", "",
            "768/FULL/ZOMBIE/CHICKEN/CHICKEN.PAM", null, null);
    public static final Preset STICKER_SUNFLOWER = new Preset(
            ReactionType.TAUNT, "STICKER_SUNFLOWER", "",
            "768/FULL/NPC/SUNFLOWER/SUNFLOWER.PAM", null, null);
    public static final Preset STICKER_DIFFICULTY = new Preset(
            ReactionType.TAUNT, "STICKER_DIFFICULTY", "",
            "768/DEV/UI/QUESTS/DIFFICULTY_METER/DIFFICULTY_METER.PAM", null, null);

    public static final String[] CHICKEN_CLIPS = {"idle2", "idle"};
    public static final String[] SUNFLOWER_CLIPS = {"sunflower_enter", "sunflower_shout", "sunflower_exit"};
    public static final String[] DIFFICULTY_METER_CLIPS = {"animation", "animation", "animation3", "animation5"};

    public static final List<Preset> ALL = List.of(
            MSG_866, EMOJI_MGP, STICKER_CHICKEN,
            MSG_TEREKOONDI, EMOJI_FEAST, STICKER_SUNFLOWER,
            MSG_ZARAB, EMOJI_TRIGGER, STICKER_DIFFICULTY);

    private ReactionPresets() {}

    public static Preset byContentId(String contentId) {
        if (contentId == null) {
            return null;
        }
        for (Preset preset : ALL) {
            if (contentId.equals(preset.contentId())) {
                return preset;
            }
        }
        return null;
    }

    public static boolean isText(Preset preset) {
        return preset != null && preset.type() == ReactionType.TEXT;
    }

    public static boolean isEmoji(Preset preset) {
        return preset != null && preset.type() == ReactionType.EMOJI;
    }

    public static boolean isSticker(Preset preset) {
        return preset != null && preset.type() == ReactionType.TAUNT;
    }

    public static boolean hasPam(Preset preset) {
        return preset != null && preset.pamPath() != null && preset.pamClip() != null;
    }

    public static boolean hasPamSequence(Preset preset) {
        return pamClipSequence(preset) != null;
    }

    public static String[] pamClipSequence(Preset preset) {
        if (preset == STICKER_DIFFICULTY) {
            return DIFFICULTY_METER_CLIPS;
        }
        if (preset == STICKER_CHICKEN) {
            return CHICKEN_CLIPS;
        }
        if (preset == STICKER_SUNFLOWER) {
            return SUNFLOWER_CLIPS;
        }
        return null;
    }

    /** Draw anchor tweak for sequence stickers in the picker (negative = lower). */
    public static float pamDrawOffsetY(Preset preset) {
        if (preset == STICKER_DIFFICULTY) {
            return -14f;
        }
        return 0f;
    }

    /** Extra downward draw nudge in the sent/received preview bubble. */
    public static float pamPreviewOffsetY(Preset preset) {
        return 0f;
    }

    /**
     * Vertical draw anchor inside the preview cell (0 = bottom, 0.5 = center).
     * Low values keep tall FX (flames, shout) inside the bubble.
     */
    public static float pamPreviewAnchorY(Preset preset) {
        if (preset == STICKER_DIFFICULTY) {
            return 0.26f;
        }
        if (preset == STICKER_SUNFLOWER) {
            return 0.30f;
        }
        return 0.5f;
    }

    private static final float PANEL_PAM_SCALE = 0.42f;
    private static final float PREVIEW_PAM_SCALE = 0.55f;
    private static final float SUNFLOWER_SCALE = 0.25f;

    /** PAM scale in the 3×3 selection panel (preview uses its own scale). */
    public static float pamPanelScale(Preset preset) {
        if (preset == STICKER_DIFFICULTY) {
            return PANEL_PAM_SCALE * 0.7f;
        }
        if (preset == STICKER_SUNFLOWER) {
            return PANEL_PAM_SCALE * SUNFLOWER_SCALE;
        }
        return PANEL_PAM_SCALE;
    }

    public static float pamPreviewScale(Preset preset) {
        if (preset == STICKER_SUNFLOWER) {
            return PREVIEW_PAM_SCALE * SUNFLOWER_SCALE;
        }
        return PREVIEW_PAM_SCALE;
    }

    /** Visual footprint of a reaction in the preview bubble (before bubble padding). */
    public static float previewContentWidth(Preset preset) {
        if (isText(preset)) {
            return Math.min(220f, 36f + preset.label().length() * 7.5f);
        }
        if (hasImage(preset)) {
            return 76f;
        }
        if (preset == STICKER_SUNFLOWER) {
            return 118f;
        }
        if (preset == STICKER_DIFFICULTY) {
            return 92f;
        }
        if (preset == STICKER_CHICKEN) {
            return 88f;
        }
        return 84f;
    }

    public static float previewContentHeight(Preset preset) {
        if (isText(preset)) {
            return preset.label().length() > 18 ? 52f : 36f;
        }
        if (hasImage(preset)) {
            return 76f;
        }
        if (preset == STICKER_SUNFLOWER) {
            return 132f;
        }
        if (preset == STICKER_DIFFICULTY) {
            return 78f * 2.1f;
        }
        if (preset == STICKER_CHICKEN) {
            return 72f;
        }
        return 72f;
    }

    public static boolean hasImage(Preset preset) {
        return preset != null && preset.imageId() != null;
    }
}
