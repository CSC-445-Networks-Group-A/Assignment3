package packets.verifications;

import packets.Packet;
import packets.PacketTypes;

import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;

/**
 * Created by Michael on 4/29/2018.
 */
public class VerifyAllPacket extends Packet {
    private final RSAPublicKey publicKey;
    private final RSAPublicKey[] keys;
    private final VerifyPacket[] values;


    public VerifyAllPacket(RSAPublicKey rsaPublicKey, HashMap<RSAPublicKey, VerifyPacket> acceptorKnowledge) {
        super(PacketTypes.MULTIPLE_VERIFICATION);
        this.publicKey = rsaPublicKey;

        RSAPublicKey[] tempKeys = new RSAPublicKey[acceptorKnowledge.size()];
        VerifyPacket[] tempValues = new VerifyPacket[acceptorKnowledge.size()];
        int count = 0;

        for (RSAPublicKey key : acceptorKnowledge.keySet()) {
            tempKeys[count] = key;
            tempValues[count] = acceptorKnowledge.get(key);
            count++;
        }

        this.keys = tempKeys;
        this.values = tempValues;

    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPublicKey[] getKeys() {
        return keys;
    }

    public VerifyPacket[] getValues() {
        return values;
    }

    public HashMap<RSAPublicKey, VerifyPacket> getAcceptorKnowledge() {
        HashMap<RSAPublicKey, VerifyPacket> acceptorKnowledge = new HashMap<>(keys.length);
        for (int i = 0; i < keys.length; i++) {
            acceptorKnowledge.put(keys[i], values[i]);
        }
        return acceptorKnowledge;
    }
}
