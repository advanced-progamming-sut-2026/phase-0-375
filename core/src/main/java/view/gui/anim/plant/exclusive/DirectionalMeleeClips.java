package view.gui.anim.plant.exclusive;

import model.game.map.Point;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/** Shared left / right / both clip names for Bonk Choy and Wasabi Whip. */
final class DirectionalMeleeClips {
    static final String RIGHT = "attack";
    static final String LEFT = "attack2";
    static final String BOTH = "attack3";

    private DirectionalMeleeClips() {}

    static String clipName(PlantInstance plant, List<ZombieInstance> targets) {
        Point pos = plant == null ? null : plant.getPosition();
        if (pos == null || targets == null || targets.isEmpty()) {
            return RIGHT;
        }

        boolean hasRight = false;
        boolean hasLeft = false;
        int plantCol = pos.getX();

        for (ZombieInstance target : targets) {
            if (target.getGridPosition() == null) continue;
            if (target.getGridX() >= plantCol) {
                hasRight = true;
            } else {
                hasLeft = true;
            }
            if (hasLeft && hasRight) break;
        }

        if (hasRight && hasLeft) return BOTH;
        if (hasLeft) return LEFT;
        return RIGHT;
    }
}
