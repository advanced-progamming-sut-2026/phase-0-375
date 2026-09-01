package model.shop;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyOfferRollTest {

    private static final List<String> PLANTS = List.of("A", "B", "C");

    @Test
    void sameDateSamePlant() {
        LocalDate day = LocalDate.of(2026, 9, 1);
        assertEquals(DailyOfferRoll.pickPlantForDate(day, PLANTS),
                DailyOfferRoll.pickPlantForDate(day, PLANTS));
    }

    @Test
    void picksFromCatalog() {
        String picked = DailyOfferRoll.pickPlantForDate(LocalDate.of(2026, 9, 1), PLANTS);
        assertEquals(true, PLANTS.contains(picked));
    }

    @Test
    void emptyCatalogReturnsNull() {
        assertNull(DailyOfferRoll.pickPlantForDate(LocalDate.now(), List.of()));
    }
}
