package model.game.save;

import model.enums.GroundType;

/** Snapshot of ice/fire/crater terrain on one cell. */
public class TerrainSave {
    private int row;
    private int col;
    private GroundType groundType;
    private String kind; // ICE / FIRE / CRATER / NONE
    private int iceHp;
    private boolean iceMelted;
    private String frozenZombieName;
    private float fireRemaining;

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }
    public GroundType getGroundType() { return groundType; }
    public void setGroundType(GroundType groundType) { this.groundType = groundType; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public int getIceHp() { return iceHp; }
    public void setIceHp(int iceHp) { this.iceHp = iceHp; }
    public boolean isIceMelted() { return iceMelted; }
    public void setIceMelted(boolean iceMelted) { this.iceMelted = iceMelted; }
    public String getFrozenZombieName() { return frozenZombieName; }
    public void setFrozenZombieName(String frozenZombieName) { this.frozenZombieName = frozenZombieName; }
    public float getFireRemaining() { return fireRemaining; }
    public void setFireRemaining(float fireRemaining) { this.fireRemaining = fireRemaining; }
}
