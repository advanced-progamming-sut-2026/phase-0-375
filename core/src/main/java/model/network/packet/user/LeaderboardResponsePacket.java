package model.network.packet.user;

import model.network.enums.LeaderboardCategory;
import model.network.packet.Packet;
import model.network.packet.PacketType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardResponsePacket extends Packet {
    private boolean success;
    private String message;
    private LeaderboardCategory category;
    private List<LeaderboardEntryDto> entries = new ArrayList<>();

    public LeaderboardResponsePacket() {
        super(PacketType.LEADERBOARD_RESPONSE);
    }

    public LeaderboardResponsePacket(boolean success, String message, LeaderboardCategory category,
                                     List<LeaderboardEntryDto> entries) {
        super(PacketType.LEADERBOARD_RESPONSE);
        this.success = success;
        this.message = message;
        this.category = category;
        if (entries != null) {
            this.entries = entries;
        }
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LeaderboardCategory getCategory() { return category; }
    public void setCategory(LeaderboardCategory category) { this.category = category; }
    public List<LeaderboardEntryDto> getEntries() { return entries; }
    public void setEntries(List<LeaderboardEntryDto> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }

    /** Public leaderboard row — no secrets. */
    public static class LeaderboardEntryDto {
        private String username;
        private String nickname;
        private int highestMyopoint;
        private int completedMiniGames;
        private int completedDailyQuests;
        private int completedNonDailyQuests;
        /** Chapter name → level. */
        private Map<String, Integer> chapterProgress = new HashMap<>();

        public LeaderboardEntryDto() {}

        public LeaderboardEntryDto(String username, String nickname, int highestMyopoint,
                                   int completedMiniGames, int completedDailyQuests,
                                   int completedNonDailyQuests, Map<String, Integer> chapterProgress) {
            this.username = username;
            this.nickname = nickname;
            this.highestMyopoint = highestMyopoint;
            this.completedMiniGames = completedMiniGames;
            this.completedDailyQuests = completedDailyQuests;
            this.completedNonDailyQuests = completedNonDailyQuests;
            if (chapterProgress != null) {
                this.chapterProgress = chapterProgress;
            }
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public int getHighestMyopoint() { return highestMyopoint; }
        public void setHighestMyopoint(int highestMyopoint) { this.highestMyopoint = highestMyopoint; }
        public int getCompletedMiniGames() { return completedMiniGames; }
        public void setCompletedMiniGames(int completedMiniGames) { this.completedMiniGames = completedMiniGames; }
        public int getCompletedDailyQuests() { return completedDailyQuests; }
        public void setCompletedDailyQuests(int completedDailyQuests) {
            this.completedDailyQuests = completedDailyQuests;
        }
        public int getCompletedNonDailyQuests() { return completedNonDailyQuests; }
        public void setCompletedNonDailyQuests(int completedNonDailyQuests) {
            this.completedNonDailyQuests = completedNonDailyQuests;
        }
        public Map<String, Integer> getChapterProgress() { return chapterProgress; }
        public void setChapterProgress(Map<String, Integer> chapterProgress) {
            this.chapterProgress = chapterProgress != null ? chapterProgress : new HashMap<>();
        }

        /** @deprecated kept for older clients that only read {@code score}. */
        public int getScore() { return highestMyopoint; }
        public void setScore(int score) { this.highestMyopoint = score; }
    }
}
