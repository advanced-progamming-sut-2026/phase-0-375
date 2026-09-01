package com.sut.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerLauncherTest {

    @Test
    @DisplayName("Should resolve default port when no arguments or properties are set")
    void testDefaultPortResolution() {
        int port = ServerLauncher.resolvePort(new String[0]);
        assertEquals(ServerLauncher.DEFAULT_PORT, port);
    }

    @Test
    @DisplayName("Should resolve port from positional CLI argument")
    void testPositionalCliPortResolution() {
        int port = ServerLauncher.resolvePort(new String[]{"9095"});
        assertEquals(9095, port);
    }

    @Test
    @DisplayName("Should resolve port from --port flag")
    void testFlagCliPortResolution() {
        int port1 = ServerLauncher.resolvePort(new String[]{"--port", "9096"});
        assertEquals(9096, port1);

        int port2 = ServerLauncher.resolvePort(new String[]{"--port=9097"});
        assertEquals(9097, port2);
    }

    @Test
    @DisplayName("Should resolve host from CLI arguments")
    void testCliHostResolution() {
        String host1 = ServerLauncher.resolveHost(new String[]{"9095", "127.0.0.1"});
        assertEquals("127.0.0.1", host1);

        String host2 = ServerLauncher.resolveHost(new String[]{"--host=192.168.1.100"});
        assertEquals("192.168.1.100", host2);
    }
}
