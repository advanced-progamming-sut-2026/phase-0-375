package model.item;

import model.enums.LootType;
import model.game.core.GameModel;

public class LootDrop {
    private LootType type;
    private int amount;


    public LootDrop(LootType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public LootType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    /**
     * Applies this loot drop to the given game model. Coins are converted
     * to sun (so the player can spend them in subsequent levels). gems
     * and seed packets are stored on the user profile via the App.
     */
    public void apply(GameModel gameModel) {
        if (gameModel == null) return;
        switch (type) {
            case COIN:
                // 1 coin -> 25 sun as a generic in-game currency mapping.
                gameModel.addSun(25 * Math.max(1, amount));
                break;
            case GEM:
            case SEED_PACKET:
                // Gems and seed packets are persistent profile rewards.
                // for now, they are no-ops at the model layer.
                break;
        }
    }
}