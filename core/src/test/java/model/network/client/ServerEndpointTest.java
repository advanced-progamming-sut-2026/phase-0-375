package model.network.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerEndpointTest {

    @AfterEach
    void clearOverrides() {
        System.clearProperty("pvz.client.host");
        System.clearProperty("pvz.client.port");
        System.clearProperty("pvz.server.host");
        System.clearProperty("pvz.server.port");
    }

    @Test
    void prefersClientSystemPropertiesOverServerBind() {
        System.setProperty("pvz.server.host", "0.0.0.0");
        System.setProperty("pvz.server.port", "8080");
        System.setProperty("pvz.client.host", "10.0.0.5");
        System.setProperty("pvz.client.port", "9090");

        assertEquals("10.0.0.5", ServerEndpoint.host());
        assertEquals(9090, ServerEndpoint.port());
    }
}
