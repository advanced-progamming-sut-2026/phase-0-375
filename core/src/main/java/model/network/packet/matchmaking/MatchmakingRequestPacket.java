package model.network.packet.matchmaking;

import model.network.enums.MatchmakingMode;
import model.network.enums.PlayerRole;
import model.network.packet.Packet;
import model.network.packet.PacketType;

public class MatchmakingRequestPacket extends Packet {
    private MatchmakingMode mode;
    private String roomCode;             // null for RANDOM / CREATE_ROOM; required for DIRECT_INVITE
    private PlayerRole preferredRole;    // PLANT, ZOMBIE, or ANY
    private String username;

    public MatchmakingRequestPacket() {
        super(PacketType.MATCHMAKING_REQUEST);
    }

    public MatchmakingRequestPacket(MatchmakingMode mode, String roomCode, PlayerRole preferredRole) {
        this(mode, roomCode, preferredRole, null);
    }

    public MatchmakingRequestPacket(MatchmakingMode mode, String roomCode, PlayerRole preferredRole, String username) {
        super(PacketType.MATCHMAKING_REQUEST);
        this.mode = mode;
        this.roomCode = roomCode;
        this.preferredRole = preferredRole;
        this.username = username;
    }

    public MatchmakingMode getMode() { return mode; }
    public void setMode(MatchmakingMode mode) { this.mode = mode; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public PlayerRole getPreferredRole() { return preferredRole; }
    public void setPreferredRole(PlayerRole preferredRole) { this.preferredRole = preferredRole; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
