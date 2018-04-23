package packets.requests;

import chain.Transaction;
import packets.Packet;
import packets.PacketTypes;

import java.net.InetAddress;

/**
 * Created by Michael on 4/18/2018.
 */
public class TransactionRequest extends Packet{
    private final Transaction transaction;
    private final InetAddress returnAddress;
    private final int returnPort;

    public TransactionRequest(Transaction requestedTransaction, InetAddress returnIPAddress, int returnPortNumber) {
        super(PacketTypes.TRANSACTION_REQUEST);
        this.transaction = requestedTransaction;
        this.returnAddress = returnIPAddress;
        this.returnPort = returnPortNumber;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public InetAddress getReturnAddress() {
        return returnAddress;
    }

    public int getReturnPort() {
        return returnPort;
    }

    public PacketTypes getPacketType() {
        return packetType;
    }
}
