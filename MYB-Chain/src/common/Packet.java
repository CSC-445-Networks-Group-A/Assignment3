package common;

/**
 * Created by Michael on 4/16/2018.
 */
public class Packet {
    public static final int ADD_USER = 1;
    public static final int GET_USER = 2;
    public static final int ADD_BLOCK = 3;
    public static final int ERROR = 4;


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
    public Packet() {

    }
}
