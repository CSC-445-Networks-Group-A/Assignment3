package client;

import chain.User;

import javax.swing.*;
import java.io.IOException;

public class Client {

    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException {

        if(User.userFileExists()){
            User myUser = User.loadUser();
            myUser.updateBlockChain();

            ClientUI client = new ClientUI(myUser);
            client.setVisible(true);
            client.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            myUser.start();
            myUser.join();
        }else {
            RegistrationView regView = new RegistrationView();
            regView.setVisible(true);
        }

    }

}
