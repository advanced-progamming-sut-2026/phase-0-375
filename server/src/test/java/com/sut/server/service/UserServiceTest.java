package com.sut.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sut.server.net.ClientConnectionHandler;
import com.sut.server.net.PacketRouter;
import com.sut.server.net.TcpServer;
import com.sut.server.repository.ServerUserRepository;
import model.network.enums.UserCommand;
import model.network.packet.user.ProfileUpdateRequestPacket;
import model.network.packet.user.ProfileUpdateResponsePacket;
import model.network.packet.user.UserCommandRequestPacket;
import model.network.packet.user.UserCommandResponsePacket;
import model.user.PasswordHasher;
import model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    @TempDir
    Path tempDir;

    private ServerUserRepository repo;
    private UserService userService;
    private AuthService authService;
    private ClientConnectionHandler connection;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Socket accepted;
    private String username;

    @BeforeEach
    void setUp() throws IOException {
        repo = new ServerUserRepository(tempDir.resolve("users.json"));
        authService = new AuthService(repo);
        userService = new UserService(repo, authService);

        username = "u-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordHasher.hash("Password123!"));
        user.setNickname("Tester");
        user.setEmail(username + "@pvz.com");
        user.setGender("male");
        user.setSecurityQuestionNumber(1);
        user.setSecurityAnswer("Sun");
        user.setCoins(100);
        user.setGems(10);
        repo.save(user);

        serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        clientSocket = new Socket("127.0.0.1", serverSocket.getLocalPort());
        accepted = serverSocket.accept();

        PacketRouter router = new PacketRouter();
        ObjectMapper mapper = TcpServer.createDefaultObjectMapper();
        connection = new ClientConnectionHandler(
                "test-conn", accepted, null, router, mapper);
        connection.setUsername(username);
        connection.setUserProfile(repo.findByUsername(username).orElseThrow());
    }

    @AfterEach
    void tearDown() throws IOException {
        try {
            if (accepted != null && !accepted.isClosed()) accepted.close();
        } catch (Exception ignored) {}
        try {
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (Exception ignored) {}
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Spend coins updates server balance; insufficient funds rejected")
    void spendCoins() {
        Map<String, String> args = new HashMap<>();
        args.put("amount", "40");
        UserCommandResponsePacket ok = userService.handleUserCommand(connection,
                new UserCommandRequestPacket(UserCommand.SPEND_COINS, args));
        assertTrue(ok.isSuccess(), ok.getMessage());
        assertEquals(60, ok.getUser().getCoins());
        assertNull(ok.getUser().getPasswordHash());

        args.put("amount", "9999");
        UserCommandResponsePacket fail = userService.handleUserCommand(connection,
                new UserCommandRequestPacket(UserCommand.SPEND_COINS, args));
        assertFalse(fail.isSuccess());
        assertEquals("INSUFFICIENT_FUNDS", fail.getErrorCode());
        assertEquals(60, repo.findByUsername(username).orElseThrow().getCoins());
    }

    @Test
    @DisplayName("Profile nickname update persists and sanitizes response")
    void profileUpdate() {
        ProfileUpdateResponsePacket resp = userService.handleProfileUpdate(connection,
                new ProfileUpdateRequestPacket(null, "NewNick", null));
        assertTrue(resp.isSuccess(), resp.getMessage());
        assertEquals("NewNick", resp.getUser().getNickname());
        assertEquals("NewNick", repo.findByUsername(username).orElseThrow().getNickname());
        assertNull(resp.getUser().getPasswordHash());
    }

    @Test
    @DisplayName("Duplicate username rejected on profile rename")
    void duplicateUsernameRejected() {
        User other = new User();
        other.setUsername("taken-user");
        other.setPasswordHash("x");
        other.setNickname("Other");
        other.setEmail("other@pvz.com");
        other.setGender("female");
        repo.save(other);

        ProfileUpdateResponsePacket resp = userService.handleProfileUpdate(connection,
                new ProfileUpdateRequestPacket("taken-user", null, null));
        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().toLowerCase().contains("taken"));
        assertTrue(repo.existsByUsername(username));
    }
}
