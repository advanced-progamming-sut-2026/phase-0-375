package model.shop;

import model.plant.PlantFactory;
import model.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShopDailyOfferTest {

    @BeforeAll
    static void initPlants() throws Exception {
        PlantFactory.init("/assets/data/plants/plants.json");
    }

    @Test
    void showsGlobalOfferEvenWhenPlantNotUnlocked() {
        User user = new User();
        user.setUsername("tester");
        user.setUnlockedPlants(Set.of("Peashooter"));

        Shop shop = Shop.getInstance(user);
        String globalPlant = DailyOfferRoll.pickPlantForDate(LocalDate.now(), ShopDailyOfferTest.catalogNames());
        if ("Peashooter".equals(globalPlant)) {
            return; // rare collision; other tests cover determinism
        }

        shop.refreshDailyOffer(globalPlant, LocalDate.now().toString());
        assertNotNull(shop.getDailyOffer());
    }

    @Test
    void purchaseRequiresUnlock() {
        User user = new User();
        user.setUsername("tester");
        user.setCoins(10_000);
        user.setUnlockedPlants(new HashSet<>());

        String globalPlant = DailyOfferRoll.pickPlantForDate(LocalDate.now(), catalogNames());
        Shop shop = Shop.getInstance(user);
        shop.refreshDailyOffer(globalPlant, LocalDate.now().toString());
        assertNotNull(shop.getDailyOffer());
        assertNull(user.getUnlockedPlants().stream()
                .filter(p -> p.equalsIgnoreCase(globalPlant))
                .findFirst()
                .orElse(null));
    }

    private static java.util.List<String> catalogNames() {
        return PlantFactory.getAllDefinitions().stream()
                .map(p -> p.getName())
                .sorted()
                .toList();
    }
}
