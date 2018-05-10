package chain;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Michael on 4/14/2018.
 */
public class BlockChain implements Serializable {
    private final static double INITIAL_WORTH = 2000000000.00;
    private final String BLOCK_CHAIN_FILE_NAME;
    private ArrayList<Block> chain;
    private double totalWorth;

    /**
     * Constructor for usage by a Client
     * */
    public BlockChain(String blockChainFileName) throws IOException {
        this.BLOCK_CHAIN_FILE_NAME = blockChainFileName;
        File blockChainFile = new File(blockChainFileName);
        if (!blockChainFile.exists() && !blockChainFileName.equalsIgnoreCase("")) {
            /*
            * First time access by user
            * */
            blockChainFile.createNewFile();
            this.chain = new ArrayList<>();
            this.totalWorth = INITIAL_WORTH;

        }else if (blockChainFileName.equalsIgnoreCase("")) {
            this.chain = new ArrayList<>();
            this.totalWorth = INITIAL_WORTH;
        }else {
            /*blockChainFile.delete();
            blockChainFile.createNewFile();*/
            this.chain = new ArrayList<>();
            readBlockChainFromFile();
            this.totalWorth = computeTotalChainWorth();
        }
    }


    private void readBlockChainFromFile(){
        File f = new File(BLOCK_CHAIN_FILE_NAME);
        FileInputStream fis = null;

        try {

            //TODO: do another of layer here
            if(f.length()!=0) {
                fis = new FileInputStream(f);
                ObjectInputStream ois = new ObjectInputStream(fis);
                boolean eof = false;
                while (!eof) {
                    Object readObj = ois.readObject();
                    Block readBlock = null;
                    if (readObj != null && readObj instanceof Block) {
                        readBlock = (Block) readObj;

                        //TODO: add block without verification？ since it is older version of the blockchain?
                        this.addBlock(readBlock);

                    } else {
                        //either null or is not an Block object
                        //terminate while loop
                        eof = true;
                    }
                } //end while loop

            }else{
                System.out.println("File empty...");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }


    public void persist(){
        File f = new File(BLOCK_CHAIN_FILE_NAME);
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            //writing older blocks first
            for(int i = chain.size() - 1; i >=0; i --){
                oos.writeObject(this.getBlocks().get(i));
                oos.flush();
            }

            //write an null object to indicate EOF
            //had to do this because readObject doesn't return null or it will throw an EOP exception

            Object eof = null;
            oos.writeObject(eof);
            oos.flush();

            fos.close();
            oos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void update(Block[] blocks) {
        for (Block block : blocks) {
            this.addBlock(block);
        }

        persist();
    }


    /**
     * Adds a Block to the BlockChain.
     * */
    public void addBlock(Block block) {
        chain.add(block);
        totalWorth = computeTotalChainWorth();
    }


    public double computeTotalChainWorth() {
        double worth = INITIAL_WORTH;
        double awardAmount = 200.00;
        int checkpointIncrement = 50000;
        int checkpoint = checkpointIncrement;

        for (int i = 1; i <= chain.size(); i++) {
            if (i == checkpoint) {
                awardAmount /= 2.0;
                checkpoint += checkpointIncrement;
            }
            worth += awardAmount;
        }

        return worth;
    }

    public double computeMinerAward(BigInteger chainLength) {
        double minerAward = 200.00;
        BigInteger checkpoint = new BigInteger("50000");
        BigInteger blockNumberToReduceAwardsAt = checkpoint;

        while (blockNumberToReduceAwardsAt.compareTo(chainLength) == -1) {
            minerAward /= 2.0;
            blockNumberToReduceAwardsAt = blockNumberToReduceAwardsAt.add(checkpoint);
        }

        return minerAward;
    }

    private byte[] getInitialHash() {
        return new byte[256];
    }

    public byte[] getMostRecentHash() {
        if (!chain.isEmpty()) {
            return chain.get(0).getProofOfWork();
        }else {
            return getInitialHash();
        }
    }

    public BigInteger getChainLength() {
        return new BigInteger(Integer.toString(chain.size()));
    }

    public Block getMostRecentBlock() {
        if (!chain.isEmpty()) {
            return chain.get(chain.size());
        }else {
            return null;
        }
    }

    public Block[] getSubChain(BigInteger origin) {
        if (chain.isEmpty()) {
            return null;
        }else {
            Integer startIndex = origin.intValue();
            Integer endIndex = chain.size();
            Integer length = (1+endIndex) - startIndex;

            Block[] subChain = new Block[length];

            for (int chainIndex = startIndex, arrayIndex = 0; chainIndex <= endIndex; chainIndex++, arrayIndex++) {
                subChain[arrayIndex] = chain.get(chainIndex);
            }

            return subChain;
        }

    }
    protected ArrayList<Block> getBlocks(){
        return this.chain;
    }

}
