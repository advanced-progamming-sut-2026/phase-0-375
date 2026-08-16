package model.zombie.behavior;

import model.enums.Chapter;
import model.enums.GroundType;
import model.enums.PlantCategory;
import model.enums.PushableItemType;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.game.map.terrain.IceTerrainStrategy;
import model.item.pushable.ArcadeMachine;
import model.item.pushable.Barrel;
import model.item.pushable.IceBlock;
import model.item.pushable.Piano;
import model.item.pushable.Pushable;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcadePushTest {

    private static final float TICK = 0.1f;
    private static final int SPAWN_COL = 5;

    @Test
    void spawnPlacesCabinetOnTileAndZombiePastBorder() {
        ArcadeMachine cabinet = new ArcadeMachine(1290);
        ZombieInstance zombie = arcadeAt(SPAWN_COL, cabinet);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);

        push.execute(zombie, stubContext(null), TICK);

        assertEquals(SPAWN_COL, cabinet.getCol());
        assertEquals(SPAWN_COL + PushBehavior.ARCADE_HAND_REACH_TILES
                        + PushBehavior.ARCADE_SPAWN_PAST_BORDER,
                zombie.getContinuousX(), 1e-4f);
        assertEquals(PushBehavior.PushPhase.WALKING, push.getPhase());
        assertFalse(push.isPushing());
    }

    @Test
    void handReachStartsPushThenCabinetMovesAfterClip() {
        PlantInstance plant = wallnut();
        ArcadeMachine cabinet = new ArcadeMachine(1290);
        ZombieInstance zombie = arcadeAt(SPAWN_COL, cabinet);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        BehaviorContext context = stubContext(plant);

        push.execute(zombie, context, TICK);
        zombie.setContinuousX(SPAWN_COL + PushBehavior.ARCADE_HAND_REACH_TILES);
        push.execute(zombie, context, TICK);

        assertTrue(push.isPushing());
        assertEquals(ZombieState.PUSHING, zombie.getState());
        assertEquals(SPAWN_COL, cabinet.getCol(), "cabinet stays put while the arm shoves");
        assertTrue(plant.getCurrentHP() > 0, "plant lives until the cabinet arrives");

        runFor(push, zombie, context, PushBehavior.ARCADE_PUSH_DURATION - TICK);
        assertTrue(push.isPushing(), "push clip still playing");
        assertEquals(SPAWN_COL, cabinet.getCol());

        push.execute(zombie, context, TICK);
        assertEquals(SPAWN_COL - 1, cabinet.getCol());
        assertTrue(plant.getCurrentHP() <= 0, "plant on the next tile is crushed");
        assertEquals(PushBehavior.PushPhase.WALKING, push.getPhase());
        assertEquals(ZombieState.WALKING, zombie.getState());
    }

    @Test
    void secondReachStartsAnotherPush() {
        ArcadeMachine cabinet = new ArcadeMachine(1290);
        ZombieInstance zombie = arcadeAt(SPAWN_COL, cabinet);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        BehaviorContext context = stubContext(null);

        push.execute(zombie, context, TICK);
        zombie.setContinuousX(SPAWN_COL + PushBehavior.ARCADE_HAND_REACH_TILES);
        push.execute(zombie, context, TICK);
        runFor(push, zombie, context, PushBehavior.ARCADE_PUSH_DURATION);

        assertEquals(SPAWN_COL - 1, cabinet.getCol());
        assertFalse(push.isPushing());

        zombie.setContinuousX(SPAWN_COL - 1 + PushBehavior.ARCADE_HAND_REACH_TILES);
        push.execute(zombie, context, TICK);
        assertTrue(push.isPushing(), "catching up to the cabinet starts push again");
    }

    @Test
    void destroyingCabinetStopsPushing() {
        ArcadeMachine cabinet = new ArcadeMachine(1290);
        ZombieInstance zombie = arcadeAt(SPAWN_COL, cabinet);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        BehaviorContext context = stubContext(null);

        push.execute(zombie, context, TICK);
        zombie.setContinuousX(SPAWN_COL + PushBehavior.ARCADE_HAND_REACH_TILES);
        push.execute(zombie, context, TICK);
        assertTrue(push.isPushing());

        cabinet.takeDamage(cabinet.getHp());
        push.execute(zombie, context, TICK);

        assertFalse(push.isPushing());
        assertEquals(ZombieState.WALKING, zombie.getState());
        assertNull(zombie.getPushableItem());
    }

    @Test
    void troglobiteWalksUntilIceThenPushesLikeArcade() {
        IceTerrainStrategy ice = new IceTerrainStrategy();
        int iceCol = SPAWN_COL - 2;
        Cell cell = cellWithIce(iceCol, ice);
        ZombieInstance zombie = troglobiteAt(SPAWN_COL);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        PlantInstance plant = wallnut();
        BehaviorContext context = iceContext(cell, iceCol, plant, iceCol - 1);

        push.execute(zombie, context, TICK);
        assertNull(zombie.getPushableItem(), "does not spawn ice");
        assertFalse(push.isPushing());
        assertEquals(GroundType.ICE, cell.getGroundType());

        zombie.setContinuousX(iceCol + PushBehavior.ARCADE_HAND_REACH_TILES);
        push.execute(zombie, context, TICK);

        Pushable claimed = zombie.getPushableItem();
        assertTrue(claimed instanceof IceBlock);
        assertEquals(iceCol, claimed.getCol());
        assertTrue(push.isPushing());
        assertEquals(GroundType.NORMAL, cell.getGroundType(), "claimed ice leaves the tile");
        assertTrue(plant.getCurrentHP() > 0, "plant lives until the ice arrives");

        runFor(push, zombie, context, PushBehavior.ARCADE_PUSH_DURATION - TICK);
        assertEquals(iceCol, claimed.getCol());
        push.execute(zombie, context, TICK);
        assertEquals(iceCol - 1, claimed.getCol());
        assertTrue(plant.getCurrentHP() <= 0);
        assertEquals(PushBehavior.PushPhase.WALKING, push.getPhase());
    }

    @Test
    void pianoSpawnsOnSameTileAndStaysWalking() {
        Piano piano = new Piano(840);
        ZombieInstance zombie = pianistAt(SPAWN_COL, piano);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);

        push.execute(zombie, stubContext(null), TICK);

        assertEquals(SPAWN_COL, piano.getCol());
        assertEquals(SPAWN_COL, zombie.getContinuousX(), 1e-4f);
        assertNotEquals(ZombieState.PUSHING, zombie.getState());
        assertFalse(push.isPushing());
    }

    @Test
    void pianoFollowsGridAndCrushesCurrentTile() {
        PlantInstance plant = wallnut();
        Piano piano = new Piano(840);
        ZombieInstance zombie = pianistAt(SPAWN_COL, piano);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        BehaviorContext context = stubContextAt(plant, SPAWN_COL);

        push.execute(zombie, context, TICK);
        assertEquals(SPAWN_COL, piano.getCol());
        assertTrue(plant.getCurrentHP() <= 0, "plant on the shared tile is crushed");

        PlantInstance next = wallnut();
        context = stubContextAt(next, SPAWN_COL - 1);
        zombie.setGridX(SPAWN_COL - 1);
        zombie.setContinuousX(SPAWN_COL - 1);
        push.execute(zombie, context, TICK);
        assertEquals(SPAWN_COL - 1, piano.getCol());
        assertTrue(next.getCurrentHP() <= 0);
        assertNotEquals(ZombieState.PUSHING, zombie.getState());
    }

    @Test
    void barrelCrushesPlantOnTileItEnters() {
        PlantInstance plant = wallnut();
        Barrel barrel = new Barrel(600);
        ZombieInstance zombie = barrelAt(SPAWN_COL, barrel);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        BehaviorContext context = stubContextAt(plant, SPAWN_COL - 1);

        push.execute(zombie, context, TICK);
        assertEquals(SPAWN_COL - 1, barrel.getCol());
        assertTrue(plant.getCurrentHP() <= 0, "plant on the barrel's tile is crushed");
        assertFalse(push.isPushing());

        PlantInstance next = wallnut();
        context = stubContextAt(next, SPAWN_COL - 2);
        zombie.setGridX(SPAWN_COL - 1);
        zombie.setContinuousX(SPAWN_COL - 1f);
        push.execute(zombie, context, TICK);
        assertEquals(SPAWN_COL - 2, barrel.getCol());
        assertTrue(next.getCurrentHP() <= 0);
    }

    @Test
    void barrelOccupancyFollowsPartBoundsOffset() {
        Barrel barrel = new Barrel(600);
        ZombieInstance zombie = barrelAt(SPAWN_COL, barrel);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        push.setBarrelFrontOffsetTiles(1.6f);
        push.execute(zombie, stubContext(null), TICK);
        assertEquals(Math.round(SPAWN_COL - 1.6f), barrel.getCol());
    }

    @Test
    void barrelWaitsUntilCentrePassesTileEdge() {
        PlantInstance plant = wallnut();
        Barrel barrel = new Barrel(600);
        ZombieInstance zombie = barrelAt(SPAWN_COL, barrel);
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        BehaviorContext context = stubContextAt(plant, SPAWN_COL - 1);

        push.setBarrelFrontOffsetTiles(0.4f);
        push.execute(zombie, context, TICK);
        assertEquals(SPAWN_COL, barrel.getCol());
        assertTrue(plant.getCurrentHP() > 0, "lip in the next tile is not enough");

        push.setBarrelFrontOffsetTiles(0.6f);
        push.execute(zombie, context, TICK);
        assertEquals(SPAWN_COL - 1, barrel.getCol());
        assertTrue(plant.getCurrentHP() <= 0, "centre past the tile edge crushes");
    }

    private static ZombieInstance arcadeAt(int col, ArcadeMachine cabinet) {
        Zombie definition = new Zombie(
                "ZombieArcade", 1290, 0.16f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 1000, 1, List.of(),
                PushableItemType.ARCADE_MACHINE, null,
                List.of(ZombieBehaviorType.PUSH));
        ZombieInstance zombie = new ZombieInstance(definition, List.of(), cabinet);
        cabinet.setPusher(zombie);
        zombie.setGridPosition(new Point(col, 0));
        zombie.setContinuousPosition(new FloatPoint(col, 0));
        return zombie;
    }

    private static ZombieInstance troglobiteAt(int col) {
        Zombie definition = new Zombie(
                "ZombieIceAgeTroglobite", 470, 0.185f, 100f, ZombieSize.NORMAL,
                Chapter.FROSTBITE_CAVES, 600, 1, List.of(),
                PushableItemType.ICE_BLOCK, null,
                List.of(ZombieBehaviorType.PUSH));
        ZombieInstance zombie = new ZombieInstance(definition, List.of(), null);
        zombie.setGridPosition(new Point(col, 0));
        zombie.setContinuousPosition(new FloatPoint(col, 0));
        return zombie;
    }

    private static Cell cellWithIce(int col, IceTerrainStrategy ice) {
        Cell cell = new Cell(0, col);
        cell.setGroundType(GroundType.ICE);
        cell.setTerrainStrategy(ice);
        return cell;
    }

    private static ZombieInstance pianistAt(int col, Piano piano) {
        Zombie definition = new Zombie(
                "ZombiePiano", 840, 0.12f, 4000f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 450, 1, List.of(),
                PushableItemType.PIANO, null,
                List.of(ZombieBehaviorType.PUSH, ZombieBehaviorType.PIANO_SWAP));
        ZombieInstance zombie = new ZombieInstance(definition, List.of(), piano);
        piano.setPusher(zombie);
        zombie.setGridPosition(new Point(col, 0));
        zombie.setContinuousPosition(new FloatPoint(col, 0));
        return zombie;
    }

    private static ZombieInstance barrelAt(int col, Barrel barrel) {
        Zombie definition = new Zombie(
                "ZombieBarrelRoller", 190, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 300, 1, List.of(),
                PushableItemType.BARREL, null,
                List.of(ZombieBehaviorType.PUSH, ZombieBehaviorType.BARREL_ROLLER));
        ZombieInstance zombie = new ZombieInstance(definition, List.of(), barrel);
        barrel.setPusher(zombie);
        zombie.setGridPosition(new Point(col, 0));
        zombie.setContinuousPosition(new FloatPoint(col, 0));
        return zombie;
    }

    private static PlantInstance wallnut() {
        return new PlantInstance(new Plant(
                1, "Wallnut", PlantCategory.WALL_NUT, List.of(), 50, 400, 0,
                0f, 0f, null, 0f, null, 0f, null));
    }

    private static void runFor(PushBehavior push, ZombieInstance zombie,
                               BehaviorContext context, float seconds) {
        for (float t = 0f; t < seconds; t += TICK) {
            push.execute(zombie, context, TICK);
        }
    }

    private static BehaviorContext stubContext(PlantInstance plant) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "getPlantAt" -> (int) args[1] == SPAWN_COL - 1 ? plant : null;
                    case "destroyPlant" -> {
                        if (args[0] != null) {
                            ((PlantInstance) args[0]).setCurrentHP(0);
                        }
                        yield null;
                    }
                    case "getZombiesInLane" -> List.of();
                    case "getCellAt" -> null;
                    case "getOrphanedPushables" -> List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static BehaviorContext stubContextAt(PlantInstance plant, int plantCol) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "getPlantAt" -> (int) args[1] == plantCol ? plant : null;
                    case "destroyPlant" -> {
                        if (args[0] != null) {
                            ((PlantInstance) args[0]).setCurrentHP(0);
                        }
                        yield null;
                    }
                    case "getZombiesInLane" -> List.of();
                    case "getCellAt" -> null;
                    case "getOrphanedPushables" -> List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static BehaviorContext iceContext(Cell iceCell, int iceCol,
                                             PlantInstance plant, int plantCol) {
        return (BehaviorContext) Proxy.newProxyInstance(
                BehaviorContext.class.getClassLoader(),
                new Class<?>[]{BehaviorContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> 9;
                    case "getCellAt" -> (int) args[0] == 0 && (int) args[1] == iceCol
                            ? iceCell : null;
                    case "getOrphanedPushables" -> List.of();
                    case "removeOrphanedPushable" -> null;
                    case "getPlantAt" -> (int) args[1] == plantCol ? plant : null;
                    case "destroyPlant" -> {
                        if (args[0] != null) {
                            ((PlantInstance) args[0]).setCurrentHP(0);
                        }
                        yield null;
                    }
                    case "getZombiesInLane" -> List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
