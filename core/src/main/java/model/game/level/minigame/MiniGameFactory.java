package model.game.level.minigame;

import model.enums.MiniGameType;
import model.game.level.LevelConfig;
import model.game.level.minigame.beghouled.BeghouledLevel;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.level.minigame.zombotany.ZombotanyLevel;

/**
 * Creates the MiniGameLevel implementation matching a MiniGameType.
 */
public final class MiniGameFactory {

    private MiniGameFactory() {
    }

    public static MiniGameLevel create(MiniGameType type, LevelConfig config, int difficultyTier) {
        if (type == null || config == null) {
            return null;
        }
        return switch (type) {
            case VASE_BREAKER -> new VaseBreakerLevel(config, type, difficultyTier);
            case WALLNUT_BOWLING -> new WallnutBowlingLevel(config, type, difficultyTier);
            case ZOMBOTANY -> new ZombotanyLevel(config, type, difficultyTier);
            case BEGHOULED -> new BeghouledLevel(config, type, difficultyTier);
            case I_ZOMBIE -> new IZombieLevel(config, type, difficultyTier);
        };
    }
}
