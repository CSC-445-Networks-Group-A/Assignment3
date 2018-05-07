package packets.proposals;

/**
 * Created by Michael on 4/22/2018.
 */

import packets.Packet;

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
        this.lastBlockRecorded = lastBlockRecorded;
        this.userAddress = userAddress;
        this.userPort = userPort;
    }

    public BigInteger getLastBlockRecorded(){
        return this.lastBlockRecorded;
    }
    public InetAddress getUserAddress(){
        return this.getUserAddress();
    }

    public int getUserPort(){
        return this.userPort;
    }
}
