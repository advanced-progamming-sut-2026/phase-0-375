package com.sut.server.service;

import model.enums.Chapter;
import model.enums.PurchaseResult;
import model.network.enums.UserCommand;
import model.network.packet.user.UserCommandRequestPacket;
import model.shop.Shop;
import model.user.User;
import model.user.persistance.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Applies authoritative user-command mutations against {@link UserRepository}.
 */
final class UserCommandApplier {

    private final UserRepository userRepository;
    private final DailyOfferService dailyOfferService;

    UserCommandApplier(UserRepository userRepository, DailyOfferService dailyOfferService) {
        this.userRepository = userRepository;
        this.dailyOfferService = dailyOfferService;
    }

    String apply(String username, UserCommandRequestPacket packet) {
        UserCommand cmd = packet.getCommand();
        return switch (cmd) {
            case ADD_COINS, SPEND_COINS, ADD_GEMS, SPEND_GEMS,
                 ADD_SEED_PACKETS, SPEND_SEED_PACKETS,
                 ADD_PLANT_FOOD, USE_PLANT_FOOD,
                 STORE_PLANT_BOOST, CONSUME_PLANT_BOOST -> applyWallet(username, packet);
            case UNLOCK_PLANT, UNLOCK_ZOMBIE, UNLOCK_MINI_GAME,
                 UNLOCK_GREENHOUSE_POT -> applyUnlock(username, packet);
            case UPDATE_CHAPTER_PROGRESS, UPDATE_HIGHEST_MYOPOINT, INCREMENT_GAMES_PLAYED,
                 UPDATE_DIFFICULTY, PLANT_IN_GREENHOUSE, HARVEST_GREENHOUSE,
                 MARK_NEWS_READ, COMPLETE_QUEST, PURCHASE_DAILY_DEAL ->
                    applyProgress(username, packet);
            case SET_SETTINGS -> applySettings(username, packet);
            case GET_DAILY_OFFER -> null;
            case SET_QUEST_PROGRESS -> applyQuestProgress(username, packet);
            case PURCHASE_SHOP_ITEM -> applyShopPurchase(username, packet);
            case SET_PLANT_LEVEL -> applyPlantLevel(username, packet);
        };
    }

    private String applyWallet(String username, UserCommandRequestPacket packet) {
        return switch (packet.getCommand()) {
            case ADD_COINS -> {
                userRepository.addCoins(username, packet.argInt("amount", 0));
                yield null;
            }
            case SPEND_COINS -> userRepository.spendCoins(username, packet.argInt("amount", 0))
                    ? null : "INSUFFICIENT_FUNDS: not enough coins.";
            case ADD_GEMS -> {
                userRepository.addGems(username, packet.argInt("amount", 0));
                yield null;
            }
            case SPEND_GEMS -> userRepository.spendGems(username, packet.argInt("amount", 0))
                    ? null : "INSUFFICIENT_FUNDS: not enough gems.";
            case ADD_SEED_PACKETS -> {
                userRepository.addSeedPackets(
                        username, packet.arg("plantName"), packet.argInt("count", 0));
                yield null;
            }
            case SPEND_SEED_PACKETS -> userRepository.spendSeedPackets(
                    username, packet.arg("plantName"), packet.argInt("count", 0))
                    ? null : "INSUFFICIENT_FUNDS: not enough seed packets.";
            case ADD_PLANT_FOOD -> userRepository.addPlantFood(username)
                    ? null : "Plant food at capacity.";
            case USE_PLANT_FOOD -> userRepository.usePlantFood(username)
                    ? null : "No plant food available.";
            case STORE_PLANT_BOOST -> {
                userRepository.storePlantBoost(username, packet.arg("plantName"));
                yield null;
            }
            case CONSUME_PLANT_BOOST -> userRepository.consumePlantBoost(
                    username, packet.arg("plantName"))
                    ? null : "No plant boost available.";
            default -> "REJECTED";
        };
    }

    private String applyUnlock(String username, UserCommandRequestPacket packet) {
        return switch (packet.getCommand()) {
            case UNLOCK_PLANT -> {
                userRepository.unlockPlant(username, packet.arg("plantName"));
                yield null;
            }
            case UNLOCK_ZOMBIE -> {
                userRepository.unlockZombie(username, packet.arg("zombieName"));
                yield null;
            }
            case UNLOCK_MINI_GAME -> {
                userRepository.unlockMiniGame(username, packet.arg("miniGameId"));
                yield null;
            }
            case UNLOCK_GREENHOUSE_POT -> {
                userRepository.unlockGreenhousePot(
                        username, packet.argInt("x", 0), packet.argInt("y", 0));
                yield null;
            }
            default -> "REJECTED";
        };
    }

