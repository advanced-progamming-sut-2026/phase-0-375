package model.network;

import model.network.dto.PlantSnapshotDto;
import model.network.dto.ProjectileSnapshotDto;
import model.network.dto.ZombieSnapshotDto;
import model.network.enums.MatchmakingMode;
import model.network.enums.MatchmakingStatus;
import model.network.enums.PlayerRole;
import model.network.enums.ReactionType;
import model.network.packet.Packet;
import model.network.packet.PacketType;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
import model.network.packet.auth.LogoutRequestPacket;
import model.network.packet.auth.RegisterRequestPacket;
import model.network.packet.auth.RegisterResponsePacket;
import model.network.packet.auth.SessionResumeRequestPacket;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.game.GameStateSnapshotPacket;
import model.network.packet.game.PlacePlantRequestPacket;
import model.network.packet.game.PlaceZombieRequestPacket;
import model.network.packet.game.PlayerActionResponsePacket;
import model.network.packet.matchmaking.CancelMatchmakingPacket;
import model.network.packet.matchmaking.MatchFoundPacket;
import model.network.packet.matchmaking.MatchmakingRequestPacket;
import model.network.packet.matchmaking.MatchmakingResponsePacket;
import model.network.packet.system.ErrorMessagePacket;
import model.network.packet.system.HeartbeatPacket;
import model.network.util.NetworkJsonMapper;
import model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketSerializationTest {

    @Test
    @DisplayName("RegisterRequestPacket and RegisterResponsePacket roundtrip serialization")
    void testRegisterPackets() throws Exception {
        RegisterRequestPacket req = new RegisterRequestPacket(
                "crazy_dave", "hashed_pwd_123", "Dave",
                "dave@pvz.com", "male", 1, "tacos"
        );
        String reqJson = NetworkJsonMapper.serialize(req);
        Packet deserializedReq = NetworkJsonMapper.deserialize(reqJson);

        assertInstanceOf(RegisterRequestPacket.class, deserializedReq);
        RegisterRequestPacket typedReq = (RegisterRequestPacket) deserializedReq;
        assertEquals(PacketType.REGISTER_REQUEST, typedReq.getType());
        assertEquals("crazy_dave", typedReq.getUsername());
        assertEquals("hashed_pwd_123", typedReq.getPasswordHash());
        assertEquals("Dave", typedReq.getNickname());
        assertEquals("dave@pvz.com", typedReq.getEmail());
        assertEquals("male", typedReq.getGender());
        assertEquals(1, typedReq.getSecurityQuestionNumber());
        assertEquals("tacos", typedReq.getSecurityAnswer());

        RegisterResponsePacket res = new RegisterResponsePacket(true, "Registration successful");
        String resJson = NetworkJsonMapper.serialize(res);
        Packet deserializedRes = NetworkJsonMapper.deserialize(resJson);

        assertInstanceOf(RegisterResponsePacket.class, deserializedRes);
        RegisterResponsePacket typedRes = (RegisterResponsePacket) deserializedRes;
        assertEquals(PacketType.REGISTER_RESPONSE, typedRes.getType());
        assertTrue(typedRes.isSuccess());
        assertEquals("Registration successful", typedRes.getMessage());
    }

    @Test
    @DisplayName("LoginRequestPacket and LoginResponsePacket roundtrip serialization with User profile")
    void testLoginPackets() throws Exception {
        LoginRequestPacket req = new LoginRequestPacket("crazy_dave", "hashed_pwd_123", true);
        String reqJson = NetworkJsonMapper.serialize(req);
        Packet deserializedReq = NetworkJsonMapper.deserialize(reqJson);

        assertInstanceOf(LoginRequestPacket.class, deserializedReq);
        LoginRequestPacket typedReq = (LoginRequestPacket) deserializedReq;
        assertEquals(PacketType.LOGIN_REQUEST, typedReq.getType());
        assertEquals("crazy_dave", typedReq.getUsername());
        assertEquals("hashed_pwd_123", typedReq.getPasswordHash());
        assertTrue(typedReq.isStayLoggedIn());

        User user = new User();
        user.setUsername("crazy_dave");
        user.setNickname("Dave");
        user.setEmail("dave@pvz.com");
        user.setCoins(1000);
        user.setGems(50);
        user.setGamesPlayed(5);

        LoginResponsePacket res = new LoginResponsePacket(true, "Welcome back", user, "tok_abc");
        String resJson = NetworkJsonMapper.serialize(res);
        Packet deserializedRes = NetworkJsonMapper.deserialize(resJson);

        assertInstanceOf(LoginResponsePacket.class, deserializedRes);
        LoginResponsePacket typedRes = (LoginResponsePacket) deserializedRes;
        assertEquals(PacketType.LOGIN_RESPONSE, typedRes.getType());
        assertTrue(typedRes.isSuccess());
        assertEquals("Welcome back", typedRes.getMessage());
        assertNotNull(typedRes.getUserProfile());
        assertEquals("crazy_dave", typedRes.getUserProfile().getUsername());
        assertEquals(1000, typedRes.getUserProfile().getCoins());
        assertEquals("tok_abc", typedRes.getSessionToken());
    }

    @Test
    @DisplayName("SessionResumeRequestPacket roundtrip serialization")
    void testSessionResumePacket() throws Exception {
        SessionResumeRequestPacket req = new SessionResumeRequestPacket("opaque_token_hex");
        String json = NetworkJsonMapper.serialize(req);
        Packet deserialized = NetworkJsonMapper.deserialize(json);

        assertInstanceOf(SessionResumeRequestPacket.class, deserialized);
        SessionResumeRequestPacket typed = (SessionResumeRequestPacket) deserialized;
        assertEquals(PacketType.SESSION_RESUME_REQUEST, typed.getType());
        assertEquals("opaque_token_hex", typed.getToken());
    }

    @Test
    @DisplayName("LogoutRequestPacket roundtrip serialization")
    void testLogoutPacket() throws Exception {
        LogoutRequestPacket req = new LogoutRequestPacket("crazy_dave", "session_tok_999");
        String json = NetworkJsonMapper.serialize(req);
        Packet deserialized = NetworkJsonMapper.deserialize(json);

        assertInstanceOf(LogoutRequestPacket.class, deserialized);
        LogoutRequestPacket typed = (LogoutRequestPacket) deserialized;
        assertEquals(PacketType.LOGOUT_REQUEST, typed.getType());
        assertEquals("crazy_dave", typed.getUsername());
        assertEquals("session_tok_999", typed.getSessionToken());
    }

    @Test
    @DisplayName("Matchmaking packets roundtrip serialization across all modes and roles")
    void testMatchmakingPackets() throws Exception {
        // MatchmakingRequestPacket
        MatchmakingRequestPacket req = new MatchmakingRequestPacket(
                MatchmakingMode.RANDOM, null, PlayerRole.PLANT, "crazy_dave"
        );
        String reqJson = NetworkJsonMapper.serialize(req);
        Packet deserializedReq = NetworkJsonMapper.deserialize(reqJson);

        assertInstanceOf(MatchmakingRequestPacket.class, deserializedReq);
        MatchmakingRequestPacket typedReq = (MatchmakingRequestPacket) deserializedReq;
        assertEquals(PacketType.MATCHMAKING_REQUEST, typedReq.getType());
        assertEquals(MatchmakingMode.RANDOM, typedReq.getMode());
        assertEquals(PlayerRole.PLANT, typedReq.getPreferredRole());
        assertEquals("crazy_dave", typedReq.getUsername());

        // MatchmakingResponsePacket
        MatchmakingResponsePacket res = new MatchmakingResponsePacket(
                MatchmakingStatus.QUEUED, null, "Queued in matchmaking pool"
        );
        String resJson = NetworkJsonMapper.serialize(res);
        Packet deserializedRes = NetworkJsonMapper.deserialize(resJson);

        assertInstanceOf(MatchmakingResponsePacket.class, deserializedRes);
        MatchmakingResponsePacket typedRes = (MatchmakingResponsePacket) deserializedRes;
        assertEquals(PacketType.MATCHMAKING_RESPONSE, typedRes.getType());
        assertEquals(MatchmakingStatus.QUEUED, typedRes.getStatus());
        assertEquals("Queued in matchmaking pool", typedRes.getMessage());

        // MatchFoundPacket
        MatchFoundPacket matchFound = new MatchFoundPacket(
                "room_101", "dr_zomboss", PlayerRole.PLANT, 5
        );
        String matchFoundJson = NetworkJsonMapper.serialize(matchFound);
        Packet deserializedMatch = NetworkJsonMapper.deserialize(matchFoundJson);

        assertInstanceOf(MatchFoundPacket.class, deserializedMatch);
        MatchFoundPacket typedMatch = (MatchFoundPacket) deserializedMatch;
        assertEquals(PacketType.MATCH_FOUND, typedMatch.getType());
        assertEquals("room_101", typedMatch.getRoomId());
        assertEquals("dr_zomboss", typedMatch.getOpponentUsername());
        assertEquals(PlayerRole.PLANT, typedMatch.getAssignedRole());
        assertEquals(5, typedMatch.getCountdownSeconds());

        // CancelMatchmakingPacket
        CancelMatchmakingPacket cancel = new CancelMatchmakingPacket("crazy_dave", "room_101", "USER_REQUEST");
        String cancelJson = NetworkJsonMapper.serialize(cancel);
        Packet deserializedCancel = NetworkJsonMapper.deserialize(cancelJson);

        assertInstanceOf(CancelMatchmakingPacket.class, deserializedCancel);
        CancelMatchmakingPacket typedCancel = (CancelMatchmakingPacket) deserializedCancel;
        assertEquals(PacketType.CANCEL_MATCHMAKING, typedCancel.getType());
        assertEquals("crazy_dave", typedCancel.getUsername());
        assertEquals("room_101", typedCancel.getRoomCode());
        assertEquals("USER_REQUEST", typedCancel.getReason());
    }

    @Test
    @DisplayName("In-game action packets roundtrip serialization")
    void testInGameActionPackets() throws Exception {
        // PlacePlantRequestPacket
        PlacePlantRequestPacket plantReq = new PlacePlantRequestPacket("Peashooter", 2, 3, 2);
        String plantJson = NetworkJsonMapper.serialize(plantReq);
        Packet deserializedPlant = NetworkJsonMapper.deserialize(plantJson);

        assertInstanceOf(PlacePlantRequestPacket.class, deserializedPlant);
        PlacePlantRequestPacket typedPlant = (PlacePlantRequestPacket) deserializedPlant;
        assertEquals(PacketType.PLACE_PLANT_REQUEST, typedPlant.getType());
        assertEquals("Peashooter", typedPlant.getPlantName());
        assertEquals(2, typedPlant.getRow());
        assertEquals(3, typedPlant.getCol());
        assertEquals(2, typedPlant.getPlantLevel());

        // PlaceZombieRequestPacket
        PlaceZombieRequestPacket zombieReq = new PlaceZombieRequestPacket("ConeheadZombie", 1, 8);
        String zombieJson = NetworkJsonMapper.serialize(zombieReq);
        Packet deserializedZombie = NetworkJsonMapper.deserialize(zombieJson);

        assertInstanceOf(PlaceZombieRequestPacket.class, deserializedZombie);
        PlaceZombieRequestPacket typedZombie = (PlaceZombieRequestPacket) deserializedZombie;
        assertEquals(PacketType.PLACE_ZOMBIE_REQUEST, typedZombie.getType());
        assertEquals("ConeheadZombie", typedZombie.getZombieName());
        assertEquals(1, typedZombie.getRow());
        assertEquals(8, typedZombie.getCol());

        // PlayerActionResponsePacket
        PlayerActionResponsePacket actionRes = new PlayerActionResponsePacket(
                true, "PLACE_PLANT", "OK", 2, 3
        );
        String actionJson = NetworkJsonMapper.serialize(actionRes);
        Packet deserializedAction = NetworkJsonMapper.deserialize(actionJson);

        assertInstanceOf(PlayerActionResponsePacket.class, deserializedAction);
        PlayerActionResponsePacket typedAction = (PlayerActionResponsePacket) deserializedAction;
        assertEquals(PacketType.PLAYER_ACTION_RESPONSE, typedAction.getType());
        assertTrue(typedAction.isSuccess());
        assertEquals("PLACE_PLANT", typedAction.getActionType());
        assertEquals("OK", typedAction.getReason());
        assertEquals(2, typedAction.getRow());
        assertEquals(3, typedAction.getCol());
    }

    @Test
    @DisplayName("GameStateSnapshotPacket roundtrip serialization with plant, zombie, and projectile DTOs")
    void testGameStateSnapshotPacket() throws Exception {
        PlantSnapshotDto plant = new PlantSnapshotDto(
                "p-1", "Sunflower", 0, 1, 300, 300, "IDLE", false, false, 1
        );
        ZombieSnapshotDto zombie = new ZombieSnapshotDto(
                "z-1", "BasicZombie", 0, 7.5f, 0.0f, 200, 200, 0, "WALKING", 0.5f, false, false, false, false
        );
        ProjectileSnapshotDto proj = new ProjectileSnapshotDto(
                "proj-1", "PEA", 0, 2.5f, 0.0f, 4.0f, "NONE"
        );

        GameStateSnapshotPacket snapshot = new GameStateSnapshotPacket(
                120L, 6.0f, 114.0f, 150, 125,
                List.of(plant), List.of(zombie), List.of(proj),
                List.of(1), false, null, null
        );

        String json = NetworkJsonMapper.serialize(snapshot);
        Packet deserialized = NetworkJsonMapper.deserialize(json);

        assertInstanceOf(GameStateSnapshotPacket.class, deserialized);
        GameStateSnapshotPacket typed = (GameStateSnapshotPacket) deserialized;
        assertEquals(PacketType.GAME_STATE_SNAPSHOT, typed.getType());
        assertEquals(120L, typed.getTick());
        assertEquals(6.0f, typed.getMatchTime(), 0.001f);
        assertEquals(114.0f, typed.getTimeRemaining(), 0.001f);
        assertEquals(150, typed.getPlantSun());
        assertEquals(125, typed.getZombieSun());
        assertFalse(typed.isGameOver());

        assertEquals(1, typed.getPlants().size());
        assertEquals("Sunflower", typed.getPlants().get(0).getPlantName());
        assertEquals(1, typed.getZombies().size());
        assertEquals("BasicZombie", typed.getZombies().get(0).getZombieName());
        assertEquals(7.5f, typed.getZombies().get(0).getX(), 0.001f);
        assertEquals(1, typed.getProjectiles().size());
        assertEquals("PEA", typed.getProjectiles().get(0).getProjectileType());
        assertEquals(List.of(1), typed.getBreachedRows());
    }

    @Test
    @DisplayName("ReactionPacket roundtrip serialization")
    void testReactionPacket() throws Exception {
        ReactionPacket reaction = new ReactionPacket("crazy_dave", ReactionType.TAUNT, "BRAINS!");
        String json = NetworkJsonMapper.serialize(reaction);
        Packet deserialized = NetworkJsonMapper.deserialize(json);

        assertInstanceOf(ReactionPacket.class, deserialized);
        ReactionPacket typed = (ReactionPacket) deserialized;
        assertEquals(PacketType.REACTION, typed.getType());
        assertEquals("crazy_dave", typed.getSenderUsername());
        assertEquals(ReactionType.TAUNT, typed.getReactionType());
        assertEquals("BRAINS!", typed.getContent());
    }

    @Test
    @DisplayName("System packets (ErrorMessagePacket and HeartbeatPacket) roundtrip serialization")
    void testSystemPackets() throws Exception {
        ErrorMessagePacket err = new ErrorMessagePacket("UNAUTHORIZED", "Invalid credentials", "Bad token");
        String errJson = NetworkJsonMapper.serialize(err);
        Packet deserializedErr = NetworkJsonMapper.deserialize(errJson);

        assertInstanceOf(ErrorMessagePacket.class, deserializedErr);
        ErrorMessagePacket typedErr = (ErrorMessagePacket) deserializedErr;
        assertEquals(PacketType.ERROR_MESSAGE, typedErr.getType());
        assertEquals("UNAUTHORIZED", typedErr.getCode());
        assertEquals("Invalid credentials", typedErr.getMessage());
        assertEquals("Bad token", typedErr.getDetails());

        long now = System.currentTimeMillis();
        HeartbeatPacket hb = new HeartbeatPacket(now, now + 5, true);
        String hbJson = NetworkJsonMapper.serialize(hb);
        Packet deserializedHb = NetworkJsonMapper.deserialize(hbJson);

        assertInstanceOf(HeartbeatPacket.class, deserializedHb);
        HeartbeatPacket typedHb = (HeartbeatPacket) deserializedHb;
        assertEquals(PacketType.HEARTBEAT, typedHb.getType());
        assertEquals(now, typedHb.getClientTimestamp());
        assertEquals(now + 5, typedHb.getServerTimestamp());
        assertTrue(typedHb.isPong());
    }

    @Test
    @DisplayName("Polymorphic deserialization ignores unknown properties safely")
    void testUnknownPropertiesTolerance() throws Exception {
        String jsonWithExtraFields = "{\"type\":\"HEARTBEAT\",\"clientTimestamp\":1000,\"serverTimestamp\":1005,\"pong\":true,\"futureField\":\"futureValue\",\"debug\":999}";
        Packet packet = NetworkJsonMapper.deserialize(jsonWithExtraFields);

        assertInstanceOf(HeartbeatPacket.class, packet);
        HeartbeatPacket hb = (HeartbeatPacket) packet;
        assertEquals(1000L, hb.getClientTimestamp());
        assertEquals(1005L, hb.getServerTimestamp());
        assertTrue(hb.isPong());
    }

    @Test
    @DisplayName("Direct Invite Packets roundtrip serialization across all decisions and statuses")
    void testDirectInvitePackets() throws Exception {
        // 1. InviteRequestPacket
        model.network.packet.InviteRequestPacket req = new model.network.packet.InviteRequestPacket(
                "bob", PlayerRole.PLANT, "alice"
        );
        String reqJson = NetworkJsonMapper.serialize(req);
        Packet deserializedReq = NetworkJsonMapper.deserialize(reqJson);
        assertInstanceOf(model.network.packet.InviteRequestPacket.class, deserializedReq);
        model.network.packet.InviteRequestPacket typedReq = (model.network.packet.InviteRequestPacket) deserializedReq;
        assertEquals(PacketType.INVITE_REQUEST, typedReq.getType());
        assertEquals("bob", typedReq.getTargetUsername());
        assertEquals(PlayerRole.PLANT, typedReq.getPreferredRole());
        assertEquals("alice", typedReq.getInviterUsername());

        // 2. InviteReceivedPacket
        model.network.packet.InviteReceivedPacket received = new model.network.packet.InviteReceivedPacket(
                "inv-12345", "alice", PlayerRole.PLANT, 10
        );
        String recJson = NetworkJsonMapper.serialize(received);
        Packet deserializedRec = NetworkJsonMapper.deserialize(recJson);
        assertInstanceOf(model.network.packet.InviteReceivedPacket.class, deserializedRec);
        model.network.packet.InviteReceivedPacket typedRec = (model.network.packet.InviteReceivedPacket) deserializedRec;
        assertEquals(PacketType.INVITE_RECEIVED, typedRec.getType());
        assertEquals("inv-12345", typedRec.getInviteId());
        assertEquals("alice", typedRec.getInviterUsername());
        assertEquals(PlayerRole.PLANT, typedRec.getInviterRole());
        assertEquals(10, typedRec.getTimeoutSeconds());

        // 3. InviteResponsePacket (ACCEPT, DECLINE, TIMEOUT)
        model.network.packet.InviteResponsePacket resAccept = new model.network.packet.InviteResponsePacket(
                "inv-12345", "alice", model.network.enums.InviteDecision.ACCEPT, "Ready to play"
        );
        String resJson = NetworkJsonMapper.serialize(resAccept);
        Packet deserializedRes = NetworkJsonMapper.deserialize(resJson);
        assertInstanceOf(model.network.packet.InviteResponsePacket.class, deserializedRes);
        model.network.packet.InviteResponsePacket typedRes = (model.network.packet.InviteResponsePacket) deserializedRes;
        assertEquals(PacketType.INVITE_RESPONSE, typedRes.getType());
        assertEquals("inv-12345", typedRes.getInviteId());
        assertEquals("alice", typedRes.getInviterUsername());
        assertEquals(model.network.enums.InviteDecision.ACCEPT, typedRes.getDecision());
        assertEquals("Ready to play", typedRes.getReason());

        // 4. CancelInvitePacket
        model.network.packet.CancelInvitePacket cancel = new model.network.packet.CancelInvitePacket(
                "inv-12345", "bob"
        );
        String cancelJson = NetworkJsonMapper.serialize(cancel);
        Packet deserializedCancel = NetworkJsonMapper.deserialize(cancelJson);
        assertInstanceOf(model.network.packet.CancelInvitePacket.class, deserializedCancel);
        model.network.packet.CancelInvitePacket typedCancel = (model.network.packet.CancelInvitePacket) deserializedCancel;
        assertEquals(PacketType.CANCEL_INVITE, typedCancel.getType());
        assertEquals("inv-12345", typedCancel.getInviteId());
        assertEquals("bob", typedCancel.getTargetUsername());

        // 5. InviteStatusPacket
        model.network.packet.InviteStatusPacket status = new model.network.packet.InviteStatusPacket(
                "inv-12345", model.network.enums.InviteStatus.BUSY, "Target is in an active match"
        );
        String statusJson = NetworkJsonMapper.serialize(status);
        Packet deserializedStatus = NetworkJsonMapper.deserialize(statusJson);
        assertInstanceOf(model.network.packet.InviteStatusPacket.class, deserializedStatus);
        model.network.packet.InviteStatusPacket typedStatus = (model.network.packet.InviteStatusPacket) deserializedStatus;
        assertEquals(PacketType.INVITE_STATUS, typedStatus.getType());
        assertEquals("inv-12345", typedStatus.getInviteId());
        assertEquals(model.network.enums.InviteStatus.BUSY, typedStatus.getStatus());
        assertEquals("Target is in an active match", typedStatus.getMessage());
    }
}
