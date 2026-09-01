package com.sut.lwjgl3.network;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Desktop LWJGL3 network client wrapper extending {@link model.network.client.NetworkClient}.
 * Provides module-level conformance to the {@code :lwjgl3} package layout.
 */
public class NetworkClient extends model.network.client.NetworkClient {

    public NetworkClient() {
        super();
    }

    public NetworkClient(String host, int port) {
        super(host, port);
    }

    public NetworkClient(String host, int port, ObjectMapper objectMapper) {
        super(host, port, objectMapper);
    }
}
