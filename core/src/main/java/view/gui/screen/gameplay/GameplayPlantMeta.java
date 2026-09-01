package view.gui.screen.gameplay;

import model.app.App;
import model.game.core.GameModel;
import model.plant.PlantFactory;
import model.user.User;

import java.util.Map;

/** Seed packet cost, recharge, level, and boost lookups. */
public final class GameplayPlantMeta {
    private GameplayPlantMeta() {}

    public static int cost(String name) {
        try {
            if (!PlantFactory.hasDefinition(name)) {
                return 0;
            }
            return PlantFactory.getDefinition(name).getCost();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    public static float rechargeTime(String name) {
        try {
            if (!PlantFactory.hasDefinition(name)) {
                return 0f;
            }
            return PlantFactory.getDefinition(name).getRechargeTime();
        } catch (IllegalStateException e) {
            return 0f;
        }
    }

    public static int level(String name) {
        User user = App.getInstance().getCurrentUser();
        Map<String, Integer> levels = user == null ? null : user.getPlantLevels();
        if (name == null || levels == null) {
            return 1;
        }
        Integer plantLevel = levels.get(name);
        return plantLevel == null || plantLevel < 1 ? 1 : plantLevel;
    }

    public static boolean boosted(String name) {
        User user = App.getInstance().getCurrentUser();
        Map<String, Boolean> boosts = user == null ? null : user.getPlantBoosts();
        return name != null && boosts != null && Boolean.TRUE.equals(boosts.get(name));
    }

    public static float seedCooldownFraction(GameModel model, String name) {
        if (model == null || model.areSeedCooldownsDisabled()) {
            return 0f;
        }
        float total = rechargeTime(name);
        if (total <= 0f) {
            return 0f;
        }
        return model.getSeedCooldown(name) / total;
    }
}
