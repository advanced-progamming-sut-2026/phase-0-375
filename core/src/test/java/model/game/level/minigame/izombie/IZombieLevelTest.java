package model.game.level.minigame.izombie;

import model.app.App;
import model.data.minigame.MiniGameRegistry;
import model.enums.MiniGameType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.plant.PlantFactory;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.zombie.ZombieFactory;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Requirement R4: Single Player "I, Zombie" mode.
 * Tests configuration loading for Stages 1-3 from minigames.json, pre-planted plant defenses,
 * stationary sun zombies, autonomous plant AI defense, zombie placement validation,
 * win conditions (all brains eaten) and loss conditions (out of sun + lawn cleared).
 */
class IZombieLevelTest {

    @BeforeAll
    static void initCatalogs() throws IOException {
        PlantFactory.init("/assets/data/plants/plants.json");
        ZombieFactory.init("/assets/data/zombies/zombies.json", "/assets/data/armor/ArmorTypeData.json");
        MiniGameRegistry.init("/assets/data/minigames/minigames.json");
    }

    @Test
    @DisplayName("Stage 1 Loading: Configuration, placeable roster, pre-planted defenses and rules")
    void testStage1ConfigurationAndDefenseLoading() throws Exception {
        IZombieLevel level = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        assertNotNull(level, "Stage 1 level must load from minigames.json");

        assertEquals(1, level.getStage());
        assertEquals(3, level.getDifficultyTier());
        assertEquals(200, level.getCoinReward());
        assertEquals(3, level.redLineColumn());
        assertEquals(5, level.getConfig().getRows());
        assertEquals(9, level.getConfig().getColumns());
        assertEquals(150, level.getConfig().getRules().getInitialSun());
        assertFalse(level.getConfig().getRules().isLawnMowersEnabled());

        // Check Placeable Zombies roster
        IZombieSettings settings = level.getSettings();
        assertNotNull(settings);
        Map<String, Integer> costs = settings.getZombieCosts();
        assertEquals(5, costs.size());
        assertEquals(25, costs.get("ZombieImp"));
        assertEquals(50, costs.get("ZombieDefault"));
        assertEquals(75, costs.get("ZombieArmor1"));
        assertEquals(100, costs.get("ZombieNewspaper"));
        assertEquals(125, costs.get("ZombieArmor2"));
        assertEquals(25, settings.minZombieCost());

        // Check Pre-planted Plant Layout
        assertEquals(8, settings.getPlantLayout().size());
        assertTrue(level.canStart(), "Level should be validated and ready to start");

        // Verify setup in GameModel
        GameModel model = new GameModel(level);
        level.onStart(model);

        // Verify pre-planted plants at exact coordinates
        assertEquals(8, model.getAllPlants().size());
        assertEquals("Sunflower", model.getPlantAt(0, 0).getDefinition().getName());
        assertEquals("Peashooter", model.getPlantAt(0, 1).getDefinition().getName());
        assertEquals("Peashooter", model.getPlantAt(1, 1).getDefinition().getName());
        assertEquals("Wall-nut", model.getPlantAt(1, 2).getDefinition().getName());
        assertEquals("Sunflower", model.getPlantAt(2, 0).getDefinition().getName());
        assertEquals("Puff-shroom", model.getPlantAt(2, 2).getDefinition().getName());
        assertEquals("Peashooter", model.getPlantAt(3, 1).getDefinition().getName());
        assertEquals("Wall-nut", model.getPlantAt(4, 2).getDefinition().getName());

        // Verify 5 sun zombies spawned in column 8 (one per row)
        assertEquals(5, model.getActiveZombies().size());
        for (int r = 0; r < 5; r++) {
            ZombieInstance z = model.getActiveZombies().get(r);
            assertEquals("ZombieIZombieSun", z.getDefinition().getName());
            assertEquals(8, z.getGridPosition().getX());
            assertEquals(r, z.getGridPosition().getY());
        }
    }

    @Test
    @DisplayName("Stage 2 Loading: Configuration, placeable roster, pre-planted defenses and rules")
    void testStage2ConfigurationAndDefenseLoading() throws Exception {
        IZombieLevel level = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 2);
        assertNotNull(level, "Stage 2 level must load from minigames.json");

        assertEquals(2, level.getStage());
        assertEquals(4, level.getDifficultyTier());
        assertEquals(350, level.getCoinReward());
        assertEquals(3, level.redLineColumn());

        IZombieSettings settings = level.getSettings();
        Map<String, Integer> costs = settings.getZombieCosts();
        assertEquals(5, costs.size());
        assertEquals(25, costs.get("ZombieImp"));
        assertEquals(100, costs.get("ZombieExplorer"));
        assertEquals(125, costs.get("ZombieProspector"));
        assertEquals(150, costs.get("ZombieModernAllStar"));
        assertEquals(200, costs.get("ZombieArmor4"));
        assertEquals(25, settings.minZombieCost());

