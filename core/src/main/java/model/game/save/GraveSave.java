package model.game.save;

import model.item.Grave.GraveType;

/** Snapshot of one grave on the lawn. */
public class GraveSave {
    private int row;
    private int col;
    private int hp;
    private GraveType type = GraveType.PLAIN;

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public GraveType getType() { return type; }
    public void setType(GraveType type) { this.type = type; }
}
