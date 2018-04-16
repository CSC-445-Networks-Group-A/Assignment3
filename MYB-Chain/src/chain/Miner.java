package chain;

/**
 * Created by Michael on 4/15/2018.
 */
public class Miner {
    private static final int NECESSARY_LEADING_ZEROS = 10;
    private final User miner;

    public Miner(User mybChainMiner) {
        this.miner = mybChainMiner;
    }


    /**
     *
     * */
    private void mine() {

    }


    /**
     * Checks all Transactions on the Ledger, verifying that each chain.Transaction's Signature is capable of being decrypted
     * by the associated "buyer".
     * */
    private void verifyLedger() {

    }
}
