package learners;

import chain.Block;
import chain.Transaction;
import chain.User;
import common.Addresses;
import common.NodeType;
import common.Ports;
import common.Pair;
import packets.acceptances.AcceptedPacket;

import packets.acceptances.AcceptedUpdatePacket;
import packets.learnings.LearnedPacket;
import packets.proposals.UpdateUsersPacket;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Michael on 4/18/2018.
 */
public class ChainHolder extends Thread{
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int COLLISION_PREVENTING_TIMEOUT_TIME = 5000;
    private final static int MIN_COLLISION_PREVENTING_TIMEOUT_TIME = 500;
    private final static int TTL = 12;
    private final static int N = 4;
    private final static int f = (N-1)/3;
    private final String PRIVATE_KEY_PATH;
    private final User holder;
    private final RSAPrivateKey holderPrivateKey;
    private final InetAddress learningAddress;
    private final InetAddress updatingAddress;
    private final InetAddress checkingAddress;
    private final InetAddress finalAcceptanceAddress;
    private final InetAddress clientUpdateAddress;
    private final int learningPort;
    private final int updatingPort;
    private final int checkingPort;
    private final int finalAcceptancePort;
    private final int clientUpdatePort;
    private ConcurrentHashMap<RSAPublicKey, AcceptedPacket> acceptedPackets;
    private ConcurrentLinkedQueue<UpdateUsersPacket> updateRequests;


    public ChainHolder(User mybHolder, int fileID) throws IOException, ClassNotFoundException {
        super("ChainHolder: " + mybHolder.getID());
        this.PRIVATE_KEY_PATH = "localhome" + File.separator + "csc445" + File.separator + "group-A" + File.separator +
                "UserResources" + File.separator + "PRIVATE" + File.separator + NodeType.LEARNER + File.separator + "PK_" + fileID + ".dat";
        this.holder = mybHolder;
        this.holderPrivateKey = holder.loadPrivateKeyFromFile(PRIVATE_KEY_PATH);
        this.learningAddress = InetAddress.getByName(Addresses.HOLDER_LEARNING_ADDRESS);
        this.updatingAddress = InetAddress.getByName(Addresses.HOLDER_UPDATING_ADDRESS);
        this.checkingAddress = InetAddress.getByName(Addresses.HOLDER_CHECKING_ADDRESS);
        this.finalAcceptanceAddress = InetAddress.getByName(Addresses.HOLDER_ACCEPTANCE_ADDRESS);
        this.clientUpdateAddress = InetAddress.getByName(Addresses.CLIENT_UPDATE_ADDRESS);
        this.learningPort = Ports.HOLDER_LEARNING_PORT;
        this.updatingPort = Ports.HOLDER_UPDATING_PORT;
        this.checkingPort = Ports.HOLDER_CHECKING_PORT;
        this.finalAcceptancePort = Ports.HOLDER_ACCEPTANCE_PORT;
        this.clientUpdatePort = Ports.CLIENT_UPDATE_PORT;
        this.acceptedPackets = new ConcurrentHashMap<>(N);
        this.updateRequests = new ConcurrentLinkedQueue<>();
    }


