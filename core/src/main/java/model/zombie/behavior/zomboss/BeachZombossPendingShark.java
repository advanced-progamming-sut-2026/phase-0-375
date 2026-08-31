package model.zombie.behavior.zomboss;

/**
 * A baby-shark projectile launched by {@link BeachZombossBehavior}.
 */
public final class BeachZombossPendingShark {

    public enum Phase {
        WALK,
        SUBMERGE,
        ATTACK,
        DONE
    }

    private final int row;
    private final int col;
    private final float walkSeconds;
    private final float submergeSeconds;
    private final float attackSeconds;
    private float phaseElapsed;
    private Phase phase = Phase.WALK;
    private boolean resolved;

    public BeachZombossPendingShark(int row, int col,
                                    float walkSeconds, float submergeSeconds, float attackSeconds) {
        this.row = row;
        this.col = col;
        this.walkSeconds = Math.max(0.05f, walkSeconds);
        this.submergeSeconds = Math.max(0.05f, submergeSeconds);
        this.attackSeconds = Math.max(0.05f, attackSeconds);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isResolved() {
        return resolved;
    }

    public float phaseProgress01() {
        float total = switch (phase) {
            case WALK -> walkSeconds;
            case SUBMERGE -> submergeSeconds;
            case ATTACK -> attackSeconds;
            default -> 1f;
        };
        return Math.min(1f, phaseElapsed / total);
    }

    public float getWalkSeconds() {
        return walkSeconds;
    }

    /**
     * @return true when the shark finishes its attack and the target plant
     *         should be destroyed
     */
    boolean tick(float deltaTime) {
        if (resolved) {
            return false;
        }
        phaseElapsed += deltaTime;
        float limit = switch (phase) {
            case WALK -> walkSeconds;
            case SUBMERGE -> submergeSeconds;
            case ATTACK -> attackSeconds;
            default -> 0f;
        };
        if (phaseElapsed < limit) {
            return false;
        }
        return advancePhase();
    }

    private boolean advancePhase() {
        phaseElapsed = 0f;
        return switch (phase) {
            case WALK -> {
                phase = Phase.SUBMERGE;
                yield false;
            }
            case SUBMERGE -> {
                phase = Phase.ATTACK;
                yield false;
            }
            case ATTACK -> {
                phase = Phase.DONE;
                resolved = true;
                yield true;
            }
            default -> false;
        };
    }
}
