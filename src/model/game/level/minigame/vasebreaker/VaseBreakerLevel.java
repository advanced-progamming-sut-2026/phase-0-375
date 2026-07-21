package model.game.level.minigame.vasebreaker;

import model.app.App;
import model.enums.MiniGameType;
import model.enums.VaseContent;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;
import model.game.map.Cell;
import model.game.map.GameMap;
import model.game.map.Point;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.zombie.ZombieFactory;
import model.zombie.definition.Zombie;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Vase Breaker mini-game.
 *
 * <p>Vase mix, layout width, hidden zombie/plant pools and the packet expiry
 * are configured per stage in minigames.json ({@link VaseBreakerSettings}).
 * Unbroken vases occupy their cell's GROUND layer, so tile status shows them
 * and nothing can be planted on top of one.
 */
public class VaseBreakerLevel extends MiniGameLevel {

    private VaseBreakerSettings settings = new VaseBreakerSettings();
    private final List<Vase> vases = new ArrayList<>();
    private final List<PendingSeedPacket> pendingPackets = new ArrayList<>();
    private final Random random = new Random();
    private boolean seeded;

    public VaseBreakerLevel(LevelConfig config, MiniGameType miniGameType, int difficultyTier) {
        super(config, miniGameType, difficultyTier);
    }

    // --- Configuration & inspection ---

    public void setSettings(VaseBreakerSettings settings) {
        if (settings != null) {
            this.settings = settings;
        }
    }

    public VaseBreakerSettings getSettings() {
        return settings;
    }

    /** All vases on the board, broken or not (copy). */
    public List<Vase> getVases() {
        return new ArrayList<>(vases);
    }

    /** Seed packets currently waiting to be planted (copy). */
    public List<PendingSeedPacket> getPendingSeedPackets() {
        return new ArrayList<>(pendingPackets);
    }

    private int totalVaseCount() {
        return settings.getRandomVaseCount() + settings.getSeedVaseCount()
                + settings.getGiantVaseCount();
    }

    // --- Level lifecycle ---

    @Override
    public boolean canStart() {
        LevelConfig config = getConfig();
        if (config == null
                || config.getRows() <= 0
                || config.getColumns() <= 0
                || config.getRules() == null) {
            return false;
        }
        int total = totalVaseCount();
        if (total <= 0) {
            return false;
        }
        int vaseCols = Math.min(Math.max(settings.getVaseColumns(), 1), config.getColumns());
        if (total > config.getRows() * vaseCols) {
            return false; // the configured vases do not fit on the board
        }
        boolean randomVases = settings.getRandomVaseCount() > 0;
        if (randomVases && settings.totalRandomWeight() <= 0f) {
            return false; // random vases need at least one positive outcome weight
        }
        if (randomVases && settings.getRandomZombieWeight() > 0f
                && !validZombiePool(settings.getZombiePool())) {
            return false;
        }
        if (settings.getGiantVaseCount() > 0
                && !validZombiePool(List.of(settings.getGiantVaseZombie()))) {
            return false;
        }
        if ((settings.getSeedVaseCount() > 0
                || (randomVases && settings.getRandomSeedWeight() > 0f))
                && !validPlantPool(settings.getPlantPool())) {
            return false;
        }
        return true;
    }

