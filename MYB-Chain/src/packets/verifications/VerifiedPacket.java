package packets.verifications;

import chain.Block;

import java.math.BigInteger;

/**
 * Created by Michael on 4/18/2018.
 */
public class VerifiedPacket {
    private final BigInteger chainLength;
    private final Block block;

    public VerifiedPacket(BigInteger blockChainLength, Block blockValue) {
        this.chainLength = blockChainLength;
        this.block = blockValue;
    }

    public BigInteger getChainLength() {
        return chainLength;
    }

    public Block getBlock() {
        return block;
    }
}
