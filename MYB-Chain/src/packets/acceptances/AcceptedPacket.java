package packets.acceptances;

import chain.Block;
import packets.Packet;
import packets.PacketTypes;

import java.math.BigInteger;

/**
 * Created by Michael on 4/21/2018.
 */
public class AcceptedPacket extends Packet {
    private final BigInteger chainLength;
    private final Block block;

    public AcceptedPacket(BigInteger blockChainLength, Block blockValue) {
        super(PacketTypes.ACCEPTACNE);
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
