package packets.verifications;

import chain.Block;
import packets.Packet;
import packets.PacketTypes;

import java.math.BigInteger;
import java.net.InetAddress;

/**
 * Created by Michael on 4/18/2018.
 */
public class VerifyPacket extends Packet implements Comparable<VerifyPacket>{
    private final BigInteger chainLength;
    private final String ID;
    private final Block block;

    public VerifyPacket(BigInteger blockChainLength, String acceptorID, Block blockValue) {
        super(PacketTypes.SINGLE_VERIFICATION);
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

    public int compareTo(VerifyPacket otherPacket) {
        byte[] thisProofOfWork = block.getProofOfWork();
        byte[] otherProofOfWork = otherPacket.getBlock().getProofOfWork();

        for (int i = 0; i < thisProofOfWork.length; i++) {
            if (thisProofOfWork[i] != 0 && otherProofOfWork[i] != 0) {
                break;
            }else if (thisProofOfWork[i] == 0 && otherProofOfWork[i] != 0) {
                return 1;
            }else if (otherProofOfWork[i] == 0 && thisProofOfWork[i] != 0) {
                return -1;
            }
        }

        if (chainLength.compareTo(otherPacket.getChainLength()) != 0) {
            return chainLength.compareTo(otherPacket.getChainLength());
        }

        if (ID.compareTo(otherPacket.getID()) != 0) {
            return ID.compareTo(otherPacket.getID());
        }

        Integer hashCode = this.hashCode();
        Integer otherHashCode = otherPacket.hashCode();

        return hashCode.compareTo(otherHashCode);

    }
}
