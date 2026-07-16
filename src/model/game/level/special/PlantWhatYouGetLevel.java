package model.game.level.special;

import model.game.level.LevelConfig;
import model.game.level.RegularLevel;

/**
 * Plant What You Get: no sun falls from the sky and sun-producing plants are
 * banned, so the player must survive on the level's initial sun budget.
 *
 * <p>The rule defaults are forced by {@code LevelRegistry} for this level
 * type ({@code sunFallsFromSky = false}, {@code sunProducingPlantsAllowed =
 * false}, {@code plantRechargeInSetup = false}):
 *
 * <ul>
 *   <li>No sky sun is enforced by {@code PvZGameLoop}, which only enables
 *       the sun-fall system when {@code sunFallsFromSky} is true.</li>
 *   <li>The sun-producer ban is enforced by
 *       {@code PlantSelectionMenuController}, which refuses plants whose
 *       category is {@code SUN_PRODUCER} when the rule disallows them.</li>
 *   <li>{@code plantRechargeInSetup} has no consumer yet: the codebase has
 *       no seed-slot recharge system (only per-placed-plant ability
 *       cooldowns), so there is deliberately nothing to enforce here.</li>
 * </ul>
 *
 * <p>Win/loss rules are the regular ones.
 */
public class PlantWhatYouGetLevel extends RegularLevel {

    public PlantWhatYouGetLevel(LevelConfig config) {
        super(config);
    }

    @Override
    public boolean canStart() {
        if (!super.canStart()) {
            return false;
        }
        // With no sky sun and no sun producers, the initial sun budget is
        // the only income; a level without one is unplayable.
        return getConfig().getRules().getInitialSun() > 0;
    }
}
