package model.network.packet.game;

import model.network.dto.PlantSnapshotDto;
import model.network.dto.ProjectileSnapshotDto;
import model.network.dto.SunSnapshotDto;
import model.network.dto.ZombieSnapshotDto;
import model.network.packet.Packet;
import model.network.packet.PacketType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameStateSnapshotPacket extends Packet {
    private long tick;
    private float matchTime;
    private float timeRemaining;
    private int plantSun;
    private int zombieSun;
    private List<PlantSnapshotDto> plants = new ArrayList<>();
    private List<ZombieSnapshotDto> zombies = new ArrayList<>();
    private List<ProjectileSnapshotDto> projectiles = new ArrayList<>();
    private List<Integer> breachedRows = new ArrayList<>();
    private List<SunSnapshotDto> suns = new ArrayList<>();
    private Map<String, Float> plantSeedCooldowns = new HashMap<>();
    private float matchDuration;
    private boolean gameOver;
    private String winnerRole;           // null, "PLANT", "ZOMBIE", or "DRAW"
    private String endReason;            // "ALL_BRAINS_EATEN", "TIME_EXPIRED", "ZOMBIE_OUT_OF_SUN", etc.

    public GameStateSnapshotPacket() {
        super(PacketType.GAME_STATE_SNAPSHOT);
    }

    public GameStateSnapshotPacket(long tick, float matchTime, float timeRemaining,
                                  int plantSun, int zombieSun,
                                  List<PlantSnapshotDto> plants,
                                  List<ZombieSnapshotDto> zombies,
                                  List<ProjectileSnapshotDto> projectiles,
                                  List<Integer> breachedRows,
                                  boolean gameOver, String winnerRole, String endReason) {
        super(PacketType.GAME_STATE_SNAPSHOT);
        this.tick = tick;
        this.matchTime = matchTime;
        this.timeRemaining = timeRemaining;
        this.plantSun = plantSun;
        this.zombieSun = zombieSun;
        this.plants = plants != null ? plants : new ArrayList<>();
        this.zombies = zombies != null ? zombies : new ArrayList<>();
        this.projectiles = projectiles != null ? projectiles : new ArrayList<>();
        this.breachedRows = breachedRows != null ? breachedRows : new ArrayList<>();
        this.gameOver = gameOver;
        this.winnerRole = winnerRole;
        this.endReason = endReason;
    }

    public long getTick() { return tick; }
    public void setTick(long tick) { this.tick = tick; }

    public float getMatchTime() { return matchTime; }
    public void setMatchTime(float matchTime) { this.matchTime = matchTime; }

    public float getTimeRemaining() { return timeRemaining; }
    public void setTimeRemaining(float timeRemaining) { this.timeRemaining = timeRemaining; }

    public int getPlantSun() { return plantSun; }
    public void setPlantSun(int plantSun) { this.plantSun = plantSun; }

    public int getZombieSun() { return zombieSun; }
    public void setZombieSun(int zombieSun) { this.zombieSun = zombieSun; }

    public List<PlantSnapshotDto> getPlants() { return plants; }
    public void setPlants(List<PlantSnapshotDto> plants) { this.plants = plants != null ? plants : new ArrayList<>(); }

    public List<ZombieSnapshotDto> getZombies() { return zombies; }
    public void setZombies(List<ZombieSnapshotDto> zombies) { this.zombies = zombies != null ? zombies : new ArrayList<>(); }

    public List<ProjectileSnapshotDto> getProjectiles() { return projectiles; }
    public void setProjectiles(List<ProjectileSnapshotDto> projectiles) { this.projectiles = projectiles != null ? projectiles : new ArrayList<>(); }

    public List<Integer> getBreachedRows() { return breachedRows; }
    public void setBreachedRows(List<Integer> breachedRows) { this.breachedRows = breachedRows != null ? breachedRows : new ArrayList<>(); }

    public List<SunSnapshotDto> getSuns() { return suns; }
    public void setSuns(List<SunSnapshotDto> suns) { this.suns = suns != null ? suns : new ArrayList<>(); }

    public Map<String, Float> getPlantSeedCooldowns() { return plantSeedCooldowns; }
    public void setPlantSeedCooldowns(Map<String, Float> plantSeedCooldowns) {
        this.plantSeedCooldowns = plantSeedCooldowns != null ? plantSeedCooldowns : new HashMap<>();
    }

    public float getMatchDuration() { return matchDuration; }
    public void setMatchDuration(float matchDuration) { this.matchDuration = matchDuration; }

    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

    public String getWinnerRole() { return winnerRole; }
    public void setWinnerRole(String winnerRole) { this.winnerRole = winnerRole; }

    public String getEndReason() { return endReason; }
    public void setEndReason(String endReason) { this.endReason = endReason; }
}
