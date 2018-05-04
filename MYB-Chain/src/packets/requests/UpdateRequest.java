package packets.requests;

import chain.User;
import packets.Packet;

import java.math.BigInteger;

/**
 * Created by Michael on 4/18/2018.
 */
public class UpdateRequest extends Packet {
    private final BigInteger lastRecordedBlock;
    private final User user;

    //TODO: we might no long need this?
  /*  public UpdateRequest(BigInteger lastBlockRecorded) {
        this.lastRecordedBlock = lastBlockRecorded;
    }*/
    public UpdateRequest(BigInteger lastBlockRecorded, User user){
        this.lastRecordedBlock = lastBlockRecorded;
        this.user = user;
    }

    public BigInteger getLastRecordedBlock() {
        return lastRecordedBlock;
    }
    public User getUser(){
        return this.user;
    }
}
