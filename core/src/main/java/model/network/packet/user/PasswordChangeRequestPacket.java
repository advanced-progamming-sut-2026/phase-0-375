package model.network.packet.user;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class PasswordChangeRequestPacket extends Packet {
    private String oldPasswordHash;
    private String newPassword;

    public PasswordChangeRequestPacket() {
        super(PacketType.PASSWORD_CHANGE_REQUEST);
    }

    public PasswordChangeRequestPacket(String oldPasswordHash, String newPassword) {
        super(PacketType.PASSWORD_CHANGE_REQUEST);
        this.oldPasswordHash = oldPasswordHash;
        this.newPassword = newPassword;
    }

    public String getOldPasswordHash() { return oldPasswordHash; }
    public void setOldPasswordHash(String oldPasswordHash) { this.oldPasswordHash = oldPasswordHash; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
