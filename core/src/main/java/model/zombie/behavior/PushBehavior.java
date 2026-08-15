package model.zombie.behavior;

import model.enums.PushableItemType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.item.pushable.IceBlock;
import model.item.pushable.Pushable;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Push behavior.
 */
public class PushBehavior implements ZombieBehavior {

    // --- Constants ---

    /**
     * Seconds the zombie spends "transferring force" before the pushable
     * snaps one cell forward. During this window the zombie is stationary
     * (state = PUSHING).
     */
    public static final float PUSH_DURATION = 0.5f;

    /** Arcade {@code push} clip length; art sets the pace. */
    public static final float ARCADE_PUSH_DURATION = 4.0333f;

    /**
     * Centre of the tile to the right of the cabinet. The cabinet stays on its
     * tile; the zombie plays {@code push} when its origin reaches that centre.
     */
    public static final float ARCADE_HAND_REACH_TILES = 1f;

    /**
     * Past {@link #ARCADE_HAND_REACH_TILES} so the zombie walks into the first push.
     */
    public static final float ARCADE_SPAWN_PAST_BORDER = 0.12f;

    /**
     * Damage dealt to a hypnotized zombie crushed by a pushable. Intentionally
     * above any zombie's HP + armor total to guarantee a one-shot kill.
     */
    public static final int CRUSH_DAMAGE = 6767;

    // --- State ---

    /** Current phase of the push cycle. */
    private PushPhase phase = PushPhase.WALKING;

    /** Seconds elapsed in the current PUSH phase. */
    private float pushTimer = 0f;

    /**
     * For Troglobite: how many ice blocks are still waiting in reserve
     * (excluding the one currently being pushed). When the current
     * pushable is destroyed, if this is greater than zero, a new ice block
     * is spawned and the zombie resumes pushing.
     */
    private int sparePushablesRemaining = 0;

