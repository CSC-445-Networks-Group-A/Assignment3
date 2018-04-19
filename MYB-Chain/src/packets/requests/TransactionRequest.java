package packets.requests;

import chain.Transaction;
import packets.Packet;
import packets.PacketTypes;

/**
 * Created by Michael on 4/18/2018.
 */
public class TransactionRequest extends Packet{
    private final Transaction transaction;

    public TransactionRequest(Transaction requestedTransaction) {
        super(PacketTypes.TRANSACTION_REQUEST);
        this.transaction = requestedTransaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public PacketTypes getPacketType() {
        return packetType;
    }
}
