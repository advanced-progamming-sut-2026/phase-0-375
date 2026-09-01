package model.network.packet;

public class CancelInvitePacket extends Packet {
    private String inviteId;
    private String targetUsername;

    public CancelInvitePacket() {
        super(PacketType.CANCEL_INVITE);
    }

    public CancelInvitePacket(String inviteId, String targetUsername) {
        super(PacketType.CANCEL_INVITE);
        this.inviteId = inviteId;
        this.targetUsername = targetUsername;
    }

    public String getInviteId() {
        return inviteId;
    }

    public void setInviteId(String inviteId) {
        this.inviteId = inviteId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }
}
