package chain;

import common.Addresses;
import common.Ports;
import packets.acceptances.AcceptedUpdatePacket;
import packets.requests.TransactionRequest;
import packets.requests.UpdateRequest;
import packets.responses.TransactionAccepted;
import packets.responses.TransactionDenied;
import packets.responses.TransactionPending;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Created by Michael on 4/14/2018.
 */
public class User extends Thread implements Serializable{
    private final static String BLOCKCHAIN_PATH = "UserResources" + File.separator + "BLOCKCHAIN";
    private final static String USER_INFO_PATH = "UserResources" + File.separator + "USER_INFO.dat";
    private final static String PRIVATE_KEY_PATH = "UserResources" + File.separator + "PRIVATE.dat";
    private final static String PUBLIC_KEY_PATH = "UserResources" + File.separator + "PUBLIC.dat";
    private final static int DESIRED_CHARS_FROM_NAMES = 3;
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int TTL = 12;
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final InetAddress requestAddress;
    private final InetAddress receiveUpdateAddress;
    private final String firstName;
    private final String lastName;
    private final String ID;
    private final int requestPort;
    private final int receiveUpdatePort;
    private HashMap<RSAPublicKey, User> knownBlockChainUsers;
    private BlockChain blockChain;
    private Double netWorth;


    public User(String firstName, String lastName, Double initialNetWorth) throws NoSuchAlgorithmException, IOException {
        super("User: " + firstName + " " + lastName);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.requestAddress = InetAddress.getByName(Addresses.USER_REQUEST_ADDRESS);
        this.receiveUpdateAddress = InetAddress.getByName(Addresses.USER_RECEIVE_UPDATE_ADDRESS);
        this.firstName = firstName;
        this.lastName = lastName;
        this.ID = generateID();
        this.requestPort = Ports.USER_REQUEST_PORT;
        this.receiveUpdatePort = Ports.USER_RECEIVE_UPDATE_PORT;
        this.knownBlockChainUsers = new HashMap<>(10);
        this.blockChain = new BlockChain(BLOCKCHAIN_PATH);
        this.netWorth = initialNetWorth;

    }

    private User(RSAPrivateKey privateKey, RSAPublicKey publicKey, String firstName, String lastName, String id, Double netWorth) throws IOException {
        super("User: " + firstName + " " + lastName);
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.requestAddress = InetAddress.getByName(Addresses.USER_REQUEST_ADDRESS);
        this.receiveUpdateAddress = InetAddress.getByName(Addresses.USER_RECEIVE_UPDATE_ADDRESS);
        this.firstName = firstName;
        this.lastName = lastName;
        this.ID = id;
        this.requestPort = Ports.USER_REQUEST_PORT;
        this.receiveUpdatePort = Ports.USER_RECEIVE_UPDATE_PORT;
        this.knownBlockChainUsers = new HashMap<>(10);
        this.blockChain = new BlockChain(BLOCKCHAIN_PATH);
        this.netWorth = netWorth;
    }

