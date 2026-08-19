package model.game.systems;

import model.enums.PlantCategory;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.game.map.Cell;
import model.game.map.Point;
import model.item.Grave;
import model.plant.ability.*;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;

public class PlantSystem implements Tickable {

    private final GameModel gameModel;
    private final GameModelPlantAbilityContext context;

    public PlantSystem(GameModel gameModel) {
        this.gameModel = gameModel;
        this.context = new GameModelPlantAbilityContext(gameModel);
    }

    public void setClipDurations(PlantClipDurations clipDurations) {
        context.setClipDurations(clipDurations != null ? clipDurations : PlantClipDurations.NONE);
    }

    public void setProjectileOrigins(PlantProjectileOrigins projectileOrigins) {
        context.setProjectileOrigins(projectileOrigins != null ? projectileOrigins : PlantProjectileOrigins.NONE);
    }

    @Override
    public void tick(float deltaTime) {
        List<PlantInstance> snapshot = new ArrayList<>(gameModel.getAllPlants());
        for (PlantInstance plant : snapshot) {
            if (plant.getState() == PlantState.DYING) continue;
            context.setCurrentPlant(plant); // attribute this tick's damage/projectiles
            plant.tick(deltaTime, context);
            if (plant.isImitating()) {
                continue;
            }
            if (plant.consumePendingArmorExplosion()) {
                triggerDeathExplosionIfNeeded(plant);
            }
            if (plant.getCurrentHP() <= 0 && plant.getState() != PlantState.DYING
                    && plant.getState() != PlantState.ATTACKING
                    && !plant.hasActiveAction()) {
                // Explode-o-nut: trigger the death explosion before
                // removing the plant from the field.
                triggerDeathExplosionIfNeeded(plant);
                plant.setState(PlantState.DYING);
                gameModel.destroyPlant(plant);
            }
        }
        context.setCurrentPlant(null);
        sweepDeadPlants();
    }

    /**
     * Instant explosives (Doom-shroom, Cherry Bomb, ...) spawn at 0 HP so they
     * must tick once and start their attack clip before leaving the cell.
     */
    static boolean shouldLeaveField(PlantInstance plant) {
        if (plant == null) {
            return false;
        }
        if (plant.getState() == PlantState.DYING) {
            return true;
        }
        if (plant.getCurrentHP() > 0) {
            return false;
        }
        return plant.getState() != PlantState.ATTACKING && !plant.hasActiveAction();
    }

    private void sweepDeadPlants() {
        for (PlantInstance plant : new ArrayList<>(gameModel.getAllPlants())) {
            if (!shouldLeaveField(plant)) {
                continue;
            }
            PlantAbility ability = plant.getAbilityStrategy();
            if (ability != null) {
                ability.onPlantDeath(plant, context);
            }
            Point pos = plant.getPosition();
            if (pos != null) {
                Cell cell = gameModel.getCellAt(pos.getY(), pos.getX());
                if (cell != null) {
                    cell.removePlaceable(plant);
                }
            }
        }
    }

    /**
     * If the dying plant is a WALL_NUT with the EXPLOSIVE tag
     * (Explode-o-nut), invokes the {@link WallAbility#onPlantDeath}
     * hook to detonate the plant in a 3x3 area.
     */
    private void triggerDeathExplosionIfNeeded(PlantInstance plant) {
        if (plant == null) return;
        Plant def = plant.getDefinition();
        if (def == null) return;
        if (def.getCategory() != PlantCategory.WALL_NUT) return;
        if (!def.hasTag(PlantTags.EXPLOSIVE)) return;

        // Reuse the singleton WallAbility strategy to fire the explosion.
        WallAbility wallAbility = new WallAbility();
        wallAbility.onPlantDeath(plant, context);
    }

    private static class GameModelPlantAbilityContext implements PlantAbilityContext {
        private final GameModel gameModel;
        private PlantInstance currentPlant; // plant currently ticking (kill attribution)
        private PlantClipDurations clipDurations = PlantClipDurations.NONE;
        private PlantProjectileOrigins projectileOrigins = PlantProjectileOrigins.NONE;


        GameModelPlantAbilityContext(GameModel gameModel) {
            this.gameModel = gameModel;
        }

        void setCurrentPlant(PlantInstance plant) { this.currentPlant = plant; }

