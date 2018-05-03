package packets.acceptances;

import chain.User;
import packets.Packet;

import java.math.BigInteger;

/**
 * Created by Yingying Xia on 2018/5/3.
 */
public class SuccessfulUpdate extends Packet {
    private final String message;
    private final User user;


    /**
     * Packet for acceptors to send to the update manager
     * indicating that the user will be updated
     * user will then be notified by the update manager
     *
     * @param message
     */
    public SuccessfulUpdate(String message, User user){
        this.message = message;
        this.user = user;
    }
    public User getUser(){
        return this.user;
    }
}
