package model.network.packet.matchmaking;

import model.network.enums.PlayerRole;
import model.network.packet.Packet;
import model.network.packet.PacketType;

public class MatchFoundPacket extends Packet {
    private String roomId;
    private String opponentUsername;
    private PlayerRole assignedRole;     // Authoritative role: PLANT or ZOMBIE
    private int countdownSeconds;        // Default 3 or 5s before loop start

    public MatchFoundPacket() {
        super(PacketType.MATCH_FOUND);
    }

    public MatchFoundPacket(String roomId, String opponentUsername, PlayerRole assignedRole) {
        this(roomId, opponentUsername, assignedRole, 3);
    }

    public MatchFoundPacket(String roomId, String opponentUsername, PlayerRole assignedRole, int countdownSeconds) {
        super(PacketType.MATCH_FOUND);
        this.roomId = roomId;
        this.opponentUsername = opponentUsername;
        this.assignedRole = assignedRole;
        this.countdownSeconds = countdownSeconds;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getOpponentUsername() { return opponentUsername; }
    public void setOpponentUsername(String opponentUsername) { this.opponentUsername = opponentUsername; }

    public PlayerRole getAssignedRole() { return assignedRole; }
    public void setAssignedRole(PlayerRole assignedRole) { this.assignedRole = assignedRole; }

    public int getCountdownSeconds() { return countdownSeconds; }
    public void setCountdownSeconds(int countdownSeconds) { this.countdownSeconds = countdownSeconds; }
}
