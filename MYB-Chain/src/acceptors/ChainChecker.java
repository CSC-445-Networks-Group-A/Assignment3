package acceptors;

import chain.Block;
import chain.Transaction;
import chain.User;
import common.Addresses;
import common.NodeType;
import common.Ports;
import common.Pair;
import packets.acceptances.AcceptedPacket;
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
    private final static int N = 4;
    private final static int f = (N-1)/3;
    private final String PRIVATE_KEY_PATH;
    private final User checker;
    private final RSAPrivateKey checkerPrivateKey;
    private final InetAddress proposalAddress;
    private final InetAddress acceptanceAddress;
    private final InetAddress minerLearningAddress;
    private final InetAddress chainHolderLearningAddress;
    private final int proposalPort;
    private final int acceptancePort;
    private final int minerLearningPort;
    private final int chainHolderLearningPort;

    /**
     * Note that for N ChainCheckers, Byzantine Paxos can handle f faulty or malicious Acceptors where
     *
     * f = (N-1)/3
     *
     * The protocol requires consensus of 2f + 1 Acceptors.
     *
     * */
    public ChainChecker(User mybChainChecker, int fileID) throws IOException, ClassNotFoundException {
        super("ChainChecker: " + mybChainChecker.getID());
        this.PRIVATE_KEY_PATH = "localhome" + File.separator + "csc445" + File.separator + "group-A" + File.separator +
                "UserResources" + File.separator + "PRIVATE" + File.separator + NodeType.ACCEPTOR + File.separator + "PK_" + fileID + ".dat";
        this.checker = mybChainChecker;
        this.checkerPrivateKey = User.loadPrivateKeyFromFile(PRIVATE_KEY_PATH);
        this.proposalAddress = InetAddress.getByName(Addresses.MINER_PROPOSAL_ADDRESS);
        this.acceptanceAddress = InetAddress.getByName(Addresses.CHECKER_ACCEPTANCE_ADDRESS);
        this.minerLearningAddress = InetAddress.getByName(Addresses.MINER_LEARNING_ADDRESS);
        this.chainHolderLearningAddress = InetAddress.getByName(Addresses.HOLDER_LEARNING_ADDRESS);
        this.proposalPort = Ports.MINER_PROPOSAL_PORT;
        this.acceptancePort = Ports.CHECKER_ACCEPTANCE_PORT;
        this.minerLearningPort = Ports.MINER_LEARNING_PORT;
        this.chainHolderLearningPort = Ports.HOLDER_LEARNING_PORT;
    }


    @Override
    public void run() {
        this.acceptBlocks();
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
                        boolean validated = validate(proposedBlock, chainLength);
                        if (validated) {
                            VerifyPacket verifiedPacket = verify(proposalPacket);
                            if (verifiedPacket != null) {
                                learn(verifiedPacket);
                            }
                        }
                        /*
                        * otherwise, ignore
                        * */

                    }
                    inputStream.close();
                    bais.close();
                } catch (SocketTimeoutException ste) {
                    ste.printStackTrace();
                } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                    running = false;
                }
            }

            multicastSocket.leaveGroup(proposalAddress);

        } catch (IOException | ClassNotFoundException e) {
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


    private VerifyPacket verify(ProposalPacket proposalPacket) throws IOException, NoSuchAlgorithmException,
            IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, BadPaddingException {
        BigInteger chainLength = proposalPacket.getChainLength();
        Block verifiedBlock = proposalPacket.getBlock();
        VerifyPacket verifiedPacket = null;
        int packetsVerified = 0;

        /*
        * FIXME --- Maybe also include a variable here - "attemptNumber" - as a control variable to keep from looping infinitely
        * */
        while (packetsVerified < (2*f + 1)) {
            byte[] encryptedData = VerifyPacket.encryptPacketData(checkerPrivateKey, chainLength, verifiedBlock);
            VerifyPacket verifyPacket = new VerifyPacket(checker.getPublicKey(), chainLength, verifiedBlock, encryptedData);
            HashMap<RSAPublicKey, VerifyPacket> packetsToValidate = sendAndReceivePackets(verifyPacket);
            HashMap<RSAPublicKey, VerifyPacket> validatedPackets = validatePackets(packetsToValidate);
            //VerifyPacket bestPacket = determineBestPacket(validatedPackets);
            Pair<VerifyPacket, Integer> agreedUponPacketInfo = attemptToAchieveConsensus(validatedPackets);

            if (agreedUponPacketInfo != null) {
                verifiedPacket = agreedUponPacketInfo.getKey();
                chainLength = verifiedPacket.getChainLength();
                verifiedBlock = verifiedPacket.getBlock();
                packetsVerified = agreedUponPacketInfo.getValue();
            }
        }

        return verifiedPacket;

    }


    private HashMap<RSAPublicKey, VerifyPacket> sendAndReceivePackets(VerifyPacket packetToSend) {
        try {
            int receiveAttempts = 0;
            int timesReset = 0;
            HashMap<RSAPublicKey, VerifyPacket> receivedProposals = new HashMap<>(N);
            receivedProposals.put(packetToSend.getPublicKey(), packetToSend);
            MulticastSocket multicastSocket = new MulticastSocket(acceptancePort);
            multicastSocket.joinGroup(acceptanceAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(
                    ThreadLocalRandom.current().nextInt(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME));
            ByteArrayInputStream bais = null;
            ObjectInputStream inputStream = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);
            Thread.sleep(ThreadLocalRandom.current().nextLong(COLLISION_PREVENTING_TIMEOUT_TIME));

            /*
            * FIXME It may be that N here should actually be 2f+1
            * */
            while (receivedProposals.size() < N) {
                outputStream.writeObject(packetToSend);
                byte[] buf = baos.toByteArray();
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, acceptanceAddress, acceptancePort);
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
            multicastSocket.leaveGroup(acceptanceAddress);
            return receivedProposals;

        } catch (IOException |InterruptedException | ClassNotFoundException e) {
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


    private Pair<VerifyPacket, Integer> attemptToAchieveConsensus(HashMap<RSAPublicKey, VerifyPacket> validatedPackets) {
        try {
            int receiveAttempts = 0;
            int timesReset = 0;
            HashMap<RSAPublicKey, VerifyAllPacket> knowledgeOfAllAcceptors = new HashMap<>(N);
            VerifyAllPacket verifyAllPacket = new VerifyAllPacket(checker.getPublicKey(), validatedPackets);
            knowledgeOfAllAcceptors.put(checker.getPublicKey(), verifyAllPacket);
            MulticastSocket multicastSocket = new MulticastSocket(acceptancePort);
            multicastSocket.joinGroup(acceptanceAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(
                    ThreadLocalRandom.current().nextInt(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME));
            ByteArrayInputStream bais = null;
            ObjectInputStream inputStream = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);
            Thread.sleep(ThreadLocalRandom.current().nextLong(COLLISION_PREVENTING_TIMEOUT_TIME));

            /*
            * FIXME It may be that N here should actually be 2f+1
            *
            * FIXME Or perhaps <<<<<    validatedPackets.size()    >>>>>
            * */
            while (knowledgeOfAllAcceptors.size() < (2*f + 1)) {
                outputStream.writeObject(verifyAllPacket);
                byte[] buf = baos.toByteArray();
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, acceptanceAddress, acceptancePort);
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

                    /*
                    * TODO: close output streams?
                    * */
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
            multicastSocket.leaveGroup(acceptanceAddress);

            return compareData(knowledgeOfAllAcceptors);

        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    private Pair<VerifyPacket, Integer> compareData(HashMap<RSAPublicKey, VerifyAllPacket> knowledgeOfAllAcceptors) {
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
                }else if (packetFrequencies.get(publicKey).get(verifyPacket) >= (2*f + 1)){
                    mostFrequentPacketPerAcceptor.put(publicKey,
                            new Pair<>(verifyPacket, packetFrequencies.get(publicKey).get(verifyPacket)));
                }

            }
        }

        if (mostFrequentPacketPerAcceptor.size() >= (2*f + 1)) {
            Pair<VerifyPacket, Integer> mostFrequentPacket = null;
            int frequencyVerification = 0;

            for (Pair<VerifyPacket, Integer> pair : mostFrequentPacketPerAcceptor.values()) {

                if (mostFrequentPacket == null) {
                    mostFrequentPacket = pair;
                    frequencyVerification = 1;

                }else if (pair.getValue() > mostFrequentPacket.getValue()) {

                    mostFrequentPacket = pair;
                    if (mostFrequentPacket.getKey().equals(pair.getKey())) {
                        frequencyVerification++;
                    }else {
                        frequencyVerification = 1;
                    }

                }else if (mostFrequentPacket.getKey().equals(pair.getKey())) {
                    frequencyVerification++;
                }
            }

            if (frequencyVerification != mostFrequentPacket.getValue()) {
                System.out.println("WARNING:\n" +
                        "Mismatch of most frequent Packet's frequency as determined by all acceptors and as determined by final verification.");
            }

            return mostFrequentPacket;
        }

        return null;

    }


    private void learn(VerifyPacket verifiedPacket) throws BadPaddingException,
            NoSuchAlgorithmException, IOException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException {
        /*
        * for miners: this.minerLearningAddress = InetAddress.getByName(Addresses.MINER_LEARNING_ADDRESS);
        * */

        /*
        * for learners: this.chainHolderLearningAddress = InetAddress.getByName(Addresses.HOLDER_LEARNING_ADDRESS);
        * */

        /*
        * send message to learners until you get a 2f + 1 responses
        * */
        byte[] encryptedData = AcceptedPacket.encryptPacketData(checkerPrivateKey, verifiedPacket.getChainLength(), verifiedPacket.getBlock());
        AcceptedPacket packetToLearn = new AcceptedPacket(checker.getPublicKey(), verifiedPacket.getChainLength(), verifiedPacket.getBlock(), encryptedData);
        AcceptedPacket packetLearned = null;
        int attemptNumber = 0;
        while (packetLearned == null) {
            if (attemptNumber % N == 0) {
                sendPacket(packetToLearn, chainHolderLearningAddress, chainHolderLearningPort);
                attemptNumber = 0;
            }

            packetLearned = receivePacket(chainHolderLearningAddress, chainHolderLearningPort);
            attemptNumber++;
        }


        /*
        * send message to all miners
        * */

        sendPacket(packetLearned, proposalAddress, proposalPort);

    }


    private int sendPacket(AcceptedPacket acceptedPacket, InetAddress address, int port) {
        try {
            System.out.println("LEARNING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learning Port:\t" + port);

            MulticastSocket multicastSocket = new MulticastSocket(port);
            multicastSocket.joinGroup(address);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(
                    ThreadLocalRandom.current().nextInt(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);

            outputStream.writeObject(acceptedPacket);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length, address, port);
            multicastSocket.send(datagramPacket);

            outputStream.close();
            baos.close();
            multicastSocket.leaveGroup(address);
            System.out.println("FINISHING LEARNING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learning Port:\t" + port);

            return 1;

        } catch (SocketTimeoutException ste) {
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }


    private AcceptedPacket receivePacket(InetAddress address, int port) {
        try {
            System.out.println("LEARNING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learning Port:\t" + port);

            MulticastSocket multicastSocket = new MulticastSocket(port);
            multicastSocket.joinGroup(address);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(
                    ThreadLocalRandom.current().nextInt(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME));


            byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
            multicastSocket.receive(datagramPacket);
            ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
            ObjectInputStream inputStream = new ObjectInputStream(bais);

            Object object = inputStream.readObject();
            AcceptedPacket packetAccepted = null;
            if ((object != null) && (object instanceof AcceptedPacket)) {
                packetAccepted = ((AcceptedPacket) object);

            }
            inputStream.close();
            bais.close();
            multicastSocket.leaveGroup(address);
            System.out.println("FINISHING LEARNING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learning Port:\t" + port);

            return packetAccepted;

        } catch (SocketTimeoutException ste) {
            return null;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


}
