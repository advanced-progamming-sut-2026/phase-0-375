package model.network.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Host/port the game client uses to reach the dedicated server.
 * <p>
 * Resolution order: {@code -Dpvz.client.host/port}, then {@code -Dpvz.server.host/port},
 * then classpath {@code /pvz-network.properties} (generated from {@code gradle.properties}),
 * then {@link NetworkClient} defaults.
 */
public final class ServerEndpoint {
    public static final String RESOURCE = "/pvz-network.properties";

    private static volatile Properties resourceProps;
    private static volatile Properties diskGradleProps;

    private ServerEndpoint() {}

    public static String host() {
        String host = firstNonBlank(
                System.getProperty("pvz.client.host"),
                System.getProperty("pvz.server.host"),
                fromGradleProps("pvz.client.host"),
                fromGradleProps("pvz.server.host"),
                resource("host"));
        return host != null ? host : NetworkClient.DEFAULT_HOST;
    }

    public static int port() {
        String raw = firstNonBlank(
                System.getProperty("pvz.client.port"),
                System.getProperty("pvz.server.port"),
                fromGradleProps("pvz.client.port"),
                fromGradleProps("pvz.server.port"),
                resource("port"));
        if (raw == null) {
            return NetworkClient.DEFAULT_PORT;
        }
        try {
            int parsed = Integer.parseInt(raw);
            return parsed > 0 ? parsed : NetworkClient.DEFAULT_PORT;
        } catch (NumberFormatException ignored) {
            return NetworkClient.DEFAULT_PORT;
        }
    }

    private static String fromGradleProps(String key) {
        Properties props = loadDiskGradleProperties();
        if (props == null) {
            return null;
        }
        return firstNonBlank(props.getProperty(key));
    }

    private static Properties loadDiskGradleProperties() {
        Properties cached = diskGradleProps;
        if (cached != null) {
            return cached;
        }
        synchronized (ServerEndpoint.class) {
            if (diskGradleProps != null) {
                return diskGradleProps;
            }
            Properties props = new Properties();
            java.io.File[] candidates = {
                    new java.io.File("gradle.properties"),
                    new java.io.File("../gradle.properties"),
            };
            for (java.io.File file : candidates) {
                if (!file.isFile()) {
                    continue;
                }
                try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                    props.load(in);
                    break;
                } catch (IOException ignored) {
                }
            }
            diskGradleProps = props;
            return props;
        }
    }

    private static String resource(String key) {
        Properties props = loadResource();
        if (props == null) {
            return null;
        }
        return firstNonBlank(props.getProperty(key));
    }

    private static Properties loadResource() {
        Properties cached = resourceProps;
        if (cached != null) {
            return cached;
        }
        synchronized (ServerEndpoint.class) {
            if (resourceProps != null) {
                return resourceProps;
            }
            try (InputStream in = ServerEndpoint.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    resourceProps = new Properties();
                    return resourceProps;
                }
                Properties loaded = new Properties();
                loaded.load(in);
                resourceProps = loaded;
                return loaded;
            } catch (IOException ignored) {
                resourceProps = new Properties();
                return resourceProps;
            }
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
