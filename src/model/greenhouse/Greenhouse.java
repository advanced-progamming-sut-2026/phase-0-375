package model.greenhouse;

/**
 * The greenhouse where the player can grow plants in pots.
 * Contains a 4x5 grid of pots (20 total). Rows 2-4 start locked
 * and can be unlocked by spending coins. The greenhouse tracks
 * last harvest times and handles plant production.
 */
public class Greenhouse {
    private Pot[][] pots;
    private int unlockedPotCount;

    public Greenhouse() {
        this.pots = new Pot[4][5];  // 4 rows, 5 columns
        this.unlockedPotCount = 0;
        initializePots();
    }

    /**
     * Initializes the 4x5 grid of pots.
     * Rows 0-1 (y=0,1) start as EMPTY; rows 2-3 (y=2,3) start as LOCKED.
     */
    private void initializePots() {}

    /**
     * Shows the current state of the greenhouse grid.
     * Each pot displays its state (locked/empty/growing/ready),
     * plant type, and remaining growth time.
     */
    public void showGreenhouse() {}

    /**
     * Plants a random seed in the pot at position (x, y).
     * 50% chance of marigold, 50% chance of a random unlocked plant
     * that has a plant food effect.
     *
     * @param x column (1-5)
     * @param y row (1-4)
     */
    public void plantPot(int x, int y) {}

    /**
     * Harvests a fully grown plant from the pot at position (x, y).
     * Marigold yields 500 coins; other plants grant a stored boost
     * for that plant type. Only one boost per plant type can be stored.
     *
     * @param x column (1-5)
     * @param y row (1-4)
     * @return the produce from harvesting, or null if not ready
     */
    public GreenhouseProduce collect(int x, int y) {
        return null;
    }

    /**
     * Accelerates the growth of the plant at position (x, y)
     * by spending 1 gem per remaining hour (ceiling).
     *
     * @param x column (1-5)
     * @param y row (1-4)
     * @return the gem cost of acceleration
     */
    public int grow(int x, int y) {
        return 0;
    }

    /**
     * Unlocks a locked pot at position (x, y) by spending coins.
     *
     * @param x column (1-5)
     * @param y row (1-4)
     * @return true if the pot was successfully unlocked
     */
    public boolean unlockPot(int x, int y) {
        return false;
    }

    /**
     * Returns the pot at the given position.
     *
     * @param x column (1-5)
     * @param y row (1-4)
     * @return the pot at (x, y), or null if out of bounds
     */
    public Pot getPot(int x, int y) {
        return null;
    }

    /**
     * Returns the time of the most recent harvest across all pots.
     *
     * @return the last harvest time, or null if no harvest yet
     */
    public java.time.LocalDateTime getLastHarvestTime() {
        return null;
    }

    /**
     * Returns the total number of plants currently producing
     * (i.e., in the GROWING state).
     *
     * @return count of growing plants
     */
    public int getProducingCount() {
        return 0;
    }

    // --- Getters ---

    public Pot[][] getPots() {
        return null; // for now. we'll change it later
    }

    public int getUnlockedPotCount() {
        return unlockedPotCount;
    }

    // --- Setters ---

    public void setPots(Pot[][] pots) {
    }

    public void setUnlockedPotCount(int unlockedPotCount) {
        this.unlockedPotCount = unlockedPotCount;
    }
}
