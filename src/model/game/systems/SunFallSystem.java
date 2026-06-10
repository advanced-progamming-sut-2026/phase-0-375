package model.game.systems;

import model.core.Tickable;
import model.enums.SunType;
import model.item.Sun;

public class SunFallSystem implements Tickable {
    private float skyDropTimer;
    private boolean skyDropEnabled;

    public boolean isSkyDropEnabled() {
        return skyDropEnabled;
    }

    public void tick(float deltaTime) {

    }

    public void spawnSkySun(int x, int y, SunType type) {

    }

    public void toggleSkyDrop(boolean enabled) {

    }
}
