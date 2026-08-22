package view.gui.anim.plant;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for exclusive plant animation resolvers. Owned by the plant team.
 *
 * <p>Register via {@link PlantAnimProfiles} from {@code view.gui.anim.plant.exclusive}.
 * Do not put per-plant clip names in the adapter or the model.
 */
public final class PlantAnimOverrides {
    @FunctionalInterface
    public interface Resolver {
        /**
         * @param role default role from {@link PlantAnimAdapter}
         * @return custom pose, or {@code null} to keep the default
         */
        AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role);
    }

    @FunctionalInterface
    public interface DurationResolver {
        /**
         * @return presentation length in seconds, or {@code 0} to fall back to the
         *         resolved clip's catalog duration
         */
        float duration(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role);
    }

    @FunctionalInterface
    public interface ImpactFractionResolver {
        /**
         * @return attack fire fraction in {@code (0, 1]}, or {@code 0} to keep the default
         */
        float fraction(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role);
    }

    private final Map<String, Resolver> byName = new HashMap<>();
    private final Map<String, DurationResolver> durationByName = new HashMap<>();
    private final Map<String, ImpactFractionResolver> impactByName = new HashMap<>();

    public PlantAnimOverrides register(String definitionName, Resolver resolver) {
        if (definitionName != null && resolver != null) {
            byName.put(definitionName, resolver);
        }
        return this;
    }

    public PlantAnimOverrides registerDuration(String definitionName, DurationResolver resolver) {
        if (definitionName != null && resolver != null) {
            durationByName.put(definitionName, resolver);
        }
        return this;
    }

    public PlantAnimOverrides registerImpactFraction(String definitionName,
                                                     ImpactFractionResolver resolver) {
        if (definitionName != null && resolver != null) {
            impactByName.put(definitionName, resolver);
        }
        return this;
    }

    public AnimPose tryResolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant == null || plant.getDefinition() == null) {
            return null;
        }
        Resolver resolver = byName.get(plant.getDefinition().getName());
        return resolver == null ? null : resolver.resolve(plant, entry, role);
    }

    public float tryDuration(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant == null || plant.getDefinition() == null) {
            return 0f;
        }
        DurationResolver resolver = durationByName.get(plant.getDefinition().getName());
        return resolver == null ? 0f : Math.max(0f, resolver.duration(plant, entry, role));
    }

    public float tryImpactFraction(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant == null || plant.getDefinition() == null) {
            return 0f;
        }
        ImpactFractionResolver resolver = impactByName.get(plant.getDefinition().getName());
        return resolver == null ? 0f : Math.max(0f, resolver.fraction(plant, entry, role));
    }

    public static PlantAnimOverrides createDefault() {
        PlantAnimOverrides overrides = new PlantAnimOverrides();
        PlantAnimProfiles.registerAll(overrides);
        return overrides;
    }
}
