package packets.proposals;

import chain.Block;

import java.math.BigInteger;

/**
 * Created by Michael on 4/18/2018.
 */
public class ProposalPacket {
    private final BigInteger chainLength;
    private final String proposerID;
    private final Block block;

    public ProposalPacket(BigInteger blockChainLength, String ID, Block blockValue) {
        this.chainLength = blockChainLength;
        this.proposerID = ID;
        this.block = blockValue;
    }

    public BigInteger getChainLength() {
        return chainLength;
    }

    public String getProposerID() {
        return proposerID;
    }

    public Block getBlock() {
        return block;
    }
}
