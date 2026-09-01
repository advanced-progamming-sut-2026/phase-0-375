package model.network.packet.auth;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class RegisterRequestPacket extends Packet {
    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private String gender;                 // "male", "female", etc.
    private int securityQuestionNumber;    // 1, 2, or 3
    private String securityAnswer;

    public RegisterRequestPacket() {
        super(PacketType.REGISTER_REQUEST);
    }

    public RegisterRequestPacket(String username, String passwordHash, String nickname,
                                 String email, String gender, int securityQuestionNumber,
                                 String securityAnswer) {
        super(PacketType.REGISTER_REQUEST);
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.securityQuestionNumber = securityQuestionNumber;
        this.securityAnswer = securityAnswer;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getSecurityQuestionNumber() { return securityQuestionNumber; }
    public void setSecurityQuestionNumber(int securityQuestionNumber) { this.securityQuestionNumber = securityQuestionNumber; }

    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }
}
