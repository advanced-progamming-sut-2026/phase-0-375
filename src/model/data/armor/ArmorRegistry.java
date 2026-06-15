package model.data.armor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.enums.ArmorType;
import model.zombie.armor.Armor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@code ArmorTypeData.json} and builds the corresponding
 * {@link Armor} objects by alias.
 */
public class ArmorRegistry {

    /** alias (e.g. "ConeDefault") -> parsed data entry */
    private final Map<String, ArmorDataEntry> byAlias = new HashMap<>();

    private ArmorRegistry() {}

    /**
     * Loads the armor registry from a classpath resource.
     *
     * @param classpathPath JSON file path
     * @throws IOException if the file cannot be read or parsed
     */
    public static ArmorRegistry load(String classpathPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = ArmorRegistry.class.getResourceAsStream(classpathPath);
        if (inputStream == null) {
            throw new IOException("ArmorTypeData resource not found: " + classpathPath);
        }
        List<ArmorDataEntry> entries = mapper.readValue(inputStream, new TypeReference<>() {});

        ArmorRegistry registry = new ArmorRegistry();
        for (ArmorDataEntry entry : entries) {
            if (entry.getAliases() != null) {
                for (String alias : entry.getAliases()) {
                    registry.byAlias.put(alias, entry);
                }
            }
        }
        return registry;
    }

    /**
     * Creates a fresh {@link Armor} instance for the given alias.
     *
     * @param alias e.g. "ConeDefault"
     * @return new Armor instance, or null if the alias is unknown
     */
    public Armor create(String alias) {
        ArmorDataEntry entry = byAlias.get(alias);
        if (entry == null) {
            return null;
        }
        ArmorDataEntry.ArmorObjData armorData = entry.getObjdata();

        ArmorType type = resolveArmorType(armorData.getArmorType());
        if (type == null) {
            return null;
        }

        Armor armor = new Armor(
                type,
                armorData.getBaseHealth(),
                armorData.isMetallic(),
                armorData.isDroppable(),
                armorData.isHelm(),
                armorData.passesDamageThrough()
        );

        // Copy visual damage-layer metadata
        if (armorData.getArmorLayers() != null) {
            armor.setDamageLayers(armorData.getArmorLayers());
        }
        if (armorData.getArmorLayerHealth() != null) {
            armor.setLayerThresholds(armorData.getArmorLayerHealth());
        }
        return armor;
    }

    public Armor create(ArmorType armorType) {
        String primaryAlias = armorType.getPrimaryAlias();
        return create(primaryAlias);
    }

    /** Returns true if the given alias is registered. */
    public boolean contains(String alias) {
        return byAlias.containsKey(alias);
    }

    // --- Helpers ---

    /**
     * Strips the RTID wrapper from a ZombieArmorProps reference.
     * e.g. "RTID(ConeDefault@ArmorTypes)" → "ConeDefault"
     */
    public static String stripRtid(String rtid) {
        if (rtid == null) return null;
        int start = rtid.indexOf('(');
        int at = rtid.indexOf('@');
        if (start < 0 || at < 0 || at <= start) return rtid;
        return rtid.substring(start + 1, at);
    }

    private ArmorType resolveArmorType(String raw) {
        if (raw == null) return null;
        switch (raw) {
            case "Cone":          return ArmorType.Cone;
            case "Bucket":        return ArmorType.Bucket;
            case "Brick":         return ArmorType.Brick;
            case "ShoulderArmor": return ArmorType.ShoulderArmor;
            case "Crown":         return ArmorType.Crown;
            case "Newspaper":     return ArmorType.Newspaper;
            default:
                System.err.println("[ArmorRegistry] Unknown armor type: " + raw);
                return null;
        }
    }
}