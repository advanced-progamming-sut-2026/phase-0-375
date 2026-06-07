package model.plant.definition;

import model.plant.ability.MeleeAbility;

public class Melee extends Plant{
    private MeleeAbility meleeAbility;

    public Melee(String name, int cost, int baseHP, int damage, float rechargeTime, MeleeAbility meleeAbility) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.meleeAbility = meleeAbility;
    }

    public MeleeAbility getMeleeAbility() {
        return meleeAbility;
    }

    public void setMeleeAbility(MeleeAbility meleeAbility) {
        this.meleeAbility = meleeAbility;
    }
}