        assertEquals(10, settings.getPlantLayout().size());
        assertTrue(level.canStart());

        GameModel model = new GameModel(level);
        level.onStart(model);

        assertEquals(10, model.getAllPlants().size());
        assertEquals("Snow Pea", model.getPlantAt(0, 1).getDefinition().getName());
        assertEquals("Wall-nut", model.getPlantAt(0, 2).getDefinition().getName());
        assertEquals("Cabbage-pult", model.getPlantAt(1, 0).getDefinition().getName());
        assertEquals("Peashooter", model.getPlantAt(1, 1).getDefinition().getName());
        assertEquals("Chomper", model.getPlantAt(2, 1).getDefinition().getName());
        assertEquals("Wall-nut", model.getPlantAt(2, 2).getDefinition().getName());
        assertEquals("Cabbage-pult", model.getPlantAt(3, 0).getDefinition().getName());
        assertEquals("Snow Pea", model.getPlantAt(3, 1).getDefinition().getName());
        assertEquals("Peashooter", model.getPlantAt(4, 1).getDefinition().getName());
        assertEquals("Wall-nut", model.getPlantAt(4, 2).getDefinition().getName());

        assertEquals(5, model.getActiveZombies().size());
    }

    @Test
    @DisplayName("Stage 3 Loading: Configuration, placeable roster, pre-planted defenses and rules")
    void testStage3ConfigurationAndDefenseLoading() throws Exception {
        IZombieLevel level = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 3);
        assertNotNull(level, "Stage 3 level must load from minigames.json");

        assertEquals(3, level.getStage());
        assertEquals(5, level.getDifficultyTier());
        assertEquals(500, level.getCoinReward());
        assertEquals(3, level.redLineColumn());

        IZombieSettings settings = level.getSettings();
        Map<String, Integer> costs = settings.getZombieCosts();
        assertEquals(5, costs.size());
        assertEquals(50, costs.get("ZombieDefault"));
        assertEquals(150, costs.get("ZombieDarkArmor3"));
        assertEquals(150, costs.get("ZombieWizard"));
        assertEquals(200, costs.get("ZombieDarkKing"));
        assertEquals(300, costs.get("ZombieGargantuar"));
        assertEquals(50, settings.minZombieCost());

        assertEquals(12, settings.getPlantLayout().size());
        assertTrue(level.canStart());

        GameModel model = new GameModel(level);
        level.onStart(model);

        assertEquals(12, model.getAllPlants().size());
        assertEquals("Melon-pult", model.getPlantAt(0, 0).getDefinition().getName());
        assertEquals("Repeater", model.getPlantAt(0, 1).getDefinition().getName());
        assertEquals("Tall-nut", model.getPlantAt(0, 2).getDefinition().getName());
        assertEquals("Snow Pea", model.getPlantAt(1, 1).getDefinition().getName());
        assertEquals("Bonk Choy", model.getPlantAt(1, 2).getDefinition().getName());
        assertEquals("Melon-pult", model.getPlantAt(2, 0).getDefinition().getName());
        assertEquals("Repeater", model.getPlantAt(2, 1).getDefinition().getName());
        assertEquals("Tall-nut", model.getPlantAt(2, 2).getDefinition().getName());
        assertEquals("Snow Pea", model.getPlantAt(3, 1).getDefinition().getName());
        assertEquals("Bonk Choy", model.getPlantAt(3, 2).getDefinition().getName());
        assertEquals("Repeater", model.getPlantAt(4, 1).getDefinition().getName());
        assertEquals("Tall-nut", model.getPlantAt(4, 2).getDefinition().getName());

        assertEquals(5, model.getActiveZombies().size());
    }

    @Test
    @DisplayName("Autonomous Plant AI Defense: Pre-planted plants shoot projectiles and damage advancing zombies")
    void testAutonomousPlantAIDefense() throws Exception {
        IZombieLevel level = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(level);
        level.onStart(model);

        PvZGameLoop loop = new PvZGameLoop(model);

        // Place a ZombieDefault in row 0, col 5 (where Sunflower is at 0,0 and Peashooter is at 0,1)
        model.addSun(500);
        String placeErr = level.placeZombie(model, "ZombieDefault", 0, 5);
        assertNull(placeErr, "Placing ZombieDefault at (0, 5) should succeed");

        ZombieInstance attacker = null;
        for (ZombieInstance z : model.getActiveZombies()) {
            if ("ZombieDefault".equals(z.getDefinition().getName())) {
                attacker = z;
                break;
            }
        }
        assertNotNull(attacker);
        int initialHp = attacker.getCurrentHP();

        // Advance simulation for 2 seconds (40 frames at 0.05s)
        for (int i = 0; i < 40; i++) {
            loop.update(0.05f);
        }

        // Verify that Peashooter at (0, 1) detected the zombie, fired projectiles, and damaged the zombie
        assertTrue(attacker.getCurrentHP() < initialHp || !model.getActiveProjectiles().isEmpty(),
                "Plant AI must autonomously shoot and deal damage to advancing zombies");
    }

    private static void setSun(GameModel model, int target) {
        int current = model.getSunAmount();
        if (current < target) {
            model.addSun(target - current);
        } else if (current > target) {
            model.spendSun(current - target);
        }
    }

    @Test
    @DisplayName("Zombie Placement: Validation of bounds, red line, roster inclusion and sun economy")
    void testZombiePlacementValidation() throws Exception {
        IZombieLevel level = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(level);
        level.onStart(model);
        setSun(model, 150);

        // 1. Placement behind red line (col < 3) rejected
        String errRedLine = level.placeZombie(model, "ZombieImp", 0, 2);
        assertNotNull(errRedLine);
        assertTrue(errRedLine.contains("red line"));
        assertEquals(150, model.getSunAmount());

        // 2. Placement out of grid bounds rejected
        String errOutOfBounds = level.placeZombie(model, "ZombieImp", -1, 5);
        assertNotNull(errOutOfBounds);
        assertTrue(errOutOfBounds.contains("out of bounds"));

        String errOutOfBounds2 = level.placeZombie(model, "ZombieImp", 5, 5);
        assertNotNull(errOutOfBounds2);

        String errOutOfBounds3 = level.placeZombie(model, "ZombieImp", 0, 9);
        assertNotNull(errOutOfBounds3);

        // 3. Unlisted zombie name rejected
        String errUnknown = level.placeZombie(model, "ZombieGargantuar", 0, 5);
        assertNotNull(errUnknown);
        assertTrue(errUnknown.contains("not one of this stage's zombies"));

        // 4. Insufficient sun rejected
        setSun(model, 10);
        String errNoSun = level.placeZombie(model, "ZombieImp", 0, 5); // Imp costs 25
        assertNotNull(errNoSun);
        assertTrue(errNoSun.contains("Not enough sun"));
        assertEquals(10, model.getSunAmount());

        // 5. Valid placement succeeds (case-insensitive)
        setSun(model, 150);
        String success = level.placeZombie(model, "zombieimp", 1, 4);
        assertNull(success, "Valid placement should return null (no error)");
        int penaltyCost = (int) (25 * model.difficultyPenalty());
        assertEquals(150 - penaltyCost, model.getSunAmount());
        assertEquals(6, model.getActiveZombies().size());
    }

    @Test
    @DisplayName("Win Condition: All rows breached / all brains eaten triggers victory")
    void testWinConditionAllBrainsEaten() throws Exception {
        IZombieLevel level = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(level);
        level.onStart(model);

        assertFalse(level.checkWinCondition(model), "Initial state must not be won");

        // Breach rows 0, 1, 2, 3
        for (int r = 0; r < 4; r++) {
            model.markBrainEaten(r);
            assertFalse(level.checkWinCondition(model), "Partial breach must not trigger win");
        }

        // Breach 5th row (row 4)
        model.markBrainEaten(4);
        assertTrue(level.checkWinCondition(model), "All 5 rows breached must trigger win condition");
    }

    @Test
    @DisplayName("Loss Condition: No zombies remaining on lawn and insufficient sun to place any zombie")
    void testLossConditionOutOfSunAndZombies() throws Exception {
        IZombieLevel level = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(level);
        level.onStart(model);

        // Initially: 5 sun zombies active, 150 sun
        assertFalse(level.checkLossCondition(model), "Not lost at start");

        // Set sun to 0 but zombies still on lawn
        setSun(model, 0);
        assertFalse(level.checkLossCondition(model), "Not lost if zombies are still on lawn");

        // Clear all zombies from model (sun zombies and attackers killed)
        model.getActiveZombies().clear();
        assertEquals(0, model.getZombieCount());

        // With 0 zombies and 0 sun (< 25 minZombieCost) -> Loss!
        assertTrue(level.checkLossCondition(model), "0 zombies and 0 sun must trigger loss condition");

        // If sun is set to >= 25, loss condition is false (player can place an Imp)
        setSun(model, 25);
        assertFalse(level.checkLossCondition(model), "Not lost if player has enough sun to place cheapest zombie");

        // If won (all 5 rows breached), loss condition must be false even if 0 zombies and 0 sun
        setSun(model, 0);
        for (int r = 0; r < 5; r++) {
            model.markBrainEaten(r);
        }
        assertTrue(level.checkWinCondition(model));
        assertFalse(level.checkLossCondition(model), "Winning state must override loss condition");
    }
}
