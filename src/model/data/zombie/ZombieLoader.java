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
        try (InputStream in = openZombieStream(classpathPath)) {
            return loadFromStream(in);
        }
    }

    /**
     * Opens the zombie definition JSON, first from the classpath, then from
     * the file system (relative to the working directory). Mirrors the
     * fallback used by {@code LevelRegistry} so both registries behave the
     * same when the assets folder is not on the classpath.
     */
    private static InputStream openZombieStream(String path) throws IOException {
        InputStream inputStream = ZombieLoader.class.getResourceAsStream(path);
        if (inputStream != null) return inputStream;

        String filePath = path.startsWith("/") ? path.substring(1) : path;
        java.io.File file = new java.io.File(filePath);
        if (!file.isFile()) {
            throw new IOException("zombies.json resource not found: " + path);
        }
        return new java.io.FileInputStream(file);
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
        ImpType impType = resolveImpType(zombieData.getImpType(), alias);
        List<ZombieBehaviorType> behaviors = resolveBehaviors(entry.getObjclass(), zombieData);
        float fireDamageMultiplier = zombieData.getFireDamageMultiplier();

        Zombie zombie = new Zombie(
                name, baseHP, speed, eatDPS,
                size, chapter, wavePointCost, weight,
                armorTypes, pushable, impType, behaviors,
                fireDamageMultiplier
        );
        populateBehaviorProps(zombie, zombieData);
        return zombie;
    }

    /**
     * Copies every behavior-relevant numeric/string field from the JSON
     * DTO onto the zombie's behavior-property map.
     */
    private void populateBehaviorProps(Zombie zombie, ZombieDataEntry.ZombieObjData data) {
        // Imp / Gargantuar
        zombie.putBehaviorProp("ImpTargetColumn", data.getImpTargetColumn());
        if (!data.getHealthThresholdToImpAmmoLayers().isEmpty()) {
            zombie.putBehaviorProp("HealthPercentThrowImp", data.getHealthThresholdToImpAmmoLayers().getFirst());
        }
        // Ra
        zombie.putBehaviorProp("MaxClaimedSunCurrency", data.getMaxClaimedSunCurrency());
        // Explorer
        zombie.putBehaviorProp("MaxTorchReach", data.getMaxTorchReach());
        // Tomb Raiser
        zombie.putBehaviorProp("NumberOfTombsToSpawn", data.getNumberOfTombsToSpawn());
        zombie.putBehaviorProp("TimeBetweenRaisings", data.getTimeBetweenRaisings());
        // Gargantuar / All Star
        zombie.putBehaviorProp("SmashDamage", data.getSmashDamage());
        zombie.putBehaviorProp("SmashDuration", data.getSmashDuration());
        zombie.putBehaviorProp("RunningSpeedScale", data.getRunningSpeedScale());
        // Hunter
        zombie.putBehaviorProp("SnowballsPerBarrage", data.getSnowballsPerBarrage());
        // Troglobite
        zombie.putBehaviorProp("NumberOfIceblocksToSpawnWith", data.getNumberOfIceblocksToSpawnWith());
        // Prospector
        zombie.putBehaviorProp("LaunchCountdown", data.getLaunchCountdown());
        // Piano
        zombie.putBehaviorProp("FastMoveSpeed", data.getFastMoveSpeed());
        // Zombotany
        zombie.putBehaviorProp("ShotIntervalSeconds", data.getShotIntervalSeconds());
        zombie.putBehaviorProp("FuseSeconds", data.getFuseSeconds());
        // I, Zombie sun zombie
        zombie.putBehaviorProp("SunProduceIntervalSeconds", data.getSunProduceIntervalSeconds());
        zombie.putBehaviorProp("SunProduceBaseAmount", data.getSunProduceBaseAmount());
        zombie.putBehaviorProp("SunProduceGrowthAmount", data.getSunProduceGrowthAmount());
        // Newspaper
        zombie.putBehaviorProp("EnragedDamageScale", data.getEnragedDamageScale());
        zombie.putBehaviorProp("EnragedSpeedScale", data.getEnragedSpeedScale());
        // Crystal Skull (Turquoise)
        zombie.putBehaviorProp("ChargingTime", data.getChargingTime());
        zombie.putBehaviorProp("LaserBeamLength", data.getLaserBeamLength());
        zombie.putBehaviorProp("LaserBeamDamage", data.getLaserBeamDamage());
        zombie.putBehaviorProp("ChargingTimeDecrementPerFiveSun", data.getChargingTimeDecrementPerFiveSun());
        // Fisherman
        zombie.putBehaviorProp("DelayBetweenCasting", data.getDelayBetweenCasting());
        // Juggler
        zombie.putBehaviorProp("MoveSpeedMultiplierWhileJuggling", data.getMoveSpeedMultiplierWhileJuggling());
        // Dark King
        zombie.putBehaviorProp("DelayBetweenKnightings", data.getDelayBetweenKnightings());
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
        if (objclass.contains("BarrelRoller")) return PushableItemType.BARREL;
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
                behaviors.add(ZombieBehaviorType.PIANO_SWAP);
                break;

            case "ZombieNewspaperProps":
                behaviors.add(ZombieBehaviorType.ENRAGE);
                break;

            case "ZombieArcadeProps":
                behaviors.add(ZombieBehaviorType.PUSH);
                break;

            case "ZombieBarrelRollerProps":
                behaviors.add(ZombieBehaviorType.PUSH);
                behaviors.add(ZombieBehaviorType.BARREL_ROLLER);
                break;

            case "ZombotanyPeashooterProps":
                behaviors.add(ZombieBehaviorType.ZOMBOTANY_PEASHOOTER);
                break;

            case "ZombotanyWallnutProps":
                // Passive: its wall-nut toughness comes from Hitpoints alone.
                break;

            case "ZombotanyJalapenoProps":
                behaviors.add(ZombieBehaviorType.ZOMBOTANY_JALAPENO);
                break;

            case "ZombotanySquashProps":
                behaviors.add(ZombieBehaviorType.ZOMBOTANY_SQUASH);
                break;
            case "IZombieSunProps":
                behaviors.add(ZombieBehaviorType.PRODUCE_SUN);
                break;

            case "ZombiePropertySheet":
            default:
                // No special behaviors.
                break;
        }

        return behaviors;
    }
}