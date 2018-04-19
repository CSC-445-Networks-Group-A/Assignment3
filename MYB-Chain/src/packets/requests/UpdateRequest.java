package packets.requests;

import java.math.BigInteger;

/**
 * Created by Michael on 4/18/2018.
 */
public class UpdateRequest extends RequestPacket {
    private final BigInteger lastRecordedBlock;

    public UpdateRequest(BigInteger lastBlockRecorded) {
        this.lastRecordedBlock = lastBlockRecorded;
    }

    public BigInteger getLastRecordedBlock() {
        return lastRecordedBlock;
    }
}
