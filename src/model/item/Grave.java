package model.item;

import model.app.App;
import model.enums.PlacableLayer;
import model.game.core.GameModel;
import model.item.placeable.Placeable;

/**
 * A gravestone dropped onto the field.
 *
 * <p>Some graves carry loot (Dark Ages): 50 sun or one plant food, paid out
 * when the grave is destroyed.</p>
 */
public class Grave extends GridItem implements Placeable {

    /** Default starting HP for a freshly-spawned grave. */
    public static final int DEFAULT_HP = 700;

    /** Loot dropped when the grave is destroyed. */
    public enum GraveType {
        PLAIN,        // no loot
        SUN,          // drops 50 sun
        PLANT_FOOD    // drops one plant food
    }

    private GraveType type = GraveType.PLAIN;

    public Grave() {
        this(DEFAULT_HP, GraveType.PLAIN);
    }

    public Grave(int hp) {
        this(hp, GraveType.PLAIN);
    }

    public Grave(int hp, GraveType type) {
        super(hp);
        this.type = type == null ? GraveType.PLAIN : type;
    }

    public GraveType getType() {
        return type;
    }

    public void setType(GraveType type) {
        this.type = type == null ? GraveType.PLAIN : type;
    }

    /** Spawns the grave's loot. Called once when the grave's HP hits zero. */
    public void applyLoot(GameModel model) {
        if (model == null) return;
        switch (type) {
            case SUN:
                model.addSun(50);
                App.logToShell("[Grave] A grave crumbled and dropped 50 sun!");
                break;
            case PLANT_FOOD:
                model.addPlantFood();
                App.logToShell("[Grave] A grave crumbled and dropped plant food!");
                break;
            default:
                break;
        }
    }

    @Override
    public boolean killsOnContact() {
        return false;
    }

    @Override
    public boolean blocksProjectiles() {
        return true;
    }

    @Override
    public PlacableLayer getLayer() {
        return PlacableLayer.GROUND;
    }
}
