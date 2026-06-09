package model.zombie;

import model.zombie.armor.Armor;

import java.util.List;

public class BasicZombie extends Zombie{
    private List<Armor> armors;

    public BasicZombie(String name, int baseHP, float speed, float eatDPS) {
        super(name, baseHP, speed, eatDPS);
    }

    public List<Armor> getArmors() {
        return armors;
    }

    public void setArmors(List<Armor> armors) {
        this.armors = armors;
    }

    public void addArmor(Armor armor){ }
}
