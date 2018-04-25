package packets.requests;

import chain.User;
import packets.Packet;
import packets.PacketTypes;

import java.net.InetAddress;

/**
 * Created by Michael on 4/18/2018.
 */
public class LoginRequest extends Packet {
    private final User user;
    private final InetAddress originAddress;
    private final int originPortNumber;

    public LoginRequest(User user, InetAddress originAddress, int originPortNumber) {
        super(PacketTypes.LOGIN);
        this.user = user;
        this.originAddress = originAddress;
        this.originPortNumber = originPortNumber;
    }

    public User getUser() {
        return user;
    }

    public InetAddress getOriginAddress() {
        return originAddress;
    }

    public int getOriginPortNumber() {
        return originPortNumber;
    }


    public PacketTypes getPacketType() {
        return packetType;
    }

}
