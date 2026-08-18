package model.zombie.behavior;

import model.enums.ArmorType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.zombie.ZombieFactory;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dark King: {@code intro}, then idle in place and periodically {@code special}
 * to knight a nearby peasant (Crown + Shoulder Armor).
 */
public class BuffBehavior implements ZombieBehavior {

    public enum KingPhase { INTRO, IDLE, SPECIAL }

    /** {@code intro} on {@code ZOMBIE_DARK_KING}. */
    public static final float INTRO_DURATION = 3.2333f;
    /** {@code idle} clip length. */
    public static final float IDLE_DURATION = 4f;
    /** {@code idle2} clip length. */
    public static final float IDLE2_DURATION = 2.9333f;
    /** {@code special} clip length. */
    public static final float SPECIAL_DURATION = 4f;
    /** Chance to play {@code idle2} instead of {@code idle} at each loop. */
    public static final float IDLE2_WEIGHT = 0.25f;

    /** Default seconds between two consecutive knighting actions. */
    public static final float DEFAULT_DELAY_BETWEEN_KNIGHTINGS = 2.5f;

    /** Default columns to either side of the King that fall within its knighting area. */
    public static final int DEFAULT_KNIGHTING_AREA_COL_RADIUS = 4;

    /** Default rows above/below the King that fall within its knighting area. */
    public static final int DEFAULT_KNIGHTING_AREA_ROW_RADIUS = 3;

    /** Definition name of the plain zombie that is eligible to be knighted. */
    public static final String PLAIN_ZOMBIE_NAME = "ZombieDefault";

    private KingPhase phase = KingPhase.INTRO;
    private float phaseTimer = 0f;
    private float knightTimer = 0f;
    private boolean idle2;

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        if (isDarkKing(zombie)) {
            tickDarkKing(zombie, context, deltaTime);
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.BUFF;
    }

    private void tickDarkKing(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (phase == KingPhase.INTRO) {
            zombie.setState(ZombieState.SPAWNING);
            phaseTimer += deltaTime;
            if (phaseTimer >= INTRO_DURATION) {
                enterIdle();
                holdStation(zombie);
            }
            return;
        }

        holdStation(zombie);

        if (phase == KingPhase.SPECIAL) {
            phaseTimer += deltaTime;
            if (phaseTimer >= SPECIAL_DURATION) {
                enterIdle();
                knightTimer = 0f;
            }
            return;
        }

        phaseTimer += deltaTime;
        if (phaseTimer >= idleDuration()) {
            pickIdle();
        }

        knightTimer += deltaTime;
        if (knightTimer < delayBetween(zombie)) {
            return;
        }
        ZombieInstance target = findKnightableZombie(zombie, context);
        if (target == null) {
            return;
        }
        knightTimer = 0f;
        phase = KingPhase.SPECIAL;
        phaseTimer = 0f;
        knight(target);
    }

    private static void holdStation(ZombieInstance zombie) {
        if (zombie.getState() != ZombieState.SPECIAL_ACTION
                && zombie.getState() != ZombieState.DYING
                && zombie.getState() != ZombieState.DEAD) {
            zombie.setState(ZombieState.SPECIAL_ACTION);
        }
    }

    private static float delayBetween(ZombieInstance zombie) {
        if (zombie.getDefinition() == null) {
            return DEFAULT_DELAY_BETWEEN_KNIGHTINGS;
        }
        float delay = zombie.getDefinition().getBehaviorPropFloat(
                "DelayBetweenKnightings", DEFAULT_DELAY_BETWEEN_KNIGHTINGS);
        return delay > 0f ? delay : DEFAULT_DELAY_BETWEEN_KNIGHTINGS;
    }

    private int colRadius(ZombieInstance zombie) {
        int r = zombie.getDefinition().getBehaviorPropInt(
                "KnightingAreaX", DEFAULT_KNIGHTING_AREA_COL_RADIUS);
        return r > 0 ? r : DEFAULT_KNIGHTING_AREA_COL_RADIUS;
    }

    private int rowRadius(ZombieInstance zombie) {
        int r = zombie.getDefinition().getBehaviorPropInt(
                "KnightingAreaY", DEFAULT_KNIGHTING_AREA_ROW_RADIUS);
        return r > 0 ? r : DEFAULT_KNIGHTING_AREA_ROW_RADIUS;
    }

