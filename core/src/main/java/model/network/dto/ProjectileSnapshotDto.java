package model.network.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectileSnapshotDto {
    private String id;
    private String projectileType;       // "PEA", "FIRE_PEA", "ICE_PEA", "CABBAGE", "MELON", "BOWLING_BULB"
    private int row;
    private float x;
    private float y;
    private float velocityX;
    private String element;              // "NONE", "ICE", "FIRE", "POISON", "BUTTER"

    public ProjectileSnapshotDto() {}

    public ProjectileSnapshotDto(String id, String projectileType, int row,
                                 float x, float y, float velocityX, String element) {
        this.id = id;
        this.projectileType = projectileType;
        this.row = row;
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.element = element;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectileType() { return projectileType; }
    public void setProjectileType(String projectileType) { this.projectileType = projectileType; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getVelocityX() { return velocityX; }
    public void setVelocityX(float velocityX) { this.velocityX = velocityX; }

    public String getElement() { return element; }
    public void setElement(String element) { this.element = element; }
}
