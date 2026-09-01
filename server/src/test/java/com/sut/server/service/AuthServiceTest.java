package com.sut.server.service;

import com.sut.server.net.ClientConnectionHandler;
import com.sut.server.net.PacketRouter;
import com.sut.server.net.TcpServer;
import com.sut.server.repository.ServerUserRepository;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
import model.network.packet.auth.LogoutRequestPacket;
import model.network.packet.auth.RegisterRequestPacket;
import model.network.packet.auth.RegisterResponsePacket;
import model.network.packet.auth.RegisterValidateRequestPacket;
import model.user.PasswordHasher;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @TempDir
    Path tempDir;

    private ServerUserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        Path storagePath = tempDir.resolve("auth-test-users.json");
        userRepository = new ServerUserRepository(storagePath);
        authService = new AuthService(userRepository,
                new com.sut.server.repository.SessionTokenStore(tempDir.resolve("sessions.json")));
    }

    private RegisterRequestPacket createValidRegisterPacket(String username, String email) {
        RegisterRequestPacket packet = new RegisterRequestPacket();
        packet.setUsername(username);
        packet.setPasswordHash("StrongP@ss1"); // Raw valid password
        packet.setNickname("Nick_" + username);
        packet.setEmail(email);
        packet.setGender("male");
        packet.setSecurityQuestionNumber(1);
        packet.setSecurityAnswer("FirstSchool");
        return packet;
    }

    @Test
    @DisplayName("Step 1 validation succeeds without creating an account")
    void testValidateRegistrationStep1Success() {
        RegisterValidateRequestPacket packet = new RegisterValidateRequestPacket(
                "step1User", "StrongP@ss1", "StepOne", "step1@pvz.com", "female");
        RegisterResponsePacket response = authService.validateRegistrationStep1(packet);

        assertTrue(response.isSuccess());
        assertFalse(userRepository.existsByUsername("step1User"));
    }

    @Test
    @DisplayName("Step 1 validation rejects weak password without creating an account")
    void testValidateRegistrationStep1RejectsWeakPassword() {
        RegisterValidateRequestPacket packet = new RegisterValidateRequestPacket(
                "step1Weak", "weak", "StepWeak", "weak@pvz.com", "male");
        RegisterResponsePacket response = authService.validateRegistrationStep1(packet);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().startsWith("Weak password:"));
        assertFalse(userRepository.existsByUsername("step1Weak"));
    }

    @Test
    @DisplayName("Should successfully register user with valid inputs and initialize starter assets")
    void testRegisterSuccess() {
        RegisterRequestPacket packet = createValidRegisterPacket("validUser", "valid@pvz.com");
        RegisterResponsePacket response = authService.register(packet);

        assertTrue(response.isSuccess());
        assertEquals("Registration successful.", response.getMessage());

        Optional<User> userOpt = userRepository.findByUsername("validUser");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();

        assertEquals("validUser", user.getUsername());
        assertEquals("valid@pvz.com", user.getEmail());
        assertEquals("Nick_validUser", user.getNickname());
        assertEquals("male", user.getGender());
        assertEquals(1, user.getSecurityQuestionNumber());
        assertEquals("FirstSchool", user.getSecurityAnswer());
        assertEquals(PasswordHasher.hash("StrongP@ss1"), user.getPasswordHash());

        // Verify starter assets and defaults
        assertEquals(4, user.getUnlockedPots());
        assertEquals(0, user.getCoins());
        assertEquals(0, user.getGems());
        assertNotNull(user.getUnlockedPlants());
        assertTrue(user.getUnlockedPlants().containsAll(User.STARTER_PLANTS));
        assertNotNull(user.getNewsPublishDates());
    }

    @Test
    @DisplayName("Should accept pre-hashed 64-character SHA-256 hex password string")
    void testRegisterPreHashedPassword() {
        String preHash = PasswordHasher.hash("MySuperPassword123!");
        RegisterRequestPacket packet = createValidRegisterPacket("hashUser", "hash@pvz.com");
        packet.setPasswordHash(preHash);

        RegisterResponsePacket response = authService.register(packet);
        assertTrue(response.isSuccess());

        User user = userRepository.findByUsername("hashUser").orElseThrow();
        assertEquals(preHash, user.getPasswordHash());
    }

    @Test
    @DisplayName("Should reject duplicate username and duplicate email (case-insensitive)")
    void testDuplicateUsernameAndEmail() {
        RegisterRequestPacket packet1 = createValidRegisterPacket("duplicateUser", "dup@pvz.com");
        assertTrue(authService.register(packet1).isSuccess());

        // Duplicate username
        RegisterRequestPacket packet2 = createValidRegisterPacket("duplicateUser", "other@pvz.com");
        RegisterResponsePacket resp2 = authService.register(packet2);
        assertFalse(resp2.isSuccess());
        assertTrue(resp2.getMessage().contains("already taken"));

        // Duplicate email with different case
        RegisterRequestPacket packet3 = createValidRegisterPacket("otherUser", "DUP@PVZ.COM");
        RegisterResponsePacket resp3 = authService.register(packet3);
        assertFalse(resp3.isSuccess());
        assertTrue(resp3.getMessage().contains("already in use"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "user name", "user@name", "user#1"})
    @DisplayName("Should reject invalid username formats")
    void testInvalidUsernames(String invalidUsername) {
        RegisterRequestPacket packet = createValidRegisterPacket(invalidUsername, "test@pvz.com");
        RegisterResponsePacket response = authService.register(packet);
        assertFalse(response.isSuccess());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short1!",        // < 8 chars
            "NOLOWERCASE1!",  // No lowercase
            "nouppercase1!",  // No uppercase
            "NoDigitsHere!",  // No digit
            "NoSpecial1234"   // No special char
    })
    @DisplayName("Should reject weak passwords with descriptive error messages")
    void testWeakPasswords(String weakPassword) {
        RegisterRequestPacket packet = createValidRegisterPacket("weakUser", "weak@pvz.com");
        packet.setPasswordHash(weakPassword);
        RegisterResponsePacket response = authService.register(packet);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().startsWith("Weak password:"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plainaddress",
            "user@sub@domain.com",
            ".user@domain.com",
            "user.@domain.com",
            "user..name@domain.com",
            "user@domain..com",
            "user@domain.c",
            "user+tag@domain.com"
    })
    @DisplayName("Should reject invalid email formats")
    void testInvalidEmails(String invalidEmail) {
        RegisterRequestPacket packet = createValidRegisterPacket("emailUser", invalidEmail);
        RegisterResponsePacket response = authService.register(packet);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().startsWith("Invalid email:"));
    }

    @Test
    @DisplayName("Should reject invalid gender, security question, and nickname boundaries")
    void testValidationEdgeCases() {
        // Invalid nickname
        RegisterRequestPacket pNick = createValidRegisterPacket("userNick", "nick@pvz.com");
        pNick.setNickname("ab"); // < 3
        assertFalse(authService.register(pNick).isSuccess());

        // Invalid gender
        RegisterRequestPacket pGender = createValidRegisterPacket("userGender", "gender@pvz.com");
        pGender.setGender("other");
        assertFalse(authService.register(pGender).isSuccess());

        // Invalid security question
        RegisterRequestPacket pQ = createValidRegisterPacket("userQ", "q@pvz.com");
        pQ.setSecurityQuestionNumber(0);
        assertFalse(authService.register(pQ).isSuccess());

        pQ.setSecurityQuestionNumber(6);
        assertFalse(authService.register(pQ).isSuccess());

        // Empty answer
        RegisterRequestPacket pAns = createValidRegisterPacket("userAns", "ans@pvz.com");
        pAns.setSecurityAnswer("   ");
        assertFalse(authService.register(pAns).isSuccess());
    }

    @Test
    @DisplayName("Should authenticate valid login requests and reject invalid credentials")
    void testLoginScenarios() {
        authService.register(createValidRegisterPacket("loginUser", "login@pvz.com"));

        // Valid login with raw password
        LoginRequestPacket validRaw = new LoginRequestPacket("loginUser", "StrongP@ss1", false);
        LoginResponsePacket respRaw = authService.login(validRaw, null);
        assertTrue(respRaw.isSuccess());
        assertNotNull(respRaw.getUserProfile());
        assertEquals("loginUser", respRaw.getUserProfile().getUsername());

        // Valid login with pre-hashed password + stay logged in mints a session token
        String hashed = PasswordHasher.hash("StrongP@ss1");
        LoginRequestPacket validHash = new LoginRequestPacket("loginUser", hashed, true);
        LoginResponsePacket respHash = authService.login(validHash, null);
        assertTrue(respHash.isSuccess());
        assertTrue(userRepository.findByUsername("loginUser").get().isStayLoggedIn());
        assertNotNull(respHash.getSessionToken());
        assertFalse(respHash.getSessionToken().isBlank());
        assertNull(authService.login(new LoginRequestPacket("loginUser", hashed, false), null).getSessionToken());

        // Invalid password
        LoginRequestPacket badPass = new LoginRequestPacket("loginUser", "WrongPassword1!", false);
        LoginResponsePacket respBadPass = authService.login(badPass, null);
        assertFalse(respBadPass.isSuccess());
        assertNull(respBadPass.getUserProfile());

        // Non-existent username
        LoginRequestPacket nonExistent = new LoginRequestPacket("ghostUser", "StrongP@ss1", false);
        LoginResponsePacket respGhost = authService.login(nonExistent, null);
        assertFalse(respGhost.isSuccess());
    }

    @Test
    @DisplayName("Stay-logged-in token can resume; logout and bad token fail")
    void testSessionTokenResumeAndRevoke() {
        authService.register(createValidRegisterPacket("tokenUser", "token@pvz.com"));

        LoginResponsePacket login = authService.login(
                new LoginRequestPacket("tokenUser", "StrongP@ss1", true), null);
        assertTrue(login.isSuccess());
        String token = login.getSessionToken();
        assertNotNull(token);

        LoginResponsePacket resumed = authService.resumeSession(
                new model.network.packet.auth.SessionResumeRequestPacket(token), null);
        assertTrue(resumed.isSuccess());
        assertEquals("tokenUser", resumed.getUserProfile().getUsername());
        assertEquals(token, resumed.getSessionToken());

        assertFalse(authService.resumeSession(
                new model.network.packet.auth.SessionResumeRequestPacket("deadbeef"), null).isSuccess());

        authService.logout(new LogoutRequestPacket("tokenUser", token), null);
        assertFalse(authService.resumeSession(
                new model.network.packet.auth.SessionResumeRequestPacket(token), null).isSuccess());
    }

    @Test
    @DisplayName("Should clear session and stayLoggedIn on logout")
    void testLogout() {
        authService.register(createValidRegisterPacket("logoutUser", "logout@pvz.com"));

        LoginRequestPacket loginReq = new LoginRequestPacket("logoutUser", "StrongP@ss1", true);
        LoginResponsePacket loginResp = authService.login(loginReq, null);
        assertTrue(userRepository.findByUsername("logoutUser").get().isStayLoggedIn());

        LogoutRequestPacket logoutReq = new LogoutRequestPacket("logoutUser", loginResp.getSessionToken());
        authService.logout(logoutReq, null);

        assertFalse(userRepository.findByUsername("logoutUser").get().isStayLoggedIn());
    }

    @Test
    @DisplayName("Should register packet handlers with PacketRouter")
    void testRegisterRoutes() {
        PacketRouter router = new PacketRouter();
        authService.registerRoutes(router);
        // Verify routes can be registered without error
        assertNotNull(router);
    }
}
