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
    private final InetAddress address;
    private final int port;
    private final String ID;
    private final Block block;

    public VerifyPacket(BigInteger blockChainLength, InetAddress acceptorAddress, int acceptorPort, String acceptorID, Block blockValue) {
        super(PacketTypes.VERIFICATION);
        this.chainLength = blockChainLength;
        this.address = acceptorAddress;
        this.port = acceptorPort;
        this.ID = acceptorID;
        this.block = blockValue;
    }

    public BigInteger getChainLength() {
        return chainLength;
    }

    public InetAddress getAddress() {
        return address;
    }

    public Integer getPort() {
        return port;
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

        if (getPort().compareTo(otherPacket.getPort()) != 0) {
            return getPort().compareTo(otherPacket.getPort());
        }

        Integer addressHashCode = address.hashCode();
        Integer otherAddressHashCode = otherPacket.getAddress().hashCode();

        if (addressHashCode.compareTo(otherAddressHashCode) != 0) {
            return addressHashCode.compareTo(otherAddressHashCode);
        }

        Integer hashCode = this.hashCode();
        Integer otherHashCode = otherPacket.hashCode();

        return hashCode.compareTo(otherHashCode);

    }
}
