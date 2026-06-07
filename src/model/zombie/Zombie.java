package model.zombie;

import model.enums.ZombieState;
import model.zombie.behavior.ZombieBehavior;

import java.util.ArrayList;
import java.util.List;

public class Zombie {
    private String name;
    private int baseHP;
    private float speed;
    private float eatDPS;
    private List<ZombieBehavior> behaviors = new  ArrayList<>();

    public Zombie(String name, int baseHP, float speed, float eatDPS) {
        this.name = name;
        this.baseHP = baseHP;
        this.speed = speed;
        this.eatDPS = eatDPS;
    }

    public String getName() {
        return name;
    }

    public int getBaseHP() {
        return baseHP;
    }

    public float getSpeed() {
        return speed;
    }

    public float getEatDPS() {
        return eatDPS;
    }

    public void takeDamage(int damage) {}

    public void addBehavior(ZombieBehavior behavior) {}
}
