package model.shop;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Deterministic daily-offer plant roll shared by server and offline clients.
 */
public final class DailyOfferRoll {
    private DailyOfferRoll() {}

    public static String pickPlantForDate(LocalDate date, List<String> plantNames) {
        if (date == null || plantNames == null || plantNames.isEmpty()) {
            return null;
        }
        int idx = new Random(date.toEpochDay()).nextInt(plantNames.size());
        return plantNames.get(idx);
    }
}
