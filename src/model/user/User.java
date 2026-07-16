package model.user;

import model.enums.Chapter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class User {
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

    private int gamesPlayed;

    private int highestMyopoint;

    private Map<Chapter, Integer> chapterProgress;

    private Set<String> unlockedPlants;
    private Set<String> unlockedZombies;
    private Set<String> unlockedMiniGames;

    private int plantFoodCount;

    private Map<String, Integer> seedPackets;
    private Map<String, Boolean> plantBoosts;
    private Map<String, Integer> plantLevels;

    private int unlockedPots;
    private Map<String, String> greenhousePots;
    private Map<String, Long> greenhousePlantTimestamps;

    private List<String> readNews;

    private int completedDailyQuests;
    private int completedNonDailyQuests;
    private int completedMiniGames;

    private Map<String, Boolean> questStatus;
    private Map<String, Boolean> purchasedDailyDeals;

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

    public Map<String, Boolean> getPurchasedDailyDeals() {
        return purchasedDailyDeals;
    }

    public void setPurchasedDailyDeals(Map<String, Boolean> purchasedDailyDeals) {
        this.purchasedDailyDeals = purchasedDailyDeals;
    }
}