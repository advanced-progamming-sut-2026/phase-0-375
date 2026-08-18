package model.zombie.behavior;

import model.enums.ArmorType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Enrage behavior.
 */
public class EnrageBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Default movement-speed multiplier applied permanently once enraged. */
    public static final float DEFAULT_ENRAGED_SPEED_SCALE = 2.0f;

    /**
     * Default eat-damage multiplier applied permanently once enraged.
     * CombatSystem multiplies each bite by this factor.
     */
    public static final float DEFAULT_ENRAGED_EAT_SCALE = 2.0f;

    /** {@code newspaper_defeat} clip length on {@code ZOMBIE_MODERN_NEWSPAPER}. */
    public static final float NEWSPAPER_DEFEAT_DURATION = 1.4f;

    // --- State ---

    /** True once the Newspaper has been destroyed and the zombie enraged. */
    private boolean enraged = false;

    /** True while {@code newspaper_defeat} plays (paper gone, speed not yet applied). */
    private boolean defeating = false;

    /** Seconds elapsed in {@link #NEWSPAPER_DEFEAT_DURATION}. */
    private float defeatTimer = 0f;

    /** True after this zombie was seen carrying an intact Newspaper. */
    private boolean hadNewspaper = false;

    /** Speed scale read from the zombie definition (cached on first tick). */
    private float cachedSpeedScale = 0f;
    /** Eat-damage scale read from the zombie definition (cached on first tick). */
    private float cachedEatScale = 0f;
    /** True once the scales have been read from the definition. */
    private boolean scalesLoaded = false;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }
        if (enraged) {
            return;
        }
        if (defeating) {
            tickDefeat(zombie, deltaTime);
            return;
        }
        if (hasNewspaper(zombie)) {
            hadNewspaper = true;
            return;
        }
        if (hadNewspaper) {
            beginDefeat(zombie);
            return;
        }
        enrage(zombie);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.ENRAGE;
    }

    // --- Core logic ---

    /** Returns true if the zombie still carries its Newspaper armor piece. */
    private boolean hasNewspaper(ZombieInstance zombie) {
        List<Armor> armors = zombie.getArmors();
        if (armors == null || armors.isEmpty()) {
            return false;
        }
        for (Armor armor : armors) {
            if (armor != null && armor.getType() == ArmorType.Newspaper && !armor.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    private void beginDefeat(ZombieInstance zombie) {
        defeating = true;
        defeatTimer = 0f;
        zombie.stopEating();
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    private void tickDefeat(ZombieInstance zombie, float deltaTime) {
        defeatTimer += deltaTime;
        if (defeatTimer < NEWSPAPER_DEFEAT_DURATION) {
            return;
        }
        defeating = false;
        if (zombie.getState() == ZombieState.DYING || zombie.getState() == ZombieState.DEAD) {
            return;
        }
        enrage(zombie);
        zombie.setState(ZombieState.WALKING);
    }

    /** Flips the zombie into the enraged state and applies the speed multiplier. */
    private void enrage(ZombieInstance zombie) {
        enraged = true;
        defeating = false;
        loadScales(zombie);
        zombie.applySpeedModifier(cachedSpeedScale);
    }

    /** Reads the speed / eat-damage scales from the zombie definition. */
    private void loadScales(ZombieInstance zombie) {
        if (scalesLoaded) return;
        cachedSpeedScale = zombie.getDefinition().getBehaviorPropFloat(
                "EnragedSpeedScale", DEFAULT_ENRAGED_SPEED_SCALE);
        cachedEatScale = zombie.getDefinition().getBehaviorPropFloat(
                "EnragedDamageScale", DEFAULT_ENRAGED_EAT_SCALE);
        if (cachedSpeedScale <= 0f) cachedSpeedScale = DEFAULT_ENRAGED_SPEED_SCALE;
        if (cachedEatScale <= 0f) cachedEatScale = DEFAULT_ENRAGED_EAT_SCALE;
        scalesLoaded = true;
    }

    // --- Getters / setters ---

    /** @return true once the zombie has enraged (Newspaper destroyed). */
    public boolean isEnraged() {
        return enraged;
    }

    /** @return true while {@code newspaper_defeat} should play. */
    public boolean isDefeating() {
        return defeating;
    }

    /** @return seconds into {@code newspaper_defeat}; 0 on the first gasp frame. */
    public float getDefeatTimer() {
        return defeatTimer;
    }

    /**
     * @return the eat-damage multiplier that CombatSystem should apply to
     *         this zombie's bites. {@code 1.0f} before enraging,
     *         the configured enraged eat scale afterward.
     */
    public float getEatDamageScale() {
        if (!enraged) return 1.0f;
        if (!scalesLoaded) return DEFAULT_ENRAGED_EAT_SCALE;
        return cachedEatScale;
    }

    public void setEnraged(boolean enraged) {
        this.enraged = enraged;
        if (enraged) {
            defeating = false;
        }
    }
}
