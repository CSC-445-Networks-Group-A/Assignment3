package packets.proposals;

import chain.Block;
import packets.Packet;
import packets.PacketTypes;

import java.math.BigInteger;

/**
 * Created by Michael on 4/18/2018.
 */
public class ProposalPacket extends Packet{
    private final BigInteger chainLength;
    private final String ID;
    private final Block block;

    public ProposalPacket(BigInteger blockChainLength, String proposerID, Block blockValue) {
        super(PacketTypes.PROPOSAL);
        this.chainLength = blockChainLength;
        this.ID = proposerID;
        this.block = blockValue;
    }

    public BigInteger getChainLength() {
        return chainLength;
    }

    public String getProposerID() {
        return ID;
    }

    public Block getBlock() {
        return block;
    }
}
