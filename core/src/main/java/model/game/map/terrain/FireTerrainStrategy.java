package model.game.map.terrain;

import model.enums.GroundType;
import model.item.placeable.Placeable;
import model.plant.definition.Plant;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;
import model.game.map.Cell;

/**
 * Temporary fire tile.
 */
public class FireTerrainStrategy implements TerrainStrategy {

    /** Default burn duration from the phase-2 Zomboss fireball impact. */
    public static final float DEFAULT_DURATION_SECONDS = 4f;

    private float remainingSeconds;

    public FireTerrainStrategy() {
        this(DEFAULT_DURATION_SECONDS);
    }

    public FireTerrainStrategy(float durationSeconds) {
        this.remainingSeconds = Math.max(0f, durationSeconds);
    }

    @Override
    public boolean canPlant(Plant plant, Cell cell) {
        return remainingSeconds <= 0f;
    }

    @Override
    public boolean isPassable(ZombieInstance zombie, Cell cell) {
        return true;
    }

    @Override
    public void onZombieEnter(ZombieInstance zombie, Cell cell, BehaviorContext context) {
        // Fire only blocks planting; zombies walk through.
    }

    @Override
    public void onTick(Cell cell, Placeable model, BehaviorContext context, float deltaTime) {
        if (remainingSeconds <= 0f || deltaTime <= 0f || cell == null) {
            return;
        }
        remainingSeconds -= deltaTime;
        if (remainingSeconds <= 0f) {
            remainingSeconds = 0f;
            cell.setGroundType(GroundType.NORMAL);
            cell.setTerrainStrategy(new NormalTerrainStrategy());
        }
    }

    public float getRemainingSeconds() {
        return remainingSeconds;
    }

    public boolean isExtinguished() {
        return remainingSeconds <= 0f;
    }
}
