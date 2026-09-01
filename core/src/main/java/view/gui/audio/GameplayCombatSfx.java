package view.gui.audio;

/**
 * Toggle combat SFX while tuning. Flip flags in code, rebuild, and compare in-game.
 */
public final class GameplayCombatSfx {
  /** {@link GameSfx#FIRE_PROJECTILE} on every shot. */
  public static boolean fireProjectileEnabled = true;

  /** {@link GameSfx#ZOMBIE_GOT_SHOT} when a projectile hits a zombie. */
  public static boolean zombieGotShotEnabled = true;

  private GameplayCombatSfx() {}
}
