package model.plant.definition;

import model.plant.ability.ModifierAbility;

public class Modifier extends Plant{
    private ModifierAbility modifierAbility;

    public Modifier(String name, int cost, int baseHP, int damage, float rechargeTime, ModifierAbility modifierAbility) {
        super(name, cost, baseHP, damage, rechargeTime);
        this.modifierAbility = modifierAbility;
    }

    public ModifierAbility getModifierAbility() {
        return modifierAbility;
    }

    public void setModifierAbility(ModifierAbility modifierAbility) {
        this.modifierAbility = modifierAbility;
    }
}
