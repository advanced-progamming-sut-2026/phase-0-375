package model.plant.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the three level upgrades (level 2, 3, 4) for a plant definition.
 */
public class PlantLevels {
    private final LevelUpgrade level2;
    private final LevelUpgrade level3;
    private final LevelUpgrade level4;

    public PlantLevels(LevelUpgrade level2, LevelUpgrade level3, LevelUpgrade level4) {
        this.level2 = level2;
        this.level3 = level3;
        this.level4 = level4;
    }

    /** Returns the upgrade applied at the given level, or {@code null} if none. */
    public LevelUpgrade getUpgrade(int level) {
        switch (level) {
            case 2: return level2;
            case 3: return level3;
            case 4: return level4;
            default: return null;
        }
    }

    /**
     * Returns every upgrade that should be active at the given target
     * level, in ascending level order. For example,
     * {@code cumulativeUpgrades(4)} returns {@code [level2, level3, level4]}.
     */
    public Map<Integer, LevelUpgrade> cumulativeUpgrades(int targetLevel) {
        Map<Integer, LevelUpgrade> out = new LinkedHashMap<>();
        if (targetLevel >= 2 && level2 != null) out.put(2, level2);
        if (targetLevel >= 3 && level3 != null) out.put(3, level3);
        if (targetLevel >= 4 && level4 != null) out.put(4, level4);
        return Collections.unmodifiableMap(out);
    }

    public LevelUpgrade getLevel2() { return level2; }
    public LevelUpgrade getLevel3() { return level3; }
    public LevelUpgrade getLevel4() { return level4; }
}
