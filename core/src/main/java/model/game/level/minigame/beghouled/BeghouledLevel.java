package model.game.level.minigame.beghouled;

import model.app.App;
import model.enums.MiniGameType;
import model.enums.PlacableLayer;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;
import model.game.map.Cell;
import model.game.map.Point;
import model.plant.PlantFactory;
import model.plant.instance.PlantInstance;
import model.zombie.ZombieFactory;
import model.zombie.instance.ZombieInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Beghouled: a match-3 board on the lawn.
 */
public class BeghouledLevel extends MiniGameLevel {

    /** Sun granted per "one sun" reward unit. */
    public static final int SUN_PER_REWARD = 50;

    private static final int LAWN_COLUMNS = 8;

    /** Attempts at generating a board that has at least one legal move. */
    private static final int MAX_BOARD_GENERATION_ATTEMPTS = 60;

    private BeghouledSettings settings = new BeghouledSettings();
    private final Random random = new Random();

    /** The plant currently occupying each cell; null = crater or empty. */
    private PlantInstance[][] board;
    /** Cells destroyed by zombies; nothing can ever occupy them again. */
    private boolean[][] craters;
    private int matchesMade;

    private float spawnTimer;
    private float currentSpawnInterval;

    public BeghouledLevel(LevelConfig config, MiniGameType miniGameType, int difficultyTier) {
        super(config, miniGameType, difficultyTier);
    }

    public BeghouledSettings getSettings() {
        return settings;
    }

    public void setSettings(BeghouledSettings settings) {
        if (settings != null) {
            this.settings = settings;
        }
    }

    public int getMatchesMade() {
        return matchesMade;
    }

    @Override
    public boolean canStart() {
        LevelConfig config = getConfig();
        if (config == null || config.getRows() <= 0 || config.getColumns() <= 0
                || config.getRules() == null) {
            return false;
        }
        // Exactly five distinct board plant types (per spec).
        List<String> plantTypes = settings.getPlantTypes();
        if (plantTypes.size() != 5 || new HashSet<>(plantTypes).size() != 5) {
            return false;
        }
        if (settings.getMatchTarget() <= 0 || settings.getZombiePool().isEmpty()
                || settings.getSpawnIntervalSeconds() <= 0
                || settings.getMinSpawnIntervalSeconds() <= 0) {
            return false;
        }
        if (!ensurePlantFactory() || !ensureZombieFactory()) {
            return false;
        }
        for (String plant : plantTypes) {
            if (!PlantFactory.hasDefinition(plant)) {
                return false;
            }
        }
        for (BeghouledSettings.UpgradeRule rule : settings.getUpgrades()) {
            if (rule.getCost() <= 0
                    || !PlantFactory.hasDefinition(rule.getFrom())
                    || !PlantFactory.hasDefinition(rule.getTo())) {
                return false;
            }
        }
        for (String zombie : settings.getZombiePool()) {
            if (!ZombieFactory.hasDefinition(zombie)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return;
        }
        int rows = getConfig().getRows();
        int cols = getConfig().getColumns();
        board = new PlantInstance[rows][cols];
        craters = new boolean[rows][cols];
        matchesMade = 0;
        currentSpawnInterval = settings.getSpawnIntervalSeconds();
        spawnTimer = settings.getFirstSpawnDelaySeconds();
        resetBoard(model);
    }

    @Override
    public void tick(float deltaTime) {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null || board == null) {
            return;
        }
        // Any plant a zombie finished eating leaves a crater behind.
        if (detectEatenPlants(model) && !possibleMoveExists()) {
            resetBoard(model);
        }
        // Endless attack: the waves never stop and slowly speed up.
        spawnTimer -= deltaTime;
        if (spawnTimer <= 0f) {
            spawnZombieFromPool(model);
            spawnTimer += currentSpawnInterval;
            currentSpawnInterval = Math.max(settings.getMinSpawnIntervalSeconds(),
                    currentSpawnInterval - settings.getSpawnIntervalDecaySeconds());
        }
    }

