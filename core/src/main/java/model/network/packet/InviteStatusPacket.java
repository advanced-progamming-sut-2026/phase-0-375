package model.network.packet;

import model.network.enums.InviteStatus;

public class InviteStatusPacket extends Packet {
    private String inviteId;
    private InviteStatus status;
    private String message;

    public InviteStatusPacket() {
        super(PacketType.INVITE_STATUS);
    }

    public InviteStatusPacket(String inviteId, InviteStatus status) {
        this(inviteId, status, null);
    }

    public InviteStatusPacket(String inviteId, InviteStatus status, String message) {
        super(PacketType.INVITE_STATUS);
        this.inviteId = inviteId;
        this.status = status;
        this.message = message;
    }

    public String getInviteId() {
        return inviteId;
    }

    public void setInviteId(String inviteId) {
        this.inviteId = inviteId;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public void setStatus(InviteStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
