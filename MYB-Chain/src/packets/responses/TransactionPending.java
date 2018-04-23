package packets.responses;

import packets.Packet;
import packets.PacketTypes;

/**
 * Created by Michael on 4/21/2018.
 */
public class TransactionPending extends Packet {
    private final String pendingMessage;

    public TransactionPending(String message) {
        super(PacketTypes.TRANSACTION_PENDING);
        this.pendingMessage = message;
    }

    public String getPendingMessage() {
        return pendingMessage;
    }

    public PacketTypes getPacketType() {
        return packetType;
    }
}
