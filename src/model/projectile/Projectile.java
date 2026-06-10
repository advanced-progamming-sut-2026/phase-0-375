package model.projectile;

import model.game.map.Point;

public abstract class Projectile {
    protected int damage;
    protected Point currentPosition;
    protected float velocity;
}
