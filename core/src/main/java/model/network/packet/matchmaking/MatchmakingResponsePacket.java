package model.network.packet.matchmaking;

import model.network.enums.MatchmakingStatus;
import model.network.packet.Packet;
import model.network.packet.PacketType;

public class MatchmakingResponsePacket extends Packet {
    private MatchmakingStatus status;
    private String roomCode;
    private String message;

    public MatchmakingResponsePacket() {
        super(PacketType.MATCHMAKING_RESPONSE);
    }

    public MatchmakingResponsePacket(MatchmakingStatus status, String roomCode, String message) {
        super(PacketType.MATCHMAKING_RESPONSE);
        this.status = status;
        this.roomCode = roomCode;
        this.message = message;
    }

    public MatchmakingStatus getStatus() { return status; }
    public void setStatus(MatchmakingStatus status) { this.status = status; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
