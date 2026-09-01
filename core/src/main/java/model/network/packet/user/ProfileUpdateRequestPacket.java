package model.network.packet.user;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class ProfileUpdateRequestPacket extends Packet {
    private String username;
    private String nickname;
    private String email;
    /** When set, updates the joust avatar id (1–30). */
    private Integer avatarId;

    public ProfileUpdateRequestPacket() {
        super(PacketType.PROFILE_UPDATE_REQUEST);
    }

    public ProfileUpdateRequestPacket(String username, String nickname, String email) {
        this(username, nickname, email, null);
    }

    public ProfileUpdateRequestPacket(String username, String nickname, String email, Integer avatarId) {
        super(PacketType.PROFILE_UPDATE_REQUEST);
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.avatarId = avatarId;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAvatarId() { return avatarId; }
    public void setAvatarId(Integer avatarId) { this.avatarId = avatarId; }
}
