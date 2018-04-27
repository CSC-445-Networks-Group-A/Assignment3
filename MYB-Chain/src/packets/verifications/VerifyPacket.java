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
    private final String ID;
    private final Block block;

    public VerifyPacket(BigInteger blockChainLength, String acceptorID, Block blockValue) {
        super(PacketTypes.VERIFICATION);
        this.chainLength = blockChainLength;
        this.ID = acceptorID;
        this.block = blockValue;
    }

    public BigInteger getChainLength() {
        return chainLength;
    }

    public String getID() {
        return ID;
    }

    public Block getBlock() {
        return block;
    }
}
