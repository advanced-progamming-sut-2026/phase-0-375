package view.gui.screen.gameplay;

import model.app.App;
import model.enums.Chapter;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.minigame.beghouled.BeghouledLevel;
import model.game.level.minigame.beghouled.BeghouledSettings;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.minigame.vasebreaker.PendingSeedPacket;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import view.gui.lawn.LawnLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Level/model lookups shared by gameplay HUD, input, and draw. */
public final class GameplayLevelQueries {
    private GameplayLevelQueries() {}

    public static GameModel model() {
        return App.getInstance().getCurrentGameModel();
    }

    public static Level currentLevel() {
        GameModel gameModel = model();
        return gameModel == null ? null : gameModel.getCurrentLevel();
    }

    public static Chapter currentChapter() {
        Level level = currentLevel();
        return level == null || level.getConfig() == null ? null : level.getConfig().getChapter();
    }

    public static LawnLayout lawnLayout() {
        GameModel gameModel = model();
        int rows = gameModel != null ? gameModel.getMap().getRows() : LawnLayout.DEFAULT_ROWS;
        int cols = gameModel != null ? gameModel.getMap().getCols() : LawnLayout.DEFAULT_COLS;
        return new LawnLayout(rows, cols);
    }

    public static boolean lawnMowersEnabled() {
        Level level = currentLevel();
        return level != null
            && level.getConfig() != null
            && level.getConfig().getRules() != null
            && level.getConfig().getRules().isLawnMowersEnabled();
    }

    public static int deadLineColumn() {
        Level level = currentLevel();
        if (level instanceof IZombieLevel iZombie) {
            return iZombie.redLineColumn();
        }
        if (level == null || level.getConfig() == null) {
            return -1;
        }
        int line = level.getConfig().getDeadLineColumn();
        if (line < 0 && level.getConfig().getRules() != null) {
            line = level.getConfig().getRules().getDeadLineColumn();
        }
        return line;
    }

    public static boolean canBowlAt(int col) {
        Level level = currentLevel();
        if (level instanceof WallnutBowlingLevel bowling) {
            return bowling.canLaunchAtColumn(col);
        }
        return true;
    }

    public static boolean canPlaceIZombieAt(int col) {
        Level level = currentLevel();
        if (level instanceof IZombieLevel iZombie) {
            return col >= iZombie.redLineColumn();
        }
        return true;
    }

    public static boolean canPlaceMultiplayerPlantAt(int col) {
        Level level = currentLevel();
        if (level instanceof IZombieLevel iZombie) {
            return col < iZombie.redLineColumn();
        }
        return true;
    }

    public static boolean shovelEnabled(GameModel gameModel) {
        if (gameModel == null) {
            return true;
        }
        if (gameModel.getCurrentLevel() instanceof IZombieLevel) {
            return false;
        }
        Level level = gameModel.getCurrentLevel();
        if (level == null || level.getConfig() == null || level.getConfig().getRules() == null) {
            return true;
        }
        return level.getConfig().getRules().isShovelEnabled();
    }

    public static boolean plantFoodHudEnabled(GameModel gameModel) {
        if (gameModel == null) {
            return true;
        }
        Level level = gameModel.getCurrentLevel();
        if (level instanceof WallnutBowlingLevel
                || level instanceof VaseBreakerLevel
                || level instanceof BeghouledLevel
                || level instanceof IZombieLevel) {
            return false;
        }
        if (level == null || level.getConfig() == null || level.getConfig().getRules() == null) {
            return true;
        }
        return level.getConfig().getRules().isPlantFoodDrops();
    }

    public static List<String> selectedPlants() {
        GameModel gameModel = model();
        if (gameModel == null || gameModel.getSelectedPlants() == null) {
            return List.of();
        }
        return gameModel.getSelectedPlants();
    }

    public static List<PendingSeedPacket> pendingPackets() {
        Level level = currentLevel();
        if (level instanceof VaseBreakerLevel vaseBreaker) {
            return vaseBreaker.getPendingSeedPackets();
        }
        return List.of();
    }

    public static List<String> iZombieRosterNames() {
        Level level = currentLevel();
        if (!(level instanceof IZombieLevel iZombie)) {
            return List.of();
        }
        return new ArrayList<>(iZombie.getSettings().getZombieCosts().keySet());
    }

    public static Map<String, Integer> iZombieRosterCosts() {
        Map<String, Integer> costs = new LinkedHashMap<>();
        Level level = currentLevel();
        if (!(level instanceof IZombieLevel iZombie)) {
            return costs;
        }
        GameModel gameModel = model();
        float penalty = gameModel == null ? 1f : gameModel.difficultyPenalty();
        for (Map.Entry<String, Integer> entry : iZombie.getSettings().getZombieCosts().entrySet()) {
            costs.put(entry.getKey(), (int) (entry.getValue() * penalty));
        }
        return costs;
    }

    public static List<String> beghouledUpgradeFromNames() {
        Level level = currentLevel();
        if (!(level instanceof BeghouledLevel beghouled)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (BeghouledSettings.UpgradeRule rule : beghouled.getSettings().getUpgrades()) {
            names.add(rule.getFrom());
        }
        return names;
    }

    public static Map<String, Integer> beghouledUpgradeCosts(Level level) {
        Map<String, Integer> costs = new HashMap<>();
        if (!(level instanceof BeghouledLevel beghouled)) {
            return costs;
        }
        for (BeghouledSettings.UpgradeRule rule : beghouled.getSettings().getUpgrades()) {
            costs.put(rule.getFrom(), rule.getCost());
        }
        return costs;
    }
}
