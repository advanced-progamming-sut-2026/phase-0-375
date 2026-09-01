package com.sut.server.room;

import model.game.core.GameModel;
import model.item.Sun;
import model.network.dto.PlantSnapshotDto;
import model.network.dto.ProjectileSnapshotDto;
import model.network.dto.SunSnapshotDto;
import model.network.dto.ZombieSnapshotDto;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Maps live simulation entities into snapshot DTOs for multiplayer broadcast.
 */
final class IZombieSnapshotMapper {

    private IZombieSnapshotMapper() {
    }

    static List<PlantSnapshotDto> mapPlants(GameModel gameModel, Function<Object, String> idFn) {
        List<PlantSnapshotDto> plantDtos = new ArrayList<>();
        for (PlantInstance plant : gameModel.getAllPlants()) {
            if (plant == null) {
                continue;
            }
            String id = idFn.apply(plant);
            int row = plant.getPosition() != null ? plant.getPosition().getY() : 0;
            int col = plant.getPosition() != null ? plant.getPosition().getX() : 0;
            int maxHp = plant.getDefinition() != null
                    ? plant.getDefinition().getBaseHP() : plant.getCurrentHP();
            String state = plant.getState() != null ? plant.getState().name() : "IDLE";
            String name = plant.getDefinition() != null
                    ? plant.getDefinition().getName() : "Unknown";
            plantDtos.add(new PlantSnapshotDto(
                    id, name, row, col, plant.getCurrentHP(), maxHp, state,
                    plant.isPlantFoodActive(), plant.isFrozen(), plant.getStackCount()));
        }
        return plantDtos;
    }

    static List<ZombieSnapshotDto> mapZombies(GameModel gameModel, Function<Object, String> idFn) {
        List<ZombieSnapshotDto> zombieDtos = new ArrayList<>();
        for (ZombieInstance zombie : gameModel.getActiveZombies()) {
            if (zombie == null) {
                continue;
            }
            zombieDtos.add(toZombieDto(zombie, idFn.apply(zombie)));
        }
        return zombieDtos;
    }

    static List<ProjectileSnapshotDto> mapProjectiles(
            GameModel gameModel, Function<Object, String> idFn) {
        List<ProjectileSnapshotDto> projectileDtos = new ArrayList<>();
        for (Projectile projectile : gameModel.getActiveProjectiles()) {
            if (projectile == null) {
                continue;
            }
            String element = projectile.getElement() != null
                    ? projectile.getElement().name() : "NONE";
            projectileDtos.add(new ProjectileSnapshotDto(
                    idFn.apply(projectile),
                    projectile.getClass().getSimpleName(),
                    projectile.getRow(),
                    projectile.getX(),
                    projectile.getY(),
                    projectile.getVelocity() * projectile.getDirection(),
                    element));
        }
        return projectileDtos;
    }

    static List<SunSnapshotDto> mapSuns(GameModel gameModel) {
        List<SunSnapshotDto> sunDtos = new ArrayList<>();
        for (Sun sun : gameModel.getActiveSuns()) {
            if (sun == null) {
                continue;
            }
            String type = sun.getType() != null ? sun.getType().name() : "NORMAL";
            sunDtos.add(new SunSnapshotDto(
                    sun.getX(), sun.getY(), sun.getValue(), type,
                    sun.getOffsetX(), sun.getOffsetY(),
                    sun.getFallRemaining(), sun.getFallDuration(),
                    sun.hasOrigin(), sun.getOriginX(), sun.getOriginY()));
        }
        return sunDtos;
    }

    private static ZombieSnapshotDto toZombieDto(ZombieInstance zombie, String id) {
        int row = zombie.getGridY();
        float x = zombie.getContinuousPosition() != null
                ? zombie.getContinuousPosition().getX()
                : (float) zombie.getGridPosition().getX();
        float y = zombie.getContinuousPosition() != null
                ? zombie.getContinuousPosition().getY()
                : (float) zombie.getGridPosition().getY();
        int maxHp = zombie.getDefinition() != null
                ? zombie.getDefinition().getBaseHP() : zombie.getCurrentHP();
        int armorHp = 0;
        if (zombie.getArmors() != null) {
            for (Armor a : zombie.getArmors()) {
                if (a != null) {
                    armorHp += a.getCurrentHealth();
                }
            }
        }
        String state = zombie.getState() != null ? zombie.getState().name() : "WALKING";
        String name = zombie.getDefinition() != null
                ? zombie.getDefinition().getName() : "Unknown";
        return new ZombieSnapshotDto(
                id, name, row, x, y, zombie.getCurrentHP(), maxHp, armorHp, state,
                zombie.getCurrentSpeed(), zombie.isChilled(), zombie.isFrozen(),
                zombie.isButtered(), zombie.isHypnotized());
    }
}
