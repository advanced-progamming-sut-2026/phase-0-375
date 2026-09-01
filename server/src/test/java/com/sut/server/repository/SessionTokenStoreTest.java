package com.sut.server.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SessionTokenStoreTest {

    @TempDir
    Path tempDir;

    private SessionTokenStore store;

    @BeforeEach
    void setUp() {
        store = new SessionTokenStore(tempDir.resolve("sessions.json"));
    }

    @Test
    @DisplayName("Minted token resolves to username; revoke clears it")
    void mintFindRevoke() {
        String raw = store.mint("alice");
        assertEquals(64, raw.length());
        assertEquals("alice", store.findUsername(raw).orElseThrow());
        assertTrue(store.findUsername("nope").isEmpty());

        store.revokeToken(raw);
        assertTrue(store.findUsername(raw).isEmpty());
    }

    @Test
    @DisplayName("revokeAllForUser removes every token for that account")
    void revokeAllForUser() {
        String a = store.mint("bob");
        String b = store.mint("bob");
        String c = store.mint("carol");
        store.revokeAllForUser("bob");
        assertTrue(store.findUsername(a).isEmpty());
        assertTrue(store.findUsername(b).isEmpty());
        assertEquals("carol", store.findUsername(c).orElseThrow());
    }

    @Test
    @DisplayName("Expired tokens are purged on lookup")
    void expiredPurged() {
        String raw = store.mint("dave", -1); // already expired
        assertTrue(store.findUsername(raw).isEmpty());
    }
}