    @Override
    public void onWaveCleared(int waveNumber) {
        // No scripted waves: zombies spawn endlessly from tick().
    }

    @Override
    public void onFail() {
        // No special teardown.
    }

    /** Win: the required number of matches has been made. */
    @Override
    public boolean checkWinCondition(GameModel model) {
        return matchesMade >= settings.getMatchTarget();
    }

    /** Loss: a zombie reached the house. */
    @Override
    public boolean checkLossCondition(GameModel model) {
        return model != null && !checkWinCondition(model) && model.isHouseBreached();
    }

    // --- Player commands ---

    /**
     * Swaps the plant at (row, col) with its neighbour in the given
     * direction. Only legal if the swap creates a match of 3+.
     *
     * @return null on success, otherwise a user-facing error message
     */
    public String swapPlant(GameModel model, int row, int col, String direction) {
        if (model == null || board == null) {return "No active game.";}
        detectEatenPlants(model);
        int targetRow = row;
        int targetCol = col;
        String dir = direction == null ? "" : direction.toLowerCase();
        if (dir.equals("up")) {
            targetRow--;
        } else if (dir.equals("down")) {
            targetRow++;
        } else if (dir.equals("left")) {
            targetCol--;
        } else if (dir.equals("right")) {
            targetCol++;
        } else {
            return "Unknown direction '" + direction + "'. Use up, down, left or right.";
        }
        if (!inBounds(row, col) || !inBounds(targetRow, targetCol)) {
            return "Swap target is out of bounds.";
        }
        if (craters[row][col] || craters[targetRow][targetCol]) {
            return "You cannot swap into a crater.";
        }
        PlantInstance first = board[row][col];
        PlantInstance second = board[targetRow][targetCol];
        if (first == null || second == null) {
            return "Both cells must contain a plant.";
        }
        // The swap is only allowed if it creates at least one match.
        String[][] names = boardNames();
        names[row][col] = plantName(second);
        names[targetRow][targetCol] = plantName(first);
        if (!createsMatchAt(names, row, col) && !createsMatchAt(names, targetRow, targetCol)) {
            return "Illegal swap: it would not create a match of 3.";
        }
        // Physically swap the two plants on the map.
        Cell firstCell = model.getMap().getCell(col, row);
        Cell secondCell = model.getMap().getCell(targetCol, targetRow);
        firstCell.removePlaceable(first);
        secondCell.removePlaceable(second);
        firstCell.addPlaceable(second);
        second.setPosition(new Point(col, row));
        secondCell.addPlaceable(first);
        first.setPosition(new Point(targetCol, targetRow));
        board[row][col] = second;
        board[targetRow][targetCol] = first;
        resolveBoard(model);
        return null;
    }

