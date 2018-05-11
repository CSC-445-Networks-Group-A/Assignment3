package chain;

import common.Addresses;
import common.Ports;
import packets.acceptances.AcceptedUpdatePacket;
import packets.learnings.LearnedPacket;
import packets.requests.TransactionRequest;
import packets.requests.UpdateRequest;
import packets.responses.TransactionAccepted;
import packets.responses.TransactionDenied;
import packets.responses.TransactionPending;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Created by Michael on 4/14/2018.
 */
public class User extends Thread implements Serializable{
    private transient final static int DESIRED_CHARS_FROM_NAMES = 3;
    private final static int TTL = 12;
    private transient final String userInfoFileName;
    private transient final String privateKeyFileName;
    private transient final String publicKeyFileName;
    private transient final String blockChainFileName;
    private final InetAddress requestAddress;
    private final InetAddress receiveUpdateAddress;
    private final String firstName;
    private final String lastName;
    private final String ID;
    private final int requestPort;
    private final int receiveUpdatePort;
    private transient RSAPublicKey publicKey;
    private transient RSAPrivateKey privateKey;
    private transient HashMap<String, User> knownBlockChainUsers;
    private transient BlockChain blockChain;
    private Double netWorth;

    /**
     * Constructor of User
     * */
    public User(String userFileName, String privateFileName, String publicFileName, String blockChainFileName,
                String firstName, String lastName, Double initialNetWorth) throws NoSuchAlgorithmException, IOException {
        super("User: " + firstName + " " + lastName);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.requestAddress = InetAddress.getByName(Addresses.USER_REQUEST_ADDRESS);
        this.receiveUpdateAddress = InetAddress.getByName(Addresses.USER_RECEIVE_UPDATE_ADDRESS);
        this.userInfoFileName = userFileName;
        this.privateKeyFileName = privateFileName;
        this.publicKeyFileName = publicFileName;
        this.blockChainFileName = blockChainFileName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.ID = generateID();
        this.requestPort = Ports.USER_REQUEST_PORT;
        this.receiveUpdatePort = Ports.USER_RECEIVE_UPDATE_PORT;
        this.knownBlockChainUsers = new HashMap<>(10);
        this.blockChain = new BlockChain(blockChainFileName);
        this.netWorth = initialNetWorth;

        knownBlockChainUsers.putIfAbsent(ID, this);
    }

    private User(String userFileName, String privateFileName, String publicFileName, String blockChainFileName,
                 RSAPrivateKey privateKey, RSAPublicKey publicKey, String firstName, String lastName, String id, Double netWorth) throws IOException {
        super("User: " + firstName + " " + lastName);
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.requestAddress = InetAddress.getByName(Addresses.USER_REQUEST_ADDRESS);
        this.receiveUpdateAddress = InetAddress.getByName(Addresses.USER_RECEIVE_UPDATE_ADDRESS);
        this.userInfoFileName = userFileName;
        this.privateKeyFileName = privateFileName;
        this.publicKeyFileName = publicFileName;
        this.blockChainFileName = blockChainFileName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.ID = id;
        this.requestPort = Ports.USER_REQUEST_PORT;
        this.receiveUpdatePort = Ports.USER_RECEIVE_UPDATE_PORT;
        this.knownBlockChainUsers = new HashMap<>(10);
        this.blockChain = new BlockChain(blockChainFileName);
        this.netWorth = netWorth;
    }


