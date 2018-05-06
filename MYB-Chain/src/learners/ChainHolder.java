package learners;

import chain.Block;
import chain.Transaction;
import chain.User;
import common.Addresses;
import common.Ports;
import javafx.util.Pair;
import packets.acceptances.AcceptedPacket;
import packets.acceptances.SuccessfulUpdate;
import packets.learnings.LearnedPacket;
import packets.verifications.VerifyAllPacket;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Michael on 4/18/2018.
 */
public class ChainHolder extends Thread{
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int COLLISION_PREVENTING_TIMEOUT_TIME = 5000;
    private final static int MIN_COLLISION_PREVENTING_TIMEOUT_TIME = 500;
    private final static int TTL = 12;
    private final static int N = 10;
    private final static int f = (N-1)/3;
    private final User holder;
    private final RSAPrivateKey holderPrivateKey;
    private final InetAddress learningAddress;
    private final InetAddress finalAcceptanceAddress;
    private final int learningPort;
    private final int finalAcceptancePort;
    private ConcurrentHashMap<RSAPublicKey, AcceptedPacket> acceptedPackets;
    private ConcurrentHashMap<RSAPublicKey, SuccessfulUpdate> successfulUpdates;


    public ChainHolder(User mybHolder) throws IOException, ClassNotFoundException {
        super("ChainHolder: " + mybHolder.getID());
        this.holder = mybHolder;
        this.holderPrivateKey = holder.loadPrivateKeyFromFile();
        this.learningAddress = InetAddress.getByName(Addresses.HOLDER_LEARNING_ADDRESS);
        this.finalAcceptanceAddress = InetAddress.getByName(Addresses.HOLDER_CHECKING_ADDRESS);
        this.learningPort = Ports.HOLDER_LEARNING_PORT;
        this.finalAcceptancePort = Ports.HOLDER_CHECKING_PORT;
        this.acceptedPackets = new ConcurrentHashMap<>(N);
        this.successfulUpdates = new ConcurrentHashMap<>(N);
    }


    @Override
    public void run() {

    }


