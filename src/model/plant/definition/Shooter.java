package model.plant.definition;

import model.plant.ability.ShooterAbility;

public class Shooter extends Plant{
    private ShooterAbility shooterAbility;

    public Shooter(String name, int cost, int baseHP, int damage, float rechargeTime, ShooterAbility shooterAbility) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.shooterAbility = shooterAbility;
    }

    public ShooterAbility getShooterAbility() {
        return shooterAbility;
    }

    public void setShooterAbility(ShooterAbility shooterAbility) {
        this.shooterAbility = shooterAbility;
    }
}