    /** True once we've initialized {@link #sparePushablesRemaining} from the definition. */
    private boolean pushableReserveInitialized = false;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }
        initPushableReserveIfNeeded(zombie);

        Pushable pushable = zombie.getPushableItem();

        // If the pushable was destroyed, drop it and either spawn the
        // next spare or let the zombie walk freely.
        if (pushable == null || pushable.isDestroyed()) {
            handleLostPushable(zombie, pushable);
            return;
        }

        // Initialize the pushable's grid position on the first tick.
        if (pushable.getPosition() == null) {
            placePushableOnSpawn(zombie, pushable);
        }

        switch (phase) {
            case WALKING:
                tickWalking(zombie, context, deltaTime, pushable);
                break;
            case PUSHING:
                tickPushing(zombie, context, deltaTime, pushable);
                break;
            default:
                break;
        }
    }

    /** Reads the zombie's spare-pushable reserve from its definition once. */
    private void initPushableReserveIfNeeded(ZombieInstance zombie) {
        if (pushableReserveInitialized) return;
        int total = zombie.getDefinition().getBehaviorPropInt(
                "NumberOfIceblocksToSpawnWith", 1);
        sparePushablesRemaining = Math.max(0, total - 1);
        pushableReserveInitialized = true;
    }

    /**
     * Called when the current pushable is gone: notifies its destruction,
     * attaches the next spare ice block if one remains, and otherwise
     * resets the zombie to plain walking.
     */
    private void handleLostPushable(ZombieInstance zombie, Pushable pushable) {
        if (pushable != null && pushable.isDestroyed()) {
            pushable.onDestroyed(); // idempotent notification
        }
        if (sparePushablesRemaining > 0) {
            // Spawn the next ice block in front of the zombie.
            Pushable next = createSparePushable(zombie);
            if (next != null) {
                sparePushablesRemaining--;
                zombie.setPushableItem(next);
                next.setPusher(zombie);
                resetToWalking(zombie);
                return;
            }
        }
        resetToWalking(zombie);
    }

    /**
     * Cabinet on the spawn tile; zombie just past hand-contact so it walks in.
     * Other pushers keep the pushable one cell ahead.
     */
    private void placePushableOnSpawn(ZombieInstance zombie, Pushable pushable) {
        int row = zombie.getGridY();
        int spawnCol = zombie.getGridX();
        if (isArcade(zombie)) {
            pushable.setPosition(new Point(spawnCol, row));
            float x = spawnCol + ARCADE_HAND_REACH_TILES + ARCADE_SPAWN_PAST_BORDER;
            if (zombie.getContinuousPosition() != null) {
                zombie.setContinuousX(x);
            } else {
                zombie.setContinuousPosition(new FloatPoint(x, row));
            }
            return;
        }
        int initCol = Math.max(0, spawnCol - 1);
        pushable.setPosition(new Point(initCol, row));
    }

    static boolean isArcade(ZombieInstance zombie) {
        return zombie.getDefinition() != null
                && zombie.getDefinition().getPushableItemType() == PushableItemType.ARCADE_MACHINE;
    }

    /**
     * Zombie origin has reached the centre of the tile to the right of the cabinet.
     */
    static boolean arcadeHandReachesCabinet(float zombieX, int cabinetCol) {
        return zombieX <= cabinetCol + ARCADE_HAND_REACH_TILES;
    }

    /** Leaves the PUSHING state and restarts the walk cycle. */
    private void resetToWalking(ZombieInstance zombie) {
        if (zombie.getState() == ZombieState.PUSHING) {
            zombie.setState(ZombieState.WALKING);
        }
        phase = PushPhase.WALKING;
        pushTimer = 0f;
    }

    /** Constructs a fresh ice block for the Troglobite's spare-reserve mechanic. */
    private Pushable createSparePushable(ZombieInstance zombie) {
        PushableItemType type = zombie.getDefinition().getPushableItemType();
        if (type == PushableItemType.ICE_BLOCK) {
            return new IceBlock(600);
        }
        return null;
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.PUSH;
    }

    // --- WALK phase ---

    /**
     * The zombie is walking toward the pushable.
     * Movement is handled by {@code ZombieSystem.moveZombie};
     * this method only checks whether the zombie has caught up.
     */
    private void tickWalking(ZombieInstance zombie, BehaviorContext context, float deltaTime, Pushable pushable) {
        // Make sure the zombie is in a movement-allowed state so
        // ZombieSystem.moveZombie can advance it.
        if (zombie.getState() == ZombieState.PUSHING
                || zombie.getState() == ZombieState.SPECIAL_ACTION) {
            zombie.setState(ZombieState.WALKING);
        }

        if (isArcade(zombie)) {
            if (pushable.getCol() <= 0) {
                return;
            }
            if (arcadeHandReachesCabinet(zombie.getContinuousX(), pushable.getCol())) {
                phase = PushPhase.PUSHING;
                pushTimer = 0f;
                zombie.setState(ZombieState.PUSHING);
            }
            return;
        }

        int zombieCol = zombie.getGridX();
        int pushableCol = pushable.getCol();

        // Zombie has caught up when the pushable is exactly one cell ahead.
        if (zombieCol - 1 == pushableCol) {
            phase = PushPhase.PUSHING;
            pushTimer = 0f;
            zombie.setState(ZombieState.PUSHING);
        }
    }

    // --- PUSH phase ---

    /**
     * The zombie is in contact with the pushable and transferring force.
     * After the push duration the item snaps one cell forward. Arcade holds
     * for the {@code push} clip so the cabinet sprite can follow the arm.
     */
    private void tickPushing(ZombieInstance zombie, BehaviorContext context,
                             float deltaTime, Pushable pushable) {
        // Ensure the zombie stays stationary (moveZombie skips PUSHING).
        if (zombie.getState() != ZombieState.PUSHING) {
            zombie.setState(ZombieState.PUSHING);
        }

        pushTimer += deltaTime;
        float duration = isArcade(zombie) ? ARCADE_PUSH_DURATION : PUSH_DURATION;
        if (pushTimer < duration) {
            return;
        }

        pushTimer = 0f;
        shoveForward(zombie, context, pushable);
        phase = PushPhase.WALKING;
        zombie.setState(ZombieState.WALKING);
    }

    /** Snaps the pushable one cell toward the house and crushes that cell. */
    private void shoveForward(ZombieInstance zombie, BehaviorContext context, Pushable pushable) {
        int row = zombie.getGridY();
        int newCol = pushable.getCol() - 1;
        if (newCol < 0) {
            return;
        }
        pushable.setPosition(new Point(newCol, row));
        pushable.push();
        crushPlantIfAny(pushable, context, row, newCol);
        crushHypnotizedZombies(pushable, context, row, newCol);
    }

    // --- Crush helpers ---

    /**
     * If a live plant occupies the pushable's new cell and the pushable
     * {@link Pushable#killsOnContact()}, instantly
     * destroys that plant.
     */
    private void crushPlantIfAny(Pushable pushable, BehaviorContext context,
                                 int row, int targetCol) {
        if (!pushable.killsOnContact()) {
            return;
        }
        PlantInstance plant = context.getPlantAt(row, targetCol);
        if (plant == null || plant.getCurrentHP() <= 0) {
            return;
        }
        pushable.onCrushPlant();
        context.destroyPlant(plant);
    }

    /**
     * For every hypnotized zombie occupying the pushable's new cell,
     * instantly kills it. The pusher itself is never a valid target.
     */
    private void crushHypnotizedZombies(Pushable pushable, BehaviorContext context,
                                        int row, int targetCol) {
        if (!pushable.killsOnContact()) {
            return;
        }
        ZombieInstance pusher = pushable.getPusher();

        List<ZombieInstance> zombiesInLane = context.getZombiesInLane(row);
        for (ZombieInstance other : zombiesInLane) {
            if (other == null || other == pusher || other.isDead()) {
                continue;
            }
            if (other.getState() != ZombieState.HYPNOTIZED) {
                continue;
            }
            Point pos = other.getGridPosition();
            if (pos == null || pos.getX() != targetCol) {
                continue;
            }
            pushable.onCrushHypnotizedZombie();
            context.damageZombie(other, CRUSH_DAMAGE);
        }
    }

    // --- Public queries ---

    /** @return true while the zombie is in the PUSH phase (stationary, applying force). */
    public boolean isPushing() {
        return phase == PushPhase.PUSHING;
    }

    /** @return true while the zombie is in the WALK phase (moving toward the pushable). */
    public boolean isWalking() {
        return phase == PushPhase.WALKING;
    }

    // --- Getters / setters ---

    public PushPhase getPhase() {
        return phase;
    }

    public void setPhase(PushPhase phase) {
        this.phase = phase;
    }

    public float getPushTimer() {
        return pushTimer;
    }

    public void setPushTimer(float pushTimer) {
        this.pushTimer = pushTimer;
    }

    public int getSparePushablesRemaining() {
        return sparePushablesRemaining;
    }

    public void setSparePushablesRemaining(int sparePushablesRemaining) {
        this.sparePushablesRemaining = sparePushablesRemaining;
    }

    // --- Inner types ---

    /**
     * The two phases of the push cycle.
     */
    public enum PushPhase {
        WALKING, // Zombie is walking toward the pushable
        PUSHING // Zombie is in contact with the pushable, applying force
    }
}