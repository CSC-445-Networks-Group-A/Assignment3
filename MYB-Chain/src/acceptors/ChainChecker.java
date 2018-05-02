package acceptors;

import chain.Block;
import chain.Transaction;
import chain.User;
import javafx.util.Pair;
import packets.proposals.ProposalPacket;
import packets.verifications.VerifyAllPacket;
import packets.verifications.VerifyPacket;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
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
    private final RSAPrivateKey checkerPrivateKey;
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
        this.checkerPrivateKey = checker.readKeyFromFile();
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
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
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

    private void verify(ProposalPacket proposalPacket) throws IOException, NoSuchAlgorithmException,
            IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, BadPaddingException {
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
        Block verifiedBlock = proposalPacket.getBlock();
        int packetsVerified = 1;

        while (packetsVerified < (2*f + 1)) {
            byte[] encryptedData = VerifyPacket.encryptPacketData(checkerPrivateKey, chainLength, verifiedBlock);
            VerifyPacket verifyPacket = new VerifyPacket(checker.getPublicKey(), chainLength, verifiedBlock, encryptedData);
            HashMap<RSAPublicKey, VerifyPacket> packetsToValidate = sendAndReceivePackets(verifyPacket);
            HashMap<RSAPublicKey, VerifyPacket> validatedPackets = validatePackets(packetsToValidate);
            VerifyPacket bestPacket = determineBestPacket(validatedPackets);
            packetsVerified = attemptToAchieveConsensus(validatedPackets, bestPacket);
        }

        return; /*TODO either the verified packet or the verified packets */

    }


    private HashMap<RSAPublicKey, VerifyPacket> sendAndReceivePackets(VerifyPacket packetToSend) {
        try {
            int receiveAttempts = 0;
            int timesReset = 0;
            HashMap<RSAPublicKey, VerifyPacket> receivedProposals = new HashMap<>(N);
            receivedProposals.put(packetToSend.getPublicKey(), packetToSend);
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
                        if (verifyPacket.getPublicKey() != null && verifyPacket.getBlock() != null) {
                            RSAPublicKey publicKey = verifyPacket.getPublicKey();
                            receivedProposals.putIfAbsent(publicKey, verifyPacket);
                            System.out.println("Block sent by:\n\n" +
                                    "Sender:\t" + publicKey + "\n\n");

                        }else {
                            System.out.println("WARNING -- Null data received:\n\n" +
                                    "Sender:\t" + verifyPacket.getPublicKey() + "\n\n" +
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


    private HashMap<RSAPublicKey, VerifyPacket> validatePackets(HashMap<RSAPublicKey, VerifyPacket> packetsToValidate)
            throws IOException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {

        HashMap<RSAPublicKey, VerifyPacket> validatedPackets = new HashMap<>(N);
        for (RSAPublicKey publicKey : packetsToValidate.keySet()) {
            VerifyPacket packet = packetsToValidate.get(publicKey);
            if (validate(packet.getBlock(), packet.getChainLength()) && packet.isHonest()) {
                validatedPackets.putIfAbsent(publicKey, packet);
            }
        }
        return validatedPackets;
    }


    private VerifyPacket determineBestPacket(HashMap<RSAPublicKey, VerifyPacket> validatedPackets) {

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


    private int attemptToAchieveConsensus(HashMap<RSAPublicKey, VerifyPacket> validatedPackets) {
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
            HashMap<RSAPublicKey, VerifyAllPacket> knowledgeOfAllAcceptors = new HashMap<>(N);
            VerifyAllPacket verifyAllPacket = new VerifyAllPacket(checker.getPublicKey(), validatedPackets);
            knowledgeOfAllAcceptors.put(checker.getPublicKey(), verifyAllPacket);
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
            *
            * FIXME Or perhaps <<<<<    validatedPackets.size()    >>>>>
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

                        if (acceptorKnowledgePacket.getPublicKey() != null) {
                            RSAPublicKey publicKey = acceptorKnowledgePacket.getPublicKey();
                            knowledgeOfAllAcceptors.putIfAbsent(publicKey, acceptorKnowledgePacket);
                            System.out.println("Block sent by:\n\n" +
                                    "Sender:\t" + acceptorKnowledgePacket.getPublicKey() + "\n\n");
                        }else {
                            System.out.println("WARNING -- Null data sent by:\n\n" +
                                    "Sender:\t" + acceptorKnowledgePacket + "\n\n" +
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

            return compareData(knowledgeOfAllAcceptors);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return -1;
    }


    private int compareData(HashMap<RSAPublicKey, VerifyAllPacket> knowledgeOfAllAcceptors) {
        HashMap<RSAPublicKey, HashMap<VerifyPacket, Integer>> packetFrequencies = new HashMap<>(N);

        for (RSAPublicKey rsaPublicKey : knowledgeOfAllAcceptors.keySet()) {
            HashMap<RSAPublicKey, VerifyPacket> acceptorKnowledge = knowledgeOfAllAcceptors.get(rsaPublicKey).getAcceptorKnowledge();
            for (RSAPublicKey publicKey : acceptorKnowledge.keySet()) {

                if (packetFrequencies.containsKey(publicKey)) {
                    HashMap<VerifyPacket, Integer> frequencies = packetFrequencies.get(publicKey);
                    VerifyPacket verifyPacket = acceptorKnowledge.get(publicKey);

                    if (frequencies.containsKey(verifyPacket)) {
                        Integer frequency = frequencies.get(verifyPacket);
                        frequencies.put(verifyPacket, (frequency + 1));
                    }else {
                        frequencies.put(verifyPacket, 1);
                    }

                    packetFrequencies.put(publicKey, frequencies);

                }else {
                    HashMap<VerifyPacket, Integer> frequencies = new HashMap<>(1);
                    frequencies.put(acceptorKnowledge.get(publicKey), 1);

                    packetFrequencies.put(publicKey, frequencies);

                }

            }
        }

        /*
        * For each publicKey, check if there is more than 1. If so, take the most agreed upon one. put into array.
        * finally, return most agreed upon from the array?
        * */

        HashMap<RSAPublicKey, Pair<VerifyPacket, Integer>> mostFrequentPacketPerAcceptor = new HashMap<>(N);

        for (RSAPublicKey publicKey : packetFrequencies.keySet()) {
            for (VerifyPacket verifyPacket : packetFrequencies.get(publicKey).keySet()) {

                if (mostFrequentPacketPerAcceptor.containsKey(publicKey)) {
                    if (mostFrequentPacketPerAcceptor.get(publicKey).getValue() < packetFrequencies.get(publicKey).get(verifyPacket)) {
                        mostFrequentPacketPerAcceptor.put(publicKey,
                                new Pair<>(verifyPacket, packetFrequencies.get(publicKey).get(verifyPacket)));
                    }
                }else if (packetFrequencies.get(publicKey).get(verifyPacket) >= (2*f+1)){
                    mostFrequentPacketPerAcceptor.put(publicKey,
                            new Pair<>(verifyPacket, packetFrequencies.get(publicKey).get(verifyPacket)));
                }

            }
        }

        if (mostFrequentPacketPerAcceptor.size() > (2*f+1)) {

        }

    }


    private void updateUsers() {

    }
}
