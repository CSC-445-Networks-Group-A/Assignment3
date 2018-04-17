package common;

/**
 * Created by Michael on 4/16/2018.
 */
public class Packet {
    public static final int ADD_USER = 1;
    public static final int GET_USER = 2;
    public static final int ADD_BLOCK = 3;
    public static final int ERROR = 4;
    byte[] dataBytes;

    //TODO: error messages ...
    public static final String[] errorMessages = { "No such user."};

    /**
     * MYB-Chain Packet Formats
     *
     * Type   Op #     Format without header
     *
     *                          Byte Allocation
     *            -----------------------------------------------
     * ADD_USER   |            Byte Structuring                 |
     *            -----------------------------------------------
     *                         Byte Allocation
     *            -----------------------------------------------
     * GET_USER   |            Byte Structuring                 |
     *            -----------------------------------------------
     *                         Byte Allocation
     *            -----------------------------------------------
     * ADD_BLOCK  |            Byte Structuring                 |
     *            -----------------------------------------------
     *                         Byte Allocation
     *            -----------------------------------------------
     * ERROR      |            Byte Structuring                 |
     *            -----------------------------------------------
     * */


    //TODO: missing params
    public Packet addUserPacket(){
        Packet packet=  new Packet();
        // TODO: Byte structuring
        return packet;
    }
    //TODO: missing params
    public Packet getUserPacket(){
        Packet packet=  new Packet();
        // TODO: Byte structuring
        return packet;
    }
    //TODO: missing params
    public Packet addBlockPacket(){
        Packet packet=  new Packet();
        // TODO: Byte structuring
        return packet;
    }
    //TODO: missing params
    public Packet errorPacket(int errorNumber){
        Packet packet=  new Packet();

        // TODO: Byte structuring
        return packet;
    }

}
