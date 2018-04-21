package packets.responses;

import packets.Packet;
import packets.PacketTypes;

/**
 * Created by Michael on 4/21/2018.
 */
public class TransactionApproved extends Packet {
    private final String acceptanceMessage;

    public TransactionApproved(String message) {
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
