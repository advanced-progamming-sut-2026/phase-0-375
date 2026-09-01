package model.network.util;

import model.user.User;

/**
 * Strips secrets before sending a User profile over the wire.
 */
public final class UserSanitizer {
    private UserSanitizer() {}

    public static User sanitize(User source) {
        if (source == null) {
            return null;
        }
        // Shallow copy via field copy so we don't mutate the repository instance.
        User u = copyShallow(source);
        u.setPasswordHash(null);
        u.setSecurityAnswer(null);
        return u;
    }

    private static User copyShallow(User s) {
        User u = new User();
        u.setUsername(s.getUsername());
        u.setNickname(s.getNickname());
        u.setEmail(s.getEmail());
        u.setGender(s.getGender());
        u.setSecurityQuestionNumber(s.getSecurityQuestionNumber());
        u.setStayLoggedIn(s.isStayLoggedIn());
        u.setCoins(s.getCoins());
        u.setGems(s.getGems());
        u.setDifficultyLevel(s.getDifficultyLevel());
        u.setGameSpeed(s.getGameSpeed());
        u.setShowLawnGrid(s.isShowLawnGrid());
        u.setDebugMode(s.isDebugMode());
        u.setMusicVolume(s.getMusicVolume());
        u.setSfxVolume(s.getSfxVolume());
        u.setGamesPlayed(s.getGamesPlayed());
        u.setHighestMyopoint(s.getHighestMyopoint());
        u.setChapterProgress(s.getChapterProgress());
        u.setUnlockedPlants(s.getUnlockedPlants());
        u.setUnlockedZombies(s.getUnlockedZombies());
        u.setUnlockedMiniGames(s.getUnlockedMiniGames());
        u.setUnlockedLevels(s.getUnlockedLevels());
        u.setPlantFoodCount(s.getPlantFoodCount());
        u.setSeedPackets(s.getSeedPackets());
        u.setPlantBoosts(s.getPlantBoosts());
        u.setPlantLevels(s.getPlantLevels());
        u.setUnlockedPots(s.getUnlockedPots());
        u.setGreenhousePots(s.getGreenhousePots());
        u.setGreenhousePlantTimestamps(s.getGreenhousePlantTimestamps());
        u.setReadNews(s.getReadNews());
        u.setNewsPublishDates(s.getNewsPublishDates());
        u.setCompletedDailyQuests(s.getCompletedDailyQuests());
        u.setCompletedNonDailyQuests(s.getCompletedNonDailyQuests());
        u.setCompletedMiniGames(s.getCompletedMiniGames());
        u.setQuestStatus(s.getQuestStatus());
        u.setQuestProgress(s.getQuestProgress());
        u.setPurchasedDailyDeals(s.getPurchasedDailyDeals());
        u.setDailyOfferPlant(s.getDailyOfferPlant());
        u.setDailyOfferDate(s.getDailyOfferDate());
        u.setDailyQuestRefreshDate(s.getDailyQuestRefreshDate());
        return u;
    }
}
