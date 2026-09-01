package model.network.packet.user;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class PasswordChangeResponsePacket extends Packet {
    private boolean success;
    private String message;

    public PasswordChangeResponsePacket() {
        super(PacketType.PASSWORD_CHANGE_RESPONSE);
    }

    public PasswordChangeResponsePacket(boolean success, String message) {
        super(PacketType.PASSWORD_CHANGE_RESPONSE);
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
