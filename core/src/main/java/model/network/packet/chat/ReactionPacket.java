package model.network.packet.chat;

import model.network.enums.ReactionType;
import model.network.packet.Packet;
import model.network.packet.PacketType;

public class ReactionPacket extends Packet {
    private String senderUsername;
    private ReactionType reactionType;   // EMOJI, TEXT, TAUNT, SURRENDER
    private String content;              // e.g. "SMILE", "GG", "BRAINS!", "SURRENDER"
    private long timestamp;

    public ReactionPacket() {
        super(PacketType.REACTION);
    }

    public ReactionPacket(String senderUsername, String reactionType, String content) {
        this(senderUsername, ReactionType.valueOf(reactionType.toUpperCase()), content, System.currentTimeMillis());
    }

    public ReactionPacket(String senderUsername, ReactionType reactionType, String content) {
        this(senderUsername, reactionType, content, System.currentTimeMillis());
    }

    public ReactionPacket(String senderUsername, ReactionType reactionType, String content, long timestamp) {
        super(PacketType.REACTION);
        this.senderUsername = senderUsername;
        this.reactionType = reactionType;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public ReactionType getReactionType() { return reactionType; }
    public void setReactionType(ReactionType reactionType) { this.reactionType = reactionType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
