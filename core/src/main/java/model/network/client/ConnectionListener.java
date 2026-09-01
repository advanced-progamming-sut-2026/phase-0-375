package model.network.client;

/**
 * Listener interface for TCP socket connection lifecycle events.
 */
public interface ConnectionListener {

    /**
     * Invoked when the socket connection is successfully established.
     */
    void onConnected();

    /**
     * Invoked when the connection is terminated.
     *
     * @param reason   Human-readable explanation of why the disconnection occurred.
     * @param expected True if the disconnection was initiated locally by client code.
     */
    void onDisconnected(String reason, boolean expected);

    /**
     * Invoked when an unexpected connection or socket I/O error occurs.
     *
     * @param cause The underlying exception.
     */
    void onError(Throwable cause);
}
