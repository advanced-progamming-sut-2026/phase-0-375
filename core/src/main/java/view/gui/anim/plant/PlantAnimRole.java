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
    ;

    // TODO: ATTACK — firing / melee cycles
    // TODO: GROWING — warm-up / stage idles
    // TODO: ARMED — Potato Mine / trap ready poses
    // TODO: DIE — death clips
    // TODO: SPECIAL — exclusive one-offs

    private final boolean looping;

    PlantAnimRole(boolean looping) {
        this.looping = looping;
    }

    public boolean isLooping() {
        return looping;
    }
}
