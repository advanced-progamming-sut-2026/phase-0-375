package com.sut.server.service;

import com.sut.server.net.ClientConnectionHandler;
import com.sut.server.net.PacketRouter;
import com.sut.server.repository.SessionTokenStore;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
import model.network.packet.auth.LogoutRequestPacket;
import model.network.packet.auth.RegisterRequestPacket;
import model.network.packet.auth.RegisterResponsePacket;
import model.network.packet.auth.RegisterValidateRequestPacket;
import model.network.packet.auth.SessionResumeRequestPacket;
import model.network.packet.system.ErrorMessagePacket;
import model.network.util.UserSanitizer;
import model.user.PasswordHasher;
import model.user.User;
import model.user.persistance.UserRepository;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Authoritative Server Authentication & Session Management Service.
 * Enforces registration validation invariants, SHA-256 credential verification,
 * active session tracking, and duplicate login policies.
 */
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-]+$");
    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    private final UserRepository userRepository;
    private final SessionTokenStore sessionTokenStore;
    private final Map<String, ClientConnectionHandler> activeSessions = new ConcurrentHashMap<>();
    private final boolean kickOnDuplicateLogin;

    public AuthService(UserRepository userRepository) {
        this(userRepository, true, sessionStoreBesideUsers(userRepository));
    }

    public AuthService(UserRepository userRepository, boolean kickOnDuplicateLogin) {
        this(userRepository, kickOnDuplicateLogin, sessionStoreBesideUsers(userRepository));
    }

    public AuthService(UserRepository userRepository, SessionTokenStore sessionTokenStore) {
        this(userRepository, true, sessionTokenStore);
    }

    public AuthService(
            UserRepository userRepository,
            boolean kickOnDuplicateLogin,
            SessionTokenStore sessionTokenStore) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.kickOnDuplicateLogin = kickOnDuplicateLogin;
        this.sessionTokenStore = Objects.requireNonNull(
                sessionTokenStore, "SessionTokenStore cannot be null");
    }

    private static SessionTokenStore sessionStoreBesideUsers(UserRepository userRepository) {
        if (userRepository instanceof com.sut.server.repository.ServerUserRepository sur) {
            Path parent = sur.getStoragePath().getParent();
            Path sessions = parent != null ? parent.resolve("sessions.json") : Path.of("sessions.json");
            return new SessionTokenStore(sessions);
        }
        return new SessionTokenStore();
    }

    /**
     * Registers all packet routes and connection lifecycle listeners with PacketRouter.
     */
    public void registerRoutes(PacketRouter router) {
        if (router == null) return;

        router.registerHandler(RegisterRequestPacket.class, (conn, packet) -> {
            RegisterResponsePacket response = register(packet);
            conn.sendPacket(response);
        });

        router.registerHandler(RegisterValidateRequestPacket.class, (conn, packet) -> {
            RegisterResponsePacket response = validateRegistrationStep1(packet);
            conn.sendPacket(response);
        });

        router.registerHandler(LoginRequestPacket.class, (conn, packet) -> {
            LoginResponsePacket response = login(packet, conn);
            conn.sendPacket(response);
        });

        router.registerHandler(SessionResumeRequestPacket.class, (conn, packet) -> {
            LoginResponsePacket response = resumeSession(packet, conn);
            conn.sendPacket(response);
        });

        router.registerHandler(LogoutRequestPacket.class, (conn, packet) -> {
            logout(packet, conn);
        });

        router.addConnectionClosedListener(this::handleDisconnect);
    }

    /**
     * Validates registration profile fields (step 1). Does not create an account.
     */
    public RegisterResponsePacket validateRegistrationStep1(RegisterValidateRequestPacket packet) {
        if (packet == null) {
            return new RegisterResponsePacket(false, "Registration request cannot be null.");
        }
        RegisterResponsePacket fields = validateRegistrationFields(
                packet.getUsername(),
                packet.getPassword(),
                packet.getNickname(),
                packet.getEmail(),
                packet.getGender());
        if (!fields.isSuccess()) {
            return fields;
        }
        return new RegisterResponsePacket(true, "All fields validated. Now choose a security question.");
    }

    /**
     * Handles user registration with field validation, duplicate checks, and profile creation.
     */
    public synchronized RegisterResponsePacket register(RegisterRequestPacket packet) {
        if (packet == null) {
            return new RegisterResponsePacket(false, "Registration request cannot be null.");
        }

        RegisterResponsePacket fields = validateRegistrationFields(
                packet.getUsername(),
                packet.getPasswordHash(),
                packet.getNickname(),
                packet.getEmail(),
                packet.getGender());
        if (!fields.isSuccess()) {
            return fields;
        }

        String username = packet.getUsername().trim();
        String rawOrHash = packet.getPasswordHash().trim();
        String passwordHash = SHA256_HEX_PATTERN.matcher(rawOrHash).matches()
                ? rawOrHash.toLowerCase()
                : PasswordHasher.hash(rawOrHash);
        int qNum = packet.getSecurityQuestionNumber();
        if (qNum < 1 || qNum > 5) {
            return new RegisterResponsePacket(false, "Question number must be 1-5.");
        }
        String securityAnswer = packet.getSecurityAnswer() != null
                ? packet.getSecurityAnswer().trim() : "";
        if (securityAnswer.isEmpty()) {
            return new RegisterResponsePacket(false, "Answer cannot be empty.");
        }

        User user = AuthUserFactory.create(
                username,
                passwordHash,
                packet.getNickname().trim(),
                packet.getEmail().trim(),
                packet.getGender().trim().toLowerCase(),
                qNum,
                securityAnswer);
        userRepository.save(user);
        return new RegisterResponsePacket(true, "Registration successful.");
    }

    /**
     * Handles user login with credential verification, duplicate session eviction, and context attachment.
     */
    public LoginResponsePacket login(LoginRequestPacket packet, ClientConnectionHandler connection) {
        if (packet == null) {
            return new LoginResponsePacket(false, "Login request cannot be null.", null);
        }

        String username = packet.getUsername() != null ? packet.getUsername().trim() : "";
        String passwordInput = packet.getPasswordHash() != null ? packet.getPasswordHash().trim() : "";

        if (username.isEmpty() || passwordInput.isEmpty()) {
            return new LoginResponsePacket(false, "Username and password cannot be empty.", null);
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new LoginResponsePacket(false, "Invalid username or password.", null);
        }

        User user = userOpt.get();
        if (!passwordMatches(user, passwordInput)) {
            return new LoginResponsePacket(false, "Invalid username or password.", null);
        }

        String normUser = username.toLowerCase();
        LoginResponsePacket duplicate = evictOrRejectDuplicate(normUser, username, connection);
        if (duplicate != null) {
            return duplicate;
        }

        String sessionToken = null;
        if (packet.isStayLoggedIn()) {
            user.setStayLoggedIn(true);
            userRepository.flush();
            sessionToken = sessionTokenStore.mint(user.getUsername());
        }

        attachSession(user, normUser, connection);
        return new LoginResponsePacket(true, "Login successful.", UserSanitizer.sanitize(user), sessionToken);
    }

    /**
     * Resumes a TCP session from a previously issued stay-logged-in token.
     */
    public LoginResponsePacket resumeSession(SessionResumeRequestPacket packet, ClientConnectionHandler connection) {
        if (packet == null || packet.getToken() == null || packet.getToken().isBlank()) {
            return new LoginResponsePacket(false, "Session token is required.", null);
        }

        Optional<String> usernameOpt = sessionTokenStore.findUsername(packet.getToken());
        if (usernameOpt.isEmpty()) {
            return new LoginResponsePacket(false, "Invalid or expired session.", null);
        }

        Optional<User> userOpt = userRepository.findByUsername(usernameOpt.get());
        if (userOpt.isEmpty()) {
            sessionTokenStore.revokeToken(packet.getToken());
            return new LoginResponsePacket(false, "Invalid or expired session.", null);
        }

        User user = userOpt.get();
        String normUser = user.getUsername().toLowerCase();

        LoginResponsePacket duplicate = evictOrRejectDuplicate(normUser, user.getUsername(), connection);
        if (duplicate != null) {
            return duplicate;
        }

        user.setStayLoggedIn(true);
        attachSession(user, normUser, connection);

        // Reuse the same token until logout/expiry (no rotation in first cut).
        return new LoginResponsePacket(
                true, "Session resumed.", UserSanitizer.sanitize(user), packet.getToken().trim());
    }

    /**
     * Handles user logout, unregisters active session, and revokes stay-logged-in token.
     */
    public void logout(LogoutRequestPacket packet, ClientConnectionHandler connection) {
        String username = (packet != null && packet.getUsername() != null && !packet.getUsername().isBlank())
                ? packet.getUsername().trim()
                : (connection != null ? connection.getUsername() : null);

        if (packet != null && packet.getSessionToken() != null && !packet.getSessionToken().isBlank()) {
            sessionTokenStore.revokeToken(packet.getSessionToken());
        } else if (username != null) {
            sessionTokenStore.revokeAllForUser(username);
        }

        if (username != null) {
            String normUser = username.toLowerCase();
            if (connection != null) {
                activeSessions.remove(normUser, connection);
            } else {
                activeSessions.remove(normUser);
            }
            userRepository.clearStayLoggedIn(username);
        }

        if (connection != null) {
            connection.setUserProfile(null);
            connection.setUsername(null);
        }
    }

    /** Revoke all stay-logged-in tokens for a user (e.g. after password change). */
    public void revokeAllSessionTokens(String username) {
        sessionTokenStore.revokeAllForUser(username);
    }

    public SessionTokenStore getSessionTokenStore() {
        return sessionTokenStore;
    }

    /**
     * Hook called on client disconnect to clean up active session registry.
     */
    public void handleDisconnect(ClientConnectionHandler connection) {
        if (connection == null) return;
        String username = connection.getUsername();
        if (username != null) {
            activeSessions.remove(username.toLowerCase(), connection);
        }
    }

    public void renameSession(String oldUsername, String newUsername, ClientConnectionHandler connection) {
        if (oldUsername == null || newUsername == null) return;
        String oldKey = oldUsername.toLowerCase();
        String newKey = newUsername.toLowerCase();
        ClientConnectionHandler existing = activeSessions.remove(oldKey);
        if (existing != null) {
            activeSessions.put(newKey, existing);
        } else if (connection != null) {
            activeSessions.put(newKey, connection);
        }
    }

    public Optional<ClientConnectionHandler> getSession(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(activeSessions.get(username.toLowerCase()));
    }

    public boolean isUserLoggedIn(String username) {
        if (username == null) return false;
        ClientConnectionHandler handler = activeSessions.get(username.toLowerCase());
        return handler != null && !handler.isClosed();
    }

    public Collection<String> getOnlineUsernames() {
        return Collections.unmodifiableSet(activeSessions.keySet());
    }

    public int getOnlineCount() {
        return activeSessions.size();
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    private boolean passwordMatches(User user, String passwordInput) {
        String stored = user.getPasswordHash();
        if (stored == null) {
            return false;
        }
        return stored.equalsIgnoreCase(passwordInput)
                || stored.equalsIgnoreCase(PasswordHasher.hash(passwordInput));
    }

    private LoginResponsePacket evictOrRejectDuplicate(
            String normUser, String username, ClientConnectionHandler connection) {
        ClientConnectionHandler existingConn = activeSessions.get(normUser);
        if (existingConn == null || existingConn == connection || existingConn.isClosed()) {
            return null;
        }
        if (kickOnDuplicateLogin) {
            existingConn.sendPacket(new ErrorMessagePacket("SESSION_REPLACED",
                    "You have been logged out because this account logged in from another connection."));
            existingConn.disconnect("Replaced by new login");
            return null;
        }
        return new LoginResponsePacket(false,
                "User '" + username + "' is already logged in on another session.", null);
    }

    private void attachSession(User user, String normUser, ClientConnectionHandler connection) {
        if (connection != null) {
            connection.setUserProfile(user);
            connection.setUsername(user.getUsername());
            activeSessions.put(normUser, connection);
        }
    }

    private RegisterResponsePacket validateRegistrationFields(String usernameRaw, String passwordRawOrHash,
                                                              String nicknameRaw, String emailRaw, String genderRaw) {
        RegisterResponsePacket usernameErr = validateUsername(usernameRaw);
        if (usernameErr != null) {
            return usernameErr;
        }
        RegisterResponsePacket passwordErr = validatePassword(passwordRawOrHash);
        if (passwordErr != null) {
            return passwordErr;
        }
        RegisterResponsePacket profileErr = validateNicknameEmailGender(nicknameRaw, emailRaw, genderRaw);
        if (profileErr != null) {
            return profileErr;
        }
        return new RegisterResponsePacket(true, "OK");
    }

    private RegisterResponsePacket validateUsername(String usernameRaw) {
        String username = usernameRaw != null ? usernameRaw.trim() : "";
        if (username.isEmpty()) {
            return new RegisterResponsePacket(false, "Username cannot be empty.");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return new RegisterResponsePacket(false,
                    "Invalid username. Only letters, numbers, and hyphens allowed.");
        }
        if (userRepository.existsByUsername(username)) {
            return new RegisterResponsePacket(false, "Username '" + username + "' is already taken.");
        }
        return null;
    }

    private RegisterResponsePacket validatePassword(String passwordRawOrHash) {
        String rawOrHash = passwordRawOrHash != null ? passwordRawOrHash.trim() : "";
        if (rawOrHash.isEmpty()) {
            return new RegisterResponsePacket(false, "Password cannot be empty.");
        }
        if (!SHA256_HEX_PATTERN.matcher(rawOrHash).matches()) {
            String pwError = validatePasswordComplexity(rawOrHash);
            if (pwError != null) {
                return new RegisterResponsePacket(false, pwError);
            }
        }
        return null;
    }

    private RegisterResponsePacket validateNicknameEmailGender(
            String nicknameRaw, String emailRaw, String genderRaw) {
        String nickname = nicknameRaw != null ? nicknameRaw.trim() : "";
        if (nickname.isEmpty()) {
            return new RegisterResponsePacket(false, "Nickname cannot be empty.");
        }
        if (nickname.length() < 3 || nickname.length() > 30) {
            return new RegisterResponsePacket(false, "Nickname must be between 3 and 30 characters.");
        }
        String email = emailRaw != null ? emailRaw.trim() : "";
        if (email.isEmpty()) {
            return new RegisterResponsePacket(false, "Email cannot be empty.");
        }
        String emailError = validateEmailFormat(email);
        if (emailError != null) {
            return new RegisterResponsePacket(false, emailError);
        }
        if (userRepository.existsByEmail(email)) {
            return new RegisterResponsePacket(false, "Email '" + email + "' is already in use.");
        }
        String gender = genderRaw != null ? genderRaw.trim().toLowerCase() : "";
        if (!gender.equals("male") && !gender.equals("female")) {
            return new RegisterResponsePacket(false, "Gender must be 'male' or 'female'.");
        }
        return null;
    }

    private static String validatePasswordComplexity(String pw) {
        if (pw.length() < 8) return "Weak password: minimum 8 characters.";
        if (!pw.matches(".*[a-z].*")) return "Weak password: must include a lowercase letter.";
        if (!pw.matches(".*[A-Z].*")) return "Weak password: must include an uppercase letter.";
        if (!pw.matches(".*\\d.*")) return "Weak password: must include a digit.";
        if (!pw.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return "Weak password: must include a special character.";
        }
        return null;
    }

    private static String validateEmailFormat(String email) {
        int at = email.indexOf('@');
        int lastAt = email.lastIndexOf('@');
        if (at == -1 || at != lastAt) return "Invalid email: must have exactly one '@'.";
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (local.isEmpty()) return "Invalid email: local part cannot be empty.";
        if (!local.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]*[a-zA-Z0-9]$") && local.length() > 1) {
            return "Invalid email: invalid local part.";
        }
        if (local.length() == 1 && !local.matches("[a-zA-Z0-9]")) {
            return "Invalid email: invalid local part.";
        }
        if (local.contains("..")) {
            return "Invalid email: local part cannot have consecutive dots.";
        }
        if (domain.isEmpty()) {
            return "Invalid email: domain cannot be empty.";
        }
        if (!domain.matches("^[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$")) {
            return "Invalid email: invalid domain.";
        }
        if (domain.contains("..")) {
            return "Invalid email: domain cannot have consecutive dots.";
        }
        if (email.matches(".*[?><, \"';:\\\\/|\\[\\]{}()+*&^%$#!].*")) {
            return "Invalid email: contains forbidden characters.";
        }
        return null;
    }
}
