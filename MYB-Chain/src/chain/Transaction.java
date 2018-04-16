package chain;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * Created by Michael on 4/11/2018.
 */
public class Transaction implements Serializable{
    private final int MAX_MESSAGE_LENGTH = 245;
    private final User buyer;
    private final User seller;
    private final Double amount;
    private final byte[] signature;
    private boolean verified;


    public Transaction(User buyerForTransaction, User sellerForTransaction, Double transactionAmount, RSAPrivateKey privateKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException {

        //chain.User buyerForTransaction = buyerForTransaction.clone();
        //chain.User sellerForTransaction = sellerForTransaction.clone();

        if ((transactionAmount >= 0.0) && (buyerForTransaction.getNetWorth() >= transactionAmount)) {
            this.buyer = buyerForTransaction;
            this.seller = sellerForTransaction;
            this.amount = transactionAmount;
            this.signature = signTransaction(privateKey);
            this.verified = false;

            if (!verify(buyerForTransaction.getPublicKey())) {
                throw new InvalidParameterException("ERROR: Unverified Signer.\n\n" +
                        "Attempted chain.Transaction with values of:\n" +
                        "Buyer Name:\t" + buyerForTransaction.getFullName() + "\n" +
                        "Buyer ID:\t" + buyerForTransaction.getID() + "\n" +
                        "Seller Name:\t" + sellerForTransaction.getFullName() + "\n" +
                        "Seller ID:\t" + sellerForTransaction.getID() + "\n" +
                        "Trans. Amt:\t" + transactionAmount.toString() + "\n");
            }
        }else if (buyerForTransaction.getNetWorth() < transactionAmount){
            throw new InvalidParameterException("ERROR: Insufficient Funds.\n\n" +
                    "Attempted chain.Transaction with values of:\n" +
                    "Buyer Name:\t" + buyerForTransaction.getFullName() + "\n" +
                    "Buyer ID:\t" + buyerForTransaction.getID() + "\n" +
                    "Seller Name:\t" + sellerForTransaction.getFullName() + "\n" +
                    "Seller ID:\t" + sellerForTransaction.getID() + "\n\n" +
                    "Buyer Net Worth:\t" + buyerForTransaction.getNetWorth() + "\n" +
                    "chain.Transaction Amt:\t" + transactionAmount.toString() + "\n");
        }else {
            throw new InvalidParameterException("ERROR: chain.Transaction Amounts cannot be negative.\n\n" +
                    "Attempted chain.Transaction with values of:\n" +
                    "Buyer Name:\t" + buyerForTransaction.getFullName() + "\n" +
                    "Buyer ID:\t" + buyerForTransaction.getID() + "\n" +
                    "Seller Name:\t" + sellerForTransaction.getFullName() + "\n" +
                    "Seller ID:\t" + sellerForTransaction.getID() + "\n" +
                    "Trans. Amt:\t" + transactionAmount.toString() + "\n");
        }

    }

    protected Transaction(User miner, Double transactionAmount) {
        /*
        * TODO -- Finish transaction constructor for miners
        * */

        this.buyer = null;
        this.seller = miner;
        this.amount = transactionAmount;
        this.signature = getMessage();
        this.verified = true;

    }


    /**
     * Creates the "message" of the transaction. Concretely, this refers to a byte[] containing each the buyer's and
     * seller's IDs (truncated if necessary to ensure that the message length is not longer than 245 bytes), along with
     * the amount of the transaction.
     * */
    private byte[] getMessage() {
        byte[] buyerBytes = buyer.getID().getBytes();
        byte[] sellerBytes = seller.getID().getBytes();
        byte[] amountBytes = new byte[8];
        ByteBuffer.wrap(amountBytes).putDouble(amount);

        int buyersBytesLength = buyerBytes.length;
        int sellersBytesLength = sellerBytes.length;

        if ((buyerBytes.length + sellerBytes.length) > MAX_MESSAGE_LENGTH) {
            buyersBytesLength = Math.toIntExact(Math.round(Math.floor(
                    (1.0*buyerBytes.length) / (1.0*(buyerBytes.length + sellerBytes.length))) * MAX_MESSAGE_LENGTH));
            sellersBytesLength = Math.toIntExact(Math.round(Math.floor(
                    (1.0*sellerBytes.length) / (1.0*(buyerBytes.length + sellerBytes.length))) * MAX_MESSAGE_LENGTH));
        }

        byte[] message = new byte[buyerBytes.length + sellerBytes.length + amountBytes.length];
        int offset = 0;

        for (int i = 0; i < buyersBytesLength; i++) {
            message[offset + i] = buyerBytes[i];
        }
        offset = buyerBytes.length;

        for (int i = 0; i < sellersBytesLength; i++) {
            message[offset + i] = sellerBytes[i];
        }
        offset += sellerBytes.length;

        for (int i = 0; i < amountBytes.length; i++) {
            message[offset + i] = amountBytes[i];
        }

        return message;
    }


    /**
     * Generates a signature for the transaction by encrypting the relevant transaction data, using the provided
     * RSAPrivateKey.
     *
     * From 3Blue1Brown video:
     * signature = sign(message, privateKey)
     * */
    private byte[] signTransaction(RSAPrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        return cipher.doFinal(getMessage());

    }


    /**
     * Verifies a transaction with the prescribed message (IDs and amount) and signature, using the provided
     * RSAPublicKey.
     *
     * From 3Blue1Brown video:
     * T/F = verify(message, signature, publicKey)
     * */
    private boolean verify(RSAPublicKey publicKey) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] message = getMessage();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decryptedData = cipher.doFinal(signature);

        if (decryptedData.length != message.length) {
            return false;
        }

        for (int i = 0; i < message.length; i++) {
            if (decryptedData[i] != message[i]) {
                return false;
            }
        }

        verified = true;
        return true;

    }

    /*private void writeObject(ObjectOutputStream objectOutputStream) throws IOException{
        //objectOutputStream.defaultWriteObject();
        objectOutputStream.writeUTF(buyerID);
        objectOutputStream.writeUTF(sellerID);
        objectOutputStream.writeDouble(amount);
        objectOutputStream.write(signature);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException{
        byte[] bytes = new byte[MAX_MESSAGE_LENGTH];
        //objectInputStream.defaultReadObject();
        String buyerUID = objectInputStream.readUTF();
        String sellerUID = objectInputStream.readUTF();
        Double transactionAmount = objectInputStream.readDouble();
        int signatureLength = objectInputStream.read(bytes);
        byte[] encryptedBytes = Arrays.copyOf(bytes, signatureLength);
    }

    private void readObjectNoData() throws ObjectStreamException{

    }*/


    public String getBuyerID() {
        return buyer.getID();
    }

    public String getSellerID() {
        return seller.getID();
    }

    public String getBuyerName() {
        return buyer.getFullName();
    }

    public String getSellerName() {
        return seller.getFullName();
    }

    public double getTransactionAmount() {
        return amount;
    }

    public byte[] getSignature() {
        return Arrays.copyOf(signature, signature.length);
    }

    public boolean isVerified() {
        return verified;
    }

}
