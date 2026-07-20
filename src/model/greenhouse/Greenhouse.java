package model.greenhouse;

import model.enums.PotState;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.user.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * The greenhouse: a 4x5 grid of 20 pots. Row y=1 starts unlocked;
 * the rest are unlocked by buying the "Pot" item from the shop.
 */
public class Greenhouse {

    public static final int ROWS = 4;
    public static final int COLS = 5;
    public static final int TOTAL_POTS = ROWS * COLS;
    public static final int DEFAULT_UNLOCKED_POTS = COLS; // row y=1

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

    /**
     * Returns the singleton bound to the given owner.
     * Pots are reloaded from the user's data ONLY when the owner changes;
     * reloading on every call used to wipe unsaved in-memory changes
     * (planting/collect/grow were reverted before being saved).
     */
    public static Greenhouse getInstance(User owner) {
        if (instance == null) {
            instance = new Greenhouse();
        }
        boolean sameOwner = owner != null && instance.owner != null
                && Objects.equals(owner.getUsername(), instance.owner.getUsername());
        instance.owner = owner;
        if (!sameOwner && owner != null) {
            instance.initializePots();
            instance.loadFromUser();
        }
        return instance;
    }

    // ──────────────────────────────────────────────
    // Initialisation & persistence
    // ──────────────────────────────────────────────

    private void initializePots() {
        this.pots = new Pot[ROWS][COLS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                pots[row][col] = new Pot(col + 1, row + 1);
            }
        }
    }

    /** Rebuilds the grid from the owner's persisted fields. */
    private void loadFromUser() {
        int unlocked = owner.getUnlockedPots();
        // row y=1 is always unlocked per the spec
        if (unlocked < DEFAULT_UNLOCKED_POTS) {
            unlocked = DEFAULT_UNLOCKED_POTS;
            owner.setUnlockedPots(unlocked);
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (row * COLS + col < unlocked) {
                    pots[row][col].forceUnlock();
                }
            }
        }
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

    /** Persists the grid back to the owner. Creates missing maps (old saves). */
    public void save() {
        Map<String, String> planted = owner.getGreenhousePots();
        if (planted == null) {
            planted = new HashMap<>();
            owner.setGreenhousePots(planted);
        }
        Map<String, Long> timestamps = owner.getGreenhousePlantTimestamps();
        if (timestamps == null) {
            timestamps = new HashMap<>();
            owner.setGreenhousePlantTimestamps(timestamps);
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

    /** Renders the grid as text; ready pots are tagged READY. */
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
        pot.isReady(); // lazy GROWING -> READY refresh
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
     * Plants a random seed at (x, y): 50% marigold, 50% a random unlocked
     * plant with plant-food ability. Returns the plant type, or null if the
     * pot is locked/occupied/invalid.
     */
    public String plantPot(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null || pot.getState() != PotState.EMPTY) {
            return null;
        }
        String chosen;
        boolean isMarigold;
        float growthHours;
        String picked = random.nextBoolean() ? null : pickRandomBoostablePlantType();
        if (picked == null) {
            // marigold branch, or no eligible plant -> marigold per spec
            chosen = MARIGOLD_TYPE;
            isMarigold = true;
            growthHours = MARIGOLD_GROWTH_HOURS;
        } else {
            chosen = picked;
            isMarigold = false;
            growthHours = RANDOM_PLANT_GROWTH_HOURS;
        }
        pot.plant(chosen, isMarigold, growthHours);
        return chosen;
    }

    /** Only unlocked plants WITH plant-food ability qualify (per spec). */
    private String pickRandomBoostablePlantType() {
        Set<String> unlocked = owner.getUnlockedPlants();
        if (unlocked == null || unlocked.isEmpty()) {
            return null;
        }
        Set<String> candidates = new HashSet<>();
        List<Plant> all;
        try {
            all = PlantFactory.getAllDefinitions();
        } catch (IllegalStateException notInitialised) {
            // greenhouse can be visited before any level initialises the factory
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
                all = PlantFactory.getAllDefinitions();
            } catch (Exception ex) {
                return null; // definitions unavailable -> marigold fallback
            }
        }
        for (Plant def : all) {
            if (def.hasPlantFood() && isUnlocked(unlocked, def.getName())) {
                candidates.add(def.getName());
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        List<String> list = new ArrayList<>(candidates);
        return list.get(random.nextInt(list.size()));
    }

    /** Case-insensitive membership test against unlocked plant names. */
    private boolean isUnlocked(Set<String> unlocked, String name) {
        if (name == null) return false;
        for (String u : unlocked) {
            if (name.equalsIgnoreCase(u)) return true;
        }
        return false;
    }

    /**
     * Harvests a ready plant. Marigold yields coins; other plants store a
     * one-shot boost (max one stored boost per plant type).
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
            return GreenhouseProduce.empty(); // boost already stored
        }
        return GreenhouseProduce.forBoost(plantType);
    }

    /** Gem cost (ceil of remaining hours) to finish growth instantly. */
    public int growCost(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null) {
            return 0;
        }
        return pot.accelerationCost();
    }

    /** Marks the plant at (x, y) as fully grown. Caller charges the gems. */
    public boolean commitGrow(int x, int y) {
        Pot pot = getPot(x, y);
        if (pot == null) {
            return false;
        }
        return pot.accelerateGrowth();
    }

    /**
     * Unlocks the next locked pot in row-major order (shop "Pot" purchase).
     * Returns the pot's (x, y), or null if all 20 pots are unlocked.
     */
    public int[] unlockNextPot() {
        int current = owner.getUnlockedPots();
        if (current >= TOTAL_POTS) {
            return null;
        }
        Pot pot = pots[current / COLS][current % COLS];
        if (!pot.unlock()) {
            return null;
        }
        owner.setUnlockedPots(current + 1);
        return new int[]{pot.getX(), pot.getY()};
    }

    /** (x, y) of the next pot that would be unlocked, or null if none left. */
    public int[] nextPotToUnlock() {
        int current = owner.getUnlockedPots();
        if (current >= TOTAL_POTS) {
            return null;
        }
        return new int[]{current % COLS + 1, current / COLS + 1};
    }

    /** Pot at (x, y), or null if out of bounds. */
    public Pot getPot(int x, int y) {
        if (x < 1 || x > COLS || y < 1 || y > ROWS) {
            return null;
        }
        return pots[y - 1][x - 1];
    }

    public int getProducingCount() {
        int count = 0;
        for (Pot[] row : pots) {
            for (Pot pot : row) {
                pot.isReady();
                if (pot.getState() == PotState.GROWING) {
                    count++;
                }
            }
        }
        return count;
    }

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

    // ── Getters ──

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
