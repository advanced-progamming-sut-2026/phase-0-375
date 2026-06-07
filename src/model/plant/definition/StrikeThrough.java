package model.plant.definition;

import model.plant.ability.StrikeThroughAbility;

public class StrikeThrough extends Plant{
    private StrikeThroughAbility strikerThrough;

    public StrikeThrough(String name, int cost, int baseHP, int damage, float rechargeTime, StrikeThroughAbility strikerThrough) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.strikerThrough = strikerThrough;
    }

    public StrikeThroughAbility getStrikerThrough() {
        return strikerThrough;
    }

    public void setStrikerThrough(StrikeThroughAbility strikerThrough) {
        this.strikerThrough = strikerThrough;
    }
}
