package model.zombie.behavior;

import model.enums.ArmorType;
import model.enums.ZombieBehaviorType;
import model.zombie.ZombieFactory;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Buff behavior.
 */
public class BuffBehavior implements ZombieBehavior {

    // --- Dark King constants ---

    /** Default seconds between two consecutive knighting actions. */
    public static final float DEFAULT_DELAY_BETWEEN_KNIGHTINGS = 2.5f;

    /** Default columns to either side of the King that fall within its knighting area. */
    public static final int DEFAULT_KNIGHTING_AREA_COL_RADIUS = 4;

    /** Default rows above/below the King that fall within its knighting area. */
    public static final int DEFAULT_KNIGHTING_AREA_ROW_RADIUS = 3;

    /** Definition name of the plain zombie that is eligible to be knighted. */
    public static final String PLAIN_ZOMBIE_NAME = "ZombieDefault";

    // --- State ---

    /** Seconds elapsed since the last knighting action. */
    private float knightTimer = 0f;

    // --- ZombieBehavior ---

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

    // --- Dark King ---

    /**
     * Periodically knights one eligible zombie within the King's
     * knighting area.
     */
    private void tickDarkKing(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        float delay = zombie.getDefinition().getBehaviorPropFloat(
                "DelayBetweenKnightings", DEFAULT_DELAY_BETWEEN_KNIGHTINGS);
        if (delay <= 0f) delay = DEFAULT_DELAY_BETWEEN_KNIGHTINGS;

        int colRadius = zombie.getDefinition().getBehaviorPropInt(
                "KnightingAreaX", DEFAULT_KNIGHTING_AREA_COL_RADIUS);
        int rowRadius = zombie.getDefinition().getBehaviorPropInt(
                "KnightingAreaY", DEFAULT_KNIGHTING_AREA_ROW_RADIUS);
        if (colRadius <= 0) colRadius = DEFAULT_KNIGHTING_AREA_COL_RADIUS;
        if (rowRadius <= 0) rowRadius = DEFAULT_KNIGHTING_AREA_ROW_RADIUS;

        knightTimer += deltaTime;
        if (knightTimer < delay) {
            return;
        }
        knightTimer -= delay;

        ZombieInstance target = findKnightableZombie(zombie, context, rowRadius, colRadius);
        if (target == null) {
            return;
        }

        knight(target);
    }

    /**
     * Searches the King's knighting area for a plain zombie that hasn't
     * already been knighted.
     */
    private ZombieInstance findKnightableZombie(ZombieInstance king,
                                                BehaviorContext context,
                                                int rowRadius, int colRadius) {
        int row = king.getGridY();
        int col = king.getGridX();

        List<ZombieInstance> nearby = context.getZombiesInArea(
                row, col, rowRadius, colRadius
        );

        for (ZombieInstance candidate : nearby) {
            if (candidate == king) {
                continue;
            }
            if (isKnightable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Equips the target zombie with a Crown helm and Shoulder Armor,
     * transforming it into a Knight.
     */
    private void knight(ZombieInstance target) {
        Armor crown = ZombieFactory.createArmor(ArmorType.Crown);
        Armor shoulderArmor = ZombieFactory.createArmor(ArmorType.ShoulderArmor);

        if (crown != null) {
            target.addArmor(crown);
        }
        if (shoulderArmor != null) {
            target.addArmor(shoulderArmor);
        }
    }

    // --- Eligibility checks ---

    /**
     * @return true if {@code candidate} is a plain zombie with no armor yet.
     */
    private boolean isKnightable(ZombieInstance candidate) {
        if (candidate == null || candidate.isDead()) {
            return false;
        }
        String name = candidate.getDefinition().getName();
        if (name == null) return false;
        String lower = name.toLowerCase();
        boolean isDarkBasic = lower.equals("zombiedefault")
                || lower.equals("zombiearmor1")
                || lower.equals("zombiearmor2")
                || lower.equals("zombiearmor4")
                || lower.equals("zombiedarkarmor3")
                || lower.startsWith("dark");
        if (!isDarkBasic) return false;
        List<Armor> armors = candidate.getArmors();
        return armors == null || armors.isEmpty();
    }

    // --- Zombie identification helpers ---

    /** @return true if this zombie is a Dark King zombie. */
    public boolean isDarkKing(ZombieInstance zombie) {
        String name = zombie.getDefinition().getName();
        if (name == null) return false;
        return name.toLowerCase().contains("king");
    }

    // --- Getters ---

    public float getKnightTimer() {
        return knightTimer;
    }

    // --- Setters ---

    public void setKnightTimer(float knightTimer) {
        this.knightTimer = knightTimer;
    }
}