        void setClipDurations(PlantClipDurations clipDurations) {
            this.clipDurations = clipDurations != null ? clipDurations : PlantClipDurations.NONE;
        }

        void setProjectileOrigins(PlantProjectileOrigins projectileOrigins) {
            this.projectileOrigins = projectileOrigins != null ? projectileOrigins : PlantProjectileOrigins.NONE;
        }

        private Plant currentDef() {
            return currentPlant != null ? currentPlant.getDefinition() : null;
        }

        @Override public int getSunAmount() { return gameModel.getSunAmount(); }
        @Override public int getRowCount() { return gameModel.getRowCount(); }
        @Override public int getColumnCount() { return gameModel.getColumnCount(); }

        @Override
        public PlantInstance getPlantAt(int row, int col) {
            return gameModel.getPlantAt(row, col);
        }

        @Override
        public List<PlantInstance> getPlantsInLane(int lane) {
            return gameModel.getPlantsInLane(lane);
        }

        @Override
        public List<PlantInstance> getAllPlants() {
            return gameModel.getAllPlants();
        }

        @Override
        public List<ZombieInstance> getZombiesInLane(int lane) {
            return gameModel.getZombiesInLane(lane);
        }

        @Override
        public List<ZombieInstance> getZombiesInArea(int row, int col, int rowRadius, int colRadius) {
            return gameModel.getZombiesInArea(row, col, rowRadius, colRadius);
        }

        @Override
        public boolean hasZombieInLane(int lane) {
            return !gameModel.getZombiesInLane(lane).isEmpty();
        }

        @Override
        public boolean hasZombieOrGraveAhead(int row, float plantX, int direction) {
            return hasZombieOrGraveAheadInRange(row, plantX, direction, Float.MAX_VALUE);
        }

        @Override
        public boolean hasZombieOrGraveAheadInRange(int row, float plantX, int direction, float maxRange) {
            for (ZombieInstance zombie : gameModel.getZombiesInLane(row)) {
                if (zombie == null || zombie.isDead() || zombie.getContinuousPosition() == null) continue;
                if (zombie.isHypnotized()) continue;
                float dx = zombie.getContinuousX() - plantX;
                if (direction > 0 && dx > 0f && dx <= maxRange) return true;
                if (direction < 0 && dx < 0f && -dx <= maxRange) return true;
            }
            int startCol = (int) plantX + direction;
            int endCol = direction > 0 ? gameModel.getColumnCount() : -1;
            int step = direction;
            for (int col = startCol; col != endCol; col += step) {
                float dist = Math.abs(col - plantX);
                if (dist > maxRange) break;
                if (gameModel.getGraveAt(row, col) != null) return true;
            }
            return false;
        }

        @Override
        public boolean hasZombieAlongDiagonal(int startRow, float startX, int dx, float dy, int maxRows, int maxCols) {
            float x = startX + dx;
            float y = startRow + dy;
            while (x >= 0 && x < maxCols && y >= 0 && y < maxRows) {
                int row = Math.round(y);
                if (row >= 0 && row < maxRows) {
                    for (ZombieInstance zombie : gameModel.getZombiesInLane(row)) {
                        if (zombie == null || zombie.isDead() || zombie.getContinuousPosition() == null) continue;
                        if (zombie.isHypnotized()) continue;
                        float zdx = zombie.getContinuousX() - startX;
                        float zdy = zombie.getContinuousY() - startRow;
                        if (dx > 0 && zdx > 0 || dx < 0 && zdx < 0) {
                            if (Math.abs(zdy - (zdx / dx) * dy) < 1.0f) return true;
                        }
                    }
                }
                x += dx;
                y += dy;
            }
            return false;
        }

