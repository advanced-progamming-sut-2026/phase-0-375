package model.plant;

import model.data.plant.PlantLoader;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single entry point for all plant creation.
 */
public class PlantFactory {

    private static PlantFactory instance;

    private final Map<String, Plant> definitionsByName;
    private final Map<Integer, Plant> definitionsById;

    private PlantFactory(List<Plant> definitions) {
        Map<String, Plant> byName = new HashMap<>(definitions.size() * 2);
        Map<Integer, Plant> byId = new HashMap<>(definitions.size() * 2);
        for (Plant plant : definitions) {
            byName.put(plant.getName(), plant);
            byId.put(plant.getId(), plant);
        }
        this.definitionsByName = Collections.unmodifiableMap(byName);
        this.definitionsById = Collections.unmodifiableMap(byId);
    }

    // --- Static factory ---

    /**
     * Loads plant definitions from a classpath resource.
     *
     * @param classpathPath e.g. {@code "data/plants.json"}
     */
    public static void init(String classpathPath) throws IOException {
        PlantLoader loader = new PlantLoader();
        List<Plant> definitions = loader.load(classpathPath);
        instance = new PlantFactory(definitions);
    }

    /** Loads plant definitions from an already-open stream (tests). */
    public static void init(java.io.InputStream stream) throws IOException {
        PlantLoader loader = new PlantLoader();
        List<Plant> definitions = loader.loadFromStream(stream);
        instance = new PlantFactory(definitions);
    }

    // --- Definition access ---

    /** Returns the plant definition for the given name, or {@code null}. */
    public static Plant getDefinition(String name) {
        requireInit();
        return instance.definitionsByName.get(name);
    }

    /** Returns the plant definition for the given id, or {@code null}. */
    public static Plant getDefinitionById(int id) {
        requireInit();
        return instance.definitionsById.get(id);
    }

    /** @return unmodifiable view of all loaded plant definitions. */
    public static List<Plant> getAllDefinitions() {
        requireInit();
        return List.copyOf(instance.definitionsByName.values());
    }

    /** @return true if a definition exists for the given name. */
    public static boolean hasDefinition(String name) {
        requireInit();
        return instance.definitionsByName.containsKey(name);
    }

    // --- Instance creation ---

    /**
     * Creates a fresh level-1 {@link PlantInstance} from the definition
     * registered under {@code name}.
     *
     * @return the new instance, or {@code null} if the name is unknown
     */
    public static PlantInstance createInstance(String name) {
        Plant definition = getDefinition(name);
        if (definition == null) {
            System.err.println("[PlantFactory] Unknown plant name: " + name);
            return null;
        }
        return createInstance(definition);
    }

    /** Creates a fresh level-1 {@link PlantInstance} from a definition. */
    public static PlantInstance createInstance(Plant definition) {
        return new PlantInstance(definition);
    }

    /**
     * Creates a fresh {@link PlantInstance} at the given target level.
     */
    public static PlantInstance createInstance(String name, int targetLevel) {
        PlantInstance plant = createInstance(name);
        if (plant != null && targetLevel > 1) {
            plant.applyLevelUpgrade(targetLevel);
        }
        return plant;
    }

    private static void requireInit() {
        if (instance == null) {
            throw new IllegalStateException("PlantFactory not initialised - call PlantFactory.init(...) first");
        }
    }
}
