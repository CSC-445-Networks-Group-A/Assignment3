package packets.responses;

import packets.Packet;
import packets.PacketTypes;

import java.io.Serializable;

/**
 * Created by Michael on 4/21/2018.
 */
public class TransactionDenied extends Packet {
    private final String denialMessage;

    public TransactionDenied(String message) {
        super(PacketTypes.TRANSACTION_DENIED);
        this.denialMessage = message;
    }

    public String getDenialMessage() {
        return denialMessage;
    }

    public PacketTypes getPacketType() {
        return packetType;
    }
}
