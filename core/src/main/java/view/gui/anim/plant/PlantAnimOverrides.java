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

    private final Map<String, Resolver> byName = new HashMap<>();

    public PlantAnimOverrides register(String definitionName, Resolver resolver) {
        if (definitionName != null && resolver != null) {
            byName.put(definitionName, resolver);
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

    public static PlantAnimOverrides createDefault() {
        PlantAnimOverrides overrides = new PlantAnimOverrides();
        PlantAnimProfiles.registerAll(overrides);
        return overrides;
    }
}
