package model.network.packet.auth;

import model.network.packet.Packet;
import model.network.packet.PacketType;

/** Step 1 registration: profile fields only; validated authoritatively on the server. */
public class RegisterValidateRequestPacket extends Packet {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String gender;

    public RegisterValidateRequestPacket() {
        super(PacketType.REGISTER_VALIDATE_REQUEST);
    }

    public RegisterValidateRequestPacket(String username, String password, String nickname,
                                         String email, String gender) {
        super(PacketType.REGISTER_VALIDATE_REQUEST);
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
