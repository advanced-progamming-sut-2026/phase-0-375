package model.network.packet.user;

import model.network.enums.LeaderboardCategory;
import model.network.packet.Packet;
import model.network.packet.PacketType;

public class LeaderboardRequestPacket extends Packet {
    private LeaderboardCategory category;

    public LeaderboardRequestPacket() {
        super(PacketType.LEADERBOARD_REQUEST);
    }

    public LeaderboardRequestPacket(LeaderboardCategory category) {
        super(PacketType.LEADERBOARD_REQUEST);
        this.category = category;
    }

    public LeaderboardCategory getCategory() { return category; }
    public void setCategory(LeaderboardCategory category) { this.category = category; }
}
