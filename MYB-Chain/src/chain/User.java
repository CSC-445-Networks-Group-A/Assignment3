package chain;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Created by Michael on 4/14/2018.
 */
public class User {
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String firstName;
    private final String lastName;
    private final String ID;
    private BlockChain blockChain;
    private Double netWorth;

    public User(String firstName, String lastName, Double initialNetWorth) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.firstName = firstName;
        this.lastName = lastName;
        /*
        * TODO ----- Replace "this.ID = firstName + "_" + lastName;" with "this.id = generateID();" post creation
        * TODO ----- of an algorithm to create a Unique Identifier.
        *
        * TODO ----- NOTE: ID must remain a String to avoid restructuring of code elsewhere.
        * */
        this.ID = firstName + "_" + lastName;
        this.netWorth = initialNetWorth;


        /*
        * TODO ----- Replace "this.blockChain = null" with an attempt to load data from file.
        * TODO ----- Note: - if no file is found, ask user if they changed the file location. If not/they can't find it,
        * TODO -----         request to download the BlockChain.
        * TODO -----       - if the file IS found, update the existing chain before leaving Constructor.
        *
        * TODO ----- NOTE: ID must remain a String to avoid restructuring of code elsewhere.
        * */
        this.blockChain = null;

        updateBlockChain();

    }

    private User(RSAPrivateKey privateKey, RSAPublicKey publicKey, String firstName, String lastName, String id, Double netWorth) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.firstName = firstName;
        this.lastName = lastName;
        this.ID = id;
        this.netWorth = netWorth;
    }

    private String generateID() {
        //Generate a unique ID for the User using its name and private key
        final int idLength = 50; //Length of ID
        //How many chars to take from the firstname, lastname, and private key.
        int charsFromFirst = ThreadLocalRandom.current().nextInt(3, (firstName.length() + 1));
        int charsFromLast = ThreadLocalRandom.current().nextInt(3, (lastName.length() + 1));
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
        return new User(privateKey, publicKey, firstName, lastName, ID, netWorth);
    }


    public void updateBlockChain() {

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
