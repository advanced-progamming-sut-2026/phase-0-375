package com.sut.server.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Opaque stay-logged-in tokens. Raw secrets are never stored — only SHA-256 hashes.
 */
public final class SessionTokenStore {

    public static final long DEFAULT_TTL_SECONDS = 30L * 24 * 60 * 60; // 30 days

    private final Path storagePath;
    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<TokenRecord> tokens = new ArrayList<>();
    private final SecureRandom random = new SecureRandom();

    public SessionTokenStore() {
        this(resolveDefaultPath());
    }

    public SessionTokenStore(Path storagePath) {
        this.storagePath = Objects.requireNonNull(storagePath);
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        load();
    }

    public static Path resolveDefaultPath() {
        Path users = ServerUserRepository.resolveStoragePath();
        Path parent = users.getParent();
        if (parent == null) {
            return Path.of("sessions.json");
        }
        return parent.resolve("sessions.json");
    }

    /** Mints a new token for {@code username}; returns the raw secret (client-facing). */
    public String mint(String username) {
        return mint(username, DEFAULT_TTL_SECONDS);
    }

    public String mint(String username, long ttlSeconds) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username required");
        }
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String raw = HexFormat.of().formatHex(secret);
        long now = Instant.now().getEpochSecond();
        TokenRecord record = new TokenRecord(hash(raw), username.trim(), now, now + ttlSeconds);
        lock.writeLock().lock();
        try {
            tokens.add(record);
            flushUnlocked();
        } finally {
            lock.writeLock().unlock();
        }
        return raw;
    }

    /** Looks up a valid (non-expired) token; empty if unknown/expired. */
    public Optional<String> findUsername(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = hash(rawToken.trim());
        long now = Instant.now().getEpochSecond();
        lock.writeLock().lock();
        try {
            boolean dirty = false;
            Iterator<TokenRecord> it = tokens.iterator();
            Optional<String> found = Optional.empty();
            while (it.hasNext()) {
                TokenRecord r = it.next();
                if (r.expiresAt <= now) {
                    it.remove();
                    dirty = true;
                    continue;
                }
                if (tokenHash.equalsIgnoreCase(r.tokenHash)) {
                    found = Optional.of(r.username);
                }
            }
            if (dirty) {
                flushUnlocked();
            }
            return found;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        String tokenHash = hash(rawToken.trim());
        lock.writeLock().lock();
        try {
            boolean removed = tokens.removeIf(r -> tokenHash.equalsIgnoreCase(r.tokenHash));
            if (removed) {
                flushUnlocked();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void revokeAllForUser(String username) {
        if (username == null || username.isBlank()) return;
        String norm = username.trim();
        lock.writeLock().lock();
        try {
            boolean removed = tokens.removeIf(r -> r.username.equalsIgnoreCase(norm));
            if (removed) {
                flushUnlocked();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return tokens.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void load() {
        if (!Files.isRegularFile(storagePath)) {
            return;
        }
        try {
            TokenFile file = mapper.readValue(storagePath.toFile(), TokenFile.class);
            if (file != null && file.tokens != null) {
                tokens.clear();
                tokens.addAll(file.tokens);
            }
        } catch (IOException e) {
            System.err.println("[SessionTokenStore] Failed to load " + storagePath + ": " + e.getMessage());
        }
    }

    private void flushUnlocked() {
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
            TokenFile file = new TokenFile();
            file.tokens = new ArrayList<>(tokens);
            mapper.writeValue(tmp.toFile(), file);
            try {
                Files.move(tmp, storagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, storagePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[SessionTokenStore] Failed to flush " + storagePath + ": " + e.getMessage());
        }
    }

    static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TokenRecord {
        public String tokenHash;
        public String username;
        public long createdAt;
        public long expiresAt;

        public TokenRecord() {}

        public TokenRecord(String tokenHash, String username, long createdAt, long expiresAt) {
            this.tokenHash = tokenHash;
            this.username = username;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class TokenFile {
        public List<TokenRecord> tokens = new ArrayList<>();
    }
}
