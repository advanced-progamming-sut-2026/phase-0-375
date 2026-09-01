package model.network.packet.user;

import model.network.enums.UserCommand;
import model.network.packet.Packet;
import model.network.packet.PacketType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserCommandRequestPacket extends Packet {
    private String clientRequestId;
    private UserCommand command;
    private Map<String, String> args = new HashMap<>();

    public UserCommandRequestPacket() {
        super(PacketType.USER_COMMAND_REQUEST);
    }

    public UserCommandRequestPacket(UserCommand command, Map<String, String> args) {
        super(PacketType.USER_COMMAND_REQUEST);
        this.clientRequestId = UUID.randomUUID().toString();
        this.command = command;
        if (args != null) {
            this.args = new HashMap<>(args);
        }
    }

    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }
    public UserCommand getCommand() { return command; }
    public void setCommand(UserCommand command) { this.command = command; }
    public Map<String, String> getArgs() { return args; }
    public void setArgs(Map<String, String> args) { this.args = args != null ? args : new HashMap<>(); }

    public String arg(String key) {
        return args != null ? args.get(key) : null;
    }

    public int argInt(String key, int defaultValue) {
        String v = arg(key);
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long argLong(String key, long defaultValue) {
        String v = arg(key);
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean argBool(String key, boolean defaultValue) {
        String v = arg(key);
        if (v == null || v.isBlank()) return defaultValue;
        return Boolean.parseBoolean(v.trim());
    }

    public float argFloat(String key, float defaultValue) {
        String v = arg(key);
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return Float.parseFloat(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
