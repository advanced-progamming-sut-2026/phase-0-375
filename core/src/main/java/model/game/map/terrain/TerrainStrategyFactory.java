package model.game.map.terrain;

import model.enums.GroundType;
import model.enums.SlideDirection;

/**
 * Factory that maps a {@link GroundType} to a fresh {@link TerrainStrategy}
 * instance.
 */
public final class TerrainStrategyFactory {

    private TerrainStrategyFactory() {

    }

    /** Returns a fresh strategy appropriate for the given ground type. */
    public static TerrainStrategy create(GroundType groundType) {
        if (groundType == null) {
            return new NormalTerrainStrategy();
        }
        switch (groundType) {
            case NORMAL:
                return new NormalTerrainStrategy();
            case WATER:
            case LOW_TIDE:
                return new WaterTerrainStrategy();
            case ICE:
                return new IceTerrainStrategy();
            case SLIDE_UP:
                return new SlideTerrainStrategy(SlideDirection.UP);
            case SLIDE_DOWN:
                return new SlideTerrainStrategy(SlideDirection.DOWN);
            case NECROMANCY:
                // Necromancy spawn is wave-driven; the tile itself behaves
                // like ordinary ground for planting / movement purposes.
                return new NormalTerrainStrategy();
            case CRATER:
                return new CraterTerrainStrategy();
            case FIRE:
                return new FireTerrainStrategy();
            default:
                return new NormalTerrainStrategy();
        }
    }
}
