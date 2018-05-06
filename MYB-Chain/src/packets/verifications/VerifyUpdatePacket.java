package packets.verifications;

import chain.Block;
import packets.Packet;

import javax.lang.model.type.ArrayType;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

/**
 * Created by Yingying Xia on 2018/5/6.
 */
public class VerifyUpdatePacket extends Packet implements Comparable<VerifyPacket>{
    private final RSAPublicKey publicKey;
    private final ArrayList<Block> blocksToUpdate;
    private final int lastUpdatedBlockNumber;

    public VerifyUpdatePacket(RSAPublicKey publicKey, ArrayList<Block> blocksToUpdate, int lastBlockNumber) {
        this.publicKey = publicKey;
        this.blocksToUpdate = blocksToUpdate;
        this.lastUpdatedBlockNumber = lastBlockNumber;
    }

    @Override
    public int compareTo(VerifyPacket verifyPacket) {
        return 0;
    }
    public ArrayList<Block> getBlocksToUpdate (){
        return blocksToUpdate;
    }
    public int getLastUpdatedBlockNumber(){
        return this.lastUpdatedBlockNumber;
    }
}
