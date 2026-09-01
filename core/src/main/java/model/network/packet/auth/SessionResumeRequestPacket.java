package model.network.packet.auth;

import model.network.packet.Packet;
import model.network.packet.PacketType;

/** Client presents a previously issued stay-logged-in token to resume a TCP session. */
public class SessionResumeRequestPacket extends Packet {
    private String token;

    public SessionResumeRequestPacket() {
        super(PacketType.SESSION_RESUME_REQUEST);
    }

    public SessionResumeRequestPacket(String token) {
        super(PacketType.SESSION_RESUME_REQUEST);
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
