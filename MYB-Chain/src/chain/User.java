package chain;

import common.Addresses;
import common.Ports;
import packets.requests.UpdateRequest;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Created by Michael on 4/14/2018.
 */
public class User implements Serializable{
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
    private BlockChain blockChain;
    private Double netWorth;
    private BigInteger lastUpdatedBlockNumber;


    public User(String firstName, String lastName, Double initialNetWorth) throws NoSuchAlgorithmException, UnknownHostException {
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
        this.receiveUpdatePort = Ports.USER_RECEIVE_UPDATE_PORT;
        this.requestPort = Ports.USER_REQUEST_PORT;
        this.blockChain = null;
        this.netWorth = initialNetWorth;
        this.lastUpdatedBlockNumber = BigInteger.valueOf(0);
        /*
        * TODO ----- Replace "this.blockChain = null" with an attempt to load data from file.
        * TODO ----- Note: - if no file is found, ask user if they changed the file location. If not/they can't find it,
        * TODO -----         request to download the BlockChain.
        * TODO -----       - if the file IS found, update the existing chain before leaving Constructor.
        * */

       updateBlockChain();

    }


    private User(RSAPrivateKey privateKey, RSAPublicKey publicKey, String firstName, String lastName, String id, Double netWorth) throws UnknownHostException {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.requestAddress = InetAddress.getByName(Addresses.USER_REQUEST_ADDRESS);
        this.receiveUpdateAddress = InetAddress.getByName(Addresses.USER_RECEIVE_UPDATE_ADDRESS);
        this.firstName = firstName;
        this.lastName = lastName;
        this.ID = id;
        this.receiveUpdatePort = Ports.USER_RECEIVE_UPDATE_PORT;
        this.requestPort = Ports.USER_REQUEST_PORT;
        this.netWorth = netWorth;
    }

