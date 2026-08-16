package model.zombie.behavior;

import model.enums.GroundType;
import model.enums.PushableItemType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.map.terrain.NormalTerrainStrategy;
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

    /** Arcade / Troglobite {@code push} clip length; art sets the pace. */
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
     * Tiles from zombie origin to the barrel centre (toward the house). Renderer
     * overwrites from PAM {@code partBounds}; tests keep the one-tile default.
     */
    private float barrelFrontOffsetTiles = 1f;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        Pushable pushable = zombie.getPushableItem();

        if (pushable == null || pushable.isDestroyed()) {
            handleLostPushable(zombie, pushable);
            if (isIce(zombie)) {
                tickHuntIce(zombie, context);
            }
            return;
        }

        // Initialize the pushable's grid position on the first tick.
        if (pushable.getPosition() == null) {
            placePushableOnSpawn(zombie, pushable);
        }

        if (isPiano(zombie)) {
            tickPiano(zombie, context, pushable);
            return;
        }
        if (isBarrel(zombie)) {
            tickBarrel(zombie, context, pushable);
            return;
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

    /**
     * Called when the current pushable is gone. Troglobite does not spawn a
     * replacement; {@link #tickHuntIce} looks for ice already on the lawn.
     */
    private void handleLostPushable(ZombieInstance zombie, Pushable pushable) {
        if (pushable != null && pushable.isDestroyed()) {
            pushable.onDestroyed(); // idempotent notification
        }
        resetToWalking(zombie);
    }

    /**
     * Walk until ice already on the lawn sits in the tile ahead, then claim it
     * and shove like Arcade (hand-reach at {@code iceCol + 1}).
     */
    private void tickHuntIce(ZombieInstance zombie, BehaviorContext context) {
        if (zombie.getState() == ZombieState.PUSHING
                || zombie.getState() == ZombieState.SPECIAL_ACTION) {
            zombie.setState(ZombieState.WALKING);
        }
        int iceCol = findIceAhead(zombie, context);
        if (iceCol < 0) {
            return;
        }
        if (!arcadeHandReachesCabinet(zombie.getContinuousX(), iceCol)) {
            return;
        }
        IceBlock block = claimIce(zombie, context, iceCol);
        if (block == null) {
            return;
        }
        phase = PushPhase.PUSHING;
        pushTimer = 0f;
        zombie.setState(ZombieState.PUSHING);
    }

    /** Nearest ice toward the house (largest column still left of the origin). */
    static int findIceAhead(ZombieInstance zombie, BehaviorContext context) {
        float x = zombie.getContinuousX();
        int row = zombie.getGridY();
        int best = -1;
        int cols = context.getColumnCount();
        for (int col = 0; col < cols; col++) {
            if (col >= x) {
                continue;
            }
            if (hasIceAt(context, row, col)) {
                best = col;
            }
        }
        return best;
    }

    static boolean hasIceAt(BehaviorContext context, int row, int col) {
        Cell cell = context.getCellAt(row, col);
        if (cell != null && cell.getTerrainStrategy() instanceof IceTerrainStrategy ice
                && !ice.isMelted()) {
            return true;
        }
        List<Pushable> orphans = context.getOrphanedPushables();
        if (orphans == null) {
            return false;
        }
        for (Pushable item : orphans) {
            if (item instanceof IceBlock block
                    && !block.isDestroyed()
                    && block.getPusher() == null
                    && block.getRow() == row
                    && block.getCol() == col) {
                return true;
            }
        }
        return false;
    }

    private IceBlock claimIce(ZombieInstance zombie, BehaviorContext context, int col) {
        int row = zombie.getGridY();
        IceBlock orphan = takeOrphanIce(context, row, col);
        if (orphan != null) {
            orphan.setPusher(zombie);
            zombie.setPushableItem(orphan);
            if (orphan.getPosition() == null) {
                orphan.setPosition(new Point(col, row));
            }
            return orphan;
        }
        Cell cell = context.getCellAt(row, col);
        if (cell == null || !(cell.getTerrainStrategy() instanceof IceTerrainStrategy ice)
                || ice.isMelted()) {
            return null;
        }
        IceBlock block = new IceBlock(Math.max(1, ice.getHp()));
        block.setContainedEntity(ice.getContainedEntity());
        ice.setContainedEntity(null);
        cell.setGroundType(GroundType.NORMAL);
        cell.setTerrainStrategy(new NormalTerrainStrategy());
        block.setPosition(new Point(col, row));
        block.setPusher(zombie);
        zombie.setPushableItem(block);
        return block;
    }

    private static IceBlock takeOrphanIce(BehaviorContext context, int row, int col) {
        List<Pushable> orphans = context.getOrphanedPushables();
        if (orphans == null) {
            return null;
        }
        for (Pushable item : orphans) {
            if (item instanceof IceBlock block
                    && !block.isDestroyed()
                    && block.getPusher() == null
                    && block.getRow() == row
                    && block.getCol() == col) {
                context.removeOrphanedPushable(block);
                return block;
            }
        }
        return null;
    }

    /** Cabinet on the spawn tile; zombie just past hand-contact so it walks in. */
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
        if (isPiano(zombie)) {
            pushable.setPosition(new Point(spawnCol, row));
            return;
        }
        if (isIce(zombie)) {
            return;
        }
        int initCol = Math.max(0, spawnCol - 1);
        pushable.setPosition(new Point(initCol, row));
    }

    static boolean isArcade(ZombieInstance zombie) {
        return zombie.getDefinition() != null
                && zombie.getDefinition().getPushableItemType() == PushableItemType.ARCADE_MACHINE;
    }

    static boolean isIce(ZombieInstance zombie) {
        return zombie.getDefinition() != null
                && zombie.getDefinition().getPushableItemType() == PushableItemType.ICE_BLOCK;
    }

    /** Cabinet and ice follow the {@code push} arm; snap when that clip ends. */
    static boolean usesArmPushClip(ZombieInstance zombie) {
        return isArcade(zombie) || isIce(zombie);
    }

    static boolean isPiano(ZombieInstance zombie) {
        return zombie.getDefinition() != null
                && zombie.getDefinition().getPushableItemType() == PushableItemType.PIANO;
    }

    static boolean isBarrel(ZombieInstance zombie) {
        return zombie.getDefinition() != null
                && zombie.getDefinition().getPushableItemType() == PushableItemType.BARREL;
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

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.PUSH;
    }

    /** Ice stays on the lawn if Troglobite dies mid-push. */
    @Override
    public void onZombieDeath(ZombieInstance zombie, BehaviorContext context) {
        if (zombie == null || context == null || !isIce(zombie)) {
            return;
        }
        Pushable pushable = zombie.getPushableItem();
        if (pushable == null || pushable.isDestroyed()) {
            return;
        }
        zombie.setPushableItem(null);
        pushable.setPusher(null);
        context.orphanPushable(pushable);
    }

    /**
     * Piano rides the same tile as the pianist. Stay walking so they move
     * linearly; crush whatever shares that cell.
     */
    private void tickPiano(ZombieInstance zombie, BehaviorContext context, Pushable pushable) {
        if (zombie.getState() == ZombieState.PUSHING
                || zombie.getState() == ZombieState.SPECIAL_ACTION) {
            zombie.setState(ZombieState.WALKING);
        }
        int row = zombie.getGridY();
        int col = zombie.getGridX();
        Point here = new Point(col, row);
        Point old = pushable.getPosition();
        if (old == null || old.getX() != col || old.getY() != row) {
            pushable.setPosition(here);
            pushable.push();
        }
        crushPlantIfAny(pushable, context, row, col);
        crushHypnotizedZombies(pushable, context, row, col);
    }

    /**
     * Barrel rides in front of the pusher. Occupancy is {@code round(zombieX - offset)}
     * so a plant dies when the barrel centre crosses the tile edge (halfway).
     */
    private void tickBarrel(ZombieInstance zombie, BehaviorContext context, Pushable pushable) {
        if (zombie.getState() == ZombieState.PUSHING
                || zombie.getState() == ZombieState.SPECIAL_ACTION) {
            zombie.setState(ZombieState.WALKING);
        }
        int row = zombie.getGridY();
        int col = Math.round(zombie.getContinuousX() - barrelFrontOffsetTiles);
        if (col < 0) {
            col = 0;
        }
        Point here = new Point(col, row);
        Point old = pushable.getPosition();
        if (old == null || old.getX() != col || old.getY() != row) {
            pushable.setPosition(here);
            pushable.push();
        }
        crushPlantIfAny(pushable, context, row, col);
        crushHypnotizedZombies(pushable, context, row, col);
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

        if (isArcade(zombie) || isIce(zombie)) {
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
     * After the push duration the item snaps one cell forward. Arcade and
     * Troglobite hold for the {@code push} clip so the prop can follow the arm.
     */
    private void tickPushing(ZombieInstance zombie, BehaviorContext context,
                             float deltaTime, Pushable pushable) {
        // Ensure the zombie stays stationary (moveZombie skips PUSHING).
        if (zombie.getState() != ZombieState.PUSHING) {
            zombie.setState(ZombieState.PUSHING);
        }

        pushTimer += deltaTime;
        float duration = usesArmPushClip(zombie) ? ARCADE_PUSH_DURATION : PUSH_DURATION;
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

    public float getBarrelFrontOffsetTiles() {
        return barrelFrontOffsetTiles;
    }

    public void setBarrelFrontOffsetTiles(float barrelFrontOffsetTiles) {
        if (barrelFrontOffsetTiles > 0.05f) {
            this.barrelFrontOffsetTiles = barrelFrontOffsetTiles;
        }
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