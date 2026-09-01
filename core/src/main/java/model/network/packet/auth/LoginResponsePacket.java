package model.network.packet.auth;

import model.network.packet.Packet;
import model.network.packet.PacketType;
import model.user.User;

public class LoginResponsePacket extends Packet {
    private boolean success;
    private String message;
    private User userProfile;
    /** Opaque stay-logged-in token; null when stay was not requested or resume failed. */
    private String sessionToken;

    public LoginResponsePacket() {
        super(PacketType.LOGIN_RESPONSE);
    }

    public LoginResponsePacket(boolean success, String message) {
        this(success, message, null, null);
    }

    public LoginResponsePacket(boolean success, String message, User userProfile) {
        this(success, message, userProfile, null);
    }

    public LoginResponsePacket(boolean success, String message, User userProfile, String sessionToken) {
        super(PacketType.LOGIN_RESPONSE);
        this.success = success;
        this.message = message;
        this.userProfile = userProfile;
        this.sessionToken = sessionToken;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public User getUserProfile() { return userProfile; }
    public void setUserProfile(User userProfile) { this.userProfile = userProfile; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}
