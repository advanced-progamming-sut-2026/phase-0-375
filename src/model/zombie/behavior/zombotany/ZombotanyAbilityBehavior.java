package model.zombie.behavior.zombotany;

import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.zombie.behavior.BehaviorContext;
import model.zombie.behavior.ZombieBehavior;
import model.zombie.instance.ZombieInstance;

import java.io.IOException;

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

    /**
     * Zombotany abilities mirror their plant counterparts, so
     * plants.json stays the single source of truth for damage balance.
     */
    protected static int definitionDamage(String plantName, int fallback) {
        try {
            PlantFactory.getAllDefinitions();
        } catch (IllegalStateException notInitialised) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
            } catch (IOException | RuntimeException loadError) {
                return fallback;
            }
        }
        if (!PlantFactory.hasDefinition(plantName)) {
            return fallback;
        }
        Plant definition = PlantFactory.getDefinition(plantName);
        return definition != null && definition.getDamage() > 0
                ? definition.getDamage()
                : fallback;
    }
}
