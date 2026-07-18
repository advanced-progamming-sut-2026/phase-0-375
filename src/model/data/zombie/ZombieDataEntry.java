package model.data.zombie;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Raw DTO that mirrors one entry in {@code zombies.json}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZombieDataEntry {

    @JsonProperty("aliases")
    private List<String> aliases;

    /** e.g. "ZombiePropertySheet", "ZombieGargantuarProps" */
    @JsonProperty("objclass")
    private String objclass;

    @JsonProperty("objdata")
    private ZombieObjData objdata;

    public String getPrimaryAlias() {
        return (aliases != null && !aliases.isEmpty()) ? aliases.get(0) : null;
    }

    // --- Getters ---

    public List<String> getAliases() {
        return aliases;
    }

    public String getObjclass() {
        return objclass;
    }

    public ZombieObjData getObjdata() {
        return objdata;
    }


    // ---------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZombieObjData {

        @JsonProperty("Hitpoints")
        private int hitPoints;

        @JsonProperty("EatDPS")
        private float eatDPS;

        @JsonProperty("Speed")
        private float speed;

        @JsonProperty("WavePointCost")
        private int wavePointCost;

        @JsonProperty("Weight")
        private int weight;

        /**
         * "imp" | "large" — absent means normal.
         * Maps to ZombieSize enum.
         */
        @JsonProperty("Size")
        private String size;

        /**
         * "egypt_imp" | "iceage_imp" — absent means the zombie throws no imp.
         * Maps to ImpType enum (or null).
         */
        @JsonProperty("ImpType")
        private String impType;

        /**
         * Column (0-based from left) where a thrown imp lands.
         * Only meaningful when impType != null.
         */
        @JsonProperty("ImpTargetColumn")
        private int impTargetColumn = 2;

        /** HP fraction at which the imp is thrown (Gargantuar = 0.5). */
        @JsonProperty("HealthThresholdToImpAmmoLayers")
        private List<HealthThresholdToImpAmmoLayer> healthThresholdToImpAmmoLayers;

        /**
         * Armor refs: e.g. ["RTID(ConeDefault@ArmorTypes)"].
         * The alias between "RTID(" and "@ArmorTypes)" is extracted by the loader.
         */
        @JsonProperty("ZombieArmorProps")
        private List<String> zombieArmorProps;

        /**
         * Behavior refs: e.g. ["RTID(ZombieIceAgeProjectileAction@ZombieActions)"].
         * The action-name before @ZombieActions is used to resolve the behavior type.
         */
        @JsonProperty("Actions")
        private List<String> actions;

        /** Whether this zombie can drop plant food on death. */
        @JsonProperty("CanSpawnPlantFood")
        private boolean canSpawnPlantFood = true;

        // ---- fields used by behaviors ----

        /** ZombieRaProps – max sun stolen. */
        @JsonProperty("MaxClaimedSunCurrency")
        private int maxClaimedSunCurrency;

        /** ZombieExplorerProps – torch reach distance. */
        @JsonProperty("MaxTorchReach")
        private float maxTorchReach;

        /** ZombieTombRaiserProps – number of tombs per cast. */
        @JsonProperty("NumberOfTombsToSpawn")
        private int numberOfTombsToSpawn;

        /** ZombieTombRaiserProps – seconds between summons. */
        @JsonProperty("TimeBetweenRaisings")
        private float timeBetweenRaisings;

        /** ZombieGargantuarProps | ZombieModernAllStarProps – one-hit plant damage. */
        @JsonProperty("SmashDamage")
        private int smashDamage;

        /** ZombieGargantuarProps – duration of the smash animation in seconds. */
        @JsonProperty("SmashDuration")
        private float smashDuration;

        /** ZombieIceAgeHunterProps – snowballs thrown per attack. */
        @JsonProperty("SnowballsPerBarrage")
        private int snowballsPerBarrage;

        /** ZombieIceAgeTroglobite – how many ice blocks it starts with. */
        @JsonProperty("NumberOfIceblocksToSpawnWith")
        private int numberOfIceblocksToSpawnWith;

        /** ZombieProspectorProps – seconds until the dynamite explodes. */
        @JsonProperty("LaunchCountdown")
        private float launchCountdown;

        /** ZombiePianoProps – fast-speed multiplier while no piano. */
        @JsonProperty("FastMoveSpeed")
        private float fastMoveSpeed;

        /** ZombotanyPeashooterProps – seconds between pea shots. */
        @JsonProperty("ShotIntervalSeconds")
        private float shotIntervalSeconds;

        /** ZombotanyJalapenoProps – seconds on the lawn before igniting. */
        @JsonProperty("FuseSeconds")
        private float fuseSeconds;

        /** IZombieSunProps - seconds between sun drops. */
        @JsonProperty("SunProduceIntervalSeconds")
        private float sunProduceIntervalSeconds;

        /** IZombieSunProps - sun granted by the first drop. */
        @JsonProperty("SunProduceBaseAmount")
        private int sunProduceBaseAmount;

        /** IZombieSunProps - extra sun added to every subsequent drop. */
        @JsonProperty("SunProduceGrowthAmount")
        private int sunProduceGrowthAmount;

        /** ZombieNewspaperProps – eat speed scale when enraged. */
        @JsonProperty("EnragedDamageScale")
        private float enragedDamageScale;

        /** ZombieNewspaperProps – movement speed scale when enraged. */
        @JsonProperty("EnragedSpeedScale")
        private float enragedSpeedScale;

        /** ZombieCrystalSkullProps – seconds of charge before laser fires. */
        @JsonProperty("ChargingTime")
        private float chargingTime;

        /** ZombieCrystalSkullProps – laser range. */
        @JsonProperty("LaserBeamLength")
        private float laserBeamLength;

        /** ZombieCrystalSkullProps – damage per tile hit by laser. */
        @JsonProperty("LaserBeamDamage")
        private int laserBeamDamage;

        /** ZombieCrystalSkullProps – sun stolen per second. */
        @JsonProperty("ChargingTimeDecrementPerFiveSun")
        private float chargingTimeDecrementPerFiveSun;

        /** ZombieBeachFishermanProps – seconds between hook casts. */
        @JsonProperty("DelayBetweenCasting")
        private float delayBetweenCasting;

        /** ZombieDarkJugglerProps – speed multiplier while juggling. */
        @JsonProperty("MoveSpeedMultiplierWhileJuggling")
        private float moveSpeedMultiplierWhileJuggling;

        /** ZombieModernAllStarProps – speed scale after eating a plant. */
        @JsonProperty("RunningSpeedScale")
        private float runningSpeedScale;

        /** ZombieDarkKingProps – seconds between knighting actions. */
        @JsonProperty("DelayBetweenKnightings")
        private float delayBetweenKnightings;

        /**
         * Multiplier applied to incoming FIRE-elemental damage.
         * Defaults to {@code 1.0f} (full damage).
         */
        @JsonProperty("FireDamageMultiplier")
        private float fireDamageMultiplier = 1.0f;

        // --- getters ---

        public int getHitPoints() {
            return hitPoints;
        }

        public float getEatDPS() {
            return eatDPS;
        }

        public float getSpeed() {
            return speed;
        }

        public int getWavePointCost() {
            return wavePointCost;
        }

        public int getWeight() {
            return weight;
        }

        public String getSize() {
            return size;
        }

        public String getImpType() {
            return impType;
        }

        public int getImpTargetColumn() {
            return impTargetColumn;
        }

        public List<Float> getHealthThresholdToImpAmmoLayers() {
            if (healthThresholdToImpAmmoLayers == null
                    || healthThresholdToImpAmmoLayers.isEmpty()) {
                return Collections.emptyList();
            }
            List<Float> out = new ArrayList<>(
                    healthThresholdToImpAmmoLayers.size());
            for (HealthThresholdToImpAmmoLayer layer : healthThresholdToImpAmmoLayers) {
                if (layer != null) {
                    out.add(layer.getHealthPercentThrowImp());
                }
            }
            return out;
        }

        /** Returns the raw (typed) list of imp-throw threshold layer entries. */
        public List<HealthThresholdToImpAmmoLayer> getHealthThresholdToImpAmmoLayerEntries() {
            return healthThresholdToImpAmmoLayers;
        }

        public List<String> getZombieArmorProps() {
            return zombieArmorProps;
        }

        public List<String> getActions() {
            return actions;
        }

        public boolean isCanSpawnPlantFood() {
            return canSpawnPlantFood;
        }

        public int getMaxClaimedSunCurrency() {
            return maxClaimedSunCurrency;
        }

        public float getMaxTorchReach() {
            return maxTorchReach;
        }

        public int getNumberOfTombsToSpawn() {
            return numberOfTombsToSpawn;
        }

        public float getTimeBetweenRaisings() {
            return timeBetweenRaisings;
        }

        public int getSmashDamage() {
            return smashDamage;
        }

        public float getSmashDuration() {
            return smashDuration;
        }

        public int getSnowballsPerBarrage() {
            return snowballsPerBarrage;
        }

        public int getNumberOfIceblocksToSpawnWith() {
            return numberOfIceblocksToSpawnWith;
        }

        public float getLaunchCountdown() {
            return launchCountdown;
        }

        public float getFastMoveSpeed() {
            return fastMoveSpeed;
        }

        public float getShotIntervalSeconds() {
            return shotIntervalSeconds;
        }

        public float getFuseSeconds() {
            return fuseSeconds;
        }

    public float getSunProduceIntervalSeconds() {
        return sunProduceIntervalSeconds;
    }

    public int getSunProduceBaseAmount() {
        return sunProduceBaseAmount;
    }

    public int getSunProduceGrowthAmount() {
        return sunProduceGrowthAmount;
    }

        public float getEnragedDamageScale() {
            return enragedDamageScale;
        }

        public float getEnragedSpeedScale() {
            return enragedSpeedScale;
        }

        public float getChargingTime() {
            return chargingTime;
        }

        public float getLaserBeamLength() {
            return laserBeamLength;
        }

        public int getLaserBeamDamage() {
            return laserBeamDamage;
        }

        public float getChargingTimeDecrementPerFiveSun() {
            return chargingTimeDecrementPerFiveSun;
        }

        public float getDelayBetweenCasting() {
            return delayBetweenCasting;
        }

        public float getMoveSpeedMultiplierWhileJuggling() {
            return moveSpeedMultiplierWhileJuggling;
        }

        public float getRunningSpeedScale() {
            return runningSpeedScale;
        }

        public float getDelayBetweenKnightings() {
            return delayBetweenKnightings;
        }

        public float getFireDamageMultiplier() {
            return fireDamageMultiplier;
        }
    }

    /**
     * Mirrors one element of the {@code HealthThresholdToImpAmmoLayers}
     * array on the Gargantuar entry. The Gargantuar throws its imp
     * when its HP drops to {@link #healthPercentThrowImp} of max.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HealthThresholdToImpAmmoLayer {

        /** HP fraction (0..1) at which the imp is thrown. */
        @JsonProperty("HealthPercentThrowImp")
        private float healthPercentThrowImp = 0.5f;

        /** Visual sprite layers to hide once the imp is thrown. */
        @JsonProperty("ProjectileLayersToHide")
        private List<String> projectileLayersToHide;

        public float getHealthPercentThrowImp() {
            return healthPercentThrowImp;
        }

        public List<String> getProjectileLayersToHide() {
            return projectileLayersToHide;
        }

        public void setHealthPercentThrowImp(float healthPercentThrowImp) {
            this.healthPercentThrowImp = healthPercentThrowImp;
        }

        public void setProjectileLayersToHide(List<String> projectileLayersToHide) {
            this.projectileLayersToHide = projectileLayersToHide;
        }
    }
}