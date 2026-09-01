package model.network.packet.system;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class HeartbeatPacket extends Packet {
    private long clientTimestamp;
    private long serverTimestamp;
    private boolean pong;

    public HeartbeatPacket() {
        super(PacketType.HEARTBEAT);
    }

    public HeartbeatPacket(long clientTimestamp) {
        this(clientTimestamp, 0L, false);
    }

    public HeartbeatPacket(long clientTimestamp, long serverTimestamp, boolean pong) {
        super(PacketType.HEARTBEAT);
        this.clientTimestamp = clientTimestamp;
        this.serverTimestamp = serverTimestamp;
        this.pong = pong;
    }

    public long getClientTimestamp() { return clientTimestamp; }
    public void setClientTimestamp(long clientTimestamp) { this.clientTimestamp = clientTimestamp; }

    public long getServerTimestamp() { return serverTimestamp; }
    public void setServerTimestamp(long serverTimestamp) { this.serverTimestamp = serverTimestamp; }

    public boolean isPong() { return pong; }
    public void setPong(boolean pong) { this.pong = pong; }

    public long getTimestamp() {
        return serverTimestamp != 0L ? serverTimestamp : clientTimestamp;
    }
}
