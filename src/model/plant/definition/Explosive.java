package model.plant.definition;

import model.plant.ability.ExplosiveAbility;

public class Explosive extends Plant{
    private ExplosiveAbility explosiveAbility;

    public Explosive(String name, int cost, int baseHP, int damage, float rechargeTime, ExplosiveAbility explosiveAbility) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.explosiveAbility = explosiveAbility;
    }

    public ExplosiveAbility getExplosiveAbility() {
        return explosiveAbility;
    }

    public void setExplosiveAbility(ExplosiveAbility explosiveAbility) {
        this.explosiveAbility = explosiveAbility;
    }
}
