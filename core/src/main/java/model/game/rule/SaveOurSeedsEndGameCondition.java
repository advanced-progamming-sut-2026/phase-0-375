package model.game.rule;

import model.game.core.GameModel;
import model.game.level.special.SaveOurSeedsLevel;
import model.game.map.Point;
import model.plant.instance.PlantInstance;

import java.util.List;

/**
 * Save Our Seeds levels are additionally lost when any protected
 * (pre-placed) plant dies or disappears.
 *
 * <p>Assumes the level setup has placed a plant on every position in
 * {@code protectedPlantPositions} before the first tick; a null or empty
 * list behaves like a regular level.
 */
public class SaveOurSeedsEndGameCondition extends AbstractEndGameCondition {
    private final SaveOurSeedsLevel saveOurSeedsLevel;

    public SaveOurSeedsEndGameCondition(SaveOurSeedsLevel saveOurSeedsLevel) {
        this.saveOurSeedsLevel = saveOurSeedsLevel;
    }

    @Override
    public boolean isGameOver(GameModel model) {
        if (super.isGameOver(model)) return true;

        List<Point> protectedPositions =
                saveOurSeedsLevel.getConfig().getProtectedPlantPositions();
        if (protectedPositions == null) return false;

        for (Point position : protectedPositions) {
            PlantInstance plant = model.getPlantAt(position.getY(), position.getX());
            if (plant == null || plant.getCurrentHP() <= 0) {
                return true;
            }
        }
        return false;
    }
}
