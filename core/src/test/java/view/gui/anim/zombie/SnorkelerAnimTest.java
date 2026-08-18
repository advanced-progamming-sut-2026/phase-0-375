package view.gui.anim.zombie;

import com.badlogic.gdx.math.Rectangle;
import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;
import view.gui.assets.PamCatalog;
import view.gui.lawn.LawnLayout;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnorkelerAnimTest {

    private static final PamCatalog.PamEntry ENTRY = new PamCatalog.PamEntry(
            "ZOMBIE_BEACH_SNORKELER",
            "768/FULL/ZOMBIE/ZOMBIE_BEACH_SNORKELER/ZOMBIE_BEACH_SNORKELER.PAM",
            Map.of("walk", 1f, "eat", 1f, "die", 2f));

    @Test
    void walkEatDieStayOnDefaults() {
        ZombieInstance zombie = snorkel();
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        SnorkelerAnim.register(overrides);
        assertNull(overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.WALK));
        assertNull(overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.EATING));
        assertNull(overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.DIE));
        assertTrue(SnorkelerAnim.isSnorkelerPam(ENTRY.path()));
        assertEquals("ripple", SnorkelerAnim.RIPPLE_CLIP);
    }

    @Test
    void waterlineIsLowerPartOfTileAndHeadSinksToIt() {
        LawnLayout layout = LawnLayout.frontLawnDefault();
        int row = 2;
        float waterY = SnorkelerAnim.waterLineY(layout, row);
        assertEquals(layout.cellBottom(row) + SnorkelerAnim.WATERLINE_FROM_BOTTOM * layout.cellHeight(),
                waterY, 1e-3f);
        assertTrue(waterY < layout.centerY(row));
        assertTrue(waterY > layout.cellBottom(row));

        float standY = layout.centerY(row);
        Rectangle skull = new Rectangle(-20f, -80f, 40f, 40f);
        float sunk = SnorkelerAnim.drawOriginY(standY, waterY, skull, 1f, 0f);
        assertEquals(waterY + (-80f + 40f), sunk, 1e-3f);
        assertEquals(waterY, sunk - (skull.y + skull.height), 1e-3f);
        assertEquals(standY, SnorkelerAnim.drawOriginY(standY, waterY, skull, 1f, 1f), 1e-3f);
        Rectangle clip = new Rectangle(-60f, -40f, 120f, 90f);
        assertEquals(waterY + clip.y, SnorkelerAnim.rippleDrawY(waterY, clip, 1f), 1e-3f);
        assertTrue(SnorkelerAnim.rippleDrawY(waterY, clip, 1f) < waterY);
        float originX = layout.centerX(8);
        Rectangle mask = FishermanAnim.drownMaskWorld(layout, originX, row, waterY);
        Rectangle sprite = FishermanAnim.spriteWorld(originX, sunk,
                new Rectangle(-30f, -80f, 60f, 160f), 1f, false);
        assertTrue(FishermanAnim.overlaps(mask, sprite));
        assertFalse(SnorkelerAnim.isSnorkelerPam("ZOMBIE_BEACH_FISHERMAN.PAM"));
    }

    private static ZombieInstance snorkel() {
        Zombie definition = new Zombie(
                "ZombieBeachSnorkel", 200, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.BIG_WAVE_BEACH, 150, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.SWIM));
        return new ZombieInstance(definition);
    }
}
