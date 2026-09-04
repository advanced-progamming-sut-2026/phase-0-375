package model.network.packet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
import model.network.packet.auth.LogoutRequestPacket;
import model.network.packet.auth.RegisterRequestPacket;
import model.network.packet.auth.RegisterResponsePacket;
import model.network.packet.auth.RegisterValidateRequestPacket;
import model.network.packet.auth.SessionResumeRequestPacket;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.game.CollectSunRequestPacket;
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
import model.network.packet.user.LeaderboardRequestPacket;
import model.network.packet.user.LeaderboardResponsePacket;
import model.network.packet.user.PasswordChangeRequestPacket;
import model.network.packet.user.PasswordChangeResponsePacket;
import model.network.packet.user.PasswordResetRequestPacket;
import model.network.packet.user.PasswordResetResponsePacket;
import model.network.packet.user.SecurityQuestionRequestPacket;
import model.network.packet.user.SecurityQuestionResponsePacket;
import model.network.packet.user.ProfileGetRequestPacket;
import model.network.packet.user.ProfileGetResponsePacket;
import model.network.packet.user.ProfileUpdateRequestPacket;
import model.network.packet.user.ProfileUpdateResponsePacket;
import model.network.packet.user.UserCommandRequestPacket;
import model.network.packet.user.UserCommandResponsePacket;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RegisterRequestPacket.class, name = "REGISTER_REQUEST"),
    @JsonSubTypes.Type(value = RegisterValidateRequestPacket.class, name = "REGISTER_VALIDATE_REQUEST"),
    @JsonSubTypes.Type(value = RegisterResponsePacket.class, name = "REGISTER_RESPONSE"),
    @JsonSubTypes.Type(value = LoginRequestPacket.class, name = "LOGIN_REQUEST"),
    @JsonSubTypes.Type(value = LoginResponsePacket.class, name = "LOGIN_RESPONSE"),
    @JsonSubTypes.Type(value = LogoutRequestPacket.class, name = "LOGOUT_REQUEST"),
    @JsonSubTypes.Type(value = SessionResumeRequestPacket.class, name = "SESSION_RESUME_REQUEST"),

    @JsonSubTypes.Type(value = ProfileGetRequestPacket.class, name = "PROFILE_GET_REQUEST"),
    @JsonSubTypes.Type(value = ProfileGetResponsePacket.class, name = "PROFILE_GET_RESPONSE"),
    @JsonSubTypes.Type(value = ProfileUpdateRequestPacket.class, name = "PROFILE_UPDATE_REQUEST"),
    @JsonSubTypes.Type(value = ProfileUpdateResponsePacket.class, name = "PROFILE_UPDATE_RESPONSE"),
    @JsonSubTypes.Type(value = PasswordChangeRequestPacket.class, name = "PASSWORD_CHANGE_REQUEST"),
    @JsonSubTypes.Type(value = PasswordChangeResponsePacket.class, name = "PASSWORD_CHANGE_RESPONSE"),
    @JsonSubTypes.Type(value = PasswordResetRequestPacket.class, name = "PASSWORD_RESET_REQUEST"),
    @JsonSubTypes.Type(value = PasswordResetResponsePacket.class, name = "PASSWORD_RESET_RESPONSE"),
    @JsonSubTypes.Type(value = SecurityQuestionRequestPacket.class, name = "SECURITY_QUESTION_REQUEST"),
    @JsonSubTypes.Type(value = SecurityQuestionResponsePacket.class, name = "SECURITY_QUESTION_RESPONSE"),

    @JsonSubTypes.Type(value = UserCommandRequestPacket.class, name = "USER_COMMAND_REQUEST"),
    @JsonSubTypes.Type(value = UserCommandResponsePacket.class, name = "USER_COMMAND_RESPONSE"),

    @JsonSubTypes.Type(value = LeaderboardRequestPacket.class, name = "LEADERBOARD_REQUEST"),
    @JsonSubTypes.Type(value = LeaderboardResponsePacket.class, name = "LEADERBOARD_RESPONSE"),

    @JsonSubTypes.Type(value = MatchmakingRequestPacket.class, name = "MATCHMAKING_REQUEST"),
    @JsonSubTypes.Type(value = MatchmakingResponsePacket.class, name = "MATCHMAKING_RESPONSE"),
    @JsonSubTypes.Type(value = MatchFoundPacket.class, name = "MATCH_FOUND"),
    @JsonSubTypes.Type(value = CancelMatchmakingPacket.class, name = "CANCEL_MATCHMAKING"),

    @JsonSubTypes.Type(value = InviteRequestPacket.class, name = "INVITE_REQUEST"),
    @JsonSubTypes.Type(value = InviteReceivedPacket.class, name = "INVITE_RECEIVED"),
    @JsonSubTypes.Type(value = InviteResponsePacket.class, name = "INVITE_RESPONSE"),
    @JsonSubTypes.Type(value = CancelInvitePacket.class, name = "CANCEL_INVITE"),
    @JsonSubTypes.Type(value = InviteStatusPacket.class, name = "INVITE_STATUS"),

    @JsonSubTypes.Type(value = PlacePlantRequestPacket.class, name = "PLACE_PLANT_REQUEST"),
    @JsonSubTypes.Type(value = PlaceZombieRequestPacket.class, name = "PLACE_ZOMBIE_REQUEST"),
    @JsonSubTypes.Type(value = CollectSunRequestPacket.class, name = "COLLECT_SUN_REQUEST"),
    @JsonSubTypes.Type(value = PlayerActionResponsePacket.class, name = "PLAYER_ACTION_RESPONSE"),

    @JsonSubTypes.Type(value = GameStateSnapshotPacket.class, name = "GAME_STATE_SNAPSHOT"),

    @JsonSubTypes.Type(value = ReactionPacket.class, name = "REACTION"),

    @JsonSubTypes.Type(value = ErrorMessagePacket.class, name = "ERROR_MESSAGE"),
    @JsonSubTypes.Type(value = HeartbeatPacket.class, name = "HEARTBEAT")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Packet {
    @JsonProperty("type")
    private PacketType type;

    protected Packet() {
    }

    protected Packet(PacketType type) {
        this.type = type;
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }
}