    /**
     * Generates a unique ID for the User, using their Names and PrivateKey
     * */
    private String generateID() {
        //Length of ID
        final int idLength = 50;
        //How many chars to take from the firstname, lastname, and private key.
        int origin = firstName.length() < DESIRED_CHARS_FROM_NAMES ? firstName.length() : DESIRED_CHARS_FROM_NAMES;
        int charsFromFirst = ThreadLocalRandom.current().nextInt(origin, (firstName.length() + 1));
        int charsFromLast = ThreadLocalRandom.current().nextInt(origin, (lastName.length() + 1));
        int charsFromPrivateKey = idLength - (charsFromLast + charsFromFirst);

        StringBuilder sb = new StringBuilder();
        int idIndex = 0;
        while(idIndex < idLength){

            int pick = 1;
            //avoid wasting time cycling though randoms
            if(charsFromFirst != 0 || charsFromLast != 0){
                pick  = ThreadLocalRandom.current().nextInt(1, 4);
            }

            if(pick == 1 && charsFromPrivateKey > 0){
                //scramble the use of the private key to avoid revealing any parts of it in order.
                sb.append(privateKey.getModulus().toString().charAt(ThreadLocalRandom.current().nextInt(0, (privateKey.getModulus().toString().length() ))));
                idIndex++;
                charsFromPrivateKey--;
            } else if(pick == 2 && charsFromFirst > 0){
                sb.append(firstName.charAt(charsFromFirst - 1));
                idIndex++;
                charsFromFirst--;
            }else if(pick == 3 && charsFromLast > 0){
                sb.append(lastName.charAt(charsFromLast - 1));
                idIndex++;
                charsFromLast--;
            }
        }

        return sb.toString();
    }


    /*private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(firstName);
        out.writeUTF(lastName);
        out.writeUTF(ID);
        out.writeDouble(netWorth);
    }


    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        firstName = in.readUTF();
        lastName = in.readUTF();
        ID = in.readUTF();
        netWorth = in.readDouble();
    }


    private void readObjectNoData() throws ObjectStreamException {
        System.out.println("No Data Retrieved");
    }*/


