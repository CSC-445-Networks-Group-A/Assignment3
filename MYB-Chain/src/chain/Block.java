package chain;

import common.Pair;
import org.omg.PortableInterceptor.InvalidSlot;

import java.io.*;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Michael on 4/11/2018.
 */
public class Block implements Serializable, Comparable<Block> {
    private static final int TRANSACTIONS_PER_BLOCK = 10;
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


    private void convertTransactionsToByteArray() throws IOException {
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
        hashManipulator = hashManipulator.add(BigInteger.ONE);
    }


    public byte[] getBlockBytes() throws IOException {
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
            blockBytes[offset] = counterBytes[i];
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
            //if (hash[0] == 0 && hash[1] == 0) {
            if (hash[0] < 127) {
                proofOfWork = hash;
                return true;
            }
            return false;
        }
    }


    public byte[] getPreviousHash() {
        return previousHash;
    }

    public byte[] getProofOfWork() {
        return proofOfWork;
    }

    public Transaction[] getTransactions() {
        Transaction[] returnValue = new Transaction[TRANSACTIONS_PER_BLOCK];
        for (int i = 0; i < transactions.length; i++) {
            if (transactions[i] == null) {
                returnValue[i] = null;
            }else {
                returnValue[i] = transactions[i].clone();
            }
        }
        return returnValue;
    }

    public boolean isFull() {
        return full;
    }


    @Override
    public int compareTo(Block otherBlock) {
        byte[] otherProofOfWork = otherBlock.getProofOfWork();

        for (int i = 0; i < proofOfWork.length; i++) {
            if (proofOfWork[i] == 0 && otherProofOfWork[i] != 0) {
                return 1;
            }else if (otherProofOfWork[i] == 0 && proofOfWork[i] != 0) {
                return -1;
            }else if (proofOfWork[i] != 0 && otherProofOfWork[i] != 0) {
                if (proofOfWork[i] < otherProofOfWork[i]) {
                    return 1;
                }else if (proofOfWork[i] > otherProofOfWork[i]) {
                    return -1;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Block) {
            Block otherBlock = (Block) obj;
            if (proofOfWork.length != otherBlock.proofOfWork.length) {
                return false;
            }

            for (int i = 0; i < proofOfWork.length; i++) {
                if (proofOfWork[i] != otherBlock.proofOfWork[i]) {
                    return false;
                }
            }

            return true;
        }else {
            return false;
        }
    }
}

