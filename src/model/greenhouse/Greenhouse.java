package model.greenhouse;

import model.enums.PotState;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.user.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The greenhouse where the player can grow plants in pots.
 * Contains a 4x5 grid of pots (20 total). Row y=1 starts unlocked;
 * rows y=2..4 start locked and can be unlocked by spending coins.
 * The greenhouse tracks last harvest times and handles plant production.
 */
public class Greenhouse {

    public static final int ROWS = 4;
    public static final int COLS = 5;
    public static final int TOTAL_POTS = ROWS * COLS;
    public static final int DEFAULT_UNLOCKED_POTS = COLS; // row y=1 starts unlocked

    private static final String MARIGOLD_TYPE = "marigold";
    private static final float MARIGOLD_GROWTH_HOURS = 2f;
    private static final float RANDOM_PLANT_GROWTH_HOURS = 8f;
    private static final int MARIGOLD_COIN_REWARD = 500;

    private static Greenhouse instance = null;

    private User owner;
    private final Random random = new Random();
    private Pot[][] pots;

    private Greenhouse() {
        this.pots = new Pot[ROWS][COLS];
        initializePots();
    }

    public static Greenhouse getInstance(User owner) {
        if (instance == null) {
            instance = new Greenhouse();
        }
        instance.owner = owner;
        instance.initializePots();
        instance.loadFromUser();
        return instance;
    }

    // ──────────────────────────────────────────────
    // Initialisation & (de)serialisation
    // ──────────────────────────────────────────────

