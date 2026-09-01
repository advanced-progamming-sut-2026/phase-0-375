package model.network.packet.matchmaking;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class CancelMatchmakingPacket extends Packet {
    private String username;
    private String roomCode;
    private String reason;

    public CancelMatchmakingPacket() {
        super(PacketType.CANCEL_MATCHMAKING);
    }

    public CancelMatchmakingPacket(String username) {
        this(username, null, "USER_CANCELLED");
    }

    public CancelMatchmakingPacket(String username, String roomCode) {
        this(username, roomCode, "USER_CANCELLED");
    }

    public CancelMatchmakingPacket(String username, String roomCode, String reason) {
        super(PacketType.CANCEL_MATCHMAKING);
        this.username = username;
        this.roomCode = roomCode;
        this.reason = reason;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
