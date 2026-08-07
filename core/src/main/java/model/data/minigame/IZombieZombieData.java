package model.data.minigame;

/**
 * JSON DTO for one placeable-zombie roster entry in an I, Zombie stage
 * (see the "placeableZombies" key in minigames.json).
 */
public class IZombieZombieData {

    /** Zombie definition name (an alias from zombies.json). */
    private String zombie;
    /** Sun cost the player pays to place one. */
    private int cost = 50;

    public String getZombie() { return zombie; }
    public void setZombie(String zombie) { this.zombie = zombie; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
}
