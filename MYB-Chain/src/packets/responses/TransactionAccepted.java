package packets.responses;

import packets.Packet;
import packets.PacketTypes;

/**
 * Created by Michael on 4/22/2018.
 */
public class TransactionAccepted extends Packet{
    private final String acceptanceMessage;

    public TransactionAccepted(String message) {
        super(PacketTypes.TRANSACTION_ACCEPTED);
        this.acceptanceMessage = message;
    }

    public String getAcceptanceMessage() {
        return acceptanceMessage;
    }

    public PacketTypes getPacketType() {
        return packetType;
    }
}