    /**
     * Allocates the 4x5 grid of pots with their coordinates. Initial state
     * is computed later from {@link User#getUnlockedPots()} in
     * {@link #loadFromUser()}.
     */
    private void initializePots() {
        this.pots = new Pot[ROWS][COLS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = col + 1;
                int y = row + 1;
                pots[row][col] = new Pot(x, y);
            }
        }
    }

    /**
     * Restores the pot grid from the user's persisted fields.
     * Pot unlock state is derived from {@code unlockedPots} (sequential),
     * planted pots and their planting times come from the
     * {@code greenhousePots} / {@code greenhousePlantTimestamps} maps.
     */
    private void loadFromUser() {
        int unlocked = owner.getUnlockedPots();
        // Defensive: per the spec row y=1 starts unlocked, so we ensure
        // the user has at least that many pots open even if they somehow
        // have an older save with unlockedPots=0.
        if (unlocked < DEFAULT_UNLOCKED_POTS) {
            unlocked = DEFAULT_UNLOCKED_POTS;
            owner.setUnlockedPots(unlocked);
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Pot pot = pots[row][col];
                int linearIndex = row * COLS + col; // 0..19, row-major
                if (linearIndex < unlocked) {
                    pot.forceUnlock();
                }
            }
        }
        // Restore planted pots
        Map<String, String> planted = owner.getGreenhousePots();
        Map<String, Long> timestamps = owner.getGreenhousePlantTimestamps();
        if (planted == null) {
            return;
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Pot pot = pots[row][col];
                if (pot.getState() == PotState.LOCKED) {
                    continue;
                }
                String key = pot.getX() + "," + pot.getY();
                String plantName = planted.get(key);
                if (plantName == null) {
                    continue;
                }
                Long ts = timestamps != null ? timestamps.get(key) : null;
                LocalDateTime plantingTime = ts != null
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.systemDefault())
                        : LocalDateTime.now();
                boolean isMarigold = MARIGOLD_TYPE.equalsIgnoreCase(plantName);
                float growthHours = isMarigold ? MARIGOLD_GROWTH_HOURS : RANDOM_PLANT_GROWTH_HOURS;
                pot.restore(plantName, isMarigold, growthHours, plantingTime);
            }
        }
    }

    /**
     * Persists the current pot grid back to the user's fields. Call this
     * after any mutation ({@link #plantPot}, {@link #collect}, {@link #grow},
     * {@link #unlockNextPot}) so the changes survive a session restart.
     */
    public void save() {
        // unlockedPots is mutated directly by unlockNextPot(), so it's already current.
        Map<String, String> planted = owner.getGreenhousePots();
        Map<String, Long> timestamps = owner.getGreenhousePlantTimestamps();
        if (planted == null || timestamps == null) {
            return;
        }
        planted.clear();
        timestamps.clear();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Pot pot = pots[row][col];
                PotState s = pot.getState();
                if (s == PotState.GROWING || s == PotState.READY) {
                    String key = pot.getX() + "," + pot.getY();
                    planted.put(key, pot.getPlantType());
                    LocalDateTime t = pot.getPlantingTime();
                    long epoch = t != null
                            ? t.atZone(ZoneId.systemDefault()).toEpochSecond()
                            : LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
                    timestamps.put(key, epoch);
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    // Commands
    // ──────────────────────────────────────────────

    /**
     * Renders the grid to a string and returns it. The controller prints
     * this verbatim. Each cell shows its state and, for growing plants,
     * the plant type and remaining growth time. Ready plants are tagged
     * {@code READY}.
     *
     * @return a multi-line textual snapshot of the greenhouse grid
     */
    public String renderGrid() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Greenhouse ===\n");
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                sb.append(formatPot(pots[row][col]));
                sb.append(col < COLS - 1 ? "  " : "\n");
            }
        }
        return sb.toString();
    }

    private String formatPot(Pot pot) {
        // forces a lazy GROWING -> READY refresh if time has passed
        pot.isReady();
        String coord = "(" + pot.getX() + "," + pot.getY() + ")";
        switch (pot.getState()) {
            case LOCKED:
                return coord + ":LOCKED";
            case EMPTY:
                return coord + ":EMPTY";
            case GROWING:
                return coord + ":GROWING[" + pot.getPlantType() + ", "
                        + String.format("%.1f", pot.getRemainingGrowthHours()) + "h left]";
            case READY:
                return coord + ":READY[" + pot.getPlantType() + "]";
            default:
                return coord + ":UNKNOWN";
        }
    }

    /**
     * Plants a random seed in the pot at {@code (x, y)}.
     *
     * @param x column (1-5)
     * @param y row (1-4)
     * @return true if a seed was successfully planted, false if the pot is
     *         locked, occupied, or the position is invalid
     */
    public String plantPot(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null || pot.getState() != PotState.EMPTY) {
            return null;
        }
        String chosen;
        boolean isMarigold;
        float growthHours;
        if (random.nextBoolean()) {
            chosen = MARIGOLD_TYPE;
            isMarigold = true;
            growthHours = MARIGOLD_GROWTH_HOURS;
        } else {
            String picked = pickRandomBoostablePlantType();
            if (picked == null) {
                chosen = MARIGOLD_TYPE;
                isMarigold = true;
                growthHours = MARIGOLD_GROWTH_HOURS;
            } else {
                chosen = picked;
                isMarigold = false;
                growthHours = RANDOM_PLANT_GROWTH_HOURS;
            }
        }
        pot.plant(chosen, isMarigold, growthHours);
        return chosen;
    }

    private String pickRandomBoostablePlantType() {
        Set<String> unlocked = owner.getUnlockedPlants();
        if (unlocked == null || unlocked.isEmpty()) {
            return null;
        }
        Set<String> candidates = new HashSet<>();
        try {
            List<Plant> all = PlantFactory.getAllDefinitions();
            for (Plant def : all) {
                if (def.hasPlantFood() && unlocked.contains(def.getName())) {
                    candidates.add(def.getName());
                }
            }
        } catch (IllegalStateException ignored) {
            // PlantFactory not initialised yet — fall back to all unlocked plants.
        }
        if (candidates.isEmpty()) {
            // Either PlantFactory wasn't initialised or no unlocked plant has a
            // plant-food effect. Either way, fall back to all unlocked plants
            // so the player can still plant *something* non-marigold.
            candidates.addAll(unlocked);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        List<String> shuffled = new ArrayList<>(candidates);
        return shuffled.get(random.nextInt(shuffled.size()));
    }

    /**
     * Harvests a fully grown plant from the pot at position (x, y).
     * Marigold yields 500 coins; other plants grant a stored boost
     * for that plant type. Only one boost per plant type can be stored.
     *
     * @param x column (1-5)
     * @param y row (1-4)
     * @return the produce from harvesting, or null if not ready or invalid position
     */
    public GreenhouseProduce collect(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null || !pot.isReady()) {
            return null;
        }
        boolean wasMarigold = pot.isMarigold();
        String plantType = pot.harvest();
        if (plantType == null) {
            return null;
        }
        if (wasMarigold) {
            return GreenhouseProduce.forCoins(MARIGOLD_COIN_REWARD);
        }
        Map<String, Boolean> boosts = owner.getPlantBoosts();
        if (boosts != null && Boolean.TRUE.equals(boosts.get(plantType))) {
            // A boost for this plant type is already stored: the pot is
            // cleared (harvest() already did that) but no extra boost.
            return GreenhouseProduce.empty();
        }
        return GreenhouseProduce.forBoost(plantType);
    }

    /**
     * Accelerates the growth of the plant at position (x, y)
     * by spending 1 gem per remaining hour (ceiling).
     *
     * @param x column (1-5)
     * @param y row (1-4)
     * @return the gem cost of acceleration
     */
    public int growCost(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null) {
            return 0;
        }
        return pot.accelerationCost();
    }

    /**
     * Unlocks a locked pot at position (x, y) by spending coins.
     *
     * @param x column (1-5)
     * @param y row (1-4)
     * @return true if the pot was successfully unlocked
     */
    public boolean commitGrow(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null) {
            return false;
        }
        return pot.accelerateGrowth();
    }

    /**
     * Unlocks the next locked pot in row-major order (i.e. the pot at
     * index {@code unlockedPots} in the linear sequence). Called by the
     * shop when the player purchases the "Pot" item. Does not charge the
     * player — the shop handles the coin deduction.
     *
     * @return the coordinates {@code (x, y)} of the newly unlocked pot,
     *         or {@code null} if all 20 pots are already unlocked
     */
    public int[] unlockNextPot() {
        int current = owner.getUnlockedPots();
        if (current >= TOTAL_POTS) {
            return null;
        }
        int row = current / COLS;
        int col = current % COLS;
        Pot pot = pots[row][col];
        if (!pot.unlock()) {
            return null;
        }
        owner.setUnlockedPots(current + 1);
        return new int[]{pot.getX(), pot.getY()};
    }

    /**
     * Returns the {@code (x, y)} coordinates of the next pot that would
     * be unlocked, or {@code null} if all pots are already unlocked.
     * Used by the shop / controller for hint messages.
     *
     * @return the next pot's coordinates, or {@code null}
     */
    public int[] nextPotToUnlock() {
        int current = owner.getUnlockedPots();
        if (current >= TOTAL_POTS) {
            return null;
        }
        int row = current / COLS;
        int col = current % COLS;
        return new int[]{col + 1, row + 1};
    }

    /**
     * Returns the pot at the given position.
     *
     * @param x column (1-5)
     * @param y row (1-4)
     * @return the pot at (x, y), or null if out of bounds
     */
    public Pot getPot(int x, int y) {
        if (x < 1 || x > COLS || y < 1 || y > ROWS) {
            return null;
        }
        return pots[y - 1][x - 1];
    }

    /**
     * Returns the number of plants currently growing (not yet ready).
     *
     * @return count of growing plants
     */
    public int getProducingCount() {
        int count = 0;
        for (Pot[] row : pots) {
            for (Pot pot : row) {
                pot.isReady(); // refresh lazy state before counting
                if (pot.getState() == PotState.GROWING) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns the number of pots that are currently ready for harvest.
     *
     * @return count of ready plants
     */
    public int getReadyCount() {
        int count = 0;
        for (Pot[] row : pots) {
            for (Pot pot : row) {
                pot.isReady();
                if (pot.getState() == PotState.READY) {
                    count++;
                }
            }
        }
        return count;
    }

    // ──────────────────────────────────────────────
    // Getters
    // ──────────────────────────────────────────────

    public User getOwner() {
        return owner;
    }

    public Pot[][] getPots() {
        return pots;
    }

    public int getUnlockedPotCount() {
        return owner.getUnlockedPots();
    }

    public List<Pot> getAllPotsAsList() {
        List<Pot> list = new ArrayList<>(TOTAL_POTS);
        for (Pot[] row : pots) {
            for (Pot pot : row) {
                list.add(pot);
            }
        }
        return list;
    }
}