    @Override
    public void onStart() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null || seeded) {
            return;
        }
        seedVases(model);
    }

    @Override
    public void tick(float deltaTime) {
        if (pendingPackets.isEmpty()) {
            return;
        }
        Iterator<PendingSeedPacket> it = pendingPackets.iterator();
        while (it.hasNext()) {
            PendingSeedPacket packet = it.next();
            packet.tick(deltaTime);
            if (packet.isExpired()) {
                it.remove();
            }
        }
    }

    @Override
    public void onWaveCleared(int waveNumber) {
        // Vase Breaker has no scripted waves.
    }

    @Override
    public void onFail() {
        // No extra behaviour on loss.
    }

    @Override
    public boolean checkWinCondition(GameModel model) {
        if (!seeded || model == null) {
            return false;
        }
        for (Vase vase : vases) {
            if (!vase.isBroken()) {
                return false;
            }
        }
        return model.getZombieCount() == 0;
    }

    @Override
    public boolean checkLossCondition(GameModel model) {
        return model != null && model.isHouseBreached();
    }

    // --- Vase seeding ---

    private void seedVases(GameModel model) {
        GameMap map = model.getMap();
        int rows = map.getRows();
        int cols = map.getCols();
        int vaseCols = Math.min(Math.max(settings.getVaseColumns(), 1), cols);

        // Candidate tiles: the rightmost vaseCols columns of the lawn.
        List<Point> tiles = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = cols - vaseCols; col < cols; col++) {
                tiles.add(new Point(col, row)); // x = column, y = row
            }
        }
        Collections.shuffle(tiles, random);

        List<VaseContent> contents = new ArrayList<>();
        for (int i = 0; i < settings.getRandomVaseCount(); i++) contents.add(rollRandomContent());
        for (int i = 0; i < settings.getSeedVaseCount(); i++) contents.add(VaseContent.SEED_PACKET);
        for (int i = 0; i < settings.getGiantVaseCount(); i++) contents.add(VaseContent.GIANT_VASE);
        Collections.shuffle(contents, random);

        int count = Math.min(contents.size(), tiles.size());
        for (int i = 0; i < count; i++) {
            Point tile = tiles.get(i);
            Vase vase = new Vase(tile, contents.get(i));
            fillVase(vase);
            vases.add(vase);
            Cell cell = map.getCell(tile.getX(), tile.getY()); // getCell(col, row)
            if (cell != null) {
                cell.addPlaceable(vase);
            }
        }
        seeded = true;
    }

    /**
     * Rolls what a random vase holds: nothing, a normal zombie from the
     * zombie pool, or a seed packet, weighted by the configured odds.
     * Gargantuars only ever come out of dedicated giant vases.
     */
    private VaseContent rollRandomContent() {
        float total = settings.totalRandomWeight();
        if (total <= 0f) {
            return VaseContent.EMPTY;
        }
        float roll = random.nextFloat() * total;
        if (roll < settings.getRandomEmptyWeight()) {
            return VaseContent.EMPTY;
        }
        roll -= settings.getRandomEmptyWeight();
        if (roll < settings.getRandomZombieWeight()) {
            return VaseContent.ZOMBIE;
        }
        return VaseContent.SEED_PACKET;
    }

    private void fillVase(Vase vase) {
        switch (vase.getContentType()) {
            case ZOMBIE -> vase.setHiddenZombie(
                    zombieDefinition(randomFrom(settings.getZombiePool())));
            case GIANT_VASE -> vase.setHiddenZombie(
                    zombieDefinition(settings.getGiantVaseZombie()));
            case SEED_PACKET -> vase.setHiddenPlant(
                    plantDefinition(randomFrom(settings.getPlantPool())));
            case EMPTY -> { /* nothing inside */ }
        }
    }

    private String randomFrom(List<String> pool) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    // --- Breaking vases ---

    /** The unbroken vase at (row, col), or null. */
    public Vase vaseAt(int row, int col) {
        for (Vase vase : vases) {
            if (!vase.isBroken()
                    && vase.getPosition() != null
                    && vase.getPosition().getY() == row
                    && vase.getPosition().getX() == col) {
                return vase;
            }
        }
        return null;
    }

    /**
     * Breaks the given vase and applies its contents.
     *
     * @return a user-facing description of what came out.
     */
    public String breakVase(GameModel model, Vase vase) {
        vase.setBroken(true);
        int row = vase.getPosition().getY();
        int col = vase.getPosition().getX();
        Cell cell = model.getMap().getCell(col, row);
        if (cell != null) {
            cell.removePlaceable(vase);
        }

        return switch (vase.getContentType()) {
            case EMPTY -> "The vase was empty.";
            case ZOMBIE, GIANT_VASE -> {
                Zombie hidden = vase.getHiddenZombie();
                if (hidden == null || hidden.getName() == null) {
                    yield "The vase was empty.";
                }
                model.spawnZombieAt(hidden.getName(), row, col);
                yield (vase.getContentType() == VaseContent.GIANT_VASE ? "A giant vase! " : "")
                        + hidden.getName() + " woke up at (" + row + ", " + col + ")!";
            }
            case SEED_PACKET -> {
                Plant plant = vase.getHiddenPlant();
                if (plant == null) {
                    yield "The vase was empty.";
                }
                pendingPackets.add(new PendingSeedPacket(
                        plant, vase.getPosition(), settings.getSeedPacketExpirySeconds()));
                yield "Found a " + plant.getName() + " seed packet! Plant it on"
                        + " any free tile within "
                        + Math.round(settings.getSeedPacketExpirySeconds()) + "s for free.";
            }
        };
    }

    /**
     * Claims a pending seed packet for this plant. Packets are not tied to
     * the tile where they dropped: they can be planted on any free tile.
     *
     * @return the claimed packet, or null when no matching packet is waiting
     *         (wrong plant, or already expired).
     */
    public PendingSeedPacket claimSeedPacket(String plantName) {
        if (plantName == null) {
            return null;
        }
        Iterator<PendingSeedPacket> it = pendingPackets.iterator();
        while (it.hasNext()) {
            PendingSeedPacket packet = it.next();
            if (packet.isExpired() || packet.getPlant() == null) {
                continue;
            }
            if (plantName.equalsIgnoreCase(packet.getPlant().getName())) {
                it.remove();
                return packet;
            }
        }
        return null;
    }

    // --- Definition lookups ---

    private static boolean validZombiePool(List<String> pool) {
        if (pool == null || pool.isEmpty() || !ensureZombieFactory()) {
            return false;
        }
        for (String name : pool) {
            if (name == null || !ZombieFactory.hasDefinition(name)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validPlantPool(List<String> pool) {
        if (pool == null || pool.isEmpty() || !ensurePlantFactory()) {
            return false;
        }
        for (String name : pool) {
            if (name == null || !PlantFactory.hasDefinition(name)) {
                return false;
            }
        }
        return true;
    }

    private static Zombie zombieDefinition(String name) {
        if (name == null || !ensureZombieFactory()) {
            return null;
        }
        return ZombieFactory.getDefinition(name);
    }

    private static Plant plantDefinition(String name) {
        if (name == null || !ensurePlantFactory() || !PlantFactory.hasDefinition(name)) {
            return null;
        }
        return PlantFactory.getDefinition(name);
    }

    /** Loads plant definitions on demand, mirroring RegularLevel. */
    private static boolean ensurePlantFactory() {
        try {
            PlantFactory.getAllDefinitions();
            return true;
        } catch (RuntimeException notInitialised) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
                return true;
            } catch (IOException | RuntimeException e) {
                return false;
            }
        }
    }

    /** Loads zombie definitions on demand (same default paths as LevelRegistry). */
    private static boolean ensureZombieFactory() {
        try {
            ZombieFactory.getAllDefinitions();
            return true;
        } catch (RuntimeException notInitialised) {
            try {
                ZombieFactory.init("/assets/data/zombies/zombies.json",
                        "/assets/data/armor/ArmorTypeData.json");
                return true;
            } catch (IOException | RuntimeException e) {
                return false;
            }
        }
    }
}
