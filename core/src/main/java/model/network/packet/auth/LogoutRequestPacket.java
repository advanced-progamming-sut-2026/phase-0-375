package model.network.packet.auth;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class LogoutRequestPacket extends Packet {
    private String username;
    private String sessionToken;

    public LogoutRequestPacket() {
        super(PacketType.LOGOUT_REQUEST);
    }

    public LogoutRequestPacket(String username) {
        this(username, null);
    }

    public LogoutRequestPacket(String username, String sessionToken) {
        super(PacketType.LOGOUT_REQUEST);
        this.username = username;
        this.sessionToken = sessionToken;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}