    @Override
    public void run() {
        try {
            Thread learningThread = new Thread(() -> learn());
            Thread updatingThread = new Thread(() -> sendUpdates());

            learningThread.start();
            updatingThread.start();

            while (!learningThread.getState().equals(State.TERMINATED) && !updatingThread.getState().equals(State.TERMINATED)) {
                /*
                * SPIN
                * */
            }

            learningThread.join();
            updatingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void learn() {
        try {
            System.out.println("STARTING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learning Port:\t" + learningPort);

            MulticastSocket multicastSocket = new MulticastSocket(learningPort);
            multicastSocket.joinGroup(learningAddress);

            boolean running = true;

            while (running) {
                try {
                    //updateIfNecessary();
                    pruneAcceptedPackets();

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
                        boolean validated = validate(block, chainLength);

                        /*
                        * FIXME -- May need to remove the validation here
                        * */
                        if (validated && acceptedPacket.isHonest()) {
                            acceptedPackets.putIfAbsent(rsaPublicKey, acceptedPacket);

                            if (acceptedPackets.size() >= (2*f + 1)) {
                                LearnedPacket learnedPacket = reassure(acceptedPacket);
                                if (learnedPacket != null) {
                                    /*
                                    * Remove packets which are equal to or less than(?) the current packet
                                    * Remove packets which have lesser chainLength values
                                    *   otherPacket.compareTo(learnedPacket) takes care of this
                                    *       Remove if otherPacket.compareTo(learnedPacket) <= 0
                                    * */
                                    if (!updateIsNecessary(learnedPacket)) {
                                        record(learnedPacket);
                                        acknowledge(multicastSocket, learnedPacket);
                                        sendLearnedPacketToUsers(learnedPacket);
                                    }else {
                                        holder.updateBlockChain();
                                    }
                                }
                            }
                        }
                    }else if ((object != null) && (object instanceof UpdateUsersPacket)) {
                        UpdateUsersPacket updateUsersPacket = (UpdateUsersPacket) object;
                        updateRequests.add(updateUsersPacket);
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


    private void sendUpdates() {
        try {
            System.out.println("STARTING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learning Port:\t" + updatingPort);

            MulticastSocket multicastSocket = new MulticastSocket(updatingPort);
            multicastSocket.joinGroup(updatingAddress);

            boolean running = true;

            while (running) {
                UpdateUsersPacket updateUsersPacket = updateRequests.poll();

                if (updateUsersPacket != null) {
                    Block[] blocks = getBlocksForUpdate(updateUsersPacket);
                    AcceptedUpdatePacket updatePacket = new AcceptedUpdatePacket(blocks, updateUsersPacket.getUserAddress(),
                            updateUsersPacket.getUserPort());

                    sendUpdate(updatePacket, updatingAddress, updatingPort);
                }
            }

            multicastSocket.leaveGroup(updatingAddress);
            System.out.println("FINISHING:\t" + Thread.currentThread().getName());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void updateIfNecessary() throws IOException {
        HashMap<BigInteger, Integer> chainLengthFrequencies = sendAndReceiveChainLengths();
        BigInteger longestChain = filter(chainLengthFrequencies);
        if (longestChain.compareTo(holder.getBlockChain().getChainLength()) == 1) {
            holder.updateBlockChain();
        }
    }


    private void pruneAcceptedPackets() {
        ConcurrentHashMap<RSAPublicKey, AcceptedPacket> newestAcceptedPackets = new ConcurrentHashMap<>(N);
        AcceptedPacket newestPacket = null;

        for (RSAPublicKey publicKey : acceptedPackets.keySet()) {
            if (newestPacket == null) {
                newestPacket = acceptedPackets.get(publicKey);
            }else if (acceptedPackets.get(publicKey).getChainLength().compareTo(newestPacket.getChainLength()) > 0) {
                newestPacket = acceptedPackets.get(publicKey);
            }
        }

        for (RSAPublicKey publicKey : acceptedPackets.keySet()) {
            if (acceptedPackets.get(publicKey).getChainLength().compareTo(newestPacket.getChainLength()) == 0) {
                newestAcceptedPackets.put(publicKey, acceptedPackets.get(publicKey));
            }
        }

        acceptedPackets = newestAcceptedPackets;
    }


    private HashMap<BigInteger, Integer> sendAndReceiveChainLengths() {
        try {
            int receiveAttempts = 0;
            int timesReset = 0;
            BigInteger sendValue = holder.getBlockChain().getChainLength();
            HashMap<BigInteger, Integer> receivedChainLengths = new HashMap<>(N);
            receivedChainLengths.put(sendValue, 1);
            MulticastSocket multicastSocket = new MulticastSocket(checkingPort);
            multicastSocket.joinGroup(checkingAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(
                    ThreadLocalRandom.current().nextInt(MIN_COLLISION_PREVENTING_TIMEOUT_TIME, COLLISION_PREVENTING_TIMEOUT_TIME));
            ByteArrayInputStream bais = null;
            ObjectInputStream inputStream = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);
            Thread.sleep(ThreadLocalRandom.current().nextLong(COLLISION_PREVENTING_TIMEOUT_TIME));


            while ((receivedChainLengths.size() < (2*f + 1)) && (timesReset < N)) {
                outputStream.writeObject(sendValue);
                byte[] buf = baos.toByteArray();
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, checkingAddress, checkingPort);
                multicastSocket.send(datagramPacket);

                buf = new byte[multicastSocket.getReceiveBufferSize()];
                datagramPacket = new DatagramPacket(buf, buf.length);

                try {
                    multicastSocket.receive(datagramPacket);
                    bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                    inputStream = new ObjectInputStream(bais);

                    Object object = inputStream.readObject();
                    if ((object != null) && (object instanceof BigInteger)) {
                        BigInteger learnedValue = (BigInteger) object;
                        if (receivedChainLengths.containsKey(learnedValue)) {
                            Integer frequency = receivedChainLengths.get(learnedValue);
                            receivedChainLengths.put(learnedValue, frequency + 1);
                        }else {
                            receivedChainLengths.put(learnedValue, 1);
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
                }
            }

            inputStream.close();
            bais.close();
            multicastSocket.leaveGroup(checkingAddress);
            return receivedChainLengths;

        } catch (IOException |InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    private BigInteger filter(HashMap<BigInteger, Integer> chainLengthFrequencies) {
        Pair<BigInteger, Integer> mostFrequent = null;

        for (BigInteger chainLength : chainLengthFrequencies.keySet()) {
            if (mostFrequent == null) {
                mostFrequent = new Pair<>(chainLength, chainLengthFrequencies.get(chainLength));
            }else if (chainLengthFrequencies.get(chainLength) > mostFrequent.getValue()) {
                mostFrequent = new Pair<>(chainLength, chainLengthFrequencies.get(chainLength));
            }
        }

        return mostFrequent.getKey();
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


    private LearnedPacket reassure(AcceptedPacket acceptedPacket) throws IOException, NoSuchAlgorithmException,
            IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, BadPaddingException {
        BigInteger chainLength = acceptedPacket.getChainLength();
        Block verifiedBlock = acceptedPacket.getBlock();
        LearnedPacket learnedPacket = null;
        int attemptsToLearn = 0;
        int packetsLearned = 0;

        /*
        * FIXME --- Maybe also include a variable here - "attemptNumber" - as a control variable to keep from looping infinitely
        * */
        while ((packetsLearned < (2*f + 1)) && (attemptsToLearn < 2*N)) {
            byte[] encryptedData = LearnedPacket.encryptPacketData(holderPrivateKey, chainLength, verifiedBlock);
            LearnedPacket packetLearned = new LearnedPacket(holder.getPublicKey(), chainLength, verifiedBlock, encryptedData);
            HashMap<RSAPublicKey, LearnedPacket> packetsToValidate = sendAndReceivePackets(packetLearned);
            HashMap<LearnedPacket, Integer> validatedPackets = validatePackets(packetsToValidate);
            //Pair<LearnedPacket, Integer> bestPacket = determineBestBlock(validatedPackets);
            Pair<LearnedPacket, Integer> agreedUponPacketInfo = checkForConsensus(validatedPackets);

            if (agreedUponPacketInfo != null) {
                learnedPacket = agreedUponPacketInfo.getKey();
                chainLength = learnedPacket.getChainLength();
                verifiedBlock = learnedPacket.getBlock();
                packetsLearned = agreedUponPacketInfo.getValue();
            }
            attemptsToLearn++;
        }

        if (packetsLearned < (2*f + 1)) {
            return null;
        }else {
            return learnedPacket;
        }
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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);
            Thread.sleep(ThreadLocalRandom.current().nextLong(COLLISION_PREVENTING_TIMEOUT_TIME));

            /*
            * FIXME It may be that N here should actually be 2f+1
            * */
            while ((receivedPackets.size() < (2*f + 1)) && (timesReset <= N)) {
                outputStream.writeObject(packetToSend);
                byte[] buf = baos.toByteArray();
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, finalAcceptanceAddress, finalAcceptancePort);
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
                        if (learnedPacket.getPublicKey() != null && learnedPacket.getBlock() != null && learnedPacket.equals(packetToSend)) {
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
            return null;
        }
    }


    private HashMap<LearnedPacket, Integer> validatePackets(HashMap<RSAPublicKey, LearnedPacket> packetsToValidate)
            throws IOException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {

        HashMap<LearnedPacket, Integer> validatedPackets = new HashMap<>(N);
        for (RSAPublicKey publicKey : packetsToValidate.keySet()) {
            LearnedPacket packet = packetsToValidate.get(publicKey);
            if (validate(packet.getBlock(), packet.getChainLength()) && packet.isHonest()) {
                for (LearnedPacket learnedPacket : validatedPackets.keySet()) {
                    if (learnedPacket.equals(packet)) {
                        Integer currentValue = validatedPackets.get(learnedPacket);
                        validatedPackets.put(learnedPacket, (currentValue + 1));
                    }else {
                        validatedPackets.put(packet, 1);
                    }
                }
                if (validatedPackets.isEmpty()) {
                    validatedPackets.put(packet, 1);
                }
                /*if (validatedPackets.containsKey(currentPair)) {
                    Integer currentValue = validatedPackets.get(currentPair);
                    validatedPackets.put(currentPair, (currentValue + 1));
                }else {
                    validatedPackets.put(currentPair, 1);
                }*/
            }
        }
        return validatedPackets;
    }


    private Block determineBestBlock(HashMap<Pair<Block, BigInteger>, Integer> validatedPackets) {

        Block bestBlock = null;

        for (Pair<Block, BigInteger> blockPair : validatedPackets.keySet()) {
            Block block = blockPair.getKey();
            if (bestBlock == null) {
                bestBlock = block;
            } else if (bestBlock.compareTo(block) == -1){
                bestBlock = block;
            }
        }

        return bestBlock;
    }


    private Pair<LearnedPacket, Integer> checkForConsensus(HashMap<LearnedPacket, Integer> validatedPacketInfo)
            throws BadPaddingException, NoSuchAlgorithmException, IOException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException {

        Pair<LearnedPacket, Integer> bestPair = null;
        for (LearnedPacket learnedPacket : validatedPacketInfo.keySet()) {

            if ((validatedPacketInfo.get(learnedPacket) >= (2*f + 1)) && (bestPair == null)) {

                byte[] encryptedData = LearnedPacket.encryptPacketData(holderPrivateKey, learnedPacket.getChainLength(),
                        learnedPacket.getBlock());
                bestPair = new Pair<>(new LearnedPacket(holder.getPublicKey(), learnedPacket.getChainLength(),
                        learnedPacket.getBlock(), encryptedData), validatedPacketInfo.get(learnedPacket));

            }else if (validatedPacketInfo.get(learnedPacket) > bestPair.getValue()){

                byte[] encryptedData = LearnedPacket.encryptPacketData(holderPrivateKey, learnedPacket.getChainLength(),
                        learnedPacket.getBlock());
                bestPair = new Pair<>(new LearnedPacket(holder.getPublicKey(), learnedPacket.getChainLength(),
                        learnedPacket.getBlock(), encryptedData), validatedPacketInfo.get(learnedPacket));
            }

        }

        return bestPair;
    }


    private boolean updateIsNecessary(LearnedPacket learnedPacket) {
        return !holder.getBlockChain().getChainLength().equals(learnedPacket.getChainLength());
    }


    private void record(LearnedPacket learnedPacket) {
        holder.getBlockChain().addBlock(learnedPacket.getBlock());
    }


    private void acknowledge(MulticastSocket socket, LearnedPacket learnedPacket) throws BadPaddingException,
            NoSuchAlgorithmException, IOException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException {
        byte[] encryptedData =
                AcceptedPacket.encryptPacketData(holderPrivateKey, holder.getBlockChain().getChainLength(), learnedPacket.getBlock());
        AcceptedPacket acceptedPacket =
                new AcceptedPacket(holder.getPublicKey(), holder.getBlockChain().getChainLength(), learnedPacket.getBlock(), encryptedData);

        sendAcceptedPacket(socket, acceptedPacket, learningAddress, learningPort);

    }

    private int sendAcceptedPacket(MulticastSocket socket, AcceptedPacket acceptedPacket, InetAddress address, int port) {
        try {
            System.out.println("ACKNOWLEDGING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learner:\t" + holder.getFullName());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);

            outputStream.writeObject(acceptedPacket);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length, address, port);
            socket.send(datagramPacket);

            outputStream.close();
            baos.close();

            System.out.println("FINISHING ACKNOWLEDGING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learner:\t" + holder.getFullName());

            return 1;

        } catch (SocketTimeoutException ste) {
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private Block[] getBlocksForUpdate(UpdateUsersPacket updateUsersPacket) {
        return holder.getBlockChain().getSubChain(updateUsersPacket.getLastBlockRecorded());
    }


    private int sendUpdate(AcceptedUpdatePacket acceptedUpdatePacket, InetAddress address, int port) {
        try {
            System.out.println("SENDING UPDATE:\t" + Thread.currentThread().getName() + "\n" +
                    "Updating Port:\t" + port);

            MulticastSocket multicastSocket = new MulticastSocket(port);
            multicastSocket.joinGroup(address);
            multicastSocket.setTimeToLive(TTL);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);

            outputStream.writeObject(acceptedUpdatePacket);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length, address, port);
            multicastSocket.send(datagramPacket);

            outputStream.close();
            baos.close();
            multicastSocket.leaveGroup(address);
            System.out.println("FINISHING SENDING UPDATE:\t" + Thread.currentThread().getName() + "\n" +
                    "Updating Port:\t" + port);

            return 1;

        } catch (SocketTimeoutException ste) {
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }


    private int sendLearnedPacketToUsers(LearnedPacket learnedPacket) {
        try {
            System.out.println("UPDATING USERS:\t" + Thread.currentThread().getName() + "\n" +
                    "Learner:\t" + holder.getFullName());

            MulticastSocket multicastSocket = new MulticastSocket(clientUpdatePort);
            multicastSocket.joinGroup(clientUpdateAddress);
            multicastSocket.setTimeToLive(TTL);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);

            outputStream.writeObject(learnedPacket);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length, clientUpdateAddress, clientUpdatePort);
            multicastSocket.send(datagramPacket);

            outputStream.close();
            baos.close();
            multicastSocket.leaveGroup(clientUpdateAddress);

            System.out.println("FINISHING USER UPDATE:\t" + Thread.currentThread().getName() + "\n" +
                    "Learner:\t" + holder.getFullName());

            return 1;

        } catch (SocketTimeoutException ste) {
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }


}