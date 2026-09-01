package com.sut.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sut.server.repository.ServerUserRepository;
import com.sut.server.service.AuthService;
import com.sut.server.service.UserService;
import controller.LoginMenuController;
import controller.MainMenuController;
import controller.RegisterMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.User;
import model.user.persistance.NullUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedClientAuthIntegrationTest {

    @TempDir
    Path tempDir;

    private TcpServer tcpServer;
    private ServerUserRepository serverRepo;
    private ServerUserRepository clientLocalRepo;
    private AuthService authService;
    private PacketRouter router;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        Path serverStoragePath = tempDir.resolve("server-users.json");
        serverRepo = new ServerUserRepository(serverStoragePath);
        authService = new AuthService(serverRepo);
        UserService userService = new UserService(serverRepo, authService);

        router = new PacketRouter();
        authService.registerRoutes(router);
        userService.registerRoutes(router);

        objectMapper = TcpServer.createDefaultObjectMapper();
        tcpServer = new TcpServer("127.0.0.1", 0, router, objectMapper);
        tcpServer.start();
        int serverPort = tcpServer.getBoundPort();

        // Isolate client local repository for tests
        Path clientStoragePath = tempDir.resolve("client-users.json");
        clientLocalRepo = new ServerUserRepository(clientStoragePath);

        System.setProperty(model.user.persistance.LocalSessionStore.PATH_PROPERTY,
                tempDir.resolve("session.json").toAbsolutePath().toString());

        App app = App.getInstance();
        app.disconnectNetwork();
        app.clearStayLoggedInToken();
        app.setUserRepository(clientLocalRepo);
        app.setServerEndpoint("127.0.0.1", serverPort);
        app.setCurrentUser(null);
        app.setCurrentMenu(MenuType.REGISTER);
    }

    @AfterEach
    void tearDown() {
        App app = App.getInstance();
        app.disconnectNetwork();
        app.clearStayLoggedInToken();
        app.setCurrentUser(null);
        app.setUserRepository(new model.user.persistance.NullUserRepository());
        System.clearProperty(model.user.persistance.LocalSessionStore.PATH_PROPERTY);

        if (tcpServer != null) {
            tcpServer.stop();
        }
    }

    @Test
    @DisplayName("R1 Unified Auth: Register -> Login -> Sync Profile -> Logout via Controllers")
    void testUnifiedAuthControllersFlow() {
        assertTimeoutPreemptively(Duration.ofSeconds(12), () -> {
            String uname = "u-" + UUID.randomUUID().toString().substring(0, 8);
            RegisterMenuController regController = RegisterMenuController.getInstance();
            LoginMenuController loginController = LoginMenuController.getInstance();
            MainMenuController mainController = MainMenuController.getInstance();

            // 1. Step 1: Register fields
            CommandResult<Void> r1 = regController.register(
                    uname,
                    "Password123!",
                    "Password123!",
                    "UnifiedPlayer",
                    uname + "@pvz.com",
                    "female"
            );
            assertTrue(r1.isSuccess(), "Step 1 registration failed: " + r1.getMessage());

            // 2. Step 2: Pick question and authenticate against server
            CommandResult<Void> r2 = regController.pickQuestion(1, "Sunflower", "Sunflower");
            assertTrue(r2.isSuccess(), "Step 2 registration failed: " + r2.getMessage());
            assertEquals(MenuType.LOGIN, App.getInstance().getCurrentMenu());

            // Verify server has user persisted; client must not own the account store
            assertTrue(serverRepo.existsByUsername(uname));
            assertFalse(clientLocalRepo.existsByUsername(uname), "Client must not persist newly registered users");
            User serverUser = serverRepo.findByUsername(uname).orElseThrow();
            assertEquals("UnifiedPlayer", serverUser.getNickname());
            assertEquals(uname + "@pvz.com", serverUser.getEmail());

            // Modify profile data on server to test authoritative sync (e.g. awards 500 coins and 20 gems)
            serverUser.setCoins(500);
            serverUser.setGems(20);
            serverUser.setGamesPlayed(3);
            serverRepo.save(serverUser);

            // 3. Login with registered credentials
            CommandResult<Void> loginRes = loginController.login(uname, "Password123!", true);
            assertTrue(loginRes.isSuccess(), "Login failed: " + loginRes.getMessage());
            assertEquals(MenuType.MAIN, App.getInstance().getCurrentMenu());

            // 4. Verify authoritative profile sync in App.currentUser
            User currentUser = App.getInstance().getCurrentUser();
            assertNotNull(currentUser, "Current user must not be null after login");
            assertEquals(uname, currentUser.getUsername());
            assertEquals("UnifiedPlayer", currentUser.getNickname());
            assertEquals(500, currentUser.getCoins(), "Coins must match server-authoritative value");
            assertEquals(20, currentUser.getGems(), "Gems must match server-authoritative value");
            assertEquals(3, currentUser.getGamesPlayed(), "Games played must match server-authoritative value");

            // Spend coins via remote command and verify persistence across "devices"
            assertTrue(App.getInstance().getUserRepository() instanceof model.user.persistance.RemoteUserRepository);
            App.getInstance().getUserRepository().spendCoins(uname, 50);
            assertEquals(450, App.getInstance().getCurrentUser().getCoins());
            assertEquals(450, serverRepo.findByUsername(uname).orElseThrow().getCoins());
            assertFalse(clientLocalRepo.existsByUsername(uname), "Client must still not persist account");

            // 5. Verify NetworkClient is active and connected in App
            assertTrue(App.getInstance().isConnected(), "App must hold an active connected NetworkClient");
            assertTrue(authService.isUserLoggedIn(uname));

            // 6. Logout and re-login (simulates another device)
            CommandResult<Void> logoutRes = mainController.logout();
            assertTrue(logoutRes.isSuccess());
            assertNull(App.getInstance().getCurrentUser());
            assertFalse(App.getInstance().isConnected());

            CommandResult<Void> login2 = loginController.login(uname, "Password123!", false);
            assertTrue(login2.isSuccess(), login2.getMessage());
            assertEquals(450, App.getInstance().getCurrentUser().getCoins(), "Re-login must load server coins");
            assertEquals(MenuType.MAIN, App.getInstance().getCurrentMenu());
        });
    }

    @Test
    @DisplayName("R1 Unified Auth: Offline register/login must fail (no client-side user store)")
    void testOfflineAuthRejected() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            String uname = "off-" + UUID.randomUUID().toString().substring(0, 8);

            tcpServer.stop();
            App.getInstance().disconnectNetwork();

            RegisterMenuController regController = RegisterMenuController.getInstance();
            LoginMenuController loginController = LoginMenuController.getInstance();

            CommandResult<Void> r1 = regController.register(
                    uname,
                    "Secret123!",
                    "Secret123!",
                    "OfflinePlayer",
                    uname + "@pvz.com",
                    "male"
            );
            assertTrue(r1.isSuccess(), "Step 1 only stashes fields: " + r1.getMessage());

            CommandResult<Void> r2 = regController.pickQuestion(2, "PeaShooter", "PeaShooter");
            assertFalse(r2.isSuccess(), "Offline registration must not succeed");
            assertTrue(r2.getMessage().toLowerCase().contains("server"));
            assertFalse(clientLocalRepo.existsByUsername(uname));
            assertFalse(serverRepo.existsByUsername(uname));

            CommandResult<Void> loginRes = loginController.login(uname, "Secret123!", false);
            assertFalse(loginRes.isSuccess(), "Offline login must not succeed");
            assertNull(App.getInstance().getCurrentUser());
            assertFalse(App.getInstance().isConnected());
        });
    }

    @Test
    @DisplayName("Stay-logged-in: token file survives process simulation and resumes TCP session")
    void testStayLoggedInTokenResume() {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            String uname = "stay-" + UUID.randomUUID().toString().substring(0, 8);
            RegisterMenuController reg = RegisterMenuController.getInstance();
            LoginMenuController login = LoginMenuController.getInstance();
            MainMenuController main = MainMenuController.getInstance();

            assertTrue(reg.register(uname, "Password123!", "Password123!", "Stay", uname + "@pvz.com", "male").isSuccess());
            assertTrue(reg.pickQuestion(1, "A", "A").isSuccess());

            assertTrue(login.login(uname, "Password123!", true).isSuccess());
            String token = App.getInstance().getSessionToken();
            assertNotNull(token);
            assertTrue(App.getInstance().getLocalSessionStore().load().isPresent());

            // Simulate next client launch: drop in-memory user/connection, keep token file
            App.getInstance().disconnectNetwork();
            App.getInstance().setCurrentUser(null);
            App.getInstance().setSessionToken(null);
            App.getInstance().setUserRepository(new NullUserRepository());

            assertTrue(App.getInstance().reconnectStayLoggedInSession());
            assertNotNull(App.getInstance().getCurrentUser());
            assertEquals(uname, App.getInstance().getCurrentUser().getUsername());
            assertTrue(App.getInstance().isConnected());
            assertTrue(App.getInstance().getUserRepository() instanceof model.user.persistance.RemoteUserRepository);

            assertTrue(main.logout().isSuccess());
            assertTrue(App.getInstance().getLocalSessionStore().load().isEmpty());

            // Token revoked — resume must fail
            App.getInstance().getLocalSessionStore().save(uname, token);
            App.getInstance().setSessionToken(null);
            assertFalse(App.getInstance().reconnectStayLoggedInSession());
            assertNull(App.getInstance().getCurrentUser());
        });
    }

    @Test
    @DisplayName("R1 Unified Auth: Invalid password fails on server")
    void testInvalidPasswordFails() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            String uname = "val-" + UUID.randomUUID().toString().substring(0, 8);
            RegisterMenuController regController = RegisterMenuController.getInstance();
            LoginMenuController loginController = LoginMenuController.getInstance();

            CommandResult<Void> r1 = regController.register(uname, "GoodPass123!", "GoodPass123!", "Valid", uname + "@pvz.com", "male");
            assertTrue(r1.isSuccess(), "Step 1 register failed: " + r1.getMessage());
            CommandResult<Void> r2 = regController.pickQuestion(1, "Answer", "Answer");
            assertTrue(r2.isSuccess(), "Step 2 register failed: " + r2.getMessage());

            CommandResult<Void> loginRes = loginController.login(uname, "WrongPassword123!", false);
            assertFalse(loginRes.isSuccess());
            assertNull(App.getInstance().getCurrentUser());
        });
    }
}
