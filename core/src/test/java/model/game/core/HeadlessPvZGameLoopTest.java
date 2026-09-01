package model.game.core;

import model.app.App;
import model.data.minigame.MiniGameRegistry;
import model.enums.MiniGameType;
import model.game.level.minigame.izombie.IZombieLevel;
import model.plant.PlantFactory;
import model.zombie.ZombieFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeadlessPvZGameLoopTest {

    @BeforeAll
    static void initCatalogs() throws Exception {
        PlantFactory.init("/assets/data/plants/plants.json");
        ZombieFactory.init("/assets/data/zombies/zombies.json", "/assets/data/armor/ArmorTypeData.json");
        MiniGameRegistry.init("/assets/data/minigames/minigames.json");
    }

    @Test
    @DisplayName("Headless: GameModel instantiates cleanly when App.getCurrentUser() is null")
    void testHeadlessGameModelInstantiationWithoutUser() throws Exception {
        App.getInstance().setCurrentUser(null);
        assertNull(App.getInstance().getCurrentUser());

        IZombieLevel level = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        assertNotNull(level);

        GameModel model = new GameModel(level);
        assertNotNull(model);
        assertEquals(3, model.getDifficultyLevel(), "Default fallback difficulty should be 3");

        level.onStart(model);
        assertFalse(model.getAllPlants().isEmpty(), "Pre-planted defenses must exist on lawn");
        assertEquals(5, model.getActiveZombies().size(), "5 stationary sun zombies should be placed in col 8");
    }

    @Test
    @DisplayName("Headless: PvZGameLoop advances 1,000 frames headlessly with zero exceptions")
    void testHeadlessSimulation1000Ticks() throws Exception {
        App.getInstance().setCurrentUser(null);

        IZombieLevel level = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(level);
        level.onStart(model);

        PvZGameLoop loop = new PvZGameLoop(model);

        for (int i = 0; i < 1000; i++) {
            loop.update(0.05f);
        }

        assertEquals(1000, model.getTick(), "GameModel must have ticked exactly 1,000 times");
        assertEquals(50.0f, model.getElapsedSeconds(), 0.01f, "Elapsed seconds should be 50.0s");
    }
}
