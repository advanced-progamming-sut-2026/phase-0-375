package model.network.packet.game;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class PlacePlantRequestPacket extends Packet {
    private String plantName;
    private int row;
    private int col;
    private int plantLevel;

    public PlacePlantRequestPacket() {
        super(PacketType.PLACE_PLANT_REQUEST);
    }

    public PlacePlantRequestPacket(String plantName, int row, int col) {
        this(plantName, row, col, 1);
    }

    public PlacePlantRequestPacket(String plantName, int row, int col, int plantLevel) {
        super(PacketType.PLACE_PLANT_REQUEST);
        this.plantName = plantName;
        this.row = row;
        this.col = col;
        this.plantLevel = plantLevel;
    }

    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }

    public int getPlantLevel() { return plantLevel; }
    public void setPlantLevel(int plantLevel) { this.plantLevel = plantLevel; }
}
