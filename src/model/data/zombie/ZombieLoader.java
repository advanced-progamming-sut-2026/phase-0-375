package model.data.zombie;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.data.armor.ArmorRegistry;
import model.enums.*;
import model.zombie.armor.Armor;
import model.zombie.behavior.*;
import model.zombie.definition.Zombie;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads every zombie definition from {@code zombies.json} and builds the
 * corresponding {@link Zombie} objects.
 */
public class ZombieLoader {

    private final ArmorRegistry armorRegistry;

    public ZombieLoader(ArmorRegistry armorRegistry) {
        this.armorRegistry = armorRegistry;
    }

    /**
     * Loads all zombie definitions from a classpath resource.
     *
     * @param classpathPath JSON file path
     * @return unmodifiable list of zombie definitions, one per JSON entry
     * @throws IOException if the file cannot be read or parsed
     */
    public List<Zombie> load(String classpathPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = ZombieLoader.class.getResourceAsStream(classpathPath);
        if (inputStream == null) {
            throw new IOException("zombies.json resource not found: " + classpathPath);
        }
        try (InputStream in = inputStream) {
            return loadFromStream(in);
        }
    }

    /**
     * Loads all zombie definitions from an open {@link InputStream}.
     * Useful for tests and for callers that already have the JSON in
     * hand.
     *
     * @param inputStream open JSON stream; not closed by this method
     * @return unmodifiable list of zombie definitions, one per JSON entry
     * @throws IOException if the stream cannot be parsed
     */
    public List<Zombie> loadFromStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("zombies.json input stream is null");
        }
        ObjectMapper mapper = new ObjectMapper();
        List<ZombieDataEntry> entries = mapper.readValue(inputStream, new TypeReference<>() {});

        List<Zombie> result = new ArrayList<>(entries.size());
        for (ZombieDataEntry entry : entries) {
            Zombie zombie = buildZombie(entry);
            if (zombie != null) {
                result.add(zombie);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private Zombie buildZombie(ZombieDataEntry entry) {
        if (entry.getObjdata() == null) {
            System.err.println("[ZombieLoader] Skipping entry with no objdata: " + entry.getPrimaryAlias());
            return null;
        }

        ZombieDataEntry.ZombieObjData zombieData = entry.getObjdata();
        String alias = entry.getPrimaryAlias();

        String name = alias;
        int baseHP = zombieData.getHitPoints();
        float speed = zombieData.getSpeed();
        float eatDPS  = zombieData.getEatDPS();
        ZombieSize size = resolveSize(zombieData.getSize());
        Chapter chapter = resolveChapter(entry.getObjclass(), alias);
        int wavePointCost = zombieData.getWavePointCost();
        int weight = zombieData.getWeight();
        List<ArmorType> armorTypes = resolveArmorTypes(zombieData.getZombieArmorProps());
        PushableItemType pushable = resolvePushable(entry.getObjclass(), zombieData);
        EquippedItemType equipped = resolveEquipped(entry.getObjclass(), zombieData);
        ImpType impType = resolveImpType(zombieData.getImpType(), alias);
        List<ZombieBehaviorType> behaviors = resolveBehaviors(entry.getObjclass(), zombieData);
        float fireDamageMultiplier = zombieData.getFireDamageMultiplier();

        return new Zombie(
                name, baseHP, speed, eatDPS,
                size, chapter, wavePointCost, weight,
                armorTypes, pushable, equipped, impType, behaviors,
                fireDamageMultiplier
        );
    }

    // --- Enum resolvers ---

    private ZombieSize resolveSize(String raw) {
        if (raw == null) return ZombieSize.NORMAL;
        switch (raw.toLowerCase()) {
            case "imp": return ZombieSize.IMP;
            case "large": return ZombieSize.LARGE;
            default: return ZombieSize.NORMAL;
        }
    }

    /** Resolves the {@link ImpType} set on a zombie definition */
    private ImpType resolveImpType(String raw, String alias) {
        if (raw != null) {
            switch (raw.toLowerCase()) {
                case "egypt_imp": return ImpType.EGYPT_IMP;
                case "iceage_imp": return ImpType.ICEAGE_IMP;
                case "dragon_imp":
                case "dark_imp_dragon":
                    return ImpType.DRAGON_IMP;
                default:
                    System.err.println("[ZombieLoader] Unknown ImpType: " + raw);
                    return null;
            }
        }
        // No explicit ImpType field - fall back to alias-based detection
        // for zombies that are imps themselves.
        if (alias != null) {
            String lower = alias.toLowerCase();
            if (lower.contains("impdragon") || lower.contains("darkimpdragon")) {
                return ImpType.DRAGON_IMP;
            }
        }
        return null;
    }

    /**
     * Derives the chapter a zombie belongs to from its objclass name.
     * Zombies shared across every chapters return null.
     */
    private Chapter resolveChapter(String objclass, String alias) {
        if (objclass == null) return null;
        if (objclass.contains("IceAge")) return Chapter.FROSTBITE_CAVES;
        if (objclass.contains("Beach")) return Chapter.BIG_WAVE_BEACH;
        if (objclass.contains("Dark")) return Chapter.DARK_AGES;
        // Explorer and TombRaiser are Egypt-specific
        if (objclass.contains("Explorer") || objclass.contains("TombRaiser") || objclass.contains("Ra"))
            return Chapter.ANCIENT_EGYPT;
        // others appear in any chapter
        return null;
    }

    /**
     * @return Proper {@link ArmorType} from the given RTID list
     */
    private List<ArmorType> resolveArmorTypes(List<String> rtidList) {
        if (rtidList == null || rtidList.isEmpty()) return Collections.emptyList();
        List<ArmorType> result = new ArrayList<>(rtidList.size());
        for (String rtid : rtidList) {
            String alias = ArmorRegistry.stripRtid(rtid);
            Armor armor = armorRegistry.create(alias);
            if (armor != null) {
                result.add(armor.getType());
            } else {
                System.err.println("[ZombieLoader] Unknown armor alias: " + alias);
            }
        }
        return result;
    }

    private PushableItemType resolvePushable(String objclass, ZombieDataEntry.ZombieObjData zombieData) {
        if (objclass == null) return null;
        if (objclass.contains("Troglobite")) return PushableItemType.ICE_BLOCK;
        if (objclass.contains("Arcade")) return PushableItemType.ARCADE_MACHINE;
        if (objclass.contains("Piano")) return PushableItemType.PIANO;
        return null;
    }

    private EquippedItemType resolveEquipped(String objclass, ZombieDataEntry.ZombieObjData zombieData) {
        // TODO: write this method after implementing EquippedItemType
        return null;
    }

    /**
     * Derives the list of special behaviors for a zombie based on its
     * {@code objclass} and numeric-data fields.
     */
    private List<ZombieBehaviorType> resolveBehaviors(String objclass, ZombieDataEntry.ZombieObjData zombieData) {
        List<ZombieBehaviorType> behaviors = new ArrayList<>();
        if (objclass == null) return behaviors;

        switch (objclass) {
            case "ZombieRaProps":
                behaviors.add(ZombieBehaviorType.STEAL_SUN);
                break;

            case "ZombieExplorerProps":
                behaviors.add(ZombieBehaviorType.SHOOT);
                break;

            case "ZombieTombRaiserProps":
                behaviors.add(ZombieBehaviorType.SUMMON);
                break;

            case "ZombieGargantuarProps":
                behaviors.add(ZombieBehaviorType.SMASH);
                behaviors.add(ZombieBehaviorType.THROW_IMP);
                break;

            case "ZombieIceAgeDodoProps":
                behaviors.add(ZombieBehaviorType.FLY);
                break;

            case "ZombieIceAgeHunterProps":
                behaviors.add(ZombieBehaviorType.SHOOT);
                break;

            case "ZombieIceAgeTroglobiteProps":
                behaviors.add(ZombieBehaviorType.PUSH);
                break;

            case "ZombieBeachFishermanProps":
                behaviors.add(ZombieBehaviorType.FISH);
                break;

            case "ZombieBeachOctopusProps":
                behaviors.add(ZombieBehaviorType.SHOOT);
                break;

            case "ZombieBeachSnorkelProps":
                behaviors.add(ZombieBehaviorType.SWIM);
                break;

            case "ZombieDarkJugglerProps":
                behaviors.add(ZombieBehaviorType.JUGGLE);
                break;

            case "ZombieLostCityJaneProps":
                behaviors.add(ZombieBehaviorType.DEFLECT_LOBBER);
                break;

            case "ZombieDarkWizardProps":
                behaviors.add(ZombieBehaviorType.TRANSFORM);
                break;

            case "ZombieDarkKingProps":
                behaviors.add(ZombieBehaviorType.BUFF);
                break;

            case "ZombieCrystalSkullProps":
                behaviors.add(ZombieBehaviorType.STEAL_SUN);
                break;

            case "ZombieProspectorProps":
                behaviors.add(ZombieBehaviorType.JUMP);
                break;

            case "ZombieModernAllStarProps":
                behaviors.add(ZombieBehaviorType.SMASH);
                break;

            case "ZombiePianoProps":
                behaviors.add(ZombieBehaviorType.PUSH);
                break;

            case "ZombieNewspaperProps":
                break;

            case "ZombieArcadeProps":
                behaviors.add(ZombieBehaviorType.PUSH);
                break;

            case "ZombiePropertySheet":
            default:
                // No special behaviors.
                break;
        }

        return behaviors;
    }
}