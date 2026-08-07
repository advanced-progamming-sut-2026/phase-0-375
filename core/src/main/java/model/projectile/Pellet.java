package model.projectile;

import model.game.map.FloatPoint;

/**
 * A standard straight-line pea (or similar) projectile fired by shooter
 * plants such as Peashooter.
 */
public class Pellet extends Projectile {

    public Pellet(int damage, FloatPoint position, int row, float velocity) {
        super(damage, position, row, velocity);
    }

    public Pellet(int damage, FloatPoint position, int row, float velocity,
                  Element element, int direction) {
        super(damage, position, row, velocity, element, direction);
    }
}