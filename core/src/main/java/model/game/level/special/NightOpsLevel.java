package model.game.level.special;

import model.game.level.LevelConfig;
import model.game.level.RegularLevel;

/**
 * Night Ops: a regular level played at night, so no sun falls from the sky.
 *
 * <p>The night rule itself lives in the level data: {@code LevelRegistry}
 * forces {@code sunFallsFromSky = false} for NIGHT_OPS levels, and the game
 * loop disables the {@code SunFallSystem} accordingly. Everything else
 * (initial graves, waves, win/loss, progression) behaves exactly like a
 * regular level, so this class inherits it.
 */
public class NightOpsLevel extends RegularLevel {

    public NightOpsLevel(LevelConfig config) {
        super(config);
    }
}