        @Override
        public boolean hasAdjacentZombie(int row, int col) {
            for (int rowDist = -1; rowDist <= 1; rowDist++) {
                for (int colDist = -1; colDist <= 1; colDist++) {
                    if (rowDist == 0 && colDist == 0) continue;
                    if (!gameModel.getZombiesInArea(row + rowDist, col + colDist, 0, 0).isEmpty()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean isNightLevel() {
            return gameModel.isNightLevel();
        }

        @Override
        public boolean isWaterTile(int row, int col) {
            return gameModel.isWaterTile(row, col);
        }

        @Override
        public model.projectile.Projectile spawnProjectile(model.projectile.Projectile p, float x, float y) {
            if (p != null && p.getSourcePlant() == null) p.setSourcePlant(currentDef());
            gameModel.spawnProjectile(p, (int) x, (int) y);
            return p;
        }

        @Override
        public void spawnSun(model.item.Sun sun) {
            gameModel.spawnSun(sun);
        }

        @Override
        public void addSun(int amount) {
            gameModel.addSun(amount);
        }

        @Override
        public void damageZombie(ZombieInstance zombie, int damage) {
            gameModel.damageZombie(zombie, damage, currentDef());
        }

        @Override
        public void damageZombieWithFire(ZombieInstance zombie, int damage) {
            if (zombie == null) return;
            gameModel.attributePlantDamage(zombie, currentDef());
            zombie.takeFireDamage(damage);
        }

        @Override
        public void damagePlant(PlantInstance plant, int damage) {
            gameModel.damagePlant(plant, damage);
        }

        @Override
        public void destroyPlant(PlantInstance plant) {
            gameModel.destroyPlant(plant);
        }

        @Override
        public boolean placePlant(PlantInstance plant, int row, int col) {
            return gameModel.placePlant(plant, row, col);
        }

        @Override
        public boolean moveZombieToLane(ZombieInstance zombie, int newRow) {
            return gameModel.moveZombieToLane(zombie, newRow);
        }

        @Override
        public void pushZombieBack(ZombieInstance zombie, float tiles) {
            gameModel.pushZombieBack(zombie, tiles);
        }

        @Override
        public void triggerFamilyPlantFood(model.enums.PlantCategory family) {
            boolean resetCooldowns = false;
            for (PlantInstance plant : new ArrayList<>(gameModel.getAllPlants())) {
                if (plant.getDefinition().getCategory() == family) {
                    if (hasResetFamilyCooldownsUpgrade(plant)) {
                        resetCooldowns = true;
                    }
                }
            }

            PlantInstance mint = currentPlant;
            for (PlantInstance plant : new ArrayList<>(gameModel.getAllPlants())) {
                if (plant.getDefinition().getCategory() == family) {
                    if (PlantInstance.isMint(plant.getDefinition())) {
                        continue;
                    }
                    setCurrentPlant(plant);
                    plant.activatePlantFood(this);
                    if (resetCooldowns) {
                        // Clear every ability cooldown on this plant.
                        for (AbilityState state : plant.getAbilityStates().values()) {
                            state.setCooldownRemaining(0f);
                        }
                        plant.setCurrentRecharge(0f);
                    }
                }
            }
            setCurrentPlant(mint);
        }

        /** @return true if the plant has the RESET_FAMILY_COOLDOWNS upgrade. */
        private boolean hasResetFamilyCooldownsUpgrade(PlantInstance plant) {
            Plant def = plant.getDefinition();
            if (def == null || def.getLevels() == null) return false;
            for (int lvl = 2; lvl <= 4; lvl++) {
                if (lvl > plant.getLevel()) break;
                LevelUpgrade upgrade = def.getLevels().getUpgrade(lvl);
                if (upgrade == null) continue;
                if (upgrade.isSpecialMechanic()
                        && upgrade.getSpecialTag() == model.enums.PlantSpecialTag.RESET_FAMILY_COOLDOWNS) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void damageIceInArea(int row, int col, int rowRadius, int colRadius, int damage) {
            gameModel.damageIceInArea(row, col, rowRadius, colRadius, damage);
        }

        @Override
        public boolean removeGraveAt(int row, int col) {
            model.item.Grave grave = gameModel.getGraveAt(row, col);
            if (grave == null) {
                return false;
            }
            grave.applyLoot(gameModel);
            return gameModel.removeGraveAt(row, col);
        }

        @Override
        public void createCraterAt(int row, int col) {
            gameModel.createCraterAt(row, col);
        }

        @Override
        public ZombieInstance spawnZombieAt(String zombieDefinitionName, int row, int col) {
            return gameModel.spawnZombieAt(zombieDefinitionName, row, col);
        }

        @Override
        public void removeZombie(ZombieInstance zombie) {
            gameModel.removeZombie(zombie);
        }

        @Override
        public float plantPresentationDuration(PlantInstance plant, PlantState presentation) {
            return clipDurations.duration(plant, presentation);
        }

        @Override
        public model.game.map.FloatPoint plantProjectileOrigin(PlantInstance plant) {
            return projectileOrigins.origin(plant);
        }
    }
}