    private User(RSAPublicKey publicKey, RSAPrivateKey privateKey, String id, Double netWorth, BigInteger lastUpdatedBlockNumber) throws UnknownHostException {
        //TO-DO: take RSAPublicKey and RSAPrivateKey here for loading file
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.requestAddress = InetAddress.getByName(Addresses.USER_REQUEST_ADDRESS);
        this.receiveUpdateAddress = InetAddress.getByName(Addresses.USER_RECEIVE_UPDATE_ADDRESS);
        this.firstName = null;
        this.lastName = null;
        this.ID = id;
        this.receiveUpdatePort = Ports.USER_RECEIVE_UPDATE_PORT;
        this.requestPort = Ports.USER_REQUEST_PORT;
        this.netWorth = netWorth;
        this.lastUpdatedBlockNumber = lastUpdatedBlockNumber;
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
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** send out an updateRequest to get the most recent copy of block chain
     *  through multicast
     * */
    private void sendUpdateRequest(){
        MulticastSocket multicastSocket = null;
        try {
            multicastSocket = new MulticastSocket(requestPort);
            multicastSocket.joinGroup(requestAddress);
            multicastSocket.setTimeToLive(TTL);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(baos);

            //   FIXME: USER PORT??? FIXME: USER PORT??? FIXME: USER PORT???
            UpdateRequest updateRequestPacket = new UpdateRequest(this.lastUpdatedBlockNumber, InetAddress.getLocalHost(),);
            outputStream.writeObject(updateRequestPacket);
            byte[] output = baos.toByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(output, output.length);

            //send updateRequest packet
            multicastSocket.send(datagramPacket);

            outputStream.close();
            baos.close();

            //leaving the group ...
            multicastSocket.leaveGroup(requestAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * receiving newly updated blockchain object?
     */
  /*  private BlockChain receiveUpdate(){

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
            BlockChain newBlockChain = null;
            if ((object != null) && (object instanceof BlockChain)) {
                newBlockChain = (BlockChain) object;
            }
            inputStream.close();
            bais.close();
            return newBlockChain;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }*/

    public void updateBlockChain() {
        //TODO: generalized path?
        String filePath = BLOCKCHAIN_PATH + File.separator + "CHAIN.dat";
        File file = new File(filePath);
        if(file.exists()){
            // if file/a copy of the block chain (possibly an old version) already exists on user's machine
            //TODO:
            readAndUpdateBlockChainFrom(filePath);

        }else{
            // no copy of the blockchain exist
            File blockchainDirectory = new File(BLOCKCHAIN_PATH);
            if (!file.exists() || !blockchainDirectory.isDirectory()) {
                blockchainDirectory.mkdirs();
            }
            downloadBlockChainTo(filePath);

        }

    }

    private void downloadBlockChainTo(String path){
        //send out a UPDATEREQUEST TO ALL
        //TODO:
        //sendUpdateRequest();
        //whatever returned
        //this.blockChain = receiveUpdate();
        //save own copy in local
        File f = new File(path);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            //writing older blocks first
            for(int i = blockChain.getChainLength().intValueExact()-1; i >=0; i --){
                oos.writeObject(blockChain.getBlocks().get(i));
                oos.flush();
            }

            //write an null object to indicate EOF
            //had to do this because readObject doesn't return null or it will throw an EOP exception
            Object eof = null;
            oos.writeObject(eof);
            oos.flush();

            fos.close();
            oos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readAndUpdateBlockChainFrom(String path){
        BlockChain bc = new BlockChain(path,true); //for loggined user who already has a copy of the block chain
        File f = new File(path);
        FileInputStream fis = null;

        try {

            fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            boolean eof = false;
            while(!eof) {
                Object readObj = ois.readObject();
                Block readBlock = null;
                if (readObj != null && readObj instanceof Block) {
                    readBlock = (Block) readObj;

                    //TODO: add block without verification？ since it is older version of the blockchain?
                    bc.addBlock(readBlock);

                }else{
                    //either null or is not an Block object
                    //terminate while loop
                    eof = true;
                }
            } //end while loop



        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void commitUser() throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, IOException {
        makeTransaction(this, 0.0);
        writeUser();
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
//        JSONParser parser = new JSONParser();
//
//        try {
//            JSONObject userJson = (JSONObject) parser.parse(new FileReader(USER_INFO_PATH));
//
//            String loadedID = (String) userJson.get("ID");
//            Double loadedNetWorth = (Double) userJson.get("netWorth");
//            BigInteger loadedLastUpdatedBlockNumber = BigInteger.valueOf((long) userJson.get("lastUpdatedBlockNumber"));
//
//            InetAddress loadedRequestAddress = InetAddress.getByName((String) userJson.get("requestAddress"));
//            Long loadedRequestPortLong = (Long) userJson.get("requestPort");
//            int loadedRequestPort = loadedRequestPortLong.intValue();
//
//            InetAddress loadedReceiveUpdateAddress = InetAddress.getByName((String) userJson.get("receiveUpdateAddress"));
//            Long loadedReceiveUpdatePortLong = (Long) userJson.get("receiveUpdatePort");
//            int loadedReceiveUpdatePort = loadedReceiveUpdatePortLong.intValue();
//
//            RSAPublicKey loadedPublicKey = User.loadPublicKeyFromFile();
//            RSAPrivateKey loadedPrivateKey = User.loadPrivateKeyFromFile();
//
//            return new User(loadedPublicKey, loadedPrivateKey, loadedID, loadedNetWorth, loadedLastUpdatedBlockNumber);
//        }catch(IOException | ParseException | ClassNotFoundException ex){
//            ex.printStackTrace();
//            return null;
//        }
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


//        JSONObject userJson = new JSONObject();
//        userJson.put("ID", this.ID);
//        userJson.put("netWorth", this.netWorth);
//        userJson.put("lastUpdatedBlockNumber", this.lastUpdatedBlockNumber);
//        userJson.put("requestAddress", this.requestAddress.getHostAddress());
//        userJson.put("requestPort", this.requestPort);
//        userJson.put("receiveUpdateAddress", this.receiveUpdateAddress.getHostAddress());
//        userJson.put("receiveUpdatePort", this.receiveUpdatePort);
//
//        FileWriter userFile = new FileWriter(USER_INFO_PATH);
//        userFile.write(userJson.toJSONString());
//        userFile.close();
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
    public Transaction makeTransaction(User seller, Double transactionAmount) throws IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidParameterException {
        return new Transaction(this, seller, transactionAmount, privateKey);
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

  //  public InetAddress getReceiveUpdateAddress() {
  //      return receiveUpdateAddress;
  //  }

   /* public int getReceiveUpdatePort() {
        return receiveUpdatePort;
    }*/
}
