package acceptors;

import chain.Block;
import chain.Transaction;
import chain.User;
import javafx.util.Pair;
import packets.proposals.ProposalPacket;
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
    private final InetAddress personalAcceptanceAddress;
    private final InetAddress learnAddress;
    private final int proposalPort;
    private final int generalAcceptancePort;
    private final int personalAcceptancePort;
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
                        int generalAcceptancePortNumber, String generalAddressToAcceptOn, int peronsalAcceptancePortNumber,
                        int learningPortNumber, String addressToLearnOn, int intialNumberOfCheckers) throws UnknownHostException {
        super("ChainChecker: " + mybChainChecker.getID());
        this.checker = mybChainChecker;
        this.proposalPort = proposalPortNumber;
        this.proposalAddress = InetAddress.getByName(addressToProposeOn);
        this.generalAcceptancePort = generalAcceptancePortNumber;
        this.generalAcceptanceAddress = InetAddress.getByName(generalAddressToAcceptOn);
        this.personalAcceptancePort = peronsalAcceptancePortNumber;
        this.personalAcceptanceAddress = new InetSocketAddress(peronsalAcceptancePortNumber).getAddress();
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

    private boolean validate(Block proposedBlock, BigInteger cahinLength) throws IOException, NoSuchAlgorithmException {
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
                    new VerifyPacket(chainLength, personalAcceptanceAddress, personalAcceptancePort, checker.getID(), verifiedBlock);
            HashMap<Pair<InetAddress, Integer>, VerifyPacket> packetsToValidate = sendAndReceivePackets(verifyPacket);
            HashMap<Pair<InetAddress, Integer>, VerifyPacket> validatedPackets = validatePackets(packetsToValidate);
            VerifyPacket bestPacket = determineBestPacket(validatedPackets);
            packetsVerified = attemptToAchieveConsensus(validatedPackets, bestPacket);
        }

        return; /*TODO either the verified packet or the verified packets */

    }


    private HashMap<Pair<InetAddress,Integer>, VerifyPacket> sendAndReceivePackets(VerifyPacket packetToSend) {
        try {
            int sendAttempts = 0;
            int timesReset = 0;
            HashMap<Pair<InetAddress, Integer>, VerifyPacket> receivedProposals = new HashMap<>(N);
            receivedProposals.putIfAbsent(new Pair<>(personalAcceptanceAddress, personalAcceptancePort), packetToSend);
            MulticastSocket multicastSocket = new MulticastSocket(generalAcceptancePort);
            multicastSocket.joinGroup(generalAcceptanceAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(Math.toIntExact(
                    ThreadLocalRandom.current().nextLong(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME)));
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
                }catch (SocketTimeoutException ste) {
                    ste.printStackTrace();
                    sendAttempts++;
                    if (sendAttempts >= (TIMEOUT_MILLISECONDS/COLLISION_PREVENTING_TIMEOUT_TIME)) {
                        multicastSocket.setSoTimeout(Math.toIntExact(
                                ThreadLocalRandom.current().nextLong(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME)));
                        timesReset++;
                        sendAttempts = 0;
                    }
                    if (timesReset >= (TIMEOUT_MILLISECONDS/COLLISION_PREVENTING_TIMEOUT_TIME)) {
                        break;
                    }
                }
                bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                inputStream = new ObjectInputStream(bais);

                Object object = inputStream.readObject();
                if ((object != null) && (object instanceof VerifyPacket)) {
                    VerifyPacket verifyPacket = (VerifyPacket) object;
                    if (verifyPacket.getAddress() != null && verifyPacket.getPort() != null) {
                        receivedProposals.putIfAbsent(new Pair<>(verifyPacket.getAddress(), verifyPacket.getPort()), verifyPacket);
                        System.out.println("Block sent by:\n\n" +
                                "Sender:\t" + verifyPacket.getID() + "\n\n");
                    }else {
                        System.out.println("WARNING -- Null block sent by:\n\n" +
                                "Sender:\t" + verifyPacket.getID() + "\n\n" +
                                "Further monitoring may be necessary");
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


    private HashMap<Pair<InetAddress, Integer>, VerifyPacket> validatePackets(
            HashMap<Pair<InetAddress, Integer>, VerifyPacket> packetsToValidate) throws IOException, NoSuchAlgorithmException {

        HashMap<Pair<InetAddress, Integer>, VerifyPacket> validatedPackets = new HashMap<>(N);
        for (Pair<InetAddress, Integer> portInfo : packetsToValidate.keySet()) {
            VerifyPacket packet = packetsToValidate.get(portInfo);
            if (validate(packet.getBlock(), packet.getChainLength())) {
                validatedPackets.putIfAbsent(portInfo, packet);
            }
        }
        return validatedPackets;
    }


    private VerifyPacket determineBestPacket(HashMap<Pair<InetAddress, Integer>, VerifyPacket> validatedPackets) {

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


    private int attemptToAchieveConsensus(HashMap<Pair<InetAddress, Integer>, VerifyPacket> validatedPackets, VerifyPacket packetToVerify) {



        return -1;
    }


    private void updateUsers() {

    }
}
