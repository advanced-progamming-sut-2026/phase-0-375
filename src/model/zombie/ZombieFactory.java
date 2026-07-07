package model.zombie;

import model.data.armor.ArmorRegistry;
import model.data.zombie.ZombieLoader;
import model.enums.ArmorType;
import model.enums.PushableItemType;
import model.item.equippable.Equippable;
import model.item.pushable.ArcadeMachine;
import model.item.pushable.IceBlock;
import model.item.pushable.Piano;
import model.item.pushable.Pushable;
import model.zombie.armor.Armor;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single entry point for all zombie creation.
 */
public class ZombieFactory {
    private static final int ICE_BLOCK_HP = 600;

    private final ArmorRegistry armorRegistry;

    /** All definitions keyed by their primary alias (e.g. "ZombieDefault"). */
    private final Map<String, Zombie> definitionsByName;

    private static ZombieFactory instance;

    private ZombieFactory(ArmorRegistry armorRegistry, List<Zombie> definitions) {
        this.armorRegistry = armorRegistry;

        Map<String, Zombie> definitionsByName = new HashMap<>(definitions.size() * 2);
        for (Zombie zombie : definitions) {
            definitionsByName.put(zombie.getName(), zombie);
        }
        this.definitionsByName = Collections.unmodifiableMap(definitionsByName);
    }

    // --- Static factory ---

    /** Loads the armor registry and zombie definitions from the given classpath resources. */
    public static void init(String zombiesJsonPath, String armorJsonPath) throws IOException {
        ArmorRegistry registry = ArmorRegistry.load(armorJsonPath);
        ZombieLoader loader = new ZombieLoader(registry);
        List<Zombie> definitions = loader.load(zombiesJsonPath);
        instance = new ZombieFactory(registry, definitions);
    }

    // --- Definition access ---

    /**
     * Returns the zombie definition for the given name, or {@code null} if
     * no definition with that name was loaded.
     */
    public static Zombie getDefinition(String name) {
        return instance.definitionsByName.get(name);
    }

    /** Returns an unmodifiable view of all loaded zombie definitions. */
    public static List<Zombie> getAllDefinitions() {
        return List.copyOf(instance.definitionsByName.values());
    }

    /** @return true if a definition exists for the given name. */
    public static boolean hasDefinition(String name) {
        return instance.definitionsByName.containsKey(name);
    }

    /** Builds a fresh {@link Armor} instance of the given {@link ArmorType}. */
    public static Armor createArmor(ArmorType armorType) {
        if (instance == null || armorType == null) return null;
        return instance.armorRegistry.create(armorType);
    }

    // --- Instance creation ---

    /** Creates a {@link ZombieInstance} from the definition registered under {@code name}. */
    public static ZombieInstance createInstance(String name) {
        Zombie definition = getDefinition(name);
        if (definition == null) {
            System.err.println("[ZombieFactory] Unknown zombie name: " + name);
            return null;
        }
        return createInstance(definition);
    }

    /** Creates {@link ZombieInstance} directly from a definition. */
    public static ZombieInstance createInstance(Zombie definition) {
        List<Armor> armors = instance.buildArmors(definition);
        Pushable pushable = instance.buildPushable(definition);
        Equippable equipped = instance.buildEquipped(definition);

        return new ZombieInstance(definition, armors, pushable, equipped);
    }

    // --- Item builders ---

    private List<Armor> buildArmors(Zombie definition) {
        if (!definition.hasArmor()) return Collections.emptyList();

        List<Armor> armors = new ArrayList<>(definition.getArmorTypes().size());
        for (ArmorType armorType : definition.getArmorTypes()) {
            Armor armor = instance.armorRegistry.create(armorType);
            if (armor != null) {
                armors.add(armor);
            } else {
                System.err.println("[ZombieFactory] Could not build armor: " + armorType);
            }
        }
        return armors;
    }

    private Pushable buildPushable(Zombie definition) {
        PushableItemType type = definition.getPushableItemType();
        if (type == null) return null;

        switch (type) {
            case ARCADE_MACHINE: return new ArcadeMachine(definition.getBaseHP());
            case ICE_BLOCK: return new IceBlock(ICE_BLOCK_HP);
            case PIANO: return new Piano(definition.getBaseHP());
            default:
                System.err.println("[ZombieFactory] Unknown PushableItemType: " + type);
                return null;
        }
    }

    private Equippable buildEquipped(Zombie definition) {
        // TODO: write this method after implementing EquippedItemType
        return null;
    }
}