    private User(RSAPublicKey publicKey, RSAPrivateKey privateKey, String id, Double netWorth) throws IOException {
        super("User: " + id);
        //TO-DO: take RSAPublicKey and RSAPrivateKey here for loading file
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.requestAddress = InetAddress.getByName(Addresses.USER_REQUEST_ADDRESS);
        this.receiveUpdateAddress = InetAddress.getByName(Addresses.USER_RECEIVE_UPDATE_ADDRESS);
        this.firstName = null;
        this.lastName = null;
        this.ID = id;
        this.requestPort = Ports.USER_REQUEST_PORT;
        this.receiveUpdatePort = Ports.USER_RECEIVE_UPDATE_PORT;
        this.knownBlockChainUsers = new HashMap<>(10);
        this.blockChain = new BlockChain(BLOCKCHAIN_PATH);
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


    protected User clone() {
        try {
            return new User(privateKey, publicKey, firstName, lastName, ID, netWorth);
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
        adfawerfaw
    }


    public void updateBlockChain() {
        sendUpdateRequest();
        receiveUpdate();
    }


    /** send out an updateRequest to get the most recent copy of block chain
     *  through multicast
     * */
    private void sendUpdateRequest(){
        MulticastSocket multicastSocket = null;
        try {
            multicastSocket = new MulticastSocket(receiveUpdatePort);
            multicastSocket.joinGroup(receiveUpdateAddress);
            multicastSocket.setTimeToLive(TTL);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);

            //   FIXME: USER PORT??? FIXME: USER PORT??? FIXME: USER PORT???
            UpdateRequest updateRequestPacket = new UpdateRequest(this.blockChain.getChainLength(), InetAddress.getLocalHost(), );
            outputStream.writeObject(updateRequestPacket);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length);

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
    private void receiveUpdate(){

        try {

            MulticastSocket multicastSocket = null;
            multicastSocket = new MulticastSocket(receiveUpdatePort);
            multicastSocket.joinGroup(receiveUpdateAddress);
            multicastSocket.setTimeToLive(TTL);
            multicastSocket.setSoTimeout(TIMEOUT_MILLISECONDS);

            byte[] buf = new byte[multicastSocket.getReceiveBufferSize()];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
            multicastSocket.receive(datagramPacket);

            ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData(), 0, datagramPacket.getLength());
            ObjectInputStream inputStream = new ObjectInputStream(bais);

            Object object = inputStream.readObject();
            if ((object != null) && (object instanceof AcceptedUpdatePacket)) {
                AcceptedUpdatePacket acceptedUpdatePacket = (AcceptedUpdatePacket) object;
                Block[] blocksToAdd = acceptedUpdatePacket.getBlocksToUpdate();
                blockChain.update(blocksToAdd);

            }
            inputStream.close();
            bais.close();
            multicastSocket.leaveGroup(receiveUpdateAddress);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public void populateKnownUsersList(){
        for(Block block : blockChain.getBlocks()){
            for(Transaction transaction : block.getTransactions()){
                if(transaction.getBuyerID().equals(transaction.getSellerID()) && transaction.getTransactionAmount() == 0){
                    // this must be a registration transaction, me -(0.0)-> me
                    knownBlockChainUsers.putIfAbsent(transaction.getSeller().getPublicKey(), transaction.getSeller());
                }
            }
        }
    }

    public String commitUser() throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, IOException {
        String response = null;
        while (response != null) {
            response = makeTransaction(this, 0.0);
            writeUser();
        }
        return response;
    }

    public static boolean userFileExists(){
        File tmpDir = new File(USER_INFO_PATH);
        return tmpDir.exists();
    }

    public static User loadUser() throws IOException, ClassNotFoundException {

        FileInputStream userFileInput = new FileInputStream(USER_INFO_PATH);
        ObjectInputStream userFileObjectInput = new ObjectInputStream(userFileInput);
        User loadedUser = (User) userFileObjectInput.readObject();
        userFileObjectInput.close();
        userFileInput.close();

        return loadedUser;
    }


    private void writeUser() throws IOException {
        FileOutputStream publicFileOutput = new FileOutputStream(PUBLIC_KEY_PATH);
        ObjectOutputStream publicObjectOutput = new ObjectOutputStream(publicFileOutput);
        publicObjectOutput.writeObject(publicKey);
        publicObjectOutput.close();
        publicFileOutput.close();

        FileOutputStream privateFileOutput = new FileOutputStream(PRIVATE_KEY_PATH);
        ObjectOutputStream privateObjectOutput = new ObjectOutputStream(privateFileOutput);
        privateObjectOutput.writeObject(privateKey);
        privateObjectOutput.close();
        privateFileOutput.close();

        FileOutputStream userFileOutput = new FileOutputStream(USER_INFO_PATH);
        ObjectOutputStream userFileObjectOutput = new ObjectOutputStream(userFileOutput);
        userFileObjectOutput.writeObject(this);
        userFileObjectOutput.close();
        userFileOutput.close();
    }


    private static RSAPublicKey loadPublicKeyFromFile() throws IOException, ClassNotFoundException {
        FileInputStream publicFileInput = new FileInputStream(PUBLIC_KEY_PATH);
        ObjectInputStream publicObjectInput = new ObjectInputStream(publicFileInput);
        RSAPublicKey loadedPublicKey = (RSAPublicKey) publicObjectInput.readObject();
        publicObjectInput.close();
        publicFileInput.close();

        return loadedPublicKey;
    }


    public static RSAPrivateKey loadPrivateKeyFromFile() throws IOException, ClassNotFoundException {
        FileInputStream privateFileInput = new FileInputStream(PRIVATE_KEY_PATH);
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
            Transaction transaction = new Transaction(this, seller, transactionAmount, privateKey);

            /*
            * FIXME: This will very likely need rework. The InetAddress and Port provided should be the the way that TCP
            * FIXME: responses are sent to the Users. Here, the same port was used that the Multicast is happening on.
            * FIXME: That will, very likely, not work as we scale up.
            * FIXME: Furthermore, the Multicast group is not joined here.
            * */
            TransactionRequest transactionRequest = new TransactionRequest(transaction, InetAddress.getLocalHost(), requestPort);
            MulticastSocket multicastSocket = new MulticastSocket(requestPort);

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
            multicastSocket.close();


            String message = null;
            ServerSocket serverSocket = new ServerSocket(requestPort);
            Socket socket = serverSocket.accept();

            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            Object object = inputStream.readObject();
            inputStream.close();
            socket.close();
            serverSocket.close();

            if (object != null) {

                if ((object instanceof TransactionPending)) {
                    TransactionPending transationPending = (TransactionPending) object;
                    message = transationPending.getPendingMessage();
                }
                if ((object instanceof TransactionDenied)) {
                    TransactionDenied transationDenied = (TransactionDenied) object;
                    message = transationDenied.getDenialMessage();
                }
                if ((object instanceof TransactionAccepted)) {
                    TransactionAccepted transationAccepted = (TransactionAccepted) object;
                    message = transationAccepted.getAcceptanceMessage();
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

    public HashMap<RSAPublicKey, User> getKnownBlockChainUsers(){
        return this.knownBlockChainUsers;
    }

}
