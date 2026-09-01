package model.network.packet.user;

import model.network.packet.Packet;
import model.network.packet.PacketType;

public class ProfileGetRequestPacket extends Packet {
    public ProfileGetRequestPacket() {
        super(PacketType.PROFILE_GET_REQUEST);
    }
}
