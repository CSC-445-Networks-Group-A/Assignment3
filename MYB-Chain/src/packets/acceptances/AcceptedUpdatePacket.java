package packets.acceptances;

import chain.Block;
import packets.Packet;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

/**
 * Created by Yingying Xia on 2018/5/6.
 */
public class AcceptedUpdatePacket extends Packet implements Comparable<AcceptedPacket> {

    private final ArrayList<Block> blocksToUpdate;
    private final int lastUpdatedBlockNumber;

    public AcceptedUpdatePacket(ArrayList<Block> blocks, int lastNumber){

        this.lastUpdatedBlockNumber = lastNumber;
        this.blocksToUpdate = blocks;
    }

    @Override
    public int compareTo(AcceptedPacket acceptedPacket) {
        return 0;
    }
}
