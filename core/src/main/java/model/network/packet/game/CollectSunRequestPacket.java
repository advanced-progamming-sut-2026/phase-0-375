package model.network.packet.game;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class CollectSunRequestPacket extends Packet {
    private int x;
    private int y;

    public CollectSunRequestPacket() {
        super(PacketType.COLLECT_SUN_REQUEST);
    }

    public CollectSunRequestPacket(int x, int y) {
        super(PacketType.COLLECT_SUN_REQUEST);
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
}
