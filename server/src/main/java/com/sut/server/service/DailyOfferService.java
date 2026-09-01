package com.sut.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.shop.DailyOfferRoll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Authoritative global daily shop offer (one plant per calendar day for all users).
 */
public class DailyOfferService {

    public static final String DEFAULT_FILE_PATH = "server/data/daily_offer.json";

    public record Snapshot(String plant, String date) {}

    private static final class State {
        public String plant;
        public String date;
    }

    private final Path storagePath;
    private final ObjectMapper mapper;
    private final Object lock = new Object();
    private Snapshot cached;

    public DailyOfferService() {
        this(resolveStoragePath());
    }

    public DailyOfferService(Path storagePath) {
        this(storagePath, new ObjectMapper());
    }

    public DailyOfferService(Path storagePath, ObjectMapper mapper) {
        this.storagePath = Objects.requireNonNull(storagePath);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public Snapshot getToday() {
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();
        synchronized (lock) {
            if (cached != null && todayStr.equals(cached.date())) {
                return cached;
            }
            loadFromDisk();
            if (cached != null && todayStr.equals(cached.date())) {
                return cached;
            }
            String plant = rollPlant(today);
            cached = new Snapshot(plant, todayStr);
            saveToDisk(cached);
            return cached;
        }
    }

    private String rollPlant(LocalDate date) {
        List<String> names = PlantFactory.getAllDefinitions().stream()
                .map(Plant::getName)
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .toList();
        return DailyOfferRoll.pickPlantForDate(date, names);
    }

    private void loadFromDisk() {
        if (!Files.isRegularFile(storagePath)) {
            cached = null;
            return;
        }
        try {
            State state = mapper.readValue(storagePath.toFile(), State.class);
            if (state == null || state.plant == null || state.date == null) {
                cached = null;
                return;
            }
            cached = new Snapshot(state.plant, state.date);
        } catch (IOException e) {
            cached = null;
        }
    }

    private void saveToDisk(Snapshot snapshot) {
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            State state = new State();
            state.plant = snapshot.plant();
            state.date = snapshot.date();
            mapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), state);
        } catch (IOException e) {
            System.err.println("[DailyOfferService] Failed to write " + storagePath + ": " + e.getMessage());
        }
    }

    private static Path resolveStoragePath() {
        String configured = System.getProperty("pvz.dailyOffer.file");
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim());
        }
        return Paths.get(DEFAULT_FILE_PATH);
    }
}
