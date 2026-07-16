package model.game.level.special;

import model.app.App;
import model.enums.PlantCategory;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.plant.PlantFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Locked Plants. The spec defines two variants, chosen per level by the
 * level data (exactly one must be configured):
 *
 * <ul>
 *   <li><b>Family pick</b> ({@code restrictedFamilies}, or
 *       {@code allFamiliesRestricted} for every family): the player builds
 *       the seed selection from their own unlocked plants, but may take at
 *       most <b>one</b> plant from each restricted family — picking one
 *       locks the rest of that family. Enforced during selection by
 *       {@code PlantSelectionMenuController}.</li>
 *   <li><b>Forced set</b> ({@code forcedPlants}): the seed selection is
 *       dictated and the player must start with exactly those plants;
 *       choosing is disabled entirely.</li>
 * </ul>
 *
 * <p>Win/loss rules are the regular ones.
 */
public class LockedPlantsLevel extends RegularLevel {

    public LockedPlantsLevel(LevelConfig config) {
        super(config);
    }

    @Override
    public boolean canStart() {
        if (!super.canStart() || hasForcedSet() == hasFamilyRestrictions()) {
            return false; // exactly one variant must be configured
        }
        return hasForcedSet()
                ? ensurePlantFactory() && forcedSetValid()
                : restrictedFamiliesValid();
    }

    @Override
    public void onStart() {
        super.onStart(); // initial graves

        if (!hasForcedSet()) {
            return; // family-pick variant: the player already chose the seeds
        }
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return;
        }
        model.setSelectedPlants(new ArrayList<>(getConfig().getForcedPlants()));
    }

    private boolean hasForcedSet() {
        List<String> forcedPlants = getConfig().getForcedPlants();
        return forcedPlants != null && !forcedPlants.isEmpty();
    }

    private boolean hasFamilyRestrictions() {
        Set<String> restrictedFamilies = getConfig().getRestrictedFamilies();
        return getConfig().isAllFamiliesRestricted()
                || (restrictedFamilies != null && !restrictedFamilies.isEmpty());
    }

    private boolean forcedSetValid() {
        for (String plantName : getConfig().getForcedPlants()) {
            if (!PlantFactory.hasDefinition(plantName)) {
                return false;
            }
        }
        return true;
    }

    /** Every named family must be a real {@link PlantCategory}. */
    private boolean restrictedFamiliesValid() {
        Set<String> restrictedFamilies = getConfig().getRestrictedFamilies();
        if (restrictedFamilies == null) {
            return true; // allFamiliesRestricted only
        }
        for (String family : restrictedFamilies) {
            if (!isKnownFamily(family)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isKnownFamily(String family) {
        for (PlantCategory category : PlantCategory.values()) {
            if (category.name().equalsIgnoreCase(family)) {
                return true;
            }
        }
        return false;
    }
}
