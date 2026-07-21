package model.data.plant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.data.zombie.ZombieLoader;
import model.enums.*;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads every plant definition from {@code plants.json} and builds the
 * corresponding {@link Plant} domain objects.
 */
public class PlantLoader {

    /**
     * Loads all plant definitions from a classpath resource.
     *
     * @param classpathPath JSON file path, e.g. {@code "data/plants.json"}
     * @return unmodifiable list of plant definitions, one per JSON entry
     * @throws IOException if the file cannot be read or parsed
     */
    public List<Plant> load(String classpathPath) throws IOException {
        try (InputStream stream = openPlantStream(classpathPath)) {
            return loadFromStream(stream);
        }
    }

    /**
     * Opens the plant definition JSON, first from the classpath, then from
     * the file system relative to the working directory.
     */
    private static InputStream openPlantStream(String path) throws IOException {
        InputStream inputStream = PlantLoader.class.getResourceAsStream(path);
        if (inputStream != null) return inputStream;

        String filePath = path.startsWith("/") ? path.substring(1) : path;
        java.io.File file = new java.io.File(filePath);
        if (!file.isFile()) {
            throw new IOException("plants.json resource not found: " + path);
        }
        return new java.io.FileInputStream(file);
    }

    /**
     * Loads all plant definitions from an open {@link InputStream}.
     */
    public List<Plant> loadFromStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("plants.json input stream is null");
        }
        ObjectMapper mapper = new ObjectMapper();
        List<PlantDataEntry> entries = mapper.readValue(inputStream, new TypeReference<>() {});

        List<Plant> result = new ArrayList<>(entries.size());
        for (PlantDataEntry entry : entries) {
            Plant plant = buildPlant(entry);
            if (plant != null) {
                result.add(plant);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // --- Builder ---

    private Plant buildPlant(PlantDataEntry entry) {
        if (entry.getName() == null || entry.getName().isEmpty()) {
            System.err.println("[PlantLoader] Skipping entry with no name: id=" + entry.getId());
            return null;
        }

        PlantCategory category = resolveCategory(entry.getCategory());
        if (category == null) {
            System.err.println("[PlantLoader] Unknown category '" + entry.getCategory()
                    + "' for plant " + entry.getName());
            return null;
        }

        List<PlantTags> tags = resolveTags(entry.getTags());
        PlantAbilityType abilityType = resolveAbilityType(entry.getAbilityType());
        if (abilityType == null) {
            System.err.println("[PlantLoader] Unknown abilityType '" + entry.getAbilityType()
                    + "' for plant " + entry.getName());
            return null;
        }

        PlantFoodType plantFoodType = resolvePlantFoodType(entry.getPlantFoodType());
        PlantLevels levels = buildLevels(entry.getUpgrades());

        return new Plant(
                entry.getId(),
                entry.getName(),
                category,
                tags,
                entry.getCost(),
                entry.getBaseHp(),
                entry.getDamage(),
                entry.getRecharge(),
                entry.getActionInterval(),
                abilityType,
                entry.getAbilityValue(),
                plantFoodType,
                entry.getPlantFoodValue(),
                levels
        );
    }

    private PlantLevels buildLevels(List<PlantDataEntry.UpgradeEntry> upgrades) {
        if (upgrades == null || upgrades.isEmpty()) {
            return new PlantLevels(null, null, null);
        }
        LevelUpgrade l2 = null, l3 = null, l4 = null;
        for (PlantDataEntry.UpgradeEntry upgradeEntry : upgrades) {
            LevelUpgrade upgrade = buildUpgrade(upgradeEntry);
            if (upgrade == null) continue;
            switch (upgrade.getLevel()) {
                case 2: l2 = upgrade; break;
                case 3: l3 = upgrade; break;
                case 4: l4 = upgrade; break;
                default:
                    System.err.println("[PlantLoader] Unexpected upgrade level "
                            + upgrade.getLevel());
            }
        }
        return new PlantLevels(l2, l3, l4);
    }

    private LevelUpgrade buildUpgrade(PlantDataEntry.UpgradeEntry upgradeEntry) {
        LevelUpgradeType type = resolveUpgradeType(upgradeEntry.getType());
        if (type == null) {
            System.err.println("[PlantLoader] Unknown upgrade type '" + upgradeEntry.getType() + "'");
            return null;
        }
        PlantSpecialTag tag = PlantSpecialTag.NONE;
        if (upgradeEntry.getSpecialTag() != null && !upgradeEntry.getSpecialTag().isEmpty()) {
            tag = resolveSpecialTag(upgradeEntry.getSpecialTag());
            if (tag == null) {
                System.err.println("[PlantLoader] Unknown special tag '"
                        + upgradeEntry.getSpecialTag() + "' - treating as NONE");
                tag = PlantSpecialTag.NONE;
            }
        }
        return new LevelUpgrade(upgradeEntry.getLevel(), type, upgradeEntry.getValue(), tag);
    }

    // --- Enum resolvers ---

    private PlantCategory resolveCategory(String raw) {
        if (raw == null) return null;
        try { return PlantCategory.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private PlantAbilityType resolveAbilityType(String raw) {
        if (raw == null) return null;
        try { return PlantAbilityType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private PlantFoodType resolvePlantFoodType(String raw) {
        if (raw == null || raw.isEmpty()) return PlantFoodType.NONE;
        try { return PlantFoodType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) {
            System.err.println("[PlantLoader] Unknown plantFoodType '" + raw + "'");
            return PlantFoodType.NONE;
        }
    }

    private LevelUpgradeType resolveUpgradeType(String raw) {
        if (raw == null) return null;
        try { return LevelUpgradeType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private PlantSpecialTag resolveSpecialTag(String raw) {
        if (raw == null || raw.isEmpty()) return PlantSpecialTag.NONE;
        try { return PlantSpecialTag.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private List<PlantTags> resolveTags(List<String> rawTags) {
        List<PlantTags> out = new ArrayList<>();
        if (rawTags == null) return out;
        for (String raw : rawTags) {
            if (raw == null) continue;
            try { out.add(PlantTags.valueOf(raw.toUpperCase())); }
            catch (IllegalArgumentException e) {
                System.err.println("[PlantLoader] Unknown tag '" + raw + "' - ignored");
            }
        }
        return out;
    }
}
