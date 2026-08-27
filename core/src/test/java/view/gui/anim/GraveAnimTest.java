package view.gui.anim;

import model.enums.Chapter;
import model.item.Grave;
import model.item.Grave.GraveType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraveAnimTest {

    @Test
    void hpMapsToDamageClips() {
        assertEquals("undamaged", GraveAnim.clipForHp(Grave.DEFAULT_HP, Grave.DEFAULT_HP));
        assertEquals("damage1", GraveAnim.clipForHp(490, Grave.DEFAULT_HP));
        assertEquals("damage2", GraveAnim.clipForHp(350, Grave.DEFAULT_HP));
        assertEquals("damage3", GraveAnim.clipForHp(210, Grave.DEFAULT_HP));
        assertEquals("damage4", GraveAnim.clipForHp(1, Grave.DEFAULT_HP));
    }

    @Test
    void darkAgesPicksLootPam() {
        assertEquals(GraveAnim.PAM_DARK_PLAIN,
            GraveAnim.pamFor(Chapter.DARK_AGES, new Grave(Grave.DEFAULT_HP, GraveType.PLAIN)));
        assertEquals(GraveAnim.PAM_DARK_SUN,
            GraveAnim.pamFor(Chapter.DARK_AGES, new Grave(Grave.DEFAULT_HP, GraveType.SUN)));
        assertEquals(GraveAnim.PAM_DARK_PLANTFOOD,
            GraveAnim.pamFor(Chapter.DARK_AGES, new Grave(Grave.DEFAULT_HP, GraveType.PLANT_FOOD)));
        assertEquals(GraveAnim.PAM_EGYPT,
            GraveAnim.pamFor(Chapter.ANCIENT_EGYPT, new Grave(Grave.DEFAULT_HP, GraveType.SUN)));
    }

    @Test
    void emergeGoesWideShortThenTallThinThenRest() {
        assertTrue(GraveAnim.scaleX(0f) > 1f);
        assertTrue(GraveAnim.scaleY(0f) < 1f);
        assertTrue(GraveAnim.scaleX(0.4f) < 1f);
        assertTrue(GraveAnim.scaleY(0.4f) > 1f);
        assertEquals(1f, GraveAnim.scaleX(1f), 1e-4f);
        assertEquals(1f, GraveAnim.scaleY(1f), 1e-4f);
    }
}