    private ZombieInstance findKnightableZombie(ZombieInstance king, BehaviorContext context) {
        List<ZombieInstance> nearby = context.getZombiesInArea(
                king.getGridY(), king.getGridX(), rowRadius(king), colRadius(king));
        for (ZombieInstance candidate : nearby) {
            if (candidate != king && isKnightable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** Equips Crown helm and Shoulder Armor so the peasant draws as a Knight. */
    private void knight(ZombieInstance target) {
        Armor crown = knightPiece(ArmorType.Crown);
        Armor shoulder = knightPiece(ArmorType.ShoulderArmor);
        if (crown != null) {
            target.addArmor(crown);
        }
        if (shoulder != null) {
            target.addArmor(shoulder);
        }
    }

    /**
     * Factory when the game has loaded armor JSON; otherwise a typed piece so
     * tests still attach Crown / Shoulder without {@link ZombieFactory#init}.
     */
    private static Armor knightPiece(ArmorType type) {
        Armor fromFactory = ZombieFactory.createArmor(type);
        if (fromFactory != null) {
            return fromFactory;
        }
        if (type == ArmorType.Crown) {
            Armor crown = new Armor(ArmorType.Crown, 800, true, true, true, false);
            crown.setDamageLayers(List.of(
                    "zombie_armor_crown_norm",
                    "zombie_armor_crown_damage_01",
                    "zombie_armor_crown_damage_02"));
            crown.setLayerThresholds(List.of(0.666f, 0.333f));
            return crown;
        }
        if (type == ArmorType.ShoulderArmor) {
            Armor shoulder = new Armor(ArmorType.ShoulderArmor, 800, false, false, false, true);
            shoulder.setDamageLayers(List.of(
                    "zombie_shoulder_armor_norm",
                    "zombie_shoulder_armor_damage_01",
                    "zombie_shoulder_armor_damage_02"));
            shoulder.setLayerThresholds(List.of(0.666f, 0.333f));
            return shoulder;
        }
        return null;
    }

    private boolean isKnightable(ZombieInstance candidate) {
        if (candidate == null || candidate.isDead()) {
            return false;
        }
        String name = candidate.getDefinition().getName();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        boolean isDarkBasic = lower.equals("zombiedefault")
                || lower.equals("zombiearmor1")
                || lower.equals("zombiearmor2")
                || lower.equals("zombiearmor4")
                || lower.equals("zombiedarkarmor3")
                || lower.startsWith("dark");
        if (!isDarkBasic) {
            return false;
        }
        List<Armor> armors = candidate.getArmors();
        if (armors == null || armors.isEmpty()) {
            return true;
        }
        for (Armor armor : armors) {
            if (armor != null && armor.getType() == ArmorType.Crown) {
                return false;
            }
        }
        return true;
    }

    private void enterIdle() {
        phase = KingPhase.IDLE;
        pickIdle();
    }

    private void pickIdle() {
        idle2 = ThreadLocalRandom.current().nextFloat() < IDLE2_WEIGHT;
        phaseTimer = 0f;
    }

    private float idleDuration() {
        return idle2 ? IDLE2_DURATION : IDLE_DURATION;
    }

    /** @return true if this zombie is a Dark King zombie. */
    public boolean isDarkKing(ZombieInstance zombie) {
        if (zombie == null || zombie.getDefinition() == null) {
            return false;
        }
        String name = zombie.getDefinition().getName();
        if (name == null) {
            return false;
        }
        return name.toLowerCase().contains("king");
    }

    public KingPhase getPhase() {
        return phase;
    }

    public float getPhaseTimer() {
        return phaseTimer;
    }

    public float getKnightTimer() {
        return knightTimer;
    }

    public boolean isIdle2() {
        return idle2;
    }

    public void setKnightTimer(float knightTimer) {
        this.knightTimer = knightTimer;
    }

    public void setPhase(KingPhase phase) {
        this.phase = phase != null ? phase : KingPhase.IDLE;
    }

    public void setPhaseTimer(float phaseTimer) {
        this.phaseTimer = phaseTimer;
    }

    public void setIdle2(boolean idle2) {
        this.idle2 = idle2;
    }
}
