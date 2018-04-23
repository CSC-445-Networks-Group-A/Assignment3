package packets.requests;

import packets.Packet;
import packets.PacketTypes;

/**
 * Created by Michael on 4/18/2018.
 */
public class LoginRequest extends Packet {
    private final String userId;

    public LoginRequest(String userId) {
        super(PacketTypes.LOGIN);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public PacketTypes getPacketType() {
        return packetType;
    }
    
}
