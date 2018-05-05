package client;

import chain.User;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Client {
    private static User myUser;

    public static void main(String args[]){

        if(User.userFileExists()){
            myUser = User.loadUser();
            ClientUI client = new ClientUI(myUser);
            client.setVisible(true);
            client.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }else {
            RegistrationView regView = new RegistrationView();
            regView.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    JOptionPane.showMessageDialog(null,  regView.getLastname() + " " +  regView.getFirstname());
                    try {
                        myUser = new User(regView.getFirstname(), regView.getLastname(), 0.0);
                        try {
                            myUser.commitUser();
                        } catch (InvalidKeyException | BadPaddingException | NoSuchPaddingException | IllegalBlockSizeException | IOException e) {
                            e.printStackTrace();
                        }
                        regView.setVisible(false);

                        ClientUI client = new ClientUI(myUser);
                        client.setVisible(true);
                        client.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

                    } catch (NoSuchAlgorithmException | UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            });

            regView.setVisible(true);
        }

    }

}
