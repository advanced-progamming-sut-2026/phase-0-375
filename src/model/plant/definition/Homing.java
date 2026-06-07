package model.plant.definition;

import model.plant.ability.HomingAbility;

public class Homing extends Plant{
    private HomingAbility homingAbility;

    public Homing(String name, int cost, int baseHP, int damage, float rechargeTime, HomingAbility homingAbility) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.homingAbility = homingAbility;
    }

    public HomingAbility getHomingAbility() {
        return homingAbility;
    }

    public void setHomingAbility(HomingAbility homingAbility) {
        this.homingAbility = homingAbility;
    }
}
