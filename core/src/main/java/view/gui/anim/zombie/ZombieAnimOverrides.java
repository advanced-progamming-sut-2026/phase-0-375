package view.gui.anim.zombie;

import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for exclusive zombie animation resolvers. Owned by the zombie team.
 *
 * <p>Register via {@link ZombieAnimProfiles}; do not put per-zombie lines in shared packages.
 */
public final class ZombieAnimOverrides {
    @FunctionalInterface
    public interface Resolver {
        /**
         * @param role default role from {@link ZombieAnimAdapter}
         * @return custom pose, or {@code null} to keep the default
         */
        AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry, ZombieAnimRole role);
    }

    private final Map<String, Resolver> byName = new HashMap<>();

    public ZombieAnimOverrides register(String definitionName, Resolver resolver) {
        if (definitionName != null && resolver != null) {
            byName.put(definitionName, resolver);
        }
        return this;
    }

    public AnimPose tryResolve(ZombieInstance zombie, PamCatalog.PamEntry entry, ZombieAnimRole role) {
        if (zombie == null || zombie.getDefinition() == null) {
            return null;
        }
        Resolver resolver = byName.get(zombie.getDefinition().getName());
        return resolver == null ? null : resolver.resolve(zombie, entry, role);
    }

    public static ZombieAnimOverrides createDefault() {
        ZombieAnimOverrides overrides = new ZombieAnimOverrides();
        ZombieAnimProfiles.registerAll(overrides);
        return overrides;
    }
}
