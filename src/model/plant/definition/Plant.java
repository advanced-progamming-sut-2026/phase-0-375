package model.plant.definition;

public abstract class Plant {
    private String name;
    private int cost;
    private int baseHP;
    private int damage;
    private float rechargeTime;

    public Plant(String name, int cost, int baseHP, int damage, float rechargeTime) {
        this.name = name;
        this.cost = cost;
        this.baseHP = baseHP;
        this.damage = damage;
        this.rechargeTime = rechargeTime;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public int getBaseHP() {
        return baseHP;
    }

    public int getDamage() {
        return damage;
    }

    public float getRechargeTime() {
        return rechargeTime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setBaseHP(int baseHP) {
        this.baseHP = baseHP;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void setRechargeTime(float rechargeTime) {
        this.rechargeTime = rechargeTime;
    }
}
