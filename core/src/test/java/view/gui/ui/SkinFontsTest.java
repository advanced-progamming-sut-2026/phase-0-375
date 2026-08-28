package view.gui.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinFontsTest {

    @Test
    void testResolveFontSpec() {
        SkinFonts.FontSpec big = SkinFonts.resolveFontSpec("big");
        assertNotNull(big);
        assertEquals(40, big.baseSize);
        assertTrue(big.ttfPath.endsWith("FBUSV8C5EI.TTF"));

        SkinFonts.FontSpec medium = SkinFonts.resolveFontSpec("medium");
        assertNotNull(medium);
        assertEquals(24, medium.baseSize);

        SkinFonts.FontSpec button = SkinFonts.resolveFontSpec("purple");
        assertNotNull(button);
        assertEquals(22, button.baseSize);
        assertTrue(button.ttfPath.endsWith("HOUSE OF TERROR.TTF"));

        SkinFonts.FontSpec npc = SkinFonts.resolveFontSpec("BRIANNETOD");
        assertNotNull(npc);
        assertEquals(16, npc.baseSize);
        assertTrue(npc.ttfPath.endsWith("BRIANNETOD.TTF"));
    }

    @Test
    void testDisposeDynamicFontsDoesNotThrow() {
        SkinFonts.disposeDynamicFonts();
    }
}
