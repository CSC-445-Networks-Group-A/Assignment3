package packets.responses;

import packets.Packet;
import packets.PacketTypes;

public class LoginDenied extends Packet {
    private final String denialMessage;

    public LoginDenied(String message) {
        super(PacketTypes.LOGIN_DENIED);
        this.denialMessage = message;
    }

    public String getDenialMessage() {
        return denialMessage;
    }

    public PacketTypes getPacketType() {
        return packetType;
    }
}
