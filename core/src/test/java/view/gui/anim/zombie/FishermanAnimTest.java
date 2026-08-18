package view.gui.anim.zombie;

import com.badlogic.gdx.math.Rectangle;
import model.enums.Chapter;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.zombie.behavior.FishBehavior;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;
import view.gui.lawn.LawnLayout;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishermanAnimTest {

    private static final PamCatalog.PamEntry ENTRY = new PamCatalog.PamEntry(
            "ZOMBIE_BEACH_FISHERMAN",
            "768/FULL/ZOMBIE/ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM",
            Map.of("intro", 1.6333f, "idle", 2.1f, "cast", 1.2667f, "reel", 1.4667f, "die", 3.4f));

    @Test
    void clipsFollowFishPhaseAndDieFallsThrough() {
        ZombieInstance zombie = fisherman();
        FishBehavior fish = (FishBehavior) zombie.getBehavior(ZombieBehaviorType.FISH);
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        FishermanAnim.register(overrides);

        AnimPose intro = overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.WALK);
        assertEquals("intro", intro.clipName());
        assertFalse(intro.loop());

        fish.setPhase(FishBehavior.FishPhase.IDLE);
        AnimPose idle = overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.WALK);
        assertEquals("idle", idle.clipName());
        assertTrue(idle.loop());

        fish.setPhase(FishBehavior.FishPhase.CASTING);
        assertEquals("cast", overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.IDLE).clipName());

        fish.setPhase(FishBehavior.FishPhase.REELING);
        assertEquals("reel", overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.IDLE).clipName());

        assertNull(overrides.tryResolve(zombie, ENTRY, ZombieAnimRole.DIE));
    }

    @Test
    void drownMaskCoversTileBelowInnertube() {
        LawnLayout layout = LawnLayout.frontLawnDefault();
        float originX = layout.centerX(8);
        float originY = layout.centerY(2);
        Rectangle innertube = new Rectangle(-10f, 80f, 40f, 40f);
        float waterY = FishermanAnim.waterY(originY, innertube, 1f);
        Rectangle mask = FishermanAnim.drownMaskWorld(layout, originX, originY, waterY);
        assertEquals(layout.cellLeft(8) - layout.cellWidth(), mask.x, 1e-3f);
        assertEquals(layout.cellBottom(2) - layout.cellHeight(), mask.y, 1e-3f);
        assertEquals(layout.cellWidth() * 3f, mask.width, 1e-3f);
        assertEquals(waterY - mask.y, mask.height, 1e-3f);
        Rectangle body = new Rectangle(-20f, 80f, 60f, 150f);
        Rectangle sprite = FishermanAnim.spriteWorld(originX, originY, body, 1f, false);
        assertTrue(FishermanAnim.overlaps(mask, sprite));
        Rectangle miss = new Rectangle(layout.cellLeft(0), layout.cellBottom(0), 10f, 10f);
        assertFalse(FishermanAnim.overlaps(mask, miss));
        assertTrue(FishermanAnim.isFishermanPam(ENTRY.path()));
    }

    private static ZombieInstance fisherman() {
        Zombie definition = new Zombie(
                "ZombieBeachFisherman", 1000, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.BIG_WAVE_BEACH, 150, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.FISH));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.SPAWNING);
        return zombie;
    }
}
