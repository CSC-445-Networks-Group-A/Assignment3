package packets.responses;


import packets.Packet;
import packets.PacketTypes;

/**
 * Created by Yingying Xia on 2018/5/2.
 */
public class GeneralResponse extends Packet {
    private final String generalMessage;

    /**
     * Just a general purpose packet for containing trivial messages
     * @param message
     */
    public GeneralResponse(String message){
        super(PacketTypes.GENERAL);
        this.generalMessage = message;
    }
}
