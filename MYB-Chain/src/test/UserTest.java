package test;

import chain.Block;
import chain.Transaction;
import chain.User;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class UserTest{

    public static void main(String args[]) throws NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeyException, IOException {

        String firstName_Person1 = "Person";
        String lastName_Person1 = "One";

        String firstName_Person2 = "Person";
        String lastName_Person2 = "Two";

        Double initialNetWorth = 20000.00;


        User buyer = null;
        User seller = null;
        try {
            buyer = new User(firstName_Person1, lastName_Person1, initialNetWorth, 0,0);
            seller = new User(firstName_Person2, lastName_Person2, initialNetWorth, 0, 0);
            buyer.login();
            seller.login();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] bytes = new byte[64];
        Arrays.fill( bytes, (byte) 1 );
        Block block = new Block(bytes);
        for(int t = 0; t < 20; t++){
            Transaction transaction = buyer.makeTransaction(seller, 10.0);
            block.addVerifiedTransaction(transaction);
        }

        boolean found = false;
        while(!found){
            found = block.computeNewHash();
            System.out.println(block.getProofOfWork());
        }






    }
}