    /**
     * Upgrades every plant of the given type on the board at once, if the
     * stage offers that upgrade and the player can afford it.
     *
     * @return null on success, otherwise a user-facing error message
     */
    public String upgradePlant(GameModel model, String fromType) {
        if (model == null || board == null) {
            return "No active game.";
        }
        detectEatenPlants(model);
        BeghouledSettings.UpgradeRule rule = settings.findUpgrade(fromType);
        if (rule == null) {
            StringBuilder available = new StringBuilder();
            for (BeghouledSettings.UpgradeRule r : settings.getUpgrades()) {
                if (available.length() > 0) {
                    available.append(", ");
                }
                available.append(r.getFrom());
            }
            return "No upgrade available for '" + fromType + "'. Upgradable: " + available + ".";
        }
        List<int[]> targets = new ArrayList<>();
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                if (board[r][c] != null && rule.getFrom().equalsIgnoreCase(plantName(board[r][c]))) {
                    targets.add(new int[] { r, c });
                }
            }
        }
        if (targets.isEmpty()) {
            return "There is no '" + rule.getFrom() + "' on the board.";
        }
        if (!model.spendSun(rule.getCost())) {
            return "Not enough sun. Need " + rule.getCost() + ", have " + model.getSunAmount() + ".";
        }
        for (int[] cell : targets) {
            int r = cell[0];
            int c = cell[1];
            model.getMap().getCell(c, r).removePlaceable(board[r][c]);
            board[r][c] = null;
            PlantInstance upgraded = PlantFactory.createInstance(rule.getTo());
            if (upgraded != null && model.placePlant(upgraded, r, c)) {
                board[r][c] = upgraded;
            }
        }
        // Upgrading can itself line up 3+ of the new plant; resolve normally.
        resolveBoard(model);
        return null;
    }

    /** One-line progress report plus the stage's upgrade price list. */
    public String statusReport(GameModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("Matches: ").append(matchesMade).append("/").append(settings.getMatchTarget());
        if (model != null) {
            sb.append(" | Sun: ").append(model.getSunAmount());
        }
        sb.append(" | Craters: ").append(craterCount());
        sb.append("\nUpgrades:");
        if (settings.getUpgrades().isEmpty()) {
            sb.append(" none");
        }
        for (BeghouledSettings.UpgradeRule rule : settings.getUpgrades()) {
            sb.append("\n  ").append(rule.getFrom()).append(" -> ").append(rule.getTo())
                    .append(" (").append(rule.getCost()).append(" sun)");
        }
        return sb.toString();
    }

    // --- Match resolution ---

    /**
     * Repeatedly finds matches, pays out sun, applies gravity and refills
     * until the board is stable. Matches formed by fallen/refilled plants
     * (cascades) pay one extra sun each.
     */
    private void resolveBoard(GameModel model) {
        int cascadeDepth = 0;
        while (true) {
            List<List<int[]>> matches = findMatches();
            if (matches.isEmpty()) {
                break;
            }
            Set<Integer> clearedCells = new HashSet<>();
            for (List<int[]> match : matches) {
                matchesMade++;
                int rewardUnits = match.size() - 2 + (cascadeDepth > 0 ? 1 : 0);
                model.addSun(SUN_PER_REWARD * rewardUnits);
                for (int[] cell : match) {
                    clearedCells.add(cell[0] * cols() + cell[1]);
                }
            }
            for (int key : clearedCells) {
                int r = key / cols();
                int c = key % cols();
                if (board[r][c] != null) {
                    model.getMap().getCell(c, r).removePlaceable(board[r][c]);
                    board[r][c] = null;
                }
            }
            applyGravity(model);
            refill(model);
            cascadeDepth++;
        }
        if (matchesMade >= settings.getMatchTarget()) {
            // Spec: reaching the target destroys every zombie in the garden.
            killAllZombies(model);
        } else if (!possibleMoveExists()) {
            resetBoard(model);
        }
    }

    /** Finds every maximal horizontal/vertical run of 3+ equal plants. */
    private List<List<int[]>> findMatches() {
        List<List<int[]>> matches = new ArrayList<>();
        for (int r = 0; r < rows(); r++) {
            int c = 0;
            while (c < cols()) {
                int run = runLength(r, c, 0, 1);
                if (board[r][c] != null && run >= 3) {
                    List<int[]> match = new ArrayList<>();
                    for (int i = 0; i < run; i++) {
                        match.add(new int[] { r, c + i });
                    }
                    matches.add(match);
                }
                c += Math.max(run, 1);
            }
        }
        for (int c = 0; c < cols(); c++) {
            int r = 0;
            while (r < rows()) {
                int run = runLength(r, c, 1, 0);
                if (board[r][c] != null && run >= 3) {
                    List<int[]> match = new ArrayList<>();
                    for (int i = 0; i < run; i++) {
                        match.add(new int[] { r + i, c });
                    }
                    matches.add(match);
                }
                r += Math.max(run, 1);
            }
        }
        return matches;
    }

    private int runLength(int r, int c, int dr, int dc) {
        String name = board[r][c] == null ? null : plantName(board[r][c]);
        if (name == null) {
            return 1;
        }
        int length = 1;
        while (inBounds(r + dr * length, c + dc * length)
                && board[r + dr * length][c + dc * length] != null
                && name.equals(plantName(board[r + dr * length][c + dc * length]))) {
            length++;
        }
        return length;
    }

    /** Plants fall towards the bottom row; craters block falling plants. */
    private void applyGravity(GameModel model) {
        for (int c = 0; c < cols(); c++) {
            int write = rows() - 1;
            for (int r = rows() - 1; r >= 0; r--) {
                if (craters[r][c]) {
                    write = r - 1;
                    continue;
                }
                if (board[r][c] != null) {
                    if (write != r && model.movePlant(board[r][c], write, c)) {
                        board[write][c] = board[r][c];
                        board[r][c] = null;
                    }
                    write--;
                }
            }
        }
    }

    /** New random plants (from the stage's five types) drop in from the top. */
    private void refill(GameModel model) {
        List<String> types = settings.getPlantTypes();
        for (int c = 0; c < cols(); c++) {
            for (int r = 0; r < rows(); r++) {
                if (!craters[r][c] && board[r][c] == null) {
                    PlantInstance plant = PlantFactory.createInstance(
                            types.get(random.nextInt(types.size())));
                    if (plant != null && model.placePlant(plant, r, c)) {
                        board[r][c] = plant;
                    }
                }
            }
        }
    }

    // --- Board generation ---

    /**
     * Clears the board and regenerates it randomly (spec: reset when no
     * more matches are possible). The new board never starts with a
     * ready-made match and always has at least one legal move.
     */
    private void resetBoard(GameModel model) {
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                if (board[r][c] != null) {
                    model.getMap().getCell(c, r).removePlaceable(board[r][c]);
                    board[r][c] = null;
                }
            }
        }
        String[][] names = null;
        for (int attempt = 0; attempt < MAX_BOARD_GENERATION_ATTEMPTS; attempt++) {
            names = generateBoardNames();
            if (namesHaveMove(names)) {
                break;
            }
        }
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                if (names[r][c] != null) {
                    PlantInstance plant = PlantFactory.createInstance(names[r][c]);
                    if (plant != null && model.placePlant(plant, r, c)) {
                        board[r][c] = plant;
                    }
                }
            }
        }
    }

    /** Random full board with no ready-made 3-match; craters stay empty. */
    private String[][] generateBoardNames() {
        List<String> types = settings.getPlantTypes();
        String[][] names = new String[rows()][cols()];
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                if (craters[r][c]) {
                    continue;
                }
                String pick;
                do {
                    pick = types.get(random.nextInt(types.size()));
                } while (wouldCompleteRun(names, r, c, pick));
                names[r][c] = pick;
            }
        }
        return names;
    }

    /** True if placing {@code name} at (r, c) completes a run of 3. */
    private boolean wouldCompleteRun(String[][] names, int r, int c, String name) {
        if (c >= 2 && name.equals(names[r][c - 1]) && name.equals(names[r][c - 2])) {
            return true;
        }
        return r >= 2 && name.equals(names[r - 1][c]) && name.equals(names[r - 2][c]);
    }

    // --- Move availability ---

    private boolean possibleMoveExists() {
        return namesHaveMove(boardNames());
    }

    /** True if any adjacent swap of two plants would create a match. */
    private boolean namesHaveMove(String[][] names) {
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                if (names[r][c] == null) {
                    continue;
                }
                if (c + 1 < cols() && names[r][c + 1] != null
                        && swapCreatesMatch(names, r, c, r, c + 1)) {
                    return true;
                }
                if (r + 1 < rows() && names[r + 1][c] != null
                        && swapCreatesMatch(names, r, c, r + 1, c)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean swapCreatesMatch(String[][] names, int r1, int c1, int r2, int c2) {
        String tmp = names[r1][c1];
        names[r1][c1] = names[r2][c2];
        names[r2][c2] = tmp;
        boolean match = createsMatchAt(names, r1, c1) || createsMatchAt(names, r2, c2);
        names[r2][c2] = names[r1][c1];
        names[r1][c1] = tmp;
        return match;
    }

    /** True if (r, c) is part of a horizontal or vertical run of 3+. */
    private boolean createsMatchAt(String[][] names, int r, int c) {
        String name = names[r][c];
        if (name == null) {
            return false;
        }
        int horizontal = 1;
        for (int i = c - 1; i >= 0 && name.equals(names[r][i]); i--) {
            horizontal++;
        }
        for (int i = c + 1; i < cols() && name.equals(names[r][i]); i++) {
            horizontal++;
        }
        if (horizontal >= 3) {
            return true;
        }
        int vertical = 1;
        for (int i = r - 1; i >= 0 && name.equals(names[i][c]); i--) {
            vertical++;
        }
        for (int i = r + 1; i < rows() && name.equals(names[i][c]); i++) {
            vertical++;
        }
        return vertical >= 3;
    }

    // --- Zombies ---

    /**
     * Marks a crater in every cell whose tracked plant is no longer on the
     * map (i.e. a zombie finished eating it).
     *
     * @return true if at least one new crater appeared
     */
    private boolean detectEatenPlants(GameModel model) {
        boolean changed = false;
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                if (board[r][c] != null && model.getMap().getCell(c, r)
                        .getPlaceable(PlacableLayer.MAIN) != board[r][c]) {
                    board[r][c] = null;
                    craters[r][c] = true;
                    changed = true;
                }
            }
        }
        return changed;
    }

    private void spawnZombieFromPool(GameModel model) {
        List<String> pool = settings.getZombiePool();
        String zombie = pool.get(random.nextInt(pool.size()));
        int spawnCol = Math.min(LAWN_COLUMNS - 1, cols() - 1);
        model.spawnZombieAt(zombie, random.nextInt(rows()), spawnCol);
    }

    /** Spec: reaching the match target destroys every zombie in the garden. */
    private void killAllZombies(GameModel model) {
        for (ZombieInstance zombie : new ArrayList<>(model.getZombies())) {
            model.removeZombie(zombie);
        }
    }

    // --- Helpers ---

    private int rows() {
        return getConfig().getRows();
    }

    private int cols() {
        return getConfig().getColumns();
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < rows() && c >= 0 && c < cols();
    }

    private String plantName(PlantInstance plant) {
        return plant.getDefinition() == null ? null : plant.getDefinition().getName();
    }

    private String[][] boardNames() {
        String[][] names = new String[rows()][cols()];
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                names[r][c] = board[r][c] == null ? null : plantName(board[r][c]);
            }
        }
        return names;
    }

    private int craterCount() {
        int count = 0;
        for (int r = 0; r < rows(); r++) {
            for (int c = 0; c < cols(); c++) {
                if (craters[r][c]) {
                    count++;
                }
            }
        }
        return count;
    }

    // --- Factory bootstrap (mirrors the other mini-games) ---

    private static boolean ensurePlantFactory() {
        try {
            PlantFactory.getAllDefinitions();
            return true;
        } catch (IllegalStateException notInitialised) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
                return true;
            } catch (IOException | RuntimeException loadError) {
                return false;
            }
        }
    }

    private static boolean ensureZombieFactory() {
        try {
            ZombieFactory.hasDefinition("ZombieDefault");
            return true;
        } catch (RuntimeException notInitialised) {
            try {
                ZombieFactory.init("/assets/data/zombies/zombies.json",
                        "/assets/data/armor/ArmorTypeData.json");
                return true;
            } catch (IOException | RuntimeException loadError) {
                return false;
            }
        }
    }
}
