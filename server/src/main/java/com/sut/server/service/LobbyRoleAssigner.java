package com.sut.server.service;

import com.sut.server.net.ClientConnectionHandler;
import model.network.enums.PlayerRole;

/**
 * Complementary plant/zombie role assignment for matchmaking and invites.
 */
final class LobbyRoleAssigner {

    private LobbyRoleAssigner() {
    }

    static PlayerRole complementaryQueueRole(PlayerRole preferred, PlayerRole candidate) {
        if (preferred == PlayerRole.PLANT
                && (candidate == PlayerRole.ZOMBIE || candidate == PlayerRole.ANY)) {
            return PlayerRole.PLANT;
        }
        if (preferred == PlayerRole.ZOMBIE
                && (candidate == PlayerRole.PLANT || candidate == PlayerRole.ANY)) {
            return PlayerRole.ZOMBIE;
        }
        if (preferred == PlayerRole.ANY && candidate == PlayerRole.PLANT) {
            return PlayerRole.ZOMBIE;
        }
        if (preferred == PlayerRole.ANY && candidate == PlayerRole.ZOMBIE) {
            return PlayerRole.PLANT;
        }
        if (preferred == PlayerRole.ANY && candidate == PlayerRole.ANY) {
            return PlayerRole.PLANT;
        }
        return null;
    }

    static PlayerRole fallbackQueueRole(PlayerRole preferred, PlayerRole candidateRole) {
        if (candidateRole == PlayerRole.PLANT) {
            return PlayerRole.ZOMBIE;
        }
        if (candidateRole == PlayerRole.ZOMBIE) {
            return PlayerRole.PLANT;
        }
        return preferred == PlayerRole.ZOMBIE ? PlayerRole.ZOMBIE : PlayerRole.PLANT;
    }

    static PlayerRole hostRoleForPrivateRoom(PlayerRole hostRole, PlayerRole guestPreferredRole) {
        if (hostRole == PlayerRole.PLANT || hostRole == PlayerRole.ZOMBIE) {
            return hostRole;
        }
        if (guestPreferredRole == PlayerRole.PLANT) {
            return PlayerRole.ZOMBIE;
        }
        if (guestPreferredRole == PlayerRole.ZOMBIE) {
            return PlayerRole.PLANT;
        }
        return PlayerRole.PLANT;
    }

    static ClientConnectionHandler[] plantThenZombie(
            PlayerRole inviterRole,
            ClientConnectionHandler inviter,
            ClientConnectionHandler target
    ) {
        if (inviterRole == PlayerRole.ZOMBIE) {
            return new ClientConnectionHandler[] {target, inviter};
        }
        return new ClientConnectionHandler[] {inviter, target};
    }
}
