package model.event;

public class GameEvent {
    public enum Type {
        SUN_COLLECTED,
        SUN_DROPPED,
        SUN_PRODUCED,
        ZOMBIE_SPAWNED,
        ZOMBIE_KILLED,
        ZOMBIE_REACHED_END,
        PLANT_PLACED,
        PLANT_DESTROYED,
        PLANT_FOOD_ACTIVATED,
        PROJECTILE_FIRED,
        PROJECTILE_HIT,
        WAVE_STARTED,
        FINAL_WAVE,
        LAWN_MOWER_TRIGGERED,
        GAME_WON,
        GAME_LOST,
        LOOT_DROPPED,
        ARMOR_DESTROYED,
        STATUS_APPLIED,
        STATUS_EXPIRED,
        PUSHABLE_DESTROYED,
        EQUIPPABLE_DESTROYED,
        ZOMBIE_TRANSFORMED,
        ZOMBIE_BUFFED,
        IMP_THROWN,
        GRAVE_SPAWNED,
        NECROMANCY_SPAWN
    }

    private Type type;

    public GameEvent(model.event.GameEvent.Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}