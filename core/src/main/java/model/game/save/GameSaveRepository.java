package model.game.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Persists one in-progress level save per username under {@code data/saves/}.
 */
public final class GameSaveRepository {

    private static final String DEFAULT_DIR = "data/saves";
    /** Optional override for tests: absolute/relative save directory. */
    public static final String DIR_PROPERTY = "pvz.save.dir";

    private final ObjectMapper mapper;
    private final File directory;

    public GameSaveRepository() {
        this(resolveDirectory());
    }

    public GameSaveRepository(File directory) {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.directory = directory == null ? new File(DEFAULT_DIR) : directory;
    }

    private static File resolveDirectory() {
        String override = System.getProperty(DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return new File(override);
        }
        return new File(DEFAULT_DIR);
    }

    public void save(GameSaveData data) throws IOException {
        if (data == null || data.getUsername() == null || data.getUsername().isBlank()) {
            throw new IllegalArgumentException("Save data requires a username.");
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create save directory: " + directory.getAbsolutePath());
        }
        mapper.writeValue(fileFor(data.getUsername()), data);
    }

    public Optional<GameSaveData> load(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        File file = fileFor(username);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(mapper.readValue(file, GameSaveData.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public boolean exists(String username) {
        return username != null && !username.isBlank() && fileFor(username).exists();
    }

    public void delete(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        File file = fileFor(username);
        if (file.exists()) {
            file.delete();
        }
    }

    private File fileFor(String username) {
        String safe = username.replaceAll("[^a-zA-Z0-9._-]", "_");
        return new File(directory, safe + ".json");
    }
}
