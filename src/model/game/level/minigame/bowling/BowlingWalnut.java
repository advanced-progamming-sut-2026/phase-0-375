package model.game.level.minigame.bowling;

import model.enums.BowlingWalnutType;
import model.projectile.Projectile;

/**
 * A rolling walnut projectile used in the Wallnut Bowling mini-game.
 */
public class BowlingWalnut extends Projectile {
    private BowlingWalnutType type;

    public BowlingWalnutType getType() {
        return type;
    }

    public void setType(BowlingWalnutType type) {
        this.type = type;
    }
}
