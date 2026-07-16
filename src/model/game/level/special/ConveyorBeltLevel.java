package model.game.level.special;

import model.app.App;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.plant.PlantFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Conveyor Belt: the player does not choose plants
 * ({@code allowsChoosingPlants} is forced off by {@code LevelRegistry});
 * instead the belt periodically delivers seed packets from the level's pool,
 * and planting a packet costs no sun (enforced by
 * {@code GameplayMenuController.plant}).
 *
 * <p>The belt lives in {@code GameModel.selectedPlants} — each entry is one
 * seed packet, duplicates allowed — so the existing "selected plants" command
 * doubles as the belt display and the plant command's selection check doubles
 * as the belt check. The selection menu itself is locked for this level type.
 *
 * <p>JSON: {@code conveyorPlants} (delivery pool, cycled in order; required),
 * {@code conveyorIntervalSeconds} (default 5), {@code conveyorCapacity}
 * (default 10; the belt pauses while full).
 *
 * <p>Win/loss rules are the regular ones.
 */
public class ConveyorBeltLevel extends RegularLevel {

    private float timeSinceLastDelivery;
    private int nextPoolIndex;

    public ConveyorBeltLevel(LevelConfig config) {
        super(config);
    }

    @Override
    public boolean canStart() {
        if (!super.canStart()) {
            return false;
        }
        List<String> pool = getConfig().getConveyorPlants();
        if (pool == null || pool.isEmpty()
                || getConfig().getConveyorIntervalSeconds() <= 0
                || getConfig().getConveyorCapacity() <= 0
                || !ensurePlantFactory()) {
            return false;
        }
        for (String plantName : pool) {
            if (!PlantFactory.hasDefinition(plantName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart(); // initial graves

        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return;
        }
        // The belt replaces the player's selection; deliver the first packet
        // immediately so the player is not left idle.
        model.setSelectedPlants(new ArrayList<>());
        deliver(model);
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);

        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return;
        }
        timeSinceLastDelivery += deltaTime;
        float interval = getConfig().getConveyorIntervalSeconds();
        while (timeSinceLastDelivery >= interval) {
            timeSinceLastDelivery -= interval;
            deliver(model);
        }
    }

    /** Puts the next packet from the pool on the belt; pauses while full. */
    private void deliver(GameModel model) {
        List<String> belt = model.getSelectedPlants();
        if (belt == null) {
            belt = new ArrayList<>();
            model.setSelectedPlants(belt);
        }
        if (belt.size() >= getConfig().getConveyorCapacity()) {
            return; // belt is full: it stalls until a packet is planted
        }
        List<String> pool = getConfig().getConveyorPlants();
        belt.add(pool.get(nextPoolIndex));
        nextPoolIndex = (nextPoolIndex + 1) % pool.size();
    }
}
