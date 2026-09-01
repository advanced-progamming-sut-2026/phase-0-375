package model.network.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZombieSnapshotDto {
    private String id;
    private String zombieName;
    private int row;
    private float x;                     // Continuous horizontal coordinate (e.g. 8.5 to -1.0)
    private float y;                     // Continuous vertical coordinate / lane offset
    private int currentHP;
    private int maxHP;
    private int armorHP;
    private String state;                // "SPAWNING", "WALKING", "EATING", "DYING"
    private float speed;
    private boolean chilled;
    private boolean frozen;
    private boolean buttered;
    private boolean hypnotized;

    public ZombieSnapshotDto() {}

    public ZombieSnapshotDto(String id, String zombieName, int row, float x, float y,
                             int currentHP, int maxHP, int armorHP, String state, float speed,
                             boolean chilled, boolean frozen, boolean buttered, boolean hypnotized) {
        this.id = id;
        this.zombieName = zombieName;
        this.row = row;
        this.x = x;
        this.y = y;
        this.currentHP = currentHP;
        this.maxHP = maxHP;
        this.armorHP = armorHP;
        this.state = state;
        this.speed = speed;
        this.chilled = chilled;
        this.frozen = frozen;
        this.buttered = buttered;
        this.hypnotized = hypnotized;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getZombieName() { return zombieName; }
    public void setZombieName(String zombieName) { this.zombieName = zombieName; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public int getCurrentHP() { return currentHP; }
    public void setCurrentHP(int currentHP) { this.currentHP = currentHP; }

    public int getMaxHP() { return maxHP; }
    public void setMaxHP(int maxHP) { this.maxHP = maxHP; }

    public int getArmorHP() { return armorHP; }
    public void setArmorHP(int armorHP) { this.armorHP = armorHP; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public boolean isChilled() { return chilled; }
    public void setChilled(boolean chilled) { this.chilled = chilled; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    public boolean isButtered() { return buttered; }
    public void setButtered(boolean buttered) { this.buttered = buttered; }

    public boolean isHypnotized() { return hypnotized; }
    public void setHypnotized(boolean hypnotized) { this.hypnotized = hypnotized; }
}
