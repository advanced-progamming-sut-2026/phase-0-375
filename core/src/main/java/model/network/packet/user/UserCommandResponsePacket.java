package model.network.packet.user;

import model.network.packet.Packet;
import model.network.packet.PacketType;
import model.user.User;

public class UserCommandResponsePacket extends Packet {
    private String clientRequestId;
    private boolean success;
    private String errorCode;
    private String message;
    private User user;

    public UserCommandResponsePacket() {
        super(PacketType.USER_COMMAND_RESPONSE);
    }

    public UserCommandResponsePacket(
            String clientRequestId, boolean success, String errorCode, String message, User user) {
        super(PacketType.USER_COMMAND_RESPONSE);
        this.clientRequestId = clientRequestId;
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.user = user;
    }

    public static UserCommandResponsePacket ok(String requestId, String message, User user) {
        return new UserCommandResponsePacket(requestId, true, null, message, user);
    }

    public static UserCommandResponsePacket fail(String requestId, String errorCode, String message) {
        return new UserCommandResponsePacket(requestId, false, errorCode, message, null);
    }

    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
