package model.plant.definition;

import model.plant.ability.MintAbility;

public class Mint extends Plant{
    private MintAbility mintAbility;

    public Mint(String name, int cost, int baseHP, int damage, float rechargeTime, MintAbility mintAbility) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.mintAbility = mintAbility;
    }

    public MintAbility getMintAbility() {
        return mintAbility;
    }

    public void setMintAbility(MintAbility mintAbility) {
        this.mintAbility = mintAbility;
    }
}
