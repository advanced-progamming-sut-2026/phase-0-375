package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.network.client.NetworkClient;
import model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedClientConnectionTest {

    private App app;

    @BeforeEach
    void setUp() {
        app = App.getInstance();
        app.disconnectNetwork();
        app.setCurrentUser(null);
        app.setCurrentMenu(MenuType.REGISTER);
    }

    @AfterEach
    void tearDown() {
        app.disconnectNetwork();
        app.setCurrentUser(null);
    }

    @Test
    @DisplayName("App manages NetworkClient reference and connection state")
    void testAppNetworkClientLifecycle() {
        assertNull(app.getNetworkClient());
        assertFalse(app.isConnected());

        NetworkClient mockClient = new NetworkClient("127.0.0.1", 9999);
        app.setNetworkClient(mockClient);
        assertSame(mockClient, app.getNetworkClient());

        app.disconnectNetwork();
        assertNull(app.getNetworkClient());
        assertFalse(app.isConnected());
    }

    @Test
    @DisplayName("App server endpoint configuration")
    void testServerEndpointConfig() {
        app.setServerEndpoint("192.168.1.100", 9090);
        assertEquals("192.168.1.100", app.getServerHost());
        assertEquals(9090, app.getServerPort());

        app.setServerEndpoint(null, -1);
        assertEquals(NetworkClient.DEFAULT_HOST, app.getServerHost());
        assertEquals(NetworkClient.DEFAULT_PORT, app.getServerPort());
    }

    @Test
    @DisplayName("LoginMenuController validation for empty inputs")
    void testLoginValidation() {
        LoginMenuController controller = LoginMenuController.getInstance();

        CommandResult<Void> emptyUser = controller.login("", "password", false);
        assertFalse(emptyUser.isSuccess());
        assertTrue(emptyUser.getMessage().contains("Username cannot be empty"));

        CommandResult<Void> emptyPass = controller.login("alice", "", false);
        assertFalse(emptyPass.isSuccess());
        assertTrue(emptyPass.getMessage().contains("Password cannot be empty"));
    }

    @Test
    @DisplayName("RegisterMenuController only guards empty/mismatch fields; authority is on the server")
    void testRegisterValidation() {
        RegisterMenuController controller = RegisterMenuController.getInstance();

        CommandResult<Void> mismatch = controller.register("alice", "Password123!", "Different123!", "Alice", "alice@pvz.com", "female");
        assertFalse(mismatch.isSuccess());
        assertTrue(mismatch.getMessage().contains("do not match"));

        CommandResult<Void> badGender = controller.register("alice", "Password123!", "Password123!", "Alice", "alice@pvz.com", "other");
        assertFalse(badGender.isSuccess());
        assertTrue(badGender.getMessage().contains("Gender must be 'male' or 'female'"));

        CommandResult<Void> emptyEmail = controller.register("alice", "Password123!", "Password123!", "Alice", "", "female");
        assertFalse(emptyEmail.isSuccess());
        assertTrue(emptyEmail.getMessage().contains("Email cannot be empty"));
    }

    @Test
    @DisplayName("MainMenuController logout cleans session and disconnects network")
    void testLogoutCleansSession() {
        MainMenuController controller = MainMenuController.getInstance();

        User user = new User();
        user.setUsername("bob");
        user.setStayLoggedIn(true);
        app.setCurrentUser(user);
        app.setCurrentMenu(MenuType.MAIN);

        NetworkClient mockClient = new NetworkClient("127.0.0.1", 9999);
        app.setNetworkClient(mockClient);

        CommandResult<Void> result = controller.logout();
        assertTrue(result.isSuccess());
        assertNull(app.getCurrentUser());
        assertFalse(app.isConnected());
        assertEquals(MenuType.REGISTER, app.getCurrentMenu());
    }
}
