package model.plant.definition;

import model.plant.ability.WallnutAbility;

public class Wallnut extends Plant{
    private WallnutAbility wallnutAbility;

    public Wallnut(String name, int cost, int baseHP, int damage, float rechargeTime, WallnutAbility wallnutAbility) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.wallnutAbility = wallnutAbility;
    }

    public WallnutAbility getWallnutAbility() {
        return wallnutAbility;
    }

    public void setWallnutAbility(WallnutAbility wallnutAbility) {
        this.wallnutAbility = wallnutAbility;
    }
}
