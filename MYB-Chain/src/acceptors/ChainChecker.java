package acceptors;

import chain.Block;
import chain.User;
import javafx.util.Pair;
import packets.acceptances.AcceptedPacket;
import packets.proposals.ProposalPacket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.*;
import java.util.HashMap;

/**
 * Created by Michael on 4/18/2018.
 */
public class ChainChecker extends Thread{
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int TTL = 12;
    private final User checker;
    private final InetAddress proposalAddress;
    private final InetAddress acceptanceAddress;
    private final InetAddress learnAddress;
    private final int proposalPort;
    private final int acceptancePort;
    private final int learnPort;
    private HashMap<Pair<InetAddress, Integer>, ChainChecker> chainCheckers;

    /**
     * Note that for N ChainCheckers, Byzantine Paxos can handle f faulty or malicious Acceptors where
     *
     * f = (N-1)/3
     *
     * The protocol requires consensus of 2f + 1 Acceptors.
     *
     * */
    public ChainChecker(User mybChainChecker, int proposalPortNumber, String addressToProposeOn, int learningPortNumber,
                        String addressToLearnOn, int intialNumberOfCheckers) throws UnknownHostException {
        super("ChainChecker: " + mybChainChecker.getID());
        this.checker = mybChainChecker;
        this.proposalPort = proposalPortNumber;
        this.proposalAddress = InetAddress.getByName(addressToProposeOn);
        this.learnPort = learningPortNumber;
        this.learnAddress = InetAddress.getByName(addressToLearnOn);
        this.chainCheckers = learnCurrentChainCheckers();
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
                        boolean validated = validate(proposedBlock);
                        if (validated) {
                            verify(proposalPacket);
                            learn(proposalPacket);
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

    private HashMap<Pair<InetAddress, Integer>, ChainChecker> learnCurrentChainCheckers() {

    }

    private void learnCurrentLearners() {

    }

    private void updateUsers() {

    }
}
