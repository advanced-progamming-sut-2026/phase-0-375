package model.network.packet;

import model.network.enums.PlayerRole;

public class InviteRequestPacket extends Packet {
    private String targetUsername;
    private PlayerRole preferredRole;
    private String inviterUsername;

    public InviteRequestPacket() {
        super(PacketType.INVITE_REQUEST);
    }

    public InviteRequestPacket(String targetUsername, PlayerRole preferredRole) {
        this(targetUsername, preferredRole, null);
    }

    public InviteRequestPacket(String targetUsername, PlayerRole preferredRole, String inviterUsername) {
        super(PacketType.INVITE_REQUEST);
        this.targetUsername = targetUsername;
        this.preferredRole = preferredRole != null ? preferredRole : PlayerRole.ANY;
        this.inviterUsername = inviterUsername;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public PlayerRole getPreferredRole() {
        return preferredRole;
    }

    public void setPreferredRole(PlayerRole preferredRole) {
        this.preferredRole = preferredRole;
    }

    public String getInviterUsername() {
        return inviterUsername;
    }

    public void setInviterUsername(String inviterUsername) {
        this.inviterUsername = inviterUsername;
    }
}
