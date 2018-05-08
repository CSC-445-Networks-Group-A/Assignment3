package packets.acceptances;

import chain.Block;
import packets.Packet;
import packets.PacketTypes;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

/**
 * Created by Yingying Xia on 2018/5/6.
 */
public class AcceptedUpdatePacket extends Packet {
    private final Block[] blocksToUpdate;
    private final InetAddress address;
    private final int port;

    public AcceptedUpdatePacket(Block[] blocks, InetAddress address, int portNumber){
        super(PacketTypes.UPDATE_ACCEPTANCE);
        this.blocksToUpdate = blocks;
        this.address = address;
        this.port = portNumber;
    }


    public Block[] getBlocksToUpdate() {
        return blocksToUpdate;
    }

    public InetAddress getUserAddress() {
        return address;
    }

    public int getUserPort() {
        return port;
    }
}
