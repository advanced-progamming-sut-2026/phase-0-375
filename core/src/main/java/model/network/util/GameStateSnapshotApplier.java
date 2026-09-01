package model.network.util;

import model.enums.PlacableLayer;
import model.enums.PlantState;
import model.enums.ZombieState;
import model.game.core.GameModel;
import model.game.map.Cell;
import model.game.map.Point;
import model.item.placeable.Placeable;
import model.network.dto.PlantSnapshotDto;
import model.network.dto.ZombieSnapshotDto;
import model.network.enums.PlayerRole;
import model.network.packet.game.GameStateSnapshotPacket;
import model.plant.PlantFactory;
import model.plant.instance.PlantInstance;
import model.zombie.ZombieFactory;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reconciles an authoritative {@link GameStateSnapshotPacket} onto a local
 * display-only {@link GameModel}. Keeps entity instances stable across ticks
 * so lawn PAM caches do not thrash.
 */
public final class GameStateSnapshotApplier {

    private final Map<String, PlantInstance> plantsById = new HashMap<>();
    private final Map<String, ZombieInstance> zombiesById = new HashMap<>();

    public void apply(GameModel model, GameStateSnapshotPacket snap, PlayerRole localRole) {
        if (model == null || snap == null) {
            return;
        }
        if (!ensureCatalogs()) {
            return;
        }

        int sun = localRole == PlayerRole.ZOMBIE ? snap.getZombieSun() : snap.getPlantSun();
        model.setSunAmount(sun);

        reconcilePlants(model, snap);
        reconcileZombies(model, snap);
        // Projectiles: local PvZGameLoop.updatePresentation() (PamPlantProjectileOrigins).
    }

    private void reconcilePlants(GameModel model, GameStateSnapshotPacket snap) {
        Set<String> seen = new HashSet<>();
        if (snap.getPlants() != null) {
            for (PlantSnapshotDto dto : snap.getPlants()) {
                if (dto == null || dto.getId() == null || dto.getPlantName() == null) {
                    continue;
                }
                seen.add(dto.getId());
                PlantInstance plant = plantsById.get(dto.getId());
                if (plant == null
                        || plant.getDefinition() == null
                        || !dto.getPlantName().equals(plant.getDefinition().getName())) {
                    if (plant != null) {
                        model.removePlantFromBoard(plant);
                    }
                    plant = PlantFactory.createInstance(dto.getPlantName());
                    if (plant == null) {
                        continue;
                    }
                    if (!model.placePlant(plant, dto.getRow(), dto.getCol())) {
                        clearCellMain(model, dto.getRow(), dto.getCol());
                        if (!model.placePlant(plant, dto.getRow(), dto.getCol())) {
                            continue;
                        }
                    }
                    plantsById.put(dto.getId(), plant);
                } else {
                    Point pos = plant.getPosition();
                    if (pos == null || pos.getX() != dto.getCol() || pos.getY() != dto.getRow()) {
                        model.removePlantFromBoard(plant);
                        if (!model.placePlant(plant, dto.getRow(), dto.getCol())) {
                            clearCellMain(model, dto.getRow(), dto.getCol());
                            model.placePlant(plant, dto.getRow(), dto.getCol());
                        }
                    }
                }
                plant.setCurrentHP(dto.getCurrentHP());
                if (!plant.hasActiveAction()) {
                    PlantState state = parsePlantState(dto.getState());
                    if (state != null) {
                        plant.setState(state);
                    }
                }
            }
        }

        Iterator<Map.Entry<String, PlantInstance>> it = plantsById.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PlantInstance> e = it.next();
            if (!seen.contains(e.getKey())) {
                model.removePlantFromBoard(e.getValue());
                it.remove();
            }
        }
    }

    private void reconcileZombies(GameModel model, GameStateSnapshotPacket snap) {
        Set<String> seen = new HashSet<>();
        if (snap.getZombies() != null) {
            for (ZombieSnapshotDto dto : snap.getZombies()) {
                if (dto == null || dto.getId() == null || dto.getZombieName() == null) {
                    continue;
                }
                seen.add(dto.getId());
                ZombieInstance zombie = zombiesById.get(dto.getId());
                if (zombie == null
                        || zombie.getDefinition() == null
                        || !dto.getZombieName().equals(zombie.getDefinition().getName())) {
                    if (zombie != null) {
                        model.removeZombie(zombie);
                    }
                    int spawnCol = Math.max(0, (int) Math.floor(dto.getX()));
                    zombie = ZombieFactory.createInstance(dto.getZombieName());
                    if (zombie == null) {
                        continue;
                    }
                    model.addExistingZombie(zombie, dto.getRow(), spawnCol);
                    zombiesById.put(dto.getId(), zombie);
                }
                model.syncZombieWorldPose(zombie, dto.getRow(), dto.getX(), dto.getY());
                zombie.setCurrentHP(dto.getCurrentHP());
                syncArmorHp(zombie, dto.getArmorHP());
                ZombieState state = parseZombieState(dto.getState());
                if (state != null) {
                    zombie.setState(state);
                }
            }
        }

        Iterator<Map.Entry<String, ZombieInstance>> it = zombiesById.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ZombieInstance> e = it.next();
            if (!seen.contains(e.getKey())) {
                model.removeZombie(e.getValue());
                it.remove();
            }
        }
    }

    /**
     * Maps authoritative total armor HP onto local armor pieces (outer-first).
     */
    static void syncArmorHp(ZombieInstance zombie, int targetTotalHp) {
        if (zombie == null) {
            return;
        }
        List<Armor> armors = zombie.getArmors();
        if (armors == null || armors.isEmpty()) {
            return;
        }
        int target = Math.max(0, targetTotalHp);
        int remaining = target;
        for (Armor armor : armors) {
            if (armor == null) {
                continue;
            }
            int base = armor.getBaseHealth();
            if (remaining <= 0) {
                armor.setCurrentHealth(0);
            } else if (remaining >= base) {
                armor.setCurrentHealth(base);
                remaining -= base;
            } else {
                armor.setCurrentHealth(remaining);
                remaining = 0;
            }
        }
        zombie.removeDestroyedArmor();
    }

    private static void clearCellMain(GameModel model, int row, int col) {
        if (model.getGameMap() == null) return;
        Cell cell = model.getGameMap().getCell(col, row);
        if (cell == null) return;
        Placeable occupant = cell.getPlaceable(PlacableLayer.MAIN);
        if (occupant instanceof PlantInstance plant) {
            model.removePlantFromBoard(plant);
        } else if (occupant != null) {
            cell.removePlaceable(occupant);
        }
    }

    private static PlantState parsePlantState(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return PlantState.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ZombieState parseZombieState(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return ZombieState.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean ensureCatalogs() {
        return ensurePlantFactory() && ensureZombieFactory();
    }

    private static boolean ensurePlantFactory() {
        try {
            PlantFactory.getAllDefinitions();
            return true;
        } catch (IllegalStateException notInitialised) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
                return true;
            } catch (Exception loadError) {
                System.err.println("[GameStateSnapshotApplier] PlantFactory init failed: " + loadError.getMessage());
                return false;
            }
        }
    }

    private static boolean ensureZombieFactory() {
        try {
            ZombieFactory.hasDefinition("ZombieDefault");
            return true;
        } catch (RuntimeException notInitialised) {
            try {
                ZombieFactory.init("/assets/data/zombies/zombies.json",
                        "/assets/data/armor/ArmorTypeData.json");
                return true;
            } catch (Exception loadError) {
                System.err.println("[GameStateSnapshotApplier] ZombieFactory init failed: " + loadError.getMessage());
                return false;
            }
        }
    }
}
