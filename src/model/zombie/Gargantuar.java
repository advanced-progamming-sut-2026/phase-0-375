package model.zombie;

public class Gargantuar extends Zombie {
    private Imp imp;

    public Gargantuar(String name, int baseHP, float speed, float eatDPS) {
        super(name, baseHP, speed, eatDPS);
    }

    public Imp getImp() {
        return imp;
    }

    public void setImp(Imp imp) {
        this.imp = imp;
    }

    public void throwImp() {}
}
