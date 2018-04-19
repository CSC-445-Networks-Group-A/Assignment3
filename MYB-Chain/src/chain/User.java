package chain;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;


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

    /*
    * TODO ----- determine Unique Identifier generating algorithm
    * */
    private String generateID() {
        return null;
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
