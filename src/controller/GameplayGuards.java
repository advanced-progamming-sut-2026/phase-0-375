package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.GameState;
import model.enums.GroundType;
import model.enums.MenuType;
import model.enums.PlacableLayer;
import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.enums.WaveManagerPhase;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.enums.BowlingWalnutType;
import model.game.level.special.ConveyorBeltLevel;
import model.game.level.special.ScoreLevel;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.vasebreaker.PendingSeedPacket;
import model.game.level.minigame.vasebreaker.Vase;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.map.Cell;
import model.game.map.GameMap;
import model.game.map.Point;
import model.game.wave.WaveManager;
import model.item.Grave;
import model.item.Sun;
import model.item.placeable.Placeable;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.user.User;
import model.zombie.ZombieFactory;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.minigame.beghouled.BeghouledLevel;

/**
 * Shared guard clauses and small helpers used by every gameplay command
 * service.
 */
final class GameplayGuards {

    private GameplayGuards() {}

    static GameModel requireGame() {
        return App.getInstance().getCurrentGameModel();
    }

    static PvZGameLoop requireLoop() {
        return App.getInstance().getCurrentGameLoop();
    }

    static CommandResult<Void> guardGameRunning() {
        GameModel model = requireGame();
        if (model == null) {
            return CommandResult.error("No active game. Start a level from the game menu first.");
        }
        if (model.getState() == GameState.WON) {
            return CommandResult.error("Level already won. Use 'menu exit' to return.");
        }
        if (model.getState() == GameState.LOST) {
            return CommandResult.error("Level already lost. Use 'menu exit' to return.");
        }
        return null;
    }

    static boolean inBounds(GameMap map, int x, int y) {
        return x >= 0 && y >= 0
                && x < map.getCols()
                && y < map.getRows();
    }

    /**
     * Returns the {@link PlantInstance} placed on the MAIN layer of the
     * given cell, or {@code null} if the cell is empty.
     */
    static PlantInstance plantAt(Cell cell) {
        if (cell == null) return null;
        var p = cell.getPlaceable(PlacableLayer.MAIN);
        return (p instanceof PlantInstance) ? (PlantInstance) p : null;
    }

    @SuppressWarnings("unchecked")
    static <T> CommandResult<T> errorTyped(String message) {
        return (CommandResult<T>) CommandResult.error(message);
    }

    @SuppressWarnings("unchecked")
    static <T> CommandResult<T> retypeError(CommandResult<Void> source) {
        return (CommandResult<T>) source;
    }
}
