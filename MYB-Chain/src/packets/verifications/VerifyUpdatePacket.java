package packets.verifications;

import chain.Block;
import packets.Packet;

import javax.lang.model.type.ArrayType;
import java.net.InetAddress;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

/**
 * Created by Yingying Xia on 2018/5/6.
 */
public class VerifyUpdatePacket extends Packet implements Comparable<VerifyUpdatePacket>{
    private final RSAPublicKey publicKey;
    private final InetAddress userAddress;
    private final int userPort;

    private final int lastUpdatedBlockNumber;

    public VerifyUpdatePacket(RSAPublicKey publicKey, InetAddress userAddress, int userPort, int lastBlockNumber) {
        this.publicKey = publicKey;
        this.lastUpdatedBlockNumber = lastBlockNumber;
        this.userAddress = userAddress;
        this.userPort = userPort;
    }

    @Override
    public int compareTo(VerifyUpdatePacket verifyUpdatePacket) {
        //TODO:
        return 0;
    }

    public int getLastUpdatedBlockNumber(){
        return this.lastUpdatedBlockNumber;
    }
    public InetAddress getUserAddress(){
        return this.userAddress;
    }

    public int getUserPort(){
        return this.userPort;
    }
}
