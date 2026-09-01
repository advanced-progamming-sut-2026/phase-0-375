package com.sut.server.service;

import model.news.NewsFactory;
import model.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Builds a newly registered user with default economy, settings, and collections.
 */
final class AuthUserFactory {

    private AuthUserFactory() {
    }

    static User create(
            String username,
            String passwordHash,
            String nickname,
            String email,
            String gender,
            int securityQuestionNumber,
            String securityAnswer
    ) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setGender(gender);
        user.setSecurityQuestionNumber(securityQuestionNumber);
        user.setSecurityAnswer(securityAnswer);
        applyDefaults(user);
        return user;
    }

    static void applyDefaults(User user) {
        applyEconomyAndSettings(user);
        applyEmptyCollections(user);
        for (String plant : User.STARTER_PLANTS) {
            user.rememberNewsPublishDate(NewsFactory.plantNewsId(plant));
        }
    }

    private static void applyEconomyAndSettings(User user) {
        user.setCoins(0);
        user.setGems(0);
        user.setDifficultyLevel(3);
        user.setGameSpeed(1);
        user.setShowLawnGrid(false);
        user.setDebugMode(false);
        user.setMusicVolume(1.0f);
        user.setSfxVolume(1.0f);
        user.setGamesPlayed(0);
        user.setPlantFoodCount(0);
        user.setUnlockedPots(4);
    }

    private static void applyEmptyCollections(User user) {
        user.setChapterProgress(new HashMap<>());
        user.setUnlockedPlants(new HashSet<>(User.STARTER_PLANTS));
        user.setUnlockedZombies(new HashSet<>());
        user.setUnlockedMiniGames(new HashSet<>());
        user.setUnlockedLevels(new HashSet<>());
        user.setSeedPackets(new HashMap<>());
        user.setPlantLevels(new HashMap<>());
        user.setPlantBoosts(new HashMap<>());
        user.setGreenhousePots(new HashMap<>());
        user.setGreenhousePlantTimestamps(new HashMap<>());
        user.setReadNews(new ArrayList<>());
        user.setNewsPublishDates(new HashMap<>());
        user.setQuestStatus(new HashMap<>());
        user.setQuestProgress(new HashMap<>());
        user.setPurchasedDailyDeals(new HashMap<>());
    }
}