    private void listen() {
        try {
            System.out.println("STARTING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learning Port:\t" + learningPort);

            MulticastSocket multicastSocket = new MulticastSocket(learningPort);
            multicastSocket.joinGroup(learningAddress);

            boolean running = true;

            while (running) {
                try {
                    byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
                    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(datagramPacket);
                    ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                    ObjectInputStream inputStream = new ObjectInputStream(bais);

                    Object object = inputStream.readObject();
                    if ((object != null) && (object instanceof AcceptedPacket)) {
                        AcceptedPacket acceptedPacket = (AcceptedPacket) object;
                        RSAPublicKey rsaPublicKey = acceptedPacket.getPublicKey();
                        Block block = acceptedPacket.getBlock();
                        BigInteger chainLength = acceptedPacket.getChainLength();
                        byte[] encryptedDate = acceptedPacket.getEncryptedData();
                        boolean validated = validate(block, chainLength);

                        if (validated && acceptedPacket.isHonest()) {
                            acceptedPackets.putIfAbsent(rsaPublicKey, acceptedPacket);

                            if (acceptedPackets.size() >= (2*f + 1)) {
                                LearnedPacket learnedPacket = learn(acceptedPacket);
                                if (learnedPacket != null) {
                                    /*
                                    * Remove packets which are equal to or less than(?) the current packet
                                    * Remove packets which have lesser chainLength values
                                    *   otherPacket.compareTo(learnedPacket) takes care of this
                                    *       Reomve if otherPacket.compareTo(learnedPacket) <= 0
                                    * */
                                    updateIfNecessary();
                                    record();
                                    acknowledge(learnedPacket);
                                }
                            }

                        }else {



                        }
                    }else if ((object != null) && (object instanceof SuccessfulUpdate)) {



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

            multicastSocket.leaveGroup(learningAddress);
            System.out.println("FINISHING:\t" + Thread.currentThread().getName());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }


    private boolean validate(Block proposedBlock, BigInteger chainLength) throws IOException, NoSuchAlgorithmException {
        Transaction[] proposedTransactions = proposedBlock.getTransactions();
        boolean valid = proposedTransactions[0].getTransactionAmount() == holder.getBlockChain().computeMinerAward(chainLength);

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


    private LearnedPacket learn(AcceptedPacket acceptedPacket) throws IOException, NoSuchAlgorithmException,
            IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, BadPaddingException {
        BigInteger chainLength = acceptedPacket.getChainLength();
        Block verifiedBlock = acceptedPacket.getBlock();
        LearnedPacket learnedPacket = null;
        int packetsLearned = 0;

        /*
        * FIXME --- Maybe also include a variable here - "attemptNumber" - as a control variable to keep from looping infinitely
        * */
        while (packetsLearned < (2*f + 1)) {
            byte[] encryptedData = LearnedPacket.encryptPacketData(holderPrivateKey, chainLength, verifiedBlock);
            LearnedPacket packetLearned = new LearnedPacket(holder.getPublicKey(), chainLength, verifiedBlock, encryptedData);
            HashMap<RSAPublicKey, LearnedPacket> packetsToValidate = sendAndReceivePackets(packetLearned);
            HashMap<Pair<Block, BigInteger>, Integer> validatedPackets = validatePackets(packetsToValidate);
            //LearnedPacket bestPacket = determineBestPacket(validatedPackets);
            Pair<LearnedPacket, Integer> agreedUponPacketInfo = checkForConsensus(validatedPackets);

            if (agreedUponPacketInfo != null) {
                learnedPacket = agreedUponPacketInfo.getKey();
                chainLength = learnedPacket.getChainLength();
                verifiedBlock = learnedPacket.getBlock();
                packetsLearned = agreedUponPacketInfo.getValue();
            }
        }

        return learnedPacket;

    }


    private HashMap<RSAPublicKey, LearnedPacket> sendAndReceivePackets(LearnedPacket packetToSend) {
        try {
            int receiveAttempts = 0;
            int timesReset = 0;
            HashMap<RSAPublicKey, LearnedPacket> receivedPackets = new HashMap<>(N);
            receivedPackets.put(packetToSend.getPublicKey(), packetToSend);
            MulticastSocket multicastSocket = new MulticastSocket(finalAcceptancePort);
            multicastSocket.joinGroup(finalAcceptanceAddress);
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
            while (receivedPackets.size() < N) {
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
                    if ((object != null) && (object instanceof LearnedPacket)) {
                        LearnedPacket learnedPacket = (LearnedPacket) object;
                        if (learnedPacket.getPublicKey() != null && learnedPacket.getBlock() != null) {
                            RSAPublicKey publicKey = learnedPacket.getPublicKey();
                            receivedPackets.putIfAbsent(publicKey, learnedPacket);
                            System.out.println("Block sent by:\n\n" +
                                    "Sender:\t" + publicKey + "\n\n");

                        }else {
                            System.out.println("WARNING -- Null data received:\n\n" +
                                    "Sender:\t" + learnedPacket.getPublicKey() + "\n\n" +
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
            multicastSocket.leaveGroup(finalAcceptanceAddress);
            return receivedPackets;

        } catch (IOException |InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    private HashMap<Pair<Block, BigInteger>, Integer> validatePackets(HashMap<RSAPublicKey, LearnedPacket> packetsToValidate)
            throws IOException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {

        HashMap<Pair<Block, BigInteger>, Integer> validatedPackets = new HashMap<>(N);
        for (RSAPublicKey publicKey : packetsToValidate.keySet()) {
            LearnedPacket packet = packetsToValidate.get(publicKey);
            if (validate(packet.getBlock(), packet.getChainLength()) && packet.isHonest()) {
                Pair<Block, BigInteger> currentPair = new Pair<>(packet.getBlock(), packet.getChainLength());
                if (validatedPackets.containsKey(currentPair)) {
                    Integer currentValue = validatedPackets.get(currentPair);
                    validatedPackets.put(currentPair, (currentValue + 1));
                }else {
                    validatedPackets.put(currentPair, 1);
                }
            }
        }
        return validatedPackets;
    }


    private LearnedPacket determineBestPacket(HashMap<RSAPublicKey, LearnedPacket> validatedPackets) {

        LearnedPacket bestPacket = null;

        for (LearnedPacket packet : validatedPackets.values()) {
            if (bestPacket == null) {
                bestPacket = packet;
            }else if (bestPacket.compareTo(packet) == -1){
                bestPacket = packet;
            }
        }

        return bestPacket;
    }


    private Pair<LearnedPacket, Integer> checkForConsensus(HashMap<Pair<Block, BigInteger>, Integer> validatedPacketInfo)
            throws BadPaddingException, NoSuchAlgorithmException, IOException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException {
        Pair<LearnedPacket, Integer> bestPair = null;
        for (Pair<Block, BigInteger> pair : validatedPacketInfo.keySet()) {

            if ((validatedPacketInfo.get(pair) >= (2*f + 1)) && (bestPair == null)) {
                byte[] encryptedData = LearnedPacket.encryptPacketData(holderPrivateKey, pair.getValue(), pair.getKey());
                bestPair = new Pair<>(
                        new LearnedPacket(holder.getPublicKey(), pair.getValue(), pair.getKey(), encryptedData), validatedPacketInfo.get(pair));

            }else if (validatedPacketInfo.get(pair) > bestPair.getValue()){
                byte[] encryptedData = LearnedPacket.encryptPacketData(holderPrivateKey, pair.getValue(), pair.getKey());
                bestPair = new Pair<>(
                        new LearnedPacket(holder.getPublicKey(), pair.getValue(), pair.getKey(), encryptedData), validatedPacketInfo.get(pair));
            }

        }
        return bestPair;
    }




}
