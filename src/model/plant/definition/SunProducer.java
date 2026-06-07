package model.plant.definition;

import model.plant.ability.SunProducerAbility ;

public class SunProducer extends Plant{
    private SunProducerAbility sunProducerAbility;

    public SunProducer(String name, int cost, int baseHP, int damage, float rechargeTime, SunProducerAbility sunProducerAbility) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.sunProducerAbility = sunProducerAbility;
    }


    public SunProducerAbility getSunProducerAbility() {
        return sunProducerAbility;
    }

    public void setSunProducerAbility(SunProducerAbility sunProducerAbility) {
        this.sunProducerAbility = sunProducerAbility;
    }
}
