package acceptors;

import chain.Block;
import chain.Transaction;
import chain.User;
import packets.proposals.ProposalPacket;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Michael on 4/18/2018.
 */
public class ChainChecker extends Thread{
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int COLLISION_PREVENTING_TIMEOUT_TIME = 5000;
    private final static int MIN_COLLISION_PREVENTING_TIMEOUT_TIME = 500;
    private final static int TTL = 12;
    private final static int N = 10;
    private final static int f = (N-1)/3;
    private final User checker;
    private final InetAddress proposalAddress;
    private final InetAddress acceptanceAddress;
    private final InetAddress learnAddress;
    private final int proposalPort;
    private final int acceptancePort;
    private final int learnPort;
    private HashMap<String, Integer> chainCheckers;

    /**
     * Note that for N ChainCheckers, Byzantine Paxos can handle f faulty or malicious Acceptors where
     *
     * f = (N-1)/3
     *
     * The protocol requires consensus of 2f + 1 Acceptors.
     *
     * */
    public ChainChecker(User mybChainChecker, int proposalPortNumber, String addressToProposeOn, int acceptancePortNumber,
                        String addressToAcceptOn, int learningPortNumber, String addressToLearnOn,
                        int intialNumberOfCheckers) throws UnknownHostException {
        super("ChainChecker: " + mybChainChecker.getID());
        this.checker = mybChainChecker;
        this.proposalPort = proposalPortNumber;
        this.proposalAddress = InetAddress.getByName(addressToProposeOn);
        this.acceptancePort = acceptancePortNumber;
        this.acceptanceAddress = InetAddress.getByName(addressToAcceptOn);
        this.learnPort = learningPortNumber;
        this.learnAddress = InetAddress.getByName(addressToLearnOn);
        this.chainCheckers = null;
    }

    @Override
    public void run() {

    }

    private void acceptBlocks() {
        try {
            MulticastSocket multicastSocket = new MulticastSocket(proposalPort);
            multicastSocket.joinGroup(proposalAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(TIMEOUT_MILLISECONDS);
            boolean running = true;
            while (running) {
                try {
                    byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
                    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(datagramPacket);
                    ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                    ObjectInputStream inputStream = new ObjectInputStream(bais);

                    Object object = inputStream.readObject();
                    if ((object != null) && (object instanceof ProposalPacket)) {
                        ProposalPacket proposalPacket = (ProposalPacket) object;
                        BigInteger chainLength = proposalPacket.getChainLength();
                        String proposerID = proposalPacket.getProposerID();
                        Block proposedBlock = proposalPacket.getBlock();
                        long roundID = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
                        boolean validated = validate(proposedBlock, chainLength);
                        if (validated) {
                            verify(proposalPacket, roundID);
                            learn(proposalPacket, roundID);
                        }else {

                        }

                    }
                    inputStream.close();
                    bais.close();
                } catch (SocketTimeoutException ste) {
                    ste.printStackTrace();
                }
            }

            multicastSocket.leaveGroup(proposalAddress);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private boolean validate(Block proposedBlock, BigInteger cahinLength) {
        Transaction[] proposedTransactions = proposedBlock.getTransactions();
        boolean valid = proposedTransactions[0].getTransactionAmount() == checker.getBlockChain().computeMinerAward(cahinLength);

        if (!valid) {
            return false;
        }else {
            for (int i = 1; i < proposedTransactions.length; i++) {
                if (!proposedTransactions[i].isVerified()) {
                    return false;
                }
            }
            //TODO: Additionally, could compute the proof of work, verify total ledger and all transactions, etc.
            return true;
        }
    }

    private void verify() {
        /*
        * Open multicast sockets to collect info
        *
        * Learn current ChainCheckers
        *
        * While a packet has not been received from all other nodes:
        *   Keep collecting info from the other nodes
        *   Store a value if no information has been received from that other node yet
        * Close the socket
        *
        * Compare packets and pick the 'best'
        *   First compare BlockChain length (pick the longest)
        *   Then compare bytes in the proof of work (pick the one with most zeros)
        *   Then compare the IDs of proposers (pick the... 'Biggest'? (compare string values))
        *   Then compare the IDs of Acceptors (pick the biggest)
        *
        * Send out this Acceptor's idea of what the 'best' is
        * While a packet has not been received from all other nodes:
        *   Keep collecting info from the other nodes
        *   Store a value if no information has been received from that other node yet
        *
        * if consensus reached:
        *   done?
        * else:
        *  retry(?)
        *
        * */
    }


    private void learnCurrentChainCheckers() {
        try {
            int numberOfCheckersFinished = 0;
            int attempts = 0;
            chainCheckers = null;
            MulticastSocket multicastSocket = new MulticastSocket(acceptancePort);
            multicastSocket.joinGroup(acceptanceAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(Math.max(MIN_COLLISION_PREVENTING_TIMEOUT_TIME,
                    Math.toIntExact(ThreadLocalRandom.current().nextLong(COLLISION_PREVENTING_TIMEOUT_TIME))));
            ByteArrayInputStream bais = null;
            ObjectInputStream inputStream = null;
            ByteArrayOutputStream baos = null;
            ObjectOutputStream outputStream = null;
            Thread.sleep(ThreadLocalRandom.current().nextLong(COLLISION_PREVENTING_TIMEOUT_TIME));

            while (numberOfCheckersFinished < N) {
                byte[] buf = checker.getID().getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                multicastSocket.send(datagramPacket);

                buf = new byte[multicastSocket.getReceiveBufferSize()];
                datagramPacket = new DatagramPacket(buf, buf.length);
                multicastSocket.receive(datagramPacket);
                bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                inputStream = new ObjectInputStream(bais);

                String chekerInfo = inputStream.readUTF();
                if (chekerInfo.equalsIgnoreCase(DONE_STRING)) {
                    numberOfCheckersFinished++;
                    /*
                    * FIXME
                    * Maye make eic -> contains...
                    * TODO
                    * Print out info, or done, or both
                    * */
                }else {
                    /*
                    * FIXME
                    * Maye make eic -> contains...
                    * TODO
                    * Print out info, or done, or both
                    * */
                }
                chainCheckers.putIfAbsent(inputStream.readUTF(), chainCheckers.size());
                attempts++;

                if (attempts >= N) {
                    multicastSocket.setSoTimeout(Math.toIntExact(ThreadLocalRandom.current().nextLong(COLLISION_PREVENTING_TIMEOUT_TIME)));
                    attempts = 0;
                }

            }

            inputStream.close();
            bais.close();

        } catch (SocketTimeoutException ste) {

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void learnCurrentLearners() {

    }

    private void updateUsers() {

    }
}