    private String applyProgress(String username, UserCommandRequestPacket packet) {
        return switch (packet.getCommand()) {
            case UPDATE_CHAPTER_PROGRESS -> {
                Chapter chapter = parseChapter(packet.arg("chapter"));
                if (chapter == null) {
                    yield "Invalid chapter.";
                }
                userRepository.updateChapterProgress(
                        username, chapter, packet.argInt("level", 0));
                yield null;
            }
            case UPDATE_HIGHEST_MYOPOINT -> {
                userRepository.updateHighestMyopoint(username, packet.argInt("myopoint", 0));
                yield null;
            }
            case INCREMENT_GAMES_PLAYED -> {
                userRepository.incrementGamesPlayed(username);
                yield null;
            }
            case UPDATE_DIFFICULTY -> {
                userRepository.updateDifficulty(username, packet.argInt("difficultyLevel", 3));
                yield null;
            }
            case PLANT_IN_GREENHOUSE -> applyGreenhousePlant(username, packet);
            case HARVEST_GREENHOUSE -> applyGreenhouseHarvest(username, packet);
            case MARK_NEWS_READ -> {
                userRepository.markNewsAsRead(username, packet.arg("newsId"));
                yield null;
            }
            case COMPLETE_QUEST -> {
                userRepository.completeQuest(
                        username, packet.arg("questId"), packet.argBool("isDaily", false));
                yield null;
            }
            case PURCHASE_DAILY_DEAL -> {
                userRepository.purchaseDailyDeal(username, packet.arg("dealId"));
                yield null;
            }
            default -> "REJECTED";
        };
    }

    private String applyGreenhousePlant(String username, UserCommandRequestPacket packet) {
        userRepository.plantInGreenhouse(
                username, packet.argInt("x", 0), packet.argInt("y", 0),
                packet.arg("plantName"),
                packet.argLong("timestamp", System.currentTimeMillis()));
        return null;
    }

    private String applyGreenhouseHarvest(String username, UserCommandRequestPacket packet) {
        userRepository.harvestGreenhousePlant(
                username, packet.argInt("x", 0), packet.argInt("y", 0));
        return null;
    }

    private String applyPlantLevel(String username, UserCommandRequestPacket packet) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) {
            return "User not found.";
        }
        User u = opt.get();
        String plantName = packet.arg("plantName");
        if (plantName == null || plantName.isBlank()) {
            return "Missing plantName.";
        }
        if (u.getPlantLevels() == null) {
            u.setPlantLevels(new HashMap<>());
        }
        u.getPlantLevels().put(plantName, packet.argInt("level", 1));
        userRepository.save(u);
        return null;
    }

    private String applySettings(String username, UserCommandRequestPacket packet) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) {
            return "User not found.";
        }
        User u = opt.get();
        applySettingsFields(u, packet);
        userRepository.save(u);
        return null;
    }

    private static void applySettingsFields(User u, UserCommandRequestPacket packet) {
        if (packet.arg("difficultyLevel") != null) {
            u.setDifficultyLevel(packet.argInt("difficultyLevel", u.getDifficultyLevel()));
        }
        if (packet.arg("gameSpeed") != null) {
            u.setGameSpeed(packet.argInt("gameSpeed", u.getGameSpeed()));
        }
        if (packet.arg("showLawnGrid") != null) {
            u.setShowLawnGrid(packet.argBool("showLawnGrid", u.isShowLawnGrid()));
        }
        if (packet.arg("debugMode") != null) {
            u.setDebugMode(packet.argBool("debugMode", u.isDebugMode()));
        }
        if (packet.arg("musicVolume") != null) {
            u.setMusicVolume(packet.argFloat("musicVolume", u.getMusicVolume()));
        }
        if (packet.arg("sfxVolume") != null) {
            u.setSfxVolume(packet.argFloat("sfxVolume", u.getSfxVolume()));
        }
    }

    private String applyQuestProgress(String username, UserCommandRequestPacket packet) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) {
            return "User not found.";
        }
        User u = opt.get();
        Map<String, Integer> progress = u.getQuestProgress();
        if (progress == null) {
            progress = new HashMap<>();
            u.setQuestProgress(progress);
        }
        String questId = packet.arg("questId");
        if (questId == null) {
            return "Missing questId.";
        }
        progress.put(questId, packet.argInt("value", 0));
        if (packet.arg("dailyQuestRefreshDate") != null) {
            u.setDailyQuestRefreshDate(packet.arg("dailyQuestRefreshDate"));
        }
        userRepository.save(u);
        return null;
    }

    private String applyShopPurchase(String username, UserCommandRequestPacket packet) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) {
            return "User not found.";
        }
        User u = opt.get();
        DailyOfferService.Snapshot offer = dailyOfferService.getToday();
        Shop shop = Shop.getInstance(u);
        shop.refreshDailyOffer(offer.plant(), offer.date());
        PurchaseResult result = shop.buy(
                packet.argInt("itemId", -1), packet.argInt("count", 1), packet.arg("plantType"));
        if (result != PurchaseResult.SUCCESS) {
            return "Shop purchase failed: " + result.name();
        }
        userRepository.save(u);
        return null;
    }

    private static Chapter parseChapter(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Chapter.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
