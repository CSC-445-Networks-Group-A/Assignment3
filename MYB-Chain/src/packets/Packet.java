package packets;

import java.io.Serializable;

/**
 * Created by Michael on 4/16/2018.
 */
public class Packet implements Serializable{
    protected final PacketTypes packetType;

    public Packet() {
        this.packetType = PacketTypes.ERROR;
    }

    public Packet(PacketTypes type) {
        this.packetType = type;
    }

    public PacketTypes getPacketType() {
        return packetType;
    }

}
