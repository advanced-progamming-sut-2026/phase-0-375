package model.network.enums;

public enum MatchmakingStatus {
    QUEUED,          // Client added to matchmaking queue
    ROOM_CREATED,    // Private room created, waiting for opponent
    MATCH_FOUND,     // Opponent matched, game room established
    CANCELLED,       // Matchmaking or room was successfully cancelled
    ERROR            // Error occurred (room full, invalid code, timeout, etc.)
}
