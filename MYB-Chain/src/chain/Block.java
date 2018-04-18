package chain;

import org.omg.PortableInterceptor.InvalidSlot;

import java.io.*;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Created by Michael on 4/11/2018.
 */
public class Block implements Serializable{
    private static final int TRANSACTIONS_PER_BLOCK = 20;
    private final byte[] previousHash;
    private Transaction[] transactions;
    private BigInteger hashManipulator;
    private byte[] transactionBytes;
    private byte[] proofOfWork;
    private int transactionCount;
    private boolean full;


    public Block(byte[] previousBlockHash) {
        this.previousHash = previousBlockHash;
        this.transactions = new Transaction[TRANSACTIONS_PER_BLOCK];
        this.hashManipulator = BigInteger.ZERO.subtract(BigInteger.ONE);
        this.transactionBytes = null;
        this.proofOfWork = null;
        this.transactionCount = 0;
        this.full = false;
    }


    public void addVerifiedTransaction(Transaction transaction) {
        if (transaction.isVerified()) {
            if (transactionCount < TRANSACTIONS_PER_BLOCK) {
                transactions[transactionCount] = transaction;
                transactionCount++;
                if (transactionCount == TRANSACTIONS_PER_BLOCK) {
                    full = true;
                }
            }else {
                throw new InvalidParameterException("Error: Block already contains " + TRANSACTIONS_PER_BLOCK +
                        " verified Transactions. No additional Transactions may be added.");
            }
        }else {
            throw new InvalidParameterException("Error: Attempt to add an unverified Transaction to the Block.");
        }
    }


    public void convertTransactionsToByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutput output;

        try {
            output = new ObjectOutputStream(baos);
            ArrayList<byte[]> allTransactions = new ArrayList<>();
            int byteCount = 0;

            for (int i = 0; i < transactions.length; i++) {
                output.writeObject(transactions[i]);
                output.flush();
                byte[] currentTransactionBytes = baos.toByteArray();
                byteCount += currentTransactionBytes.length;
                baos.reset();
                allTransactions.add(currentTransactionBytes);
            }

            transactionBytes = new byte[byteCount];
            byteCount = 0;

            for (byte[] bytes : allTransactions) {
                for (int i = 0; i < bytes.length; i++) {
                    transactionBytes[byteCount] = bytes[i];
                }
                byteCount += bytes.length;
            }

        } finally {
            baos.close();
        }

    }


    private void incrementCounter() {
        hashManipulator.add(BigInteger.ONE);
    }


    private byte[] getBlockBytes() throws IOException {
        if (transactionBytes == null) {
            convertTransactionsToByteArray();
        }

        byte[] counterBytes = hashManipulator.toByteArray();
        byte[] blockBytes = new byte[previousHash.length + transactionBytes.length + counterBytes.length];
        int offset = 0;

        for (int i = 0; i < previousHash.length; i++, offset++) {
            blockBytes[offset] = previousHash[i];
        }
        for (int i = 0; i < transactionBytes.length; i++, offset++) {
            blockBytes[offset] = transactionBytes[i];
        }
        for (int i = 0; i < counterBytes.length; i++, offset++) {
            blockBytes[offset] = transactionBytes[i];
        }

        return blockBytes;
    }


    public boolean computeNewHash() throws NoSuchAlgorithmException, IOException {
        if (proofOfWork != null) {
            return true;
        }else {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            incrementCounter();
            byte[] hash = digest.digest(getBlockBytes());
            if (hash[0] == 0 && hash[1] == 0) {
                proofOfWork = hash;
                return true;
            }
            return false;
        }
    }


    public byte[] getProofOfWork() {
        return proofOfWork;
    }

    public boolean isFull() {
        return full;
    }

}

