package model.user.persistance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Client-only stay-logged-in credential file. Stores username + opaque token — never password.
 * Override path with system property {@code pvz.session.file} (useful in tests).
 */
public final class LocalSessionStore {

    public static final String DEFAULT_RELATIVE_PATH = "data/session.json";
    public static final String PATH_PROPERTY = "pvz.session.file";

    private final Path overridePath;
    private final ObjectMapper mapper = new ObjectMapper();

    public LocalSessionStore() {
        this(null);
    }

    public LocalSessionStore(Path path) {
        this.overridePath = path;
    }

    public Path getPath() {
        if (overridePath != null) {
            return overridePath;
        }
        String prop = System.getProperty(PATH_PROPERTY);
        if (prop != null && !prop.isBlank()) {
            return Path.of(prop.trim());
        }
        return Path.of(DEFAULT_RELATIVE_PATH);
    }

    public void save(String username, String token) {
        if (username == null || username.isBlank() || token == null || token.isBlank()) {
            return;
        }
        Path path = getPath();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            SessionFile file = new SessionFile();
            file.username = username.trim();
            file.token = token.trim();
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), file);
        } catch (Exception e) {
            System.err.println("[LocalSessionStore] Failed to write " + path + ": " + e.getMessage());
        }
    }

    public Optional<SessionFile> load() {
        Path path = getPath();
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            SessionFile file = mapper.readValue(path.toFile(), SessionFile.class);
            if (file == null || file.username == null || file.username.isBlank()
                    || file.token == null || file.token.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(file);
        } catch (Exception e) {
            System.err.println("[LocalSessionStore] Failed to read " + path + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public void clear() {
        Path path = getPath();
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            System.err.println("[LocalSessionStore] Failed to delete " + path + ": " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SessionFile {
        public String username;
        public String token;
    }
}
