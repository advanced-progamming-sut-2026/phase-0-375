package view.gui.lawn;

import model.enums.Chapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LawnBackgroundRendererTest {
    @Test
    void egyptMatchesFrontLawnPanelLayout() {
        LawnBackgroundRenderer.Style egypt = LawnBackgroundRenderer.Style.EGYPT;
        assertEquals("DelayLoad_Background_Egypt_Compressed", egypt.atlasGroup());
        assertEquals("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_LEFT", egypt.leftId());
        assertEquals("IMAGE_BACKGROUNDS_EGYPT_TEXTURE", egypt.centerId());
        assertEquals("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_RIGHT", egypt.rightId());
        assertNull(egypt.row05Id());
        assertSame(egypt, LawnBackgroundRenderer.Style.forChapter(Chapter.ANCIENT_EGYPT));
        assertEquals(LawnLayout.TEXTURE_LEFT_WIDTH + LawnLayout.TEXTURE_WIDTH, LawnLayout.WORLD_WIDTH);
    }

    @Test
    void iceAgeMatchesFrontLawnPanelLayout() {
        LawnBackgroundRenderer.Style ice = LawnBackgroundRenderer.Style.ICE_AGE;
        assertEquals("DelayLoad_Background_Iceage_Compressed", ice.atlasGroup());
        assertEquals("IMAGE_BACKGROUNDS_ICEAGE_TEXTURE_LEFT", ice.leftId());
        assertEquals("IMAGE_BACKGROUNDS_ICEAGE_TEXTURE", ice.centerId());
        assertEquals("IMAGE_BACKGROUNDS_ICEAGE_TEXTURE_RIGHT", ice.rightId());
        assertNull(ice.row05Id());
        assertSame(ice, LawnBackgroundRenderer.Style.forChapter(Chapter.FROSTBITE_CAVES));
        assertEquals(-17f, LawnLayout.WORLD_HEIGHT - 785f);
    }

    @Test
    void beachMatchesFrontLawnPanelLayout() {
        LawnBackgroundRenderer.Style beach = LawnBackgroundRenderer.Style.BEACH;
        assertEquals("DelayLoad_Background_Beach_Compressed", beach.atlasGroup());
        assertEquals("IMAGE_BACKGROUNDS_BEACH_TEXTURE_LEFT", beach.leftId());
        assertEquals("IMAGE_BACKGROUNDS_BEACH_TEXTURE", beach.centerId());
        assertEquals("IMAGE_BACKGROUNDS_BEACH_TEXTURE_RIGHT", beach.rightId());
        assertNull(beach.row05Id());
        assertSame(beach, LawnBackgroundRenderer.Style.forChapter(Chapter.BIG_WAVE_BEACH));
    }

    @Test
    void darkAgesMatchesFrontLawnPanelLayout() {
        LawnBackgroundRenderer.Style dark = LawnBackgroundRenderer.Style.DARK;
        assertEquals("DelayLoad_Background_Dark_Compressed", dark.atlasGroup());
        assertEquals("IMAGE_BACKGROUNDS_DARK_TEXTURE_LEFT", dark.leftId());
        assertEquals("IMAGE_BACKGROUNDS_DARK_TEXTURE", dark.centerId());
        assertEquals("IMAGE_BACKGROUNDS_DARK_TEXTURE_RIGHT", dark.rightId());
        assertNull(dark.row05Id());
        assertSame(dark, LawnBackgroundRenderer.Style.forChapter(Chapter.DARK_AGES));
    }
}
