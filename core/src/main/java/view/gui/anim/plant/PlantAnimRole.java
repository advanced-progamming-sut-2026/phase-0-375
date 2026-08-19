package view.gui.anim.plant;

/**
 * Plant-only view clip roles. Owned by the plant team.
 *
 * <p>Not a copy of {@link model.enums.PlantState} — map simulation state → role in
 * {@link PlantAnimAdapter}. Add plant-only roles here without touching zombie code.
 */
public enum PlantAnimRole {
    IDLE(true),
    PLANT_FOOD_ON(false),
    PLANT_FOOD(true),
    PLANT_FOOD_OFF(false),
    ATTACK(false),
    SPECIAL(false)
    ;

    private final boolean looping;

    PlantAnimRole(boolean looping) {
        this.looping = looping;
    }

    public boolean isLooping() {
        return looping;
    }
}
