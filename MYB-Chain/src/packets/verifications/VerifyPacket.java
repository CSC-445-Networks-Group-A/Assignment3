package packets.verifications;

import chain.Block;
import packets.Packet;
import packets.PacketTypes;

import java.math.BigInteger;

/**
 * Created by Michael on 4/18/2018.
 */
public class VerifyPacket extends Packet {
    private final BigInteger chainLength;
    private final Block block;

    public VerifyPacket(BigInteger blockChainLength, Block blockValue) {
        super(PacketTypes.VERIFICATION);
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
