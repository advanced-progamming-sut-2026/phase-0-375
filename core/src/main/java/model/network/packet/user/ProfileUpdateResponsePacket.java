package model.network.packet.user;

import model.network.packet.Packet;
import model.network.packet.PacketType;
import model.user.User;

public class ProfileUpdateResponsePacket extends Packet {
    private boolean success;
    private String message;
    private User user;

    public ProfileUpdateResponsePacket() {
        super(PacketType.PROFILE_UPDATE_RESPONSE);
    }

    public ProfileUpdateResponsePacket(boolean success, String message, User user) {
        super(PacketType.PROFILE_UPDATE_RESPONSE);
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
