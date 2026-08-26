package model.zombie.behavior.zombotany;

import model.enums.PlantCategory;
import model.enums.ZombieState;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.zombie.behavior.BehaviorContext;
import model.zombie.behavior.ZombieBehavior;
import model.zombie.instance.ZombieInstance;

import java.io.IOException;
import java.util.List;

/**
 * Base class for Zombotany mini-game abilities: zombies that look like
 * regular zombies but wield one plant's power. Each concrete ability lives
 * in its own class; this base only provides the plumbing they share.
 */
public abstract class ZombotanyAbilityBehavior implements ZombieBehavior {

    /**
     * Damage used to destroy the acting zombie itself.
     */
    public static final int SELF_DESTRUCT_DAMAGE = 100_000;

    protected static void selfDestruct(ZombieInstance zombie, BehaviorContext context) {
        context.damageZombie(zombie, SELF_DESTRUCT_DAMAGE);
    }

    protected static void beginSpecialAction(ZombieInstance zombie) {
        if (zombie.getState() != ZombieState.DYING && zombie.getState() != ZombieState.DEAD) {
            zombie.setState(ZombieState.SPECIAL_ACTION);
        }
    }

    protected static void clearSpecialAction(ZombieInstance zombie) {
        if (zombie.getState() == ZombieState.SPECIAL_ACTION) {
            zombie.setState(ZombieState.WALKING);
        }
    }

    /**
     * Zombotany abilities mirror their plant counterparts, so
     * plants.json stays the single source of truth for damage balance.
     */
    protected static int definitionDamage(String plantName, int fallback) {
        Plant definition = plantDefinition(plantName);
        return definition != null && definition.getDamage() > 0
                ? definition.getDamage()
                : fallback;
    }

    /** Loaded plant definition, or a named stub so projectile art can still resolve. */
    protected static Plant plantDefinition(String plantName) {
        try {
            PlantFactory.getAllDefinitions();
        } catch (IllegalStateException notInitialised) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
            } catch (IOException | RuntimeException loadError) {
                return dummyPlant(plantName);
            }
        }
        try {
            if (PlantFactory.hasDefinition(plantName)) {
                return PlantFactory.getDefinition(plantName);
            }
        } catch (RuntimeException ignored) {
            return dummyPlant(plantName);
        }
        return dummyPlant(plantName);
    }

    private static Plant dummyPlant(String plantName) {
        return new Plant(0, plantName, PlantCategory.SHOOTER, List.of(), 0, 1, 0,
                0f, 0f, null, 0f, null, 0f, null);
    }
}
