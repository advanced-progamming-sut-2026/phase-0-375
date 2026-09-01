package model.network.packet;

import model.network.enums.InviteDecision;

public class InviteResponsePacket extends Packet {
    private String inviteId;
    private String inviterUsername;
    private InviteDecision decision;
    private String reason;

    public InviteResponsePacket() {
        super(PacketType.INVITE_RESPONSE);
    }

    public InviteResponsePacket(String inviteId, String inviterUsername, InviteDecision decision) {
        this(inviteId, inviterUsername, decision, null);
    }

    public InviteResponsePacket(String inviteId, String inviterUsername, InviteDecision decision, String reason) {
        super(PacketType.INVITE_RESPONSE);
        this.inviteId = inviteId;
        this.inviterUsername = inviterUsername;
        this.decision = decision;
        this.reason = reason;
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

    public InviteDecision getDecision() {
        return decision;
    }

    public void setDecision(InviteDecision decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
