package model.zombie.behavior;

import model.enums.ArmorType;
import model.enums.ZombieBehaviorType;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Enrage behavior.
 */
public class EnrageBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Default movement-speed multiplier applied permanently once enraged. */
    public static final float DEFAULT_ENRAGED_SPEED_SCALE = 3.0f;

    /**
     * Default eat-damage multiplier applied permanently once enraged.
     * CombatSystem multiplies each bite by this factor.
     */
    public static final float DEFAULT_ENRAGED_EAT_SCALE = 3.0f;

    // --- State ---

    /** True once the Newspaper has been destroyed and the zombie enraged. */
    private boolean enraged = false;

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
            return; // permanent transition - nothing else to do
        }

        if (!hasNewspaper(zombie)) {
            enrage(zombie);
        }
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

    /** Flips the zombie into the enraged state and applies the speed multiplier. */
    private void enrage(ZombieInstance zombie) {
        enraged = true;
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
    }
}
