package model.network.client;

/**
 * Lifecycle states of the TCP socket {@link NetworkClient}.
 */
public enum ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}
