package model.user.persistance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalSessionStoreTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearProp() {
        System.clearProperty(LocalSessionStore.PATH_PROPERTY);
    }

    @Test
    @DisplayName("Save/load/clear roundtrip")
    void roundtrip() {
        Path file = tempDir.resolve("session.json");
        LocalSessionStore store = new LocalSessionStore(file);
        assertTrue(store.load().isEmpty());

        store.save("alice", "tok123");
        var loaded = store.load().orElseThrow();
        assertEquals("alice", loaded.username);
        assertEquals("tok123", loaded.token);

        store.clear();
        assertTrue(store.load().isEmpty());
    }
}
