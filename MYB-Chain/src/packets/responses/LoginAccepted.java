package packets.responses;

import packets.Packet;
import packets.PacketTypes;

public class LoginAccepted extends Packet {
    private final String message;
    private final String newUserId;

    public LoginAccepted(String message, String userId) {
        super(PacketTypes.LOGIN_ACCEPTED);
        this.message = message;
        this.newUserId = userId;
    }

    public String getMessage() {
        return message;
    }

    public String getNewUserId() {
        return newUserId;
    }

    public PacketTypes getPacketType() {
        return packetType;
    }
}
