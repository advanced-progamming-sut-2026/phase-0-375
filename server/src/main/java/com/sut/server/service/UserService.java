package com.sut.server.service;

import com.sut.server.net.ClientConnectionHandler;
import com.sut.server.net.PacketRouter;
import model.network.enums.LeaderboardCategory;
import model.network.packet.user.LeaderboardRequestPacket;
import model.network.packet.user.LeaderboardResponsePacket;
import model.network.packet.user.PasswordChangeRequestPacket;
import model.network.packet.user.PasswordChangeResponsePacket;
import model.network.packet.user.PasswordResetRequestPacket;
import model.network.packet.user.PasswordResetResponsePacket;
import model.network.packet.user.ProfileGetRequestPacket;
import model.network.packet.user.ProfileGetResponsePacket;
import model.network.packet.user.ProfileUpdateRequestPacket;
import model.network.packet.user.ProfileUpdateResponsePacket;
import model.network.packet.user.UserCommandRequestPacket;
import model.network.packet.user.UserCommandResponsePacket;
import model.network.util.UserSanitizer;
import model.user.PasswordHasher;
import model.user.User;
import model.user.persistance.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Authoritative profile, password, economy, and progress mutations.
 */
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-]+$");

    private final UserRepository userRepository;
    private final AuthService authService;
    private final DailyOfferService dailyOfferService;
    private final UserCommandApplier commandApplier;

    public UserService(UserRepository userRepository) {
        this(userRepository, null, null);
    }

    public UserService(UserRepository userRepository, AuthService authService) {
        this(userRepository, authService, null);
    }

    public UserService(UserRepository userRepository, AuthService authService, DailyOfferService dailyOfferService) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.authService = authService;
        this.dailyOfferService = dailyOfferService != null ? dailyOfferService : new DailyOfferService();
        this.commandApplier = new UserCommandApplier(this.userRepository, this.dailyOfferService);
    }

    public void registerRoutes(PacketRouter router) {
        if (router == null) return;

        router.registerHandler(ProfileGetRequestPacket.class, (conn, packet) ->
                conn.sendPacket(handleProfileGet(conn)));

        router.registerHandler(ProfileUpdateRequestPacket.class, (conn, packet) ->
                conn.sendPacket(handleProfileUpdate(conn, packet)));

        router.registerHandler(PasswordChangeRequestPacket.class, (conn, packet) ->
                conn.sendPacket(handlePasswordChange(conn, packet)));

        router.registerHandler(PasswordResetRequestPacket.class, (conn, packet) ->
                conn.sendPacket(handlePasswordReset(packet)));

        router.registerHandler(UserCommandRequestPacket.class, (conn, packet) ->
                conn.sendPacket(handleUserCommand(conn, packet)));

        router.registerHandler(LeaderboardRequestPacket.class, (conn, packet) ->
                conn.sendPacket(handleLeaderboard(packet)));
    }

    public ProfileGetResponsePacket handleProfileGet(ClientConnectionHandler connection) {
        User user = requireUser(connection);
        if (user == null) {
            return new ProfileGetResponsePacket(false, "Not authenticated.", null);
        }
        return new ProfileGetResponsePacket(true, "OK", enrichDailyOffer(UserSanitizer.sanitize(user)));
    }

    public ProfileUpdateResponsePacket handleProfileUpdate(ClientConnectionHandler connection,
                                                           ProfileUpdateRequestPacket packet) {
        User user = requireUser(connection);
        if (user == null) {
            return new ProfileUpdateResponsePacket(false, "Not authenticated.", null);
        }
        if (packet == null) {
            return new ProfileUpdateResponsePacket(false, "Invalid request.", null);
        }
        ProfileUpdateResponsePacket usernameErr = applyUsernameChange(user, connection, packet);
        if (usernameErr != null) {
            return usernameErr;
        }
        ProfileUpdateResponsePacket nickErr = applyNicknameChange(user, packet);
        if (nickErr != null) {
            return nickErr;
        }
        ProfileUpdateResponsePacket emailErr = applyEmailChange(user, packet);
        if (emailErr != null) {
            return emailErr;
        }
        userRepository.save(user);
        userRepository.flush();
        return new ProfileUpdateResponsePacket(true, "Profile updated.", UserSanitizer.sanitize(user));
    }

    public PasswordChangeResponsePacket handlePasswordChange(ClientConnectionHandler connection,
                                                             PasswordChangeRequestPacket packet) {
        User user = requireUser(connection);
        if (user == null) {
            return new PasswordChangeResponsePacket(false, "Not authenticated.");
        }
        if (packet == null || packet.getOldPasswordHash() == null || packet.getNewPassword() == null) {
            return new PasswordChangeResponsePacket(false, "Old and new passwords are required.");
        }
        String stored = user.getPasswordHash();
        String oldHash = packet.getOldPasswordHash().trim();
        if (stored == null || !stored.equalsIgnoreCase(oldHash)) {
            return new PasswordChangeResponsePacket(false, "Old password is incorrect.");
        }
        String pwErr = validatePasswordComplexity(packet.getNewPassword());
        if (pwErr != null) {
            return new PasswordChangeResponsePacket(false, pwErr);
        }
        if (PasswordHasher.hash(packet.getNewPassword()).equalsIgnoreCase(stored)) {
            return new PasswordChangeResponsePacket(false, "New password must differ from the old one.");
        }
        userRepository.updatePassword(user.getUsername(), PasswordHasher.hash(packet.getNewPassword()));
        userRepository.flush();
        if (authService != null) {
            authService.revokeAllSessionTokens(user.getUsername());
        }
        return new PasswordChangeResponsePacket(true, "Password changed successfully.");
    }

    public PasswordResetResponsePacket handlePasswordReset(PasswordResetRequestPacket packet) {
        if (packet == null || packet.getUsername() == null || packet.getEmail() == null
                || packet.getSecurityAnswer() == null || packet.getNewPassword() == null) {
            return new PasswordResetResponsePacket(false, "All password-reset fields are required.");
        }
        String username = packet.getUsername().trim();
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) {
            return new PasswordResetResponsePacket(false, "Username not found.");
        }
        User user = opt.get();
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(packet.getEmail().trim())) {
            return new PasswordResetResponsePacket(false, "Email does not match this username.");
        }
        if (!userRepository.verifySecurityAnswer(username, packet.getSecurityAnswer())) {
            return new PasswordResetResponsePacket(false, "Incorrect security answer.");
        }
        String pwErr = validatePasswordComplexity(packet.getNewPassword());
        if (pwErr != null) {
            return new PasswordResetResponsePacket(false, pwErr);
        }
        userRepository.updatePassword(username, PasswordHasher.hash(packet.getNewPassword()));
        userRepository.flush();
        if (authService != null) {
            authService.revokeAllSessionTokens(username);
        }
        return new PasswordResetResponsePacket(true, "Password updated! Please log in.");
    }

    public UserCommandResponsePacket handleUserCommand(ClientConnectionHandler connection,
                                                       UserCommandRequestPacket packet) {
        String reqId = packet != null ? packet.getClientRequestId() : null;
        User user = requireUser(connection);
        if (user == null) {
            return UserCommandResponsePacket.fail(reqId, "UNAUTHORIZED", "Not authenticated.");
        }
        if (packet == null || packet.getCommand() == null) {
            return UserCommandResponsePacket.fail(reqId, "BAD_REQUEST", "Missing command.");
        }

        String username = user.getUsername();
        try {
            String err = commandApplier.apply(username, packet);
            if (err != null) {
                String code = err.contains("INSUFFICIENT") ? "INSUFFICIENT_FUNDS" : "REJECTED";
                return UserCommandResponsePacket.fail(reqId, code, err);
            }
            userRepository.flush();
            User fresh = userRepository.findByUsername(username).orElse(user);
            if (connection != null) {
                connection.setUserProfile(fresh);
            }
            return UserCommandResponsePacket.ok(reqId, "OK", enrichDailyOffer(UserSanitizer.sanitize(fresh)));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Command failed.";
            return UserCommandResponsePacket.fail(reqId, "ERROR", msg);
        }
    }

    public LeaderboardResponsePacket handleLeaderboard(LeaderboardRequestPacket packet) {
        LeaderboardCategory category = packet != null && packet.getCategory() != null
                ? packet.getCategory() : LeaderboardCategory.MYOPOINT;
        // Always ship full public rows so the client can sort any column.
        List<User> all = new ArrayList<>(userRepository.findAll());
        List<User> ranked = switch (category) {
            case MYOPOINT -> all.stream()
                    .sorted(Comparator.comparingInt(User::getHighestMyopoint).reversed()
                            .thenComparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            case CHAPTER -> userRepository.findAllOrderByChapterProgressDesc();
            case MINI_GAMES -> userRepository.findAllOrderByMiniGamesDesc();
            case QUESTS -> userRepository.findAllOrderByCompletedQuestsDesc();
        };
        List<LeaderboardResponsePacket.LeaderboardEntryDto> entries = new ArrayList<>();
        for (User u : ranked) {
            Map<String, Integer> chapter = new HashMap<>();
            if (u.getChapterProgress() != null) {
                u.getChapterProgress().forEach((ch, lvl) -> {
                    if (ch != null) chapter.put(ch.name(), lvl == null ? 0 : lvl);
                });
            }
            entries.add(new LeaderboardResponsePacket.LeaderboardEntryDto(
                    u.getUsername(),
                    u.getNickname(),
                    u.getHighestMyopoint(),
                    u.getCompletedMiniGames(),
                    u.getCompletedDailyQuests(),
                    u.getCompletedNonDailyQuests(),
                    chapter));
        }
        return new LeaderboardResponsePacket(true, "OK", category, entries);
    }

    private ProfileUpdateResponsePacket applyUsernameChange(
            User user, ClientConnectionHandler connection, ProfileUpdateRequestPacket packet) {
        String oldUsername = user.getUsername();
        String newUsername = packet.getUsername() != null ? packet.getUsername().trim() : null;
        if (newUsername == null || newUsername.isEmpty() || newUsername.equals(oldUsername)) {
            return null;
        }
        if (!USERNAME_PATTERN.matcher(newUsername).matches()) {
            return new ProfileUpdateResponsePacket(false,
                    "Invalid username. Only letters, numbers, and hyphens allowed.", null);
        }
        if (userRepository.existsByUsername(newUsername)) {
            return new ProfileUpdateResponsePacket(false,
                    "Username '" + newUsername + "' is already taken.", null);
        }
        userRepository.delete(user);
        user.setUsername(newUsername);
        if (connection != null) {
            connection.setUsername(newUsername);
        }
        if (authService != null) {
            authService.renameSession(oldUsername, newUsername, connection);
        }
        return null;
    }

    private ProfileUpdateResponsePacket applyNicknameChange(User user, ProfileUpdateRequestPacket packet) {
        String newNickname = packet.getNickname() != null ? packet.getNickname().trim() : null;
        if (newNickname == null || newNickname.isEmpty()) {
            return null;
        }
        if (newNickname.length() < 3 || newNickname.length() > 30) {
            return new ProfileUpdateResponsePacket(false,
                    "Nickname must be between 3 and 30 characters.", null);
        }
        user.setNickname(newNickname);
        return null;
    }

    private ProfileUpdateResponsePacket applyEmailChange(User user, ProfileUpdateRequestPacket packet) {
        String newEmail = packet.getEmail() != null ? packet.getEmail().trim() : null;
        String current = user.getEmail() != null ? user.getEmail() : "";
        if (newEmail == null || newEmail.isEmpty() || newEmail.equalsIgnoreCase(current)) {
            return null;
        }
        String emailErr = validateEmailFormat(newEmail);
        if (emailErr != null) {
            return new ProfileUpdateResponsePacket(false, emailErr, null);
        }
        if (userRepository.existsByEmail(newEmail)) {
            return new ProfileUpdateResponsePacket(false,
                    "Email '" + newEmail + "' is already in use.", null);
        }
        user.setEmail(newEmail);
        return null;
    }

    private User enrichDailyOffer(User user) {
        if (user == null) {
            return null;
        }
        DailyOfferService.Snapshot offer = dailyOfferService.getToday();
        user.setDailyOfferPlant(offer.plant());
        user.setDailyOfferDate(offer.date());
        return user;
    }

    private User requireUser(ClientConnectionHandler connection) {
        if (connection == null || connection.getUsername() == null) {
            return null;
        }
        if (connection.getUserProfile() != null) {
            return connection.getUserProfile();
        }
        return userRepository.findByUsername(connection.getUsername()).orElse(null);
    }

    private static String validatePasswordComplexity(String pw) {
        if (pw == null || pw.length() < 8) return "Weak password: minimum 8 characters.";
        if (!pw.matches(".*[a-z].*")) return "Weak password: must include a lowercase letter.";
        if (!pw.matches(".*[A-Z].*")) return "Weak password: must include an uppercase letter.";
        if (!pw.matches(".*\\d.*")) return "Weak password: must include a digit.";
        if (!pw.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*"))
            return "Weak password: must include a special character.";
        return null;
    }

    private static String validateEmailFormat(String email) {
        int at = email.indexOf('@');
        int lastAt = email.lastIndexOf('@');
        if (at == -1 || at != lastAt) return "Invalid email: must have exactly one '@'.";
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (local.isEmpty()) return "Invalid email: local part cannot be empty.";
        if (!local.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]*[a-zA-Z0-9]$") && local.length() > 1)
            return "Invalid email: invalid local part.";
        if (local.length() == 1 && !local.matches("[a-zA-Z0-9]")) return "Invalid email: invalid local part.";
        if (local.contains("..")) return "Invalid email: local part cannot have consecutive dots.";
        if (domain.isEmpty()) return "Invalid email: domain cannot be empty.";
        if (!domain.matches("^[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$")) return "Invalid email: invalid domain.";
        if (domain.contains("..")) return "Invalid email: domain cannot have consecutive dots.";
        if (email.matches(".*[?><, \"';:\\\\/|\\[\\]{}()+*&^%$#!].*"))
            return "Invalid email: contains forbidden characters.";
        return null;
    }
}
