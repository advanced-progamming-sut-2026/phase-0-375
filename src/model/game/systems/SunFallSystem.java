package model.game.systems;

import model.game.core.Tickable;
import model.enums.SunType;

public class SunFallSystem implements Tickable {
    private float skyDropTimer;
    private boolean skyDropEnabled;

    public SunFallSystem(float skyDropTimer, boolean skyDropEnabled) {
        this.skyDropTimer = skyDropTimer;
        this.skyDropEnabled = skyDropEnabled;
    }


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
