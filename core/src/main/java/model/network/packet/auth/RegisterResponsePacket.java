package model.network.packet.auth;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class RegisterResponsePacket extends Packet {
    private boolean success;
    private String message;

    public RegisterResponsePacket() {
        super(PacketType.REGISTER_RESPONSE);
    }

    public RegisterResponsePacket(boolean success, String message) {
        super(PacketType.REGISTER_RESPONSE);
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
