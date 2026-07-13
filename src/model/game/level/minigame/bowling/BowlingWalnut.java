package model.game.level.minigame.bowling;

import model.enums.BowlingWalnutType;
import model.game.map.FloatPoint;
import model.projectile.Projectile;

/**
 * A rolling walnut projectile used in the Wallnut Bowling mini-game.
 */
public class BowlingWalnut extends Projectile {
    private BowlingWalnutType type;

    public BowlingWalnut(int damage, FloatPoint position, int row, float velocity) {
        super(damage, position, row, velocity);
    }

    public BowlingWalnut(int damage, FloatPoint position, int row, float velocity,
                         Element element, int direction) {
        super(damage, position, row, velocity, element, direction);
    }

    public BowlingWalnutType getType() {
        return type;
    }

    public void setType(BowlingWalnutType type) {
        this.type = type;
    }
}
