package chain;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import packets.acceptances.AcceptedPacket;
import packets.requests.UpdateRequest;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Created by Michael on 4/14/2018.
 */
public class User {
    private final static String USER_INFO_PATH = "UserResources/USER_INFO.dat";
    private final static int DESIRED_CHARS_FROM_NAMES = 3;
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String firstName;
    private final String lastName;
    private final String ID;
    private BlockChain blockChain;
    private Double netWorth;
    private BigInteger lastUpdatedBlockNumber;
    private final static int TIMEOUT_MILLISECONDS = 20000;
    private final static int TTL = 12;
    private int requestPort;
    private int receiveUpdatePort;
    private InetAddress requestAddress;
    private InetAddress receiveUpdateAddress;


    public User(String firstName, String lastName, Double initialNetWorth, int requestPort, int receiveUpdatePort) throws NoSuchAlgorithmException {
        this.receiveUpdatePort = receiveUpdatePort;
        this.requestPort = requestPort;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.firstName = firstName;
        this.lastName = lastName;
        this.ID = generateID();
        this.netWorth = initialNetWorth;
        this.lastUpdatedBlockNumber = BigInteger.valueOf(0);
        /*
        * TODO ----- Replace "this.blockChain = null" with an attempt to load data from file.
        * TODO ----- Note: - if no file is found, ask user if they changed the file location. If not/they can't find it,
        * TODO -----         request to download the BlockChain.
        * TODO -----       - if the file IS found, update the existing chain before leaving Constructor.
        *
        * TODO ----- NOTE: ID must remain a String to avoid restructuring of code elsewhere.
        * */
        this.blockChain = null;

//        updateBlockChain();

    }


    private User(RSAPrivateKey privateKey, RSAPublicKey publicKey, String firstName, String lastName, String id, Double netWorth, InetAddress requestAddress) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.firstName = firstName;
        this.lastName = lastName;
        this.ID = id;
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
        return new User(privateKey, publicKey, firstName, lastName, ID, netWorth, requestAddress);
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

            UpdateRequest updateRequestPacket = new UpdateRequest(this.lastUpdatedBlockNumber);
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
     * receiving newly updated blockchain object? Blocks?
     */
    private BlockChain receiveUpdate(){

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
    }

    public void updateBlockChain() {
        //TODO: generalized path?
        String path = "blk.dat";
        File file = new File(path);
        if(file.exists()){
            // if file/a copy of the block chain (possibly an old version) already exists on user's machine
            //TODO:
            readAndUpdateBlockChainFrom(path);

        }else{
            // no copy of the blockchain exist
            downloadBlockChainTo(path);

        }

    }

    private void downloadBlockChainTo(String path){
        //send out a UPDATEREQUEST TO ALL
        sendUpdateRequest();
        //whatever returned
        this.blockChain = receiveUpdate();
        //save own copy in local
        File f = new File(path);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            //writing older blocks first
            for(int i = blockChain.getChainLength().intValueExact()-1; i >=0; i --){
//                oos.writeObject(blockChain.getBlocks().get(i));
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



        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void login(){

//        if(){
//
//        }
//
//        update();
    }

    public User loadUser() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject a = (JSONObject) parser.parse(new FileReader("UserResources/USER_INFO.dat"));

        String loadedPublicKey = (String) a.get("publicKey");
        System.out.println(loadedPublicKey);

        String loadedID = (String) a.get("ID");
        System.out.println(loadedID);

        Double loadedNetWorth = (Double) a.get("netWorth");
        System.out.println(loadedNetWorth);

        long loadedLastUpdatedBlockNumber = (long) a.get("lastUpdatedBlockNumber");
        System.out.println(loadedLastUpdatedBlockNumber);

        long loadedRequestPort = (long) a.get("requestPort");
        System.out.println(loadedRequestPort);

        String loadedRequestAddress = (String) a.get("requestAddress");
        System.out.println(loadedRequestAddress);

        String loadedReceiveUpdateAddress = (String) a.get("receiveUpdateAddress");
        System.out.println(loadedReceiveUpdateAddress);

        long loadedReceiveUpdatePort = (long) a.get("receiveUpdatePort");
        System.out.println(loadedReceiveUpdatePort);

        return null;
    }


    public void writeUser() throws IOException {
        JSONObject obj = new JSONObject();
//        obj.put("publicKey", this.publicKey.getEncoded());
        obj.put("ID", this.ID);
        obj.put("netWorth", this.netWorth);
        obj.put("lastUpdatedBlockNumber", this.lastUpdatedBlockNumber);
        obj.put("requestPort", this.requestPort);
        obj.put("requestAddress", this.requestAddress);
        obj.put("receiveUpdateAddress", this.receiveUpdateAddress);
        obj.put("receiveUpdatePort", this.receiveUpdatePort);

        FileWriter file = new FileWriter("UserResources/USER_INFO.dat");
        file.write(obj.toJSONString());
        file.close();
    }



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

}
