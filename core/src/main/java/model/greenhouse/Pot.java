package model.greenhouse;

import model.enums.PotState;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Represents a single pot/slot in the greenhouse.
 *
 * <p>A pot can be locked, empty, growing a plant, or ready for harvest.
 * Each pot has a fixed 1-indexed position {@code (x, y)} in the 3×4
 * greenhouse grid. The pot itself is a transient object rebuilt each
 * session; its persistent state lives on the owning {@link model.user.User}
 * (see {@link model.greenhouse.Greenhouse}). Restoration helpers
 * ({@link #restore}, {@link #forceUnlock}) exist so the greenhouse can
 * rebuild a pot's in-memory state from the user's persisted fields on
 * entry.</p>
 */
public class Pot {
    private final int x;
    private final int y;
    private PotState state;
    private String plantType;
    private LocalDateTime plantingTime;
    private float growthDurationHours;
    private boolean isMarigold;

    /**
     * Creates a new pot at the given grid position. Initial state is
     * {@link PotState#LOCKED} for {@code y >= 2}, {@link PotState#EMPTY}
     * for {@code y == 1}. The owning {@link model.greenhouse.Greenhouse}
     * overrides this via {@link #forceUnlock()} during loading if the
     * user has already unlocked more pots.
     *
     * @param x column (1..{@link Greenhouse#COLS})
     * @param y row (1..{@link Greenhouse#ROWS})
     */
    public Pot(int x, int y) {
        this.x = x;
        this.y = y;
        this.state = (y >= 2) ? PotState.LOCKED : PotState.EMPTY;
        this.plantType = null;
        this.plantingTime = null;
        this.growthDurationHours = 0;
        this.isMarigold = false;
    }

    /**
     * Marks this pot as unlocked (i.e. transitions from {@link PotState#LOCKED}
     * to {@link PotState#EMPTY}). Used by the greenhouse when loading a
     * user's persisted {@code unlockedPots} count, regardless of the pot's
     * y-coordinate rule.
     */
    public void forceUnlock() {
        if (state == PotState.LOCKED) {
            state = PotState.EMPTY;
        }
    }

    /**
     * Unlocks this pot by changing its state from {@link PotState#LOCKED}
     * to {@link PotState#EMPTY}. Unlike {@link #forceUnlock()} this only
     * succeeds if the pot is currently locked, matching the user-facing
     * unlock command's semantics.
     *
     * @return true if the pot was successfully unlocked
     */
    public boolean unlock() {
        if (state != PotState.LOCKED) {
            return false;
        }
        state = PotState.EMPTY;
        return true;
    }

    /**
     * Plants a seed in this pot. The pot must be {@link PotState#EMPTY}.
     *
     * @param plantType         the type of plant to grow
     * @param isMarigold        whether the plant is a marigold
     * @param growthDurationHours hours until the plant is fully grown
     */
    public void plant(String plantType, boolean isMarigold,
                      float growthDurationHours) {
        if (state != PotState.EMPTY) {
            return;
        }
        this.plantType = plantType;
        this.isMarigold = isMarigold;
        this.growthDurationHours = growthDurationHours;
        this.plantingTime = LocalDateTime.now();
        this.state = PotState.GROWING;
    }

    /**
     * Restores a previously planted pot from persisted state. Used by
     * {@link model.greenhouse.Greenhouse#loadFromUser()} on entry. The
     * pot must already be unlocked (the greenhouse enforces this before
     * calling).
     *
     * @param plantType         the type of plant that was grown
     * @param isMarigold        whether the plant is a marigold
     * @param growthDurationHours hours until fully grown (matches the
     *                            plant type's rules)
     * @param plantingTime      the wall-clock time the seed was planted
     */
    public void restore(String plantType, boolean isMarigold,
                        float growthDurationHours,
                        LocalDateTime plantingTime) {
        if (state == PotState.LOCKED) {
            return;
        }
        this.plantType = plantType;
        this.isMarigold = isMarigold;
        this.growthDurationHours = growthDurationHours;
        this.plantingTime = plantingTime;
        this.state = PotState.GROWING;
        refreshState(); // promote to READY if enough time has already elapsed
    }

    /**
     * Checks if the plant in this pot has finished growing.
     *
     * @return true if the plant is fully grown and ready for harvest
     */
    public boolean isReady() {
        refreshState();
        return state == PotState.READY;
    }

    /**
     * Calculates the remaining hours until the plant is fully grown.
     *
     * @return remaining growth hours, or 0 if already ready or not growing
     */
    public float getRemainingGrowthHours() {
        refreshState();
        if (state != PotState.GROWING) {
            return 0;
        }
        float elapsedHours = elapsedHoursSincePlanting();
        float remaining = growthDurationHours - elapsedHours;
        return Math.max(0, remaining);
    }

    /**
     * Returns the gem cost (ceiling of remaining growth hours) to instantly
     * finish growing the plant in this pot, or 0 if the pot is not
     * currently growing. Does not mutate state.
     *
     * @return the gem cost, or 0 if not applicable
     */
    public int accelerationCost() {
        refreshState();
        if (state != PotState.GROWING) {
            return 0;
        }
        return (int) Math.ceil(getRemainingGrowthHours());
    }

    /**
     * Commits a {@code grow} action: marks the plant as fully grown
     * immediately. The caller is responsible for verifying the player can
     * afford the cost returned by {@link #accelerationCost()} and for
     * deducting the gems.
     *
     * @return true if the plant was successfully grown
     */
    public boolean accelerateGrowth() {
        refreshState();
        if (state != PotState.GROWING) {
            return false;
        }
        this.state = PotState.READY;
        return true;
    }

    /**
     * Harvests the plant from this pot, resetting it to {@link PotState#EMPTY}.
     * Returns the plant type. Note: check {@link #isMarigold()} before
     * calling this, since it resets the marigold flag along with the rest
     * of the pot state.
     *
     * @return the type of plant that was harvested, or {@code null} if not ready
     */
    public String harvest() {
        refreshState();
        if (state != PotState.READY) {
            return null;
        }
        String harvestedType = this.plantType;
        this.plantType = null;
        this.plantingTime = null;
        this.growthDurationHours = 0;
        this.isMarigold = false;
        this.state = PotState.EMPTY;
        return harvestedType;
    }

    /**
     * Promotes a {@link PotState#GROWING} pot to {@link PotState#READY}
     * if enough time has elapsed since planting.
     */
    private void refreshState() {
        if (state == PotState.GROWING && plantingTime != null
                && elapsedHoursSincePlanting() >= growthDurationHours) {
            state = PotState.READY;
        }
    }

    private float elapsedHoursSincePlanting() {
        if (plantingTime == null) {
            return 0;
        }
        long elapsedMinutes = Duration.between(plantingTime, LocalDateTime.now()).toMinutes();
        return elapsedMinutes / 60f;
    }

    // --- Getters ---

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public PotState getState() {
        return state;
    }

    public String getPlantType() {
        return plantType;
    }

    public LocalDateTime getPlantingTime() {
        return plantingTime;
    }

    public float getGrowthDurationHours() {
        return growthDurationHours;
    }

    public boolean isMarigold() {
        return isMarigold;
    }

    // --- Setters (for persistence / framework use) ---

    public void setState(PotState state) {
        this.state = state;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public void setPlantingTime(LocalDateTime plantingTime) {
        this.plantingTime = plantingTime;
    }

    public void setGrowthDurationHours(float growthDurationHours) {
        this.growthDurationHours = growthDurationHours;
    }

    public void setMarigold(boolean marigold) {
        isMarigold = marigold;
    }
}
