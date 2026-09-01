package view.gui.ui;

import model.network.enums.ReactionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactionPresetsTest {

    @Test
    void byContentIdResolvesKnownPresets() {
        assertEquals(ReactionPresets.MSG_866, ReactionPresets.byContentId("MSG_866"));
        assertEquals(ReactionPresets.EMOJI_MGP, ReactionPresets.byContentId("EMOJI_MGP"));
        assertEquals(ReactionPresets.STICKER_CHICKEN, ReactionPresets.byContentId("STICKER_CHICKEN"));
    }

    @Test
    void catalogHasNineEntriesAcrossTypes() {
        assertEquals(9, ReactionPresets.ALL.size());
        assertTrue(ReactionPresets.ALL.stream().anyMatch(p -> p.type() == ReactionType.TEXT));
        assertTrue(ReactionPresets.ALL.stream().anyMatch(p -> p.type() == ReactionType.EMOJI));
        assertTrue(ReactionPresets.ALL.stream().anyMatch(p -> p.type() == ReactionType.TAUNT));
        assertNotNull(ReactionPresets.byContentId("EMOJI_TRIGGER"));
        assertEquals("idle2", ReactionPresets.CHICKEN_CLIPS[0]);
        assertEquals("idle", ReactionPresets.CHICKEN_CLIPS[1]);
        assertEquals("sunflower_exit", ReactionPresets.SUNFLOWER_CLIPS[2]);
        assertEquals(4, ReactionPresets.DIFFICULTY_METER_CLIPS.length);
        assertEquals("animation", ReactionPresets.DIFFICULTY_METER_CLIPS[0]);
        assertEquals("animation", ReactionPresets.DIFFICULTY_METER_CLIPS[1]);
        assertEquals("animation5", ReactionPresets.DIFFICULTY_METER_CLIPS[3]);
        assertEquals("8-6-6", ReactionPresets.MSG_866.label());
        assertEquals("image_ui_hud_eventbutton_event_icon_feastivus_up", ReactionPresets.EMOJI_FEAST.imageId());
    }

    @Test
    void mirroredDrawableSwapsSideInsets() {
        com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable src =
                new com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable();
        src.setLeftWidth(8f);
        src.setRightWidth(30f);
        src.setMinWidth(200f);

        com.badlogic.gdx.scenes.scene2d.utils.Drawable mirrored = UiDrawables.mirrored(src);
        assertEquals(30f, mirrored.getLeftWidth(), 0.001f);
        assertEquals(8f, mirrored.getRightWidth(), 0.001f);
        assertEquals(200f, mirrored.getMinWidth(), 0.001f);
    }

    @Test
    void tailAnchorUsesLayoutWidthForRightCorner() {
        float stageW = 1302f;
        float layoutW = 420f;
        float x = ReactionBubbleLayout.tailAnchorX(
                ReactionBubbleLayout.Corner.BOTTOM_RIGHT, stageW, layoutW);
        assertEquals(stageW - ReactionBubbleLayout.PAD_X_RIGHT - layoutW, x, 0.001f);
    }
}
