package model.plant.definition;

import model.plant.ability.LobberAbility;

public class Lobber extends Plant{
    private LobberAbility lobberAbility;

    public Lobber(String name, int cost, int baseHP, int damage, float rechargeTime, LobberAbility lobberAbility) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.lobberAbility = lobberAbility;
    }

    public LobberAbility getLobberAbility() {
        return lobberAbility;
    }

    public void setLobberAbility(LobberAbility lobberAbility) {
        this.lobberAbility = lobberAbility;
    }
}
