package client;

import chain.Transaction;
import chain.User;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

public class Main {
    private static final String CLIENT_TO_PROPOSER_ADDRESS = "230.0.0.0";

    public static void main(String[] args) throws NoSuchAlgorithmException, IllegalBlockSizeException,
            InvalidKeyException, BadPaddingException, NoSuchPaddingException {
        String firstName_Person1 = "Person";
        String lastName_Person1 = "One";

        String firstName_Person2 = "Person";
        String lastName_Person2 = "Two";

        Double initialNetWorth = 20000.00;

        User buyer = new User(firstName_Person1, lastName_Person1, initialNetWorth);
        User seller = new User(firstName_Person2, lastName_Person2, initialNetWorth);

        printPersonInfo(buyer);
        printPersonInfo(seller);

        Double amount = 10000.00;

        Transaction transaction = buyer.makeTransaction(seller, amount);
        printTransactionInfo(transaction, buyer.getPublicKey());

    }


    private static void requestTransaction(User buyer, User seller, Double amount) throws InvalidKeyException,
            BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException {
        Transaction transaction = buyer.makeTransaction(seller, amount);




    }

    private static void printPersonInfo(User person) {
        System.out.println("Person Info:\n" +
                "First Name:\t" + person.getFirstName() + "\n" +
                "Last Name:\t" + person.getLastName() + "\n" +
                "UID:\t" + person.getID() + "\n" +
                "Net Worth:\t" + person.getNetWorth() + "\n" +
                "Public Key:\t" + person.getPublicKey() + "\n");

    }

    private static void printTransactionInfo(Transaction transaction, RSAPublicKey buyerPublicKey)
            throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        System.out.println("Transaction Info:\n" +
                "Buyer ID:\t" + transaction.getBuyerID() + "\n" +
                "Seller ID:\t" + transaction.getSellerID() + "\n" +
                "Trans Amt:\t" + transaction.getTransactionAmount() + "\n" +
                "Signature:\t" + transaction.getSignature() + "\n" +
                "Verified? -->\t" + transaction.isVerified() + "\n");

        byte[] signature = transaction.getSignature();
        System.out.println("Signature Length:\t" + signature.length);
        for (int i = 0; i < signature.length; i++) {
            System.out.println("signature[" + i + "]:\t" + signature[i]);
        }
    }
}
