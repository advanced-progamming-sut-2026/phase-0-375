package model.network.util;

import model.data.minigame.MiniGameRegistry;
import model.enums.MiniGameType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.minigame.MiniGameLevel;
import model.network.dto.PlantSnapshotDto;
import model.network.dto.ZombieSnapshotDto;
import model.network.enums.PlayerRole;
import model.network.packet.game.GameStateSnapshotPacket;
import model.plant.PlantFactory;
import model.zombie.ZombieFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStateSnapshotApplierTest {

    @BeforeAll
    static void initCatalogs() throws Exception {
        try {
            PlantFactory.init("/assets/data/plants/plants.json");
        } catch (IllegalStateException ignored) {}
        try {
            ZombieFactory.init("/assets/data/zombies/zombies.json", "/assets/data/armor/ArmorTypeData.json");
        } catch (IllegalStateException ignored) {}
        try {
            MiniGameRegistry.init("/assets/data/minigames/minigames.json");
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void applyPlacesPlantsAndZombiesAndSetsRoleSun() throws Exception {
        MiniGameLevel level = MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(level);
        GameStateSnapshotApplier applier = new GameStateSnapshotApplier();

        GameStateSnapshotPacket snap = new GameStateSnapshotPacket(
                1L, 1f, 179f, 200, 75,
                List.of(new PlantSnapshotDto("p1", "Peashooter", 0, 1, 300, 300, "IDLE", false, false, 1)),
                List.of(new ZombieSnapshotDto("z1", "ZombieDefault", 2, 5.4f, 2f, 200, 200, 0, "WALKING", 0.2f,
                        false, false, false, false)),
                List.of(), List.of(), false, null, null
        );

        applier.apply(model, snap, PlayerRole.PLANT);
        assertEquals(200, model.getSunAmount());
        assertEquals(1, model.getAllPlants().size());
        assertEquals(1, model.getActiveZombies().size());
        assertEquals(5.4f, model.getActiveZombies().get(0).getContinuousX(), 0.01f);

        applier.apply(model, snap, PlayerRole.ZOMBIE);
        assertEquals(75, model.getSunAmount());

        // Stable identity: second apply keeps one plant/zombie
        applier.apply(model, snap, PlayerRole.PLANT);
        assertEquals(1, model.getAllPlants().size());
        assertEquals(1, model.getActiveZombies().size());
    }

    @Test
    void applyAllowsZombiePastLeftEdgeWithoutCrashing() throws Exception {
        MiniGameLevel level = MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(level);
        GameStateSnapshotApplier applier = new GameStateSnapshotApplier();

        GameStateSnapshotPacket onLawn = new GameStateSnapshotPacket(
                1L, 1f, 179f, 200, 75,
                List.of(),
                List.of(new ZombieSnapshotDto("z1", "ZombieDefault", 0, 0.2f, 0f, 200, 200, 0, "WALKING", 0.2f,
                        false, false, false, false)),
                List.of(), List.of(), false, null, null
        );
        applier.apply(model, onLawn, PlayerRole.ZOMBIE);

        GameStateSnapshotPacket pastBrain = new GameStateSnapshotPacket(
                2L, 2f, 178f, 200, 75,
                List.of(),
                List.of(new ZombieSnapshotDto("z1", "ZombieDefault", 0, -1.0f, 0f, 200, 200, 0, "EATING", 0.2f,
                        false, false, false, false)),
                List.of(), List.of(), false, null, null
        );
        assertDoesNotThrow(() -> applier.apply(model, pastBrain, PlayerRole.ZOMBIE));
        assertEquals(1, model.getActiveZombies().size());
        assertEquals(-1.0f, model.getActiveZombies().get(0).getContinuousX(), 0.01f);
    }

    @Test
    void applyDoesNotStompActivePlantPresentation() throws Exception {
        MiniGameLevel level = MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(level);
        GameStateSnapshotApplier applier = new GameStateSnapshotApplier();
        PvZGameLoop loop = new PvZGameLoop(model);

        GameStateSnapshotPacket setup = new GameStateSnapshotPacket(
                1L, 1f, 179f, 200, 75,
                List.of(new PlantSnapshotDto("p1", "Peashooter", 0, 1, 300, 300, "IDLE", false, false, 1)),
                List.of(new ZombieSnapshotDto("z1", "ZombieDefault", 0, 3f, 0f, 200, 200, 0, "WALKING", 0.2f,
                        false, false, false, false)),
                List.of(), List.of(), false, null, null
        );
        applier.apply(model, setup, PlayerRole.PLANT);

        for (int i = 0; i < 30; i++) {
            loop.updatePresentation(1f / 30f);
        }

        GameStateSnapshotPacket idleSnap = new GameStateSnapshotPacket(
                2L, 2f, 178f, 200, 75,
                List.of(new PlantSnapshotDto("p1", "Peashooter", 0, 1, 300, 300, "IDLE", false, false, 1)),
                List.of(new ZombieSnapshotDto("z1", "ZombieDefault", 0, 3f, 0f, 200, 200, 0, "WALKING", 0.2f,
                        false, false, false, false)),
                List.of(), List.of(), false, null, null
        );
        applier.apply(model, idleSnap, PlayerRole.PLANT);
        assertTrue(model.getAllPlants().get(0).hasActiveAction() || !model.getProjectiles().isEmpty());
    }

    @Test
    void applySyncsZombieArmorDamageAndPop() throws Exception {
        MiniGameLevel level = MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(level);
        GameStateSnapshotApplier applier = new GameStateSnapshotApplier();

        GameStateSnapshotPacket fullArmor = new GameStateSnapshotPacket(
                1L, 1f, 179f, 200, 75,
                List.of(),
                List.of(new ZombieSnapshotDto("z1", "ZombieArmor1", 0, 5f, 0f, 190, 190, 370, "WALKING", 0.2f,
                        false, false, false, false)),
                List.of(), List.of(), false, null, null
        );
        applier.apply(model, fullArmor, PlayerRole.PLANT);
        assertEquals(1, model.getActiveZombies().size());
        assertEquals(370, model.getActiveZombies().get(0).getTotalArmorHealth());

        GameStateSnapshotPacket damaged = new GameStateSnapshotPacket(
                2L, 2f, 178f, 200, 75,
                List.of(),
                List.of(new ZombieSnapshotDto("z1", "ZombieArmor1", 0, 5f, 0f, 190, 190, 100, "WALKING", 0.2f,
                        false, false, false, false)),
                List.of(), List.of(), false, null, null
        );
        applier.apply(model, damaged, PlayerRole.PLANT);
        assertEquals(100, model.getActiveZombies().get(0).getTotalArmorHealth());
        assertEquals(2, model.getActiveZombies().get(0).getArmors().get(0).getCurrentDamageLayer());

        GameStateSnapshotPacket popped = new GameStateSnapshotPacket(
                3L, 3f, 177f, 200, 75,
                List.of(),
                List.of(new ZombieSnapshotDto("z1", "ZombieArmor1", 0, 5f, 0f, 190, 190, 0, "WALKING", 0.2f,
                        false, false, false, false)),
                List.of(), List.of(), false, null, null
        );
        applier.apply(model, popped, PlayerRole.PLANT);
        assertEquals(0, model.getActiveZombies().get(0).getTotalArmorHealth());
        assertTrue(model.getActiveZombies().get(0).getArmors().isEmpty());
    }
}
