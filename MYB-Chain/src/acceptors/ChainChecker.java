package acceptors;

import chain.Block;
import chain.Transaction;
import chain.User;
import javafx.util.Pair;
import packets.proposals.ProposalPacket;
import packets.verifications.VerifyAllPacket;
import packets.verifications.VerifyPacket;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final InetAddress generalAcceptanceAddress;
    private final InetAddress learnAddress;
    private final int proposalPort;
    private final int generalAcceptancePort;
    private final int learnPort;

    /**
     * Note that for N ChainCheckers, Byzantine Paxos can handle f faulty or malicious Acceptors where
     *
     * f = (N-1)/3
     *
     * The protocol requires consensus of 2f + 1 Acceptors.
     *
     * */
    public ChainChecker(User mybChainChecker, int proposalPortNumber, String addressToProposeOn,
                        int generalAcceptancePortNumber, String generalAddressToAcceptOn,
                        int learningPortNumber, String addressToLearnOn, int intialNumberOfCheckers) throws UnknownHostException {
        super("ChainChecker: " + mybChainChecker.getID());
        this.checker = mybChainChecker;
        this.proposalPort = proposalPortNumber;
        this.proposalAddress = InetAddress.getByName(addressToProposeOn);
        this.generalAcceptancePort = generalAcceptancePortNumber;
        this.generalAcceptanceAddress = InetAddress.getByName(generalAddressToAcceptOn);
        this.learnPort = learningPortNumber;
        this.learnAddress = InetAddress.getByName(addressToLearnOn);
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
                        //long roundID = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
                        boolean validated = validate(proposedBlock, chainLength);
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

    private boolean validate(Block proposedBlock, BigInteger chainLength) throws IOException, NoSuchAlgorithmException {
        Transaction[] proposedTransactions = proposedBlock.getTransactions();
        boolean valid = proposedTransactions[0].getTransactionAmount() == checker.getBlockChain().computeMinerAward(chainLength);

        if (!valid) {
            return false;
        }else {
            for (int i = 1; i < proposedTransactions.length; i++) {
                if (!proposedTransactions[i].isVerified()) {
                    return false;
                }
            }

            byte[] proofOfWork = proposedBlock.getProofOfWork();
            byte[] blockBytes = proposedBlock.getBlockBytes();
            return verifyProofOfWork(proofOfWork, blockBytes);
        }
    }

    private boolean verifyProofOfWork(byte[] proofOfWork, byte[] blockBytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(blockBytes);

        if (hash.length != proofOfWork.length) {
            return false;
        }

        for (int i = 0; i < proofOfWork.length; i++) {
            if (proofOfWork[i] != hash[i]) {
                return false;
            }
        }
        return true;
    }

    private void verify(ProposalPacket proposalPacket) throws IOException, NoSuchAlgorithmException {
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
        BigInteger chainLength = proposalPacket.getChainLength();
        String proposerID = proposalPacket.getProposerID();
        Block verifiedBlock = proposalPacket.getBlock();
        int packetsVerified = 1;

        while (packetsVerified < (2*f + 1)) {
            VerifyPacket verifyPacket =
                    new VerifyPacket(chainLength, checker.getID(), verifiedBlock);
            HashMap<String, VerifyPacket> packetsToValidate = sendAndReceivePackets(verifyPacket);
            HashMap<String, VerifyPacket> validatedPackets = validatePackets(packetsToValidate);
            VerifyPacket bestPacket = determineBestPacket(validatedPackets);
            packetsVerified = attemptToAchieveConsensus(validatedPackets, bestPacket);
        }

        return; /*TODO either the verified packet or the verified packets */

    }


    private HashMap<String, VerifyPacket> sendAndReceivePackets(VerifyPacket packetToSend) {
        try {
            int receiveAttempts = 0;
            int timesReset = 0;
            HashMap<String, VerifyPacket> receivedProposals = new HashMap<>(N);
            receivedProposals.put(packetToSend.getID(), packetToSend);
            MulticastSocket multicastSocket = new MulticastSocket(generalAcceptancePort);
            multicastSocket.joinGroup(generalAcceptanceAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(
                    ThreadLocalRandom.current().nextInt(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME));
            ByteArrayInputStream bais = null;
            ObjectInputStream inputStream = null;
            ByteArrayOutputStream baos = null;
            ObjectOutputStream outputStream = null;
            Thread.sleep(ThreadLocalRandom.current().nextLong(COLLISION_PREVENTING_TIMEOUT_TIME));

            /*
            * FIXME It may be that N here should actually be 2f+1
            * */
            while (receivedProposals.size() < N) {
                outputStream.writeObject(packetToSend);
                byte[] buf = baos.toByteArray();
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                multicastSocket.send(datagramPacket);

                buf = new byte[multicastSocket.getReceiveBufferSize()];
                datagramPacket = new DatagramPacket(buf, buf.length);

                try {
                    multicastSocket.receive(datagramPacket);
                    bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                    inputStream = new ObjectInputStream(bais);

                    Object object = inputStream.readObject();
                    if ((object != null) && (object instanceof VerifyPacket)) {
                        VerifyPacket verifyPacket = (VerifyPacket) object;
                        if (verifyPacket.getID() != null && verifyPacket.getBlock() != null) {
                            String id = verifyPacket.getID();
                            if (receivedProposals.containsKey(id)) {
                                receivedProposals.put(id, null);
                            }else {
                                receivedProposals.put(id, verifyPacket);
                            }
                            System.out.println("Block sent by:\n\n" +
                                    "Sender:\t" + id + "\n\n");
                        }else {
                            System.out.println("WARNING -- Null block received:\n\n" +
                                    "Sender:\t" + verifyPacket.getID() + "\n\n" +
                                    "Further monitoring may be necessary");
                        }
                    }

                }catch (SocketTimeoutException ste) {
                    ste.printStackTrace();
                    receiveAttempts++;
                    if (receiveAttempts >= 2*N) {
                        multicastSocket.setSoTimeout(
                                ThreadLocalRandom.current().nextInt(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME));
                        timesReset++;
                        receiveAttempts = 0;
                    }
                    if (timesReset >= N) {
                        break;
                    }

                }
            }

            inputStream.close();
            bais.close();
            multicastSocket.leaveGroup(generalAcceptanceAddress);
            return receivedProposals;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    private HashMap<String, VerifyPacket> validatePackets(HashMap<String, VerifyPacket> packetsToValidate) throws IOException, NoSuchAlgorithmException {

        HashMap<String, VerifyPacket> validatedPackets = new HashMap<>(N);
        for (String acceptorID : packetsToValidate.keySet()) {
            VerifyPacket packet = packetsToValidate.get(acceptorID);
            if (validate(packet.getBlock(), packet.getChainLength())) {
                validatedPackets.putIfAbsent(acceptorID, packet);
            }
        }
        return validatedPackets;
    }


    private VerifyPacket determineBestPacket(HashMap<String, VerifyPacket> validatedPackets) {

        VerifyPacket bestPacket = null;

        for (VerifyPacket packet : validatedPackets.values()) {
            if (bestPacket == null) {
                bestPacket = packet;
            }else if (bestPacket.compareTo(packet) == -1){
                bestPacket = packet;
            }
        }

        return bestPacket;
    }


    private int attemptToAchieveConsensus(HashMap<String, VerifyPacket> validatedPackets, VerifyPacket packetToVerify) {
        /*
        * while consensus is not achieved
        *     send out what this acceptor believes to be the best packet
        *     receive packets back
        *     pick the best/most agreed upon
        * */
        int agreeingAcceptors = 1;

        while (agreeingAcceptors < (2*f + 1)) {
            agreeingAcceptors = 1;
            /*
            * Communicate with each address individually
            * Store results as Address, Result mapping
            * Send out mapping via multicast and receive other mappings via multicast.
            * Compare, and keep track of the number that agree for each address
            *
            * */

        }




        try {
            int receiveAttempts = 0;
            int timesReset = 0;
            HashMap<String, VerifyAllPacket> knowledgeOfAllAcceptors = new HashMap<>(N);
            VerifyAllPacket verifyAllPacket = new VerifyAllPacket(checker.getID(), validatedPackets);
            knowledgeOfAllAcceptors.put(checker.getID(), verifyAllPacket);
            MulticastSocket multicastSocket = new MulticastSocket(generalAcceptancePort);
            multicastSocket.joinGroup(generalAcceptanceAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(
                    ThreadLocalRandom.current().nextInt(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME));
            ByteArrayInputStream bais = null;
            ObjectInputStream inputStream = null;
            ByteArrayOutputStream baos = null;
            ObjectOutputStream outputStream = null;
            Thread.sleep(ThreadLocalRandom.current().nextLong(COLLISION_PREVENTING_TIMEOUT_TIME));

            /*
            * FIXME It may be that N here should actually be 2f+1
            * */
            while (knowledgeOfAllAcceptors.size() < N) {
                outputStream.writeObject(verifyAllPacket);
                byte[] buf = baos.toByteArray();
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                multicastSocket.send(datagramPacket);

                buf = new byte[multicastSocket.getReceiveBufferSize()];
                datagramPacket = new DatagramPacket(buf, buf.length);

                try {
                    multicastSocket.receive(datagramPacket);
                    bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                    inputStream = new ObjectInputStream(bais);

                    Object object = inputStream.readObject();
                    if ((object != null) && (object instanceof VerifyAllPacket)) {
                        VerifyAllPacket acceptorKnowledgePacket = (VerifyAllPacket) object;

                        if (acceptorKnowledgePacket.getID() != null) {
                            String id = acceptorKnowledgePacket.getID();
                            if (knowledgeOfAllAcceptors.containsKey(id)) {
                                knowledgeOfAllAcceptors.put(id, null);
                            }else {
                                knowledgeOfAllAcceptors.put(id, acceptorKnowledgePacket);
                            }
                            System.out.println("Block sent by:\n\n" +
                                    "Sender:\t" + acceptorKnowledgePacket.getID() + "\n\n");
                        }else {
                            System.out.println("WARNING -- Null block sent by:\n\n" +
                                    "Sender:\t" + acceptorKnowledgePacket.getID() + "\n\n" +
                                    "Further monitoring may be necessary");
                        }
                    }

                }catch (SocketTimeoutException ste) {
                    ste.printStackTrace();
                    receiveAttempts++;
                    if (receiveAttempts >= 2*N) {
                        multicastSocket.setSoTimeout(
                                ThreadLocalRandom.current().nextInt(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME));
                        timesReset++;
                        receiveAttempts = 0;
                    }
                    if (timesReset >= N) {
                        break;
                    }

                }
            }

            inputStream.close();
            bais.close();
            multicastSocket.leaveGroup(generalAcceptanceAddress);

            compareData(knowledgeOfAllAcceptors, validatedPackets, packetToVerify);

            /*
            * This
            * */
            return receivedProposals;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;

        return -1;
    }


    private void updateUsers() {

    }
}