    protected User clone() {
        try {
            //fixme --- Is blockChainFileName needed?..
            return new User("", "","", "", privateKey, publicKey, firstName, lastName, ID, netWorth);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    /**
     * This will continuously receive blocks added to the chain
     * */
    public void run() {
        boolean running = true;

        System.out.println("Receiving Update Packets:\t" + Thread.currentThread().getName() + "\n" +
                "Receive Port:\t" + receiveUpdatePort);
        try {
            MulticastSocket multicastSocket = new MulticastSocket(receiveUpdatePort);
            multicastSocket.joinGroup(receiveUpdateAddress);
            multicastSocket.setTimeToLive(TTL);

            while (running) {
                try {
                    byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
                    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(datagramPacket);
                    ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
                    ObjectInputStream inputStream = new ObjectInputStream(bais);

                    Object object = inputStream.readObject();
                    LearnedPacket learnedPacket = null;
                    if ((object != null) && (object instanceof LearnedPacket)) {
                        learnedPacket = ((LearnedPacket) object);
                        Block nextBlock = learnedPacket.getBlock();
                        boolean upToDate = compareProofOfWork(blockChain.getMostRecentBlock().getProofOfWork(), nextBlock.getPreviousHash());
                        if (!upToDate) {
                            updateBlockChain();
                        }else {
                            blockChain.addBlock(nextBlock);
                        }
                        blockChain.persist();
                    }
                    inputStream.close();
                    bais.close();
                } catch (SocketTimeoutException ste) {
                    ste.printStackTrace();
                }
            }

            multicastSocket.leaveGroup(receiveUpdateAddress);
            System.out.println("FINISHING LEARNING:\t" + Thread.currentThread().getName() + "\n" +
                    "Learning Port:\t" + receiveUpdatePort);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private boolean compareProofOfWork(byte[] lastKnownProofOfWork, byte[] previousProofOfWork) {
        if (lastKnownProofOfWork.length != previousProofOfWork.length) {
            return false;
        }

        for (int i = 0; i < lastKnownProofOfWork.length; i++) {
            if (lastKnownProofOfWork[i] != previousProofOfWork[i]) {
                return false;
            }
        }
        return true;
    }


    public void updateBlockChain() throws IOException {
        /*InetAddress address = InetAddress.getLocalHost();
        address.getHostAddress();
        address.getHostName();*/

        ServerSocket serverSocket = new ServerSocket(0);
        InetAddress address = InetAddress.getLocalHost();
        int receivePort = serverSocket.getLocalPort();
        //InetAddress address = serverSocket.getInetAddress();
        sendUpdateRequest(address, receivePort);
        receiveUpdate(serverSocket);

        populateKnownUsersList();
    }


    /** send out an updateRequest to get the most recent copy of block chain
     *  through multicast
     * */
    private void sendUpdateRequest(InetAddress address, int receivePort){
        MulticastSocket multicastSocket = null;
        try {
            multicastSocket = new MulticastSocket(receiveUpdatePort);
            multicastSocket.joinGroup(receiveUpdateAddress);
            multicastSocket.setTimeToLive(TTL);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);

            //   FIXME: USER PORT??? FIXME: USER PORT??? FIXME: USER PORT???
            UpdateRequest updateRequestPacket = new UpdateRequest(this.blockChain.getChainLength(), address, receivePort);
            outputStream.writeObject(updateRequestPacket);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length, receiveUpdateAddress, receiveUpdatePort);

            //send updateRequest packet
            multicastSocket.send(datagramPacket);

            outputStream.close();
            baos.close();

            //leaving the group ...
            multicastSocket.leaveGroup(receiveUpdateAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * receiving newly updated blockchain object?
     */
    private void receiveUpdate(ServerSocket serverSocket){

        try {
            Socket socket = serverSocket.accept();
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            Object object = inputStream.readObject();
            inputStream.close();
            socket.close();
            serverSocket.close();

            if ((object != null) && (object instanceof AcceptedUpdatePacket)) {
                AcceptedUpdatePacket acceptedUpdatePacket = (AcceptedUpdatePacket) object;
                Block[] blocksToAdd = acceptedUpdatePacket.getBlocksToUpdate();
                blockChain.update(blocksToAdd);

            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs through the blockchain and finds all known users
     * */
    public void populateKnownUsersList(){
        for(Block block : blockChain.getBlocks()){
            for(Transaction transaction : block.getTransactions()){
                if(transaction.getBuyerID().equals(transaction.getSellerID()) && transaction.getTransactionAmount() == 0){
                    // this must be a registration transaction, me -(0.0)-> me
                    knownBlockChainUsers.putIfAbsent(transaction.getSeller().getID(), transaction.getSeller());
                }
            }
        }
    }

    /**
     * makes a blank transaction, thus adding user to the chain, and writes the users files
     * */
    public String commitUser() throws IOException {
        String response = makeTransaction(this, 0.0);
        while(response == null){
            response = makeTransaction(this, 0.0);
        }

        persist();

        return response;
    }


    /**
     * Saves the user, public and private keys to a file
     * */
    public void persist() throws IOException {
        FileOutputStream publicFileOutput = new FileOutputStream(publicKeyFileName);
        ObjectOutputStream publicObjectOutput = new ObjectOutputStream(publicFileOutput);
        publicObjectOutput.writeObject(publicKey);
        publicObjectOutput.close();
        publicFileOutput.close();

        FileOutputStream privateFileOutput = new FileOutputStream(privateKeyFileName);
        ObjectOutputStream privateObjectOutput = new ObjectOutputStream(privateFileOutput);
        privateObjectOutput.writeObject(privateKey);
        privateObjectOutput.close();
        privateFileOutput.close();

        FileOutputStream userFileOutput = new FileOutputStream(userInfoFileName);
        ObjectOutputStream userFileObjectOutput = new ObjectOutputStream(userFileOutput);
        userFileObjectOutput.writeObject(this);
        userFileObjectOutput.close();
        userFileOutput.close();
    }


    /**
     * Reads the serialized user object from a file
     * */
    public static User loadUser(String userInfoFileName, String privateKeyFileName, String publicKeyFileName, String blockChainFileName) throws IOException, ClassNotFoundException {

        FileInputStream userFileInput = new FileInputStream(userInfoFileName);
        ObjectInputStream userFileObjectInput = new ObjectInputStream(userFileInput);
        User loadedUser = (User) userFileObjectInput.readObject();
        userFileObjectInput.close();
        userFileInput.close();

        loadedUser.privateKey = loadPrivateKeyFromFile(privateKeyFileName);
        loadedUser.publicKey = loadPublicKeyFromFile(publicKeyFileName);
        loadedUser.blockChain = new BlockChain(blockChainFileName);
        loadedUser.knownBlockChainUsers = new HashMap<>(10);
        loadedUser.knownBlockChainUsers.putIfAbsent(loadedUser.getID(), loadedUser);
        return loadedUser;
    }

    /**
     * Reads the serialized public key from a file
     * */
    private static RSAPublicKey loadPublicKeyFromFile(String path) throws IOException, ClassNotFoundException {
        FileInputStream publicFileInput = new FileInputStream(path);
        ObjectInputStream publicObjectInput = new ObjectInputStream(publicFileInput);
        RSAPublicKey loadedPublicKey = (RSAPublicKey) publicObjectInput.readObject();
        publicObjectInput.close();
        publicFileInput.close();

        return loadedPublicKey;
    }


    /**
     * Read the serialized private key from a file
     * */
    public static RSAPrivateKey loadPrivateKeyFromFile(String path) throws IOException, ClassNotFoundException {
        FileInputStream privateFileInput = new FileInputStream(path);
        ObjectInputStream privateObjectInput = new ObjectInputStream(privateFileInput);
        RSAPrivateKey loadedPrivateKey = (RSAPrivateKey) privateObjectInput.readObject();
        privateObjectInput.close();
        privateFileInput.close();

        return loadedPrivateKey;
    }


    /**
     * This function is responsible for creating a Transaction object with the specified seller, for the specified
     * transactionAmount, where upon the completion of a successful Transaction, the specified transactionAmount will be
     * sent to the seller.
     * TODO
     * Note that this includes creating a Transaction and sending a TransactionRequest to a Miner via the
     * USER_REQUEST_ADDRESS and USER_REQUEST_PORT in the Addresses and Ports classes respectively. The User will then
     * wait to hear back from the Miner via TCP over the provided InetAddress and port specified in the TransactionRequest.
     * */
    public String makeTransaction(User seller, Double transactionAmount) {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            InetAddress address = InetAddress.getLocalHost();
            int receivePort = serverSocket.getLocalPort();
            Transaction transaction = new Transaction(this, seller, transactionAmount, privateKey);
            TransactionRequest transactionRequest = new TransactionRequest(transaction, address, receivePort);
            MulticastSocket multicastSocket = new MulticastSocket(requestPort);
            multicastSocket.joinGroup(requestAddress);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);
            outputStream.writeObject(transactionRequest);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length, requestAddress, requestPort);

            System.out.print("Sending TransactionRequest... ");
            multicastSocket.send(datagramPacket);
            System.out.println("TransactionRequest sent.");

            outputStream.close();
            baos.close();
            multicastSocket.leaveGroup(requestAddress);


            String message = null;
            Socket socket = serverSocket.accept();
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            Object object = inputStream.readObject();
            inputStream.close();
            socket.close();
            serverSocket.close();

            if (object != null) {

                if ((object instanceof TransactionPending)) {
                    TransactionPending transactionPending = (TransactionPending) object;
                    message = transactionPending.getPendingMessage();
                }else if ((object instanceof TransactionDenied)) {
                    TransactionDenied transactionDenied = (TransactionDenied) object;
                    message = transactionDenied.getDenialMessage();
                }else if ((object instanceof TransactionAccepted)) {
                    TransactionAccepted transactionAccepted = (TransactionAccepted) object;
                    message = transactionAccepted.getAcceptanceMessage();
                }

                if(message != null){
                    System.out.println("Got Response msg: " + message);
                }
            }

            return message;

        } catch (IOException | ClassNotFoundException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            return null;
        }
    }


    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getID() {
        return ID;
    }

    public double getNetWorth() {
        return netWorth;
    }

    public BlockChain getBlockChain() {
        return blockChain;
    }

    public HashMap<String, User> getKnownBlockChainUsers(){
        return this.knownBlockChainUsers;
    }

}
