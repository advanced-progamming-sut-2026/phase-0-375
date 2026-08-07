package model.game.level;

import model.game.level.special.*;

public class LevelFactory {
    private LevelFactory() {}

    public static Level create(LevelConfig config) {
        if (config == null || config.getLevelType() == null) return null;
        switch (config.getLevelType()) {
            case CONVEYOR_BELT: return new ConveyorBeltLevel(config);
            case LOCKED_PLANTS: return new LockedPlantsLevel(config);
            case SAVE_OUR_SEEDS: return new SaveOurSeedsLevel(config);
            case TIMED_WAR: return new TimedWarLevel(config);
            case NIGHT_OPS: return new NightOpsLevel(config);
            case DEAD_LINE: return new DeadLineLevel(config);
            case LOVE_YOUR_PLANTS: return new LoveYourPlantsLevel(config);
            case PLANT_WHAT_YOU_GET: return new PlantWhatYouGetLevel(config);
            case NORMAL:
            default: return new RegularLevel(config);
        }
    }
}
