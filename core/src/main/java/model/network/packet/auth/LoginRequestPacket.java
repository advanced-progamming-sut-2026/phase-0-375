package model.network.packet.auth;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class LoginRequestPacket extends Packet {
    private String username;
    private String passwordHash;
    private boolean stayLoggedIn;

    public LoginRequestPacket() {
        super(PacketType.LOGIN_REQUEST);
    }

    public LoginRequestPacket(String username, String passwordHash, boolean stayLoggedIn) {
        super(PacketType.LOGIN_REQUEST);
        this.username = username;
        this.passwordHash = passwordHash;
        this.stayLoggedIn = stayLoggedIn;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isStayLoggedIn() { return stayLoggedIn; }
    public void setStayLoggedIn(boolean stayLoggedIn) { this.stayLoggedIn = stayLoggedIn; }
}
