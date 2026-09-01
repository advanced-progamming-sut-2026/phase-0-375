package model.network;

import model.network.enums.InviteDecision;
import model.network.enums.InviteStatus;
import model.network.enums.PlayerRole;
import model.network.packet.CancelInvitePacket;
import model.network.packet.InviteReceivedPacket;
import model.network.packet.InviteRequestPacket;
import model.network.packet.InviteResponsePacket;
import model.network.packet.InviteStatusPacket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InviteFlowUnitTest {

    @Test
    @DisplayName("Invite Request packet creation and validation")
    void testInviteRequestPacketCreation() {
        InviteRequestPacket packet = new InviteRequestPacket("Bob", PlayerRole.PLANT, "Alice");
        assertEquals("Bob", packet.getTargetUsername());
        assertEquals(PlayerRole.PLANT, packet.getPreferredRole());
        assertEquals("Alice", packet.getInviterUsername());
    }

    @Test
    @DisplayName("Invite Received packet timeout default and custom values")
    void testInviteReceivedPacketTimeout() {
        InviteReceivedPacket packet1 = new InviteReceivedPacket("inv-1", "Alice", PlayerRole.ZOMBIE);
        assertEquals(10, packet1.getTimeoutSeconds());

        InviteReceivedPacket packet2 = new InviteReceivedPacket("inv-2", "Alice", PlayerRole.PLANT, 15);
        assertEquals(15, packet2.getTimeoutSeconds());
    }

    @Test
    @DisplayName("Invite Response decision types")
    void testInviteResponseDecisions() {
        InviteResponsePacket accept = new InviteResponsePacket("inv-1", "Alice", InviteDecision.ACCEPT);
        assertEquals(InviteDecision.ACCEPT, accept.getDecision());

        InviteResponsePacket decline = new InviteResponsePacket("inv-1", "Alice", InviteDecision.DECLINE, "Busy");
        assertEquals(InviteDecision.DECLINE, decline.getDecision());
        assertEquals("Busy", decline.getReason());

        InviteResponsePacket timeout = new InviteResponsePacket("inv-1", "Alice", InviteDecision.TIMEOUT);
        assertEquals(InviteDecision.TIMEOUT, timeout.getDecision());
    }

    @Test
    @DisplayName("Cancel Invite packet properties")
    void testCancelInvite() {
        CancelInvitePacket cancel = new CancelInvitePacket("inv-1", "Bob");
        assertEquals("inv-1", cancel.getInviteId());
        assertEquals("Bob", cancel.getTargetUsername());
    }

    @Test
    @DisplayName("Invite Status packet statuses")
    void testInviteStatusPacket() {
        InviteStatusPacket p1 = new InviteStatusPacket("inv-1", InviteStatus.PENDING, "Waiting...");
        assertEquals(InviteStatus.PENDING, p1.getStatus());

        InviteStatusPacket p2 = new InviteStatusPacket("inv-1", InviteStatus.BUSY, "Target is busy");
        assertEquals(InviteStatus.BUSY, p2.getStatus());

        InviteStatusPacket p3 = new InviteStatusPacket("inv-1", InviteStatus.TIMED_OUT, "Timed out");
        assertEquals(InviteStatus.TIMED_OUT, p3.getStatus());
    }
}
