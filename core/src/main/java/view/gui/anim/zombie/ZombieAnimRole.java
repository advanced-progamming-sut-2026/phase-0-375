package view.gui.anim.zombie;

/**
 * Zombie-only view clip roles. Owned by the zombie team.
 *
 * <p>Not a copy of {@link model.enums.ZombieState} — map simulation state → role in
 * {@link ZombieAnimAdapter}. Add zombie-only roles here without touching plant code.
 */
public enum ZombieAnimRole {
    IDLE,
    EATING,
    WALK,

    // TODO: DIE — death clips
    // TODO: SPECIAL — smash / push / item / rage poses
}
