package packets.verifications;

import javafx.util.Pair;
import packets.Packet;
import packets.PacketTypes;

import java.net.InetAddress;
import java.util.HashMap;

/**
 * Created by Michael on 4/29/2018.
 */
public class VerifyAllPacket extends Packet {
    private final String ID;
    private final String[] keys;
    private final VerifyPacket[] values;


    public VerifyAllPacket(String acceptorID, HashMap<String, VerifyPacket> acceptorKnowledge) {
        super(PacketTypes.MULTIPLE_VERIFICATION);
        this.ID = acceptorID;

        String[] tempKeys = new String[acceptorKnowledge.size()];
        VerifyPacket[] tempValues = new VerifyPacket[acceptorKnowledge.size()];
        int count = 0;

        for (String key : acceptorKnowledge.keySet()) {
            tempKeys[count] = key;
            tempValues[count] = acceptorKnowledge.get(key);
            count++;
        }

        this.keys = tempKeys;
        this.values = tempValues;

    }

    public String getID() {
        return ID;
    }

    public String[] getKeys() {
        return keys;
    }

    public VerifyPacket[] getValues() {
        return values;
    }

    public HashMap<String, VerifyPacket> getAcceptorKnowledge() {
        HashMap<String, VerifyPacket> acceptorKnowledge = new HashMap<>(keys.length);
        for (int i = 0; i < keys.length; i++) {
            acceptorKnowledge.put(keys[i], values[i]);
        }
        return acceptorKnowledge;
    }
}
