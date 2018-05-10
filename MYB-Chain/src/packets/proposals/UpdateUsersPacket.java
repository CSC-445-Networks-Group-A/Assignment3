package packets.proposals;

/**
 * Created by Michael on 4/22/2018.
 */

import packets.Packet;
import packets.PacketTypes;

import java.math.BigInteger;
import java.net.InetAddress;

/**
 * proposer to acceptors
 */
public class UpdateUsersPacket extends Packet {
    private final BigInteger lastBlockRecorded;
    private final InetAddress userAddress;
    private final int userPort;
    public UpdateUsersPacket(BigInteger lastBlockRecorded, InetAddress userAddress, int userPort) {
        super(PacketTypes.UPDATE_REQUEST);
        this.lastBlockRecorded = lastBlockRecorded;
        this.userAddress = userAddress;
        this.userPort = userPort;
    }

    public BigInteger getLastBlockRecorded(){
        return this.lastBlockRecorded;
    }
    public InetAddress getUserAddress(){
        return this.userAddress;
    }

    public int getUserPort(){
        return this.userPort;
    }
}
