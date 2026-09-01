package model.network.packet.game;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class PlayerActionResponsePacket extends Packet {
    private boolean success;
    private String actionType;           // "PLACE_PLANT" or "PLACE_ZOMBIE"
    private String reason;               // "OK", "INSUFFICIENT_SUN", "INVALID_COLUMN", "CELL_OCCUPIED", etc.
    private int row;
    private int col;

    public PlayerActionResponsePacket() {
        super(PacketType.PLAYER_ACTION_RESPONSE);
    }

    public PlayerActionResponsePacket(boolean success, String actionType, String reason) {
        this(success, actionType, reason, -1, -1);
    }

    public PlayerActionResponsePacket(boolean success, String actionType, String reason, int row, int col) {
        super(PacketType.PLAYER_ACTION_RESPONSE);
        this.success = success;
        this.actionType = actionType;
        this.reason = reason;
        this.row = row;
        this.col = col;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }
}
