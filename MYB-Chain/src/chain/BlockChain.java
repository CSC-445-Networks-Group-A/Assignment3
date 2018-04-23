package chain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Michael on 4/14/2018.
 */
public class BlockChain {
    private static long INITIAL_WORTH = 2000000000;
    private HashMap<User, Double> users;
    private ArrayList<Block> chain;
    private BigDecimal totalWorth;

    /**
     * Constructor for first usage, ever.
     * */
    public BlockChain(BigDecimal initialCoinCount) {

    }

    /**
     * Constructor for usage by a new User
     * */
    public BlockChain(String directoryToStoreBlockChain) {

    }


    /**
     * Constructor for a return User, who is either logged in, or who has some version of the BlockChain stored in the
     * provided directory.
     * */
    public BlockChain(String storageDirectory, boolean loggedIn) {

    }


    /**
     * Returns a byte[] representing the initial Users of the BlockChain being created.
     * */
    private byte[] getUserBytes() {
        byte[][] allUserBytes = new byte[users.size()][];
        int index = 0;
        int totalBytes = 0;

        for (User user : users.keySet()) {
            allUserBytes[index] = user.getID().getBytes();
            totalBytes += allUserBytes[index].length;
        }

        byte[] userBytes = new byte[totalBytes];
        for (int i = 0; i < allUserBytes.length; i++) {
            for (int j = 0; j < allUserBytes[i].length; j++) {
                userBytes[i+j] = allUserBytes[i][j];
            }
        }

        return userBytes;
    }


    /**
     * Computes the initial hash to be passed to the first created Block of the BlockChain.
     * */
    private byte[] computeInitialHash() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(getUserBytes());
    }


    /**
     * Used to apportion some number of Coins to the provided investor.
     *
     * Note: This method is only callable in the case that the number of coins being offered remain to be apportioned to
     * any given User.
     * */
    protected void conductICO(User investor) {

    }

    /**
     * Adds a Block to the BlockChain.
     * */
    protected void addBlock(Block block) {

    }


    public byte[] getMostRecentHash() {
        return chain.get(0).getProofOfWork();
    }

    public BigInteger getChainLength() {
        return new BigInteger(new Integer(chain.size()).toString());
    }

    public Block getMostRecentBlock() {
        return chain.get(0);
    }

}
