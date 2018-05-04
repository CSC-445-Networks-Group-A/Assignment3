package packets.acceptances;

import chain.User;
import packets.Packet;

import java.math.BigInteger;
import java.net.InetAddress;

/**
 * Created by Yingying Xia on 2018/5/3.
 */

/**
 * fixme: do not take user objects
 */
public class SuccessfulUpdate extends Packet {
    private final String message;
    private final InetAddress updateAddress;
    private final int updatePort;


    /**
     * Packet for acceptors to send to the update manager/proposers
     * indicating that the user will be updated
     * user will then be notified by the update manager
     *
     * @param message
     */
    public SuccessfulUpdate(String message,InetAddress updateAddress, int updatePort){
        this.message = message;
        this.updateAddress = updateAddress;
        this.updatePort = updatePort;
    }
    public InetAddress getUserAddress(){
        return this.updateAddress;
    }

    public int getUserPort(){
        return this.updatePort;
    }
}
