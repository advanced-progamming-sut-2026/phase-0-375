package model.network.packet.user;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class PasswordResetResponsePacket extends Packet {
    private boolean success;
    private String message;

    public PasswordResetResponsePacket() {
        super(PacketType.PASSWORD_RESET_RESPONSE);
    }

    public PasswordResetResponsePacket(boolean success, String message) {
        super(PacketType.PASSWORD_RESET_RESPONSE);
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
