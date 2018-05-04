package packets.requests;

import chain.User;
import packets.Packet;

import java.math.BigInteger;
import java.net.InetAddress;

/**
 * Created by Michael on 4/18/2018.
 */

/**
 * client to proposer
 * TODO: pass in address and port instead
 */
public class UpdateRequest extends Packet {
    private final BigInteger lastRecordedBlock;
    private final InetAddress userAddress;
    private final int userPort;

    //TODO: we might no long need this ?
    /* public UpdateRequest(BigInteger lastBlockRecorded) {
        this.lastRecordedBlock = lastBlockRecorded;
    }*/
    public UpdateRequest(BigInteger lastBlockRecorded, InetAddress userAddress, int userPort) {
        this.lastRecordedBlock = lastBlockRecorded;
        this.userAddress = userAddress;
        this.userPort = userPort;
    }

    public BigInteger getLastRecordedBlock() {
        return lastRecordedBlock;
    }

    public int getUserPort(){
        return userPort;
    }
    public InetAddress getUserAddress(){
        return userAddress;
    }

}
