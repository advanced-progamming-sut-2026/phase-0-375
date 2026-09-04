package model.network.packet.user;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class SecurityQuestionRequestPacket extends Packet {
    private String username;
    private String email;

    public SecurityQuestionRequestPacket() {
        super(PacketType.SECURITY_QUESTION_REQUEST);
    }

    public SecurityQuestionRequestPacket(String username, String email) {
        super(PacketType.SECURITY_QUESTION_REQUEST);
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
