package packets.acceptances;

import chain.Block;
import packets.Packet;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

/**
 * Created by Yingying Xia on 2018/5/6.
 */
public class AcceptedUpdatePacket extends Packet implements Comparable<AcceptedUpdatePacket> {


    private final int lastUpdatedBlockNumber;

    public AcceptedUpdatePacket(int lastNumber){

        this.lastUpdatedBlockNumber = lastNumber;

    }

    public int getLastUpdatedBlockNumber(){
        return this.lastUpdatedBlockNumber;
    }

    @Override
    public int compareTo(AcceptedUpdatePacket acceptedPacket) {
        if(this.lastUpdatedBlockNumber == acceptedPacket.getLastUpdatedBlockNumber()){
            return 0;
        }else if (this.lastUpdatedBlockNumber < acceptedPacket.lastUpdatedBlockNumber){
            return -1;
        }else{
            return 1;
        }
    }
}
