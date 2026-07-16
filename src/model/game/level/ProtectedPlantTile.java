package model.game.level;

import model.game.map.Point;

/**
 * Level-data entry for Save Our Seeds: which plant sits on which tile.
 *
 * <p>This is pure configuration (a position and a plant name). The runtime
 * wrapper around the actual pre-placed plant is
 * {@link model.plant.instance.ProtectedPlant}.
 *
 * @see model.game.level.special.SaveOurSeedsLevel
 */
public class ProtectedPlantTile {

    private final Point position;

    /** Plant name from the level data; null = use the level default. */
    private final String plantName;

    public ProtectedPlantTile(Point position, String plantName) {
        this.position = position;
        this.plantName = plantName;
    }

    public Point getPosition() {
        return position;
    }

    public String getPlantName() {
        return plantName;
    }
}
