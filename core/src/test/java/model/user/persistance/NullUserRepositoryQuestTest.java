package model.user.persistance;

import model.app.App;
import model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class NullUserRepositoryQuestTest {

    private final App app = App.getInstance();

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("alice");
        user.setCoins(100);
        user.setQuestStatus(new HashMap<>());
        app.setCurrentUser(user);
        app.setUserRepository(new NullUserRepository());
    }

    @AfterEach
    void tearDown() {
        app.setCurrentUser(null);
        app.setUserRepository(new NullUserRepository());
    }

    @Test
    void completeQuestMarksLocalUserStatus() {
        NullUserRepository repo = new NullUserRepository();
        repo.completeQuest("alice", "Daily Sun", true);
        assertTrue(app.getCurrentUser().getQuestStatus().get("Daily Sun"));
    }

    @Test
    void addCoinsUpdatesLocalUser() {
        NullUserRepository repo = new NullUserRepository();
        repo.addCoins("alice", 50);
        assertEquals(150, app.getCurrentUser().getCoins());
    }
}
