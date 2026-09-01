package model.network.packet.system;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class ErrorMessagePacket extends Packet {
    private String code;                 // "UNAUTHORIZED", "INVALID_PACKET", "ROOM_NOT_FOUND", "INTERNAL_ERROR"
    private String message;              // Human readable explanation
    private String details;              // Optional diagnostic info / trace

    public ErrorMessagePacket() {
        super(PacketType.ERROR_MESSAGE);
    }

    public ErrorMessagePacket(String code, String message) {
        this(code, message, null);
    }

    public ErrorMessagePacket(String code, String message, String details) {
        super(PacketType.ERROR_MESSAGE);
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
