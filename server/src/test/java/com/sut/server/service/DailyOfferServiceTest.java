package com.sut.server.service;

import model.plant.PlantFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DailyOfferServiceTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initPlants() throws Exception {
        PlantFactory.init("/assets/data/plants/plants.json");
    }

    @Test
    void sameDayReturnsSameOffer() {
        DailyOfferService service = new DailyOfferService(tempDir.resolve("daily_offer.json"));
        DailyOfferService.Snapshot first = service.getToday();
        DailyOfferService.Snapshot second = service.getToday();
        assertNotNull(first.plant());
        assertEquals(first.plant(), second.plant());
        assertEquals(first.date(), second.date());
    }
}
