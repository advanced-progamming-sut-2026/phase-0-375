package model.network.packet.game;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class PlaceZombieRequestPacket extends Packet {
    private String zombieName;
    private int row;
    private int col;

    public PlaceZombieRequestPacket() {
        super(PacketType.PLACE_ZOMBIE_REQUEST);
    }

    public PlaceZombieRequestPacket(String zombieName, int row, int col) {
        super(PacketType.PLACE_ZOMBIE_REQUEST);
        this.zombieName = zombieName;
        this.row = row;
        this.col = col;
    }

    public String getZombieName() { return zombieName; }
    public void setZombieName(String zombieName) { this.zombieName = zombieName; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }
}
