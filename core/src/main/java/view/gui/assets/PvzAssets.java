package view.gui.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Shared libPVZ assets for gameplay screens. Created once per {@link view.gui.PvzGdxGame}.
 */
public final class PvzAssets implements Disposable {
    public final TextureBank textures;
    public final PamPlayer player;
    public final PamCatalog pamCatalog;
    public final PlantSpritesheetCatalog plantSheets;
    public final String resolution;
    public final FileHandle root;

    private PvzAssets(TextureBank textures, PamPlayer player, PamCatalog pamCatalog,
                      PlantSpritesheetCatalog plantSheets, String resolution, FileHandle root) {
        this.textures = textures;
        this.player = player;
        this.pamCatalog = pamCatalog;
        this.plantSheets = plantSheets;
        this.resolution = resolution;
        this.root = root;
    }

    public static PvzAssets createDefault() {
        FileHandle root = resolveAssetsRoot();
        String res = System.getProperty("pvz.resolution", "768");
        TextureBank textures = new TextureBank(res, root);
        PamCatalog catalog = PamCatalog.load(root);
        PlantSpritesheetCatalog sheets = new PlantSpritesheetCatalog(root, res);
        return new PvzAssets(textures, new PamPlayer(textures, root), catalog, sheets, res, root);
    }

    /**
     * Ensures {@code -Dpvz.assets} / {@code -Dpvz.resolution} are set before libGDX starts.
     * Safe to call from the desktop launcher (IDE runs often omit Gradle {@code systemProperty}).
     */
    public static void applyLauncherDefaults() {
        if (isBlank(System.getProperty("pvz.assets")) || isBlank(System.getProperty("pvz.resolution"))) {
            Properties props = loadGradleProperties();
            if (isBlank(System.getProperty("pvz.assets"))) {
                String fromEnv = System.getenv("PVZ_ASSETS");
                String fromProps = props.getProperty("pvz.assets");
                String path = !isBlank(fromEnv) ? fromEnv.trim()
                        : !isBlank(fromProps) ? fromProps.trim()
                        : null;
                if (path != null) {
                    System.setProperty("pvz.assets", path);
                }
            }
            if (isBlank(System.getProperty("pvz.resolution"))) {
                String res = props.getProperty("pvz.resolution", "768");
                System.setProperty("pvz.resolution", res.trim());
            }
        }
    }

    private static FileHandle resolveAssetsRoot() {
        String prop = System.getProperty("pvz.assets");
        if (isBlank(prop)) {
            String env = System.getenv("PVZ_ASSETS");
            if (!isBlank(env)) {
                prop = env;
            }
        }
        if (isBlank(prop)) {
            Properties props = loadGradleProperties();
            prop = props.getProperty("pvz.assets");
        }
        if (isBlank(prop)) {
            throw new IllegalStateException(
                    "pvz.assets is not set. Add it to gradle.properties, pass -Dpvz.assets=/path/to/pvz-assets, "
                            + "or set PVZ_ASSETS.");
        }

        String path = prop.trim();
        FileHandle handle = Gdx.files.absolute(path);
        if (!handle.exists() || !handle.isDirectory()) {
            // Absolute FileHandle can miss WSL/Windows quirks; retry via java.io.File.
            File file = new File(path);
            if (!file.isDirectory()) {
                File parentCandidate = new File("..", path);
                if (parentCandidate.isDirectory()) {
                    file = parentCandidate;
                }
            }
            if (file.isDirectory()) {
                handle = Gdx.files.absolute(file.getAbsolutePath());
            } else {
                throw new IllegalStateException(
                        "pvz.assets path does not exist or is not a directory: " + path);
            }
        }

        FileHandle resources = handle.child("RESOURCES.json");
        if (!resources.exists()) {
            resources = handle.child("resources.json");
        }
        if (!resources.exists()) {
            throw new IllegalStateException(
                    "No RESOURCES.json / resources.json under pvz.assets: " + handle.path());
        }

        Gdx.app.log("PvzAssets", "Using asset root: " + handle.path());
        return handle;
    }

    private static Properties loadGradleProperties() {
        Properties props = new Properties();
        File[] candidates = {
                new File("gradle.properties"),
                new File("../gradle.properties"),
        };
        for (File file : candidates) {
            if (!file.isFile()) {
                continue;
            }
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
                return props;
            } catch (IOException ignored) {
                // try next
            }
        }
        return props;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public void dispose() {
        textures.dispose();
    }
}
