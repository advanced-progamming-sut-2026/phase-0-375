package model.network.enums;

public enum MatchmakingMode {
    RANDOM,          // Queue in random matchmaking pool
    CREATE_ROOM,     // Create a private room with generated invite code
    DIRECT_INVITE    // Join a private room with an existing invite code
}
