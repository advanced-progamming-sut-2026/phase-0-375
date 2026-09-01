package model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import model.enums.Chapter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    public static final Set<String> STARTER_PLANTS = Set.of("Sunflower", "Peashooter", "Wall-nut", "Potato Mine");

    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private String gender;

    private int securityQuestionNumber;
    private String securityAnswer;

    private boolean stayLoggedIn;

    private int coins;
    private int gems;

    private int difficultyLevel;
    private int gameSpeed = 1;
    private boolean showLawnGrid;
    private boolean debugMode;
    private float musicVolume = 1f;
    private float sfxVolume = 1f;

    private int gamesPlayed;

    private int highestMyopoint;

    private Map<Chapter, Integer> chapterProgress;

    private Set<String> unlockedPlants;
    private Set<String> unlockedZombies;
    private Set<String> unlockedMiniGames;
    private Set<String> unlockedLevels;

    private int plantFoodCount;

    private Map<String, Integer> seedPackets;
    private Map<String, Boolean> plantBoosts;
    private Map<String, Integer> plantLevels;

    private int unlockedPots;
    private Map<String, String> greenhousePots;
    private Map<String, Long> greenhousePlantTimestamps;

    private List<String> readNews;
    private Map<String, String> newsPublishDates;

    private int completedDailyQuests;
    private int completedNonDailyQuests;
    private int completedMiniGames;

    private Map<String, Boolean> questStatus;
    private Map<String, Integer> questProgress;
    private Map<String, Boolean> purchasedDailyDeals;

    // Daily offer persistence (plant + date)
    private String dailyOfferPlant;
    private String dailyOfferDate;

    // Last date the daily quests were refreshed
    private String dailyQuestRefreshDate;

    public User() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getSecurityQuestionNumber() {
        return securityQuestionNumber;
    }

    public void setSecurityQuestionNumber(int securityQuestionNumber) {
        this.securityQuestionNumber = securityQuestionNumber;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    public boolean isStayLoggedIn() {
        return stayLoggedIn;
    }

    public void setStayLoggedIn(boolean stayLoggedIn) {
        this.stayLoggedIn = stayLoggedIn;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = gems;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public int getGameSpeed() {
        return gameSpeed < 1 ? 1 : Math.min(gameSpeed, 3);
    }

    public void setGameSpeed(int gameSpeed) {
        this.gameSpeed = gameSpeed;
    }

    public boolean isShowLawnGrid() {
        return showLawnGrid;
    }

    public void setShowLawnGrid(boolean showLawnGrid) {
        this.showLawnGrid = showLawnGrid;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public float getMusicVolume() {
        return clamp01(musicVolume);
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
    }

    public float getSfxVolume() {
        return clamp01(sfxVolume);
    }

    public void setSfxVolume(float sfxVolume) {
        this.sfxVolume = sfxVolume;
    }

    private static float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getHighestMyopoint() {
        return highestMyopoint;
    }

    public void setHighestMyopoint(int highestMyopoint) {
        this.highestMyopoint = highestMyopoint;
    }

    public Map<Chapter, Integer> getChapterProgress() {
        return chapterProgress;
    }

    public void setChapterProgress(Map<Chapter, Integer> chapterProgress) {
        this.chapterProgress = chapterProgress;
    }

    public Set<String> getUnlockedPlants() {
        return unlockedPlants;
    }

    public void setUnlockedPlants(Set<String> unlockedPlants) {
        this.unlockedPlants = unlockedPlants;
    }

    public Set<String> getUnlockedZombies() {
        return unlockedZombies;
    }

    public void setUnlockedZombies(Set<String> unlockedZombies) {
        this.unlockedZombies = unlockedZombies;
    }

    public Set<String> getUnlockedMiniGames() {
        return unlockedMiniGames;
    }

    public void setUnlockedMiniGames(Set<String> unlockedMiniGames) {
        this.unlockedMiniGames = unlockedMiniGames;
    }

    public Set<String> getUnlockedLevels() {
        return unlockedLevels;
    }

    public void setUnlockedLevels(Set<String> unlockedLevels) {
        this.unlockedLevels = unlockedLevels;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public void setPlantFoodCount(int plantFoodCount) {
        this.plantFoodCount = plantFoodCount;
    }

    public Map<String, Integer> getSeedPackets() {
        return seedPackets;
    }

    public void setSeedPackets(Map<String, Integer> seedPackets) {
        this.seedPackets = seedPackets;
    }

    public Map<String, Boolean> getPlantBoosts() {
        return plantBoosts;
    }

    public void setPlantBoosts(Map<String, Boolean> plantBoosts) {
        this.plantBoosts = plantBoosts;
    }

    public Map<String, Integer> getPlantLevels() {
        return plantLevels;
    }

    public void setPlantLevels(Map<String, Integer> plantLevels) {
        this.plantLevels = plantLevels;
    }

    public int getUnlockedPots() {
        return unlockedPots;
    }

    public void setUnlockedPots(int unlockedPots) {
        this.unlockedPots = unlockedPots;
    }

    public Map<String, String> getGreenhousePots() {
        return greenhousePots;
    }

    public void setGreenhousePots(Map<String, String> greenhousePots) {
        this.greenhousePots = greenhousePots;
    }

    public Map<String, Long> getGreenhousePlantTimestamps() {
        return greenhousePlantTimestamps;
    }

    public void setGreenhousePlantTimestamps(Map<String, Long> greenhousePlantTimestamps) {
        this.greenhousePlantTimestamps = greenhousePlantTimestamps;
    }

    public List<String> getReadNews() {
        return readNews;
    }

    public void setReadNews(List<String> readNews) {
        this.readNews = readNews;
    }

    public Map<String, String> getNewsPublishDates() {
        return newsPublishDates;
    }

    public void setNewsPublishDates(Map<String, String> newsPublishDates) {
        this.newsPublishDates = newsPublishDates;
    }

    public LocalDate rememberNewsPublishDate(String newsId) {
        if (newsId == null || newsId.isBlank()) {
            return LocalDate.now();
        }
        if (newsPublishDates == null) {
            newsPublishDates = new HashMap<>();
        }
        String existing = newsPublishDates.get(newsId);
        if (existing != null && !existing.isBlank()) {
            try {
                return LocalDate.parse(existing);
            } catch (DateTimeParseException ignored) {
                // Fall through and rewrite a bad value.
            }
        }
        LocalDate today = LocalDate.now();
        newsPublishDates.put(newsId, today.toString());
        return today;
    }

    public int getCompletedDailyQuests() {
        return completedDailyQuests;
    }

    public void setCompletedDailyQuests(int completedDailyQuests) {
        this.completedDailyQuests = completedDailyQuests;
    }

    public int getCompletedNonDailyQuests() {
        return completedNonDailyQuests;
    }

    public void setCompletedNonDailyQuests(int completedNonDailyQuests) {
        this.completedNonDailyQuests = completedNonDailyQuests;
    }

    public int getCompletedMiniGames() {
        return completedMiniGames;
    }

    public void setCompletedMiniGames(int completedMiniGames) {
        this.completedMiniGames = completedMiniGames;
    }

    public Map<String, Boolean> getQuestStatus() {
        return questStatus;
    }

    public void setQuestStatus(Map<String, Boolean> questStatus) {
        this.questStatus = questStatus;
    }

    public Map<String, Integer> getQuestProgress() {
        return questProgress;
    }

    public void setQuestProgress(Map<String, Integer> questProgress) {
        this.questProgress = questProgress;
    }

    public String getDailyQuestRefreshDate() {
        return dailyQuestRefreshDate;
    }

    public void setDailyQuestRefreshDate(String dailyQuestRefreshDate) {
        this.dailyQuestRefreshDate = dailyQuestRefreshDate;
    }

    public Map<String, Boolean> getPurchasedDailyDeals() {
        return purchasedDailyDeals;
    }

    public void setPurchasedDailyDeals(Map<String, Boolean> purchasedDailyDeals) {
        this.purchasedDailyDeals = purchasedDailyDeals;
    }

    public String getDailyOfferPlant() {
        return dailyOfferPlant;
    }

    public void setDailyOfferPlant(String dailyOfferPlant) {
        this.dailyOfferPlant = dailyOfferPlant;
    }

    public String getDailyOfferDate() {
        return dailyOfferDate;
    }

    public void setDailyOfferDate(String dailyOfferDate) {
        this.dailyOfferDate = dailyOfferDate;
    }
}
