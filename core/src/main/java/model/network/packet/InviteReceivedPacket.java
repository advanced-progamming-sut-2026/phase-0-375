package model.network.packet;

import model.network.enums.PlayerRole;

public class InviteReceivedPacket extends Packet {
    private String inviteId;
    private String inviterUsername;
    private PlayerRole inviterRole;
    private int timeoutSeconds;

    public InviteReceivedPacket() {
        super(PacketType.INVITE_RECEIVED);
    }

    public InviteReceivedPacket(String inviteId, String inviterUsername, PlayerRole inviterRole) {
        this(inviteId, inviterUsername, inviterRole, 10);
    }

    public InviteReceivedPacket(String inviteId, String inviterUsername, PlayerRole inviterRole, int timeoutSeconds) {
        super(PacketType.INVITE_RECEIVED);
        this.inviteId = inviteId;
        this.inviterUsername = inviterUsername;
        this.inviterRole = inviterRole != null ? inviterRole : PlayerRole.ANY;
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;
    }

    public String getInviteId() {
        return inviteId;
    }

    public void setInviteId(String inviteId) {
        this.inviteId = inviteId;
    }

    public String getInviterUsername() {
        return inviterUsername;
    }

    public void setInviterUsername(String inviterUsername) {
        this.inviterUsername = inviterUsername;
    }

    public PlayerRole getInviterRole() {
        return inviterRole;
    }

    public void setInviterRole(PlayerRole inviterRole) {
        this.inviterRole = inviterRole;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
