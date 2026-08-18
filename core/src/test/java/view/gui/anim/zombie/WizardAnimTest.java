package view.gui.anim.zombie;

import model.enums.Chapter;
import model.enums.PlantCategory;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.map.Point;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.behavior.TransformBehavior;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class WizardAnimTest {

    @Test
    void sheepPlaysWhileCastingEatIsWalkAndIdleFallsThrough() {
        PlantInstance plant = pea();
        ZombieInstance zombie = wizard();
        TransformBehavior transform = (TransformBehavior) zombie.getBehavior(ZombieBehaviorType.TRANSFORM);
        PamCatalog.PamEntry entry = new PamCatalog.PamEntry(
                "ZOMBIE_DARK_WIZARD",
                "768/FULL/ZOMBIE/ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM",
                Map.of("walk", 3f, "eat", 8.6f, "die", 2.2f, "sheep", 2.3f));
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        WizardAnim.register(overrides);
        BehaviorContext lawn = lawn(plant);

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.WALK));

        AnimPose eating = overrides.tryResolve(zombie, entry, ZombieAnimRole.EATING);
        assertEquals("walk", eating.clipName());
        assertEquals(ZombieAnimRole.EATING, eating.role());

        transform.setCastTimer(TransformBehavior.TRANSFORM_INTERVAL);
        transform.execute(zombie, lawn, 0.1f);
        AnimPose sheep = overrides.tryResolve(zombie, entry, ZombieAnimRole.IDLE);
        assertEquals(WizardAnim.SHEEP_CLIP, sheep.clipName());
        assertFalse(sheep.loop());
        assertEquals(ZombieAnimRole.EATING, sheep.role());

        assertNull(overrides.tryResolve(zombie, entry, ZombieAnimRole.DIE));
    }

    private static ZombieInstance wizard() {
        Zombie definition = new Zombie(
                "ZombieWizard", 490, 0.12f, 100f, ZombieSize.NORMAL,
                Chapter.DARK_AGES, 800, 1, List.of(), null, null,
                List.of(ZombieBehaviorType.TRANSFORM));
        ZombieInstance zombie = new ZombieInstance(definition);
        zombie.setState(ZombieState.WALKING);
        zombie.setGridPosition(new Point(6, 0));
        return zombie;
    }

    private static PlantInstance pea() {
        PlantInstance p = new PlantInstance(new Plant(
                1, "Peashooter", PlantCategory.SHOOTER, List.of(), 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
        p.setPosition(new Point(3, 0));
        return p;
    }

    private static BehaviorContext lawn(PlantInstance plant) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAllPlants" -> List.of(plant);
                    case "getPlantAt" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
