package proposers;

import chain.Block;
import chain.Transaction;
import chain.User;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Michael on 4/15/2018.
 */
public class Miner extends Thread{
    private static final String PROPOSER_TO_CLIENT_ADDRESS = "230.0.0.0";
    private static final String PROPOSER_TO_ACCEPTOR_ADDRESS = "230.0.0.1";
    private static final int PORT = 2690;
    private final User miner;
    private DatagramSocket datagramSocket;
    private ConcurrentLinkedQueue<Transaction> pendingTransactions;
    private Block block;

    public Miner(User mybChainMiner) throws SocketException {
        super("Miner: " + mybChainMiner.getID());
        this.miner = mybChainMiner;
        this.datagramSocket = new DatagramSocket(PORT);
        this.pendingTransactions = new ConcurrentLinkedQueue<>();
        this.block = new Block(miner.getBlockChain().getMostRecentHash());
    }

    @Override
    public void run() {

    }

    /**
     *
     * */
    private void mine() {

    }


    /**
     *
     * */
    private void listenToClients() {

    }

    /**
     * Checks all Transactions on the Ledger, verifying that each chain.Transaction's Signature is capable of being decrypted
     * by the associated "buyer".
     * */
    private void verifyLedger() {

    }


    private void propose() {

    }
}
